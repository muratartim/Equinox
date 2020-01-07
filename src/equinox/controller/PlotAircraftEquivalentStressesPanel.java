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
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import control.validationField.DoubleValidationField;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.ElementStress;
import equinox.data.ElementTypeForStress;
import equinox.data.EquinoxTheme;
import equinox.data.ReferenceLoadCase;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.input.AircraftEquivalentStressType;
import equinox.data.input.PlotAircraftEquivalentStressesInput;
import equinox.data.ui.PlotContour;
import equinox.task.GetElementGroups;
import equinox.task.GetElementGroups.ElementGroupsRequestingPanel;
import equinox.task.GetMissions;
import equinox.task.GetMissions.MissionRequestingPanel;
import equinox.task.GetReferenceLoadCases;
import equinox.task.GetReferenceLoadCases.ReferenceLoadCaseRequestingPanel;
import equinox.utility.SpinnerListener;
import equinox.utility.Utility;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

/**
 * Class for plot A/C equivalent stresses panel controller.
 *
 * @author Murat Artim
 * @date Sep 10, 2015
 * @time 2:48:21 PM
 */
public class PlotAircraftEquivalentStressesPanel implements InternalInputSubPanel, ElementGroupsRequestingPanel, ReferenceLoadCaseRequestingPanel, MissionRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ChoiceBox<PlotContour> contour_;

	@FXML
	private ComboBox<ElementTypeForStress> elementType_;

	@FXML
	private ComboBox<AircraftEquivalentStressType> stress_;

	@FXML
	private ComboBox<ElementStress> refStressComp_;

	@FXML
	private ComboBox<ReferenceLoadCase> onegLC_, dpLC_, vgLC_;

	@FXML
	private ComboBox<String> mission_;

	@FXML
	private ToggleSwitch interpolate_, oneg_, dp_, vg_, outlines_, lowerBound_, upperBound_;

	@FXML
	private ListView<String> groups_;

	@FXML
	private Spinner<Integer> opacity_, beamExtrusion_, rodExtrusion_, numPPs_;

	@FXML
	private TitledPane stressPane_, loadCasePane_, dataSourcePane_;

	@FXML
	private RadioButton weightedNum_, weightedDist_, closest_;

	@FXML
	private DoubleValidationField distPPs_, onegLCFac_, dpLCFac_, vgLCFac_, lowerBoundVal_, upperBoundVal_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup plot contour
		contour_.setItems(FXCollections.observableArrayList(PlotContour.values()));
		contour_.getSelectionModel().select(PlotContour.SMOOTHED);

		// setup element types and stress components
		elementType_.setItems(FXCollections.observableArrayList(ElementTypeForStress.values()));
		stress_.setItems(FXCollections.observableArrayList(AircraftEquivalentStressType.values()));
		refStressComp_.setItems(FXCollections.observableArrayList(ElementStress.values()));

		// setup opacities
		opacity_.setValueFactory(new IntegerSpinnerValueFactory(0, 100, 100));
		opacity_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(opacity_));

		// set multiple selection for lists
		groups_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// add listeners to text fields
		distPPs_.setDefaultValue(100.0);
		distPPs_.setMinimumValue(0.0, false);
		onegLCFac_.setDefaultValue(1.0);
		dpLCFac_.setDefaultValue(1.0);
		vgLCFac_.setDefaultValue(1.0);
		lowerBoundVal_.setDefaultValue(0.0);
		upperBoundVal_.setDefaultValue(null);

		// setup settings
		beamExtrusion_.setValueFactory(new IntegerSpinnerValueFactory(10, 100, 50));
		beamExtrusion_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(beamExtrusion_));
		rodExtrusion_.setValueFactory(new IntegerSpinnerValueFactory(10, 100, 20));
		rodExtrusion_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(rodExtrusion_));
		numPPs_.setValueFactory(new IntegerSpinnerValueFactory(2, 100, 3));
		numPPs_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(numPPs_));

		// bind components
		loadCasePane_.disableProperty().bind(interpolate_.selectedProperty().not());
		dataSourcePane_.disableProperty().bind(interpolate_.selectedProperty().not());
		onegLC_.disableProperty().bind(oneg_.selectedProperty().not());
		dpLC_.disableProperty().bind(dp_.selectedProperty().not());
		vgLC_.disableProperty().bind(vg_.selectedProperty().not());
		onegLCFac_.disableProperty().bind(oneg_.selectedProperty().not());
		dpLCFac_.disableProperty().bind(dp_.selectedProperty().not());
		vgLCFac_.disableProperty().bind(vg_.selectedProperty().not());
		distPPs_.disableProperty().bind(weightedDist_.selectedProperty().not());
		numPPs_.disableProperty().bind(weightedNum_.selectedProperty().not());
		lowerBoundVal_.disableProperty().bind(lowerBound_.selectedProperty().not());
		upperBoundVal_.disableProperty().bind(upperBound_.selectedProperty().not());

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
	public String getHeader() {
		return "Plot Eq. Stresses";
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {

		// get selected equivalent stress
		AircraftFatigueEquivalentStress selected = (AircraftFatigueEquivalentStress) owner_.getSelectedFiles().get(0);

		// get element groups
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetElementGroups(this, selected.getParentItem().getParentItem()));
		tm.runTaskInParallel(new GetReferenceLoadCases(this, selected.getParentItem().getParentItem()));
		tm.runTaskInParallel(new GetMissions(this, selected));
	}

	@Override
	public void setElementGroups(ArrayList<String> groups) {
		groups_.getItems().setAll(groups);
	}

	@Override
	public void setReferenceLoadCases(ArrayList<ReferenceLoadCase> refCases) {
		onegLC_.getItems().setAll(refCases);
		dpLC_.getItems().setAll(refCases);
		vgLC_.getItems().setAll(refCases);
		onegLC_.getSelectionModel().clearSelection();
		dpLC_.getSelectionModel().clearSelection();
		vgLC_.getSelectionModel().clearSelection();
	}

	@Override
	public void setMissions(ArrayList<String> missions) {
		mission_.getItems().setAll(missions);
		mission_.getSelectionModel().clearSelection();
	}

	@FXML
	private void onOkClicked() {

		// get selected equivalent stress
		AircraftFatigueEquivalentStress selected = (AircraftFatigueEquivalentStress) owner_.getSelectedFiles().get(0);

		// check inputs
		if (!checkInputs(selected))
			return;

		// get stress inputs
		ElementTypeForStress elementType = elementType_.getSelectionModel().getSelectedItem();
		AircraftEquivalentStressType stressComponent = stress_.getSelectionModel().getSelectedItem();
		String mission = mission_.getSelectionModel().getSelectedItem();
		boolean interpolate = interpolate_.isSelected();

		// get reference load case inputs
		ReferenceLoadCase onegLC = oneg_.isSelected() ? onegLC_.getSelectionModel().getSelectedItem() : null;
		ReferenceLoadCase dpLC = dp_.isSelected() ? dpLC_.getSelectionModel().getSelectedItem() : null;
		ReferenceLoadCase vgLC = vg_.isSelected() ? vgLC_.getSelectionModel().getSelectedItem() : null;
		ElementStress refStressComp = refStressComp_.getSelectionModel().getSelectedItem();

		// set reference load case factors
		if (onegLC != null) {
			onegLC.setFactor(Double.parseDouble(onegLCFac_.getText()));
		}
		if (dpLC != null) {
			dpLC.setFactor(Double.parseDouble(dpLCFac_.getText()));
		}
		if (vgLC != null) {
			vgLC.setFactor(Double.parseDouble(vgLCFac_.getText()));
		}

		// get interpolation inputs
		int dataSource = -1;
		if (closest_.isSelected()) {
			dataSource = PlotAircraftEquivalentStressesInput.CLOSEST_PP;
		}
		else if (weightedNum_.isSelected()) {
			dataSource = PlotAircraftEquivalentStressesInput.WEIGHTED_NUM;
		}
		else if (weightedDist_.isSelected()) {
			dataSource = PlotAircraftEquivalentStressesInput.WEIGHTED_DIST;
		}
		int maxPPs = numPPs_.getValue();
		double maxDist = Double.parseDouble(distPPs_.getText());

		// get plot settings
		int beamExtrusion = beamExtrusion_.getValue();
		int rodExtrusion = rodExtrusion_.getValue();
		int opacity = opacity_.getValue();
		boolean outlines = outlines_.isSelected();
		PlotContour contour = contour_.getSelectionModel().getSelectedItem();

		// get value range
		Double lowerBound = null;
		if (lowerBound_.isSelected()) {
			String val = lowerBoundVal_.getText();
			if ((val != null) && !val.trim().isEmpty()) {
				lowerBound = Double.parseDouble(val);
			}
		}
		Double upperBound = null;
		if (upperBound_.isSelected()) {
			String val = upperBound_.getText();
			if ((val != null) && !val.trim().isEmpty()) {
				upperBound = Double.parseDouble(val);
			}
		}

		// create input
		PlotAircraftEquivalentStressesInput input = new PlotAircraftEquivalentStressesInput(selected, elementType, stressComponent, mission, interpolate, onegLC, dpLC, vgLC, refStressComp, dataSource, beamExtrusion, rodExtrusion, opacity, outlines, contour);
		input.setMaximumNumberOfPilotPoints(maxPPs);
		input.setMaximumPilotPointDistance(maxDist);

		// set value ranges
		input.setValueRange(lowerBound, upperBound);

		// set element group inputs
		for (String group : groups_.getSelectionModel().getSelectedItems()) {
			input.addGroup(group);
		}

		// LATER create and start statistics task
		// owner_.getOwner().getActiveTasksPanel().runTaskSequentially(new PlotAircraftEquivalentStresses(input));
	}

	/**
	 * Checks inputs.
	 *
	 * @param selected
	 *            Selected A/C equivalent stress.
	 * @return True if inputs are valid.
	 */
	private boolean checkInputs(AircraftFatigueEquivalentStress selected) {

		// nothing selected
		if (selected == null)
			return false;

		// no element type selected
		ElementTypeForStress elementType = elementType_.getSelectionModel().getSelectedItem();
		if (elementType == null) {
			String message = "Please select an element type to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(elementType_);
			return false;
		}

		// no equivalent stress type selected
		AircraftEquivalentStressType stressType = stress_.getSelectionModel().getSelectedItem();
		if (stressType == null) {
			String message = "Please select equivalent stress type to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(stress_);
			return false;
		}

		// no mission selected
		String mission = mission_.getSelectionModel().getSelectedItem();
		if (mission == null) {
			String message = "Please select a fatigue mission to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(mission_);
			return false;
		}

		// interpolate stresses
		if (interpolate_.isSelected()) {

			// no load case selected
			if (!oneg_.isSelected() && !dp_.isSelected() && !vg_.isSelected()) {
				String message = "Please select at least 1 reference load case to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(oneg_);
				return false;
			}

			// 1G cruise selected
			if (oneg_.isSelected()) {

				// no loadcase selected
				if (onegLC_.getSelectionModel().isEmpty()) {
					String message = "Please select 1G cruise load case to proceed.";
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(onegLC_);
					return false;
				}

				// invalid factor
				String message = onegLCFac_.validate();
				if (message != null) {
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(onegLCFac_);
					return false;
				}
			}

			// dp selected
			if (dp_.isSelected()) {

				// no loadcase selected
				if (dpLC_.getSelectionModel().isEmpty()) {
					String message = "Please select delta-P load case to proceed.";
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(dpLC_);
					return false;
				}

				// invalid factor
				String message = dpLCFac_.validate();
				if (message != null) {
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(dpLCFac_);
					return false;
				}
			}

			// vg selected
			if (vg_.isSelected()) {

				// no loadcase selected
				if (vgLC_.getSelectionModel().isEmpty()) {
					String message = "Please select vertical gust load case to proceed.";
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(vgLC_);
					return false;
				}

				// invalid factor
				String message = vgLCFac_.validate();
				if (message != null) {
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(vgLCFac_);
					return false;
				}
			}

			// no stress component selected
			ElementStress refStressComp = refStressComp_.getSelectionModel().getSelectedItem();
			if (refStressComp == null) {
				String message = "Please select a stress component for refernce load cases to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(refStressComp_);
				return false;
			}

			// weighted average selected
			if (weightedDist_.isSelected()) {
				String message = distPPs_.validate();
				if (message != null) {
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(weightedDist_);
					return false;
				}
			}
		}

		// check lower bound
		if (lowerBound_.isSelected()) {
			String message = lowerBoundVal_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(lowerBoundVal_);
				return false;
			}
		}

		// check upper bound
		if (upperBound_.isSelected()) {
			String message = upperBoundVal_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(upperBoundVal_);
				return false;
			}
		}

		// invalid value range
		if (lowerBound_.isSelected() && upperBound_.isSelected()) {
			double lowerBound = Double.parseDouble(lowerBoundVal_.getText());
			double upperBound = Double.parseDouble(upperBoundVal_.getText());
			if (lowerBound >= upperBound) {
				String message = "Invalid value range specified. Please make sure lower bound is smaller than upper bound.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(lowerBound_);
				return false;
			}
		}

		// inputs are valid
		return true;
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
		// LATER owner_.getOwner().get3DViewer().setVisible(false);
	}

	@FXML
	private void onResetClicked() {

		// reset element group selections
		groups_.getSelectionModel().clearSelection();

		// reset element type options
		elementType_.getSelectionModel().clearSelection();
		stress_.getSelectionModel().clearSelection();
		mission_.getSelectionModel().clearSelection();
		interpolate_.setSelected(false);

		// reset load case selections
		oneg_.setSelected(true);
		dp_.setSelected(true);
		vg_.setSelected(false);
		onegLC_.getSelectionModel().clearSelection();
		dpLC_.getSelectionModel().clearSelection();
		vgLC_.getSelectionModel().clearSelection();
		refStressComp_.getSelectionModel().clearSelection();
		onegLCFac_.reset();
		dpLCFac_.reset();
		vgLCFac_.reset();

		// reset interpolation options
		closest_.setSelected(true);
		numPPs_.getValueFactory().setValue(3);
		distPPs_.reset();

		// reset value range
		lowerBound_.setSelected(true);
		lowerBoundVal_.reset();
		upperBound_.setSelected(false);
		upperBoundVal_.reset();

		// reset settings
		contour_.getSelectionModel().select(PlotContour.SMOOTHED);
		opacity_.getValueFactory().setValue(100);
		outlines_.setSelected(false);
		beamExtrusion_.getValueFactory().setValue(50);
		rodExtrusion_.getValueFactory().setValue(20);

		// expand first panel
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@FXML
	private void onElementTypeSelected() {

		// get selected item
		ElementTypeForStress selected = elementType_.getSelectionModel().getSelectedItem();

		// no selection
		if ((selected == null) || elementType_.getSelectionModel().isEmpty())
			return;

		// beam or rod stresses
		if (selected.equals(ElementTypeForStress.BEAM) || selected.equals(ElementTypeForStress.ROD)) {
			refStressComp_.getItems().setAll(FXCollections.observableArrayList(ElementStress.SX));
			refStressComp_.getSelectionModel().clearSelection();
		}

		// other
		else {
			refStressComp_.getItems().setAll(FXCollections.observableArrayList(ElementStress.values()));
			refStressComp_.getSelectionModel().clearSelection();
		}
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to plot A/C equivalent stresses", null);
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static PlotAircraftEquivalentStressesPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("PlotAircraftEquivalentStressesPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			PlotAircraftEquivalentStressesPanel controller = (PlotAircraftEquivalentStressesPanel) fxmlLoader.getController();

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
