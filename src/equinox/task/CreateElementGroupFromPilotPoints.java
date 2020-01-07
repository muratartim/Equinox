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
 * Class for create element group from pilot points task.
 *
 * @author Murat Artim
 * @date Sep 8, 2015
 * @time 9:48:49 AM
 */
public class CreateElementGroupFromPilotPoints extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** A/C model. */
	private final AircraftModel model_;

	/** Group name. */
	private final String name_;

	/**
	 * Creates create element group from pilot points task.
	 *
	 * @param model
	 *            A/C model.
	 * @param name
	 *            Group name.
	 */
	public CreateElementGroupFromPilotPoints(AircraftModel model, String name) {
		model_ = model;
		name_ = name;
	}

	@Override
	public String getTaskTitle() {
		return "Create element group from linked pilot points";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Create element group from linked pilot points");

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

		// update progress info
		updateMessage("Creating element group...");

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

			// get number of elements
			int numel = 0, realNum = 0;
			sql = "select count(name) as numel from stf_files inner join pilot_points_" + model_.getID() + " on ";
			sql += "stf_files.file_id = pilot_points_" + model_.getID() + ".stf_id order by name";
			try (ResultSet getNumel = statement.executeQuery(sql)) {
				while (getNumel.next()) {
					numel = getNumel.getInt("numel");
				}
			}

			// prepare statement for inserting into element groups
			sql = "insert into ELEMENT_GROUPS_" + model_.getID() + "(group_id, eid) values(?, ?)";
			try (PreparedStatement insertToGroups = connection.prepareStatement(sql)) {

				// prepare statement for checking if EID already exists in element group
				sql = "select eid from ELEMENT_GROUPS_" + model_.getID() + " where group_id = ? and eid = ?";
				try (PreparedStatement checkEID = connection.prepareStatement(sql)) {

					// set group ID
					insertToGroups.setInt(1, groupID);
					checkEID.setInt(1, groupID);

					// create and execute statement to get linked STF file names
					sql = "select stf_files.eid from stf_files inner join pilot_points_" + model_.getID() + " on ";
					sql += "stf_files.file_id = pilot_points_" + model_.getID() + ".stf_id order by name";
					try (ResultSet getEIDs = statement.executeQuery(sql)) {

						// loop over STF file names
						int count = 0;
						while (getEIDs.next()) {

							// task cancelled
							if (isCancelled())
								return;

							// update progress
							updateProgress(count, numel);
							count++;

							// get EID
							int eid = Integer.parseInt(getEIDs.getString("eid"));

							// check EID for existence
							boolean exists = false;
							checkEID.setInt(2, eid);
							try (ResultSet check = checkEID.executeQuery()) {
								while (check.next()) {
									exists = true;
								}
							}

							// EID already exists
							if (exists) {
								continue;
							}

							// insert into groups
							insertToGroups.setInt(2, eid);
							insertToGroups.executeUpdate();
							realNum++;
						}
					}
				}
			}

			// task cancelled
			if (isCancelled())
				return;

			// update number of elements of group
			sql = "update ELEMENT_GROUP_NAMES_" + model_.getID() + " set numel = " + realNum + " where group_id = " + groupID;
			statement.executeUpdate(sql);
		}
	}
}
