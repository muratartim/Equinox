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
import equinox.controller.RfortAddOmissionsPopup.RfortDirectOmissionAddingPanel;
import equinox.controller.RfortAddPercentOmissionsPopup.RfortPercentOmissionAddingPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.AnalysisEngine;
import equinox.data.EquinoxTheme;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.Settings;
import equinox.data.StressComponent;
import equinox.data.input.RfortExtendedInput;
import equinox.data.ui.RfortOmission;
import equinox.data.ui.RfortPilotPoint;
import equinox.data.ui.SerializableRfortPilotPoint;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.CreateRfortAnalysis;
import equinox.task.GetConvTableSheetNames;
import equinox.task.GetConvTableSheetNames.ConversionTableSheetsRequestingPanel;
import equinox.task.GetFatigueMaterials;
import equinox.task.GetLinearMaterials;
import equinox.task.GetPreffasMaterials;
import equinox.task.RfortAnalysis;
import equinox.task.SaveTask;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for RFORT Extended tool controller panel.
 *
 * @author Murat Artim
 * @date Jan 16, 2015
 * @time 9:50:23 AM
 */
public class RfortExtendedPanel implements InternalInputSubPanel, SchedulingPanel, ConversionTableSheetsRequestingPanel, RfortPercentOmissionAddingPanel, RfortDirectOmissionAddingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Pilot points popup. */
	private RfortPilotPointsPopup pilotPointsPopup_;

	/** Add percent omissions popup. */
	private RfortAddPercentOmissionsPopup addPercentOmissionsPopup_;

	/** Add direct omissions popup. */
	private RfortAddOmissionsPopup addDirectOmissionsPopup_;

	/** Pilot point list. */
	private final ObservableList<RfortPilotPoint> pilotPointList_ = FXCollections.observableArrayList();

	@FXML
	private VBox root_;

	@FXML
	private Accordion accordion_;

	@FXML
	private BorderPane dropZone_;

	@FXML
	private TextField targetFlights_;

	@FXML
	private DoubleValidationField refDP_, dpFac_, overallFac_;

	@FXML
	private IntegerValidationField rotation_, runTillFlight_;

	@FXML
	private ToggleSwitch addDP_, enableSlog_, fatigueAnalysis_, preffasAnalysis_, linearAnalysis_;

	@FXML
	private ChoiceBox<StressComponent> stressComponent_;

	@FXML
	private ListView<RfortPilotPoint> pilotPoints_;

	@FXML
	private ListView<RfortOmission> omissions_;

	@FXML
	private Button resetPilotPoints_, addPilotPoints_, removeOmissions_, resetOmissions_;

	@FXML
	private MenuButton addOmissions_;

	@FXML
	private ImageView dropInputsImage_, dropOutputImage_, ana_, cvt_, fls_, xls_, txt_;

	@FXML
	private Hyperlink browseInputs_;

	@FXML
	private ComboBox<String> sheet_;

	@FXML
	private SplitMenuButton ok_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// load popups
		pilotPointsPopup_ = RfortPilotPointsPopup.load(this, pilotPointList_);
		addPercentOmissionsPopup_ = RfortAddPercentOmissionsPopup.load(this);
		addDirectOmissionsPopup_ = RfortAddOmissionsPopup.load(this);

		// add change listener to conversion table check image
		xls_.imageProperty().addListener(new ChangeListener<Image>() {

			@Override
			public void changed(ObservableValue<? extends Image> observable, Image oldValue, Image newValue) {

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
				owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetConvTableSheetNames(RfortExtendedPanel.this, conversionTable));
			}
		});

		// set listeners
		addDP_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onAddDPSelected(newValue);
			}
		});

		// setup omission levels list
		omissions_.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<RfortOmission>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends RfortOmission> c) {
				removeOmissions_.setDisable(omissions_.getSelectionModel().getSelectedItems().isEmpty());
			}
		});
		omissions_.getItems().addListener(new ListChangeListener<RfortOmission>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends RfortOmission> c) {
				resetOmissions_.setDisable(omissions_.getItems().isEmpty());
			}
		});
		omissions_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		omissions_.setPlaceholder(new Label("No omission added."));

		// setup pilot points table
		pilotPointList_.addListener(new ListChangeListener<RfortPilotPoint>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends RfortPilotPoint> c) {
				resetPilotPoints_.setDisable(pilotPointList_.isEmpty());
			}
		});
		pilotPoints_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		pilotPoints_.setPlaceholder(new Label("No pilot point added."));
		pilotPoints_.setItems(pilotPointList_);

		// add field listeners
		refDP_.setDefaultValue(1327.0);
		dpFac_.setDefaultValue(1.0);
		overallFac_.setDefaultValue(1.0);
		rotation_.setDefaultValue(0);
		runTillFlight_.setDefaultValue(null);
		runTillFlight_.setMinimumValue(1, true);
		runTillFlight_.setAsOptionalInput(true);

		// add stress components
		stressComponent_.setItems(FXCollections.observableArrayList(StressComponent.values()));
		stressComponent_.getSelectionModel().select(0);

		// add listener
		stressComponent_.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<StressComponent>() {

			@Override
			public void changed(ObservableValue<? extends StressComponent> observable, StressComponent oldValue, StressComponent newValue) {
				rotation_.setDisable(!newValue.equals(StressComponent.ROTATED));
			}
		});

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

		// request materials
		IsamiVersion isamiVersion = (IsamiVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_VERSION);
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetFatigueMaterials(pilotPointsPopup_, isamiVersion));
		tm.runTaskInParallel(new GetPreffasMaterials(pilotPointsPopup_, isamiVersion));
		tm.runTaskInParallel(new GetLinearMaterials(pilotPointsPopup_, isamiVersion));
	}

	@Override
	public String getHeader() {
		return "RFORT Extended " + RfortAnalysis.VERSION;
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// create input
		RfortExtendedInput input = new RfortExtendedInput();

		// set pilot points
		for (RfortPilotPoint pp : pilotPointList_) {
			input.addPilotPoint(pp);
		}

		// check inputs
		if (!checkInputs(input.getPilotPoints()))
			return;

		// set input files
		input.setANAFile((Path) ana_.getUserData());
		input.setCVTFile((Path) cvt_.getUserData());
		input.setFLSFile((Path) fls_.getUserData());
		input.setXLSFile((Path) xls_.getUserData());
		input.setTXTFile((Path) txt_.getUserData());
		input.setConversionTableSheet(sheet_.getSelectionModel().getSelectedItem());

		// set options
		input.setAddDP(addDP_.isSelected());
		input.setRefDP(Double.parseDouble(refDP_.getText()));
		input.setDPFactor(Double.parseDouble(dpFac_.getText()));
		input.setOverallFactor(Double.parseDouble(overallFac_.getText()));
		input.setComponent(stressComponent_.getSelectionModel().getSelectedItem());
		input.setRotation(Integer.parseInt(rotation_.getText()));
		input.setRunTillFlight(runTillFlight_.getText().isEmpty() ? null : Integer.parseInt(runTillFlight_.getText()));
		input.setEnableSlogMode(enableSlog_.isSelected());
		input.setTargetFlights(targetFlights_.getText());

		// set omissions
		for (RfortOmission omission : omissions_.getItems()) {
			input.addOmission(omission);
		}

		// set analysis types
		input.setAnalysisTypes(fatigueAnalysis_.isSelected(), preffasAnalysis_.isSelected(), linearAnalysis_.isSelected());

		// get analysis engine
		AnalysisEngine engine = (AnalysisEngine) owner_.getOwner().getSettings().getValue(Settings.ANALYSIS_ENGINE);
		IsamiVersion isamiVersion = (IsamiVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_VERSION);
		IsamiSubVersion isamiSubVersion = (IsamiSubVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_SUB_VERSION);
		boolean applyCompression = (boolean) owner_.getOwner().getSettings().getValue(Settings.APPLY_COMPRESSION);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new CreateRfortAnalysis(input, engine).setIsamiEngineInputs(isamiVersion, isamiSubVersion, applyCompression));
		}
		else {
			tm.runTaskInParallel(new SaveTask(new CreateRfortAnalysis(input, engine).setIsamiEngineInputs(isamiVersion, isamiSubVersion, applyCompression), scheduleDate));
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Returns pilot point popup node.
	 *
	 * @return Pilot point popup node.
	 */
	public Node getPilotPointPopupNode() {
		return addPilotPoints_;
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
	public void addOmissions(RfortOmission... omissions) {

		// get current omissions
		ObservableList<RfortOmission> current = omissions_.getItems();

		// loop over omissions
		for (RfortOmission omission : omissions) {

			// get index of omission
			int index = current.indexOf(omission);

			// add
			if (index == -1) {
				current.add(omission);
			}
			else {
				current.set(index, omission);
			}
		}
	}

	@FXML
	private void onPercentageOmissionsClicked() {
		addPercentOmissionsPopup_.show(addOmissions_);
	}

	@FXML
	private void onOmissionValuesClicked() {

		// get pilot point names
		ArrayList<String> pps = new ArrayList<>();
		for (RfortPilotPoint pp : pilotPoints_.getItems()) {
			if (pp.isIncludeInRfort()) {
				pps.add(pp.getName());
			}
		}

		// show popup
		addDirectOmissionsPopup_.show(pps, addOmissions_);
	}

	@FXML
	private void onRemoveOmissionsClicked() {
		omissions_.getItems().removeAll(omissions_.getSelectionModel().getSelectedItems());
	}

	@FXML
	private void onResetOmissionsClicked() {
		omissions_.getItems().clear();
	}

	@FXML
	private void onBrowseInputsClicked() {

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
		processInputFiles(files);
	}

	@FXML
	private void onResetPilotPointsClicked() {
		pilotPointList_.clear();
	}

	@FXML
	private void onAddPilotPointsClicked() {
		pilotPointsPopup_.show(fatigueAnalysis_.isSelected(), preffasAnalysis_.isSelected(), linearAnalysis_.isSelected());
	}

	@FXML
	private void onDragOverInput(DragEvent event) {

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
				if (fileType.equals(FileType.ANA) || fileType.equals(FileType.GZ) || fileType.equals(FileType.ZIP) || fileType.equals(FileType.CVT) || fileType.equals(FileType.FLS) || fileType.equals(FileType.XLS) || fileType.equals(FileType.TXT)) {
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
				if (fileType.equals(FileType.ANA) || fileType.equals(FileType.GZ) || fileType.equals(FileType.ZIP) || fileType.equals(FileType.CVT) || fileType.equals(FileType.FLS) || fileType.equals(FileType.XLS) || fileType.equals(FileType.TXT)) {
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
		if ((event.getGestureSource() != dropZone_) && db.hasFiles()) {

			// process files
			success = processInputFiles(db.getFiles());
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}

	/**
	 * Called when add delta-p option is selected.
	 *
	 * @param isSelected
	 *            True if the option is selected.
	 */
	private void onAddDPSelected(boolean isSelected) {
		refDP_.setDisable(!isSelected);
		dpFac_.setDisable(!isSelected);
		if (!isSelected) {
			refDP_.reset();
			dpFac_.reset();
		}
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("RFORT Extended", null);
	}

	@FXML
	private void onDownloadSampleInputClicked() {
		owner_.getOwner().downloadSampleInput("Rfort");
	}

	@FXML
	private void onResetClicked() {

		// clear all files
		ImageView[] items = { ana_, txt_, cvt_, fls_, xls_ };
		for (ImageView item : items) {
			item.setUserData(null);
			item.setImage(AddSpectrumPanel.EMPTY);
		}

		// reset hyperlink
		browseInputs_.setVisited(false);

		// reset worksheet
		sheet_.getSelectionModel().clearSelection();
		sheet_.setValue(null);
		sheet_.getItems().clear();
		sheet_.setDisable(true);

		// reset analysis types
		preffasAnalysis_.setSelected(false);
		linearAnalysis_.setSelected(false);

		// reset pilot points
		pilotPoints_.getSelectionModel().clearSelection();
		pilotPointList_.clear();

		// reset omission levels
		omissions_.getSelectionModel().clearSelection();
		omissions_.getItems().clear();

		// reset options
		addDP_.setSelected(false);
		refDP_.reset();
		refDP_.setDisable(true);
		dpFac_.reset();
		dpFac_.setDisable(true);
		overallFac_.reset();
		stressComponent_.getSelectionModel().select(StressComponent.NORMAL_X);
		rotation_.reset();
		runTillFlight_.reset();
		targetFlights_.clear();
		enableSlog_.setSelected(false);

		// expand first panel
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
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
	 * Checks inputs.
	 *
	 * @param pps
	 *            Pilot points.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(ArrayList<SerializableRfortPilotPoint> pps) {

		// check ANA file
		Path anaFile = (Path) ana_.getUserData();
		if ((anaFile == null) || !Files.exists(anaFile)) {
			String message = "Please select a valid ANA file to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(ana_);
			return false;
		}

		// check CVT file
		Path cvtFile = (Path) cvt_.getUserData();
		if ((cvtFile == null) || !Files.exists(cvtFile)) {
			String message = "Please select a valid CVT file to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(cvt_);
			return false;
		}

		// check FLS file
		Path flsFile = (Path) fls_.getUserData();
		if ((flsFile == null) || !Files.exists(flsFile)) {
			String message = "Please select a valid FLS file to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(fls_);
			return false;
		}

		// check XLS file
		Path xlsFile = (Path) xls_.getUserData();
		if ((xlsFile == null) || !Files.exists(xlsFile)) {
			String message = "Please select a valid conversion table to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(xls_);
			return false;
		}

		// check TXT file
		Path txtFile = (Path) txt_.getUserData();
		if ((txtFile == null) || !Files.exists(txtFile)) {
			String message = "Please select a valid TXT file to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(txt_);
			return false;
		}

		// no conversion table sheet selected
		if (sheet_.getSelectionModel().isEmpty()) {
			String message = "Please select a conversion table sheet to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(sheet_);
			return false;
		}

		// add delta-P selected
		if (addDP_.isSelected()) {

			// check reference DP
			String message = refDP_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(refDP_);
				return false;
			}

			// check reference DP
			message = dpFac_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(dpFac_);
				return false;
			}
		}

		// check rotation angle
		if (stressComponent_.getSelectionModel().getSelectedItem().equals(StressComponent.ROTATED)) {
			String message = rotation_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(rotation_);
				return false;
			}
		}

		// check run till flight
		String message = runTillFlight_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(runTillFlight_);
			return false;
		}

		// check target flights
		String targetFlights = targetFlights_.getText();
		if ((targetFlights != null) && !targetFlights.isEmpty()) {

			// multiple flight numbers
			if (targetFlights.contains(",")) {

				// loop over flight numbers
				String[] split = targetFlights.split(",");
				for (String number : split) {

					// cast to integer
					try {
						Integer.parseInt(number.trim());
					}

					// invalid flight number
					catch (NumberFormatException e1) {
						message = "Please enter valid flight numbers.";
						PopOver popOver = new PopOver();
						popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
						popOver.setDetachable(false);
						popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
						popOver.setHideOnEscape(true);
						popOver.setAutoHide(true);
						popOver.show(targetFlights_);
						return false;
					}
				}
			}

			// single flight number
			else {

				// cast to integer
				try {
					Integer.parseInt(targetFlights);
				}

				// invalid flight number
				catch (NumberFormatException e1) {
					message = "Please enter a valid flight number.";
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(targetFlights_);
					return false;
				}
			}
		}

		// check pilot points
		if ((pps == null) || pps.isEmpty()) {
			message = "Please add at least 1 pilot point (STF file) to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(pilotPoints_);
			return false;
		}
		for (SerializableRfortPilotPoint pp : pps) {
			if (!pp.getFile().exists()) {
				message = "Pilot point '" + pp.getName() + "' doesn't exist. Please select valid pilot points.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(pilotPoints_);
				return false;
			}
		}

		// check omissions
		if (omissions_.getItems().isEmpty()) {
			message = "Please add at least 1 omission to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(omissions_);
			return false;
		}

		// check passed
		return true;
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

			// CVT
			else if (fileType.equals(FileType.CVT)) {
				owner_.getOwner().setInitialDirectory(file);
				cvt_.setUserData(file.toPath());
				if (!toBeAnimated.contains(cvt_)) {
					toBeAnimated.add(cvt_);
				}
				success = true;
			}

			// FLS
			else if (fileType.equals(FileType.FLS)) {
				owner_.getOwner().setInitialDirectory(file);
				fls_.setUserData(file.toPath());
				if (!toBeAnimated.contains(fls_)) {
					toBeAnimated.add(fls_);
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

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static RfortExtendedPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("RfortExtendedPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			RfortExtendedPanel controller = (RfortExtendedPanel) fxmlLoader.getController();

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
