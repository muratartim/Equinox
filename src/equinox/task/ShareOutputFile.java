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
import java.sql.Connection;
import java.util.ArrayList;

import equinox.Equinox;
import equinox.data.fileType.SpectrumItem;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.plugin.FileType;
import equinox.process.SaveOutputFileProcess;
import equinox.serverUtilities.SharedFileInfo;
import equinox.task.InternalEquinoxTask.FileSharingTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;

/**
 * Class for share analysis output file task.
 *
 * @author Murat Artim
 * @date 27 Apr 2017
 * @time 16:18:35
 *
 */
public class ShareOutputFile extends TemporaryFileCreatingTask<Void> implements LongRunningTask, FileSharingTask {

	/** Item to share. */
	private final SpectrumItem item_;

	/** Recipients. */
	private final ArrayList<ExchangeUser> recipients_;

	/**
	 * Creates share spectrum item task.
	 *
	 * @param item
	 *            File to share.
	 * @param recipients
	 *            Recipients.
	 */
	public ShareOutputFile(SpectrumItem item, ArrayList<ExchangeUser> recipients) {
		item_ = item;
		recipients_ = recipients;
	}

	@Override
	public String getTaskTitle() {
		return "Share file";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Sharing file...");

		// save item to temporary file
		Path path = saveFile();

		// upload file to filer
		shareFile(path, recipients_, SharedFileInfo.FILE);
		return null;
	}

	/**
	 * Saves item to temporary file.
	 *
	 * @return Path to temporary file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path saveFile() throws Exception {

		// update info
		updateMessage("Saving item to temporary file...");

		// initialize output file
		Path zipFile = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// save output file
			Path outputFile = new SaveOutputFileProcess(this, item_, getWorkingDirectory()).start(connection);

			// zip STF file
			zipFile = getWorkingDirectory().resolve(FileType.getNameWithoutExtension(outputFile));
			Utility.zipFile(outputFile, zipFile.toFile(), this);
		}

		// return output file
		return zipFile;
	}
}
