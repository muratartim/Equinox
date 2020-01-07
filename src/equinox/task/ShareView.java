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
import java.util.ArrayList;

import javax.imageio.ImageIO;

import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.SharedFileInfo;
import equinox.task.InternalEquinoxTask.FileSharingTask;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

/**
 * Class for upload view task.
 *
 * @author Murat Artim
 * @date Sep 22, 20141
 * @time 12:00:15 PM
 */
public class ShareView extends TemporaryFileCreatingTask<Void> implements ShortRunningTask, FileSharingTask {

	/** View image. */
	private final WritableImage image_;

	/** View image name. */
	private final String name_;

	/** Recipients. */
	private final ArrayList<ExchangeUser> recipients_;

	/**
	 * Creates upload view task.
	 *
	 * @param name
	 *            View image name.
	 * @param image
	 *            View image.
	 * @param recipients
	 *            Recipients.
	 */
	public ShareView(String name, WritableImage image, ArrayList<ExchangeUser> recipients) {
		name_ = name;
		image_ = image;
		recipients_ = recipients;
	}

	@Override
	public String getTaskTitle() {
		return "Share view";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.SHARE_FILE);

		// update progress info
		updateTitle("Sharing view...");

		// save image to temporary file
		Path path = saveImage();

		// upload file to filer
		shareFile(path, recipients_, SharedFileInfo.VIEW);
		return null;
	}

	/**
	 * Saves view image to temporary file.
	 *
	 * @return Path to temporary file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path saveImage() throws Exception {
		updateMessage("Saving view to temporary file...");
		Path output = getWorkingDirectory().resolve(FileType.appendExtension(name_, FileType.PNG));
		ImageIO.write(SwingFXUtils.fromFXImage(image_, null), "png", output.toFile());
		return output;
	}
}
