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
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;

import equinox.Equinox;
import equinox.data.ui.SavedTaskItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.collections.ObservableList;

/**
 * Class for re-schedule saved tasks task.
 *
 * @author Murat Artim
 * @date Oct 10, 2015
 * @time 1:49:27 PM
 */
public class RescheduleSavedTasks extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** Tasks to re-schedule. */
	private final SavedTaskItem[] tasks_;

	/** Schedule date. */
	private final Date scheduleDate_;

	/**
	 * Creates re-schedule saved tasks task.
	 *
	 * @param tasks
	 *            Tasks to re-schedule.
	 * @param scheduleDate
	 *            Schedule date.
	 */
	public RescheduleSavedTasks(ObservableList<SavedTaskItem> tasks, Date scheduleDate) {
		tasks_ = new SavedTaskItem[tasks.size()];
		for (int i = 0; i < tasks.size(); i++) {
			tasks_[i] = tasks.get(i);
		}
		scheduleDate_ = scheduleDate;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Reschedule saved tasks";
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Rescheduling saved tasks");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// prepare statement
				String sql = "update saved_tasks set schedule_date = ? where task_id = ?";
				try (PreparedStatement statement = connection.prepareStatement(sql)) {

					// create time stamp
					Timestamp date = new Timestamp(scheduleDate_.getTime());

					// loop over tasks
					for (SavedTaskItem task : tasks_) {

						// re-schedule
						statement.setTimestamp(1, date);
						statement.setInt(2, task.getTaskID());
						statement.executeUpdate();

						// set date to task
						task.setDate(scheduleDate_);
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

		// update schedule dates in UI
		ObservableList<SavedTaskItem> tasks = taskPanel_.getOwner().getOwner().getScheduledTasksPanel().getScheduledTasks().getItems();
		for (int i = 0; i < tasks.size(); i++) {
			for (SavedTaskItem rescheduledTask : tasks_) {
				if (tasks.get(i).getTaskID() == rescheduledTask.getTaskID()) {
					tasks.set(i, rescheduledTask);
				}
			}
		}

		// sort list
		Collections.sort(tasks);
	}
}
