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

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import control.validationField.DoubleValidationField;
import control.validationField.IntegerValidationField;
import equinox.controller.FatigueMaterialsPopup.FatigueMaterialAddingPanel;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.LoadcaseFactorsPopup.LoadcaseFactorAddingPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.controller.SegmentFactorsPopup.SegmentFactorAddingPanel;
import equinox.data.AnalysisEngine;
import equinox.data.DTInterpolation;
import equinox.data.EquinoxTheme;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.LoadcaseFactor;
import equinox.data.SegmentFactor;
import equinox.data.Settings;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.Spectrum;
import equinox.data.input.DamageAngleInput;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.data.material.FatigueMaterialItem;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.Material;
import equinox.font.IconicFont;
import equinox.task.BucketDamageAngleAnalysis;
import equinox.task.DamageAngleAnalysis;
import equinox.task.GetDeltaPInfo;
import equinox.task.GetDeltaPInfo.DeltaPInfoRequestingPanel;
import equinox.task.GetMaterials;
import equinox.task.GetMaterials.MaterialRequestingPanel;
import equinox.task.SaveTask;
import equinox.utility.IncrementAngleFieldListener;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

/**
 * Class for damage angle panel controller.
 *
 * @author Murat Artim
 * @date Aug 6, 2014
 * @time 3:00:40 PM
 */
