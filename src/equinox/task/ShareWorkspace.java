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
import java.util.Arrays;

import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.SharedFileInfo;
import equinox.task.InternalEquinoxTask.FileSharingTask;
import equinox.utility.Utility;

/**
 * Class for share workspace task.
 *
 * @author Murat Artim
 * @date Dec 11, 2014
 * @time 1:41:17 PM
 */
public class ShareWorkspace extends InternalEquinoxTask<Void> implements FileSharingTask {

	/** Recipient. */
	private final ExchangeUser recipient_;

	/** Path to workspace to be shared. */
	private final Path path_;

	/**
	 * Creates share workspace task.
	 *
	 * @param recipient
	 *            Recipient name.
	 * @param path
	 *            Path to workspace to be shared.
	 */
	public ShareWorkspace(ExchangeUser recipient, Path path) {
		recipient_ = recipient;
		path_ = path;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Share workspace";
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.SHARE_FILE);

		// save item to temporary file
		Path path = zipFile();

		// upload file to filer
		shareFile(path, Arrays.asList(recipient_), SharedFileInfo.WORKSPACE);
		return null;
	}

	/**
	 * Zips item to temporary file.
	 *
	 * @return Path to temporary file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path zipFile() throws Exception {

		// update info
		updateMessage("Saving item to temporary file...");

		// get file name
		Path fileName = path_.getFileName();
		if (fileName == null)
			throw new Exception("Cannot get file name.");

		// get parent directory
		Path parentDir = path_.getParent();
		if (parentDir == null)
			throw new Exception("Cannot get workspace parent directory.");

		// create output path
		Path output = parentDir.resolve(FileType.appendExtension(fileName.toString(), FileType.ZIP));

		// zip
		Utility.zipFile(path_, output.toFile(), this);

		// return output file
		return output;
	}
}
