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
import equinox.dataServer.remote.data.AircraftModelInfo;
import equinox.dataServer.remote.data.AircraftModelInfo.AircraftModelInfoType;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.AddAircraftModel;
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
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for add A/C model panel controller.
 *
 * @author Murat Artim
 * @date Jul 3, 2015
 * @time 3:11:24 PM
 */
public class AddAircraftModelPanel implements InternalInputSubPanel, SchedulingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_, container_;

	@FXML
	private BorderPane dropZone_;

	@FXML
	private ImageView dropImage_, f07_, f06_, grp_;

	@FXML
	private Hyperlink browse_;

	@FXML
	private TextField program_, modelName_;

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
		return "Add New A/C Model";
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// get files
		Path f07File = (Path) f07_.getUserData();
		Path f06File = (Path) f06_.getUserData();
		Path grpFile = (Path) grp_.getUserData();

		// get A/C program and model name
		String program = program_.getText();
		String modelName = modelName_.getText();

		// check inputs
		if (!checkInputs(f07File, f06File, grpFile, program, modelName))
			return;

		// create A/C model info
		AircraftModelInfo info = new AircraftModelInfo();
		info.setInfo(AircraftModelInfoType.AC_PROGRAM, program);
		info.setInfo(AircraftModelInfoType.MODEL_NAME, modelName);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new AddAircraftModel(info, f06File, f07File, grpFile));
		}
		else {
			tm.runTaskInParallel(new SaveTask(new AddAircraftModel(info, f06File, f07File, grpFile), scheduleDate));
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
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

			// F07
			if (fileType.equals(FileType.F07)) {
				owner_.getOwner().setInitialDirectory(file);
				f07_.setUserData(file.toPath());
				if (!toBeAnimated.contains(f07_)) {
					toBeAnimated.add(f07_);
				}
				success = true;
			}

			// F06
			else if (fileType.equals(FileType.F06)) {
				owner_.getOwner().setInitialDirectory(file);
				f06_.setUserData(file.toPath());
				if (!toBeAnimated.contains(f06_)) {
					toBeAnimated.add(f06_);
				}
				success = true;
			}

			// SEC
			else if (fileType.equals(FileType.GRP)) {
				owner_.getOwner().setInitialDirectory(file);
				grp_.setUserData(file.toPath());
				if (!toBeAnimated.contains(grp_)) {
					toBeAnimated.add(grp_);
				}
				success = true;
			}
		}

		// animate file types
		if (success && !toBeAnimated.isEmpty()) {
			Animator.bouncingScale(0.0, 100.0, 1.0, 1.5, 1.0, (EventHandler<ActionEvent>) event -> {
				for (ImageView item : toBeAnimated) {
					item.setImage(Utility.getImage("full.png"));
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
		if (event.getGestureSource() != dropZone_ && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.F07) || fileType.equals(FileType.F06) || fileType.equals(FileType.GRP)) {
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
		if (event.getGestureSource() != dropZone_ && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.F07) || fileType.equals(FileType.F06) || fileType.equals(FileType.GRP)) {
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
		if (event.getGestureSource() != dropZone_ && db.hasFiles()) {

			// process files
			success = processFiles(db.getFiles());
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}

	@FXML
	private void onBrowseClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.getCustomFileFilter("Input files", FileType.F07, FileType.F06, FileType.GRP));

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
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
		ImageView[] items = { f07_, f06_, grp_ };
		for (ImageView item : items) {
			item.setUserData(null);
			item.setImage(AddSpectrumPanel.EMPTY);
		}

		// reset hyperlink
		browse_.setVisited(false);

		// reset model
		program_.clear();
		modelName_.clear();
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to add new aircraft model", null);
	}

	@FXML
	private void onDownloadSampleInputClicked() {
		owner_.getOwner().downloadSampleInput("AddNewAircraftModel");
	}

	@FXML
	private void onModelNameEntered() {
		onOKClicked();
	}

	/**
	 * Checks inputs and shows warning messages if necessary.
	 *
	 * @param f07File
	 *            F07 file.
	 * @param f06File
	 *            F06 file.
	 * @param grpFile
	 *            GRP file.
	 * @param program
	 *            A/C program.
	 * @param modelName
	 *            A/C model name.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(Path f07File, Path f06File, Path grpFile, String program, String modelName) {

		// check inputs
		String message = null;
		Node node = null;

		// check
		if (f07File == null || !Files.exists(f07File)) {
			message = "Please select a valid F07 file to proceed.";
			node = f07_;
		}
		else if (f06File == null || !Files.exists(f06File)) {
			message = "Please select a valid F06 file to proceed.";
			node = f06_;
		}
		else if (grpFile != null && !Files.exists(grpFile)) {
			message = "Please select a valid GRP file to proceed.";
			node = grp_;
		}
		else if (program == null || program.trim().isEmpty()) {
			message = "Please enter an A/C program to proceed.";
			node = program_;
		}
		else if (modelName == null || modelName.trim().isEmpty()) {
			message = "Please enter an A/C model name to proceed.";
			node = modelName_;
		}

		// all valid inputs
		if (message == null)
			return true;

		// show warning
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
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
	public static AddAircraftModelPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("AddAircraftModelPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			AddAircraftModelPanel controller = (AddAircraftModelPanel) fxmlLoader.getController();

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
