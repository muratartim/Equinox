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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.DownloadPilotPointAttributesRequest;
import equinox.dataServer.remote.message.DownloadPilotPointAttributesResponse;
import equinox.network.DataServerManager;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for download pilot point attributes task.
 *
 * @author Murat Artim
 * @date 7 Jul 2018
 * @time 00:50:30
 */
public class DownloadPilotPointAttributes extends TemporaryFileCreatingTask<Void> implements LongRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Pilot point info. */
	private final PilotPointInfo info_;

	/** Output file. */
	private Path output_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates download pilot point task.
	 *
	 * @param info
	 *            Pilot point info.
	 * @param output
	 *            Output file.
	 */
	public DownloadPilotPointAttributes(PilotPointInfo info, Path output) {
		info_ = info;
		output_ = output;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Download pilot point attributes";
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
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.DOWNLOAD_PILOT_POINT);

		// update progress info
		updateTitle("Downloading pilot point attributes");

		// download pilot point data
		downloadData();

		// return
		return null;
	}

	/**
	 * Downloads pilot point attributes from filer.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void downloadData() throws Exception {

		// update progress info
		updateTitle("Downloading pilot point attributes");
		updateMessage("Downloading pilot point attributes '" + (String) info_.getInfo(PilotPointInfoType.NAME) + "'...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// get pilot point ID
			long ppID = (long) info_.getInfo(PilotPointInfoType.ID);

			// create request message
			DownloadPilotPointAttributesRequest request = new DownloadPilotPointAttributesRequest();
			request.setListenerHashCode(hashCode());
			request.setDownloadId(ppID);

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
				return;

			// get query message
			DataMessage message = serverMessageRef.get();

			// permission denied
			if (message instanceof DatabaseQueryPermissionDenied)
				throw new PermissionDeniedException(((DatabaseQueryPermissionDenied) message).getPermission());

			// query failed
			else if (message instanceof DatabaseQueryFailed)
				throw new ServerDatabaseQueryFailedException((DatabaseQueryFailed) message);

			// query succeeded
			else if (message instanceof DownloadPilotPointAttributesResponse) {

				// get download URL
				String downloadUrl = ((DownloadPilotPointAttributesResponse) message).getDownloadUrl();

				// download file
				if (downloadUrl != null) {
					try (FilerConnection filer = getFilerConnection()) {
						if (filer.fileExists(downloadUrl)) {
							filer.getSftpChannel().get(downloadUrl, output_.toString());
						}
					}
				}
			}
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}
}