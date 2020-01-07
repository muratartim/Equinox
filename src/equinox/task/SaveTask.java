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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.ui.SavedTaskItem;
import equinox.network.AutomationClientHandler;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomationTask;

/**
 * Class for save task task.
 *
 * @author Murat Artim
 * @date Oct 8, 2015
 * @time 12:02:19 PM
 */
public class SaveTask extends InternalEquinoxTask<SavedTaskItem> implements ShortRunningTask, AutomationTask {

	/** Serializable task. */
	private final SerializableTask serializableTask_;

	/** Schedule date. */
	private final Date scheduleDate_;

	/** Task title. */
	private final String title_;

	/** Request id. */
	private int requestId = -1;

	/** Automation client handler. */
	private AutomationClientHandler automationClientHandler = null;

	/**
	 * Creates save task task.
	 *
	 * @param task
	 *            Task to save.
	 * @param scheduleDate
	 *            Schedule date (can be null).
	 */
	public SaveTask(SavableTask task, Date scheduleDate) {
		serializableTask_ = task.getSerializableTask();
		scheduleDate_ = scheduleDate;
		title_ = task.getTaskTitle();
	}

	@Override
	public void setAutomationClientHandler(AutomationClientHandler automationClientHandler, int requestId) {
		this.automationClientHandler = automationClientHandler;
		this.requestId = requestId;
	}

	/**
	 * Returns true if task is scheduled.
	 *
	 * @return True if task is scheduled.
	 */
	public boolean isScheduled() {
		return scheduleDate_ != null;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save '" + title_ + "'";
	}

	@Override
	protected SavedTaskItem call() throws Exception {

		// update progress info
		updateTitle("Saving '" + title_ + "'");

		// initialize task
		SavedTaskItem task = null;

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// create statement
				String sql = "insert into saved_tasks(schedule_date, title, data) values(?, ?, ?)";
				try (PreparedStatement save = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

					// set schedule date
					if (scheduleDate_ == null) {
						save.setNull(1, java.sql.Types.TIMESTAMP);
					}
					else {
						save.setTimestamp(1, new Timestamp(scheduleDate_.getTime()));
					}

					// set task title
					save.setString(2, title_);

					// set task data
					try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
						try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
							oos.writeObject(serializableTask_);
							oos.flush();
							byte[] bytes = bos.toByteArray();
							try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
								save.setBinaryStream(3, bais, bytes.length);
							}
						}
					}

					// execute update
					save.executeUpdate();

					// get task ID
					try (ResultSet resultSet = save.getGeneratedKeys()) {
						while (resultSet.next()) {
							task = new SavedTaskItem(resultSet.getBigDecimal(1).intValue(), title_, scheduleDate_);
						}
					}
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return task ID
				return task;
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

		// add task to task panel
		try {

			// get task
			SavedTaskItem task = get();

			// saved task
			if (scheduleDate_ == null) {
				taskPanel_.getOwner().getOwner().getSavedTasksPanel().getSavedTasks().getItems().add(task);
			}
			else {
				taskPanel_.getOwner().getOwner().getScheduledTasksPanel().getScheduledTasks().getItems().add(task);
				Collections.sort(taskPanel_.getOwner().getOwner().getScheduledTasksPanel().getScheduledTasks().getItems());
			}

			// notify client handler of task completion (if set)
			if (automationClientHandler != null) {
				automationClientHandler.taskCompleted(requestId);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);

			// notify client handler of task failure (if set)
			if (automationClientHandler != null) {
				automationClientHandler.taskFailed(requestId);
			}
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// notify client handler of task failure (if set)
		if (automationClientHandler != null) {
			automationClientHandler.taskFailed(requestId);
		}
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// notify client handler of task failure (if set)
		if (automationClientHandler != null) {
			automationClientHandler.taskFailed(requestId);
		}
	}
}
