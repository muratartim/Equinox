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
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.SavePilotPointImageRequest;
import equinox.dataServer.remote.message.SavePilotPointImageResponse;
import equinox.network.DataServerManager;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for save pilot point image to global database task.
 *
 * @author Murat Artim
 * @date Jun 29, 2016
 * @time 2:40:26 PM
 */
public class SavePilotPointImageToGlobalDB extends InternalEquinoxTask<byte[]> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Requesting panel. */
	private final DownloadPilotPointImagePage panel_;

	/** Pilot point info. */
	private final PilotPointInfo info_;

	/** Image file. */
	private final Path imageFile_;

	/** Pilot point image type. */
	private final PilotPointImageType imageType_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates save pilot point image to global database task.
	 *
	 * @param info
	 *            Pilot point info.
	 * @param imageType
	 *            Pilot point image type.
	 * @param imageFile
	 *            Image file. Can be null for resetting the image.
	 * @param panel
	 *            Calling panel.
	 */
	public SavePilotPointImageToGlobalDB(PilotPointInfo info, PilotPointImageType imageType, Path imageFile, DownloadPilotPointImagePage panel) {
		info_ = info;
		imageType_ = imageType;
		imageFile_ = imageFile;
		panel_ = panel;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save pilot point image";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected byte[] call() throws Exception {

		// check permission
		checkPermission(Permission.SAVE_PILOT_POINT_IMAGE);

		// update progress info
		updateTitle("Saving pilot point image to database");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			SavePilotPointImageRequest request = new SavePilotPointImageRequest();
			request.setListenerHashCode(hashCode());
			request.setImageType(imageType_);
			request.setPilotPointInfo(info_);
			request.setDelete(imageFile_ == null);

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
			else if (message instanceof SavePilotPointImageResponse)
				return postProcessServerResponse((SavePilotPointImageResponse) message);

			// return result
			throw new Exception("Invalid server response received.");
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

	/**
	 * Post processes server response.
	 *
	 * @param response
	 *            Server response message.
	 * @return Image bytes.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private byte[] postProcessServerResponse(SavePilotPointImageResponse response) throws Exception {

		// get image URL
		String imageUrl = response.getImageUrl();

		// image URL exists
		if (imageUrl != null) {

			// get filer connection
			try (FilerConnection filer = getFilerConnection()) {

				// remove image data from filer
				if (response.isRemoveFromFiler()) {
					filer.getSftpChannel().rm(imageUrl);
				}

				// upload image data to filer
				if (response.isUploadToFiler() && imageFile_ != null) {
					filer.getSftpChannel().put(imageFile_.toString(), imageUrl);
				}
			}
		}

		// no image file
		if (imageFile_ == null)
			return null;

		// get image bytes
		byte[] imageBytes = new byte[(int) imageFile_.toFile().length()];
		try (ImageInputStream imgStream = ImageIO.createImageInputStream(imageFile_.toFile())) {
			imgStream.read(imageBytes);
		}

		// return image bytes
		return imageBytes;
	}
}
