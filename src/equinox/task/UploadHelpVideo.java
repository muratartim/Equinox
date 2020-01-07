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

import equinox.dataServer.remote.data.HelpVideoInfo;
import equinox.dataServer.remote.data.HelpVideoInfo.HelpVideoInfoType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.UploadHelpVideoRequest;
import equinox.dataServer.remote.message.UploadHelpVideoResponse;
import equinox.network.DataServerManager;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableUploadHelpVideo;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for upload help video task.
 *
 * @author Murat Artim
 * @date Sep 30, 2014
 * @time 10:07:00 AM
 */
public class UploadHelpVideo extends InternalEquinoxTask<Boolean> implements LongRunningTask, SavableTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Help video movie file. */
	private final Path movFile_;

	/** Help video name, duration and description. */
	private final String name_, duration_, description_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates upload help video task.
	 *
	 * @param name
	 *            Help video name.
	 * @param duration
	 *            Help video duration.
	 * @param description
	 *            Help video description.
	 * @param movFile
	 *            Help video movie file.
	 */
	public UploadHelpVideo(String name, String duration, String description, Path movFile) {
		name_ = name;
		duration_ = duration;
		description_ = description;
		movFile_ = movFile;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableUploadHelpVideo(name_, duration_, description_, movFile_.toFile());
	}

	@Override
	public String getTaskTitle() {
		return "Upload help video '" + name_ + "'";
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
		checkPermission(Permission.UPLOAD_HELP_VIDEO);

		// update progress info
		updateTitle("Uploading help video '" + name_ + "'");
		updateMessage("Please wait...");

		// upload file
		String url = uploadInputFile();

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		boolean isUploaded = false;

		try {

			// create request message
			UploadHelpVideoRequest request = new UploadHelpVideoRequest();
			request.setListenerHashCode(hashCode());

			// create and set help video info
			HelpVideoInfo info = new HelpVideoInfo();
			info.setInfo(HelpVideoInfoType.NAME, name_);
			info.setInfo(HelpVideoInfoType.DESCRIPTION, description_);
			info.setInfo(HelpVideoInfoType.DURATION, duration_);
			info.setInfo(HelpVideoInfoType.DATA_SIZE, movFile_.toFile().length());
			info.setInfo(HelpVideoInfoType.DATA_URL, url);
			request.setInfo(info);

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
			else if (message instanceof UploadHelpVideoResponse) {
				isUploaded = ((UploadHelpVideoResponse) message).isUploaded();
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
	 * Uploads video info to global database.
	 *
	 * @return Returns URL to uploaded file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String uploadInputFile() throws Exception {

		// initialize URL
		String url = null;

		// get connection to filer
		try (FilerConnection filer = getFilerConnection()) {

			// set path to destination file
			url = filer.getDirectoryPath(FilerConnection.VIDEOS) + "/" + name_ + ".mov";

			// delete video file from filer (if exists)
			if (filer.fileExists(url)) {
				filer.getSftpChannel().rm(url);
			}

			// upload file to filer
			filer.getSftpChannel().put(movFile_.toString(), url);
		}

		// return URL
		return url;
	}
}
