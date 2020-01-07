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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.data.ClientPluginInfo;
import equinox.data.EquinoxPluginManager;
import equinox.dataServer.remote.data.ServerPluginInfo;
import equinox.dataServer.remote.data.ServerPluginInfo.PluginInfoType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.GetPluginInfoRequest;
import equinox.dataServer.remote.message.GetPluginInfoResponse;
import equinox.network.DataServerManager;
import equinox.plugin.Plugin;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for check for plugin updates task.
 *
 * @author Murat Artim
 * @date Apr 1, 2015
 * @time 11:52:59 AM
 */
public class CheckForPluginUpdates extends TemporaryFileCreatingTask<ArrayList<ClientPluginInfo>> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** True if the no update available information should be displayed. */
	private final boolean showNoUpdateInfo_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates check for plugin updates task.
	 *
	 * @param showNoUpdateInfo
	 *            True if the no update available information should be displayed.
	 */
	public CheckForPluginUpdates(boolean showNoUpdateInfo) {
		showNoUpdateInfo_ = showNoUpdateInfo;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Check for plugin updates";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected ArrayList<ClientPluginInfo> call() throws Exception {

		// check permission
		checkPermission(Permission.CHECK_FOR_PLUGIN_UPDATES);

		// update progress info
		updateTitle("Checking for plugin updates...");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		ArrayList<ClientPluginInfo> plugins = new ArrayList<>();

		try {

			// create request message
			GetPluginInfoRequest request = new GetPluginInfoRequest();
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
			else if (message instanceof GetPluginInfoResponse) {
				processResponse((GetPluginInfoResponse) message, plugins);
			}

			// return plugins
			return plugins;
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

			// get to-be-updated plugins
			ArrayList<ClientPluginInfo> toBeUpdated = get();

			// no update available
			if (toBeUpdated.isEmpty()) {

				// show information
				if (showNoUpdateInfo_) {
					String title = "Check for plugin updates";
					String message = "All plugins are up to date! No new update is available.";
					taskPanel_.getOwner().getOwner().getNotificationPane().showOk(title, message);
				}
			}

			// updates available
			else {
				taskPanel_.getOwner().getOwner().getNotificationPane().showPluginUpdates(toBeUpdated);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Processes server response.
	 *
	 * @param message
	 *            Server response.
	 * @param plugins
	 *            List to extract plugins.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void processResponse(GetPluginInfoResponse message, ArrayList<ClientPluginInfo> plugins) throws Exception {

		// get connection to filer
		try (FilerConnection filerConnection = getFilerConnection()) {

			// get plugin manager
			EquinoxPluginManager pm = taskPanel_.getOwner().getOwner().getPluginManager();

			// loop over plugins
			for (int i = 0; i < message.size(); i++) {

				// get server info
				ServerPluginInfo serverInfo = message.getPlugin(i);
				double version = (double) serverInfo.getInfo(PluginInfoType.VERSION_NUMBER);

				// setup install button
				Plugin existingPlugin = pm.getPlugin((String) serverInfo.getInfo(PluginInfoType.NAME));

				// plugin doesn't exist
				if (existingPlugin == null) {
					continue;
				}

				// no update needed
				else if (existingPlugin.getVersion() == version) {
					continue;
				}

				// update available
				else if (existingPlugin.getVersion() < version) {

					// create client plugin info
					ClientPluginInfo clientInfo = new ClientPluginInfo();
					for (PluginInfoType type : PluginInfoType.values()) {
						clientInfo.setInfo(type, serverInfo.getInfo(type));
					}

					// set plugin image
					String jarName = (String) serverInfo.getInfo(PluginInfoType.JAR_NAME);
					String imageUrl = (String) serverInfo.getInfo(PluginInfoType.IMAGE_URL);
					Path tempImage = getWorkingDirectory().resolve(jarName.replaceFirst(".jar", ".png"));
					filerConnection.getSftpChannel().get(imageUrl, tempImage.toString());
					clientInfo.setImage(tempImage.toFile());

					// add to plugins
					plugins.add(clientInfo);
				}
			}
		}
	}
}
