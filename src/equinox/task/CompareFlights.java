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

import org.jfree.chart.JFreeChart;

import equinox.Equinox;
import equinox.controller.CompareFlightsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.fileType.Flight;
import equinox.data.input.FlightComparisonInput;
import equinox.process.CompareFlightsProcess;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;

/**
 * Class for compare flights task.
 *
 * @author Murat Artim
 * @date Sep 16, 2014
 * @time 11:47:59 AM
 */
public class CompareFlights extends InternalEquinoxTask<JFreeChart> implements ShortRunningTask, MultipleInputTask<Flight>, AutomaticTaskOwner<JFreeChart> {

	/** Automatic inputs. */
	private final List<Flight> flights_;

	/** Comparison input. */
	private final FlightComparisonInput input_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<JFreeChart>> automaticTasks_ = null;

	/**
	 * Creates compare flights task.
	 *
	 * @param input
	 *            Comparison input.
	 */
	public CompareFlights(FlightComparisonInput input) {
		input_ = input;
		flights_ = Collections.synchronizedList(new ArrayList<>());
	}

	/**
	 * Adds typical flight.
	 *
	 * @param flight
	 *            Typical flight to add.
	 */
	public void addTypicalFlight(Flight flight) {
		flights_.add(flight);
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<JFreeChart> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<JFreeChart>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	synchronized public void setInputThreshold(int inputThreshold) {
		inputThreshold_ = inputThreshold;
	}

	@Override
	synchronized public void addAutomaticInput(AutomaticTaskOwner<Flight> task, Flight input, ExecutionMode mode) {
		automaticInputAdded(task, input, mode, flights_, inputThreshold_);
	}

	@Override
	synchronized public void inputFailed(AutomaticTaskOwner<Flight> task, ExecutionMode mode) {
		inputThreshold_ = automaticInputFailed(task, mode, flights_, inputThreshold_);
	}

	@Override
	public String getTaskTitle() {
		return "Compare flights";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected JFreeChart call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_TYPICAL_FLIGHT_COMPARISON);

		// update progress info
		updateTitle("Comparing flights...");

		// initialize chart
		JFreeChart chart = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			chart = new CompareFlightsProcess(this, input_, flights_).start(connection);
		}

		// return chart
		return chart;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get chart
			JFreeChart chart = get();

			// user started task
			if (automaticTasks_ == null) {

				// get comparison view panel
				CompareFlightsViewPanel panel = (CompareFlightsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.COMPARE_FLIGHTS_VIEW);

				// set data
				panel.setFlightComparisonChart(chart, false);

				// show flight comparison panel
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.COMPARE_FLIGHTS_VIEW);
			}

			// automatic task
			else {
				automaticTaskOwnerSucceeded(chart, automaticTasks_, taskPanel_);
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
