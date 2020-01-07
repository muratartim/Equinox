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
import equinox.data.SeriesKey;
import equinox.data.input.ExternalFlightPlotInput;
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
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Class for plot external flights panel controller.
 *
 * @author Murat Artim
 * @date Mar 15, 2015
 * @time 4:38:45 PM
 */
public class PlotExternalFlightsPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_, flightsContainer_;

	@FXML
	private ToggleSwitch showMarkers_, showCrosshair_, showPeakInfo_, includeSequenceName_, includeEID_, includeProgram_, includeSection_, includeMission_;

	@FXML
	private TitledPane namingPane_;

	@FXML
	private Button refresh_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set naming options
		ToggleSwitch[] namings = { includeEID_, includeSequenceName_, includeProgram_, includeSection_, includeMission_ };
		for (ToggleSwitch toggle : namings) {
			toggle.selectedProperty().addListener(new ChangeListener<Boolean>() {

				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					setupRefreshButton();
				}
			});
		}

		// set display options
		showMarkers_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onShowMarkersSelected(newValue);
			}
		});
		showCrosshair_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onShowCrosshairSelected(newValue);
			}
		});
		showPeakInfo_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onShowPeakInfoSelected(newValue);
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
	public void showing() {
		// no implementation
	}

	@Override
	public String getHeader() {
		return "Plot Flight";
	}

	/**
	 * Called when plotting process has ended.
	 *
	 * @param plot
	 *            Chart plot.
	 */
	public void plottingCompleted(XYPlot plot) {

		// setup series visibility
		setupSeriesVisibility(plot);

		// reset refresh button
		refresh_.setDisable(true);
	}

	/**
	 * Returns series naming options.
	 *
	 * @return Series naming options.
	 */
	public boolean[] getNamingOptions() {
		boolean[] options = new boolean[6];
		options[0] = includeSequenceName_.isSelected();
		options[1] = includeEID_.isSelected();
		options[2] = true;
		options[3] = includeProgram_.isSelected();
		options[4] = includeSection_.isSelected();
		options[5] = includeMission_.isSelected();
		return options;
	}

	@FXML
	private void onRefreshClicked() {

		// get naming parameters
		boolean includeFlightName = true;
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

		// plot
		ExternalPlotViewPanel panel = (ExternalPlotViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.EXTERNAL_PLOT_VIEW);
		panel.plot();
	}

	/**
	 * Called when show markers option is selected.
	 *
	 * @param isSelected
	 *            True if option is selected.
	 */
	private void onShowMarkersSelected(boolean isSelected) {
		XYPlot plot = getChart().getXYPlot();
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setBaseShapesVisible(isSelected);
	}

	/**
	 * Called when show crosshair option is selected.
	 *
	 * @param isSelected
	 *            True if option is selected.
	 */
	private void onShowCrosshairSelected(boolean isSelected) {
		XYPlot plot = getChart().getXYPlot();
		plot.setDomainCrosshairVisible(isSelected);
		plot.setRangeCrosshairVisible(isSelected);
	}

	/**
	 * Called when show peak info option is selected.
	 *
	 * @param isSelected
	 *            True if option is selected.
	 */
	private void onShowPeakInfoSelected(boolean isSelected) {
		XYPlot plot = getChart().getXYPlot();
		if (!isSelected) {
			plot.clearAnnotations();
		}
		else {
			if (!plot.getAnnotations().isEmpty())
				return;
			ExternalPlotViewPanel panel = (ExternalPlotViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.EXTERNAL_PLOT_VIEW);
			TextTitle peakInfo = panel.getPeakInfo();
			XYTitleAnnotation ta = new XYTitleAnnotation(0.98, 0.02, peakInfo, RectangleAnchor.BOTTOM_RIGHT);
			ta.setMaxWidth(0.48);
			plot.addAnnotation(ta);
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
		showCrosshair_.setSelected(true);
		showPeakInfo_.setSelected(true);

		// reset naming options
		includeSequenceName_.setSelected(false);
		includeEID_.setSelected(false);
		includeProgram_.setSelected(false);
		includeSection_.setSelected(false);
		includeMission_.setSelected(false);

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
		ExternalPlotViewPanel panel = (ExternalPlotViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.EXTERNAL_PLOT_VIEW);
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
		ExternalPlotViewPanel panel = (ExternalPlotViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.EXTERNAL_PLOT_VIEW);
		ExternalFlightPlotInput input = panel.getInput();
		if (input == null)
			return;

		// check for change on series naming
		boolean[] newNaming = { includeSequenceName_.isSelected(), includeEID_.isSelected(), true, includeProgram_.isSelected(), includeSection_.isSelected(), includeMission_.isSelected() };
		boolean[] namingOptions = input.getNamingOptions();
		for (int i = 0; i < namingOptions.length; i++) {
			if (newNaming[i] != namingOptions[i]) {
				refresh_.setDisable(false);
				return;
			}
		}

		// disable (no change)
		refresh_.setDisable(true);
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static PlotExternalFlightsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("PlotExternalFlightsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			PlotExternalFlightsPanel controller = (PlotExternalFlightsPanel) fxmlLoader.getController();

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
