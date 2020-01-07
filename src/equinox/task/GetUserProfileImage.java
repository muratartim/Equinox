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

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.serverUtilities.FilerConnection;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get user profile image task.
 *
 * @author Murat Artim
 * @date 5 Apr 2018
 * @time 11:45:55
 */
public class GetUserProfileImage extends TemporaryFileCreatingTask<byte[]> implements ShortRunningTask {

	/** User. */
	private final ExchangeUser user;

	/** Requesting panel. */
	private final UserProfileImageRequestingPanel panel;

	/**
	 * Creates get user profile image task.
	 *
	 * @param user
	 *            User.
	 * @param panel
	 *            Requesting panel.
	 */
	public GetUserProfileImage(ExchangeUser user, UserProfileImageRequestingPanel panel) {
		this.user = user;
		this.panel = panel;
	}

	@Override
	public String getTaskTitle() {
		return "Get user profile image";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected byte[] call() throws Exception {

		// update progress info
		updateTitle("Getting user profile image...");
		updateMessage("Please wait...");

		// create temporary path
		Path tempFile = getWorkingDirectory().resolve(user.getAlias() + ".png");

		// get image
		try (FilerConnection filer = getFilerConnection()) {

			// create image URL
			String imageUrl = filer.getDirectoryPath(FilerConnection.USERS) + "/" + user.getAlias() + ".png";

			// image exists
			if (filer.fileExists(imageUrl)) {
				filer.getSftpChannel().get(imageUrl, tempFile.toString());
			}

			// no image found
			else
				return null;
		}

		// read image bytes
		byte[] imageBytes = new byte[(int) tempFile.toFile().length()];
		try (ImageInputStream imgStream = ImageIO.createImageInputStream(tempFile.toFile())) {
			imgStream.read(imageBytes);
		}

		// return image bytes
		return imageBytes;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {
			panel.setUserProfileImage(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// set file info
		panel.setUserProfileImage(null);
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// set file info
		panel.setUserProfileImage(null);
	}

	/**
	 * Interface for user profile image requesting panels.
	 *
	 * @author Murat Artim
	 * @date 5 Apr 2018
	 * @time 11:49:20
	 */
	public interface UserProfileImageRequestingPanel {

		/**
		 * Sets user profile image.
		 *
		 * @param imageBytes
		 *            Image bytes.
		 */
		void setUserProfileImage(byte[] imageBytes);
	}
}
