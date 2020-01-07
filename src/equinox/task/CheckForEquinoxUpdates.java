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

import equinox.Equinox;
import equinox.controller.MainScreen;
import equinox.data.Settings;
import equinox.dataServer.remote.data.EquinoxUpdate;
import equinox.dataServer.remote.message.CheckForEquinoxUpdatesRequest;
import equinox.dataServer.remote.message.CheckForEquinoxUpdatesResponse;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.network.DataServerManager;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for check for Equinox updates task.
 *
 * @author Murat Artim
 * @date May 26, 2014
 * @time 12:14:14 PM
 */
public class CheckForEquinoxUpdates extends TemporaryFileCreatingTask<EquinoxUpdate> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** True if the no update available information should be displayed. */
	private final boolean showNoUpdateInfo_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates check for Equinox updates task.
	 *
	 * @param showNoUpdateInfo
	 *            True if the no update available information should be displayed.
	 */
	public CheckForEquinoxUpdates(boolean showNoUpdateInfo) {
		showNoUpdateInfo_ = showNoUpdateInfo;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Check for container updates";
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
	protected EquinoxUpdate call() throws Exception {

		// check permission
		checkPermission(Permission.CHECK_FOR_EQUINOX_UPDATES);

		// update progress info
		updateTitle("Checking for container updates...");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		EquinoxUpdate update = null;

		try {

			// create request message
			CheckForEquinoxUpdatesRequest request = new CheckForEquinoxUpdatesRequest();
			request.setListenerHashCode(hashCode());
			request.setOsArch(Equinox.OS_ARCH);
			request.setOsType(Equinox.OS_TYPE);
			request.setVersionNumber(Equinox.getContainerVersion());

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
			else if (message instanceof CheckForEquinoxUpdatesResponse) {
				update = ((CheckForEquinoxUpdatesResponse) message).getUpdate();
			}

			// return update
			return update;
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

		try {

			// get update
			final EquinoxUpdate update = get();

			// no new update available
			if (update == null) {

				// show information
				if (showNoUpdateInfo_) {
					String title = "Check for Equinox Container updates";
					String message = "Your Equinox Container is up to date! No new update is available.";
					taskPanel_.getOwner().getOwner().getNotificationPane().showOk(title, message);
				}

				// get main screen
				MainScreen mainScreen = taskPanel_.getOwner().getOwner();

				// check for plugin updates
				if (Equinox.USER.hasPermission(Permission.CHECK_FOR_PLUGIN_UPDATES, false, mainScreen)) {
					if ((boolean) mainScreen.getSettings().getValue(Settings.NOTIFY_PLUGIN_UPDATES)) {
						mainScreen.getActiveTasksPanel().runTaskInParallel(new CheckForPluginUpdates(false));
					}
				}

				// check for material updates
				if (Equinox.USER.hasPermission(Permission.CHECK_FOR_MATERIAL_UPDATES, false, mainScreen)) {
					if ((boolean) mainScreen.getSettings().getValue(Settings.NOTIFY_MATERIAL_UPDATES)) {
						mainScreen.getActiveTasksPanel().runTaskInParallel(new CheckForMaterialUpdates(false));
					}
				}

				// return
				return;
			}

			// create and show notification
			taskPanel_.getOwner().getOwner().getNotificationPane().showContainerUpdates(update);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
