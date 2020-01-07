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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.exchangeServer.remote.listener.ExchangeMessageListener;
import equinox.exchangeServer.remote.message.ExchangeMessage;
import equinox.network.ExchangeServerManager;
import equinox.utility.exception.IgnoredFailureException;

/**
 * Interface for all exchange server message listener tasks.
 *
 * @author Murat Artim
 * @date 16 Jul 2018
 * @time 01:36:48
 */
public interface ExchangeMessageListenerTask extends ExchangeMessageListener {

	/**
	 * Waits for exchange server to complete.
	 *
	 * @param task
	 *            Listener task.
	 * @param isServerCompleted
	 *            Atomic boolean to check whether the server process is completed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	default void waitForExchangeServer(InternalEquinoxTask<?> task, AtomicBoolean isServerCompleted) throws Exception {

		// not connected to server
		ExchangeServerManager em = task.getTaskPanel().getOwner().getOwner().getExchangeServerManager();
		if (!em.isConnected())
			throw new IgnoredFailureException("Exchange service is currently not available. Please connect to the service and try again.");

		// loop while server is running
		while (!isServerCompleted.get()) {

			// task cancelled
			if (task.isCancelled())
				return;

			// not connected to server
			if (!em.isConnected())
				throw new IgnoredFailureException("Exchange service is currently not available. Please connect to the service and try again.");

			// sleep a bit
			try {
				Thread.sleep(1000);
			}

			// task interrupted
			catch (InterruptedException e) {
				if (task.isCancelled())
					return;
			}
		}
	}

	/**
	 * Processes exchange message from the server.
	 *
	 * @param serverMessage
	 *            Server message.
	 * @param task
	 *            Listener task.
	 * @param serverMessageRef
	 *            Reference to server message.
	 * @param isServerCompleted
	 *            Atomic boolean to check whether the server process is completed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	default void processServerExchangeMessage(ExchangeMessage serverMessage, InternalEquinoxTask<?> task, AtomicReference<ExchangeMessage> serverMessageRef, AtomicBoolean isServerCompleted) throws Exception {
		serverMessageRef.set(serverMessage);
		isServerCompleted.set(true);
	}
}