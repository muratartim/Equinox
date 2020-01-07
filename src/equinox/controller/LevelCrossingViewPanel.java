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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.plugin.FileType;
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
 * Class for level crossing view panel controller.
 *
 * @author Murat Artim
 * @date Jul 21, 2014
 * @time 11:31:20 AM
 */
public class LevelCrossingViewPanel implements InternalViewSubPanel, CrosshairListener {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Chart. */
	private JFreeChart chart_;

	/** Crosshair information. */
	private TextTitle info_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.##");

	/** Crosshair coordinates. */
	private double crosshairX_ = 0.0, crosshairY_ = 0.0;

	@FXML
	private VBox root_;

	@FXML
	private SwingNode container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create empty chart
		chart_ = CrosshairListenerXYPlot.createXYLineChart("Level Crossing", null, null, null, PlotOrientation.VERTICAL, true, false, false, this);
		chart_.setBackgroundPaint(new Color(245, 245, 245));
		chart_.setAntiAlias(true);
		chart_.setTextAntiAlias(true);

		// setup plot
		XYPlot plot = chart_.getXYPlot();
		LogarithmicAxis xAxis = new LogarithmicAxis("Number of Cycles");
		xAxis.setAllowNegativesFlag(true);
		plot.setDomainAxis(xAxis);
		plot.setRangeAxis(new NumberAxis("Stress"));
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.setDomainPannable(true);
		plot.setRangePannable(true);

		// setup crosshairs
		BasicStroke defdom = (BasicStroke) plot.getDomainCrosshairStroke();
		plot.setDomainCrosshairStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, defdom.getMiterLimit(), defdom.getDashArray(), defdom.getDashPhase()));
		plot.setRangeCrosshairStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, defdom.getMiterLimit(), defdom.getDashArray(), defdom.getDashPhase()));
		plot.setDomainCrosshairPaint(new Color(70, 130, 180));
		plot.setRangeCrosshairPaint(new Color(70, 130, 180));

		// create information box
		info_ = new TextTitle("Select point to see info.");
		info_.setBackgroundPaint(new Color(70, 130, 180));
		info_.setPaint(Color.white);
		info_.setFrame(new BlockBorder(Color.lightGray));
		info_.setPosition(RectangleEdge.BOTTOM);
		XYTitleAnnotation ta = new XYTitleAnnotation(0.98, 0.02, info_, RectangleAnchor.BOTTOM_RIGHT);
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
	public HBox getControls() {
		return null;
	}

	@Override
	public String getHeader() {
		return "Level Crossing View";
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
		fileChooser.setInitialFileName("Level Crossing" + FileType.PNG.getExtension());
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
		return "Level Crossing";
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
	public void crosshairValueChanged(double x, double y) {

		// crosshair coordinates did not change
		if (crosshairX_ == x && crosshairY_ == y)
			return;

		// update coordinates
		crosshairX_ = x;
		crosshairY_ = y;

		// update info
		info_.setText("NOC: " + format_.format(x) + ", Stress: " + format_.format(y));

		// set background paint
		XYPlot plot = chart_.getXYPlot();
		XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
		for (int i = 0; i < dataset.getSeriesCount(); i++) {
			XYSeries series = dataset.getSeries(i);
			for (int j = 0; j < series.getItemCount(); j++) {
				if (series.getX(j).doubleValue() == x && series.getY(j).doubleValue() == y) {
					if (plot.getRenderer().isSeriesVisible(i)) {
						info_.setBackgroundPaint(plot.getRenderer().getSeriesPaint(i));
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
	 * Called when plotting has been completed.
	 *
	 * @param data
	 *            Chart data.
	 * @param xAxisLabel
	 *            X axis label.
	 * @param isExternal
	 *            True if external level crossing was plotted.
	 */
	public void plottingCompleted(XYSeriesCollection data, String xAxisLabel, boolean isExternal) {

		// set chart data
		XYPlot plot = chart_.getXYPlot();
		plot.setDataset(data);
		plot.getDomainAxis().setLabel(xAxisLabel);

		// notify level crossing panel
		if (!isExternal) {
			LevelCrossingPanel panel = (LevelCrossingPanel) owner_.getOwner().getInputPanel().getSubPanel(InputPanel.LEVEL_CROSSING_PANEL);
			panel.plottingCompleted();
		}
		else {
			ExternalLevelCrossingPanel panel = (ExternalLevelCrossingPanel) owner_.getOwner().getInputPanel().getSubPanel(InputPanel.EXTERNAL_LEVEL_CROSSING_PANEL);
			panel.plottingCompleted();
		}
	}

	/**
	 * Returns info title.
	 *
	 * @return Info title.
	 */
	public TextTitle getInfo() {
		return info_;
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static LevelCrossingViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("LevelCrossingViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			LevelCrossingViewPanel controller = (LevelCrossingViewPanel) fxmlLoader.getController();

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
