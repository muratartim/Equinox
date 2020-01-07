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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.plugin.FileType;
import equinox.task.SaveImage;
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
 * Class for mission parameter plot view panel controller.
 *
 * @author Murat Artim
 * @date Nov 27, 2014
 * @time 3:04:07 PM
 */
public class MissionParameterPlotViewPanel implements InternalViewSubPanel, ChartMouseListener {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Chart. */
	private JFreeChart chart_;

	/** Chart panel. */
	private ChartPanel chartPanel_;

	/** Crosshair overlay. */
	private CrosshairOverlay crosshairOverlay_;

	/** X crosshair. */
	private Crosshair xCrosshair_;

	/** Y crosshairs. */
	private Crosshair[] yCrosshairs_;

	/** Header. */
	private String header_ = "Mission Parameters View";

	@FXML
	private VBox root_;

	@FXML
	private SwingNode container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create empty chart
		chart_ = ChartFactory.createXYLineChart("Mission Parameters Plot", "Mission Parameter", "", null);
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
		NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
		xAxis.setAutoRangeIncludesZero(false);
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setAutoRangeIncludesZero(false);
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setBaseShapesVisible(true);
		plot.setDomainPannable(true);
		plot.setRangePannable(true);

		// create swing node content
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				chartPanel_ = new ChartPanel(chart_);
				chartPanel_.setPopupMenu(null);
				chartPanel_.setMouseWheelEnabled(true);
				container_.setContent(chartPanel_);
				chartPanel_.addChartMouseListener(MissionParameterPlotViewPanel.this);
			}
		});
	}

	@Override
	public ViewPanel getOwner() {
		return owner_;
	}

	@Override
	public HBox getControls() {
		return null;
	}

	@Override
	public String getHeader() {
		return header_;
	}

	@Override
	public Parent getRoot() {
		return root_;
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
		fileChooser.setInitialFileName("Mission Parameters Plot" + FileType.PNG.getExtension());
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
		return "Mission Parameters Plot";
	}

	@Override
	public WritableImage getViewImage() {
		return container_.snapshot(null, null);
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
	public void chartMouseClicked(ChartMouseEvent event) {
		// no implementation
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent event) {

		// no data
		if (crosshairOverlay_ == null)
			return;

		// get data area and plot
		Rectangle2D dataArea = chartPanel_.getScreenDataArea();
		XYPlot plot = (XYPlot) event.getChart().getPlot();

		// calculate and set x crosshair value
		ValueAxis xAxis = plot.getDomainAxis();
		double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
		xCrosshair_.setValue(x);

		// calculate and set y crosshair values
		for (int i = 0; i < yCrosshairs_.length; i++) {
			double y = DatasetUtilities.findYValue(plot.getDataset(), i, x);
			yCrosshairs_[i].setValue(y);
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
	 * Called when show series selected.
	 *
	 * @param visible
	 *            True if visible.
	 * @param seriesID
	 *            Series ID.
	 */
	public void onShowSeriesSelected(boolean visible, int seriesID) {
		XYPlot plot = chart_.getXYPlot();
		if (plot.getRenderer().isSeriesVisible(seriesID) != visible) {
			plot.getRenderer().setSeriesVisible(seriesID, visible);
			if (yCrosshairs_ != null) {
				yCrosshairs_[seriesID].setVisible(visible);
			}
		}
	}

	/**
	 * Called when show crosshairs selected.
	 *
	 * @param show
	 *            True to show crosshairs.
	 */
	public void onShowCrosshairsSelected(boolean show) {
		if ((xCrosshair_ == null) || (yCrosshairs_ == null))
			return;
		xCrosshair_.setVisible(show);
		XYItemRenderer renderer = chart_.getXYPlot().getRenderer();
		for (int i = 0; i < yCrosshairs_.length; i++) {
			if (!show) {
				yCrosshairs_[i].setVisible(false);
			}
			else {
				yCrosshairs_[i].setVisible(renderer.isSeriesVisible(i));
			}
		}
	}

	/**
	 * Called when plotting has been completed.
	 *
	 * @param data
	 *            Chart data.
	 * @param title
	 *            Chart title.
	 * @param xAxisLabel
	 *            X axis label.
	 * @param yAxisLabel
	 *            Y axis label.
	 * @param panel
	 *            Panel awaiting for plot completion notification. Can be null.
	 * @param xAxisInverted
	 *            True if X axis should be inverted.
	 * @param yAxisInverted
	 *            True if Y axis should be inverted.
	 * @param panelHeader
	 *            Panel header.
	 */
	public void plottingCompleted(XYSeriesCollection data, String title, String xAxisLabel, String yAxisLabel, PlotCompletionPanel panel, boolean xAxisInverted, boolean yAxisInverted, String panelHeader) {

		// set panel header
		header_ = panelHeader;

		// invoke on AWT event dispatching thread
		SwingUtilities.invokeLater(() -> {

			// set chart data
			XYPlot plot = chart_.getXYPlot();
			plot.setDataset(data);
			chart_.setTitle(title);
			plot.getDomainAxis().setLabel(xAxisLabel);
			plot.getRangeAxis().setLabel(yAxisLabel);

			// inverted axis
			plot.getDomainAxis().setInverted(xAxisInverted);
			plot.getRangeAxis().setInverted(yAxisInverted);

			// remove overlay (if there is)
			if (crosshairOverlay_ != null) {
				chartPanel_.removeOverlay(crosshairOverlay_);
			}

			// create new overlay
			crosshairOverlay_ = new CrosshairOverlay();

			// create and add x crosshair
			xCrosshair_ = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0f));
			xCrosshair_.setLabelVisible(true);
			crosshairOverlay_.addDomainCrosshair(xCrosshair_);

			// create and add y crosshairs
			yCrosshairs_ = new Crosshair[data.getSeriesCount()];
			for (int i = 0; i < yCrosshairs_.length; i++) {
				yCrosshairs_[i] = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0f));
				yCrosshairs_[i].setLabelVisible(true);
				if ((i % 2) != 0) {
					yCrosshairs_[i].setLabelAnchor(RectangleAnchor.TOP_RIGHT);
				}
				crosshairOverlay_.addRangeCrosshair(yCrosshairs_[i]);
			}

			// add overlay to chart panel
			chartPanel_.addOverlay(crosshairOverlay_);

			// notify panel on JavaFX thread
			if (panel != null) {
				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						panel.plottingCompleted(data);
					}
				});
			}
		});
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static MissionParameterPlotViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("MissionParameterPlotViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			MissionParameterPlotViewPanel controller = (MissionParameterPlotViewPanel) fxmlLoader.getController();

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

	/**
	 * Interface for panels awaiting for plot completion notification.
	 *
	 * @author Murat Artim
	 * @date Sep 25, 2015
	 * @time 4:34:20 PM
	 */
	public interface PlotCompletionPanel {

		/**
		 * Called when plotting is completed.
		 *
		 * @param data
		 *            Plot data.
		 */
		void plottingCompleted(XYSeriesCollection data);
	}
}
