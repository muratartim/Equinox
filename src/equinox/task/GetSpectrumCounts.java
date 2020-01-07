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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import equinox.Equinox;
import equinox.controller.HealthMonitorViewPanel;
import equinox.controller.ViewPanel;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DataServerStatisticsRequestFailed;
import equinox.dataServer.remote.message.GetSpectrumCountsRequest;
import equinox.dataServer.remote.message.GetSpectrumCountsResponse;
import equinox.network.DataServerManager;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.DataServerStatisticsRequestFailedException;

/**
 * Class for get spectrum counts task.
 *
 * @author Murat Artim
 * @date 28 Jul 2018
 * @time 20:21:40
 */
public class GetSpectrumCounts extends InternalEquinoxTask<GetSpectrumCountsResponse> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Server query completion indicator. */
	private final AtomicBoolean isServerCompleted;

	/** Data server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates get user locations task.
	 */
	public GetSpectrumCounts() {
		isServerCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get spectrum counts";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isServerCompleted);
	}

	@Override
	protected GetSpectrumCountsResponse call() throws Exception {

		// progress info
		updateMessage("Getting spectrum counts...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			GetSpectrumCountsRequest request = new GetSpectrumCountsRequest();
			request.setListenerHashCode(hashCode());

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getDataServerManager();
			watcher.addMessageListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForDataServer(this, isServerCompleted);

			// remove from network watcher
			watcher.removeMessageListener(this);
			removeListener = false;

			// enable task canceling
			taskPanel_.updateCancelState(true);

			// get query message
			DataMessage message = serverMessageRef.get();

			// failed
			if (message instanceof DataServerStatisticsRequestFailed)
				throw new DataServerStatisticsRequestFailedException((DataServerStatisticsRequestFailed) message);

			// succeeded
			else if (message instanceof GetSpectrumCountsResponse)
				return (GetSpectrumCountsResponse) message;

			// invalid server response
			throw new Exception("Invalid server response.");
		}

		// exception occurred
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Spectrum counts request has failed.", e);
			return null;
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set data to health monitoring panel
		try {
			HealthMonitorViewPanel panel = (HealthMonitorViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.HEALTH_MONITOR_VIEW);
			panel.setSpectrumCounts(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}