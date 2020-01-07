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
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.SeriesKey;
import equinox.data.fileType.ExternalFlight;
import equinox.data.input.ExternalFlightComparisonInput;
import equinox.task.CompareExternalFlights;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Class for compare external flights panel controller.
 *
 * @author Murat Artim
 * @date Mar 15, 2015
 * @time 5:34:12 PM
 */
public class CompareExternalFlightsPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_, flightsContainer_;

	@FXML
	private ToggleSwitch showMarkers_, includeFlightName_, includeSequenceName_, includeEID_, includeProgram_, includeSection_, includeMission_;

	@FXML
	private TitledPane namingPane_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set listeners
		showMarkers_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onShowMarkersSelected(newValue));

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
		flightsContainer_.getChildren().clear();
		onOkClicked();
	}

	@Override
	public String getHeader() {
		return "Compare Flights";
	}

	/**
	 * Sets chart to this panel.
	 *
	 * @param chart
	 *            Chart to set.
	 */
	public void setChart(JFreeChart chart) {

		// reset flights container
		flightsContainer_.getChildren().clear();

		// get chart plot
		XYPlot plot = chart.getXYPlot();

		// loop over datasets
		for (int i = 0; i < plot.getDatasetCount(); i++) {

			// get dataset
			XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset(i);

			// get renderer
			XYItemRenderer renderer = plot.getRenderer(i);

			// loop over series
			for (int j = 0; j < dataset.getSeriesCount(); j++) {

				// get series
				XYSeries series = dataset.getSeries(j);

				// get series key
				SeriesKey key = (SeriesKey) series.getKey();

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
				tSwitch.setUserData(key.getID());
				tSwitch.setSelected(renderer.isSeriesVisible(j));

				// create label
				Label label = new Label(key.getName());
				HBox.setHgrow(label, Priority.ALWAYS);
				label.setMaxWidth(Double.MAX_VALUE);

				// add components to horizontal box
				hBox.getChildren().add(tSwitch);
				hBox.getChildren().add(label);

				// set listener to toggle switch
				tSwitch.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onShowFlightSelected(newValue, (int) tSwitch.getUserData()));

				// add flights to hide/show list
				flightsContainer_.getChildren().add(hBox);
			}
		}
	}

	/**
	 * Called when show flight selected.
	 *
	 * @param visible
	 *            True if visible.
	 * @param flightID
	 *            Flight ID.
	 */
	private void onShowFlightSelected(boolean visible, int flightID) {
		XYPlot plot = getChart().getXYPlot();
		for (int i = 0; i < plot.getDatasetCount(); i++) {
			XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset(i);
			SeriesKey key = (SeriesKey) dataset.getSeries(0).getKey();
			if (key.getID() == flightID) {
				plot.getRenderer(i).setSeriesVisible(0, visible);
			}
		}
	}

	/**
	 * Called when show markers option is selected.
	 *
	 * @param isSelected
	 *            True if option is selected.
	 */
	private void onShowMarkersSelected(boolean isSelected) {
		XYPlot plot = getChart().getXYPlot();
		for (int i = 0; i < plot.getDatasetCount(); i++) {
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer(i);
			renderer.setBaseShapesVisible(isSelected);
		}
	}

	@FXML
	private void onOkClicked() {

		// get naming parameters
		boolean includeFlightName = includeFlightName_.isSelected();
		boolean includeSequenceName = includeSequenceName_.isSelected();
		boolean includeEID = includeEID_.isSelected();
		boolean includeProgram = includeProgram_.isSelected();
		boolean includeSection = includeSection_.isSelected();
		boolean includeMission = includeMission_.isSelected();

		// no naming selected
		if (!includeSequenceName && !includeEID && !includeProgram && !includeSection && !includeMission && !includeFlightName) {
			String message = "Please select at least 1 naming for plot series.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(namingPane_);
			return;
		}

		// get selected flights
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();

		// create input
		ExternalFlightComparisonInput input = new ExternalFlightComparisonInput();
		input.setShowMarkers(showMarkers_.isSelected());

		// set naming parameters
		input.setIncludeFlightName(includeFlightName);
		input.setIncludeSequenceName(includeSequenceName);
		input.setIncludeEID(includeEID);
		input.setIncludeProgram(includeProgram);
		input.setIncludeSection(includeSection);
		input.setIncludeMission(includeMission);

		// set flight visibility
		for (Node node : flightsContainer_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			int flightID = (int) ts.getUserData();
			input.setFlightVisible(flightID, ts.isSelected());
		}

		// create task
		CompareExternalFlights task = new CompareExternalFlights(input);

		// add flights
		selected.forEach(x -> task.addTypicalFlight((ExternalFlight) x));

		// compare flights
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(task);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {

		// reset naming options
		if (!includeFlightName_.isSelected()) {
			includeFlightName_.setSelected(true);
		}
		includeSequenceName_.setSelected(false);
		includeEID_.setSelected(false);
		includeProgram_.setSelected(false);
		includeSection_.setSelected(false);
		includeMission_.setSelected(false);

		// reset markers
		showMarkers_.setSelected(false);

		// reset show/hide flights
		for (Node node : flightsContainer_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			if (!ts.isSelected()) {
				ts.setSelected(true);
			}
		}
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to compare flights", null);
	}

	/**
	 * Returns the spectrum plot.
	 *
	 * @return The spectrum plot.
	 */
	private JFreeChart getChart() {
		CompareFlightsViewPanel panel = (CompareFlightsViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.COMPARE_FLIGHTS_VIEW);
		return panel.getChart();
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static CompareExternalFlightsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CompareExternalFlightsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CompareExternalFlightsPanel controller = (CompareExternalFlightsPanel) fxmlLoader.getController();

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
