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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

import equinox.data.EquinoxTheme;
import equinox.exchangeServer.remote.message.ShareFile;
import equinox.plugin.FileType;
import equinox.serverUtilities.SharedFileInfo;
import equinox.serverUtilities.SharedFileInfo.SharedFileInfoType;
import equinox.task.DownloadSharedFile;
import equinox.task.DownloadSharedInstructionSet;
import equinox.task.DownloadSharedSpectrum;
import equinox.task.DownloadSharedView;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

/**
 * Class for share view chat message panel.
 *
 * @author Murat Artim
 * @date Sep 23, 2014
 * @time 11:09:04 AM
 */
public class ShareFileChatMessagePanel implements Initializable {

	/** The owner chat panel. */
	private ChatPopup owner_;

	/** Message. */
	private ShareFile message_;

	@FXML
	private VBox root_, baloon_;

	@FXML
	private Label id_, time_, fileName_, size_;

	@FXML
	private Button open_, run_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onRunClicked() {

		// get file type
		SharedFileInfo info = message_.getSharedFileInfo();
		int fileType = (int) info.getInfo(SharedFileInfoType.FILE_TYPE);

		// instruction set
		if (fileType == SharedFileInfo.INSTRUCTION_SET) {

			// get task manager
			ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();

			// download and run instruction set
			tm.runTaskInParallel(new DownloadSharedInstructionSet(info, null, true));

			// disable button
			run_.setDisable(true);
		}
	}

	@FXML
	private void onOpenClicked() {

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();

		// get file type
		SharedFileInfo info = message_.getSharedFileInfo();
		int fileType = (int) info.getInfo(SharedFileInfoType.FILE_TYPE);

		// view
		if (fileType == SharedFileInfo.VIEW) {
			tm.runTaskInParallel(new DownloadSharedView(info));
		}

		// file or workspace
		else if (fileType == SharedFileInfo.FILE || fileType == SharedFileInfo.WORKSPACE) {

			// get file chooser
			FileChooser fileChooser = owner_.getOwner().getOwner().getFileChooser(FileType.ZIP.getExtensionFilter());

			// show save dialog
			fileChooser.setInitialFileName(FileType.appendExtension((String) info.getInfo(SharedFileInfoType.FILE_NAME), FileType.ZIP));
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.getOwner().getOwner().setInitialDirectory(selectedFile);

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
			FileChooser fileChooser = owner_.getOwner().getOwner().getFileChooser(FileType.ZIP.getExtensionFilter());

			// show save dialog
			fileChooser.setInitialFileName(FileType.appendExtension((String) info.getInfo(SharedFileInfoType.FILE_NAME), FileType.ZIP));
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.getOwner().getOwner().setInitialDirectory(selectedFile);

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, FileType.ZIP);

			// add and start task
			tm.runTaskInParallel(new DownloadSharedInstructionSet(info, output.toPath(), false));
		}

		// disable button
		open_.setDisable(true);
	}

	/**
	 * Loads and returns chat message panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @param message
	 *            Message.
	 * @return Chat message panel.
	 */
	public static VBox load(ChatPopup owner, ShareFile message) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ShareFileChatMessagePanel.fxml"));
			fxmlLoader.load();

			// get controller
			ShareFileChatMessagePanel controller = (ShareFileChatMessagePanel) fxmlLoader.getController();

			// set attributes
			controller.owner_ = owner;
			controller.message_ = message;
			SharedFileInfo info = message.getSharedFileInfo();
			int fileType = (int) info.getInfo(SharedFileInfoType.FILE_TYPE);
			controller.run_.setVisible(fileType == SharedFileInfo.INSTRUCTION_SET);
			controller.fileName_.setText((String) info.getInfo(SharedFileInfoType.FILE_NAME));
			controller.fileName_.setTextFill(Color.BLACK);
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
			controller.id_.setText(text);
			controller.id_.setTextFill(Color.BLACK);
			DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US);
			controller.time_.setText(format.format(new Date()));
			controller.size_.setText("File Size: " + Utility.readableFileSize((long) info.getInfo(SharedFileInfoType.DATA_SIZE)));
			controller.baloon_.getStyleClass().add("chatBaloonReceived");
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
			controller.open_.setText(buttonText);

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
