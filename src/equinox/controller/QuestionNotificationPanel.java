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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Class for question notification panel controller.
 *
 * @author Murat Artim
 * @date Dec 12, 2014
 * @time 11:15:37 AM
 */
public class QuestionNotificationPanel implements Initializable {

	@FXML
	private VBox root_;

	@FXML
	private Label title_, text_;

	@FXML
	private Button yesButton_, noButton_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Loads question notification panel.
	 *
	 * @param title
	 *            Title of notification.
	 * @param text
	 *            Text of notification.
	 * @param yesButtonText
	 *            Yes button text.
	 * @param noButtonText
	 *            No button text.
	 * @param yesButtonAction
	 *            Yes button action.
	 * @param noButtonAction
	 *            No button action.
	 * @return The newly loaded panel root.
	 */
	public static VBox load(String title, String text, String yesButtonText, String noButtonText, EventHandler<ActionEvent> yesButtonAction, EventHandler<ActionEvent> noButtonAction) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("QuestionNotificationPanel.fxml"));
			fxmlLoader.load();

			// get controller
			QuestionNotificationPanel controller = (QuestionNotificationPanel) fxmlLoader.getController();

			// set attributes
			controller.title_.setText(title);
			// DEPRECATION using org.apache.commons.text.WordUtils instead of org.apache.commons.lang3.text.WordUtils
			controller.text_.setText(WordUtils.wrap(text, 100));
			controller.yesButton_.setText(yesButtonText);
			controller.noButton_.setText(noButtonText);
			controller.yesButton_.setOnAction(yesButtonAction);
			controller.noButton_.setOnAction(noButtonAction);

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
}
