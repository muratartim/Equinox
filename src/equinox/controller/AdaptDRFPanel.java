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
import org.controlsfx.control.ToggleSwitch;

import control.validationField.DoubleValidationField;
import control.validationField.IntegerValidationField;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.EquinoxTheme;
import equinox.data.input.AdaptDRFInput;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.AdaptDRF;
import equinox.task.SaveTask;
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
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

/**
 * Class for adapt DRF plugin panel controller.
 *
 * @author Murat Artim
 * @date 24 Aug 2017
 * @time 10:37:27
 *
 */
public class AdaptDRFPanel implements InternalInputSubPanel, SchedulingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private Accordion accordion_;

	@FXML
	private ImageView dropInputsImage_, ana_, txt_, dropOutputImage_, outputDir_;

	@FXML
	private Hyperlink browseInputs_, browseOutput_;

	@FXML
	private ToggleSwitch addComments_;

	@FXML
	private TextField targetEvent_;

	@FXML
	private IntegerValidationField runTillFlightCount_;

	@FXML
	private DoubleValidationField currentDRF_, newDRF_;

	@FXML
	private TitledPane filesPane_, outputPane_;

	@FXML
	private SplitMenuButton ok_;

	@FXML
	private BorderPane inputZone_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup text field listeners
		runTillFlightCount_.setDefaultValue(null);
		runTillFlightCount_.setMinimumValue(1, true);
		runTillFlightCount_.setAsOptionalInput(true);
		currentDRF_.setDefaultValue(1.0);
		currentDRF_.setMinimumValue(0.0, false);
		newDRF_.setDefaultValue(1.0);
		newDRF_.setMinimumValue(0.0, false);

		// expand first panel
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

		// reset panel
		onResetClicked();

		// expand first panel
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@Override
	public String getHeader() {
		return "Adapt DRF " + AdaptDRF.VERSION;
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// check inputs
		if (!checkInputs())
			return;

		// create input
		AdaptDRFInput input = new AdaptDRFInput();

		// get input files
		Path ana = (Path) ana_.getUserData();
		Path txt = (Path) txt_.getUserData();

		// get parameters
		String targetEvent = targetEvent_.getText();
		double currentDRF = Double.parseDouble(currentDRF_.getText().trim());
		double newDRF = Double.parseDouble(newDRF_.getText().trim());
		String runTillFlightString = runTillFlightCount_.getText();
		int runTillFlight = (runTillFlightString == null) || runTillFlightString.isEmpty() ? -1 : Integer.parseInt(runTillFlightString);
		boolean addComments = addComments_.isSelected();

		// get output directory
		Path outputDir = (Path) outputDir_.getUserData();

		// set inputs
		input.setANAFile(ana);
		input.setTXTFile(txt);
		input.setTargetEvent(targetEvent);
		input.setCurrentDRF(currentDRF);
		input.setNewDRF(newDRF);
		input.setRunTillFlight(runTillFlight);
		input.setAddComments(addComments);
		input.setOutputDirectory(outputDir);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new AdaptDRF(input));
		}

		// save it for later
		else {
			tm.runTaskInParallel(new SaveTask(new AdaptDRF(input), scheduleDate));
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Checks inputs and returns true if they pass.
	 *
	 * @return True if the inputs are acceptable.
	 */
	private boolean checkInputs() {

		// no ANA file given
		Path ana = (Path) ana_.getUserData();
		if ((ana == null) || !Files.exists(ana)) {
			accordion_.setExpandedPane(accordion_.getPanes().get(0));
			String message = "Please supply input ANA file to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(filesPane_.isExpanded() ? ana_ : filesPane_);
			return false;
		}

		// no TXT file given
		Path txt = (Path) txt_.getUserData();
		if ((txt == null) || !Files.exists(txt)) {
			accordion_.setExpandedPane(accordion_.getPanes().get(0));
			String message = "Please supply input TXT file to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(filesPane_.isExpanded() ? txt_ : filesPane_);
			return false;
		}

		// no target fatigue event given
		String targetEvent = targetEvent_.getText();
		if ((targetEvent == null) || targetEvent.trim().isEmpty()) {
			String message = "Please supply target fatigue event to proceed.";
			accordion_.setExpandedPane(accordion_.getPanes().get(1));
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(targetEvent_);
			return false;
		}

		// check double validation fields
		DoubleValidationField[] doubleValidation = { currentDRF_, newDRF_ };
		for (DoubleValidationField field : doubleValidation) {
			String message = field.validate();
			if (message != null) {
				accordion_.setExpandedPane(accordion_.getPanes().get(1));
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(field);
				return false;
			}
		}

		// check integer validation fields
		IntegerValidationField[] integerValidation = { runTillFlightCount_ };
		for (IntegerValidationField field : integerValidation) {
			String message = field.validate();
			if (message != null) {
				accordion_.setExpandedPane(accordion_.getPanes().get(1));
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(field);
				return false;
			}
		}

		// check relative inputs
		double currentDRF = Double.parseDouble(currentDRF_.getText().trim());
		double newDRF = Double.parseDouble(newDRF_.getText().trim());
		if ((newDRF <= (0.5 * currentDRF)) || (newDRF >= (2.0 * currentDRF))) {
			String message = "New DRF value should be within the following limits: 0.5 * currentDRF < newDRF < 2.0 * currentDRF.";
			accordion_.setExpandedPane(accordion_.getPanes().get(1));
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(newDRF_);
			return false;
		}

		// no output directory given
		Path outputDir = (Path) outputDir_.getUserData();
		if ((outputDir == null) || !Files.exists(outputDir) || !Files.isDirectory(outputDir)) {
			accordion_.setExpandedPane(accordion_.getPanes().get(2));
			String message = "Please supply output directory to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(outputPane_.isExpanded() ? outputDir_ : outputPane_);
			return false;
		}

		// check passed
		return true;
	}

	@SuppressWarnings("static-method")
	@FXML
	private void onDragOverInput(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if (db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.ANA) || fileType.equals(FileType.GZ) || fileType.equals(FileType.ZIP) || fileType.equals(FileType.TXT)) {
					event.acceptTransferModes(TransferMode.ANY);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragEnteredInput(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if (db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.ANA) || fileType.equals(FileType.GZ) || fileType.equals(FileType.ZIP) || fileType.equals(FileType.TXT)) {
					dropInputsImage_.setImage(AddSpectrumPanel.DROP_ZONE_ACTIVE);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragExitedInput(DragEvent event) {
		dropInputsImage_.setImage(AddSpectrumPanel.DROP_ZONE);
		event.consume();
	}

	@FXML
	private void onDragDroppedInput(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		boolean success = false;
		if (db.hasFiles()) {

			// process input files
			success = processInputFiles(db.getFiles());
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}

	@FXML
	private void onBrowseInputsClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.getCustomFileFilter("Input files", FileType.ANA, FileType.GZ, FileType.ZIP, FileType.TXT));

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if ((files == null) || files.isEmpty())
			return;

		// process files
		processInputFiles(files);
	}

	/**
	 * Processes given input files.
	 *
	 * @param files
	 *            Input files.
	 * @return True if process completed successfully.
	 */
	private boolean processInputFiles(List<File> files) {

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

			// ANA, GZ or ZIP
			if (fileType.equals(FileType.ANA) || fileType.equals(FileType.GZ) || fileType.equals(FileType.ZIP)) {
				owner_.getOwner().setInitialDirectory(file);
				ana_.setUserData(file.toPath());
				if (!toBeAnimated.contains(ana_)) {
					toBeAnimated.add(ana_);
				}
				success = true;
			}

			// TXT
			else if (fileType.equals(FileType.TXT)) {
				owner_.getOwner().setInitialDirectory(file);
				txt_.setUserData(file.toPath());
				if (!toBeAnimated.contains(txt_)) {
					toBeAnimated.add(txt_);
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

	@SuppressWarnings("static-method")
	@FXML
	private void onDragOverOutput(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if (db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// accepted type
				if (file.isDirectory()) {
					event.acceptTransferModes(TransferMode.ANY);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragEnteredOutput(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if (db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// accepted type
				if (file.isDirectory()) {
					dropOutputImage_.setImage(AddSpectrumPanel.DROP_ZONE_ACTIVE);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragExitedOutput(DragEvent event) {
		dropOutputImage_.setImage(AddSpectrumPanel.DROP_ZONE);
		event.consume();
	}

	@FXML
	private void onDragDroppedOutput(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		boolean success = false;
		if (db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// directory
				if (file.isDirectory()) {
					processOutputDirectory(file);
					success = true;
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
	private void onBrowseOutputClicked() {

		// get file chooser
		DirectoryChooser directoryChooser = owner_.getOwner().getDirectoryChooser();

		// show open dialog
		File directory = directoryChooser.showDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if ((directory == null) || !directory.exists())
			return;

		// process directory
		processOutputDirectory(directory);
	}

	/**
	 * Processes given output directory.
	 *
	 * @param file
	 *            Output directory.
	 */
	private void processOutputDirectory(File file) {

		// set data
		owner_.getOwner().setInitialDirectory(file);
		outputDir_.setUserData(file.toPath());

		// animate file types
		Animator.bouncingScale(0.0, 100.0, 1.0, 1.5, 1.0, new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				outputDir_.setImage(Utility.getImage("full.png"));
			}
		}, outputDir_).play();
	}

	@FXML
	private void onResetClicked() {

		// clear all files
		ImageView[] items = { ana_, txt_, outputDir_ };
		for (ImageView item : items) {
			item.setUserData(null);
			item.setImage(AddSpectrumPanel.EMPTY);
		}

		// reset hyperlink
		browseInputs_.setVisited(false);
		browseOutput_.setVisited(false);

		// reset parameters
		targetEvent_.clear();
		currentDRF_.reset();
		newDRF_.reset();
		runTillFlightCount_.reset();
		addComments_.setSelected(false);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		// LATER on help clicked
		owner_.getOwner().showHelp("AdaptDRF", null);
	}

	@FXML
	private void onDownloadSampleInputClicked() {
		owner_.getOwner().downloadSampleInput("AdaptDRF");
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

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static AdaptDRFPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("AdaptDRFPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			AdaptDRFPanel controller = (AdaptDRFPanel) fxmlLoader.getController();

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
