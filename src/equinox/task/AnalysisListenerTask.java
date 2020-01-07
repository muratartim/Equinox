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

import equinox.analysisServer.remote.listener.AnalysisMessageListener;
import equinox.analysisServer.remote.message.AnalysisMessage;
import equinox.analysisServer.remote.message.AnalysisProgress;
import equinox.network.AnalysisServerManager;
import equinox.utility.exception.IgnoredFailureException;

/**
 * Interface for server analysis listener task.
 *
 * @author Murat Artim
 * @date 24 Jan 2018
 * @time 13:33:50
 */
public interface AnalysisListenerTask extends AnalysisMessageListener {

	/**
	 * Waits for server analysis to complete.
	 *
	 * @param task
	 *            Listener task.
	 * @param isAnalysisCompleted
	 *            Atomic boolean to check whether the server analysis is completed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	default void waitForAnalysisServer(InternalEquinoxTask<?> task, AtomicBoolean isAnalysisCompleted) throws Exception {

		// not connected to server
		AnalysisServerManager am = task.getTaskPanel().getOwner().getOwner().getAnalysisServerManager();
		if (!am.isConnected())
			throw new IgnoredFailureException("Analysis service is currently not available. Please connect to the service and try again.");

		// loop while analysis is running
		while (!isAnalysisCompleted.get()) {

			// task cancelled
			if (task.isCancelled())
				return;

			// not connected to server
			if (!am.isConnected())
				throw new IgnoredFailureException("Analysis service is currently not available. Please connect to the service and try again.");

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
	 * Processes analysis message from the server.
	 *
	 * @param serverMessage
	 *            Server message.
	 * @param task
	 *            Listener task.
	 * @param serverMessageRef
	 *            Reference to server message.
	 * @param isAnalysisComplete
	 *            Atomic boolean to check whether the server analysis process is completed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	default void processServerAnalysisMessage(AnalysisMessage serverMessage, InternalEquinoxTask<?> task, AtomicReference<AnalysisMessage> serverMessageRef, AtomicBoolean isAnalysisComplete) throws Exception {

		// analysis progress
		if (serverMessage instanceof AnalysisProgress) {
			task.updateMessage(((AnalysisProgress) serverMessage).getProgressMessage());
		}

		// analysis completed
		else {
			serverMessageRef.set(serverMessage);
			isAnalysisComplete.set(true);
		}
	}
}
