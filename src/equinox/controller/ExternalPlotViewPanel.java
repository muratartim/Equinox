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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.ExternalFlight;
import equinox.data.input.ExternalFlightPlotInput;
import equinox.plugin.FileType;
import equinox.task.PlotExternalTypicalFlights;
import equinox.task.SaveImage;
import equinox.utility.CrosshairListenerXYPlot;
import equinox.utility.CrosshairListenerXYPlot.CrosshairListener;
import equinox.utility.Utility;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for external plot view panel.
 *
 * @author Murat Artim
 * @date Mar 15, 2015
 * @time 4:29:31 PM
 */
public class ExternalPlotViewPanel implements InternalViewSubPanel, CrosshairListener {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Chart. */
	private JFreeChart chart_;

	/** Peak information. */
	private TextTitle peakInfo_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.##");

	/** Plot input. */
	private ExternalFlightPlotInput input_;

	/** Typical flights. */
	private ExternalFlight[] flights_;

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
		SwingUtilities.invokeLater(() -> {
			ChartPanel panel = new ChartPanel(chart_);
			panel.setPopupMenu(null);
			panel.setMouseWheelEnabled(true);
			container_.setContent(panel);
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
		return "External Flight Plot View";
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
		if (crosshairX_ == x && crosshairY_ == y)
			return;

		// update coordinates
		crosshairX_ = x;
		crosshairY_ = y;

		// update info
		peakInfo_.setText("Peak: " + format_.format(x) + ", Stress: " + format_.format(y));

		// set background paint
		XYPlot plot = chart_.getXYPlot();
		XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
		for (int i = 0; i < dataset.getSeriesCount(); i++) {
			XYSeries series = dataset.getSeries(i);
			for (int j = 0; j < series.getItemCount(); j++) {
				if (series.getX(j).doubleValue() == x && series.getY(j).doubleValue() == y) {
					if (plot.getRenderer().isSeriesVisible(i)) {
						peakInfo_.setBackgroundPaint(plot.getRenderer().getSeriesPaint(i));
						return;
					}
				}
			}
		}
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
	public void plot(ExternalFlight[] flights) {

		// create plot input
		input_ = new ExternalFlightPlotInput();
		flights_ = flights;

		// plot
		plot();
	}

	/**
	 * Plots the spectrum with the currently selected plot options.
	 *
	 */
	public void plot() {

		// no input
		if (input_ == null || flights_ == null)
			return;

		// get options panel
		PlotExternalFlightsPanel panel = (PlotExternalFlightsPanel) owner_.getOwner().getInputPanel().getSubPanel(InputPanel.PLOT_EXTERNAL_FLIGHTS_PANEL);

		// set series naming options
		input_.setNamingOptions(panel.getNamingOptions());

		// plot
		PlotExternalTypicalFlights task = new PlotExternalTypicalFlights(input_);
		Arrays.asList(flights_).forEach(x -> task.addTypicalFlight(x));
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(task);
	}

	/**
	 * Returns plot input.
	 *
	 * @return Plot input.
	 */
	public ExternalFlightPlotInput getInput() {
		return input_;
	}

	/**
	 * Called when plotting process has ended.
	 *
	 * @param dataset
	 *            Chart data.
	 */
	public void plottingCompleted(XYDataset dataset) {

		// set dataset
		XYPlot plot = chart_.getXYPlot();
		plot.setDataset(dataset);

		// notify options panel
		PlotExternalFlightsPanel panel = (PlotExternalFlightsPanel) owner_.getOwner().getInputPanel().getSubPanel(InputPanel.PLOT_EXTERNAL_FLIGHTS_PANEL);
		panel.plottingCompleted(plot);
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
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot text view panel.
	 */
	public static ExternalPlotViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ExternalPlotViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			ExternalPlotViewPanel controller = (ExternalPlotViewPanel) fxmlLoader.getController();

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
