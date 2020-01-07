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

import equinox.controller.ViewPanel;
import equinox.controller.WebViewPanel;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.DownloadHelpVideoRequest;
import equinox.dataServer.remote.message.DownloadHelpVideoResponse;
import equinox.network.DataServerManager;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for download help video task.
 *
 * @author Murat Artim
 * @date Sep 29, 2014
 * @time 6:10:05 PM
 */
public class DownloadHelpVideo extends TemporaryFileCreatingTask<Path> implements LongRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Video name. */
	private final String name_;

	/** Video id. */
	private final long id_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates download help video task.
	 *
	 * @param id
	 *            Video file id.
	 * @param name
	 *            Video name.
	 */
	public DownloadHelpVideo(long id, String name) {
		id_ = id;
		name_ = name;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	/**
	 * Creates download help video task.
	 *
	 * @param name
	 *            Video name.
	 */
	public DownloadHelpVideo(String name) {
		id_ = -1L;
		name_ = name;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Download help video";
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
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.DOWNLOAD_HELP_VIDEO);

		// update progress info
		updateTitle("Downloading help video");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create path to output file
			Path output = getWorkingDirectory().resolve(FileType.appendExtension(name_, FileType.MOV));
			setFileAsPermanent(output);

			// create request message
			DownloadHelpVideoRequest request = new DownloadHelpVideoRequest();
			request.setListenerHashCode(hashCode());
			request.setVideoId(id_);
			request.setVideoName(name_);

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
			else if (message instanceof DownloadHelpVideoResponse) {

				// get download URL
				String downloadUrl = ((DownloadHelpVideoResponse) message).getDownloadUrl();

				// download file
				if (downloadUrl != null) {
					try (FilerConnection filer = getFilerConnection()) {
						if (filer.fileExists(downloadUrl)) {
							filer.getSftpChannel().get(downloadUrl, output.toString());
						}
					}
				}
			}

			// return output path
			return output;
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

			// notify web view panel
			WebViewPanel panel = (WebViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.WEB_VIEW);
			panel.videoDownloaded(get());
		}

		// exception occurred
		catch (Exception e) {
			handleResultRetrievalException(e);
		}
	}
}
