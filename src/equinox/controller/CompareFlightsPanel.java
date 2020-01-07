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
import java.util.Collection;
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
import equinox.data.Segment;
import equinox.data.SeriesKey;
import equinox.data.fileType.Flight;
import equinox.data.input.FlightComparisonInput;
import equinox.data.input.FlightPlotInput;
import equinox.task.CompareFlights;
import equinox.task.GetFlightSegments;
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
 * Class for compare flights panel controller.
 *
 * @author Murat Artim
 * @date Sep 15, 2014
 * @time 6:04:57 PM
 */
public class CompareFlightsPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Data label and plot options. */
	private ToggleSwitch[] componentOptions_;

	@FXML
	private VBox root_, flightsContainer_;

	@FXML
	private ComboBox<Segment> selectSegment_;

	@FXML
	private ToggleSwitch increment_, dp_, dt_, oneg_, total_, includeFlightName_, includeSpectrumName_, includeSTFName_, includeEID_, includeSequenceName_, includeProgram_, includeSection_, includeMission_, showMarkers_;

	@FXML
	private TitledPane namingPane_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(0));

		// set component options
		componentOptions_ = new ToggleSwitch[4];
		componentOptions_[FlightPlotInput.INCREMENT_STRESS_COMP] = increment_;
		componentOptions_[FlightPlotInput.DP_STRESS_COMP] = dp_;
		componentOptions_[FlightPlotInput.DT_STRESS_COMP] = dt_;
		componentOptions_[FlightPlotInput.ONE_G_STRESS_COMP] = oneg_;
		for (ToggleSwitch toggle : componentOptions_) {
			toggle.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onComponentSelected());
		}

		// set display options
		showMarkers_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onShowMarkersSelected());
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

		// reset combo boxes
		flightsContainer_.getChildren().clear();

		// get selected flights
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();

		// get common flight segments
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetFlightSegments(selected.toArray(new Flight[selected.size()])));
	}

	@Override
	public String getHeader() {
		return "Compare Flights";
	}

	/**
	 * Sets common flight segments.
	 *
	 * @param segments
	 *            List of flight segments.
	 */
	public void setCommonSegments(Collection<Segment> segments) {

		// reset segments
		selectSegment_.getItems().clear();
		selectSegment_.getSelectionModel().clearSelection();
		selectSegment_.setValue(null);

		// set segments
		selectSegment_.getItems().setAll(segments);
		selectSegment_.getItems().add(0, new Segment("All segments", -1));
		selectSegment_.getSelectionModel().selectFirst();

		// plot
		onOkClicked();
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

				// add to container
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
	 * Called when a stress component option is selected.
	 */
	private void onComponentSelected() {

		// setup components
		if (!increment_.isSelected() && !dp_.isSelected() && !dt_.isSelected() && !oneg_.isSelected()) {
			total_.setSelected(false);
			total_.setDisable(true);
			return;
		}
		if (increment_.isSelected() && dp_.isSelected() && dt_.isSelected() && oneg_.isSelected()) {
			total_.setSelected(false);
			total_.setDisable(true);
		}
		else {
			total_.setDisable(false);
		}
	}

	/**
	 * Called when show markers option is selected.
	 */
	private void onShowMarkersSelected() {
		XYPlot plot = getChart().getXYPlot();
		for (int i = 0; i < plot.getDatasetCount(); i++) {
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer(i);
			renderer.setBaseShapesVisible(showMarkers_.isSelected());
		}
	}

	@FXML
	private void onOkClicked() {

		// check inputs
		if (!checkInputs())
			return;

		// get selected segment
		Segment segment = selectSegment_.getSelectionModel().getSelectedIndex() == 0 ? null : selectSegment_.getValue();

		// create input
		FlightComparisonInput input = new FlightComparisonInput(segment);
		input.setPlotComponentOptions(getComponentOptions(), total_.isSelected());
		input.setShowMarkers(showMarkers_.isSelected());

		// get naming parameters
		boolean includeFlightName = includeFlightName_.isSelected();
		boolean includeSpectrumName = includeSpectrumName_.isSelected();
		boolean includeSTFName = includeSTFName_.isSelected();
		boolean includeEID = includeEID_.isSelected();
		boolean includeSequenceName = includeSequenceName_.isSelected();
		boolean includeProgram = includeProgram_.isSelected();
		boolean includeSection = includeSection_.isSelected();
		boolean includeMission = includeMission_.isSelected();

		// set naming parameters
		input.setIncludeFlightName(includeFlightName);
		input.setIncludeSpectrumName(includeSpectrumName);
		input.setIncludeSTFName(includeSTFName);
		input.setIncludeEID(includeEID);
		input.setIncludeSequenceName(includeSequenceName);
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
		CompareFlights task = new CompareFlights(input);

		// add typical flights
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();
		Flight[] flights = selected.toArray(new Flight[selected.size()]);
		for (Flight flight : flights) {
			task.addTypicalFlight(flight);
		}

		// compare flights
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(task);
	}

	/**
	 * Returns true if all inputs are valid.
	 *
	 * @return True if all inputs are valid.
	 */
	private boolean checkInputs() {

		// get naming parameters
		boolean includeFlightName = includeFlightName_.isSelected();
		boolean includeSpectrumName = includeSpectrumName_.isSelected();
		boolean includeSTFName = includeSTFName_.isSelected();
		boolean includeEID = includeEID_.isSelected();
		boolean includeSequenceName = includeSequenceName_.isSelected();
		boolean includeProgram = includeProgram_.isSelected();
		boolean includeSection = includeSection_.isSelected();
		boolean includeMission = includeMission_.isSelected();

		// no naming selected
		if (!includeSpectrumName && !includeSTFName && !includeEID && !includeSequenceName && !includeProgram && !includeSection && !includeMission && !includeFlightName) {
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

		// get component parameters
		boolean increment = increment_.isSelected();
		boolean oneg = oneg_.isSelected();
		boolean deltap = dp_.isSelected();
		boolean deltat = dt_.isSelected();

		// no stress component selected
		if (!increment && !oneg && !deltap && !deltat) {
			String message = "Please select at least 1 stress component.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(increment_);
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

		// reset segments
		selectSegment_.getSelectionModel().selectFirst();

		// reset stress components
		increment_.setSelected(true);
		dp_.setSelected(true);
		dt_.setSelected(true);
		oneg_.setSelected(true);
		total_.setSelected(false);
		total_.setDisable(true);

		// reset naming options
		includeFlightName_.setSelected(true);
		includeSpectrumName_.setSelected(false);
		includeSTFName_.setSelected(false);
		includeEID_.setSelected(false);
		includeSequenceName_.setSelected(false);
		includeProgram_.setSelected(false);
		includeSection_.setSelected(false);
		includeMission_.setSelected(false);

		// reset markers
		showMarkers_.setSelected(false);
		onShowMarkersSelected();

		// reset show/hide flights
		for (Node node : flightsContainer_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			if (!ts.isSelected()) {
				ts.setSelected(true);
				onShowFlightSelected(true, (int) ts.getUserData());
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
	 * Returns component options.
	 *
	 * @return Component options.
	 */
	private boolean[] getComponentOptions() {
		boolean[] options = new boolean[componentOptions_.length];
		for (int i = 0; i < options.length; i++) {
			options[i] = componentOptions_[i].isSelected();
		}
		return options;
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static CompareFlightsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CompareFlightsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CompareFlightsPanel controller = (CompareFlightsPanel) fxmlLoader.getController();

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
