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
import equinox.data.ElementStress;
import equinox.data.ElementTypeForStress;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.input.CompareElementStressesInput;
import equinox.task.CompareElementStresses;
import equinox.task.GetElementGroups;
import equinox.task.GetElementGroups.ElementGroupsRequestingPanel;
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
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

/**
 * Class for compare element stresses panel controller.
 *
 * @author Murat Artim
 * @date Aug 26, 2015
 * @time 10:43:28 AM
 */
public class CompareElementStressesPanel implements InternalInputSubPanel, ElementGroupsRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ComboBox<ElementTypeForStress> elementType_;

	@FXML
	private ComboBox<ElementStress> stressComponent_;

	@FXML
	private ListView<String> groups_;

	@FXML
	private IntegerValidationField limit_;

	@FXML
	private ChoiceBox<String> order_;

	@FXML
	private ToggleSwitch dataLabels_;

	@FXML
	private TitledPane stressPane_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup element types and stress components
		elementType_.setItems(FXCollections.observableArrayList(ElementTypeForStress.values()));
		stressComponent_.setItems(FXCollections.observableArrayList(ElementStress.values()));

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
	public String getHeader() {
		return "Compare Element Stresses";
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {

		// get selected load case
		AircraftLoadCase selected = (AircraftLoadCase) owner_.getSelectedFiles().get(0);

		// get element groups and positions
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetElementGroups(this, selected.getParentItem().getParentItem()));
	}

	@Override
	public void setElementGroups(ArrayList<String> groups) {
		groups_.getItems().setAll(groups);
	}

	@FXML
	private void onOkClicked() {

		// get selected load case
		AircraftLoadCase loadCase = (AircraftLoadCase) owner_.getSelectedFiles().get(0);

		// nothing selected
		if (loadCase == null)
			return;

		// get stress inputs
		ElementTypeForStress elementType = elementType_.getSelectionModel().getSelectedItem();
		ElementStress stressComponent = stressComponent_.getSelectionModel().getSelectedItem();

		// no element type selected
		if (elementType == null) {
			accordion_.setExpandedPane(accordion_.getPanes().get(0));
			String message = "Please select an element type to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(elementType_);
			return;
		}

		// no stress component selected
		if (stressComponent == null) {
			accordion_.setExpandedPane(accordion_.getPanes().get(0));
			String message = "Please select a stress component to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(stressComponent_);
			return;
		}

		// check limit
		String message = limit_.validate();
		if (message != null) {
			accordion_.setExpandedPane(accordion_.getPanes().get(2));
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(limit_);
			return;
		}

		// create input
		CompareElementStressesInput input = new CompareElementStressesInput(loadCase, elementType, stressComponent);

		// set options
		input.setLabelDisplay(dataLabels_.isSelected());
		input.setLimit(Integer.parseInt(limit_.getText()));
		input.setOrder(order_.getSelectionModel().getSelectedItem().equals("Descending"));

		// set element group inputs
		for (String group : groups_.getSelectionModel().getSelectedItems()) {
			input.addGroup(group);
		}

		// create and start statistics task
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new CompareElementStresses(input));
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {

		// reset element group and position selections
		groups_.getSelectionModel().clearSelection();

		// reset element type options
		elementType_.getSelectionModel().clearSelection();
		stressComponent_.getSelectionModel().clearSelection();

		// reset options
		order_.getSelectionModel().select(0);
		limit_.reset();
		dataLabels_.setSelected(true);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to compare element stresses", null);
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
			stressComponent_.getItems().setAll(FXCollections.observableArrayList(ElementStress.SX));
			stressComponent_.getSelectionModel().clearSelection();
		}

		// other
		else {
			stressComponent_.getItems().setAll(FXCollections.observableArrayList(ElementStress.values()));
			stressComponent_.getSelectionModel().clearSelection();
		}
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static CompareElementStressesPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CompareElementStressesPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CompareElementStressesPanel controller = (CompareElementStressesPanel) fxmlLoader.getController();

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
