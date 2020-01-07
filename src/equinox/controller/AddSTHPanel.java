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
import java.util.List;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.EquinoxTheme;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.AddStressSequence;
import equinox.task.SaveTask;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for add STH panel controller.
 *
 * @author Murat Artim
 * @date Mar 12, 2015
 * @time 10:30:06 AM
 */
public class AddSTHPanel implements InternalInputSubPanel, SchedulingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private BorderPane dropZone_;

	@FXML
	private ImageView dropImage_, sth_, fls_;

	@FXML
	private Hyperlink browse_;

	@FXML
	private SplitMenuButton ok_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
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
		return "Add New Stress Sequence";
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// get files
		@SuppressWarnings("unchecked")
		List<File> sthFiles = (List<File>) sth_.getUserData();
		Path flsFile = (Path) fls_.getUserData();

		// check inputs
		if (!checkInputs(sthFiles, flsFile))
			return;

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run tasks
		for (File sthFile : sthFiles) {

			// run now
			if (runNow) {
				tm.runTaskInParallel(new AddStressSequence(sthFile.toPath(), flsFile));
			}
			else {
				tm.runTaskInParallel(new SaveTask(new AddStressSequence(sthFile.toPath(), flsFile), scheduleDate));
			}
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
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
				if (fileType.equals(FileType.STH) || fileType.equals(FileType.FLS)) {
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
				if (fileType.equals(FileType.STH) || fileType.equals(FileType.FLS)) {
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

			// process files
			success = processFiles(db.getFiles());
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}

	/**
	 * Processes selected files.
	 *
	 * @param files
	 *            Selected files.
	 * @return True if process completed successfully.
	 */
	public boolean processFiles(List<File> files) {

		// check file types
		ArrayList<ImageView> toBeAnimated = new ArrayList<>();
		boolean success = false;
		for (File file : files) {

			// get file type
			FileType fileType = FileType.getFileType(file);

			// not recognized
			if (fileType == null) {
				continue;
			}

			// FLS
			if (fileType.equals(FileType.FLS)) {
				owner_.getOwner().setInitialDirectory(file);
				fls_.setUserData(file.toPath());
				if (!toBeAnimated.contains(fls_)) {
					toBeAnimated.add(fls_);
				}
				success = true;
			}

			// STH
			else if (fileType.equals(FileType.STH)) {
				owner_.getOwner().setInitialDirectory(file);
				@SuppressWarnings("unchecked")
				List<File> sthFiles = (List<File>) sth_.getUserData();
				if (sthFiles == null) {
					sthFiles = new ArrayList<>();
					sthFiles.add(file);
					sth_.setUserData(sthFiles);
					if (!toBeAnimated.contains(sth_)) {
						toBeAnimated.add(sth_);
					}
				}
				else {
					sthFiles.add(file);
				}
				success = true;
			}
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
	private void onBrowseClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.getCustomFileFilter("Input files", FileType.STH, FileType.FLS));

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if ((files == null) || files.isEmpty())
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(files.get(0));

		// process files
		processFiles(files);
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

		// clear all files
		ImageView[] items = { sth_, fls_ };
		for (ImageView item : items) {
			item.setUserData(null);
			item.setImage(AddSpectrumPanel.EMPTY);
		}

		// reset hyperlink
		browse_.setVisited(false);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to add new stress sequence", null);
	}

	@FXML
	private void onDownloadSampleInputClicked() {
		owner_.getOwner().downloadSampleInput("AddNewStressSequenceFromSTH");
	}

	/**
	 * Check inputs and shows warning if necessary.
	 *
	 * @param sthFiles
	 *            STH files.
	 * @param flsFile
	 *            FLS file.
	 * @return True if the inputs are correct.
	 */
	private boolean checkInputs(List<File> sthFiles, Path flsFile) {

		// check inputs
		String message = null;
		Node node = null;

		// check files
		if ((sthFiles == null) || sthFiles.isEmpty()) {
			message = "Please select at least 1 STH file to proceed.";
			node = sth_;
		}
		else if (!sthFiles.isEmpty()) {
			for (File file : sthFiles) {
				if (!file.exists()) {
					message = "STH file'" + file.getName() + "' doesn't exist.";
					message += " Please select valid STH files to proceed.";
					node = sth_;
					break;
				}
			}
		}
		else if ((flsFile == null) || !Files.exists(flsFile)) {
			message = "Please select a valid FLS file to proceed.";
			node = fls_;
		}

		// all valid inputs
		if (message == null)
			return true;

		// show warning
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(node);
		return false;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static AddSTHPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("AddSTHPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			AddSTHPanel controller = (AddSTHPanel) fxmlLoader.getController();

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
