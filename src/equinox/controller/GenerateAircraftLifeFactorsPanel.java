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

import control.validationField.IntegerValidationField;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.ElementTypeForStress;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.input.CompareAircraftLifeFactorsInput;
import equinox.data.input.LifeFactorType;
import equinox.task.GenerateAircraftLifeFactors;
import equinox.task.GetElementGroups;
import equinox.task.GetElementGroups.ElementGroupsRequestingPanel;
import equinox.task.GetMissions;
import equinox.task.GetMissions.MissionRequestingPanel;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.VBox;

/**
 * Class for generate A/C model life factors panel controller.
 *
 * @author Murat Artim
 * @date Sep 23, 2015
 * @time 11:13:27 AM
 */
public class GenerateAircraftLifeFactorsPanel implements InternalInputSubPanel, ElementGroupsRequestingPanel, MissionRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ComboBox<LifeFactorType> factorType_;

	@FXML
	private ComboBox<String> mission_;

	@FXML
	private ComboBox<ElementTypeForStress> elementType_;

	@FXML
	private ListView<String> groups_;

	@FXML
	private IntegerValidationField limit_;

	@FXML
	private ChoiceBox<String> order_;

	@FXML
	private ToggleSwitch dataLabels_, includeBasis_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set factor types
		factorType_.setItems(FXCollections.observableArrayList(LifeFactorType.values()));

		// set element types
		elementType_.setItems(FXCollections.observableArrayList(ElementTypeForStress.values()));

		// set multiple selection for lists
		groups_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// set results order
		order_.setItems(FXCollections.observableArrayList("Descending", "Ascending"));
		order_.getSelectionModel().select(0);

		// setup end flight number field
		limit_.setDefaultValue(10);
		limit_.setMinimumValue(1, true);

		// set listeners
		dataLabels_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				StatisticsViewPanel panel = (StatisticsViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);
				panel.setLabelsVisible(newValue);
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
	public String getHeader() {
		return "Generate Life Factors";
	}

	@Override
	public void showing() {

		// get selected equivalent stress
		AircraftFatigueEquivalentStress selected = (AircraftFatigueEquivalentStress) owner_.getSelectedFiles().get(0);

		// get element groups and missions
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
	}

	@FXML
	private void onOkClicked() {

		// check inputs
		if (!checkInputs())
			return;

		// get selected equivalent stress
		AircraftFatigueEquivalentStress eqStress = (AircraftFatigueEquivalentStress) owner_.getSelectedFiles().get(0);

		// get element type
		ElementTypeForStress elementType = elementType_.getSelectionModel().getSelectedItem();

		// get factor type
		LifeFactorType factorType = factorType_.getSelectionModel().getSelectedItem();

		// get basis mission
		String mission = mission_.getSelectionModel().getSelectedItem();

		// create input
		CompareAircraftLifeFactorsInput input = new CompareAircraftLifeFactorsInput(eqStress, factorType, elementType, mission);

		// set inputs
		input.setLabelDisplay(dataLabels_.isSelected());
		input.setIncludeBasisMission(includeBasis_.isSelected());
		input.setLimit(Integer.parseInt(limit_.getText()));
		input.setOrder(order_.getSelectionModel().getSelectedItem().equals("Descending"));

		// set element group inputs
		for (String group : groups_.getSelectionModel().getSelectedItems()) {
			input.addGroup(group);
		}

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GenerateAircraftLifeFactors(input));
	}

	/**
	 * Checks inputs.
	 *
	 * @return True if inputs are valid.
	 */
	private boolean checkInputs() {

		// get selected equivalent stress
		AircraftFatigueEquivalentStress eqStress = (AircraftFatigueEquivalentStress) owner_.getSelectedFiles().get(0);

		// nothing selected
		if (eqStress == null)
			return false;

		// get element type
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

		// get factor type
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

		// get basis mission
		String mission = mission_.getSelectionModel().getSelectedItem();
		if (mission == null) {
			String message = "Please select basis fatigue mission for comparison.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(mission_);
			return false;
		}

		// check limit
		String message = limit_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(limit_);
			return false;
		}

		// valid inputs
		return true;
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {

		// reset comparison inputs
		factorType_.getSelectionModel().clearSelection();
		mission_.setValue(null);
		mission_.getSelectionModel().clearSelection();
		elementType_.setValue(null);
		elementType_.getSelectionModel().clearSelection();

		// reset element group and position selections
		groups_.getSelectionModel().clearSelection();

		// reset options
		order_.getSelectionModel().select(0);
		limit_.reset();
		if (!dataLabels_.isSelected()) {
			dataLabels_.setSelected(true);
		}
		if (!includeBasis_.isSelected()) {
			includeBasis_.setSelected(true);
		}
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to generate aircraft model life factors", null);
	}

	/**
	 * Loads and returns spectrum statistics panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded spectrum statistics panel.
	 */
	public static GenerateAircraftLifeFactorsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("GenerateAircraftLifeFactorsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			GenerateAircraftLifeFactorsPanel controller = (GenerateAircraftLifeFactorsPanel) fxmlLoader.getController();

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
