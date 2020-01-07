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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;

import equinox.Equinox;
import equinox.data.Settings;
import equinox.data.ui.NotificationPanel;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.application.Platform;

/**
 * Class for save settings task.
 *
 * @author Murat Artim
 * @date Apr 30, 2014
 * @time 1:58:31 PM
 */
public class SaveSettings extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** Settings to save. */
	private final Settings settings_;

	/** True if application restart is required. */
	private final boolean restart_;

	/**
	 * Creates save settings task.
	 *
	 * @param settings
	 *            Settings to save.
	 * @param restart
	 *            True if application restart is required.
	 */
	public SaveSettings(Settings settings, boolean restart) {
		settings_ = settings;
		restart_ = restart;
	}

	@Override
	public String getTaskTitle() {
		return "Save settings";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {
		try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(Equinox.SETTINGS_FILE.toFile())))) {
			out.writeObject(settings_);
		}
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// show restart question
		if (restart_) {

			// set title and message
			String title = "Restart Equinox";
			String message = "Changes to settings will take effect after restart of Equinox. Do you want to restart Equinox now?";

			// show notification
			NotificationPanel np = taskPanel_.getOwner().getOwner().getNotificationPane();
			np.showQuestion(title, message, "Yes", "No", event -> {

				try {

					// hide notification
					np.hide();

					// cancel all tasks
					taskPanel_.getOwner().getOwner().getActiveTasksPanel().cancelAllTasks();

					// restart container
					taskPanel_.getOwner().getOwner().getOwner().restartContainer();

					// exit
					Platform.exit();
				}

				// exception occurred
				catch (Exception e) {

					// log exception
					Equinox.LOGGER.log(Level.WARNING, "Exception occured during restarting Equinox.", e);

					// create and show notification
					String message1 = "Exception occured during restarting Equinox: " + e.getLocalizedMessage();
					message1 += " Click 'Details' for more information.";
					np.showError("Problem encountered", message1, e);
				}
			}, event -> np.hide());
		}
	}
}
