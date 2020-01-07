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

import equinox.Equinox;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.UploadMaterialsRequest;
import equinox.dataServer.remote.message.UploadMaterialsResponse;
import equinox.network.DataServerManager;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.SharedFileInfo;
import equinox.serverUtilities.SharedFileInfo.SharedFileInfoType;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for upload materials task.1
 *
 * @author Murat Artim
 * @date Nov 27, 2015
 * @time 2:23:42 PM
 */
public class UploadMaterials extends TemporaryFileCreatingTask<Boolean> implements LongRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Path to MS Excel file. */
	private final Path xlsFile_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates upload materials task.
	 *
	 * @param xlsFile
	 *            Path to MS Excel file.
	 */
	public UploadMaterials(Path xlsFile) {
		xlsFile_ = xlsFile;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Upload materials";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.UPLOAD_MATERIALS);

		// update progress info
		updateTitle("Uploading materials...");
		updateMessage("Please wait...");

		// upload file
		String url = uploadInputFile();

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		boolean isUploaded = false;

		try {

			// create request message
			UploadMaterialsRequest request = new UploadMaterialsRequest();
			request.setListenerHashCode(hashCode());

			// create and set share file info
			SharedFileInfo info = new SharedFileInfo();
			info.setInfo(SharedFileInfoType.OWNER, Equinox.USER.getUsername());
			info.setInfo(SharedFileInfoType.FILE_TYPE, SharedFileInfo.FILE);
			info.setInfo(SharedFileInfoType.FILE_NAME, xlsFile_.getFileName().toString());
			info.setInfo(SharedFileInfoType.DATA_SIZE, xlsFile_.toFile().length());
			info.setInfo(SharedFileInfoType.DATA_URL, url);
			request.setSharedFileInfo(info);

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
			else if (message instanceof UploadMaterialsResponse) {
				isUploaded = ((UploadMaterialsResponse) message).isUploaded();
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

	/**
	 * Uploads input Excel file and returns URL to the uploaded file.
	 *
	 * @return URL to the uploaded file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String uploadInputFile() throws Exception {

		// zip input file
		Path zipFile = getWorkingDirectory().resolve(FileType.getNameWithoutExtension(xlsFile_));
		Utility.zipFile(xlsFile_, zipFile.toFile(), this);

		// update info
		updateMessage("Uploading input file to filer...");
		String url = null;

		// get filer connection
		try (FilerConnection filer = getFilerConnection()) {

			// set path to destination file
			url = filer.getDirectoryPath(FilerConnection.EXCHANGE) + "/" + Equinox.USER.getAlias() + "_" + getClass().getSimpleName() + "_" + System.currentTimeMillis() + ".zip";

			// upload file to filer
			filer.getSftpChannel().put(zipFile.toString(), url);
		}

		// return url
		return url;
	}
}
