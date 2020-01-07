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

import equinox.Equinox;
import equinox.data.EquinoxTheme;
import equinox.data.ui.Notification;
import equinox.exchangeServer.remote.message.InstructionSetRunRequest;
import equinox.exchangeServer.remote.message.InstructionSetRunResponse;
import equinox.serverUtilities.SharedFileInfo;
import equinox.serverUtilities.SharedFileInfo.SharedFileInfoType;
import equinox.task.DownloadSharedInstructionSet;
import equinox.task.GetUserProfileImage;
import equinox.task.GetUserProfileImage.UserProfileImageRequestingPanel;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

/**
 * Class for incoming run request panel controller.
 *
 * @author Murat Artim
 * @date 24 Sep 2018
 * @time 13:34:17
 */
public class IncomingRunRequestPanel implements Initializable, UserProfileImageRequestingPanel {

	/** The owner main screen. */
	private MainScreen mainScreen_;

	/** Message. */
	private InstructionSetRunRequest message_;

	@FXML
	private VBox root_;

	@FXML
	private Label title_, fileName_;

	@FXML
	private Button accept_;

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

		// set message text
		SharedFileInfo info = message_.getSharedFileInfo();
		String text = (String) info.getInfo(SharedFileInfoType.OWNER) + " requests to run following instruction set: ";
		text += "\n" + (String) info.getInfo(SharedFileInfoType.FILE_NAME);

		// show
		mainScreen_.getNotificationPane().show(new Notification(MessageType.NONE, text, -1, root_, false, false));
	}

	@FXML
	private void onAcceptClicked() {

		// get file type
		SharedFileInfo info = message_.getSharedFileInfo();

		// get task manager
		ActiveTasksPanel tm = mainScreen_.getActiveTasksPanel();

		// download and run instruction set
		tm.runTaskInParallel(new DownloadSharedInstructionSet(info, null, true));

		// disable button
		accept_.setDisable(true);

		// send acceptance message to sender
		InstructionSetRunResponse response = new InstructionSetRunResponse();
		response.setAccepted(true);
		response.setSender(Equinox.USER.createExchangeUser());
		response.setRecipient(message_.getSender());

		// send message
		mainScreen_.getExchangeServerManager().sendMessage(response);

		// hide
		mainScreen_.getNotificationPane().hide();
	}

	@FXML
	private void onRejectClicked() {

		// send rejection message to sender
		InstructionSetRunResponse response = new InstructionSetRunResponse();
		response.setAccepted(false);
		response.setSender(Equinox.USER.createExchangeUser());
		response.setRecipient(message_.getSender());

		// send message
		mainScreen_.getExchangeServerManager().sendMessage(response);

		// hide
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
	public static VBox load(MainScreen mainScreen, InstructionSetRunRequest message) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("IncomingRunRequestPanel.fxml"));
			fxmlLoader.load();

			// get controller
			IncomingRunRequestPanel controller = (IncomingRunRequestPanel) fxmlLoader.getController();

			// set attributes
			controller.mainScreen_ = mainScreen;
			controller.message_ = message;
			SharedFileInfo info = message.getSharedFileInfo();
			String title = (String) info.getInfo(SharedFileInfoType.OWNER) + " requests to run following instruction set:";
			controller.title_.setText(title);
			controller.fileName_.setText((String) info.getInfo(SharedFileInfoType.FILE_NAME));

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