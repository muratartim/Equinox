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
import java.sql.ResultSet;
import java.sql.Statement;

import equinox.Equinox;
import equinox.controller.ActiveTasksPanel;
import equinox.controller.DeleteElementGroupsPanel;
import equinox.data.fileType.AircraftModel;
import equinox.task.InternalEquinoxTask.LongRunningTask;

/**
 * Class for delete element group task.
 *
 * @author Murat Artim
 * @date Jul 30, 2015
 * @time 11:06:07 AM
 */
public class DeleteElementGroup extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** The owner panel. */
	private final DeleteElementGroupsPanel panel_;

	/** Model. */
	private final AircraftModel model_;

	/** Group and new name. */
	private final String group_;

	/**
	 * Creates rename element group task.
	 *
	 * @param panel
	 *            The owner panel.
	 * @param model
	 *            A/C model.
	 * @param group
	 *            Element group.
	 */
	public DeleteElementGroup(DeleteElementGroupsPanel panel, AircraftModel model, String group) {
		panel_ = panel;
		model_ = model;
		group_ = group;
	}

	@Override
	public String getTaskTitle() {
		return "Delete element group '" + group_ + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Deleting element group '" + group_ + "'...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// delete
				deleteGroup(connection);

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

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// get element groups and positions
		ActiveTasksPanel tm = taskPanel_.getOwner();
		tm.runTaskInParallel(new GetElementGroups(panel_, model_));
	}

	/**
	 * Renames element group.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void deleteGroup(Connection connection) throws Exception {

		// create statement
		try (Statement statement = connection.createStatement()) {

			// get group ID
			int groupID = -1;
			String sql = "select group_id from element_group_names_" + model_.getID() + " where name = '" + group_ + "'";
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					groupID = resultSet.getInt("group_id");
				}
			}

			// group not found
			if (groupID == -1)
				return;

			// delete from groups
			sql = "delete from element_groups_" + model_.getID() + " where group_id = " + groupID;
			statement.executeUpdate(sql);

			// delete from group names
			sql = "delete from element_group_names_" + model_.getID() + " where group_id = " + groupID;
			statement.executeUpdate(sql);
		}
	}
}
