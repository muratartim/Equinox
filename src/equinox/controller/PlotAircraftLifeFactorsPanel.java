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
import equinox.data.ElementTypeForStress;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.input.LifeFactorType;
import equinox.data.input.PlotAircraftLifeFactorsInput;
import equinox.data.ui.PlotContour;
import equinox.task.GetElementGroups;
import equinox.task.GetElementGroups.ElementGroupsRequestingPanel;
import equinox.task.GetMissions;
import equinox.task.GetMissions.MissionRequestingPanel;
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
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.VBox;

/**
 * Class for plot A/C model life factors panel controller.
 *
 * @author Murat Artim
 * @date Oct 4, 2015
 * @time 6:44:03 PM
 */
public class PlotAircraftLifeFactorsPanel implements InternalInputSubPanel, ElementGroupsRequestingPanel, MissionRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ChoiceBox<PlotContour> contour_;

	@FXML
	private ComboBox<ElementTypeForStress> elementType_;

	@FXML
	private ComboBox<LifeFactorType> factorType_;

	@FXML
	private ComboBox<String> mission_, basisMission_;

	@FXML
	private ToggleSwitch outlines_, lowerBound_, upperBound_;

	@FXML
	private ListView<String> groups_;

	@FXML
	private Spinner<Integer> opacity_, beamExtrusion_, rodExtrusion_;

	@FXML
	private DoubleValidationField lowerBoundVal_, upperBoundVal_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup plot contour
		contour_.setItems(FXCollections.observableArrayList(PlotContour.values()));
		contour_.getSelectionModel().select(PlotContour.SMOOTHED);

		// setup element types and stress components
		elementType_.setItems(FXCollections.observableArrayList(ElementTypeForStress.values()));
		factorType_.setItems(FXCollections.observableArrayList(LifeFactorType.values()));

		// setup opacities
		opacity_.setValueFactory(new IntegerSpinnerValueFactory(0, 100, 100));
		opacity_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(opacity_));

		// set multiple selection for lists
		groups_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// add listeners to text fields
		lowerBoundVal_.setDefaultValue(0.0);
		upperBoundVal_.setDefaultValue(null);

		// setup settings
		beamExtrusion_.setValueFactory(new IntegerSpinnerValueFactory(10, 100, 50));
		beamExtrusion_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(beamExtrusion_));
		rodExtrusion_.setValueFactory(new IntegerSpinnerValueFactory(10, 100, 20));
		rodExtrusion_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(rodExtrusion_));

		// bind components
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
		return "Plot Life Factors";
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
		tm.runTaskInParallel(new GetMissions(this, selected));
	}

	@Override
	public void setElementGroups(ArrayList<String> groups) {
		groups_.getItems().setAll(groups);
	}

	@Override
	public void setMissions(ArrayList<String> missions) {
		mission_.getItems().setAll(missions);
		mission_.getSelectionModel().clearSelection();
		basisMission_.getItems().setAll(missions);
		basisMission_.getSelectionModel().clearSelection();
	}

	@FXML
	private void onOkClicked() {

		// get selected equivalent stress
		AircraftFatigueEquivalentStress selected = (AircraftFatigueEquivalentStress) owner_.getSelectedFiles().get(0);

		// check inputs
		if (!checkInputs(selected))
			return;

		// get factor inputs
		ElementTypeForStress elementType = elementType_.getSelectionModel().getSelectedItem();
		LifeFactorType factorType = factorType_.getSelectionModel().getSelectedItem();
		String mission = mission_.getSelectionModel().getSelectedItem();
		String basisMission = basisMission_.getSelectionModel().getSelectedItem();

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
		PlotAircraftLifeFactorsInput input = new PlotAircraftLifeFactorsInput(selected, elementType, factorType, mission, basisMission, beamExtrusion, rodExtrusion, opacity, outlines, contour);

		// set value ranges
		input.setValueRange(lowerBound, upperBound);

		// set element group inputs
		for (String group : groups_.getSelectionModel().getSelectedItems()) {
			input.addGroup(group);
		}

		// LATER create and start statistics task
		// owner_.getOwner().getActiveTasksPanel().runTaskSequentially(new PlotAircraftLifeFactors(input));
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

		// no life factor type selected
		LifeFactorType factorType = factorType_.getSelectionModel().getSelectedItem();
		if (factorType == null) {
			String message = "Please select life factor type to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(factorType_);
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

		// no basis mission selected
		String basisMission = basisMission_.getSelectionModel().getSelectedItem();
		if (basisMission == null) {
			String message = "Please select a basis fatigue mission to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(basisMission_);
			return false;
		}

		// same mission and basis mission
		if (mission.equals(basisMission)) {
			String message = "Target and basis missions cannot be the same. Please select unique missions to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(basisMission_);
			return false;
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
		factorType_.getSelectionModel().clearSelection();
		mission_.getSelectionModel().clearSelection();
		basisMission_.getSelectionModel().clearSelection();

		// reset value range
		lowerBound_.setSelected(true);
		lowerBoundVal_.reset();
		upperBound_.setSelected(false);
		upperBoundVal_.reset();

		// reset settings
		opacity_.getValueFactory().setValue(100);
		outlines_.setSelected(false);
		beamExtrusion_.getValueFactory().setValue(50);
		rodExtrusion_.getValueFactory().setValue(20);
		contour_.getSelectionModel().select(PlotContour.SMOOTHED);

		// expand first panel
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to plot A/C life factors", null);
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static PlotAircraftLifeFactorsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("PlotAircraftLifeFactorsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			PlotAircraftLifeFactorsPanel controller = (PlotAircraftLifeFactorsPanel) fxmlLoader.getController();

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
