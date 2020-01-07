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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.Equinox;
import equinox.dataServer.remote.message.CheckForMaterialUpdatesRequest;
import equinox.dataServer.remote.message.CheckForMaterialUpdatesResponse;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.network.DataServerManager;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for check for material updates task.
 *
 * @author Murat Artim
 * @date 31 May 2017
 * @time 15:06:44
 *
 */
public class CheckForMaterialUpdates extends InternalEquinoxTask<ArrayList<String>> implements ShortRunningTask, DatabaseQueryListenerTask {

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
	public CheckForMaterialUpdates(boolean showNoUpdateInfo) {
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
		return "Check for material updates";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected ArrayList<String> call() throws Exception {

		// check permission
		checkPermission(Permission.CHECK_FOR_MATERIAL_UPDATES);

		// update progress info
		updateTitle("Checking for material updates...");
		updateMessage("Please wait...");

		// get local material ISAMI versions
		ArrayList<String> localMaterialIsamiVersions = getLocalMaterialIsamiVersions();

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		ArrayList<String> toBeDownloaded = new ArrayList<>();

		try {

			// create request message
			CheckForMaterialUpdatesRequest request = new CheckForMaterialUpdatesRequest();
			request.setListenerHashCode(hashCode());
			for (String version : localMaterialIsamiVersions) {
				request.addMaterialIsamiVersion(version);
			}

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
			else if (message instanceof CheckForMaterialUpdatesResponse) {
				toBeDownloaded = ((CheckForMaterialUpdatesResponse) message).getMaterialIsamiVersions();
			}

			// return result
			return toBeDownloaded;
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

			// get to-be-downloaded material ISAMI versions
			ArrayList<String> toBeDownloaded = get();

			// no update available
			if (toBeDownloaded == null || toBeDownloaded.isEmpty()) {

				// show information
				if (showNoUpdateInfo_) {
					String title = "Check for material updates";
					String message = "Material library is up to date! No new update is available.";
					taskPanel_.getOwner().getOwner().getNotificationPane().showOk(title, message);
				}
			}

			// updates available
			else {
				taskPanel_.getOwner().getOwner().getNotificationPane().showMaterialUpdates(toBeDownloaded);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Returns local material ISAMI versions.
	 *
	 * @return Local material ISAMI versions.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static ArrayList<String> getLocalMaterialIsamiVersions() throws Exception {

		// initialize version
		ArrayList<String> isamiVersions = new ArrayList<>();

		// get local database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get library version from fatigue materials table
				try (ResultSet resultSet = statement.executeQuery("select distinct isami_version from fatigue_materials")) {
					while (resultSet.next()) {
						isamiVersions.add(resultSet.getString("isami_version"));
					}
				}
			}
		}

		// return material ISAMI versions
		return isamiVersions;
	}
}
