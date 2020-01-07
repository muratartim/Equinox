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
import equinox.data.StressComponent;
import equinox.data.input.MyCheckInput;
import equinox.data.input.MyCheckInput.AircraftProgram;
import equinox.data.ui.MyCheckMission;
import equinox.data.ui.MyCheckMission.MissionType;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.GetConvTableSheetNames;
import equinox.task.GetConvTableSheetNames.ConversionTableSheetsRequestingPanel;
import equinox.task.MyCheck;
import equinox.task.SaveTask;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TabPane;
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
 * Class for MyCheck tool panel controller.
 *
 * @author Murat Artim
 * @date Mar 16, 2015
 * @time 4:15:09 PM
 */
public class MyCheckPanel implements InternalInputSubPanel, ConversionTableSheetsRequestingPanel, SchedulingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private Accordion accordion_;

	@FXML
	private ComboBox<String> sheet_;

	@FXML
	private ChoiceBox<AircraftProgram> program_;

	@FXML
	private ComboBox<MissionType> missionType_;

	@FXML
	private ImageView dropInputsImage_, ana_, cvt_, fls_, xls_, txt_, stf_, dropOutputImage_, outputDir_;

	@FXML
	private Hyperlink browseInputs_, browseOutput_;

	@FXML
	private Button addMission_, removeMission_;

	@FXML
	private ListView<MyCheckMission> missionList_;

	@FXML
	private ToggleSwitch esgCount_, roundTheClock_, returnTo1g_, printFSF_, printFactors_, write1g_, countFlights_, enableSlog_, esgSTH_, warnCombo_, removeNegative_, addDP_, moreText_, fixedXaxis_, fixedYaxis_;

	@FXML
	private IntegerValidationField maxPoints_, runTillFlightCount_, cvtWarnings_, rotation_, runTillFlightSTH_, flightsPerPage_, loadFlights_, yAxisText_, neutralYAxis_;

	@FXML
	private DoubleValidationField refDP_, dpFactor_, overallFactor_, refDT_, xAxisScale_, yAxisScale_;

	@FXML
	private ChoiceBox<StressComponent> stressComponent_;

	@FXML
	private TitledPane missionsPane_, outputPane_;

	@FXML
	private SplitMenuButton ok_;

	@FXML
	private TabPane optionsTabPane_;

	@FXML
	private BorderPane inputZone_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// bind components
		program_.disableProperty().bind(returnTo1g_.selectedProperty().not());
		maxPoints_.disableProperty().bind(countFlights_.selectedProperty().not());

		// set aircraft programs
		program_.setItems(FXCollections.observableArrayList(AircraftProgram.values()));
		program_.getSelectionModel().select(0);

		// set mission types
		missionType_.setItems(FXCollections.observableArrayList(MissionType.values()));

		// bind components
		inputZone_.disableProperty().bind(missionType_.getSelectionModel().selectedItemProperty().isNull());

		// setup mission list
		missionList_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// bind remove mission button
		removeMission_.disableProperty().bind(missionList_.getSelectionModel().selectedItemProperty().isNull());

		// add change listener to conversion table check image
		xls_.imageProperty().addListener((ChangeListener<Image>) (observable, oldValue, newValue) -> {

			// setup add button
			setupAddButton();

			// no value given
			if (newValue == null)
				return;

			// get data
			Path conversionTable = (Path) xls_.getUserData();

			// no data
			if (conversionTable == null) {
				sheet_.getSelectionModel().clearSelection();
				sheet_.setValue(null);
				sheet_.getItems().clear();
				sheet_.setDisable(true);
				return;
			}

			// get worksheet names
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetConvTableSheetNames(MyCheckPanel.this, conversionTable));
		});

		// add change listeners to file check images
		setListenersToCheckImages(ana_, txt_, cvt_);

		// add stress components
		stressComponent_.setItems(FXCollections.observableArrayList(StressComponent.values()));
		stressComponent_.getSelectionModel().select(0);

		// add listener
		stressComponent_.getSelectionModel().selectedItemProperty().addListener((ChangeListener<StressComponent>) (observable, oldValue, newValue) -> rotation_.setDisable(!newValue.equals(StressComponent.ROTATED)));

		// setup text field listeners
		maxPoints_.setDefaultValue(123456);
		maxPoints_.setMinimumValue(1, true);
		runTillFlightCount_.setDefaultValue(null);
		runTillFlightCount_.setMinimumValue(1, true);
		runTillFlightCount_.setAsOptionalInput(true);
		cvtWarnings_.setDefaultValue(10);
		cvtWarnings_.setMinimumValue(0, true);
		refDP_.setDefaultValue(1327.0);
		refDP_.setMinimumValue(0.0, true);
		dpFactor_.setDefaultValue(1.0);
		overallFactor_.setDefaultValue(1.0);
		rotation_.setDefaultValue(0);
		runTillFlightSTH_.setDefaultValue(null);
		runTillFlightSTH_.setMinimumValue(1, true);
		runTillFlightSTH_.setAsOptionalInput(true);
		refDT_.setDefaultValue(0.0);
		refDT_.setMinimumValue(0.0, true);
		flightsPerPage_.setDefaultValue(1);
		flightsPerPage_.setMinimumValue(1, true);
		loadFlights_.setDefaultValue(6);
		loadFlights_.setMinimumValue(1, true);
		xAxisScale_.setDefaultValue(0.25);
		xAxisScale_.setMinimumValue(0.1, true);
		yAxisScale_.setDefaultValue(2.0);
		yAxisScale_.setMinimumValue(0.1, true);
		yAxisText_.setDefaultValue(425);
		yAxisText_.setMinimumValue(0, true);
		neutralYAxis_.setDefaultValue(290);
		neutralYAxis_.setMinimumValue(0, true);

		// expand first panel
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@Override
	public void setConversionTableSheetNames(String[] sheetNames) {

		// clear all sheets
		sheet_.getSelectionModel().clearSelection();
		sheet_.setValue(null);
		sheet_.getItems().clear();

		// set sheet names
		sheet_.getItems().setAll(sheetNames);

		// enable menu button
		sheet_.setDisable(false);
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
		return "MyCheck " + MyCheck.VERSION;
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// check inputs
		if (!checkInputs())
			return;

		// create input
		MyCheckInput input = new MyCheckInput();

		// set missions
		input.setMissions(missionList_.getItems());

		// set count ANA options
		boolean esgCount = esgCount_.isSelected();
		boolean roundTheClock = roundTheClock_.isSelected();
		boolean printFSF = printFSF_.isSelected();
		boolean printFactors = printFactors_.isSelected();
		boolean write1g = write1g_.isSelected();
		boolean returnTo1g = returnTo1g_.isSelected();
		AircraftProgram program = program_.getSelectionModel().getSelectedItem();
		boolean countFlights = countFlights_.isSelected();
		int maxPoints = Integer.parseInt(maxPoints_.getText());
		String runTillCountString = runTillFlightCount_.getText();
		int runTillCount = runTillCountString == null || runTillCountString.isEmpty() ? -1 : Integer.parseInt(runTillCountString);
		int cvtWarnings = Integer.parseInt(cvtWarnings_.getText());
		input.setCountANABooleans(new boolean[] { esgCount, roundTheClock, printFSF, printFactors, write1g, returnTo1g, countFlights });
		input.setCountANAIntegers(new int[] { maxPoints, runTillCount, cvtWarnings });
		input.setAircraftProgram(program);

		// set generate STH options
		boolean enableSlog = enableSlog_.isSelected();
		boolean esgSTH = esgSTH_.isSelected();
		boolean warnCombo = warnCombo_.isSelected();
		boolean removeNegative = removeNegative_.isSelected();
		boolean addDP = addDP_.isSelected();
		double refDP = Double.parseDouble(refDP_.getText());
		double dpFactor = Double.parseDouble(dpFactor_.getText());
		double overallFactor = Double.parseDouble(overallFactor_.getText());
		StressComponent component = stressComponent_.getSelectionModel().getSelectedItem();
		double rotation = Double.parseDouble(rotation_.getText());
		String runTillSthString = runTillFlightSTH_.getText();
		int runTillSth = runTillSthString == null || runTillSthString.isEmpty() ? -1 : Integer.parseInt(runTillSthString);
		double refDT = Double.parseDouble(refDT_.getText());
		input.setGenerateSTHBooleans(new boolean[] { enableSlog, esgSTH, warnCombo, removeNegative, addDP });
		input.setGenerateSTHDoubles(new double[] { refDP, dpFactor, overallFactor, rotation, refDT });
		input.setStressComponent(component);
		input.setRunTillFlightSTH(runTillSth);

		// set plot options
		boolean moreText = moreText_.isSelected();
		boolean fixedXaxis = fixedXaxis_.isSelected();
		boolean fixedYaxis = fixedYaxis_.isSelected();
		int flightsPerPage = Integer.parseInt(flightsPerPage_.getText());
		int loadFlights = Integer.parseInt(loadFlights_.getText());
		double xAxisScale = Double.parseDouble(xAxisScale_.getText());
		double yAxisScale = Double.parseDouble(yAxisScale_.getText());
		int yAxisText = Integer.parseInt(yAxisText_.getText());
		int neutralYAxis = Integer.parseInt(neutralYAxis_.getText());
		input.setPlotBooleans(new boolean[] { moreText, fixedXaxis, fixedYaxis });
		input.setPlotDoubles(new double[] { xAxisScale, yAxisScale });
		input.setPlotIntegers(new int[] { flightsPerPage, loadFlights, yAxisText, neutralYAxis });

		// set output directory
		input.setOutputDirectory((Path) outputDir_.getUserData());

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new MyCheck(input));
		}
		else {
			tm.runTaskInParallel(new SaveTask(new MyCheck(input), scheduleDate));
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
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
				if (fileType.equals(FileType.ANA) || fileType.equals(FileType.GZ) || fileType.equals(FileType.ZIP) || fileType.equals(FileType.TXT) || fileType.equals(FileType.CVT) || fileType.equals(FileType.XLS) || fileType.equals(FileType.STF)) {
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
				if (fileType.equals(FileType.ANA) || fileType.equals(FileType.GZ) || fileType.equals(FileType.ZIP) || fileType.equals(FileType.TXT) || fileType.equals(FileType.CVT) || fileType.equals(FileType.XLS) || fileType.equals(FileType.STF)) {
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
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.getCustomFileFilter("Input files", FileType.ANA, FileType.GZ, FileType.ZIP, FileType.TXT, FileType.CVT, FileType.XLS, FileType.STF));

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
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

			// CVT
			else if (fileType.equals(FileType.CVT)) {
				owner_.getOwner().setInitialDirectory(file);
				cvt_.setUserData(file.toPath());
				if (!toBeAnimated.contains(cvt_)) {
					toBeAnimated.add(cvt_);
				}
				success = true;
			}

			// XLS
			else if (fileType.equals(FileType.XLS)) {
				owner_.getOwner().setInitialDirectory(file);
				xls_.setUserData(file.toPath());
				if (!toBeAnimated.contains(xls_)) {
					toBeAnimated.add(xls_);
				}
				success = true;
			}

			// STF
			else if (fileType.equals(FileType.STF)) {
				owner_.getOwner().setInitialDirectory(file);
				stf_.setUserData(file.toPath());
				if (!toBeAnimated.contains(stf_)) {
					toBeAnimated.add(stf_);
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
		if (directory == null || !directory.exists())
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
		Animator.bouncingScale(0.0, 100.0, 1.0, 1.5, 1.0, (EventHandler<ActionEvent>) event -> outputDir_.setImage(Utility.getImage("full.png")), outputDir_).play();
	}

	@FXML
	private void onAddMissionClicked() {

		// create and add mission to list
		MissionType type = missionType_.getSelectionModel().getSelectedItem();
		Path ana = (Path) ana_.getUserData();
		Path txt = (Path) txt_.getUserData();
		Path cvt = (Path) cvt_.getUserData();
		Path xls = (Path) xls_.getUserData();
		Path stf = (Path) stf_.getUserData();
		String sheet = sheet_.getSelectionModel().getSelectedItem();
		missionList_.getItems().add(new MyCheckMission(type, ana, txt, cvt, xls, sheet, stf));

		// clear mission type
		missionType_.getSelectionModel().clearSelection();
		missionType_.setValue(null);

		// clear all files
		ImageView[] items = { ana_, txt_, cvt_, xls_, stf_ };
		for (ImageView item : items) {
			item.setUserData(null);
			item.setImage(AddSpectrumPanel.EMPTY);
		}

		// reset worksheet
		sheet_.getSelectionModel().clearSelection();
		sheet_.setValue(null);
		sheet_.getItems().clear();
		sheet_.setDisable(true);

		// reset hyperlink
		browseInputs_.setVisited(false);

		// setup add button
		setupAddButton();
	}

	@FXML
	private void onRemoveMissionClicked() {
		ObservableList<MyCheckMission> selected = missionList_.getSelectionModel().getSelectedItems();
		missionList_.getItems().removeAll(selected);
		setupAddButton();
	}

	@FXML
	private void onWorksheetSelected() {
		setupAddButton();
	}

	@FXML
	private void onMissionTypeSelected() {
		setupAddButton();
	}

	@FXML
	private void onResetClicked() {

		// clear mission type
		missionType_.getSelectionModel().clearSelection();
		missionType_.setValue(null);

		// clear all files
		ImageView[] items = { ana_, txt_, cvt_, xls_, stf_, outputDir_ };
		for (ImageView item : items) {
			item.setUserData(null);
			item.setImage(AddSpectrumPanel.EMPTY);
		}

		// reset worksheet
		sheet_.getSelectionModel().clearSelection();
		sheet_.setValue(null);
		sheet_.getItems().clear();
		sheet_.setDisable(true);

		// reset hyperlink
		browseInputs_.setVisited(false);
		browseOutput_.setVisited(false);

		// clear mission list
		missionList_.getItems().clear();

		// setup add button
		setupAddButton();

		// reset count ANA options
		esgCount_.setSelected(false);
		roundTheClock_.setSelected(true);
		returnTo1g_.setSelected(true);
		program_.getSelectionModel().select(AircraftProgram.A320_NEO);
		printFSF_.setSelected(false);
		printFactors_.setSelected(true);
		write1g_.setSelected(true);
		countFlights_.setSelected(false);
		maxPoints_.reset();
		runTillFlightCount_.reset();
		cvtWarnings_.reset();

		// reset generate STH options
		enableSlog_.setSelected(true);
		esgSTH_.setSelected(false);
		warnCombo_.setSelected(false);
		removeNegative_.setSelected(false);
		addDP_.setSelected(false);
		refDP_.reset();
		dpFactor_.reset();
		overallFactor_.reset();
		stressComponent_.getSelectionModel().select(StressComponent.NORMAL_X);
		rotation_.reset();
		runTillFlightSTH_.reset();
		refDT_.reset();

		// reset plot options
		moreText_.setSelected(true);
		flightsPerPage_.reset();
		loadFlights_.reset();
		fixedXaxis_.setSelected(false);
		xAxisScale_.reset();
		fixedYaxis_.setSelected(false);
		yAxisScale_.reset();
		yAxisText_.reset();
		neutralYAxis_.reset();
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("MyCheck", null);
	}

	@FXML
	private void onDownloadSampleInputClicked() {
		owner_.getOwner().downloadSampleInput("MyCheck");
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

		// no mission given
		if (missionList_.getItems().isEmpty()) {
			accordion_.setExpandedPane(accordion_.getPanes().get(0));
			String message = "Please supply at least 1 mission for validation.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(missionsPane_.isExpanded() ? missionList_ : missionsPane_);
			return false;
		}

		// check double validation fields
		DoubleValidationField[] doubleValidation = { refDP_, dpFactor_, overallFactor_, refDT_, xAxisScale_, yAxisScale_ };
		for (DoubleValidationField field : doubleValidation) {
			String message = field.validate();
			if (message != null) {
				accordion_.setExpandedPane(accordion_.getPanes().get(1));
				if (field.equals(refDP_) || field.equals(dpFactor_) || field.equals(overallFactor_) || field.equals(refDT_)) {
					optionsTabPane_.getSelectionModel().select(1);
				}
				else {
					optionsTabPane_.getSelectionModel().select(2);
				}
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
		IntegerValidationField[] integerValidation = { maxPoints_, runTillFlightCount_, cvtWarnings_, rotation_, runTillFlightSTH_, flightsPerPage_, loadFlights_, yAxisText_, neutralYAxis_ };
		for (IntegerValidationField field : integerValidation) {
			String message = field.validate();
			if (message != null) {
				accordion_.setExpandedPane(accordion_.getPanes().get(1));
				if (field.equals(maxPoints_) || field.equals(runTillFlightCount_) || field.equals(cvtWarnings_)) {
					optionsTabPane_.getSelectionModel().select(0);
				}
				else if (field.equals(rotation_) || field.equals(runTillFlightSTH_)) {
					optionsTabPane_.getSelectionModel().select(1);
				}
				else {
					optionsTabPane_.getSelectionModel().select(2);
				}
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

		// no output directory given
		Path outputDir = (Path) outputDir_.getUserData();
		if (outputDir == null || !Files.exists(outputDir) || !Files.isDirectory(outputDir)) {
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

	/**
	 * Sets listeners to file check images.
	 *
	 * @param images
	 *            Images to set listeners.
	 */
	private void setListenersToCheckImages(ImageView... images) {
		for (ImageView image : images) {
			image.imageProperty().addListener((ChangeListener<Image>) (observable, oldValue, newValue) -> setupAddButton());
		}
	}

	/**
	 * Sets up add mission button state.
	 *
	 */
	private void setupAddButton() {

		// all required inputs are supplied
		if (ana_.getUserData() != null && txt_.getUserData() != null && cvt_.getUserData() != null && xls_.getUserData() != null && !sheet_.getSelectionModel().isEmpty() && !missionType_.getSelectionModel().isEmpty()) {

			// check if selected mission type is already in the list
			MissionType type = missionType_.getSelectionModel().getSelectedItem();
			for (MyCheckMission mission : missionList_.getItems()) {
				if (mission.getMissionType().equals(type)) {
					addMission_.setDisable(true);
					return;
				}
			}

			// enable button
			addMission_.setDisable(false);
			return;
		}

		// disable button
		addMission_.setDisable(true);
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static MyCheckPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("MyCheckPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			MyCheckPanel controller = (MyCheckPanel) fxmlLoader.getController();

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
