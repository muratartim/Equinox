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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import equinox.controller.DownloadPilotPointImagePage;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.GetPilotPointImageRequest;
import equinox.dataServer.remote.message.GetPilotPointImageResponse;
import equinox.network.DataServerManager;
import equinox.serverUtilities.FilerConnection;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for get pilot point image task.
 *
 * @author Murat Artim
 * @date Feb 16, 2016
 * @time 9:39:13 AM
 */
public class GetPilotPointImage extends TemporaryFileCreatingTask<byte[]> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Pilot point ID. */
	private final long pilotPointID_;

	/** Requesting panel. */
	private final DownloadPilotPointImagePage panel_;

	/** Pilot point image type. */
	private final PilotPointImageType imageType_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates get pilot point image task.
	 *
	 * @param pilotPointID
	 *            Pilot point ID.
	 * @param imageType
	 *            Pilot point image type.
	 * @param panel
	 *            Requesting panel.
	 */
	public GetPilotPointImage(long pilotPointID, PilotPointImageType imageType, DownloadPilotPointImagePage panel) {
		pilotPointID_ = pilotPointID;
		imageType_ = imageType;
		panel_ = panel;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Get " + imageType_.getPageName();
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
	protected byte[] call() throws Exception {

		// update progress info
		updateTitle("Getting image from database...");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			GetPilotPointImageRequest request = new GetPilotPointImageRequest();
			request.setListenerHashCode(hashCode());
			request.setImageType(imageType_);
			request.setPilotPointId(pilotPointID_);

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
			else if (message instanceof GetPilotPointImageResponse) {

				// get image URL
				String imageUrl = ((GetPilotPointImageResponse) message).getImageUrl();

				// no image
				if (imageUrl == null)
					return null;

				// download image to temporary file
				Path tempFile = getWorkingDirectory().resolve(pilotPointID_ + "_" + imageType_.getFileName());
				try (FilerConnection filer = getFilerConnection()) {
					if (filer.fileExists(imageUrl)) {
						filer.getSftpChannel().get(imageUrl, tempFile.toString());
					}
				}

				// read image bytes
				byte[] imageBytes = new byte[(int) tempFile.toFile().length()];
				try (ImageInputStream imgStream = ImageIO.createImageInputStream(tempFile.toFile())) {
					imgStream.read(imageBytes);
				}

				// return image bytes
				return imageBytes;
			}

			// no aircraft program found
			throw new Exception("No bug report found.");
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

		// set file info
		try {
			panel_.setPilotPointImage(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
