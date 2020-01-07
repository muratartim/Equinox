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

import java.awt.TrayIcon.MessageType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.apache.commons.text.WordUtils;

import equinox.data.EquinoxTheme;
import equinox.data.ui.Notification;
import equinox.exchangeServer.remote.message.ChatMessage;
import equinox.task.GetUserProfileImage;
import equinox.task.GetUserProfileImage.UserProfileImageRequestingPanel;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

/**
 * Chat notification panel controller.
 *
 * @author Murat Artim
 * @date Dec 11, 2014
 * @time 5:31:10 PM
 */
public class ChatNotificationPanel implements Initializable, UserProfileImageRequestingPanel {

	/** The owner main screen. */
	private MainScreen mainScreen_;

	/** Chat panel. */
	private ChatPopup chatPanel_;

	/** Chat message. */
	private ChatMessage message_;

	@FXML
	private VBox root_;

	@FXML
	private Label title_, text_;

	@FXML
	private Circle image_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		ImagePattern pattern = new ImagePattern(Utility.getImage("userBig.png"));
		image_.setFill(pattern);
	}

	@Override
	public void setUserProfileImage(byte[] imageBytes) {

		// set image
		if (imageBytes != null) {
			ImagePattern pattern = new ImagePattern(new Image(new ByteArrayInputStream(imageBytes)));
			image_.setFill(pattern);
		}

		// setup and show notification
		String text = message_.getSender().toString() + " says: " + message_.getMessage();
		mainScreen_.getNotificationPane().show(new Notification(MessageType.NONE, text, -1, root_, false, false));
	}

	@FXML
	private void onButtonClicked() {
		chatPanel_.show(message_.getSender());
		mainScreen_.getNotificationPane().hide();
	}

	/**
	 * Loads and returns shared file notification panel.
	 *
	 * @param mainScreen
	 *            Main screen.
	 * @param chatPanel
	 *            Chat panel.
	 * @param message
	 *            Message.
	 * @return The newly loaded shared file panel.
	 */
	public static VBox load(MainScreen mainScreen, ChatPopup chatPanel, ChatMessage message) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ChatNotificationPanel.fxml"));
			fxmlLoader.load();

			// get controller
			ChatNotificationPanel controller = (ChatNotificationPanel) fxmlLoader.getController();

			// set attributes
			controller.mainScreen_ = mainScreen;
			controller.message_ = message;
			controller.chatPanel_ = chatPanel;
			controller.title_.setText(message.getSender().toString() + " says:");
			controller.text_.setText(WordUtils.wrap(message.getMessage(), 100));

			// request sender profile image
			ActiveTasksPanel tm = controller.mainScreen_.getActiveTasksPanel();
			tm.runTaskSilently(new GetUserProfileImage(message.getSender(), controller), false);

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
}
