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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import equinox.controller.ImageViewPanel;
import equinox.controller.ViewPanel;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.SharedFileInfo;
import equinox.serverUtilities.SharedFileInfo.SharedFileInfoType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.image.Image;

/**
 * Class for download shared view task.
 *
 * @author Murat Artim
 * @date Sep 22, 2014
 * @time 3:12:25 PM
 */
public class DownloadSharedView extends TemporaryFileCreatingTask<Image> implements ShortRunningTask {

	/** File exchange ID. */
	private final SharedFileInfo info;

	/**
	 * Creates download shared view task.
	 *
	 * @param info
	 *            Shared file info.
	 */
	public DownloadSharedView(SharedFileInfo info) {
		this.info = info;
	}

	@Override
	public String getTaskTitle() {
		return "Download shared view";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Image call() throws Exception {

		// update progress info
		updateTitle("Downloading shared view from server...");

		// initialize image
		Image image = null;

		// get shared file URL
		String url = (String) info.getInfo(SharedFileInfoType.DATA_URL);

		// download from filer
		if (url != null) {
			try (FilerConnection filer = getFilerConnection()) {
				if (filer.fileExists(url)) {
					Path tempFile = getWorkingDirectory().resolve((String) info.getInfo(SharedFileInfoType.FILE_NAME));
					filer.getSftpChannel().get(url, tempFile.toString());
					image = new Image(Files.newInputStream(tempFile));
				}
			}
		}

		// return image
		return image;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set results to view panel
		try {
			ImageViewPanel panel = (ImageViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.IMAGE_VIEW);
			panel.setView(get());
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.IMAGE_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
