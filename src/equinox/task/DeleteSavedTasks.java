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
import java.util.ArrayList;

import equinox.Equinox;
import equinox.data.ui.SavedTaskItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.collections.ObservableList;

/**
 * Class for delete saved task task.
 *
 * @author Murat Artim
 * @date Oct 9, 2015
 * @time 1:16:26 PM
 */
public class DeleteSavedTasks extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** Tasks to delete. */
	private final SavedTaskItem[] tasks_;

	/** True if tasks are scheduled tasks. */
	private final boolean isScheduled_;

	/**
	 * Creates delete saved task task.
	 *
	 * @param tasks
	 *            Tasks to delete.
	 * @param isScheduled
	 *            True if tasks are scheduled tasks.
	 */
	public DeleteSavedTasks(ObservableList<SavedTaskItem> tasks, boolean isScheduled) {
		tasks_ = new SavedTaskItem[tasks.size()];
		for (int i = 0; i < tasks.size(); i++) {
			tasks_[i] = tasks.get(i);
		}
		isScheduled_ = isScheduled;
	}

	/**
	 * Creates delete saved task task.
	 *
	 * @param tasks
	 *            Tasks to delete.
	 * @param isScheduled
	 *            True if tasks are scheduled tasks.
	 */
	public DeleteSavedTasks(ArrayList<SavedTaskItem> tasks, boolean isScheduled) {
		tasks_ = new SavedTaskItem[tasks.size()];
		for (int i = 0; i < tasks.size(); i++) {
			tasks_[i] = tasks.get(i);
		}
		isScheduled_ = isScheduled;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Delete " + (isScheduled_ ? "scheduled" : "saved") + " tasks";
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Deleting " + (isScheduled_ ? "scheduled" : "saved") + " tasks");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// prepare statement
				String sql = "delete from saved_tasks where task_id = ?";
				try (PreparedStatement statement = connection.prepareStatement(sql)) {

					// loop over tasks
					for (SavedTaskItem task : tasks_) {

						// delete
						statement.setInt(1, task.getTaskID());
						statement.executeUpdate();
					}
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return
				return null;
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				if (connection != null) {
					connection.rollback();
					connection.setAutoCommit(true);
				}

				// propagate exception
				throw e;
			}
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// saved task
		if (!isScheduled_) {
			taskPanel_.getOwner().getOwner().getSavedTasksPanel().getSavedTasks().getItems().removeAll(tasks_);
		}
		else {
			taskPanel_.getOwner().getOwner().getScheduledTasksPanel().getScheduledTasks().getItems().removeAll(tasks_);
		}
	}
}
