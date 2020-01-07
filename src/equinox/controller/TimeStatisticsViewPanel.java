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
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleInsets;

import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.plugin.FileType;
import equinox.task.SaveImage;
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
 * Class for time statistics view panel controller.
 *
 * @author Murat Artim
 * @date 21 Jul 2017
 * @time 11:12:05
 *
 */
public class TimeStatisticsViewPanel implements InternalViewSubPanel {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Chart. */
	private JFreeChart chart_;

	@FXML
	private VBox root_;

	@FXML
	private SwingNode container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create chart
		chart_ = ChartFactory.createTimeSeriesChart("Time Statistics", // title
				"Time", // x-axis label
				"Value", // y-axis label
				null, // data
				true, // create legend?
				false, // generate tooltips?
				false // generate URLs?
		);
		chart_.setBackgroundPaint(new Color(245, 245, 245));
		chart_.getLegend().setVisible(false);
		chart_.setAntiAlias(true);
		chart_.setTextAntiAlias(true);

		// setup plot
		XYPlot plot = (XYPlot) chart_.getPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		plot.setRangePannable(true);

		// set renderer properties
		XYItemRenderer r = plot.getRenderer();
		if (r instanceof XYLineAndShapeRenderer) {
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
			renderer.setBaseShapesVisible(true);
			renderer.setBaseShapesFilled(true);
			renderer.setDrawSeriesLineAsPath(true);
		}

		// set axis properties
		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat());

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
		return "Time Statistics View";
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
		fileChooser.setInitialFileName("Time Statistics Plot" + FileType.PNG.getExtension());
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
		return "Time Statistics Plot";
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

	/**
	 * Sets plot data.
	 *
	 * @param dataset
	 *            Dataset.
	 * @param title
	 *            Chart title.
	 * @param xAxisLabel
	 *            Category axis label.
	 * @param yAxisLabel
	 *            Y axis label.
	 * @param legendVisible
	 *            True if legend should be visible.
	 */
	public void setPlotData(TimeSeriesCollection dataset, String title, String xAxisLabel, String yAxisLabel, boolean legendVisible) {

		// set chart title
		chart_.setTitle(title);

		// set legend visibility
		chart_.getLegend().setVisible(legendVisible);

		// set axis labels
		XYPlot plot = (XYPlot) chart_.getPlot();
		plot.getRangeAxis().setLabel(yAxisLabel);
		plot.getDomainAxis().setLabel(xAxisLabel);

		// set dataset
		plot.setDataset(dataset);
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static TimeStatisticsViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("TimeStatisticsViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			TimeStatisticsViewPanel controller = (TimeStatisticsViewPanel) fxmlLoader.getController();

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
