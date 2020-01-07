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
package equinox.task;

import java.awt.Color;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LayeredBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.SortOrder;

import equinox.controller.DamageContributionViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.Pair;
import equinox.data.StatisticsPlotAttributes;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for save category dataset task.
 *
 * @author Murat Artim
 * @date 2 Sep 2018
 * @time 00:46:37
 */
public class SaveCategoryDataset extends InternalEquinoxTask<Path> implements ShortRunningTask, SingleInputTask<Pair<CategoryDataset, StatisticsPlotAttributes>>, AutomaticTaskOwner<Path> {

	/** Category dataset. */
	private CategoryDataset dataset;

	/** Plot attributes. */
	private StatisticsPlotAttributes plotAttributes;

	/** Path to output file. */
	private final Path output;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save category dataset task.
	 *
	 * @param dataset
	 *            Category dataset. Can be null for automatic execution.
	 * @param output
	 *            Path to output file.
	 */
	public SaveCategoryDataset(CategoryDataset dataset, Path output) {
		this.dataset = dataset;
		this.output = output;
	}

	@Override
	public void setAutomaticInput(Pair<CategoryDataset, StatisticsPlotAttributes> input) {
		this.dataset = input.getElement1();
		this.plotAttributes = input.getElement2();
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Path> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Path>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save category dataset plot";
	}

	@Override
	protected Path call() throws Exception {

		// update info
		updateMessage("Saving category dataset plot...");

		// create bar chart
		JFreeChart chart = ChartFactory.createBarChart("Statistics", "Category", "Value", null, PlotOrientation.VERTICAL, true, false, false);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// create subtitle
		TextTitle textTitle = new TextTitle();
		chart.addSubtitle(1, textTitle);

		// setup plot
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.getDomainAxis().setMaximumCategoryLabelLines(10);
		plot.setRangePannable(true);

		// set chart title
		chart.setTitle(plotAttributes.getTitle());

		// set sub title
		if (plotAttributes.getSubTitle() == null) {
			textTitle.setVisible(false);
		}
		else {
			textTitle.setText(plotAttributes.getSubTitle());
			textTitle.setVisible(true);
		}

		// set legend visibility
		chart.getLegend().setVisible(plotAttributes.isLegendVisible());

		// set axis labels
		plot.getRangeAxis().setLabel(plotAttributes.getYAxisLabel());
		plot.getDomainAxis().setLabel(plotAttributes.getXAxisLabel());
		plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		// set dataset
		plot.setDataset(dataset);

		// standard renderer
		if (!plotAttributes.isLayered()) {
			BarRenderer standardRenderer = (BarRenderer) plot.getRenderer();
			standardRenderer.setDrawBarOutline(false);
			standardRenderer.setBarPainter(new StandardBarPainter());
			standardRenderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
			standardRenderer.setBaseToolTipGenerator(null);
			plot.setRenderer(standardRenderer);
			plot.setRowRenderingOrder(SortOrder.ASCENDING);
		}

		// layered renderer
		else {
			BarRenderer layeredRenderer = new LayeredBarRenderer();
			layeredRenderer.setDrawBarOutline(true);
			layeredRenderer.setBarPainter(new StandardBarPainter());
			layeredRenderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
			layeredRenderer.setBaseToolTipGenerator(null);
			plot.setRenderer(layeredRenderer);
			plot.setRowRenderingOrder(SortOrder.DESCENDING);
		}

		// set label visibility
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		for (int i = 0; i < dataset.getRowCount(); i++) {
			renderer.setSeriesItemLabelsVisible(i, plotAttributes.isLabelsVisible());
		}

		// set colors
		if (dataset.getRowCount() <= DamageContributionViewPanel.COLORS.length) {
			for (int i = 0; i < dataset.getRowCount(); i++) {
				renderer.setSeriesPaint(i, DamageContributionViewPanel.COLORS[i]);
				renderer.setSeriesOutlinePaint(i, Color.white);
			}
		}

		// setup chart dimensions
		int width = 658;
		int height = 597;

		// plot
		ChartUtilities.saveChartAsPNG(output.toFile(), chart, width, height);

		// return output path
		return output;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get output file
			Path file = get();

			// manage automatic tasks
			automaticTaskOwnerSucceeded(file, automaticTasks_, taskPanel_);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}
}