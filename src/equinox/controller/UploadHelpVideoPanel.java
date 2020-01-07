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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.EquinoxTheme;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.SaveTask;
import equinox.task.UploadHelpVideo;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for upload help video panel controller.
 *
 * @author Murat Artim
 * @date Mar 25, 2016
 * @time 1:48:22 PM
 */
public class UploadHelpVideoPanel implements InternalInputSubPanel, SchedulingPanel {

	/** Maximum description length. */
	private static final int MAX_DESCRIPTION_LENGTH = 500;

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_, container_;

	@FXML
	private BorderPane dropZone_;

	@FXML
	private ImageView dropImage_, mov_;

	@FXML
	private Hyperlink browse_;

	@FXML
	private TextField name_, duration_;

	@FXML
	private TextArea description_;

	@FXML
	private Accordion accordion_;

	@FXML
	private SplitMenuButton ok_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {
		onResetClicked();
	}

	@Override
	public String getHeader() {
		return "Upload Help Video";
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// get inputs
		String name = name_.getText();
		String duration = duration_.getText();
		String description = description_.getText();
		Path movFile = (Path) mov_.getUserData();

		// check inputs
		if (!checkInputs(name, description, movFile, duration))
			return;

		// create and submit task
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new UploadHelpVideo(name, duration, description, movFile));
		}
		else {
			tm.runTaskInParallel(new SaveTask(new UploadHelpVideo(name, duration, description, movFile), scheduleDate));
		}

		// return file view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Processes selected file.
	 *
	 * @param file
	 *            Selected file.
	 * @return True if process completed successfully.
	 */
	public boolean processFile(File file) {

		// check file types
		ArrayList<ImageView> toBeAnimated = new ArrayList<>();
		boolean success = false;

		// get file type
		FileType fileType = FileType.getFileType(file);

		// not recognized
		if (fileType == null)
			return success;

		// MOV
		if (fileType.equals(FileType.MOV)) {
			owner_.getOwner().setInitialDirectory(file);
			mov_.setUserData(file.toPath());
			if (!toBeAnimated.contains(mov_)) {
				toBeAnimated.add(mov_);
			}
			success = true;
		}

		// animate file types
		if (success && !toBeAnimated.isEmpty()) {
			Animator.bouncingScale(0.0, 100.0, 1.0, 1.5, 1.0, new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					for (ImageView item : toBeAnimated) {
						item.setImage(Utility.getImage("full.png"));
					}
				}
			}, toBeAnimated).play();
		}

		// return
		return success;
	}

	@FXML
	private void onDragOver(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if ((event.getGestureSource() != dropZone_) && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.MOV)) {
					event.acceptTransferModes(TransferMode.ANY);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragEntered(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if ((event.getGestureSource() != dropZone_) && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.MOV)) {
					dropImage_.setImage(AddSpectrumPanel.DROP_ZONE_ACTIVE);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragExited(DragEvent event) {
		dropImage_.setImage(AddSpectrumPanel.DROP_ZONE);
		event.consume();
	}

	@FXML
	private void onDragDropped(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		boolean success = false;
		if ((event.getGestureSource() != dropZone_) && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.MOV)) {
					success = processFile(file);
					break;
				}
			}
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}

	@FXML
	private void onBrowseClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.MOV.getExtensionFilter());

		// show open dialog
		File file = fileChooser.showOpenDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if ((file == null) || !file.exists())
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(file);

		// process file
		processFile(file);
	}

	@FXML
	private void onOKClicked() {
		setTaskScheduleDate(true, null);
	}

	@FXML
	private void onSaveTaskClicked() {
		setTaskScheduleDate(false, null);
	}

	@FXML
	private void onScheduleTaskClicked() {
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
		popOver.setDetachable(false);
		popOver.setContentNode(ScheduleTaskPanel.load(popOver, this, null));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(ok_);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {

		// reset info
		name_.clear();
		duration_.clear();
		description_.clear();

		// reset data
		browse_.setVisited(false);
		mov_.setUserData(null);
		mov_.setImage(AddSpectrumPanel.EMPTY);

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	/**
	 * Checks inputs.
	 *
	 * @param name
	 *            Help video reference name.
	 * @param description
	 *            Help video description.
	 * @param movFile
	 *            Help video movie file.
	 * @param duration
	 *            Help video duration.
	 * @return True if inputs are valid.
	 */
	private boolean checkInputs(String name, String description, Path movFile, String duration) {

		// invalid name
		if ((name == null) || name.isEmpty()) {
			String message = "Please enter help video name to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(name_);
			return false;
		}

		// invalid duration
		if ((duration == null) || duration.isEmpty()) {
			String message = "Please enter help video duration to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(duration_);
			return false;
		}

		// invalid description
		if ((description == null) || description.isEmpty()) {
			String message = "Please help video description to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(description_);
			return false;
		}

		// check description length
		if (description.length() > MAX_DESCRIPTION_LENGTH) {
			String message = "Help video description exceeds maximum allowed number of characters (" + MAX_DESCRIPTION_LENGTH + ").";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(description_);
			return false;
		}

		// check movie file
		if ((movFile == null) || !Files.exists(movFile)) {
			String message = "Please supply help video movie file to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(mov_);
			return false;
		}

		// valid inputs
		return true;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static UploadHelpVideoPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("UploadHelpVideoPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			UploadHelpVideoPanel controller = (UploadHelpVideoPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
