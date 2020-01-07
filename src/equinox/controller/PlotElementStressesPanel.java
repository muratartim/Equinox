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
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.fileType.AircraftModel;
import equinox.data.input.PlotElementStressesInput;
import equinox.data.ui.LoadCaseFactorTableItem;
import equinox.data.ui.PlotContour;
import equinox.task.GetElementGroups;
import equinox.task.GetElementGroups.ElementGroupsRequestingPanel;
import equinox.task.PlotElementStresses;
import equinox.utility.SpinnerListener;
import equinox.utility.Utility;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;

/**
 * Class for plot element stresses panel controller.
 *
 * @author Murat Artim
 * @date Aug 7, 2015
 * @time 1:56:16 PM
 */
public class PlotElementStressesPanel implements InternalInputSubPanel, ElementGroupsRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ChoiceBox<PlotContour> contour_;

	@FXML
	private ComboBox<ElementTypeForStress> elementType_;

	@FXML
	private ComboBox<ElementStress> stressComponent_;

	@FXML
	private ToggleSwitch outlines_, lowerBound_, upperBound_;

	@FXML
	private ListView<String> groups_;

	@FXML
	private Spinner<Integer> opacity_, beamExtrusion_, rodExtrusion_;

	@FXML
	private DoubleValidationField lowerBoundVal_, upperBoundVal_;

	@FXML
	private TitledPane stressPane_;

	@FXML
	private TableView<LoadCaseFactorTableItem> factorsTable_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup plot contour
		contour_.setItems(FXCollections.observableArrayList(PlotContour.values()));
		contour_.getSelectionModel().select(PlotContour.DISCRETE);

		// setup element types and stress components
		elementType_.setItems(FXCollections.observableArrayList(ElementTypeForStress.values()));
		stressComponent_.setItems(FXCollections.observableArrayList(ElementStress.values()));

		// initialize factors table
		TableColumn<LoadCaseFactorTableItem, String> nameCol = new TableColumn<>("Load Case");
		nameCol.setCellValueFactory(new PropertyValueFactory<LoadCaseFactorTableItem, String>("name"));
		TableColumn<LoadCaseFactorTableItem, String> factorCol = new TableColumn<>("Factor");
		factorCol.setCellValueFactory(new PropertyValueFactory<LoadCaseFactorTableItem, String>("factor"));
		factorCol.setCellFactory(TextFieldTableCell.<LoadCaseFactorTableItem>forTableColumn());
		factorCol.setOnEditCommit((CellEditEvent<LoadCaseFactorTableItem, String> t) -> {
			t.getTableView().getItems().get(t.getTablePosition().getRow()).setFactor(t.getNewValue());
		});
		factorsTable_.getColumns().add(nameCol);
		factorsTable_.getColumns().add(factorCol);
		factorCol.setPrefWidth(75);
		nameCol.prefWidthProperty().bind(factorsTable_.widthProperty().subtract(factorCol.widthProperty()));
		factorsTable_.setPlaceholder(new Label("No load cases selected."));

		// setup opacities
		opacity_.setValueFactory(new IntegerSpinnerValueFactory(0, 100, 100));
		opacity_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(opacity_));

		// set multiple selection for lists
		groups_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// setup settings
		beamExtrusion_.setValueFactory(new IntegerSpinnerValueFactory(10, 100, 50));
		beamExtrusion_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(beamExtrusion_));
		rodExtrusion_.setValueFactory(new IntegerSpinnerValueFactory(10, 100, 20));
		rodExtrusion_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(rodExtrusion_));

		// add listeners to text fields
		lowerBoundVal_.setDefaultValue(null);
		upperBoundVal_.setDefaultValue(null);

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
		return "Plot Element Stresses";
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {

		// get selected load cases
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();

		// reset load case factors table
		factorsTable_.getItems().clear();
		for (TreeItem<String> item : selected) {
			factorsTable_.getItems().add(new LoadCaseFactorTableItem((AircraftLoadCase) item));
		}

		// get A/C model
		AircraftModel model = ((AircraftLoadCase) selected.get(0)).getParentItem().getParentItem();

		// get element groups and positions
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetElementGroups(this, model));
	}

	@Override
	public void setElementGroups(ArrayList<String> groups) {
		groups_.getItems().setAll(groups);
	}

	@FXML
	private void onOkClicked() {

		// check inputs
		if (!checkInputs())
			return;

		// get inputs
		ElementTypeForStress elementType = elementType_.getSelectionModel().getSelectedItem();
		ElementStress stressComponent = stressComponent_.getSelectionModel().getSelectedItem();
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

		// get load case factors
		ArrayList<LoadCaseFactorTableItem> loadCases = new ArrayList<>();
		for (LoadCaseFactorTableItem factor : factorsTable_.getItems()) {

			// check factor
			try {
				Double.parseDouble(factor.getFactor());
			}

			// invalid factor
			catch (Exception e) {
				accordion_.setExpandedPane(accordion_.getPanes().get(1));
				String message = "Invalid load case factor specified. Please make sure to supply numeric values.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(factorsTable_);
				return;
			}

			// add
			loadCases.add(factor);
		}

		// create input
		PlotElementStressesInput input = new PlotElementStressesInput(loadCases, elementType, stressComponent, beamExtrusion, rodExtrusion, opacity, outlines, contour);

		// set value ranges
		input.setValueRange(lowerBound, upperBound);

		// set element group inputs
		for (String group : groups_.getSelectionModel().getSelectedItems()) {
			input.addGroup(group);
		}

		// create and start statistics task
		owner_.getOwner().getActiveTasksPanel().runTaskSequentially(new PlotElementStresses(input));
	}

	/**
	 * Checks inputs.
	 *
	 * @return True if inputs are valid.
	 */
	private boolean checkInputs() {

		// no element type selected
		ElementTypeForStress elementType = elementType_.getSelectionModel().getSelectedItem();
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
			return false;
		}

		// no stress component selected
		ElementStress stressComponent = stressComponent_.getSelectionModel().getSelectedItem();
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
			return false;
		}

		// check lower bound
		if (lowerBound_.isSelected()) {
			String message = lowerBoundVal_.validate();
			if (message != null) {
				accordion_.setExpandedPane(accordion_.getPanes().get(2));
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
				accordion_.setExpandedPane(accordion_.getPanes().get(2));
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
				accordion_.setExpandedPane(accordion_.getPanes().get(2));
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
	}

	@FXML
	private void onResetClicked() {

		// reset element group and position selections
		groups_.getSelectionModel().clearSelection();

		// reset element type options
		elementType_.getSelectionModel().clearSelection();
		stressComponent_.getSelectionModel().clearSelection();
		opacity_.getValueFactory().setValue(100);
		outlines_.setSelected(false);

		// reset settings
		beamExtrusion_.getValueFactory().setValue(50);
		rodExtrusion_.getValueFactory().setValue(20);
		contour_.getSelectionModel().select(PlotContour.DISCRETE);

		// reset value range
		lowerBound_.setSelected(false);
		lowerBoundVal_.reset();
		upperBound_.setSelected(false);
		upperBoundVal_.reset();

		// reset load case factors
		for (LoadCaseFactorTableItem item : factorsTable_.getItems()) {
			item.setFactor("1.0");
		}

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to plot element stresses", null);
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
	public static PlotElementStressesPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("PlotElementStressesPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			PlotElementStressesPanel controller = (PlotElementStressesPanel) fxmlLoader.getController();

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
