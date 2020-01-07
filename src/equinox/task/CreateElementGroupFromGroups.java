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
 * Class for create group from existing groups task.
 *
 * @author Murat Artim
 * @date Aug 3, 2015
 * @time 1:27:05 PM
 */
public class CreateElementGroupFromGroups extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** Operation index. */
	public static final int UNION = 0, INTERSECTION = 1, COMPLEMENT = 2, DIFFERENCE = 3;

	/** Model. */
	private final AircraftModel model_;

	/** Group name. */
	private final String name_, groupA_, groupB_;

	/** Operation. */
	private final int operation_;

	/**
	 * Creates create group from existing groups task.
	 *
	 * @param model
	 *            A/C model.
	 * @param name
	 *            Name of new group.
	 * @param groupA
	 *            Group A.
	 * @param groupB
	 *            Group B.
	 * @param operation
	 *            Operation.
	 */
	public CreateElementGroupFromGroups(AircraftModel model, String name, String groupA, String groupB, int operation) {
		model_ = model;
		name_ = name;
		groupA_ = groupA;
		groupB_ = groupB;
		operation_ = operation;
	}

	@Override
	public String getTaskTitle() {
		return "Create element group from existing groups";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Creating element group from existing groups");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// create group
				try (Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
					createGroup(connection, statement);
				}

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
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void createGroup(Connection connection, Statement statement) throws Exception {

		// get group IDs
		int groupAID = -1, groupBID = -1;
		String sql = "select name, group_id from element_group_names_" + model_.getID() + " where name = '" + groupA_ + "' or name = '" + groupB_ + "'";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				if (resultSet.getString("name").equals(groupA_)) {
					groupAID = resultSet.getInt("group_id");
				}
				else {
					groupBID = resultSet.getInt("group_id");
				}
			}
		}

		// cannot create group
		if ((groupAID == -1) || (groupBID == -1) || isCancelled())
			return;

		// add to group names
		updateMessage("Creating group '" + name_ + "'...");
		int groupID = -1;
		sql = "insert into ELEMENT_GROUP_NAMES_" + model_.getID() + "(name) values('" + name_ + "')";
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
		int numel = -1;

		// union
		if (operation_ == UNION) {
			numel = union(connection, statement, groupID, groupAID, groupBID);
		}
		else if (operation_ == INTERSECTION) {
			numel = intersection(connection, statement, groupID, groupAID, groupBID);
		}
		else if (operation_ == COMPLEMENT) {
			numel = complement(connection, statement, groupID, groupAID, groupBID);
		}
		else if (operation_ == DIFFERENCE) {
			numel = difference(connection, statement, groupID, groupAID, groupBID);
		}

		// task cancelled
		if ((numel == -1) || isCancelled())
			return;

		// update number of elements of group
		updateMessage("Updating number of elements of group...");
		sql = "update ELEMENT_GROUP_NAMES_" + model_.getID() + " set numel = " + numel + " where group_id = " + groupID;
		statement.executeUpdate(sql);
	}

	/**
	 * Creates new group.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param groupID
	 *            Group ID.
	 * @param groupAID
	 *            Group A ID.
	 * @param groupBID
	 *            Group B ID.
	 * @return Number of elements of the newly created group.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int union(Connection connection, Statement statement, int groupID, int groupAID, int groupBID) throws Exception {

		// initialize number of elements
		int numel = 0;

		// prepare statement to insert EIDs
		String sql = "insert into element_groups_" + model_.getID() + "(group_id, eid) values(?, ?)";
		try (PreparedStatement insertToGroups = connection.prepareStatement(sql)) {

			// set group ID
			insertToGroups.setInt(1, groupID);

			// create and execute statement to select element IDs
			sql = "select distinct eid from element_groups_" + model_.getID() + " where group_id = " + groupAID + " or group_id = " + groupBID + " order by eid";
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// move to last row
				if (resultSet.last()) {

					// get number of elements
					numel = resultSet.getRow();

					// move to beginning
					resultSet.beforeFirst();

					// loop over element IDs
					int num = 0;
					while (resultSet.next()) {

						// task cancelled
						if (isCancelled())
							return -1;

						// progress info
						updateProgress(num, numel);

						// insert element
						insertToGroups.setInt(2, resultSet.getInt("eid"));
						insertToGroups.executeUpdate();

						// increment num
						num++;
					}
				}
			}
		}

		// return number of elements
		return numel;
	}

	/**
	 * Creates new group.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param groupID
	 *            Group ID.
	 * @param groupAID
	 *            Group A ID.
	 * @param groupBID
	 *            Group B ID.
	 * @return Number of elements of the newly created group.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int intersection(Connection connection, Statement statement, int groupID, int groupAID, int groupBID) throws Exception {

		// initialize number of elements
		int numel = 0;

		// prepare statement to insert EIDs
		String sql = "insert into element_groups_" + model_.getID() + "(group_id, eid) values(?, ?)";
		try (PreparedStatement insertToGroups = connection.prepareStatement(sql)) {

			// set group ID
			insertToGroups.setInt(1, groupID);

			// create and execute statement to select element IDs
			sql = "select eid, count(eid) from element_groups_" + model_.getID();
			sql += " where group_id = " + groupAID + " or group_id = " + groupBID;
			sql += " group by eid having(count(eid) > 1) order by eid";
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// move to last row
				if (resultSet.last()) {

					// get number of elements
					numel = resultSet.getRow();

					// move to beginning
					resultSet.beforeFirst();

					// loop over element IDs
					int num = 0;
					while (resultSet.next()) {

						// task cancelled
						if (isCancelled())
							return -1;

						// progress info
						updateProgress(num, numel);

						// insert element
						insertToGroups.setInt(2, resultSet.getInt("eid"));
						insertToGroups.executeUpdate();

						// increment num
						num++;
					}
				}
			}
		}

		// return number of elements
		return numel;
	}

	/**
	 * Creates new group.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param groupID
	 *            Group ID.
	 * @param groupAID
	 *            Group A ID.
	 * @param groupBID
	 *            Group B ID.
	 * @return Number of elements of the newly created group.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int complement(Connection connection, Statement statement, int groupID, int groupAID, int groupBID) throws Exception {

		// initialize number of elements
		int numel = 0;

		// prepare statement to insert EIDs
		String sql = "insert into element_groups_" + model_.getID() + "(group_id, eid) values(?, ?)";
		try (PreparedStatement insertToGroups = connection.prepareStatement(sql)) {

			// set group ID
			insertToGroups.setInt(1, groupID);

			// create and execute statement to select element IDs
			sql = "select eid from element_groups_" + model_.getID();
			sql += " where group_id = " + groupAID + " and eid not in";
			sql += " (select eid from element_groups_" + model_.getID() + " where group_id = " + groupBID + ")";
			sql += " order by eid";
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// move to last row
				if (resultSet.last()) {

					// get number of elements
					numel = resultSet.getRow();

					// move to beginning
					resultSet.beforeFirst();

					// loop over element IDs
					int num = 0;
					while (resultSet.next()) {

						// task cancelled
						if (isCancelled())
							return -1;

						// progress info
						updateProgress(num, numel);

						// insert element
						insertToGroups.setInt(2, resultSet.getInt("eid"));
						insertToGroups.executeUpdate();

						// increment num
						num++;
					}
				}
			}
		}

		// return number of elements
		return numel;
	}

	/**
	 * Creates new group.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param groupID
	 *            Group ID.
	 * @param groupAID
	 *            Group A ID.
	 * @param groupBID
	 *            Group B ID.
	 * @return Number of elements of the newly created group.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int difference(Connection connection, Statement statement, int groupID, int groupAID, int groupBID) throws Exception {

		// initialize number of elements
		int numel = 0;

		// prepare statement to insert EIDs
		String sql = "insert into element_groups_" + model_.getID() + "(group_id, eid) values(?, ?)";
		try (PreparedStatement insertToGroups = connection.prepareStatement(sql)) {

			// set group ID
			insertToGroups.setInt(1, groupID);

			// create and execute statement to select element IDs
			sql = "select eid, count(eid) from element_groups_" + model_.getID();
			sql += " where group_id = " + groupAID + " or group_id = " + groupBID;
			sql += " group by eid having(count(eid) = 1) order by eid";
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// move to last row
				if (resultSet.last()) {

					// get number of elements
					numel = resultSet.getRow();

					// move to beginning
					resultSet.beforeFirst();

					// loop over element IDs
					int num = 0;
					while (resultSet.next()) {

						// task cancelled
						if (isCancelled())
							return -1;

						// progress info
						updateProgress(num, numel);

						// insert element
						insertToGroups.setInt(2, resultSet.getInt("eid"));
						insertToGroups.executeUpdate();

						// increment num
						num++;
					}
				}
			}
		}

		// return number of elements
		return numel;
	}
}
