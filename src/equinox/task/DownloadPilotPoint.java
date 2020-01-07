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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.data.EmbeddedTask;
import equinox.data.Pair;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.DownloadPilotPointRequest;
import equinox.dataServer.remote.message.DownloadPilotPointResponse;
import equinox.network.DataServerManager;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.utility.Utility;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for download pilot point task.
 *
 * @author Murat Artim
 * @date Feb 16, 2016
 * @time 11:04:03 AM
 */
public class DownloadPilotPoint extends TemporaryFileCreatingTask<AddSTFFiles> implements LongRunningTask, DatabaseQueryListenerTask, SingleInputTask<Pair<PilotPointInfo, Spectrum>>, AutomaticTaskOwner<STFFile> {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Pilot point info. */
	private PilotPointInfo info_;

	/** Output file. */
	private Path output_;

	/** Spectrum to add the pilot point. */
	private Spectrum spectrum_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/** Automatic tasks. The key is the STF file name and the value is the task. */
	private HashMap<String, EmbeddedTask<STFFile>> automaticTasks_ = null;

	/**
	 * Creates download pilot point task.
	 *
	 * @param info
	 *            Pilot point info. Can be null for automatic execution.
	 * @param output
	 *            Output file. Null should be given if the pilot point should be added to spectrum.
	 * @param spectrum
	 *            Spectrum to add the pilot point. Can be null for just downloading.
	 */
	public DownloadPilotPoint(PilotPointInfo info, Path output, Spectrum spectrum) {
		info_ = info;
		output_ = output;
		spectrum_ = spectrum;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Download pilot point";
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
	public void setAutomaticInput(Pair<PilotPointInfo, Spectrum> input) {
		info_ = input.getElement1();
		spectrum_ = input.getElement2();
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<STFFile> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<STFFile>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	protected AddSTFFiles call() throws Exception {

		// check permission
		checkPermission(Permission.DOWNLOAD_PILOT_POINT);

		// update progress info
		updateTitle("Downloading pilot point");

		// check output file
		if (output_ == null) {
			String name = (String) info_.getInfo(PilotPointInfoType.NAME);
			output_ = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
		}

		// download pilot point data
		downloadData();

		// add to database
		if (spectrum_ != null) {

			// extract archive
			Path stfFile = Utility.extractFileFromZIP(output_, this, FileType.STF, getWorkingDirectory());

			// get STF file name path
			Path stfFileNamePath = stfFile.getFileName();

			// file name exists
			if (stfFileNamePath != null) {

				// create add STF files task
				ArrayList<PilotPointInfo> info = new ArrayList<>();
				info.add(info_);
				AddSTFFiles task = new AddSTFFiles(null, spectrum_, info);

				// copy and set STF file
				ArrayList<File> inputFiles = new ArrayList<>();
				Path stfFileCopy = task.getWorkingDirectory().resolve(stfFileNamePath.toString());
				inputFiles.add(Files.copy(stfFile, stfFileCopy, StandardCopyOption.REPLACE_EXISTING).toFile());
				task.setSTFFiles(inputFiles);

				// return task
				return task;
			}
		}

		// return
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// add to database
			AddSTFFiles task = get();
			if (task != null) {

				// pass automatic tasks to resulting task
				if (automaticTasks_ != null) {
					Iterator<Entry<String, EmbeddedTask<STFFile>>> iterator = automaticTasks_.entrySet().iterator();
					while (iterator.hasNext()) {
						Entry<String, EmbeddedTask<STFFile>> entry = iterator.next();
						task.addAutomaticTask(entry.getKey(), entry.getValue());
					}
				}

				// execute resulting task
				taskPanel_.getOwner().runTaskSilently(task, true);
			}
		}

		// exception occurred
		catch (Exception e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	/**
	 * Downloads pilot point data from filer.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void downloadData() throws Exception {

		// update progress info
		updateTitle("Downloading pilot point");
		updateMessage("Downloading pilot point '" + (String) info_.getInfo(PilotPointInfoType.NAME) + "'...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// get pilot point ID
			long ppID = (long) info_.getInfo(PilotPointInfoType.ID);

			// create request message
			DownloadPilotPointRequest request = new DownloadPilotPointRequest();
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
			else if (message instanceof DownloadPilotPointResponse) {

				// get download URL
				String downloadUrl = ((DownloadPilotPointResponse) message).getDownloadUrl();

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
