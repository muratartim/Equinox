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
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.task.InternalEquinoxTask.LongRunningTask;

/**
 * Class for create element group from equivalent stress task.
 *
 * @author Murat Artim
 * @date Sep 7, 2015
 * @time 4:52:52 PM
 */
public class CreateElementGroupFromEquivalentStress extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** Equivalent stress. */
	private final AircraftFatigueEquivalentStress equivalentStress_;

	/** Group name. */
	private final String name_;

	/**
	 * Creates create element group from equivalent stress task.
	 *
	 * @param equivalentStress
	 *            Equivalent stress.
	 * @param name
	 *            Group name.
	 */
	public CreateElementGroupFromEquivalentStress(AircraftFatigueEquivalentStress equivalentStress, String name) {
		equivalentStress_ = equivalentStress;
		name_ = name;
	}

	@Override
	public String getTaskTitle() {
		return "Create element group from equivalent stresses '" + equivalentStress_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Creating element group from equivalent stresses '" + equivalentStress_.getName() + "'");

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
			String sql = "insert into ELEMENT_GROUP_NAMES_" + equivalentStress_.getID() + "(name) values('" + name_ + "')";
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
			int numel = 0;
			sql = "select count(eid) as elcount from ac_eq_stresses_" + equivalentStress_.getID() + " where name = '" + equivalentStress_.getName() + "'";
			try (ResultSet getElementCount = statement.executeQuery(sql)) {
				while (getElementCount.next()) {
					numel = getElementCount.getInt("elcount");
				}
			}

			// prepare statement for inserting into element groups
			sql = "insert into ELEMENT_GROUPS_" + equivalentStress_.getID() + "(group_id, eid) values(?, ?)";
			try (PreparedStatement insertToGroups = connection.prepareStatement(sql)) {

				// set group ID
				insertToGroups.setInt(1, groupID);

				// prepare statement for selecting element IDs
				sql = "select distinct eid from ac_eq_stresses_" + equivalentStress_.getID() + " where name = '" + equivalentStress_.getName() + "' order by eid";
				try (ResultSet getEIDs = statement.executeQuery(sql)) {

					// loop over EIDs
					int count = 0;
					while (getEIDs.next()) {

						// task cancelled
						if (isCancelled())
							return;

						// update progress
						updateProgress(count, numel);
						count++;

						// insert into groups
						insertToGroups.setInt(2, getEIDs.getInt("eid"));
						insertToGroups.executeUpdate();
					}
				}
			}

			// task cancelled
			if (isCancelled())
				return;

			// update number of elements of group
			sql = "update ELEMENT_GROUP_NAMES_" + equivalentStress_.getID() + " set numel = " + numel + " where group_id = " + groupID;
			statement.executeUpdate(sql);
		}
	}
}