public class DamageAnglePanel implements InternalInputSubPanel, DeltaPInfoRequestingPanel, SchedulingPanel, LoadcaseFactorAddingPanel, SegmentFactorAddingPanel, FatigueMaterialAddingPanel, MaterialRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** True if the loadcase/segment factors are enabled. */
	private boolean loadcaseSegmentFactorsEnabled_ = true;

	@FXML
	private VBox root_, iterationPanel_;

	@FXML
	private ToggleSwitch removeNegative_, omission_, generateData_;

	@FXML
	private TextField incrementAngle_;

	@FXML
	private Label dtLoadcaseInfLabel_, refDTValInfLabel_, dtLoadcaseSupLabel_, refDTValSupLabel_;

	@FXML
	private Button addFatigueMaterials_, removeFatigueMaterials_, resetFatigueMaterials_, addSegmentFactors_, removeSegmentFactors_, resetSegmentFactors_, addLoadcaseFactors_, removeLoadcaseFactors_, resetLoadcaseFactors_;

	@FXML
	private MenuButton onegMethod_, incMethod_, dpMethod_, dtMethod_;

	@FXML
	private ChoiceBox<DTInterpolation> dtInterpolation_;

	@FXML
	private ListView<FatigueMaterial> fatigueMaterials_;

	@FXML
	private ListView<SegmentFactor> segmentFactors_;

	@FXML
	private ListView<LoadcaseFactor> loadcaseFactors_;

	@FXML
	private TitledPane loadcasesPane_, segmentsPane_;

	@FXML
	private Accordion accordion1_, accordion2_;

	@FXML
	private SplitMenuButton ok_;

	@FXML
	private DoubleValidationField omissionLevel_, oneG_, increment_, deltaP_, deltaT_, refDPVal_, refDTValSup_, refDTValInf_;

	@FXML
	private IntegerValidationField dpLoadcase_, dtLoadcaseInf_, dtLoadcaseSup_;

	@FXML
	private Spinner<Integer> startAngle_, endAngle_;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		// bind components
		omissionLevel_.disableProperty().bind(omission_.selectedProperty().not());
		fatigueMaterials_.getSelectionModel().getSelectedItems().addListener((ListChangeListener<FatigueMaterial>) c -> removeFatigueMaterials_.setDisable(fatigueMaterials_.getSelectionModel().getSelectedItems().isEmpty()));
		fatigueMaterials_.getItems().addListener((ListChangeListener<FatigueMaterial>) c -> resetFatigueMaterials_.setDisable(fatigueMaterials_.getItems().isEmpty()));
		segmentFactors_.getSelectionModel().getSelectedItems().addListener((ListChangeListener<SegmentFactor>) c -> removeSegmentFactors_.setDisable(segmentFactors_.getSelectionModel().getSelectedItems().isEmpty()));
		segmentFactors_.getItems().addListener((ListChangeListener<SegmentFactor>) c -> resetSegmentFactors_.setDisable(segmentFactors_.getItems().isEmpty()));
		loadcaseFactors_.getSelectionModel().getSelectedItems().addListener((ListChangeListener<LoadcaseFactor>) c -> removeLoadcaseFactors_.setDisable(loadcaseFactors_.getSelectionModel().getSelectedItems().isEmpty()));
		loadcaseFactors_.getItems().addListener((ListChangeListener<LoadcaseFactor>) c -> resetLoadcaseFactors_.setDisable(loadcaseFactors_.getItems().isEmpty()));

		// set place holders for lists
		fatigueMaterials_.setPlaceholder(new Label("No fatigue material added."));
		segmentFactors_.setPlaceholder(new Label("No segment factor added."));
		loadcaseFactors_.setPlaceholder(new Label("No loadcase factor added."));

		// enable multiple selection for lists
		fatigueMaterials_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		segmentFactors_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		loadcaseFactors_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// setup double validation fields
		omissionLevel_.setDefaultValue(0.0);
		omissionLevel_.setMinimumValue(0.0, true);
		oneG_.setDefaultValue(1.0);
		increment_.setDefaultValue(1.0);
		deltaP_.setDefaultValue(1.0);
		deltaT_.setDefaultValue(1.0);
		refDPVal_.setDefaultValue(null);
		refDPVal_.setAsOptionalInput(true);
		refDTValSup_.setDefaultValue(null);
		refDTValInf_.setDefaultValue(null);

		// setup integer validation fields
		dpLoadcase_.setDefaultValue(null);
		dpLoadcase_.setAsOptionalInput(true);
		dtLoadcaseInf_.setDefaultValue(null);
		dtLoadcaseSup_.setDefaultValue(null);

		// add DT interpolations
		dtInterpolation_.setItems(FXCollections.observableArrayList(DTInterpolation.values()));
		dtInterpolation_.getSelectionModel().select(0);

		// add listener
		dtInterpolation_.getSelectionModel().selectedItemProperty().addListener((ChangeListener<DTInterpolation>) (observable, oldValue, newValue) -> {

			// null
			if (newValue == null)
				return;

			// setup components
			interpolationSelected(newValue);
		});

		// setup spinners
		IntegerSpinnerValueFactory startFactory = new IntegerSpinnerValueFactory(0, 180, 0, 10);
		IntegerSpinnerValueFactory endFactory = new IntegerSpinnerValueFactory(0, 180, 180, 10);
		startFactory.maxProperty().bind(endFactory.valueProperty());
		endFactory.minProperty().bind(startFactory.valueProperty());
		startAngle_.setValueFactory(startFactory);
		endAngle_.setValueFactory(endFactory);

		// add listener to increment angle
		incrementAngle_.textProperty().addListener(new IncrementAngleFieldListener(incrementAngle_, startAngle_, endAngle_));

		// expand first panes
		accordion1_.setExpandedPane(accordion1_.getPanes().get(0));
		accordion2_.setExpandedPane(accordion2_.getPanes().get(0));
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

		// loadcase/segment modifiers not enabled
		if (!loadcaseSegmentFactorsEnabled_) {
			loadcasesPane_.setExpanded(false);
			loadcasesPane_.setDisable(true);
			segmentsPane_.setExpanded(false);
			segmentsPane_.setDisable(true);
		}

		// loadcase/segment modifiers enabled
		else {
			loadcasesPane_.setDisable(false);
			segmentsPane_.setDisable(false);
		}

		// reset panel
		onResetClicked();
	}

	@Override
	public String getHeader() {
		return "Damage Angle Analysis";
	}

	@Override
	public void setDeltaPInfo(String dpLoadcase, Double refDP) {
		refDPVal_.setText(refDP == null ? null : refDP.toString());
		dpLoadcase_.setText(dpLoadcase);
	}

	@Override
	public void addLoadcaseFactors(LoadcaseFactor[] loadcaseFactors) {
		for (LoadcaseFactor lf : loadcaseFactors) {
			if (!loadcaseFactors_.getItems().contains(lf)) {
				loadcaseFactors_.getItems().add(lf);
			}
			else {
				int index = loadcaseFactors_.getItems().indexOf(lf);
				loadcaseFactors_.getItems().set(index, lf);
			}
		}
	}

	@Override
	public Node getLoadcaseFactorPopupNode() {
		return addLoadcaseFactors_;
	}

	@Override
	public void addSegmentFactors(SegmentFactor[] segmentFactors) {
		for (SegmentFactor sf : segmentFactors) {
			if (!segmentFactors_.getItems().contains(sf)) {
				segmentFactors_.getItems().add(sf);
			}
			else {
				int index = segmentFactors_.getItems().indexOf(sf);
				segmentFactors_.getItems().set(index, sf);
			}
		}
	}

	@Override
	public Node getSegmentFactorPopupNode() {
		return addSegmentFactors_;
	}

	@Override
	public void addFatigueMaterials(ObservableList<FatigueMaterialItem> materials) {
		for (FatigueMaterialItem item : materials) {
			FatigueMaterial material = item.getMaterial();
			if (!fatigueMaterials_.getItems().contains(material)) {
				fatigueMaterials_.getItems().add(material);
			}
			else {
				int index = fatigueMaterials_.getItems().indexOf(material);
				fatigueMaterials_.getItems().set(index, material);
			}
		}
	}

	@Override
	public Node getFatigueMaterialPopupNode() {
		return addFatigueMaterials_;
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// check inputs
		if (!checkInputs(runNow, scheduleDate))
			return;

		// start
		start(runNow, scheduleDate);
	}

	@Override
	public void setMaterials(Material[] materials) {
		if (materials[GetMaterials.FATIGUE_MATERIAL] != null) {
			fatigueMaterials_.getItems().add((FatigueMaterial) materials[GetMaterials.FATIGUE_MATERIAL]);
		}
	}

	/**
	 * Sets loadcase/segment factors enabled or disabled.
	 *
	 * @param enable
	 *            True to enable.
	 */
	public void enableLoadcaseSegmentFactors(boolean enable) {
		loadcaseSegmentFactorsEnabled_ = enable;
	}

	/**
	 * Creates and starts the task.
	 *
	 * @param runNow
	 *            True if task(s) should be run right now.
	 * @param scheduleDate
	 *            Schedule date (can be null).
	 */
	private void start(boolean runNow, Date scheduleDate) {

		// create input
		DamageAngleInput input = new DamageAngleInput();

		// set overall stress modifiers
		TextField[] values = { oneG_, increment_, deltaP_, deltaT_ };
		MenuButton[] methods = { onegMethod_, incMethod_, dpMethod_, dtMethod_ };
		for (int i = 0; i < values.length; i++) {
			for (MenuItem method : methods[i].getItems()) {
				if (((RadioMenuItem) method).isSelected()) {
					input.setStressModifier(i, Double.parseDouble(values[i].getText()), method.getText());
					break;
				}
			}
		}

		// set loadcase factors
		if (!loadcaseFactors_.getItems().isEmpty()) {
			input.setLoadcaseFactors(loadcaseFactors_.getItems());
		}

		// set segment factors
		if (!segmentFactors_.getItems().isEmpty()) {
			input.setSegmentFactors(segmentFactors_.getItems());
		}

		// set delta-p values
		String dpLoadcase = dpLoadcase_.getText() == null || dpLoadcase_.getText().isEmpty() ? null : dpLoadcase_.getText();
		Double refDP = refDPVal_.getText() == null || refDPVal_.getText().isEmpty() ? null : Double.parseDouble(refDPVal_.getText());
		input.setDPLoadcase(dpLoadcase);
		input.setReferenceDP(refDP);

		// set delta-t values
		String dtLoadcaseSup = dtLoadcaseSup_.getText() == null || dtLoadcaseSup_.getText().isEmpty() ? null : dtLoadcaseSup_.getText();
		Double refDTSup = refDTValSup_.getText() == null || refDTValSup_.getText().isEmpty() ? null : Double.parseDouble(refDTValSup_.getText());
		String dtLoadcaseInf = dtLoadcaseInf_.getText() == null || dtLoadcaseInf_.getText().isEmpty() ? null : dtLoadcaseInf_.getText();
		Double refDTInf = refDTValInf_.getText() == null || refDTValInf_.getText().isEmpty() ? null : Double.parseDouble(refDTValInf_.getText());
		DTInterpolation dtInterpolation = dtInterpolation_.getSelectionModel().getSelectedItem();
		input.setDTInterpolation(dtInterpolation);
		input.setDTLoadcaseSup(dtLoadcaseSup);
		input.setReferenceDTSup(refDTSup);
		input.setDTLoadcaseInf(dtLoadcaseInf);
		input.setReferenceDTInf(refDTInf);

		// set iteration settings
		int start = startAngle_.getValue();
		int end = endAngle_.getValue();
		int inc = Integer.parseInt(incrementAngle_.getText());
		input.setIterationAngles(start, end, inc);

		// set omission parameters
		input.setRemoveNegativeStresses(removeNegative_.isSelected());
		input.setApplyOmission(omission_.isSelected());
		double omissionLevel = !omission_.isSelected() ? 0.0 : Double.parseDouble(omissionLevel_.getText());
		input.setOmissionLevel(omissionLevel);

		// set post-processing options
		input.setGenerateMaxdamData(generateData_.isSelected());

		// get analysis engine
		AnalysisEngine engine = (AnalysisEngine) owner_.getOwner().getSettings().getValue(Settings.ANALYSIS_ENGINE);
		IsamiVersion isamiVersion = (IsamiVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_VERSION);
		IsamiSubVersion isamiSubVersion = (IsamiSubVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_SUB_VERSION);
		boolean applyCompression = (boolean) owner_.getOwner().getSettings().getValue(Settings.APPLY_COMPRESSION);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// loop over selected files
		for (TreeItem<String> item : owner_.getSelectedFiles()) {

			// loop over materials
			for (FatigueMaterial material : fatigueMaterials_.getItems()) {

				// STF file
				if (item instanceof STFFile) {

					// run now
					if (runNow) {
						tm.runTaskInParallel(new DamageAngleAnalysis((STFFile) item, input, material, engine).setIsamiEngineInputs(isamiVersion, isamiSubVersion, applyCompression));
					}
					else {
						tm.runTaskInParallel(new SaveTask(new DamageAngleAnalysis((STFFile) item, input, material, engine).setIsamiEngineInputs(isamiVersion, isamiSubVersion, applyCompression), scheduleDate));
					}
				}

				// STF file bucket
				else if (item instanceof STFFileBucket) {

					// run now
					if (runNow) {
						tm.runTaskSequentially(new BucketDamageAngleAnalysis((STFFileBucket) item, input, material, engine).setIsamiEngineInputs(isamiVersion, isamiSubVersion, applyCompression));
					}
					else {
						tm.runTaskInParallel(new SaveTask(new BucketDamageAngleAnalysis((STFFileBucket) item, input, material, engine).setIsamiEngineInputs(isamiVersion, isamiSubVersion, applyCompression), scheduleDate));
					}
				}
			}
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Checks inputs.
	 *
	 * @param runNow
	 *            True if task(s) should be run right now.
	 * @param scheduleDate
	 *            Schedule date (can be null).
	 * @return True if inputs are valid.
	 */
	private boolean checkInputs(boolean runNow, Date scheduleDate) {

		// start-end angles same
		if (startAngle_.getValue() == endAngle_.getValue()) {
			String message = "Start angle must be smaller than the end angle. Please supply valid values.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(startAngle_);
			return false;
		}

		// no fatigue material added
		if (fatigueMaterials_.getItems().isEmpty()) {
			String message = "No fatigue material added. Please add at least 1 fatigue material to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(fatigueMaterials_);
			return false;
		}

		// check 1g stress modifier
		String message = oneG_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(oneG_);
			return false;
		}

		// check increment stress modifier
		message = increment_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(increment_);
			return false;
		}

		// check DP stress modifier
		message = deltaP_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(deltaP_);
			return false;
		}

		// check DT stress modifier
		message = deltaT_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(deltaT_);
			return false;
		}

		// check reference DP loadcase
		message = dpLoadcase_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(dpLoadcase_);
			return false;
		}

		// check reference DP value
		message = refDPVal_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(refDPVal_);
			return false;
		}

		// get DT interpolation
		DTInterpolation dtInterpolation = dtInterpolation_.getSelectionModel().getSelectedItem();

		// one or two points
		if (dtInterpolation.equals(DTInterpolation.ONE_POINT) || dtInterpolation.equals(DTInterpolation.TWO_POINTS)) {

			// check DT Sup loadcase
			message = dtLoadcaseSup_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(dtLoadcaseSup_);
				return false;
			}

			// check DT Sup value
			message = refDTValSup_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(refDTValSup_);
				return false;
			}
		}

		// two points
		else if (dtInterpolation.equals(DTInterpolation.TWO_POINTS)) {

			// check DT Inf loadcase
			message = dtLoadcaseInf_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(dtLoadcaseInf_);
				return false;
			}

			// check DT Inf value
			message = refDTValInf_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(refDTValInf_);
				return false;
			}
		}

		// check omission level
		if (omission_.isSelected()) {
			message = omissionLevel_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(omissionLevel_);
				return false;
			}
		}

		// check delta-p values
		if (getSelectedSpectrum() != null) {

			// no delta-p loadcase given
			String dpLoadcase = dpLoadcase_.getText();
			if (dpLoadcase == null || dpLoadcase.trim().isEmpty()) {

				// create confirmation action
				PopOver popOver = new PopOver();
				EventHandler<ActionEvent> handler = event -> {
					popOver.hide();
					start(runNow, scheduleDate);
				};

				// show question
				String warning = "No delta-p loadcase supplied. Do you want to proceed without delta-p loadcase?";
				popOver.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel2.load(popOver, warning, 50, "Yes", handler, NotificationPanel2.QUESTION));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(ok_);
				return false;
			}

			// no reference delta-p given
			String refDP = refDPVal_.getText();
			if (refDP == null || refDP.trim().isEmpty()) {

				// create confirmation action
				PopOver popOver = new PopOver();
				EventHandler<ActionEvent> handler = event -> {
					popOver.hide();
					start(runNow, scheduleDate);
				};

				// show question
				String warning = "No reference delta-p value supplied. Do you want to proceed without reference delta-p value?";
				popOver.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel2.load(popOver, warning, 50, "Yes", handler, NotificationPanel2.QUESTION));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(ok_);
				return false;
			}
		}

		// valid inputs
		return true;
	}

	@FXML
	private void onAddLoadcaseFactorsClicked() {

		// get spectrum from selected items
		Spectrum spectrum = null;
		TreeItem<String> selected = owner_.getSelectedFiles().get(0);
		if (selected instanceof STFFile) {
			spectrum = ((STFFile) selected).getParentItem();
		}
		else if (selected instanceof STFFileBucket) {
			spectrum = ((STFFileBucket) selected).getParentItem();
		}

		// show popup
		((LoadcaseFactorsPopup) owner_.getPopup(InputPanel.LOADCASE_FACTORS_POPUP)).show(this, spectrum);
	}

	@FXML
	private void onRemoveLoadcaseFactorsClicked() {
		loadcaseFactors_.getItems().removeAll(loadcaseFactors_.getSelectionModel().getSelectedItems());
	}

	@FXML
	private void onResetLoadcaseFactorsClicked() {
		loadcaseFactors_.getItems().clear();
	}

	@FXML
	private void onAddSegmentFactorsClicked() {

		// get spectrum from selected items
		Spectrum spectrum = null;
		TreeItem<String> selected = owner_.getSelectedFiles().get(0);
		if (selected instanceof STFFile) {
			spectrum = ((STFFile) selected).getParentItem();
		}
		else if (selected instanceof STFFileBucket) {
			spectrum = ((STFFileBucket) selected).getParentItem();
		}

		// show popup
		((SegmentFactorsPopup) owner_.getPopup(InputPanel.SEGMENT_FACTORS_POPUP)).show(this, spectrum);
	}

	@FXML
	private void onRemoveSegmentFactorsClicked() {
		segmentFactors_.getItems().removeAll(segmentFactors_.getSelectionModel().getSelectedItems());
	}

	@FXML
	private void onResetSegmentFactorsClicked() {
		segmentFactors_.getItems().clear();
	}

	@FXML
	private void onAddFatigueMaterialsClicked() {
		((FatigueMaterialsPopup) owner_.getPopup(InputPanel.FATIGUE_MATERIALS_POPUP)).show(this, ArrowLocation.LEFT_TOP, false);
	}

	@FXML
	private void onRemoveFatigueMaterialsClicked() {
		fatigueMaterials_.getItems().removeAll(fatigueMaterials_.getSelectionModel().getSelectedItems());
	}

	@FXML
	private void onResetFatigueMaterialsClicked() {
		fatigueMaterials_.getItems().clear();
	}

	@FXML
	private void onResetClicked() {

		// reset iteration settings
		incrementAngle_.setText("10");
		startAngle_.getValueFactory().setValue(0);
		endAngle_.getValueFactory().setValue(180);

		// reset overall stress factors
		oneG_.reset();
		increment_.reset();
		deltaP_.reset();
		deltaT_.reset();
		MenuButton[] buttons = { onegMethod_, incMethod_, dpMethod_, dtMethod_ };
		for (MenuButton button : buttons) {
			button.setText(GenerateStressSequenceInput.MULTIPLY);
			for (MenuItem item : button.getItems()) {
				RadioMenuItem radio = (RadioMenuItem) item;
				if (radio.getText().equals(GenerateStressSequenceInput.MULTIPLY)) {
					radio.setSelected(true);
					break;
				}
			}
		}

		// reset reference values
		dpLoadcase_.reset();
		refDPVal_.reset();
		dtInterpolation_.getSelectionModel().select(0);

		// reset omission inputs
		removeNegative_.setSelected(false);
		omission_.setSelected(true);
		omissionLevel_.reset();

		// reset segment factors
		segmentFactors_.getSelectionModel().clearSelection();
		segmentFactors_.getItems().clear();

		// reset loadcase factors
		loadcaseFactors_.getSelectionModel().clearSelection();
		loadcaseFactors_.getItems().clear();

		// reset post-processing options
		generateData_.setSelected(false);

		// expand first panes
		accordion1_.setExpandedPane(accordion1_.getPanes().get(0));
		accordion2_.setExpandedPane(accordion2_.getPanes().get(0));

		// request delta-p info
		requestDeltaPInfo();

		// request material info
		fatigueMaterials_.getSelectionModel().clearSelection();
		fatigueMaterials_.getItems().clear();
		IsamiVersion isamiVersion = (IsamiVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_VERSION);
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetMaterials(owner_.getSelectedFiles(), this, isamiVersion));
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

	@SuppressWarnings("static-method")
	@FXML
	private void onMethodSelected(ActionEvent e) {
		RadioMenuItem item = (RadioMenuItem) e.getSource();
		MenuButton owner = (MenuButton) item.getParentPopup().getOwnerNode();
		owner.setText(item.getText());
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to perform damage angle analysis", null);
	}

	/**
	 * Requests delta-p info only if all selected STF files are from the same STF file.
	 */
	private void requestDeltaPInfo() {

		// get spectrum
		Spectrum spectrum = getSelectedSpectrum();

		// selected STF files are from the same spectrum
		if (spectrum != null) {

			// get delta-p info
			ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
			tm.runTaskInParallel(new GetDeltaPInfo(spectrum, this));
		}
	}

	/**
	 * Returns the spectrum of selected STF files, or null if STF files are not from the same spectrum.
	 *
	 * @return The spectrum of selected STF files, or null if STF files are not from the same spectrum.
	 */
	private Spectrum getSelectedSpectrum() {

		// initialize spectrum
		Spectrum spectrum = null;

		// loop over selected files
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();
		for (TreeItem<String> item : selected) {

			// initialize spectrum
			Spectrum set = null;

			// STF file
			if (item instanceof STFFile) {
				set = ((STFFile) item).getParentItem();
			}
			else if (item instanceof STFFileBucket) {
				set = ((STFFileBucket) item).getParentItem();
			}

			// selected STF files are not from the same spectrum
			if (spectrum != null && !spectrum.equals(set)) {
				spectrum = null;
				break;
			}

			// update spectrum
			spectrum = set;
		}

		// return spectrum
		return spectrum;
	}

	/**
	 * Called when a new DT interpolation is selected.
	 *
	 * @param interpolation
	 *            Selected interpolation.
	 */
	private void interpolationSelected(DTInterpolation interpolation) {

		// none
		if (interpolation.equals(DTInterpolation.NONE)) {
			dtLoadcaseInf_.reset();
			refDTValInf_.reset();
			dtLoadcaseSup_.reset();
			refDTValSup_.reset();
			dtLoadcaseInfLabel_.setDisable(true);
			refDTValInfLabel_.setDisable(true);
			dtLoadcaseSupLabel_.setDisable(true);
			refDTValSupLabel_.setDisable(true);
			dtLoadcaseInf_.setDisable(true);
			refDTValInf_.setDisable(true);
			dtLoadcaseSup_.setDisable(true);
			refDTValSup_.setDisable(true);
		}

		// 1 point
		else if (interpolation.equals(DTInterpolation.ONE_POINT)) {
			dtLoadcaseSup_.reset();
			refDTValSup_.reset();
			dtLoadcaseInfLabel_.setDisable(true);
			refDTValInfLabel_.setDisable(true);
			dtLoadcaseSupLabel_.setDisable(false);
			refDTValSupLabel_.setDisable(false);
			dtLoadcaseInf_.setDisable(true);
			refDTValInf_.setDisable(true);
			dtLoadcaseSup_.setDisable(false);
			refDTValSup_.setDisable(false);
			dtLoadcaseSupLabel_.setText("DT load case:");
			refDTValSupLabel_.setText("Reference DT:");
		}

		// 2 points
		else if (interpolation.equals(DTInterpolation.TWO_POINTS)) {
			dtLoadcaseInfLabel_.setDisable(false);
			refDTValInfLabel_.setDisable(false);
			dtLoadcaseSupLabel_.setDisable(false);
			refDTValSupLabel_.setDisable(false);
			dtLoadcaseInf_.setDisable(false);
			refDTValInf_.setDisable(false);
			dtLoadcaseSup_.setDisable(false);
			refDTValSup_.setDisable(false);
			dtLoadcaseInfLabel_.setText("DT load case (Inf.):");
			refDTValInfLabel_.setText("Reference DT (Inf.):");
			dtLoadcaseSupLabel_.setText("DT load case (Sup.):");
			refDTValSupLabel_.setText("Reference DT (Sup.):");
		}
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static DamageAnglePanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DamageAnglePanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			DamageAnglePanel controller = (DamageAnglePanel) fxmlLoader.getController();

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
