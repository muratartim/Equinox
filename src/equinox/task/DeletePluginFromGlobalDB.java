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
import equinox.controller.ViewPanel;
import equinox.data.ClientPluginInfo;
import equinox.dataServer.remote.data.ServerPluginInfo;
import equinox.dataServer.remote.data.ServerPluginInfo.PluginInfoType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.DeletePluginRequest;
import equinox.dataServer.remote.message.DeletePluginResponse;
import equinox.network.DataServerManager;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for delete plugin from global database task.
 *
 * @author Murat Artim
 * @date Mar 30, 2016
 * @time 1:54:24 PM
 */
public class DeletePluginFromGlobalDB extends InternalEquinoxTask<Boolean> implements LongRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Plugin info. */
	private final ClientPluginInfo pluginInfo_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates delete plugin from global database task.
	 *
	 * @param pluginInfo
	 *            Plugin info.
	 */
	public DeletePluginFromGlobalDB(ClientPluginInfo pluginInfo) {
		pluginInfo_ = pluginInfo;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Delete plugin '" + (String) pluginInfo_.getInfo(PluginInfoType.NAME) + "' from AF-Twin database";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.DELETE_EQUINOX_PLUGIN);

		// update progress info
		updateTitle("Deleting plugin from the databse...");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		boolean isDeleted = false;

		try {

			// create server plugin info
			ServerPluginInfo serverInfo = new ServerPluginInfo();
			for (PluginInfoType type : PluginInfoType.values()) {
				serverInfo.setInfo(type, pluginInfo_.getInfo(type));
			}

			// create request message
			DeletePluginRequest request = new DeletePluginRequest();
			request.setListenerHashCode(hashCode());
			request.setPluginInfo(serverInfo);

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
			else if (message instanceof DeletePluginResponse) {
				isDeleted = ((DeletePluginResponse) message).isPluginDeleted();
			}

			// return result
			return isDeleted;
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

			// notify panel
			if (get()) {

				// update plugin view
				if (Equinox.USER.hasPermission(Permission.GET_EQUINOX_PLUGIN_INFO, false, taskPanel_.getOwner().getOwner())) {
					if (taskPanel_.getOwner().getOwner().getViewPanel().getCurrentSubPanelIndex() == ViewPanel.PLUGIN_VIEW) {
						taskPanel_.getOwner().runTaskInParallel(new GetPlugins());
					}
				}
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
