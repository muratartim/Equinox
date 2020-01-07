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
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import equinox.Equinox;
import equinox.controller.IntroPanel;
import equinox.data.fileType.SpectrumItem;
import equinox.data.ui.SavedTaskItem;
import equinox.process.LoadAllFilesProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for load files task.
 *
 * @author Murat Artim
 * @date Dec 12, 2013
 * @time 5:18:57 PM
 */
public class LoadAllFiles extends InternalEquinoxTask<ArrayList<SpectrumItem>> implements ShortRunningTask {

	/** Intro panel. */
	private final IntroPanel introPanel_;

	/** Lists for saved and scheduled tasks. */
	private final ArrayList<SavedTaskItem> savedTasks_, scheduledTasks_;

	/**
	 * Creates load files at start up task.
	 *
	 * @param introPanel
	 *            Intro panel. Can be null, if not called at application startup.
	 */
	public LoadAllFiles(IntroPanel introPanel) {
		introPanel_ = introPanel;
		savedTasks_ = new ArrayList<>();
		scheduledTasks_ = new ArrayList<>();
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Loading workspace...";
	}

	@Override
	protected ArrayList<SpectrumItem> call() throws Exception {

		// update info
		updateMessage("Load files...");

		// initialize list
		ArrayList<SpectrumItem> files = null;

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// load files
			files = new LoadAllFilesProcess(this).start(connection);

			// load saved and scheduled tasks
			loadSavedAndScheduledTasks(connection);
		}

		// return list
		return files;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// add files to file root
			taskPanel_.getOwner().getOwner().getInputPanel().getFileTreeRoot().getChildren().addAll(get());

			// set saved and scheduled tasks
			taskPanel_.getOwner().getOwner().getSavedTasksPanel().getSavedTasks().getItems().setAll(savedTasks_);
			taskPanel_.getOwner().getOwner().getScheduledTasksPanel().getScheduledTasks().getItems().setAll(scheduledTasks_);

			// startup
			if (introPanel_ != null) {

				// notify intro panel
				introPanel_.hideIntro();

				// start scheduled thread pool
				CheckScheduledTasks check = new CheckScheduledTasks(introPanel_.getOwner().getActiveTasksPanel());
				((ScheduledExecutorService) Equinox.SCHEDULED_THREADPOOL).scheduleAtFixedRate(check, 30, 60, TimeUnit.SECONDS);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// notify intro panel
		if (introPanel_ != null) {
			introPanel_.hideIntro();
		}
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// notify intro panel
		if (introPanel_ != null) {
			introPanel_.hideIntro();
		}
	}

	/**
	 * Loads saved and scheduled tasks from the database.
	 *
	 * @param connection
	 *            database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadSavedAndScheduledTasks(Connection connection) throws Exception {

		// create statement
		try (Statement statement = connection.createStatement()) {

			// load saved tasks
			updateMessage("Loading saved tasks...");
			String sql = "select task_id, title from saved_tasks where schedule_date is null order by task_id";
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					int taskID = resultSet.getInt("task_id");
					String title = resultSet.getString("title");
					savedTasks_.add(new SavedTaskItem(taskID, title, null));
				}
			}

			// remove expired scheduled tasks
			updateMessage("Deleting expired scheduled tasks...");
			statement.executeUpdate("delete from saved_tasks where schedule_date is not null and schedule_date <= CURRENT_TIMESTAMP");

			// load scheduled tasks
			updateMessage("Loading scheduled tasks...");
			sql = "select task_id, schedule_date, title from saved_tasks where schedule_date is not null order by schedule_date";
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					int taskID = resultSet.getInt("task_id");
					Date date = new Date(resultSet.getTimestamp("schedule_date").getTime());
					String title = resultSet.getString("title");
					scheduledTasks_.add(new SavedTaskItem(taskID, title, date));
				}
			}
		}
	}
}
