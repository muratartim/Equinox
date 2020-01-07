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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.data.fileType.Spectrum;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.DownloadPilotPointsRequest;
import equinox.dataServer.remote.message.DownloadPilotPointsResponse;
import equinox.network.DataServerManager;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for downloading multiple tasks.
 *
 * @author Murat Artim
 * @date 23 Jan 2017
 * @time 10:45:21
 */
public class DownloadPilotPoints extends TemporaryFileCreatingTask<AddSTFFiles> implements LongRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Pilot point info. */
	private final ArrayList<PilotPointInfo> info_;

	/** Spectrum to add the pilot point. */
	private final Spectrum spectrum_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates download pilot points task.
	 *
	 * @param info
	 *            Pilot point info.
	 * @param spectrum
	 *            Spectrum to add the pilot point. Can be null for just downloading.
	 */
	public DownloadPilotPoints(ArrayList<PilotPointInfo> info, Spectrum spectrum) {
		info_ = info;
		spectrum_ = spectrum;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Download pilot points";
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
	protected AddSTFFiles call() throws Exception {

		// check permission
		checkPermission(Permission.DOWNLOAD_PILOT_POINT);

		// update progress info
		updateTitle("Downloading pilot point");

		// create add STF files task
		AddSTFFiles task = new AddSTFFiles(null, spectrum_, info_);
		ArrayList<File> inputFiles = new ArrayList<>();

		// get download URLs
		DownloadPilotPointsResponse response = getDownloadUrls();

		// task cancelled
		if (isCancelled())
			return null;

		// get connection to filer
		try (FilerConnection filer = getFilerConnection()) {

			// loop over pilot points
			for (PilotPointInfo info : info_) {

				// get pilot point ID
				long ppID = (long) info.getInfo(PilotPointInfoType.ID);
				String name = (String) info.getInfo(PilotPointInfoType.NAME);
				Path output = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());

				// download data
				String url = response.getDownloadUrl(ppID);
				if (url != null) {
					if (filer.fileExists(url)) {
						filer.getSftpChannel().get(url, output.toString());
					}
				}

				// add to database
				if (spectrum_ != null) {

					// extract archive
					Path stfFile = Utility.extractFileFromZIP(output, this, FileType.STF, getWorkingDirectory());

					// get STF file name path
					Path stfFileNamePath = stfFile.getFileName();

					// file name exists
					if (stfFileNamePath != null) {

						// copy and set STF file
						Path stfFileCopy = task.getWorkingDirectory().resolve(stfFileNamePath.toString());
						inputFiles.add(Files.copy(stfFile, stfFileCopy, StandardCopyOption.REPLACE_EXISTING).toFile());
					}
				}
			}
		}

		// set input STF files to task
		task.setSTFFiles(inputFiles);

		// return task
		return task;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// add to database
			AddSTFFiles task = get();
			if (task != null) {
				taskPanel_.getOwner().runTaskSequentially(task);
			}
		}

		// exception occurred
		catch (Exception e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Returns download URLs from the central database.
	 *
	 * @return download URLs.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DownloadPilotPointsResponse getDownloadUrls() throws Exception {

		// update progress info
		updateMessage("Downloading pilot point URLs...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			DownloadPilotPointsRequest request = new DownloadPilotPointsRequest();
			request.setListenerHashCode(hashCode());

			// add pilot point ids
			for (PilotPointInfo info : info_) {
				long ppID = (long) info.getInfo(PilotPointInfoType.ID);
				request.addDownloadId(ppID);
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
			else if (message instanceof DownloadPilotPointsResponse)
				return (DownloadPilotPointsResponse) message;

			// no url found
			throw new Exception("No URL found for pilot point data in the central database.");
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}
}
