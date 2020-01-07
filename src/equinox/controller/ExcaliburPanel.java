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
import java.util.logging.Level;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import control.validationField.DoubleValidationField;
import control.validationField.IntegerValidationField;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.EquinoxTheme;
import equinox.data.ExcaliburElement1DStressSortingCriteria;
import equinox.data.ExcaliburElement2DStressSortingCriteria;
import equinox.data.ExcaliburFrameStressSortingCriteria;
import equinox.data.ExcaliburStressSortingCriteria;
import equinox.data.ExcaliburStressType;
import equinox.data.input.ExcaliburInput;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.Excalibur;
import equinox.task.GetConvTableSheetNames;
import equinox.task.GetConvTableSheetNames.ConversionTableSheetsRequestingPanel;
import equinox.task.SaveTask;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

/**
 * Class for Excalibur panel controller.
 *
 * @author Murat Artim
 * @date 28 Nov 2017
 * @time 22:48:15
 */
public class ExcaliburPanel implements InternalInputSubPanel, ConversionTableSheetsRequestingPanel, SchedulingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private Accordion accordion_;

	@FXML
	private TitledPane correlationPane_, stressPane_, pressurePane_, criteriaPane_, outputPane_, optionsPane_;

	@FXML
	private BorderPane correlationZone_, stressZone_, outputZone_;

	@FXML
	private ImageView dropCorrelationImage_, dropStressImage_, dropOutputImage_, xls_, lck_, stressDir_, outputDir_;

	@FXML
	private Hyperlink browseCorrelation_, browseStressDir_, browseOutputDir_;

	@FXML
	private ComboBox<String> correlationSheet_;

	@FXML
	private ComboBox<ExcaliburStressType> stressType_;

	@FXML
	private ComboBox<ExcaliburStressSortingCriteria> sortingCriteria_;

	@FXML
	private ComboBox<Level> logLevel_;

	@FXML
	private DoubleValidationField rotationAngle_;

	@FXML
	private SplitMenuButton ok_;

	@FXML
	private ToggleSwitch includeDP_, runParallel_;

	@FXML
	private IntegerValidationField dpLoadcaseNumber_;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		// add change listener to attributes table check image
		xls_.imageProperty().addListener(new ChangeListener<Image>() {

			@Override
			public void changed(ObservableValue<? extends Image> observable, Image oldValue, Image newValue) {

				// no value given
				if (newValue == null)
					return;

				// get data
				Path attributesTable = (Path) xls_.getUserData();

				// no data
				if (attributesTable == null) {
					correlationSheet_.getSelectionModel().clearSelection();
					correlationSheet_.setValue(null);
					correlationSheet_.getItems().clear();
					correlationSheet_.setDisable(true);
					return;
				}

				// get worksheet names
				owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetConvTableSheetNames(ExcaliburPanel.this, attributesTable));
			}
		});

		// add stress types
		stressType_.getItems().clear();
		stressType_.getItems().addAll(ExcaliburStressType.values());

		// setup delta-p loadcase number field
		dpLoadcaseNumber_.setDefaultValue(null);
		dpLoadcaseNumber_.setMinimumValue(1, true);
		dpLoadcaseNumber_.disableProperty().bind(includeDP_.selectedProperty().not());

		// set log levels
		logLevel_.getItems().clear();
		logLevel_.getItems().addAll(Level.ALL, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.INFO, Level.OFF, Level.SEVERE, Level.WARNING);
		logLevel_.getSelectionModel().select(Level.WARNING);

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
		onResetClicked();
	}

	@Override
	public String getHeader() {
		return "Excalibur " + Excalibur.VERSION;
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// check inputs
		if (!checkInputs())
			return;

		// create input
		ExcaliburInput input = new ExcaliburInput();

		// get loadcase correlation input
		Path xls = (Path) xls_.getUserData();
		Path lck = (Path) lck_.getUserData();
		String sheet = correlationSheet_.getSelectionModel().getSelectedItem();
		input.setAttributesTable(xls.toFile());
		input.setLoadcaseKeysFile(lck.toFile());
		input.setAttributesTableSheet(sheet);

		// get element stress input
		ExcaliburStressType stressType = stressType_.getSelectionModel().getSelectedItem();
		Path stressDir = (Path) stressDir_.getUserData();
		input.setStressType(stressType);
		input.setStressDirectory(stressDir.toFile());

		// get delta-p loadcase number
		Integer dpLoadcase = includeDP_.isSelected() ? Integer.parseInt(dpLoadcaseNumber_.getText()) : null;
		input.setDpLoadcaseNumber(dpLoadcase);

		// get stress sorting criteria input
		ExcaliburStressSortingCriteria criteria = sortingCriteria_.getSelectionModel().getSelectedItem();
		double angle = 0.0;
		if (criteria.equals(ExcaliburElement2DStressSortingCriteria.MAX_ROTATED_STRESS)) {
			angle = Double.parseDouble(rotationAngle_.getText());
		}
		input.setStressSortingCriteria(criteria);
		input.setRotationAngle(angle);

		// get output directory
		Path outputDir = (Path) outputDir_.getUserData();
		input.setOutputDirectory(outputDir.toFile());

		// set options
		Level logLevel = logLevel_.getSelectionModel().getSelectedItem();
		input.setLogLevel(logLevel);
		input.setRunInParallel(runParallel_.isSelected());

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new Excalibur(input));
		}

		// save it for later
		else {
			tm.runTaskInParallel(new SaveTask(new Excalibur(input), scheduleDate));
		}
	}

	@Override
	public void setConversionTableSheetNames(String[] sheetNames) {

		// clear all sheets
		correlationSheet_.getSelectionModel().clearSelection();
		correlationSheet_.setValue(null);
		correlationSheet_.getItems().clear();

		// set sheet names
		correlationSheet_.getItems().setAll(sheetNames);

		// enable menu button
		correlationSheet_.setDisable(false);
	}

	@FXML
	private void onDragOverCorrelation(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if ((event.getGestureSource() != correlationZone_) && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.XLS) || fileType.equals(FileType.LCK)) {
					event.acceptTransferModes(TransferMode.ANY);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragEnteredCorrelation(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if ((event.getGestureSource() != correlationZone_) && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.XLS) || fileType.equals(FileType.LCK)) {
					dropCorrelationImage_.setImage(AddSpectrumPanel.DROP_ZONE_ACTIVE);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragExitedCorrelation(DragEvent event) {
		dropCorrelationImage_.setImage(AddSpectrumPanel.DROP_ZONE);
		event.consume();
	}

	@FXML
	private void onDragDroppedCorrelation(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		boolean success = false;
		if ((event.getGestureSource() != correlationZone_) && db.hasFiles()) {

			// process files
			success = processCorrelationFiles(db.getFiles());
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}

	@FXML
	private void onBrowseCorrelationClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.getSpectrumFileFilter(false));

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if ((files == null) || files.isEmpty())
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(files.get(0));

		// process files
		processCorrelationFiles(files);
	}

	/**
	 * Processes input correlation files.
	 *
	 * @param files
	 *            Selected correlation files.
	 * @return True if process completed successfully.
	 */
	private boolean processCorrelationFiles(List<File> files) {

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

			// XLS
			if (fileType.equals(FileType.XLS)) {
				owner_.getOwner().setInitialDirectory(file);
				xls_.setUserData(file.toPath());
				if (!toBeAnimated.contains(xls_)) {
					toBeAnimated.add(xls_);
				}
				success = true;
			}

			// LCK
			else if (fileType.equals(FileType.LCK)) {
				owner_.getOwner().setInitialDirectory(file);
				lck_.setUserData(file.toPath());
				if (!toBeAnimated.contains(lck_)) {
					toBeAnimated.add(lck_);
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
	private void onDownloadSampleCorrelationClicked() {
		owner_.getOwner().downloadSampleInput("excaliburLoadcaseCorrelationInput");
	}

	@FXML
	private void onStressTypeSelected() {

		// clear stress sorting criterion
		sortingCriteria_.getItems().clear();

		// clear any current sorting criteria selection
		sortingCriteria_.getSelectionModel().clearSelection();

		// no selection
		if (stressType_.getSelectionModel().isEmpty())
			return;

		// get selected stress type
		ExcaliburStressType stressType = stressType_.getSelectionModel().getSelectedItem();

		// 1D element
		if (stressType.equals(ExcaliburStressType.ELEMENT_1D)) {
			sortingCriteria_.getItems().addAll(ExcaliburElement1DStressSortingCriteria.values());
			sortingCriteria_.setVisibleRowCount(ExcaliburElement1DStressSortingCriteria.values().length);
		}

		// 2D element
		else if (stressType.equals(ExcaliburStressType.ELEMENT_2D)) {
			sortingCriteria_.getItems().addAll(ExcaliburElement2DStressSortingCriteria.values());
			sortingCriteria_.setVisibleRowCount(ExcaliburElement2DStressSortingCriteria.values().length);
		}

		// frame
		else if (stressType.equals(ExcaliburStressType.FRAME)) {
			sortingCriteria_.getItems().addAll(ExcaliburFrameStressSortingCriteria.values());
			sortingCriteria_.setVisibleRowCount(ExcaliburFrameStressSortingCriteria.values().length);
		}

		// clear any current sorting criteria selection
		sortingCriteria_.getSelectionModel().clearSelection();
	}

	@FXML
	private void onDragOverStress(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if ((event.getGestureSource() != stressZone_) && db.hasFiles()) {

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
	private void onDragEnteredStress(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if ((event.getGestureSource() != stressZone_) && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// accepted type
				if (file.isDirectory()) {
					dropStressImage_.setImage(AddSpectrumPanel.DROP_ZONE_ACTIVE);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragExitedStress(DragEvent event) {
		dropStressImage_.setImage(AddSpectrumPanel.DROP_ZONE);
		event.consume();
	}

	@FXML
	private void onDragDroppedStress(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		boolean success = false;
		if ((event.getGestureSource() != stressZone_) && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// directory
				if (file.isDirectory()) {
					processStressDirectory(file);
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
	private void onBrowseStressDirectoryClicked() {

		// get file chooser
		DirectoryChooser directoryChooser = owner_.getOwner().getDirectoryChooser();

		// show open dialog
		File directory = directoryChooser.showDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if ((directory == null) || !directory.exists())
			return;

		// process directory
		processStressDirectory(directory);
	}

	/**
	 * Processes given stress directory.
	 *
	 * @param directory
	 *            Stress directory.
	 */
	private void processStressDirectory(File directory) {

		// set data
		owner_.getOwner().setInitialDirectory(directory);
		stressDir_.setUserData(directory.toPath());

		// animate file types
		Animator.bouncingScale(0.0, 100.0, 1.0, 1.5, 1.0, new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				stressDir_.setImage(Utility.getImage("full.png"));
			}
		}, stressDir_).play();
	}

	@FXML
	private void onDownloadSampleStressFileClicked() {
		owner_.getOwner().downloadSampleInput("excaliburStressInput");
	}

	@FXML
	private void onSortingCriteriaSelected() {

		// get stress sorting criteria
		ExcaliburStressSortingCriteria criteria = sortingCriteria_.getSelectionModel().getSelectedItem();

		// no criteria selected
		if (criteria == null) {
			rotationAngle_.clear();
			rotationAngle_.setDisable(true);
			return;
		}

		// setup rotation angle
		rotationAngle_.setDisable(!criteria.equals(ExcaliburElement2DStressSortingCriteria.MAX_ROTATED_STRESS));
		if (rotationAngle_.isDisable()) {
			rotationAngle_.reset();
		}
	}

	@FXML
	private void onDragOverOutput(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if ((event.getGestureSource() != outputZone_) && db.hasFiles()) {

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
		if ((event.getGestureSource() != outputZone_) && db.hasFiles()) {

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
		if ((event.getGestureSource() != outputZone_) && db.hasFiles()) {

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
	private void onBrowseOutputDirectoryClicked() {

		// get file chooser
		DirectoryChooser directoryChooser = owner_.getOwner().getDirectoryChooser();

		// show open dialog
		File directory = directoryChooser.showDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if ((directory == null) || !directory.exists())
			return;

		// process directory
		processStressDirectory(directory);
	}

	/**
	 * Processes given output directory.
	 *
	 * @param directory
	 *            Output directory.
	 */
	private void processOutputDirectory(File directory) {

		// set data
		owner_.getOwner().setInitialDirectory(directory);
		outputDir_.setUserData(directory.toPath());

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
		ImageView[] items = { xls_, lck_, stressDir_, outputDir_ };
		for (ImageView item : items) {
			item.setUserData(null);
			item.setImage(AddSpectrumPanel.EMPTY);
		}

		// reset worksheet
		correlationSheet_.getSelectionModel().clearSelection();
		correlationSheet_.setValue(null);
		correlationSheet_.getItems().clear();
		correlationSheet_.setDisable(true);

		// reset hyperlinks
		browseCorrelation_.setVisited(false);
		browseStressDir_.setVisited(false);
		browseOutputDir_.setVisited(false);

		// clear stress type
		stressType_.getSelectionModel().clearSelection();

		// reset delta-p inputs
		includeDP_.setSelected(true);
		dpLoadcaseNumber_.clear();

		// reset options
		logLevel_.getSelectionModel().select(Level.WARNING);
		runParallel_.setSelected(true);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("Excalibur", null);
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
	 * Checks inputs and returns true if they pass.
	 *
	 * @return True if the inputs are acceptable.
	 */
	private boolean checkInputs() {

		// no attributes table file given
		Path xls = (Path) xls_.getUserData();
		if ((xls == null) || !Files.exists(xls)) {
			String message = "Please supply input attributes table file to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(correlationPane_.isExpanded() ? xls_ : correlationPane_);
			return false;
		}

		// no loadcase keys file given
		Path lck = (Path) lck_.getUserData();
		if ((lck == null) || !Files.exists(lck)) {
			String message = "Please supply input loadcase keys file to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(correlationPane_.isExpanded() ? lck_ : correlationPane_);
			return false;
		}

		// no sheet selected from attributes table
		String sheet = correlationSheet_.getSelectionModel().getSelectedItem();
		if ((sheet == null) || sheet.isEmpty()) {
			String message = "Please select a valid worksheet from the attributes table file to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(correlationPane_.isExpanded() ? correlationSheet_ : correlationPane_);
			return false;
		}

		// no stress type selected
		if (stressType_.getSelectionModel().isEmpty()) {
			String message = "Please select a valid stress type to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(stressPane_.isExpanded() ? stressType_ : stressPane_);
			return false;
		}

		// no input stress directory selected
		Path stressDir = (Path) stressDir_.getUserData();
		if ((stressDir == null) || !Files.exists(stressDir) || !Files.isDirectory(stressDir)) {
			String message = "Please supply a valid input element stress directory to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(stressPane_.isExpanded() ? stressDir_ : stressPane_);
			return false;
		}

		// check delta-p loadcase number
		if (includeDP_.isSelected()) {
			if (dpLoadcaseNumber_.validate() != null) {
				String message = "Please supply a valid delta-p loadcase number to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(pressurePane_.isExpanded() ? dpLoadcaseNumber_ : pressurePane_);
				return false;
			}
		}

		// no stress sorting criteria selected
		ExcaliburStressSortingCriteria criteria = sortingCriteria_.getSelectionModel().getSelectedItem();
		if (criteria == null) {
			String message = "Please supply a valid stress sorting criteria to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(criteriaPane_.isExpanded() ? sortingCriteria_ : criteriaPane_);
			return false;
		}

		// no rotation angle is given
		if (criteria.equals(ExcaliburElement2DStressSortingCriteria.MAX_ROTATED_STRESS)) {
			if (rotationAngle_.validate() != null) {
				String message = "Please supply a valid stress rotation angle to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(criteriaPane_.isExpanded() ? rotationAngle_ : criteriaPane_);
				return false;
			}
		}

		// no output directory selected
		Path outputDir = (Path) outputDir_.getUserData();
		if ((outputDir == null) || !Files.exists(outputDir) || !Files.isDirectory(outputDir)) {
			String message = "Please supply a valid output directory to proceed.";
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

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static ExcaliburPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ExcaliburPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			ExcaliburPanel controller = (ExcaliburPanel) fxmlLoader.getController();

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
