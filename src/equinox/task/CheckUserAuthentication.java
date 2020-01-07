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

import java.util.TimerTask;

import equinox.controller.InputPanel;
import equinox.data.UserAuthentication;
import javafx.application.Platform;

/**
 * Class for check user authentication task.
 *
 * @author Murat Artim
 * @date 2 Jul 2018
 * @time 13:50:35
 */
public class CheckUserAuthentication extends TimerTask {

	/** Input panel. */
	private final InputPanel owner;

	/**
	 * Creates check user authentication task.
	 *
	 * @param owner
	 *            The owner panel.
	 */
	public CheckUserAuthentication(InputPanel owner) {
		this.owner = owner;
	}

	@Override
	public void run() {

		try {

			// run on JavaFX thread
			Platform.runLater(() -> {

				// get user authentication
				UserAuthentication userAuth = (UserAuthentication) owner.getAuthenticationButton().getUserData();

				// notify UI
				owner.authenticationStatusChanged(userAuth == null ? true : userAuth.isExpired());
			});
		}

		// exception occurred
		catch (Exception e) {
			// ignore
		}
	}
}