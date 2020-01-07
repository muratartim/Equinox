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

import java.awt.Desktop.Action;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.logging.Level;

import org.apache.commons.text.WordUtils;

import equinox.Equinox;
import equinox.data.EquinoxTheme;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Class for plugin task ok notification panel controller.
 *
 * @author Murat Artim
 * @date Mar 16, 2015
 * @time 11:08:43 AM
 */
public class DirectoryOutputtingOkNotificationPanel implements Initializable {

	/** The owner main screen. */
	private MainScreen mainScreen_;

	/** Output directory of the plugin task. */
	private Path outputDirectory_;

	@FXML
	private VBox root_;

	@FXML
	private Label title_, text_;

	@FXML
	private Button button_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onButtonClicked() {

		// hide notification
		mainScreen_.getNotificationPane().hide();

		// open file in default editor
		try {

			// desktop is not supported
			if (!java.awt.Desktop.isDesktopSupported()) {
				String message = "Cannot open file explorer. Desktop class is not supported.";
				mainScreen_.getNotificationPane().showWarning(message, null);
				return;
			}

			// open action is not supported
			if (!java.awt.Desktop.getDesktop().isSupported(Action.OPEN)) {
				String message = "Cannot open file explorer. Open action is not supported.";
				mainScreen_.getNotificationPane().showWarning(message, null);
				return;
			}

			// open directory in explorer
			java.awt.Desktop.getDesktop().open(outputDirectory_.toFile());
		}

		// exception occurred
		catch (IOException e) {
			String msg = "Exception occurred during opening plugin output directory: ";
			Equinox.LOGGER.log(Level.WARNING, msg, e);
			msg += e.getLocalizedMessage();
			msg += " Click 'Details' for more information.";
			mainScreen_.getNotificationPane().showError("Problem encountered", msg, e);
		}
	}

	/**
	 * Loads and returns shared file notification panel.
	 *
	 * @param mainScreen
	 *            Main screen.
	 * @param title
	 *            Title of notification.
	 * @param message
	 *            Message.
	 * @param buttonText
	 *            Button text.
	 * @param outputDirectory
	 *            Output directory of the plugin task.
	 * @return The newly loaded shared file panel.
	 */
	public static VBox load(MainScreen mainScreen, String title, String message, String buttonText, Path outputDirectory) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DirectoryOutputtingOkNotificationPanel.fxml"));
			fxmlLoader.load();

			// get controller
			DirectoryOutputtingOkNotificationPanel controller = (DirectoryOutputtingOkNotificationPanel) fxmlLoader.getController();

			// set attributes
			controller.mainScreen_ = mainScreen;
			controller.outputDirectory_ = outputDirectory;
			controller.title_.setText(title);
			// DEPRECATION using org.apache.commons.text.WordUtils instead of org.apache.commons.lang3.text.WordUtils
			controller.text_.setText(WordUtils.wrap(message, 130));
			controller.button_.setText(buttonText);

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
}
