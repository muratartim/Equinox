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
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import control.validationField.DoubleValidationField;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.MissionParameter;
import equinox.data.fileType.SpectrumItem;
import equinox.task.AssignMissionParameters;
import equinox.task.GetMissionParameterNames;
import equinox.task.GetMissionParameterNames.MissionParameterNamesRequestingPanel;
import equinox.task.GetMissionParameters;
import equinox.task.GetMissionParameters.MissionParameterRequestingPanel;
import equinox.utility.Utility;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

/**
 * Class for mission parameters panel.
 *
 * @author Murat Artim
 * @date Nov 26, 2014
 * @time 10:03:05 AM
 */
public class MissionParametersPanel implements InternalInputSubPanel, MissionParameterRequestingPanel, MissionParameterNamesRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Spectrum item to edit. */
	private SpectrumItem spectrumItem_;

	/** Initial parameters. */
	private ArrayList<MissionParameter> initialParameters_;

	/** Auto-completion binding for parameter names. */
	private AutoCompletionBinding<String> autoCompletionBinding_;

	@FXML
	private VBox root_;

	@FXML
	private TextField parameterName_;

	@FXML
	private DoubleValidationField parameterValue_;

	@FXML
	private Button add_, remove_;

	@FXML
	private TableView<MissionParameter> parametersTable_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// initialize mission parameters table
		TableColumn<MissionParameter, String> parameterNameCol = new TableColumn<>("Name");
		parameterNameCol.setCellValueFactory(new PropertyValueFactory<MissionParameter, String>("name"));
		TableColumn<MissionParameter, Double> parameterValueCol = new TableColumn<>("Value");
		parameterValueCol.setCellValueFactory(new PropertyValueFactory<MissionParameter, Double>("value"));
		parametersTable_.getColumns().add(parameterNameCol);
		parametersTable_.getColumns().add(parameterValueCol);
		parameterNameCol.prefWidthProperty().bind(parametersTable_.widthProperty().divide(2));
		parameterValueCol.prefWidthProperty().bind(parametersTable_.widthProperty().divide(2));
		parametersTable_.setPlaceholder(new Label("No parameters defined."));
		parametersTable_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// add listener to parameter value
		parameterValue_.setDefaultValue(null);

		// bind add and remove buttons
		remove_.disableProperty().bind(parametersTable_.getSelectionModel().selectedItemProperty().isNull());
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

		// get selected CDF set
		spectrumItem_ = (SpectrumItem) owner_.getSelectedFiles().get(0);

		// set basic info
		parameterValue_.reset();
		parameterName_.clear();

		// get mission parameters
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetMissionParameters(this, spectrumItem_));
		tm.runTaskInParallel(new GetMissionParameterNames(this));
	}

	@Override
	public String getHeader() {
		return "Assign Mission Parameters";
	}

	@Override
	public void setMissionParameters(ArrayList<MissionParameter> parameters) {
		initialParameters_ = parameters;
		parametersTable_.getItems().setAll(initialParameters_);
	}

	@Override
	public void setMissionParameterNames(ArrayList<String> names) {
		parameterName_.clear();
		if (autoCompletionBinding_ != null) {
			autoCompletionBinding_.dispose();
		}
		autoCompletionBinding_ = TextFields.bindAutoCompletion(parameterName_, names);
	}

	@FXML
	private void onResetClicked() {
		parameterName_.clear();
		parameterValue_.reset();
		parametersTable_.getItems().setAll(initialParameters_);
	}

	@FXML
	private void onOkClicked() {

		// no change
		if (!checkForChange()) {
			owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
			return;
		}

		// get inputs
		ObservableList<MissionParameter> items = parametersTable_.getItems();
		MissionParameter[] mps = new MissionParameter[items.size()];
		for (int i = 0; i < mps.length; i++) {
			mps[i] = items.get(i);
		}

		// edit CDF info
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new AssignMissionParameters<>(spectrumItem_, mps));

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to assign mission parameters", null);
	}

	@FXML
	private void onValueEntered() {
		onAddClicked();
	}

	@FXML
	private void onAddClicked() {

		// check inputs
		if (!checkInputs())
			return;

		// get inputs
		String name = parameterName_.getText();
		String val = parameterValue_.getText();

		// add mission parameter
		addToMissionParameters(new MissionParameter(name, Double.parseDouble(val)));
	}

	/**
	 * Checks inputs.
	 *
	 * @return True if inputs are valid.
	 */
	private boolean checkInputs() {

		// no name given
		String name = parameterName_.getText();
		if (name == null || name.isEmpty()) {
			String message = "No parameter name given. Please enter parameter name.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(parameterName_);
			return false;
		}

		// no value given
		String message = parameterValue_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(parameterValue_);
			return false;
		}

		// valid inputs
		return true;
	}

	@FXML
	private void onRemoveClicked() {

		// no item selected
		if (parametersTable_.getSelectionModel().isEmpty())
			return;

		// get selected items
		ObservableList<MissionParameter> selected = parametersTable_.getSelectionModel().getSelectedItems();

		// remove from list
		parametersTable_.getItems().removeAll(selected);
	}

	/**
	 * Adds given mission parameter.
	 *
	 * @param parameter
	 *            Mission parameter to add.
	 */
	private void addToMissionParameters(MissionParameter parameter) {

		// get table items
		ObservableList<MissionParameter> items = parametersTable_.getItems();

		// check if it already exists (if so replace)
		for (int i = 0; i < items.size(); i++) {
			MissionParameter item = items.get(i);
			if (item.getName().equals(parameter.getName())) {
				items.set(i, parameter);
				return;
			}
		}

		// add
		items.add(parameter);
	}

	/**
	 * Returns true if there is change.
	 *
	 * @return True if there is change.
	 */
	private boolean checkForChange() {

		// check number of mission parameters
		ObservableList<MissionParameter> items = parametersTable_.getItems();
		if (items.size() != initialParameters_.size())
			return true;

		// check mission parameters
		for (int i = 0; i < items.size(); i++) {
			if (!items.get(i).equals(initialParameters_.get(i)))
				return true;
		}

		// no change
		return false;
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static MissionParametersPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("MissionParametersPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			MissionParametersPanel controller = (MissionParametersPanel) fxmlLoader.getController();

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
