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
import java.util.Collections;
import java.util.HashMap;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleAnchor;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.Segment;
import equinox.data.SeriesKey;
import equinox.data.fileType.Flight;
import equinox.data.input.FlightPlotInput;
import equinox.task.GetPeakInfo;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Class for plot flights panel controller.
 *
 * @author Murat Artim
 * @date Sep 3, 2014
 * @time 1:11:26 PM
 */
public class PlotFlightsPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Peak info array. */
	private RadioButton[] peakInfo_;

	/** Data label and plot options. */
	private ToggleSwitch[] componentOptions_;

	/** Segment list. */
	private HashMap<Flight, ArrayList<Segment>> segments_;

	@FXML
	private VBox root_, flightsContainer_, infoContainer_;

	@FXML
	private ToggleSwitch increment_, dp_, dt_, oneg_, total_, includeSpectrumName_, includeSTFName_, includeEID_, includeSequenceName_, includeProgram_, includeSection_, includeMission_, showMarkers_, showCrosshair_, showPeakInfo_;

	@FXML
	private RadioButton totalStress_, dpPressure_, dtTemperature_, classCode_, dpStress_, dtStress_, onegEvent_, onegIssy_, onegStress_, onegComment_, incEvent_, incIssy_, incFac_, incStress_, incComment_, incLinear_, segment_, peakNumber_;

	@FXML
	private ComboBox<Flight> selectFlight_;

	@FXML
	private ComboBox<Segment> selectSegment_;

	@FXML
	private ToggleButton showSegment_;

	@FXML
	private TitledPane peakInfoPane_, namingPane_;

	@FXML
	private Button refresh_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create segments list
		segments_ = new HashMap<>();

		// setup peak info array
		peakInfo_ = new RadioButton[18];
		peakInfo_[GetPeakInfo.PEAK_VALUE] = totalStress_;
		peakInfo_[GetPeakInfo.CLASS_CODE] = classCode_;
		peakInfo_[GetPeakInfo.ONE_G_FLIGHT_PHASE] = onegEvent_;
		peakInfo_[GetPeakInfo.ONE_G_ISSY_CODE] = onegIssy_;
		peakInfo_[GetPeakInfo.ONE_G_STRESS] = onegStress_;
		peakInfo_[GetPeakInfo.ONE_G_COMMENT] = onegComment_;
		peakInfo_[GetPeakInfo.INCREMENT_FLIGHT_PHASE] = incEvent_;
		peakInfo_[GetPeakInfo.INCREMENT_ISSY_CODE] = incIssy_;
		peakInfo_[GetPeakInfo.INCREMENT_FACTOR] = incFac_;
		peakInfo_[GetPeakInfo.INCREMENT_STRESS] = incStress_;
		peakInfo_[GetPeakInfo.INCREMENT_COMMENT] = incComment_;
		peakInfo_[GetPeakInfo.DELTA_P_PRESSURE] = dpPressure_;
		peakInfo_[GetPeakInfo.DELTA_P_STRESS] = dpStress_;
		peakInfo_[GetPeakInfo.LINEARITY] = incLinear_;
		peakInfo_[GetPeakInfo.SEGMENT] = segment_;
		peakInfo_[GetPeakInfo.PEAK_NUMBER] = peakNumber_;
		peakInfo_[GetPeakInfo.DELTA_T_TEMPERATURE] = dtTemperature_;
		peakInfo_[GetPeakInfo.DELTA_T_STRESS] = dtStress_;

		// set component options
		componentOptions_ = new ToggleSwitch[4];
		componentOptions_[FlightPlotInput.INCREMENT_STRESS_COMP] = increment_;
		componentOptions_[FlightPlotInput.DP_STRESS_COMP] = dp_;
		componentOptions_[FlightPlotInput.DT_STRESS_COMP] = dt_;
		componentOptions_[FlightPlotInput.ONE_G_STRESS_COMP] = oneg_;
		for (ToggleSwitch toggle : componentOptions_) {
			toggle.selectedProperty().addListener(new ChangeListener<Boolean>() {

				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					onComponentSelected();
				}
			});
		}
		total_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onComponentSelected();
			}
		});

		// set naming options
		ToggleSwitch[] namings = { includeSpectrumName_, includeSTFName_, includeEID_, includeSequenceName_, includeProgram_, includeSection_, includeMission_ };
		for (ToggleSwitch toggle : namings) {
			toggle.selectedProperty().addListener(new ChangeListener<Boolean>() {

				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					onNamingOptionSelected();
				}
			});
		}

		// set display options
		showMarkers_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onShowMarkersSelected();
			}
		});
		showCrosshair_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onShowCrosshairSelected();
			}
		});
		showPeakInfo_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onShowPeakInfoSelected();
			}
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
		// no implementation
	}

	@Override
	public String getHeader() {
		return "Plot Flight";
	}

	/**
	 * Returns selected peak info.
	 *
	 * @return Selected peak info.
	 */
	public int getSelectedPeakInfo() {
		for (int i = 0; i < peakInfo_.length; i++) {
			if (peakInfo_[i].isSelected())
				return i;
		}
		return 0;
	}

	/**
	 * Called when plotting process has ended.
	 *
	 * @param segments
	 *            Segments to set.
	 * @param plot
	 *            Chart plot.
	 */
	public void plottingCompleted(HashMap<Flight, ArrayList<Segment>> segments, XYPlot plot) {

		// set segments
		segments_ = segments;

		// set select flight component
		selectFlight_.getItems().setAll(segments.keySet());
		selectFlight_.getSelectionModel().clearSelection();
		selectFlight_.setValue(null);

		// set select segment components
		selectSegment_.getItems().clear();
		selectSegment_.getSelectionModel().clearSelection();
		selectSegment_.setValue(null);
		showSegment_.setSelected(false);
		onSegmentSelected();

		// setup series visibility
		setupSeriesVisibility(plot);

		// reset refresh button
		refresh_.setDisable(true);
	}

	/**
	 * Returns component options.
	 *
	 * @return Component options.
	 */
	public boolean[] getComponentOptions() {
		boolean[] options = new boolean[componentOptions_.length];
		for (int i = 0; i < options.length; i++) {
			options[i] = componentOptions_[i].isSelected();
		}
		return options;
	}

	/**
	 * Returns series naming options.
	 *
	 * @return Series naming options.
	 */
	public boolean[] getNamingOptions() {
		boolean[] options = new boolean[8];
		options[0] = includeSpectrumName_.isSelected();
		options[1] = includeSTFName_.isSelected();
		options[2] = includeEID_.isSelected();
		options[3] = includeSequenceName_.isSelected();
		options[4] = true;
		options[5] = includeProgram_.isSelected();
		options[6] = includeSection_.isSelected();
		options[7] = includeMission_.isSelected();
		return options;
	}

	/**
	 * Returns true if plot on total stress is selected.
	 *
	 * @return True if plot on total stress is selected.
	 */
	public boolean isPlotOnTotalStress() {
		return total_.isSelected();
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

		// setup refresh button
		setupRefreshButton();
	}

	/**
	 * Called when a naming option is selected.
	 */
	private void onNamingOptionSelected() {
		setupRefreshButton();
	}

	@FXML
	private void onRefreshClicked() {

		// check inputs
		if (!checkInputs())
			return;

		// plot
		PlotViewPanel panel = (PlotViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.PLOT_VIEW);
		panel.plot();
	}

	/**
	 * Returns true if all inputs are valid.
	 *
	 * @return True if all inputs are valid.
	 */
	private boolean checkInputs() {

		// get naming parameters
		boolean includeFlightName = true;
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
	private void onPeakInfoSelected() {
		PlotViewPanel panel = (PlotViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.PLOT_VIEW);
		panel.updatePeakInfo();
	}

	/**
	 * Called when show markers option is selected.
	 */
	private void onShowMarkersSelected() {
		XYPlot plot = getChart().getXYPlot();
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setBaseShapesVisible(showMarkers_.isSelected());
	}

	/**
	 * Called when show crosshair option is selected.
	 */
	private void onShowCrosshairSelected() {
		XYPlot plot = getChart().getXYPlot();
		plot.setDomainCrosshairVisible(showCrosshair_.isSelected());
		plot.setRangeCrosshairVisible(showCrosshair_.isSelected());
	}

	/**
	 * Called when show peak info option is selected.
	 */
	private void onShowPeakInfoSelected() {
		XYPlot plot = getChart().getXYPlot();
		if (!showPeakInfo_.isSelected()) {
			plot.clearAnnotations();
			peakInfoPane_.setDisable(true);
		}
		else {
			if (!plot.getAnnotations().isEmpty())
				return;
			PlotViewPanel panel = (PlotViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.PLOT_VIEW);
			TextTitle peakInfo = panel.getPeakInfo();
			XYTitleAnnotation ta = new XYTitleAnnotation(0.98, 0.02, peakInfo, RectangleAnchor.BOTTOM_RIGHT);
			ta.setMaxWidth(0.48);
			plot.addAnnotation(ta);
			peakInfoPane_.setDisable(false);
		}
	}

	@FXML
	private void onFlightSelected() {

		// no segments
		if (segments_.isEmpty())
			return;

		// get selected flight
		Flight selected = selectFlight_.getValue();
		if (selected == null)
			return;

		// get unique segments
		ArrayList<Segment> uniqueSegments = new ArrayList<>();
		for (Segment segment : segments_.get(selected)) {
			if (!uniqueSegments.contains(segment)) {
				uniqueSegments.add(segment);
			}
		}

		// sort segments
		Collections.sort(uniqueSegments);

		// set select segment components
		selectSegment_.getItems().setAll(uniqueSegments);
		selectSegment_.getSelectionModel().clearSelection();
		selectSegment_.setValue(null);
	}

	@FXML
	private void onSegmentSelected() {

		// get plot view panel
		PlotViewPanel panel = (PlotViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.PLOT_VIEW);

		// hide selected
		if (!showSegment_.isSelected()) {
			panel.removeSegmentMarkers();
		}
		else {

			// no segment
			if (segments_.isEmpty())
				return;

			// get selected flight
			Flight flight = selectFlight_.getValue();
			if (flight == null)
				return;

			// get selected segment
			Segment segment = selectSegment_.getValue();
			if (segment == null)
				return;

			// get selected segments
			ArrayList<Segment> segments = new ArrayList<>();
			for (Segment s : segments_.get(flight)) {
				if (s.equals(segment)) {
					segments.add(s);
				}
			}

			// show segment markers
			panel.showSegmentMarkers(segments);
		}
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {

		// reset options
		showMarkers_.setSelected(false);
		onShowMarkersSelected();
		showCrosshair_.setSelected(true);
		onShowCrosshairSelected();
		showPeakInfo_.setSelected(true);
		onShowPeakInfoSelected();

		// reset show segment
		selectFlight_.setValue(null);
		selectFlight_.getSelectionModel().clearSelection();
		selectSegment_.setValue(null);
		selectSegment_.getSelectionModel().clearSelection();
		selectSegment_.getItems().clear();
		showSegment_.setSelected(false);
		onSegmentSelected();

		// reset naming options
		includeSpectrumName_.setSelected(false);
		includeSTFName_.setSelected(false);
		includeEID_.setSelected(false);
		includeSequenceName_.setSelected(false);
		includeProgram_.setSelected(false);
		includeSection_.setSelected(false);
		includeMission_.setSelected(false);

		// reset show peak info
		totalStress_.setSelected(true);
		onPeakInfoSelected();

		// reset plot inputs
		increment_.setSelected(true);
		dp_.setSelected(true);
		dt_.setSelected(true);
		oneg_.setSelected(true);
		total_.setSelected(false);
		total_.setDisable(true);
		setupRefreshButton();

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
		owner_.getOwner().showHelp("How to plot typical flights", null);
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
		XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
		for (int i = 0; i < dataset.getSeriesCount(); i++) {
			SeriesKey key = (SeriesKey) dataset.getSeries(i).getKey();
			if (key.getID() == flightID) {
				plot.getRenderer().setSeriesVisible(i, visible);
			}
		}
	}

	/**
	 * Returns the spectrum plot.
	 *
	 * @return The spectrum plot.
	 */
	private JFreeChart getChart() {
		PlotViewPanel panel = (PlotViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.PLOT_VIEW);
		return panel.getChart();
	}

	/**
	 * Sets series visibility.
	 *
	 * @param plot
	 *            Chart plot.
	 */
	private void setupSeriesVisibility(XYPlot plot) {

		// reset flights container
		flightsContainer_.getChildren().clear();

		// get dataset
		XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();

		// get renderer
		XYItemRenderer renderer = plot.getRenderer();

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
			tSwitch.selectedProperty().addListener(new ChangeListener<Boolean>() {

				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					onShowFlightSelected(newValue, (int) tSwitch.getUserData());
				}
			});

			// add to container
			flightsContainer_.getChildren().add(hBox);
		}
	}

	/**
	 * Sets up refresh button state.
	 *
	 */
	private void setupRefreshButton() {

		// get plot input
		PlotViewPanel panel = (PlotViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.PLOT_VIEW);
		FlightPlotInput input = panel.getInput();
		if (input == null)
			return;

		// check change on stress components
		if (input.isPlotOnTotalStress() != total_.isSelected()) {
			refresh_.setDisable(false);
			return;
		}
		boolean[] newState = { increment_.isSelected(), dp_.isSelected(), dt_.isSelected(), oneg_.isSelected() };
		boolean[] options = input.getPlotComponentOptions();
		for (int i = 0; i < options.length; i++) {
			if (newState[i] != options[i]) {
				refresh_.setDisable(false);
				return;
			}
		}

		// check for change on series naming
		boolean[] newNaming = { includeSpectrumName_.isSelected(), includeSTFName_.isSelected(), includeEID_.isSelected(), includeSequenceName_.isSelected(), true, includeProgram_.isSelected(), includeSection_.isSelected(), includeMission_.isSelected() };
		boolean[] namingOptions = input.getNamingOptions();
		for (int i = 0; i < namingOptions.length; i++) {
			if (newNaming[i] != namingOptions[i]) {
				refresh_.setDisable(false);
				return;
			}
		}

		// check change on plot on total stress
		refresh_.setDisable(input.isPlotOnTotalStress() == total_.isSelected());
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static PlotFlightsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("PlotFlightsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			PlotFlightsPanel controller = (PlotFlightsPanel) fxmlLoader.getController();

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
