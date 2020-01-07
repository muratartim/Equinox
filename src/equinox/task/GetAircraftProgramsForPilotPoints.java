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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.GetAircraftProgramsForPilotPointsRequest;
import equinox.dataServer.remote.message.GetAircraftProgramsForPilotPointsResponse;
import equinox.network.DataServerManager;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for get aircraft programs for pilot points task.
 *
 * @author Murat Artim
 * @date 9 Aug 2017
 * @time 21:48:14
 *
 */
public class GetAircraftProgramsForPilotPoints extends InternalEquinoxTask<ArrayList<String>> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Constant for all aircraft programs. */
	public static final String ALL_PROGRAMS = "All aircraft programs";

	/** Requesting panel. */
	private final PilotPointAircraftProgramRequestingPanel panel_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates get aircraft programs from global database task.
	 *
	 * @param panel
	 *            Requesting panel.
	 */
	public GetAircraftProgramsForPilotPoints(PilotPointAircraftProgramRequestingPanel panel) {
		panel_ = panel;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get aircraft programs";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected ArrayList<String> call() throws Exception {

		// update progress info
		updateTitle("Retreiving aircraft programs from database...");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			GetAircraftProgramsForPilotPointsRequest request = new GetAircraftProgramsForPilotPointsRequest();
			request.setListenerHashCode(hashCode());

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getDataServerManager();
			watcher.addMessageListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForDataServer(this, isQueryCompleted);

			// remove from network watcher
			watcher.removeMessageListener(this);
			removeListener = false;

			// enable task canceling
			taskPanel_.updateCancelState(true);

			// task cancelled
			if (isCancelled())
				return null;

			// get query message
			DataMessage message = serverMessageRef.get();

			// permission denied
			if (message instanceof DatabaseQueryPermissionDenied)
				throw new PermissionDeniedException(((DatabaseQueryPermissionDenied) message).getPermission());

			// query failed
			else if (message instanceof DatabaseQueryFailed)
				throw new ServerDatabaseQueryFailedException((DatabaseQueryFailed) message);

			// query succeeded
			else if (message instanceof GetAircraftProgramsForPilotPointsResponse)
				return ((GetAircraftProgramsForPilotPointsResponse) message).getPrograms();

			// no aircraft program found
			throw new Exception("No aircraft program found for pilot points.");
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

		// set results to bug report panel
		try {
			panel_.setAircraftProgramsForPilotPoints(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for aircraft program requesting panel.
	 *
	 * @author Murat Artim
	 * @date 26 Jul 2017
	 * @time 13:32:57
	 *
	 */
	public interface PilotPointAircraftProgramRequestingPanel {

		/**
		 * Sets given list of aircraft programs.
		 *
		 * @param programs
		 *            Aircraft programs.
		 */
		void setAircraftProgramsForPilotPoints(Collection<String> programs);
	}
}
