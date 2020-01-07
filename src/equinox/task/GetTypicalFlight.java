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

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.data.EmbeddedTask;
import equinox.data.fileType.Flight;
import equinox.data.fileType.StressSequence;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for get typical flight task.
 *
 * @author Murat Artim
 * @date 5 Sep 2018
 * @time 21:57:10
 */
public class GetTypicalFlight extends InternalEquinoxTask<Flight> implements ShortRunningTask, SingleInputTask<StressSequence>, AutomaticTaskOwner<Flight> {

	/** Typical flight name. */
	private final String flightName;

	private StressSequence stressSequence;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Flight>> automaticTasks_ = null;

	/**
	 * Creates get flight task.
	 *
	 * @param flightName
	 *            Typical flight name.
	 */
	public GetTypicalFlight(String flightName) {
		this.flightName = flightName;
	}

	@Override
	public void setAutomaticInput(StressSequence input) {
		this.stressSequence = input;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Flight> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Flight>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get typical flight";
	}

	@Override
	protected Flight call() throws Exception {

		// update info
		updateMessage("Returning typical flight...");

		// find and return flight
		for (Flight flight : stressSequence.getFlights().getFlights()) {
			if (flight.getName().equals(flightName))
				return flight;
		}

		// flight not found
		addWarning("Could not find typical flight with name '" + flightName + "' under stress sequence '" + stressSequence.getName() + "'. Connected tasks will not be executed.");
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get flight
			Flight flight = get();

			// manage automatic tasks
			automaticTaskOwnerSucceeded(flight, automaticTasks_, taskPanel_);
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