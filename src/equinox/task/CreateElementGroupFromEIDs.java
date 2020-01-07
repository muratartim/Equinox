/*
 * Copyright 2018 Murat Artim (muratartim@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package equinox.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import equinox.Equinox;
import equinox.data.fileType.AircraftModel;
import equinox.task.InternalEquinoxTask.LongRunningTask;

/**
 * Class for create element group from element IDs task.
 *
 * @author Murat Artim
 * @date Jul 30, 2015
 * @time 2:16:27 PM
 */
public class CreateElementGroupFromEIDs extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** Model. */
	private final AircraftModel model_;

	/** Group name. */
	private final String name_;

	/** Element IDs. */
	private final int[] eids_;

	/**
	 * Creates create element group from element IDs task.
	 *
	 * @param model
	 *            A/C model.
	 * @param name
	 *            Group name.
	 * @param eids
	 *            Element IDs.
	 */
	public CreateElementGroupFromEIDs(AircraftModel model, String name, int[] eids) {
		model_ = model;
		name_ = name;
		eids_ = eids;
	}

	@Override
	public String getTaskTitle() {
		return "Create element group from EIDs";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Creating element group from EIDs");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// create group
				createGroup(connection);

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				connection.rollback();
				connection.setAutoCommit(true);

				// propagate exception
				throw e;
			}
		}
		return null;
	}

	/**
	 * Creates element group.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void createGroup(Connection connection) throws Exception {

		// create statement
		try (Statement statement = connection.createStatement()) {

			// add to group names
			int groupID = -1;
			String sql = "insert into ELEMENT_GROUP_NAMES_" + model_.getID() + "(name) values('" + name_ + "')";
			statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
			try (ResultSet resultSet = statement.getGeneratedKeys()) {
				while (resultSet.next()) {
					groupID = resultSet.getBigDecimal(1).intValue();
				}
			}

			// cannot create group
			if ((groupID == -1) || isCancelled())
				return;

			// initialize number of elements
			int numel = 0;

			// prepare statement for inserting into element groups
			sql = "insert into ELEMENT_GROUPS_" + model_.getID() + "(group_id, eid) values(?, ?)";
			try (PreparedStatement insertToGroups = connection.prepareStatement(sql)) {

				// set group ID
				insertToGroups.setInt(1, groupID);

				// prepare statement for selecting element IDs
				sql = "select eid from ELEMENTS_" + model_.getID() + " where eid = ?";
				try (PreparedStatement selectElement = connection.prepareStatement(sql)) {

					// loop over EIDs
					for (int eid : eids_) {

						// task cancelled
						if (isCancelled())
							return;

						// insert into groups
						selectElement.setInt(1, eid);
						try (ResultSet resultSet = selectElement.executeQuery()) {
							boolean exists = false;
							while (resultSet.next()) {
								insertToGroups.setInt(2, resultSet.getInt("eid"));
								insertToGroups.executeUpdate();
								numel++;
								exists = true;
							}

							// invalid element ID
							if (!exists) {
								addWarning("Invalid element ID encountered: " + eid);
							}
						}
					}
				}
			}

			// task cancelled
			if (isCancelled())
				return;

			// update number of elements of group
			sql = "update ELEMENT_GROUP_NAMES_" + model_.getID() + " set numel = " + numel + " where group_id = " + groupID;
			statement.executeUpdate(sql);
		}
	}
}
