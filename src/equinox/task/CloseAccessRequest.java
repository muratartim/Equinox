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

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import equinox.Equinox;
import equinox.dataServer.remote.data.AccessRequest;
import equinox.dataServer.remote.data.AccessRequest.AccessRequestInfo;
import equinox.dataServer.remote.message.CloseAccessRequestRequest;
import equinox.dataServer.remote.message.CloseAccessRequestResponse;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.GetAccessRequestsRequest;
import equinox.network.DataServerManager;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for close access request task.
 *
 * @author Murat Artim
 * @date 15 Apr 2018
 * @time 20:19:46
 */
public class CloseAccessRequest extends InternalEquinoxTask<Boolean> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial Id. */
	private static final long serialVersionUID = 1L;

	/** Access request. */
	private final AccessRequest request_;

	/** Closure text. */
	private final String closure_;

	/** True if access is granted. */
	private final boolean isGrantAccess_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates close access request task.
	 *
	 * @param request
	 *            Access request.
	 * @param closure
	 *            Closure text.
	 * @param isGrantAccess
	 *            True if access is granted.
	 */
	public CloseAccessRequest(AccessRequest request, String closure, boolean isGrantAccess) {
		request_ = request;
		closure_ = closure;
		isGrantAccess_ = isGrantAccess;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Close user access request";
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
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.CLOSE_ACCESS_REQUEST);

		// update progress info
		updateTitle("Closing user access request in the database...");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		boolean isClosed = false;

		try {

			// create request message
			CloseAccessRequestRequest request = new CloseAccessRequestRequest();
			request.setListenerHashCode(hashCode());
			request.setRequestId((long) request_.getInfo(AccessRequestInfo.ID));
			request.setClosure(closure_);
			request.setIsGrantAccess(isGrantAccess_);

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
			else if (message instanceof CloseAccessRequestResponse) {
				isClosed = ((CloseAccessRequestResponse) message).isAccessRequestClosed();
			}

			// return result
			return isClosed;
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

		// set results to plugins panel
		try {

			// successfully closed
			if (get()) {

				// refresh requests list
				taskPanel_.getOwner().runTaskInParallel(new GetAccessRequests(isGrantAccess_ ? GetAccessRequestsRequest.GRANTED : GetAccessRequestsRequest.REJECTED));

				// open default mail application
				try {

					// desktop is not supported
					if (!Desktop.isDesktopSupported()) {
						String message = "Cannot open default mail application. Desktop class is not supported.";
						taskPanel_.getOwner().getOwner().getNotificationPane().showWarning(message, null);
						return;
					}

					// get desktop
					Desktop desktop = Desktop.getDesktop();

					// open action is not supported
					if (!desktop.isSupported(Desktop.Action.MAIL)) {
						String message = "Cannot open default mail application. Mail action is not supported.";
						taskPanel_.getOwner().getOwner().getNotificationPane().showWarning(message, null);
						return;
					}

					// open mail application
					String permissionName = (String) request_.getInfo(AccessRequestInfo.PERMISSION_NAME);
					String email = (String) request_.getInfo(AccessRequestInfo.USER_EMAIL);
					String subject = "Equinox user permission request for operation '" + permissionName + "'";
					subject = URLEncoder.encode(subject, "UTF-8").replace("+", "%20");
					String body = "Dear Equinox user,\n\n";
					String acceptedText = "accepted due to following administrator remarks. You can execute the mentioned operation once Equinox is restarted.";
					String rejectedText = "unfortunately rejected due to following administrator remarks. You could contact AF-Twin user administrator for further details.";
					body += "Your permission request for operation '" + permissionName + "' has been " + (isGrantAccess_ ? acceptedText : rejectedText);
					body += "\n\n" + closure_;
					body += "\n\nBest regards,\nAF-Twin User Administration";
					body = URLEncoder.encode(body, "UTF-8").replace("+", "%20");
					desktop.mail(new URI("mailto:" + email + "?subject=" + subject + "&body=" + body));
				}

				// exception occurred
				catch (Exception e) {
					String msg = "Exception occurred during mailing user administrator: ";
					Equinox.LOGGER.log(Level.WARNING, msg, e);
					msg += e.getLocalizedMessage();
					msg += " Click 'Details' for more information.";
					taskPanel_.getOwner().getOwner().getNotificationPane().showError("Problem encountered", msg, e);
				}
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
