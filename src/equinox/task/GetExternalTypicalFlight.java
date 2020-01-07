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
import equinox.data.fileType.ExternalFlight;
import equinox.data.fileType.ExternalStressSequence;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for get external typical flight task.
 *
 * @author Murat Artim
 * @date 11 Sep 2018
 * @time 15:17:01
 */
public class GetExternalTypicalFlight extends InternalEquinoxTask<ExternalFlight> implements ShortRunningTask, SingleInputTask<ExternalStressSequence>, AutomaticTaskOwner<ExternalFlight> {

	/** Typical flight name. */
	private final String flightName;

	private ExternalStressSequence stressSequence;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<ExternalFlight>> automaticTasks_ = null;

	/**
	 * Creates get external flight task.
	 *
	 * @param flightName
	 *            Typical flight name.
	 */
	public GetExternalTypicalFlight(String flightName) {
		this.flightName = flightName;
	}

	@Override
	public void setAutomaticInput(ExternalStressSequence input) {
		this.stressSequence = input;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<ExternalFlight> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<ExternalFlight>> getAutomaticTasks() {
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
	protected ExternalFlight call() throws Exception {

		// update info
		updateMessage("Returning typical flight...");

		// find and return flight
		for (ExternalFlight flight : stressSequence.getFlights().getFlights()) {
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
			ExternalFlight flight = get();

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