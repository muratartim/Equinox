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
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeriesCollection;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.MissionParameterPlotViewPanel.PlotCompletionPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.EquivalentStressComparisonInput;
import equinox.task.CompareEquivalentStresses;
import equinox.task.CompareEquivalentStressesWithMissionParameters;
import equinox.task.GetMissionParameterNames;
import equinox.task.GetMissionParameterNames.MissionParameterNamesRequestingPanel;
import equinox.task.InternalEquinoxTask;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Class for compare equivalent stress panel controller.
 *
 * @author Murat Artim
 * @date Oct 9, 2014
 * @time 11:46:22 AM
 */
public class CompareEquivalentStressPanel implements InternalInputSubPanel, MissionParameterNamesRequestingPanel, PlotCompletionPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_, seriesContainer_;

	@FXML
	private ComboBox<String> missionParameters_;

	@FXML
	private ToggleSwitch plotMissionParameters_, includeSpectrumName_, includeSTFName_, includeSequenceName_, includeMaterialName_, includeOmissionLevel_, includeProgram_, includeSection_, includeMission_, includeEID_, dataLabels_, showMarkers_, showCrosshairs_;

	@FXML
	private Accordion accordion_;

	@FXML
	private TitledPane seriesPane_, namingPane_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set listeners
		plotMissionParameters_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onPlotMissionParametersSelected(newValue));
		showMarkers_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onShowMarkersSelected(newValue));
		showCrosshairs_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onShowCrosshairsSelected(newValue));
		dataLabels_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
			StatisticsViewPanel panel = (StatisticsViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);
			panel.setLabelsVisible(newValue);
		});

		// expand first pane
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
		seriesContainer_.getChildren().clear();

		// expand first panel
		accordion_.setExpandedPane(accordion_.getPanes().get(0));

		// get first selected file
		SpectrumItem selected = (SpectrumItem) owner_.getSelectedFiles().get(0);

		// equivalent stress
		if (selected instanceof FatigueEquivalentStress || selected instanceof PreffasEquivalentStress || selected instanceof LinearEquivalentStress) {
			includeSTFName_.setDisable(false);
			includeSpectrumName_.setDisable(false);
			includeSequenceName_.setDisable(false);
		}

		// external equivalent stress
		else if (selected instanceof ExternalFatigueEquivalentStress || selected instanceof ExternalPreffasEquivalentStress || selected instanceof ExternalLinearEquivalentStress) {
			includeSTFName_.setDisable(true);
			includeSpectrumName_.setDisable(true);
			includeSTFName_.setSelected(false);
			includeSpectrumName_.setSelected(false);
			includeSequenceName_.setDisable(false);
		}

		// fast equivalent stress
		else if (selected instanceof FastFatigueEquivalentStress || selected instanceof FastPreffasEquivalentStress || selected instanceof FastLinearEquivalentStress) {
			includeSTFName_.setDisable(false);
			includeSpectrumName_.setDisable(false);
			includeSequenceName_.setDisable(true);
			includeSequenceName_.setSelected(false);
		}

		// get mission parameters
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetMissionParameterNames(this));
	}

	@Override
	public String getHeader() {
		return "Compare Eq. Stresses";
	}

	@Override
	public void setMissionParameterNames(ArrayList<String> names) {
		missionParameters_.setValue(null);
		missionParameters_.getSelectionModel().clearSelection();
		missionParameters_.getItems().setAll(names);
	}

	@Override
	public void plottingCompleted(XYSeriesCollection data) {

		// setup show/hide series components
		seriesPane_.setDisable(false);
		seriesContainer_.getChildren().clear();
		for (int i = 0; i < data.getSeriesCount(); i++) {

			// create horizontal box
			HBox hBox = new HBox();
			hBox.setAlignment(Pos.CENTER_LEFT);
			hBox.setSpacing(5.0);
			hBox.setMaxWidth(Double.MAX_VALUE);

			// create toggle switch
			ToggleSwitch tSwitch = new ToggleSwitch();
			HBox.setHgrow(tSwitch, Priority.NEVER);
			tSwitch.setPrefWidth(35.0);
			tSwitch.setMinWidth(ToggleSwitch.USE_PREF_SIZE);
			tSwitch.setMaxWidth(ToggleSwitch.USE_PREF_SIZE);
			tSwitch.setUserData(i);
			tSwitch.setSelected(true);

			// create label
			Label label = new Label(data.getSeries(i).getKey().toString());
			HBox.setHgrow(label, Priority.ALWAYS);
			label.setMaxWidth(Double.MAX_VALUE);

			// add components to horizontal box
			hBox.getChildren().add(tSwitch);
			hBox.getChildren().add(label);

			// set listener to toggle switch
			tSwitch.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onShowSeriesSelected(newValue, (int) tSwitch.getUserData()));

			// add to container
			seriesContainer_.getChildren().add(hBox);
			onShowSeriesSelected(true, i);
		}
	}

	/**
	 * Called when plot against mission parameters option is selected.
	 *
	 * @param isSelected
	 *            True if plot against mission parameters option is selected.
	 */
	private void onPlotMissionParametersSelected(boolean isSelected) {
		missionParameters_.setDisable(!isSelected);
		showMarkers_.setDisable(!isSelected);
		showCrosshairs_.setDisable(!isSelected);
		dataLabels_.setDisable(isSelected);
		if (!isSelected) {
			seriesPane_.setExpanded(false);
		}
		seriesPane_.setDisable(!isSelected);
	}

	@FXML
	private void onResetClicked() {

		// mission parameter options
		missionParameters_.setValue(null);
		missionParameters_.getSelectionModel().clearSelection();
		plotMissionParameters_.setSelected(false);

		// reset options
		dataLabels_.setSelected(true);
		showMarkers_.setSelected(true);
		showCrosshairs_.setSelected(true);

		// reset naming options
		includeSpectrumName_.setSelected(false);
		includeEID_.setSelected(false);
		includeMaterialName_.setSelected(true);
		includeOmissionLevel_.setSelected(false);
		includeProgram_.setSelected(false);
		includeSection_.setSelected(false);
		includeMission_.setSelected(false);

		// get first selected file
		SpectrumItem selected = (SpectrumItem) owner_.getSelectedFiles().get(0);

		// select specific options
		if (selected instanceof FatigueEquivalentStress || selected instanceof PreffasEquivalentStress || selected instanceof LinearEquivalentStress || selected instanceof FastFatigueEquivalentStress || selected instanceof FastPreffasEquivalentStress
				|| selected instanceof FastLinearEquivalentStress) {
			if (!includeSTFName_.isSelected()) {
				includeSTFName_.setSelected(true);
			}
			includeSequenceName_.setSelected(false);
		}
		else if (selected instanceof ExternalFatigueEquivalentStress || selected instanceof ExternalPreffasEquivalentStress || selected instanceof ExternalLinearEquivalentStress) {
			if (!includeSequenceName_.isSelected()) {
				includeSequenceName_.setSelected(true);
			}
			includeSTFName_.setSelected(false);
		}

		// reset show/hide series
		onPlotMissionParametersSelected(false);
		for (Node node : seriesContainer_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			ts.setSelected(true);
		}
	}

	@FXML
	private void onOkClicked() {

		// check inputs
		if (!checkInputs())
			return;

		// create input
		EquivalentStressComparisonInput input = new EquivalentStressComparisonInput();

		// set inputs
		input.setLabelDisplay(dataLabels_.isSelected());
		input.setMissionParameterName(missionParameters_.getSelectionModel().getSelectedItem());

		// get naming parameters
		boolean includeSpectrumName = includeSpectrumName_.isSelected();
		boolean includeSTFName = includeSTFName_.isSelected();
		boolean includeEID = includeEID_.isSelected();
		boolean includeSequenceName = includeSequenceName_.isSelected();
		boolean includeMaterialName = includeMaterialName_.isSelected();
		boolean includeOmissionLevel = includeOmissionLevel_.isSelected();
		boolean includeProgram = includeProgram_.isSelected();
		boolean includeSection = includeSection_.isSelected();
		boolean includeMission = includeMission_.isSelected();

		// set naming parameters
		input.setIncludeSpectrumName(includeSpectrumName);
		input.setIncludeSTFName(includeSTFName);
		input.setIncludeEID(includeEID);
		input.setIncludeSequenceName(includeSequenceName);
		input.setIncludeMaterialName(includeMaterialName);
		input.setIncludeOmissionLevel(includeOmissionLevel);
		input.setIncludeProgram(includeProgram);
		input.setIncludeSection(includeSection);
		input.setIncludeMission(includeMission);

		// create task
		InternalEquinoxTask<?> task = null;

		// with mission parameters
		if (plotMissionParameters_.isSelected()) {

			// create task
			task = new CompareEquivalentStressesWithMissionParameters(input, this);

			// add selected equivalent stresses
			ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();
			for (TreeItem<String> item : selected) {
				((CompareEquivalentStressesWithMissionParameters) task).addEquivalentStress((SpectrumItem) item);
			}
		}

		// without mission parameters
		else {

			// create task
			task = new CompareEquivalentStresses(input);

			// add selected equivalent stresses
			ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();
			for (TreeItem<String> item : selected) {
				((CompareEquivalentStresses) task).addEquivalentStress((SpectrumItem) item);
			}
		}

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(task);
	}

	/**
	 * Checks inputs.
	 *
	 * @return True if inputs are valid.
	 */
	private boolean checkInputs() {

		// no mission parameter selected
		if (plotMissionParameters_.isSelected() && missionParameters_.getSelectionModel().isEmpty()) {
			String message = "Please select a mission parameter for comparison.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(missionParameters_);
			return false;
		}

		// get naming parameters
		boolean includeSpectrumName = includeSpectrumName_.isSelected();
		boolean includeSTFName = includeSTFName_.isSelected();
		boolean includeEID = includeEID_.isSelected();
		boolean includeSequenceName = includeSequenceName_.isSelected();
		boolean includeMaterialName = includeMaterialName_.isSelected();
		boolean includeOmissionLevel = includeOmissionLevel_.isSelected();
		boolean includeProgram = includeProgram_.isSelected();
		boolean includeSection = includeSection_.isSelected();
		boolean includeMission = includeMission_.isSelected();

		// no naming selected
		if (!includeSpectrumName && !includeSTFName && !includeSequenceName && !includeMaterialName && !includeOmissionLevel && !includeProgram && !includeSection && !includeMission && !includeEID) {
			String message = "Please select at least 1 naming for plot series.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(namingPane_);
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
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to compare equivalent stresses", null);
	}

	/**
	 * Called when show markers option is selected.
	 *
	 * @param isSelected
	 *            True if show markers option is selected.
	 */
	private void onShowMarkersSelected(boolean isSelected) {
		JFreeChart chart = getMissionParametersChart();
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
		renderer.setBaseShapesVisible(isSelected);
	}

	/**
	 * Called when show crosshairs option is selected.
	 *
	 * @param isSelected
	 *            True if show crosshairs option is selected.
	 */
	private void onShowCrosshairsSelected(boolean isSelected) {
		MissionParameterPlotViewPanel panel = (MissionParameterPlotViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PARAMETERS_VIEW);
		panel.onShowCrosshairsSelected(isSelected);
	}

	/**
	 * Called when show series selected.
	 *
	 * @param visible
	 *            True if visible.
	 * @param seriesID
	 *            Series ID.
	 */
	private void onShowSeriesSelected(boolean visible, int seriesID) {
		if (seriesPane_.isDisable())
			return;
		MissionParameterPlotViewPanel panel = (MissionParameterPlotViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PARAMETERS_VIEW);
		panel.onShowSeriesSelected(visible, seriesID);
	}

	/**
	 * Returns the mission parameters chart.
	 *
	 * @return The mission parameters chart.
	 */
	private JFreeChart getMissionParametersChart() {
		MissionParameterPlotViewPanel panel = (MissionParameterPlotViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PARAMETERS_VIEW);
		return panel.getChart();
	}

	/**
	 * Loads and returns spectrum statistics panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded spectrum statistics panel.
	 */
	public static CompareEquivalentStressPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CompareEquivalentStressPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CompareEquivalentStressPanel controller = (CompareEquivalentStressPanel) fxmlLoader.getController();

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
