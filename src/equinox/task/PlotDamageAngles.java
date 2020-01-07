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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.Equinox;
import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.Pair;
import equinox.data.StatisticsPlotAttributes;
import equinox.data.fileType.DamageAngle;
import equinox.process.PlotDamageAnglesProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;

/**
 * Class for plot damage angles task.
 *
 * @author Murat Artim
 * @date Apr 7, 2016
 * @time 9:31:32 AM
 */
public class PlotDamageAngles extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask, MultipleInputTask<DamageAngle>, AutomaticTaskOwner<Pair<CategoryDataset, StatisticsPlotAttributes>> {

	/**
	 * Enumeration for results ordering.
	 *
	 * @author Murat Artim
	 * @date Jul 10, 2014
	 * @time 1:05:06 PM
	 */
	public enum ResultOrdering {

	/** Ordering. */
	ANGLE("Order by angles"), DESCENDING("Order by descending eq. stresses"), ASCENDING("Order by ascending eq. stresses");

		/** Name of ordering. */
		private final String name_;

		/**
		 * Creates ordering constant.
		 *
		 * @param name
		 *            Name of ordering.
		 */
		ResultOrdering(String name) {
			name_ = name;
		}

		/**
		 * Returns the name of ordering.
		 *
		 * @return Name of ordering.
		 */
		public String getName() {
			return name_;
		}
	}

	/** Order and data label options. */
	private final boolean showLabels_;

	/** Damage angles. */
	private final List<DamageAngle> damageAngles_;

	/** Results ordering. */
	private final ResultOrdering order_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Pair<CategoryDataset, StatisticsPlotAttributes>>> automaticTasks_ = null;

	/**
	 * Creates plot damage angles task.
	 *
	 * @param damageAngles
	 *            Damage angles. Can be null for automatic execution.
	 * @param order
	 *            Results ordering.
	 * @param showLabels
	 *            True to show labels.
	 */
	public PlotDamageAngles(DamageAngle[] damageAngles, ResultOrdering order, boolean showLabels) {
		damageAngles_ = damageAngles == null ? Collections.synchronizedList(new ArrayList<>()) : Arrays.asList(damageAngles);
		order_ = order;
		showLabels_ = showLabels;
	}

	@Override
	synchronized public void setInputThreshold(int inputThreshold) {
		inputThreshold_ = inputThreshold;
	}

	@Override
	synchronized public void addAutomaticInput(AutomaticTaskOwner<DamageAngle> task, DamageAngle input, ExecutionMode mode) {
		automaticInputAdded(task, input, mode, damageAngles_, inputThreshold_);
	}

	@Override
	synchronized public void inputFailed(AutomaticTaskOwner<DamageAngle> task, ExecutionMode mode) {
		inputThreshold_ = automaticInputFailed(task, mode, damageAngles_, inputThreshold_);
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
		return "Plot damage angles";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// update progress info
		updateTitle("Plotting damage angles...");

		// create data set
		DefaultCategoryDataset dataset = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			dataset = new PlotDamageAnglesProcess(this, damageAngles_, order_).start(connection);
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
			String title = "Damage Angles";
			String xAxisLabel = "Angle (in degrees)";
			String yAxisLabel = "Fatigue Equivalent Stress";
			boolean legendVisible = damageAngles_.size() > 1;

			// user initiated task
			if (automaticTasks_ == null) {

				// get column plot panel
				StatisticsViewPanel panel = (StatisticsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);

				// set chart data to panel
				panel.setPlotData(dataset, title, null, xAxisLabel, yAxisLabel, legendVisible, showLabels_, false);

				// show column chart plot panel
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
			}

			// automatic task
			else {

				// create plot attributes
				StatisticsPlotAttributes plotAttributes = new StatisticsPlotAttributes();
				plotAttributes.setLabelsVisible(showLabels_);
				plotAttributes.setLayered(false);
				plotAttributes.setLegendVisible(legendVisible);
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
