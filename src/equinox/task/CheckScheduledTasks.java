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

import java.util.ArrayList;
import java.util.Date;
import java.util.TimerTask;

import equinox.controller.ActiveTasksPanel;
import equinox.data.ui.SavedTaskItem;
import javafx.application.Platform;

/**
 * Class for check scheduled tasks timer-task.
 *
 * @author Murat Artim
 * @date Oct 10, 2015
 * @time 2:52:54 PM
 */
public class CheckScheduledTasks extends TimerTask {

	/** Task manager panel. */
	private final ActiveTasksPanel tm_;

	/**
	 * Creates check scheduled tasks task.
	 *
	 * @param tm
	 *            Task manager panel.
	 */
	public CheckScheduledTasks(ActiveTasksPanel tm) {
		tm_ = tm;
	}

	@Override
	public void run() {

		// initialize array to store tasks to be executed
		ArrayList<SavedTaskItem> toBeExecuted = null;

		// create now
		Date now = new Date();

		// loop over all scheduled tasks
		for (SavedTaskItem task : tm_.getOwner().getScheduledTasksPanel().getScheduledTasks().getItems()) {

			// should be executed
			if (task.getDate().before(now)) {

				// create list of tasks (if not already done so)
				if (toBeExecuted == null)
					toBeExecuted = new ArrayList<>();

				// add task to to-be-executed list
				toBeExecuted.add(task);
			}
		}

		// no tasks to be executed
		if (toBeExecuted == null || toBeExecuted.isEmpty())
			return;

		// run tasks
		runTasks(toBeExecuted);
	}

	/**
	 * Runs tasks in JavaFX thread.
	 *
	 * @param toBeExecuted
	 *            Tasks to be executed.
	 */
	private void runTasks(ArrayList<SavedTaskItem> toBeExecuted) {

		// run in JavaFX thread
		Platform.runLater(new Runnable() {

			@Override
			public void run() {

				// run tasks
				RunSavedTasks run = new RunSavedTasks(toBeExecuted, true, false);
				DeleteSavedTasks delete = new DeleteSavedTasks(toBeExecuted, true);
				tm_.runTasksSequentially(run, delete);
			}
		});
	}
}
