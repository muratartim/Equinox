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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import equinox.data.EquinoxTheme;
import equinox.data.ui.Notification;
import equinox.exchangeServer.remote.message.ShareFile;
import equinox.plugin.FileType;
import equinox.serverUtilities.SharedFileInfo;
import equinox.serverUtilities.SharedFileInfo.SharedFileInfoType;
import equinox.task.DownloadSharedFile;
import equinox.task.DownloadSharedInstructionSet;
import equinox.task.DownloadSharedSpectrum;
import equinox.task.DownloadSharedView;
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
import javafx.stage.FileChooser;

/**
 * Class for incoming file panel controller.
 *
 * @author Murat Artim
 * @date Dec 11, 2014
 * @time 11:19:42 AM
 */
public class IncomingFilePanel implements Initializable, UserProfileImageRequestingPanel {

	/** The owner main screen. */
	private MainScreen mainScreen_;

	/** Message. */
	private ShareFile message_;

	@FXML
	private VBox root_;

	@FXML
	private Label title_, fileName_, fileSize_;

	@FXML
	private Button button_, run_;

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
		int fileType = (int) info.getInfo(SharedFileInfoType.FILE_TYPE);
		String text = (String) info.getInfo(SharedFileInfoType.OWNER) + " shared ";
		if (fileType == SharedFileInfo.VIEW) {
			text += "view:";
		}
		else if (fileType == SharedFileInfo.FILE) {
			text += "file:";
		}
		else if (fileType == SharedFileInfo.WORKSPACE) {
			text += "workspace:";
		}
		else if (fileType == SharedFileInfo.SPECTRUM) {
			text += "spectrum:";
		}
		else if (fileType == SharedFileInfo.INSTRUCTION_SET) {
			text += "instruction set:";
		}
		text += "\n" + (String) info.getInfo(SharedFileInfoType.FILE_NAME);

		// show
		mainScreen_.getNotificationPane().show(new Notification(MessageType.NONE, text, -1, root_, false, false));
	}

	@FXML
	private void onRunClicked() {

		// get file type
		SharedFileInfo info = message_.getSharedFileInfo();
		int fileType = (int) info.getInfo(SharedFileInfoType.FILE_TYPE);

		// instruction set
		if (fileType == SharedFileInfo.INSTRUCTION_SET) {

			// get task manager
			ActiveTasksPanel tm = mainScreen_.getActiveTasksPanel();

			// download and run instruction set
			tm.runTaskInParallel(new DownloadSharedInstructionSet(info, null, true));

			// disable button
			run_.setDisable(true);
		}

		// hide
		mainScreen_.getNotificationPane().hide();
	}

	@FXML
	private void onButtonClicked() {

		// get task manager
		ActiveTasksPanel tm = mainScreen_.getActiveTasksPanel();

		// get file info
		SharedFileInfo info = message_.getSharedFileInfo();
		int fileType = (int) info.getInfo(SharedFileInfoType.FILE_TYPE);

		// view
		if (fileType == SharedFileInfo.VIEW) {
			tm.runTaskInParallel(new DownloadSharedView(info));
		}

		// file or workspace
		else if (fileType == SharedFileInfo.FILE || fileType == SharedFileInfo.WORKSPACE) {

			// get file chooser
			FileChooser fileChooser = mainScreen_.getFileChooser(FileType.ZIP.getExtensionFilter());

			// show save dialog
			fileChooser.setInitialFileName(FileType.appendExtension((String) info.getInfo(SharedFileInfoType.FILE_NAME), FileType.ZIP));
			File selectedFile = fileChooser.showSaveDialog(mainScreen_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			mainScreen_.setInitialDirectory(selectedFile);

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, FileType.ZIP);

			// add and start task
			tm.runTaskInParallel(new DownloadSharedFile(info, output.toPath()));
		}

		// spectrum
		else if (fileType == SharedFileInfo.SPECTRUM) {
			tm.runTaskInParallel(new DownloadSharedSpectrum(info));
		}

		// instruction set
		else if (fileType == SharedFileInfo.INSTRUCTION_SET) {

			// get file chooser
			FileChooser fileChooser = mainScreen_.getFileChooser(FileType.ZIP.getExtensionFilter());

			// show save dialog
			fileChooser.setInitialFileName(FileType.appendExtension((String) info.getInfo(SharedFileInfoType.FILE_NAME), FileType.ZIP));
			File selectedFile = fileChooser.showSaveDialog(mainScreen_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			mainScreen_.setInitialDirectory(selectedFile);

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, FileType.ZIP);

			// add and start task
			tm.runTaskInParallel(new DownloadSharedInstructionSet(info, output.toPath(), false));
		}

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
	public static VBox load(MainScreen mainScreen, ShareFile message) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("IncomingFilePanel.fxml"));
			fxmlLoader.load();

			// get controller
			IncomingFilePanel controller = (IncomingFilePanel) fxmlLoader.getController();

			// set attributes
			controller.mainScreen_ = mainScreen;
			controller.message_ = message;
			SharedFileInfo info = message.getSharedFileInfo();
			String title = (String) info.getInfo(SharedFileInfoType.OWNER) + " shared ";
			int fileType = (int) info.getInfo(SharedFileInfoType.FILE_TYPE);
			controller.run_.setVisible(fileType == SharedFileInfo.INSTRUCTION_SET);
			if (fileType == SharedFileInfo.VIEW) {
				title += "view";
			}
			else if (fileType == SharedFileInfo.FILE) {
				title += "file";
			}
			else if (fileType == SharedFileInfo.WORKSPACE) {
				title += "workspace";
			}
			else if (fileType == SharedFileInfo.SPECTRUM) {
				title += "spectrum";
			}
			else if (fileType == SharedFileInfo.INSTRUCTION_SET) {
				title += "instruction set";
			}
			controller.title_.setText(title);
			controller.fileName_.setText((String) info.getInfo(SharedFileInfoType.FILE_NAME));
			controller.fileSize_.setText("File Size: " + Utility.readableFileSize((long) info.getInfo(SharedFileInfoType.DATA_SIZE)));
			String buttonText = null;
			if (fileType == SharedFileInfo.VIEW) {
				buttonText = "Open";
			}
			else if (fileType == SharedFileInfo.FILE) {
				buttonText = "Save As...";
			}
			else if (fileType == SharedFileInfo.WORKSPACE) {
				buttonText = "Save As...";
			}
			else if (fileType == SharedFileInfo.SPECTRUM) {
				buttonText = "Load";
			}
			else if (fileType == SharedFileInfo.INSTRUCTION_SET) {
				buttonText = "Save As...";
			}
			controller.button_.setText(buttonText);

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
