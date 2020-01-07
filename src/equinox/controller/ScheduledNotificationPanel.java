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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Class for scheduled notification panel controller.
 *
 * @author Murat Artim
 * @date Dec 12, 2014
 * @time 9:31:45 AM
 */
public class ScheduledNotificationPanel implements Initializable {

	/** The owner main screen. */
	private MainScreen mainScreen_;

	@FXML
	private VBox root_;

	@FXML
	private Label text_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onButtonClicked() {
		mainScreen_.getMenuBarPanel().showScheduledTasks();
		mainScreen_.getNotificationPane().hide();
	}

	/**
	 * Loads and returns shared file notification panel.
	 *
	 * @param mainScreen
	 *            Main screen.
	 * @param message
	 *            Message.
	 * @return The newly loaded shared file panel.
	 */
	public static VBox load(MainScreen mainScreen, String message) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ScheduledNotificationPanel.fxml"));
			fxmlLoader.load();

			// get controller
			ScheduledNotificationPanel controller = (ScheduledNotificationPanel) fxmlLoader.getController();

			// set attributes
			controller.mainScreen_ = mainScreen;
			// DEPRECATION using org.apache.commons.text.WordUtils instead of org.apache.commons.lang3.text.WordUtils
			controller.text_.setText(WordUtils.wrap(message, 100));

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
}
