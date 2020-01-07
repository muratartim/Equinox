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

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.LengthAdjustmentType;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.Segment;
import equinox.data.SeriesKey;
import equinox.data.fileType.Flight;
import equinox.data.input.FlightPlotInput;
import equinox.plugin.FileType;
import equinox.task.GetPeakInfo;
import equinox.task.PlotTypicalFlights;
import equinox.task.SaveImage;
import equinox.utility.CrosshairListenerXYPlot;
import equinox.utility.CrosshairListenerXYPlot.CrosshairListener;
import equinox.utility.Utility;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for plot spectrum panel controller.
 *
 * @author Murat Artim
 * @date Sep 2, 2014
 * @time 4:07:10 PM
 */
public class PlotViewPanel implements InternalViewSubPanel, CrosshairListener {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Chart. */
	private JFreeChart chart_;

	/** Peak information. */
	private TextTitle peakInfo_;

	/** Plot input. */
	private FlightPlotInput input_;

	/** Crosshair coordinates. */
	private double crosshairX_ = 0.0, crosshairY_ = 0.0;

	@FXML
	private VBox root_;

	@FXML
	private SwingNode container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create XY line chart
		chart_ = CrosshairListenerXYPlot.createXYLineChart("Stress Time History", "Time", "Stress", null, PlotOrientation.VERTICAL, true, false, false, this);
		chart_.setBackgroundPaint(new Color(245, 245, 245));
		chart_.setAntiAlias(true);
		chart_.setTextAntiAlias(true);

		// setup plot
		XYPlot plot = chart_.getXYPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setDomainPannable(true);
		plot.setRangePannable(true);

		// create information box
		peakInfo_ = new TextTitle("Select peak to see info.");
		peakInfo_.setBackgroundPaint(new Color(70, 130, 180));
		peakInfo_.setPaint(Color.white);
		peakInfo_.setFrame(new BlockBorder(Color.lightGray));
		peakInfo_.setPosition(RectangleEdge.BOTTOM);
		XYTitleAnnotation ta = new XYTitleAnnotation(0.98, 0.02, peakInfo_, RectangleAnchor.BOTTOM_RIGHT);
		ta.setMaxWidth(0.48);
		plot.addAnnotation(ta);

