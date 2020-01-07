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
import java.util.List;
import java.util.concurrent.ExecutionException;

import equinox.data.EmbeddedTask;
import equinox.data.fileType.ExternalFlight;
import equinox.data.fileType.ExternalStressSequence;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for select all external typical flights task.
 *
 * @author Murat Artim
 * @date 10 Sep 2018
 * @time 10:53:13
 */
public class SelectAllExternalFlights extends InternalEquinoxTask<List<ExternalFlight>> implements ShortRunningTask, SingleInputTask<ExternalStressSequence>, AutomaticTaskOwner<List<ExternalFlight>> {

	/** External stress sequence. */
	private ExternalStressSequence file_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<List<ExternalFlight>>> automaticTasks_ = null;

	/**
	 * Creates select all external typical flights task.
	 *
	 * @param file
	 *            External stress sequence. Can be null for automatic execution.
	 */
	public SelectAllExternalFlights(ExternalStressSequence file) {
		file_ = file;
	}

	@Override
	public void setAutomaticInput(ExternalStressSequence input) {
		file_ = input;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<List<ExternalFlight>> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<List<ExternalFlight>>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public String getTaskTitle() {
		return "Select all external flights";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected List<ExternalFlight> call() throws Exception {
		return file_.getFlights().getFlights();
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// select file
		try {

			// get flight
			List<ExternalFlight> flights = get();

			// manage automatic tasks
			if (automaticTasks_ != null) {
				automaticTaskOwnerSucceeded(flights, automaticTasks_, taskPanel_);
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