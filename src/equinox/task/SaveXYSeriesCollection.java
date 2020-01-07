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

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import equinox.data.EmbeddedTask;
import equinox.data.Pair;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.utility.CrosshairListenerXYPlot;

/**
 * Class for save XY series collection task.
 *
 * @author Murat Artim
 * @date 4 Sep 2018
 * @time 09:35:12
 */
public class SaveXYSeriesCollection extends InternalEquinoxTask<Path> implements ShortRunningTask, SingleInputTask<Pair<XYSeriesCollection, String>>, AutomaticTaskOwner<Path> {

	/** Dataset. */
	private XYSeriesCollection dataset;

	/** X-axis label. */
	private String xAxisLabel;

	/** Path to output file. */
	private final Path output;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save XY series collection task.
	 *
	 * @param dataset
	 *            XY series collection. Can be null for automatic execution.
	 * @param output
	 *            Path to output file.
	 */
	public SaveXYSeriesCollection(XYSeriesCollection dataset, Path output) {
		this.dataset = dataset;
		this.output = output;
	}

	@Override
	public void setAutomaticInput(Pair<XYSeriesCollection, String> input) {
		this.dataset = input.getElement1();
		this.xAxisLabel = input.getElement2();
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
		return "Save XY series collection";
	}

	@Override
	protected Path call() throws Exception {

		// update info
		updateMessage("Saving XY series collection plot...");

		// create empty chart
		JFreeChart chart = CrosshairListenerXYPlot.createXYLineChart("Level Crossings", null, null, null, PlotOrientation.VERTICAL, true, false, false, null);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		XYPlot plot = chart.getXYPlot();
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

		// set chart data
		plot.setDataset(dataset);
		plot.getDomainAxis().setLabel(xAxisLabel);

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