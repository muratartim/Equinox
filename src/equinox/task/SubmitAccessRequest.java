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

import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.SubmitAccessRequestRequest;
import equinox.dataServer.remote.message.SubmitAccessRequestResponse;
import equinox.network.DataServerManager;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for submit access request task.
 *
 * @author Murat Artim
 * @date 14 Apr 2018
 * @time 23:48:30
 */
public class SubmitAccessRequest extends InternalEquinoxTask<Integer> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Requested permission. */
	private final Permission permission;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates submit access request task.
	 *
	 * @param permission
	 *            Requested permission.
	 */
	public SubmitAccessRequest(Permission permission) {
		this.permission = permission;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Sumbit user access request";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Integer call() throws Exception {

		// check permission
		checkPermission(Permission.SUBMIT_ACCESS_REQUEST);

		// update progress info
		updateTitle("Sumbitting access request");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			SubmitAccessRequestRequest request = new SubmitAccessRequestRequest();
			request.setListenerHashCode(hashCode());
			request.setPermission(permission);

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
			else if (message instanceof SubmitAccessRequestResponse)
				return ((SubmitAccessRequestResponse) message).getResponse();

			// invalid response
			throw new Exception("Invalid response received from server.");
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

		// show info
		try {

			// get server response
			int serverResponse = get();

			// request successfully submitted
			if (serverResponse == SubmitAccessRequestResponse.SUBMITTED) {
				String title = "Request submitted";
				String message = "Thank you very much. Your request has been successfully submitted. You will be notified once we process your request.";
				taskPanel_.getOwner().getOwner().getNotificationPane().showInfo(title, message);
			}

			// request already pending
			else if (serverResponse == SubmitAccessRequestResponse.PENDING) {
				String title = "Request pending";
				String message = "Your request has already been previously submitted and pending. You will be notified once we process your request.";
				taskPanel_.getOwner().getOwner().getNotificationPane().showInfo(title, message);
			}

			// request rejected
			else if (serverResponse == SubmitAccessRequestResponse.REJECTED) {
				String title = "Request rejected";
				String message = "Unfortunately your request has been rejected. You could contact AF-Twin user administrator for further details.";
				taskPanel_.getOwner().getOwner().getNotificationPane().showWarning(message, title);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
