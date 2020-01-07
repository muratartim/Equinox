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
import org.controlsfx.control.PopOver;

import equinox.data.EquinoxTheme;
import equinox.utility.Utility;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Class for confirmation panel controller.
 *
 * @author Murat Artim
 * @date Sep 12, 2014
 * @time 12:43:54 PM
 */
public class NotificationPanel2 implements Initializable {

	/** Notification type. */
	public static final Image QUESTION = Utility.getImage("question.png"), UPDATES = Utility.getImage("notifUpdates.png");

	/** The owner pop-over. */
	private PopOver popOver_;

	@FXML
	private VBox root_;

	@FXML
	private Label label_;

	@FXML
	private Button ok_;

	@FXML
	private ImageView image_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param popOver
	 *            The owner pop-over.
	 * @param message
	 *            Message text.
	 * @param wrapLength
	 *            Wrapping length.
	 * @param okText
	 *            Ok button text.
	 * @param okHandler
	 *            Ok button handler.
	 * @param image
	 *            Image of notification.
	 * @return The newly loaded file CDF set panel.
	 */
	public static VBox load(PopOver popOver, String message, int wrapLength, String okText, EventHandler<ActionEvent> okHandler, Image image) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("NotificationPanel2.fxml"));
			fxmlLoader.load();

			// get controller
			NotificationPanel2 controller = (NotificationPanel2) fxmlLoader.getController();

			// set attributes
			controller.popOver_ = popOver;
			// DEPRECATION using org.apache.commons.text.WordUtils instead of org.apache.commons.lang3.text.WordUtils
			controller.label_.setText(WordUtils.wrap(message, wrapLength));
			controller.label_.setTextFill(Color.BLACK);
			controller.ok_.setText(okText);
			controller.ok_.setOnAction(okHandler);
			controller.image_.setFitHeight(image.getHeight());
			controller.image_.setFitWidth(image.getWidth());
			controller.image_.setImage(image);

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
