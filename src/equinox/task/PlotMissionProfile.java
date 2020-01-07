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

import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;

import equinox.Equinox;
import equinox.controller.InputPanel;
import equinox.controller.MissionProfileComparisonViewPanel;
import equinox.controller.MissionProfilePanel;
import equinox.controller.MissionProfileViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.MissionProfileComparisonPlotAttributes;
import equinox.data.Pair;
import equinox.data.fileType.StressSequence;
import equinox.process.PlotMissionProfileProcess;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for plot mission profile task.
 *
 * @author Murat Artim
 * @date May 30, 2016
 * @time 10:14:48 PM
 */
public class PlotMissionProfile extends InternalEquinoxTask<XYDataset[]> implements ShortRunningTask, SingleInputTask<StressSequence>, AutomaticTaskOwner<Pair<XYDataset[], MissionProfileComparisonPlotAttributes>> {

	/** Input stress sequence. */
	private StressSequence sequence_;

	/** Step plotting options. */
	private final boolean[] plotStep_;

	/** Increment plotting options. */
	private final boolean plotPosInc_, plotNegInc_;

	/** Plot process. */
	private PlotMissionProfileProcess process_;

	/** Mode of this task. */
	private final boolean isComparison_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Pair<XYDataset[], MissionProfileComparisonPlotAttributes>>> automaticTasks_ = null;

	/**
	 * Creates plot mission profile task.
	 *
	 * @param sequence
	 *            Input stress sequence. Can be null for automatic execution.
	 * @param plotPosInc
	 *            True to plot positive increments.
	 * @param plotNegInc
	 *            True to plot negative increments.
	 * @param plotStep
	 *            Step plotting options. Array size must be 8 (for 8 steps). Null can be given for plotting all steps.
	 * @param isComparison
	 *            True if profile comparison mode.
	 */
	public PlotMissionProfile(StressSequence sequence, boolean plotPosInc, boolean plotNegInc, boolean[] plotStep, boolean isComparison) {
		sequence_ = sequence;
		plotPosInc_ = plotPosInc;
		plotNegInc_ = plotNegInc;
		if (plotStep == null) {
			plotStep_ = new boolean[] { true, true, true, true, true, true, true, true };
		}
		else {
			plotStep_ = plotStep;
		}
		isComparison_ = isComparison;
	}

	@Override
	public void setAutomaticInput(StressSequence input) {
		sequence_ = input;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Pair<XYDataset[], MissionProfileComparisonPlotAttributes>> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Pair<XYDataset[], MissionProfileComparisonPlotAttributes>>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Plot mission profile";
	}

	@Override
	protected XYDataset[] call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_MISSION_PROFILE);

		// update progress info
		updateTitle("Plotting mission profile...");

		// initialize dataset
		XYDataset[] datasets = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			process_ = new PlotMissionProfileProcess(this, sequence_, plotPosInc_, plotNegInc_, plotStep_);
			datasets = process_.start(connection);
		}

		// return data sets
		return datasets;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get chart data
			XYDataset[] datasets = get();

			// user initiated execution
			if (automaticTasks_ == null) {

				// single profile plot
				if (!isComparison_) {

					// get mission profile view panel
					MissionProfileViewPanel panel = (MissionProfileViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PROFILE_VIEW);

					// set data and title
					String title = "Mission Profile";
					String subTitle = sequence_.getName() + ", " + sequence_.getParentItem().getMission();
					double maxDiff = process_.getMaxPositiveIncrement() - process_.getMinNegativeIncrement();
					panel.plottingCompleted((IntervalXYDataset) datasets[0], datasets[1], datasets[2], process_.getSegments(), maxDiff, title, subTitle);

					// show view
					taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.MISSION_PROFILE_VIEW);
				}

				// comparison
				else {

					// get mission profile view panel
					MissionProfileComparisonViewPanel panel = (MissionProfileComparisonViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PROFILE_COMPARISON_VIEW);

					// set data and title
					String title = sequence_.getName() + ", " + sequence_.getParentItem().getMission();
					double maxDiff = process_.getMaxPositiveIncrement() - process_.getMinNegativeIncrement();
					panel.plottingCompleted((IntervalXYDataset) datasets[0], datasets[1], datasets[2], process_.getSegments(), maxDiff, title, sequence_);

					// show view
					taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.MISSION_PROFILE_COMPARISON_VIEW);
				}

				// show plot input panel
				MissionProfilePanel panel = (MissionProfilePanel) taskPanel_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.MISSION_PROFILE_PANEL);
				panel.setMode(isComparison_);
				taskPanel_.getOwner().getOwner().getInputPanel().showSubPanel(InputPanel.MISSION_PROFILE_PANEL);
			}

			// automatic execution
			else {

				// single profile plot
				if (isComparison_) {

					// create plot attributes
					String title = sequence_.getName() + ", " + sequence_.getParentItem().getMission();
					double maxDiff = process_.getMaxPositiveIncrement() - process_.getMinNegativeIncrement();
					MissionProfileComparisonPlotAttributes attributes = new MissionProfileComparisonPlotAttributes(maxDiff, title);

					// manage automatic tasks
					automaticTaskOwnerSucceeded(new Pair<>(datasets, attributes), automaticTasks_, taskPanel_);
				}
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
