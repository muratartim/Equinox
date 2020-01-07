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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.dataServer.remote.data.SampleInputInfo;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.UploadSampleInputsRequest;
import equinox.dataServer.remote.message.UploadSampleInputsResponse;
import equinox.network.DataServerManager;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for upload sample inputs task.
 *
 * @author Murat Artim
 * @date Sep 7, 2015
 * @time 2:39:24 PM
 */
public class UploadSampleInputs extends InternalEquinoxTask<Boolean> implements LongRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** List of video files to upload. */
	private final List<File> files_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates upload sample inputs task.
	 *
	 * @param files
	 *            List of sample input files to upload.
	 */
	public UploadSampleInputs(List<File> files) {
		files_ = files;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Upload sample input files";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.UPLOAD_SAMPLE_INPUTS);

		// update progress info
		updateTitle("Uploading sample inputs");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		boolean isUploaded = false;

		try {

			// create request message
			UploadSampleInputsRequest request = new UploadSampleInputsRequest();
			request.setListenerHashCode(hashCode());

			// get connection to filer
			try (FilerConnection filer = getFilerConnection()) {

				// loop over files
				int count = 0;
				for (File file : files_) {

					// task cancelled
					if (isCancelled())
						return null;

					// create info
					SampleInputInfo info = new SampleInputInfo();

					// get file name
					String name = Utility.correctFileName(FileType.getNameWithoutExtension(file.getName()));
					info.setName(name);

					// update info
					updateMessage("Uploading file '" + name + "'...");
					updateProgress(count, files_.size());
					count++;

					// upload plugin jar file to filer
					String url = filer.getDirectoryPath(FilerConnection.INPUT_SAMPLES) + "/" + name + ".zip";
					info.setDataUrl(url);
					info.setDataSize(file.length());

					// delete data from filer (if exists)
					if (filer.fileExists(url)) {
						filer.getSftpChannel().rm(url);
					}

					// upload to filer
					filer.getSftpChannel().put(file.toPath().toString(), url);

					// add to request
					request.addInfo(info);
				}
			}

			// task cancelled
			if (isCancelled())
				return null;

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
			else if (message instanceof UploadSampleInputsResponse) {
				isUploaded = ((UploadSampleInputsResponse) message).isUploaded();
			}

			// return result
			return isUploaded;
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}
}
