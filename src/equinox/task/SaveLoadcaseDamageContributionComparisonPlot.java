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

import java.awt.BasicStroke;
import java.awt.Color;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.MultiplePiePlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.util.TableOrder;

import equinox.controller.DamageContributionViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.Pair;
import equinox.data.ui.PieLabelGenerator;
import equinox.dataServer.remote.data.ContributionType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for save loadcase damage contribution comparison plot task.
 *
 * @author Murat Artim
 * @date 16 Sep 2018
 * @time 00:26:09
 */
public class SaveLoadcaseDamageContributionComparisonPlot extends InternalEquinoxTask<Path> implements ShortRunningTask, SingleInputTask<Pair<CategoryDataset, ContributionType>>, AutomaticTaskOwner<Path> {

	/** Path to output file. */
	private final Path output;

	/** Chart dataset. */
	private CategoryDataset dataset;

	/** Contribution type. */
	private ContributionType contributionType;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save loadcase damage contribution comparison plot task.
	 *
	 * @param dataset
	 *            Chart dataset. Can be null for automatic execution.
	 * @param contributionType
	 *            Contribution type. Can be null for automatic execution.
	 * @param output
	 *            Path to output file.
	 */
	public SaveLoadcaseDamageContributionComparisonPlot(CategoryDataset dataset, ContributionType contributionType, Path output) {
		this.output = output;
		this.dataset = dataset;
		this.contributionType = contributionType;
	}

	@Override
	public void setAutomaticInput(Pair<CategoryDataset, ContributionType> input) {
		this.dataset = input.getElement1();
		this.contributionType = input.getElement2();
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
		return "Save loadcase damage contribution comparison plot";
	}

	@Override
	protected Path call() throws Exception {

		// update info
		updateMessage("Saving loadcase damage contribution comparison plot...");

		// set shadow theme
		ChartFactory.setChartTheme(new StandardChartTheme("JFree/Shadow", true));

		// create multiple chart
		JFreeChart chart = ChartFactory.createMultiplePieChart("Contribution Comparison", null, TableOrder.BY_COLUMN, true, false, false);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup multiple plot
		MultiplePiePlot mplot = (MultiplePiePlot) chart.getPlot();
		mplot.setOutlinePaint(null);
		mplot.setBackgroundPaint(null);
		mplot.setNoDataMessage("No data available.");

		// setup sub-chart plot
		JFreeChart subchart = mplot.getPieChart();
		subchart.setBackgroundPaint(null);
		TextTitle title = subchart.getTitle();
		title.setPaint(new Color(112, 128, 144));
		title.setFont(title.getFont().deriveFont(14f));
		PiePlot splot = (PiePlot) subchart.getPlot();
		splot.setNoDataMessage("No data available.");
		splot.setLabelGenerator(new PieLabelGenerator("{0} ({2})"));
		splot.setLabelBackgroundPaint(new Color(220, 220, 220));
		splot.setIgnoreZeroValues(true);
		splot.setMaximumLabelWidth(0.20);
		splot.setInteriorGap(0.04);
		splot.setBaseSectionOutlinePaint(new Color(245, 245, 245));
		splot.setSectionOutlinesVisible(true);
		splot.setBaseSectionOutlineStroke(new BasicStroke(1.5f));
		splot.setBackgroundPaint(new Color(112, 128, 144, 20));
		splot.setOutlinePaint(new Color(112, 128, 144));
		splot.setExplodePercent("Rest", 0.20);

		// set chart title
		String chartTitle = contributionType.getName();
		chartTitle += " Eq. Stress Contribution Comparison";
		chart.setTitle(chartTitle);

		// set dataset
		mplot.setDataset(dataset);

		// set colors
		if (dataset.getRowCount() <= DamageContributionViewPanel.COLORS.length) {
			for (int i = 0; i < dataset.getRowCount(); i++) {
				if (dataset.getRowKey(i).equals("Rest")) {
					splot.setSectionPaint(dataset.getRowKey(i), Color.LIGHT_GRAY);
				}
				else {
					splot.setSectionPaint(dataset.getRowKey(i), DamageContributionViewPanel.COLORS[i]);
				}
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