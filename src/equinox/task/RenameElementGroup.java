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
import java.sql.Statement;

import equinox.Equinox;
import equinox.controller.ActiveTasksPanel;
import equinox.controller.RenameElementGroupPanel;
import equinox.data.fileType.AircraftModel;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for rename element group task.
 *
 * @author Murat Artim
 * @date Jul 27, 2015
 * @time 1:40:34 PM
 */
public class RenameElementGroup extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** The owner panel. */
	private final RenameElementGroupPanel panel_;

	/** Model. */
	private final AircraftModel model_;

	/** Group and new name. */
	private final String group_, name_;

	/**
	 * Creates rename element group task.
	 *
	 * @param panel
	 *            The owner panel.
	 * @param model
	 *            A/C model.
	 * @param group
	 *            Element group.
	 * @param name
	 *            New group name.
	 */
	public RenameElementGroup(RenameElementGroupPanel panel, AircraftModel model, String group, String name) {
		panel_ = panel;
		model_ = model;
		group_ = group;
		name_ = name;
	}

	@Override
	public String getTaskTitle() {
		return "Rename element group";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Renaming element group...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// rename file
				renameGroup(connection);

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
	private void renameGroup(Connection connection) throws Exception {

		// create statement
		try (Statement statement = connection.createStatement()) {
			String sql = "update element_group_names_" + model_.getID() + " set name = '" + name_ + "' where name = '" + group_ + "'";
			statement.executeUpdate(sql);
		}
	}
}
