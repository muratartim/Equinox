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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jfree.data.xy.XYSeriesCollection;

import equinox.Equinox;
import equinox.controller.LevelCrossingViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.Pair;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.LevelCrossingInput;
import equinox.process.PlotLevelCrossingProcess;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;

/**
 * Class for plot level crossing task.
 *
 * @author Murat Artim
 * @date Jul 21, 2014
 * @time 11:11:54 AM
 */
public class PlotLevelCrossing extends InternalEquinoxTask<XYSeriesCollection> implements ShortRunningTask, MultipleInputTask<SpectrumItem>, AutomaticTaskOwner<Pair<XYSeriesCollection, String>> {

	/** Input. */
	private final LevelCrossingInput input_;

	/** Automatic inputs. */
	private final List<SpectrumItem> equivalentStresses_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Pair<XYSeriesCollection, String>>> automaticTasks_ = null;

	/**
	 * Creates plot level crossing task.
	 *
	 * @param input
	 *            Level crossing input.
	 */
	public PlotLevelCrossing(LevelCrossingInput input) {
		input_ = input;
		equivalentStresses_ = Collections.synchronizedList(new ArrayList<>());
	}

	/**
	 * Adds equivalent stress.
	 *
	 * @param equivalentStress
	 *            Equivalent stress to add.
	 */
	public void addEquivalentStress(SpectrumItem equivalentStress) {
		equivalentStresses_.add(equivalentStress);
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Pair<XYSeriesCollection, String>> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Pair<XYSeriesCollection, String>>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	synchronized public void setInputThreshold(int inputThreshold) {
		inputThreshold_ = inputThreshold;
	}

	@Override
	synchronized public void addAutomaticInput(AutomaticTaskOwner<SpectrumItem> task, SpectrumItem input, ExecutionMode mode) {
		automaticInputAdded(task, input, mode, equivalentStresses_, inputThreshold_);
	}

	@Override
	synchronized public void inputFailed(AutomaticTaskOwner<SpectrumItem> task, ExecutionMode mode) {
		inputThreshold_ = automaticInputFailed(task, mode, equivalentStresses_, inputThreshold_);
	}

	@Override
	public String getTaskTitle() {
		return "Plot level crossing";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected XYSeriesCollection call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_LEVEL_CROSSINGS);

		// update progress info
		updateTitle("Plotting level crossing...");

		// initialize dataset
		XYSeriesCollection dataset = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			dataset = new PlotLevelCrossingProcess(this, input_, equivalentStresses_).start(connection);
		}

		// return data set
		return dataset;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get data set
			XYSeriesCollection dataset = get();
			String xAxisLabel = input_.isNormalize() ? "Number of Cycles (Normalized by spectrum validities)" : "Number of Cycles";

			// user started task
			if (automaticTasks_ == null) {

				// get level crossing view panel
				LevelCrossingViewPanel panel = (LevelCrossingViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.LEVEL_CROSSING_VIEW);

				// set data
				panel.plottingCompleted(dataset, xAxisLabel, false);

				// show level crossing view panel
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.LEVEL_CROSSING_VIEW);
			}

			// automatic task
			else {
				automaticTaskOwnerSucceeded(new Pair<>(dataset, xAxisLabel), automaticTasks_, taskPanel_);
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
