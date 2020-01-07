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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import equinox.Equinox;
import equinox.analysisServer.remote.message.AnalysisMessage;
import equinox.analysisServer.remote.message.AnalysisServerStatisticsRequest;
import equinox.analysisServer.remote.message.AnalysisServerStatisticsRequestFailed;
import equinox.analysisServer.remote.message.AnalysisServerStatisticsResponse;
import equinox.controller.HealthMonitorViewPanel;
import equinox.controller.ViewPanel;
import equinox.network.AnalysisServerManager;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.AnalysisServerStatisticsRequestFailedException;

/**
 * Class for get analysis requests task.
 *
 * @author Murat Artim
 * @date 28 Jul 2018
 * @time 01:49:08
 */
public class GetAnalysisRequests extends InternalEquinoxTask<AnalysisServerStatisticsResponse> implements ShortRunningTask, AnalysisListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Server query completion indicator. */
	private final AtomicBoolean isServerCompleted;

	/** Data server query message. */
	private final AtomicReference<AnalysisMessage> serverMessageRef;

	/** Data collection period in minutes. */
	private final long period;

	/**
	 * Creates get server diagnostics task.
	 *
	 * @param period
	 *            Data collection period in minutes.
	 */
	public GetAnalysisRequests(long period) {
		isServerCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
		this.period = period;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get analysis requests";
	}

	@Override
	public void respondToAnalysisMessage(AnalysisMessage message) throws Exception {
		processServerAnalysisMessage(message, this, serverMessageRef, isServerCompleted);
	}

	@Override
	protected AnalysisServerStatisticsResponse call() throws Exception {

		// progress info
		updateMessage("Getting analysis requests...");

		// initialize variables
		AnalysisServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			AnalysisServerStatisticsRequest request = new AnalysisServerStatisticsRequest();
			request.setListenerHashCode(hashCode());
			request.setFrom(new Date(Instant.now().minus(period, ChronoUnit.MINUTES).toEpochMilli()));
			request.setTo(new Date());

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getAnalysisServerManager();
			watcher.addMessageListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForAnalysisServer(this, isServerCompleted);

			// remove from network watcher
			watcher.removeMessageListener(this);
			removeListener = false;

			// enable task canceling
			taskPanel_.updateCancelState(true);

			// get query message
			AnalysisMessage message = serverMessageRef.get();

			// failed
			if (message instanceof AnalysisServerStatisticsRequestFailed)
				throw new AnalysisServerStatisticsRequestFailedException((AnalysisServerStatisticsRequestFailed) message);

			// succeeded
			else if (message instanceof AnalysisServerStatisticsResponse)
				return (AnalysisServerStatisticsResponse) message;

			// invalid server response
			throw new Exception("Invalid server response.");
		}

		// exception occurred
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Analysis server diagnostics request has failed.", e);
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
			panel.setAnalysisRequests(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
