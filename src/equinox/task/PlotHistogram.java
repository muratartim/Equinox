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

import java.sql.Connection;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.Equinox;
import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.Pair;
import equinox.data.StatisticsPlotAttributes;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.HistogramInput;
import equinox.process.PlotHistogramProcess;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for plot histogram task.
 *
 * @author Murat Artim
 * @date Apr 6, 2016
 * @time 2:30:52 PM
 */
public class PlotHistogram extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask, SingleInputTask<SpectrumItem>, AutomaticTaskOwner<Pair<CategoryDataset, StatisticsPlotAttributes>> {

	/** Histogram input. */
	private final HistogramInput input_;

	/** Equivalent stress. */
	private SpectrumItem equivalentStress_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Pair<CategoryDataset, StatisticsPlotAttributes>>> automaticTasks_ = null;

	/**
	 * Creates plot histogram task.
	 *
	 * @param input
	 *            Input.
	 * @param equivalentStress
	 *            Equivalent stress. Can be null for automatic execution.
	 */
	public PlotHistogram(HistogramInput input, SpectrumItem equivalentStress) {
		input_ = input;
		equivalentStress_ = equivalentStress;
	}

	@Override
	public void setAutomaticInput(SpectrumItem input) {
		equivalentStress_ = input;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Pair<CategoryDataset, StatisticsPlotAttributes>> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Pair<CategoryDataset, StatisticsPlotAttributes>>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public String getTaskTitle() {
		return "Plot rainflow histogram";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_TYPICAL_FLIGHT_STATISTICS);

		// update progress info
		updateTitle("Plotting rainflow histogram...");

		// create data set
		DefaultCategoryDataset dataset = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			dataset = new PlotHistogramProcess(this, input_, equivalentStress_).start(connection);
		}

		// return dataset
		return dataset;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get dataset
			CategoryDataset dataset = get();
			String title = "Rainflow Histogram";
			String yAxisLabel = "Number of cycles";
			String xAxisLabel = input_.getDataType().getName();

			// user started task
			if (automaticTasks_ == null) {

				// get column plot panel
				StatisticsViewPanel panel = (StatisticsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);

				// set chart data to panel
				panel.setPlotData(dataset, title, null, xAxisLabel, yAxisLabel, false, input_.getLabelsVisible(), false);

				// show column chart plot panel
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
			}

			// automatic task
			else {

				// create plot attributes
				StatisticsPlotAttributes plotAttributes = new StatisticsPlotAttributes();
				plotAttributes.setLabelsVisible(input_.getLabelsVisible());
				plotAttributes.setLayered(false);
				plotAttributes.setLegendVisible(false);
				plotAttributes.setSubTitle(null);
				plotAttributes.setTitle(title);
				plotAttributes.setXAxisLabel(xAxisLabel);
				plotAttributes.setYAxisLabel(yAxisLabel);

				// manage automatic tasks
				automaticTaskOwnerSucceeded(new Pair<>(dataset, plotAttributes), automaticTasks_, taskPanel_);
			}
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
