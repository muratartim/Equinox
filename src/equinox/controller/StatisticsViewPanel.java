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
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LayeredBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.SortOrder;

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
 * Class for plot column panel controller.
 *
 * @author Murat Artim
 * @date Apr 6, 2016
 * @time 1:22:45 PM
 */
public class StatisticsViewPanel implements InternalViewSubPanel {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Chart. */
	private JFreeChart chart_;

	/** Chart subtitle. */
	private TextTitle subTitle_;

	/** Chart renderer. */
	private BarRenderer standardRenderer_, layeredRenderer_;

	@FXML
	private VBox root_;

	@FXML
	private SwingNode container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create bar chart
		chart_ = ChartFactory.createBarChart("Statistics", "Category", "Value", null, PlotOrientation.VERTICAL, true, false, false);
		chart_.getLegend().setVisible(false);
		chart_.setBackgroundPaint(new Color(245, 245, 245));
		chart_.setAntiAlias(true);
		chart_.setTextAntiAlias(true);

		// create subtitle
		subTitle_ = new TextTitle();
		chart_.addSubtitle(1, subTitle_);

		// setup plot
		CategoryPlot plot = (CategoryPlot) chart_.getPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.getDomainAxis().setMaximumCategoryLabelLines(10);
		plot.setRangePannable(true);

		// create standard renderer
		standardRenderer_ = (BarRenderer) plot.getRenderer();
		standardRenderer_.setDrawBarOutline(false);
		standardRenderer_.setBarPainter(new StandardBarPainter());
		standardRenderer_.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		standardRenderer_.setBaseToolTipGenerator(null);

		// create layered renderer
		layeredRenderer_ = new LayeredBarRenderer();
		layeredRenderer_.setDrawBarOutline(true);
		layeredRenderer_.setBarPainter(new StandardBarPainter());
		layeredRenderer_.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		layeredRenderer_.setBaseToolTipGenerator(null);

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
		return "Statistics View";
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
		fileChooser.setInitialFileName("Statistics Plot" + FileType.PNG.getExtension());
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
		return "Statistics Plot";
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
	 * Sets item label visibility.
	 *
	 * @param isVisible
	 *            True if items labels should be visible.
	 */
	public void setLabelsVisible(boolean isVisible) {
		CategoryPlot plot = (CategoryPlot) chart_.getPlot();
		CategoryItemRenderer renderer = plot.getRenderer();
		CategoryDataset dataset = plot.getDataset();
		for (int i = 0; i < dataset.getRowCount(); i++) {
			renderer.setSeriesItemLabelsVisible(i, isVisible);
		}
	}

	/**
	 * Sets plot data.
	 *
	 * @param dataset
	 *            Dataset.
	 * @param title
	 *            Chart title.
	 * @param subTitle
	 *            Chart subtitle.
	 * @param xAxisLabel
	 *            Category axis label.
	 * @param yAxisLabel
	 *            Y axis label.
	 * @param legendVisible
	 *            True if legend should be visible.
	 * @param labelsVisible
	 *            True if labels should be visible.
	 * @param isLayered
	 *            True if chart should be layered bar chart.
	 */
	public void setPlotData(CategoryDataset dataset, String title, String subTitle, String xAxisLabel, String yAxisLabel, boolean legendVisible, boolean labelsVisible, boolean isLayered) {

		// set chart title
		chart_.setTitle(title);

		// set sub title
		if (subTitle == null) {
			subTitle_.setVisible(false);
		}
		else {
			subTitle_.setText(subTitle);
			subTitle_.setVisible(true);
		}

		// set legend visibility
		chart_.getLegend().setVisible(legendVisible);

		// set axis labels
		CategoryPlot plot = (CategoryPlot) chart_.getPlot();
		plot.getRangeAxis().setLabel(yAxisLabel);
		plot.getDomainAxis().setLabel(xAxisLabel);
		plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		// set dataset
		plot.setDataset(dataset);

		// standard renderer
		if (!isLayered) {
			if (plot.getRenderer() instanceof LayeredBarRenderer) {
				plot.setRenderer(standardRenderer_);
				plot.setRowRenderingOrder(SortOrder.ASCENDING);
			}
		}

		// layered renderer
		else {
			if ((plot.getRenderer() instanceof LayeredBarRenderer) == false) {
				plot.setRenderer(layeredRenderer_);
				plot.setRowRenderingOrder(SortOrder.DESCENDING);
			}
		}

		// set label visibility
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		for (int i = 0; i < dataset.getRowCount(); i++) {
			renderer.setSeriesItemLabelsVisible(i, labelsVisible);
		}

		// set colors
		if (dataset.getRowCount() <= DamageContributionViewPanel.COLORS.length) {
			for (int i = 0; i < dataset.getRowCount(); i++) {
				renderer.setSeriesPaint(i, DamageContributionViewPanel.COLORS[i]);
				renderer.setSeriesOutlinePaint(i, Color.white);
			}
		}
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static StatisticsViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("StatisticsViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			StatisticsViewPanel controller = (StatisticsViewPanel) fxmlLoader.getController();

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
