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

import equinox.dataServer.remote.listener.DataMessageListener;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryProgress;
import equinox.network.DataServerManager;
import equinox.utility.exception.IgnoredFailureException;

/**
 * Interface for server database query listener task.
 *
 * @author Murat Artim
 * @date 24 Jan 2018
 * @time 11:58:48
 */
public interface DatabaseQueryListenerTask extends DataMessageListener {

	/**
	 * Waits for server database query to complete.
	 *
	 * @param task
	 *            Listener task.
	 * @param isQueryCompleted
	 *            Atomic boolean to check whether the server query process is completed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	default void waitForDataServer(InternalEquinoxTask<?> task, AtomicBoolean isQueryCompleted) throws Exception {

		// not connected to server
		DataServerManager dm = task.getTaskPanel().getOwner().getOwner().getDataServerManager();
		if (!dm.isConnected())
			throw new IgnoredFailureException("Data service is currently not available. Please connect to the service and try again.");

		// loop while query is running
		while (!isQueryCompleted.get()) {

			// task cancelled
			if (task.isCancelled())
				return;

			// not connected to server
			if (!dm.isConnected())
				throw new IgnoredFailureException("Data service is currently not available. Please connect to the service and try again.");

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
	 * Processes database query message from the server.
	 *
	 * @param serverMessage
	 *            Server message.
	 * @param task
	 *            Listener task.
	 * @param serverMessageRef
	 *            Reference to server message.
	 * @param isQueryCompleted
	 *            Atomic boolean to check whether the server query process is completed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	default void processServerDataMessage(DataMessage serverMessage, InternalEquinoxTask<?> task, AtomicReference<DataMessage> serverMessageRef, AtomicBoolean isQueryCompleted) throws Exception {

		// database query progress
		if (serverMessage instanceof DatabaseQueryProgress) {
			task.updateMessage(((DatabaseQueryProgress) serverMessage).getProgressMessage());
		}

		// database query completed
		else {
			serverMessageRef.set(serverMessage);
			isQueryCompleted.set(true);
		}
	}
}
