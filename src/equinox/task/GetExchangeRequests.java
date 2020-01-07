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
import equinox.controller.HealthMonitorViewPanel;
import equinox.controller.ViewPanel;
import equinox.exchangeServer.remote.message.ExchangeMessage;
import equinox.exchangeServer.remote.message.ExchangeServerStatisticsRequest;
import equinox.exchangeServer.remote.message.ExchangeServerStatisticsRequestFailed;
import equinox.exchangeServer.remote.message.ExchangeServerStatisticsResponse;
import equinox.network.ExchangeServerManager;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.ExchangeServerStatisticsRequestFailedException;

/**
 * Class for get exchange requests task.
 *
 * @author Murat Artim
 * @date 28 Jul 2018
 * @time 01:57:00
 */
public class GetExchangeRequests extends InternalEquinoxTask<ExchangeServerStatisticsResponse> implements ShortRunningTask, ExchangeMessageListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Server query completion indicator. */
	private final AtomicBoolean isServerCompleted;

	/** Data server query message. */
	private final AtomicReference<ExchangeMessage> serverMessageRef;

	/** Data collection period in minutes. */
	private final long period;

	/**
	 * Creates get server diagnostics task.
	 *
	 * @param period
	 *            Data collection period in minutes.
	 */
	public GetExchangeRequests(long period) {
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
		return "Get share requests";
	}

	@Override
	public void respondToExchangeMessage(ExchangeMessage message) throws Exception {
		processServerExchangeMessage(message, this, serverMessageRef, isServerCompleted);
	}

	@Override
	protected ExchangeServerStatisticsResponse call() throws Exception {

		// progress info
		updateMessage("Getting exchange server diagnostics...");

		// initialize variables
		ExchangeServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			ExchangeServerStatisticsRequest request = new ExchangeServerStatisticsRequest();
			request.setListenerHashCode(hashCode());
			request.setFrom(new Date(Instant.now().minus(period, ChronoUnit.MINUTES).toEpochMilli()));
			request.setTo(new Date());

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getExchangeServerManager();
			watcher.addMessageListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForExchangeServer(this, isServerCompleted);

			// remove from network watcher
			watcher.removeMessageListener(this);
			removeListener = false;

			// enable task canceling
			taskPanel_.updateCancelState(true);

			// get query message
			ExchangeMessage message = serverMessageRef.get();

			// failed
			if (message instanceof ExchangeServerStatisticsRequestFailed)
				throw new ExchangeServerStatisticsRequestFailedException((ExchangeServerStatisticsRequestFailed) message);

			// succeeded
			else if (message instanceof ExchangeServerStatisticsResponse)
				return (ExchangeServerStatisticsResponse) message;

			// invalid server response
			throw new Exception("Invalid server response.");
		}

		// exception occurred
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Exchange server diagnostics request has failed.", e);
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
			panel.setExchangeRequests(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