		// create swing node content
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				ChartPanel panel = new ChartPanel(chart_);
				panel.setPopupMenu(null);
				panel.setMouseWheelEnabled(true);
				container_.setContent(panel);
			}
		});
	}

	@Override
	public ViewPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public HBox getControls() {
		return null;
	}

	@Override
	public String getHeader() {
		return "Flight Plot View";
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
	public void hiding() {
		// no implementation
	}

	@Override
	public boolean canSaveView() {
		return true;
	}

	@Override
	public void saveView() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName("Typical Flight Plot" + FileType.PNG.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File file = FileType.appendExtension(selectedFile, FileType.PNG);

		// take snapshot of the scene
		WritableImage snapshot = container_.snapshot(null, null);

		// create and start task in progress panel
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SaveImage(file, snapshot));
	}

	@Override
	public String getViewName() {
		return "Typical Flight Plot";
	}

	@Override
	public WritableImage getViewImage() {
		return container_.snapshot(null, null);
	}

	@Override
	public void crosshairValueChanged(double x, double y) {

		// crosshair coordinates did not change
		if ((crosshairX_ == x) && (crosshairY_ == y))
			return;

		// update coordinates
		crosshairX_ = x;
		crosshairY_ = y;

		// get peak info
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				updatePeakInfo(x, y);
			}
		});
	}

	/**
	 * Updates peak info from database.
	 *
	 */
	public void updatePeakInfo() {

		// get peak info
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				updatePeakInfo(crosshairX_, crosshairY_);
			}
		});
	}

	/**
	 * Returns the chart of this panel.
	 *
	 * @return The chart of this panel.
	 */
	public JFreeChart getChart() {
		return chart_;
	}

	/**
	 * Plots the spectrum with the currently selected plot options.
	 *
	 * @param flights
	 *            Selected flights to plot.
	 */
	public void plot(Flight[] flights) {

		// create plot input
		input_ = new FlightPlotInput(flights);

		// plot
		plot();
	}

	/**
	 * Plots the spectrum with the currently selected plot options.
	 *
	 */
	public void plot() {

		// no input
		if (input_ == null)
			return;

		// get options panel
		PlotFlightsPanel panel = (PlotFlightsPanel) owner_.getOwner().getInputPanel().getSubPanel(InputPanel.PLOT_FLIGHTS_PANEL);

		// set plot component options
		input_.setPlotComponentOptions(panel.getComponentOptions(), panel.isPlotOnTotalStress());

		// set series naming options
		input_.setNamingOptions(panel.getNamingOptions());

		// plot
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new PlotTypicalFlights(input_));
	}

	/**
	 * Returns plot input.
	 *
	 * @return Plot input.
	 */
	public FlightPlotInput getInput() {
		return input_;
	}

	/**
	 * Sets peak info.
	 *
	 * @param info
	 *            Peak info.
	 */
	public void setPeakInfo(String info) {
		if (info == null)
			return;
		peakInfo_.setText(info);
		chart_.getXYPlot().annotationChanged(null);
	}

	/**
	 * Called when plotting process has ended.
	 *
	 * @param dataset
	 *            Chart data.
	 * @param segments
	 *            Flight segments.
	 */
	public void plottingCompleted(XYDataset dataset, HashMap<Flight, ArrayList<Segment>> segments) {

		// set dataset
		XYPlot plot = chart_.getXYPlot();
		plot.setDataset(dataset);

		// notify options panel
		PlotFlightsPanel panel = (PlotFlightsPanel) owner_.getOwner().getInputPanel().getSubPanel(InputPanel.PLOT_FLIGHTS_PANEL);
		panel.plottingCompleted(segments, plot);
	}

	/**
	 * Removes segment markers.
	 *
	 */
	public void removeSegmentMarkers() {
		getChart().getXYPlot().clearDomainMarkers();
	}

	/**
	 * Shows segment markers.
	 *
	 * @param segments
	 *            Flight segments to highlight.
	 */
	public void showSegmentMarkers(ArrayList<Segment> segments) {

		// get plot
		XYPlot plot = getChart().getXYPlot();

		// clear current markers
		plot.clearDomainMarkers();

		// create and add segment markers
		Color color = new Color(150, 150, 255);
		Font font = new Font("SansSerif", Font.PLAIN, 11);
		for (Segment segment : segments) {
			IntervalMarker marker = new IntervalMarker(segment.getStartPeak(), segment.getEndPeak());
			marker.setLabelOffsetType(LengthAdjustmentType.EXPAND);
			marker.setPaint(color);
			marker.setLabel(segment.toString());
			marker.setLabelFont(font);
			marker.setLabelPaint(color);
			marker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
			marker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
			plot.addDomainMarker(marker, Layer.BACKGROUND);
		}
	}

	/**
	 * Returns peak info title.
	 *
	 * @return Peak info title.
	 */
	public TextTitle getPeakInfo() {
		return peakInfo_;
	}

	/**
	 * Updates peak info from database.
	 *
	 * @param x
	 *            Crosshair X.
	 * @param y
	 *            Crosshair Y.
	 */
	private void updatePeakInfo(double x, double y) {

		// no input
		if (input_ == null)
			return;

		// get selected peak info
		PlotFlightsPanel panel = (PlotFlightsPanel) owner_.getOwner().getInputPanel().getSubPanel(InputPanel.PLOT_FLIGHTS_PANEL);
		int peakInfo = panel.getSelectedPeakInfo();

		// peak info
		if (peakInfo == GetPeakInfo.PEAK_NUMBER) {
			setPeakInfo("Peak number: " + (int) x);
			return;
		}

		// get selected flight
		Flight flight = getSelectedFlight(x, y);
		if (flight == null)
			return;

		// get peak info
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetPeakInfo(flight, (int) x, peakInfo, PlotViewPanel.this));
	}

	/**
	 * Finds and returns selected flight.
	 *
	 * @param x
	 *            Crosshair X.
	 * @param y
	 *            Crosshair Y.
	 * @return Selected flight.
	 */
	private Flight getSelectedFlight(double x, double y) {

		// get selected series name
		String name = null;
		int id = -1;
		XYPlot plot = chart_.getXYPlot();
		XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
		seriesSearch: for (int i = 0; i < dataset.getSeriesCount(); i++) {
			XYSeries series = dataset.getSeries(i);
			if (plot.getRenderer().isSeriesVisible(i)) {
				for (int j = 0; j < series.getItemCount(); j++) {
					if ((series.getX(j).doubleValue() == x) && (series.getY(j).doubleValue() == y)) {
						SeriesKey key = (SeriesKey) series.getKey();
						name = key.getName();
						id = key.getID();
						peakInfo_.setBackgroundPaint(plot.getRenderer().getSeriesPaint(i));
						break seriesSearch;
					}
				}
			}
		}

		// no name found
		if (name == null)
			return null;

		// component stress series
		if (name.contains(",")) {
			name = name.split(",")[0];
		}

		// get selected flight
		for (Flight flight : input_.getFlights()) {
			if (flight.getName().equals(name) && (flight.getID() == id))
				return flight;
		}

		// no flight found
		return null;
	}

	/**
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot text view panel.
	 */
	public static PlotViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("PlotViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			PlotViewPanel controller = (PlotViewPanel) fxmlLoader.getController();

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
