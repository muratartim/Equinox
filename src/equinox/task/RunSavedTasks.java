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
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.ActiveTasksPanel;
import equinox.data.ui.SavedTaskItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

/**
 * Class for run saved tasks task.
 *
 * @author Murat Artim
 * @date Oct 9, 2015
 * @time 1:40:49 PM
 */
public class RunSavedTasks extends InternalEquinoxTask<ArrayList<SerializableTask>> implements ShortRunningTask {

	/** Tasks to run. */
	private final SavedTaskItem[] tasks_;

	/** True if tasks are scheduled tasks. */
	private final boolean isScheduled_, runSequentially_;

	/**
	 * Creates run saved tasks task.
	 *
	 * @param tasks
	 *            Tasks to run.
	 * @param isScheduled
	 *            True if tasks are scheduled tasks.
	 * @param runSequentially
	 *            True if the tasks should be run sequentially.
	 */
	public RunSavedTasks(ObservableList<SavedTaskItem> tasks, boolean isScheduled, boolean runSequentially) {
		tasks_ = new SavedTaskItem[tasks.size()];
		for (int i = 0; i < tasks.size(); i++) {
			tasks_[i] = tasks.get(i);
		}
		isScheduled_ = isScheduled;
		runSequentially_ = runSequentially;
	}

	/**
	 * Creates run saved tasks task.
	 *
	 * @param tasks
	 *            Tasks to run.
	 * @param isScheduled
	 *            True if tasks are scheduled tasks.
	 * @param runSequentially
	 *            True if the tasks should be run sequentially.
	 */
	public RunSavedTasks(ArrayList<SavedTaskItem> tasks, boolean isScheduled, boolean runSequentially) {
		tasks_ = new SavedTaskItem[tasks.size()];
		for (int i = 0; i < tasks.size(); i++) {
			tasks_[i] = tasks.get(i);
		}
		isScheduled_ = isScheduled;
		runSequentially_ = runSequentially;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Run " + (isScheduled_ ? "scheduled" : "saved") + " tasks";
	}

	@Override
	protected ArrayList<SerializableTask> call() throws Exception {

		// update progress info
		updateTitle("Running " + (isScheduled_ ? "scheduled" : "saved") + " tasks");

		// create list to store serializable tasks
		ArrayList<SerializableTask> serializableTasks = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement
			String sql = "select data from saved_tasks where task_id = ?";
			try (PreparedStatement statement = connection.prepareStatement(sql)) {

				// loop over tasks
				for (SavedTaskItem task : tasks_) {

					// get serializable task
					statement.setInt(1, task.getTaskID());
					try (ResultSet resultSet = statement.executeQuery()) {
						while (resultSet.next()) {
							Blob blob = resultSet.getBlob("data");
							byte[] bytes = blob.getBytes(1L, (int) blob.length());
							blob.free();
							try (ByteArrayInputStream bos = new ByteArrayInputStream(bytes)) {
								try (ObjectInputStream ois = new ObjectInputStream(bos)) {
									serializableTasks.add((SerializableTask) ois.readObject());
								}
							}
						}
					}
				}
			}
		}

		// return serializable tasks
		return serializableTasks;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get task manager
			ActiveTasksPanel tm = taskPanel_.getOwner();

			// get file tree root
			TreeItem<String> root = tm.getOwner().getInputPanel().getFileTreeRoot();

			// get tasks
			ArrayList<SerializableTask> tasks = get();

			// run tasks in parallel
			if (!runSequentially_) {

				// loop over tasks
				for (SerializableTask task : tasks) {

					// get task
					InternalEquinoxTask<?> internalTask = (InternalEquinoxTask<?>) task.getTask(root);

					// bucket task
					if (internalTask instanceof BucketTask) {
						tm.runTaskSequentially(internalTask);
					}
					else {
						tm.runTaskInParallel(internalTask);
					}
				}
			}

			// run sequentially
			else {
				for (SerializableTask task : tasks) {
					tm.runTaskSequentially((InternalEquinoxTask<?>) task.getTask(root));
				}
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
