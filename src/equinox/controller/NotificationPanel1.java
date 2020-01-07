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
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Class for warning panel controller.
 *
 * @author Murat Artim
 * @date Sep 12, 2014
 * @time 10:19:23 AM
 */
public class NotificationPanel1 implements Initializable {

	/** Notification type. */
	public static final Image WARNING = Utility.getImage("warning.png"), INFO = Utility.getImage("infoBig.png"), THUMB = Utility.getImage("likeBig.png");

	@FXML
	private VBox root_;

	@FXML
	private Label label_;

	@FXML
	private ImageView image_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param text
	 *            Message text.
	 * @param wrapLength
	 *            Wrapping length.
	 * @param image
	 *            Image of notification.
	 * @return The newly loaded file CDF set panel.
	 */
	public static VBox load(String text, int wrapLength, Image image) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("NotificationPanel1.fxml"));
			fxmlLoader.load();

			// get controller
			NotificationPanel1 controller = (NotificationPanel1) fxmlLoader.getController();

			// set attributes
			// DEPRECATION using org.apache.commons.text.WordUtils instead of org.apache.commons.lang3.text.WordUtils
			controller.label_.setText(WordUtils.wrap(text, wrapLength));
			controller.label_.setTextFill(Color.BLACK);
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
