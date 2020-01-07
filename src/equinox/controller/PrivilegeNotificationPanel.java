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
package equinox.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.apache.commons.text.WordUtils;

import equinox.data.EquinoxTheme;
import equinox.serverUtilities.Permission;
import equinox.task.SubmitAccessRequest;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Class for user privilege notification panel controller.
 *
 * @author Murat Artim
 * @date 4 Aug 2016
 * @time 14:19:37
 */
public class PrivilegeNotificationPanel implements Initializable {

	/** The owner panel. */
	private MainScreen mainScreen_;

	/** The denied permission. */
	private Permission permission_;

	@FXML
	private VBox root_;

	@FXML
	private Label message_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onButtonClicked() {
		mainScreen_.getActiveTasksPanel().runTaskInParallel(new SubmitAccessRequest(permission_));
		mainScreen_.getNotificationPane().hide();
	}

	/**
	 * Loads and returns shared file notification panel.
	 *
	 * @param message
	 *            Warning message.
	 * @param permission
	 *            The denied permission.
	 * @param mainScreen
	 *            The owner main screen.
	 * @return The newly loaded shared file panel.
	 */
	public static VBox load(String message, Permission permission, MainScreen mainScreen) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("PrivilegeNotificationPanel.fxml"));
			fxmlLoader.load();

			// get controller
			PrivilegeNotificationPanel controller = (PrivilegeNotificationPanel) fxmlLoader.getController();

			// set attributes
			// DEPRECATION using org.apache.commons.text.WordUtils instead of org.apache.commons.lang3.text.WordUtils
			controller.message_.setText(WordUtils.wrap(message, 110));
			controller.mainScreen_ = mainScreen;
			controller.permission_ = permission;

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
}
