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
import java.sql.Connection;
import java.util.ArrayList;

import equinox.Equinox;
import equinox.data.fileType.STFFileBucket;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.process.SaveSTFBucketProcess;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.SharedFileInfo;
import equinox.task.InternalEquinoxTask.FileSharingTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;

/**
 * Class for share STF file bucket task.
 *
 * @author Murat Artim
 * @date 6 Sep 2016
 * @time 09:57:23
 */
public class ShareSTFBucket extends TemporaryFileCreatingTask<Void> implements LongRunningTask, FileSharingTask {

	/** STF file bucket to share. */
	private final STFFileBucket bucket_;

	/** Recipients. */
	private final ArrayList<ExchangeUser> recipients_;

	/**
	 * Creates share STF file bucket task.
	 *
	 * @param bucket
	 *            STF file bucket to share.
	 * @param recipients
	 *            Recipients.
	 */
	public ShareSTFBucket(STFFileBucket bucket, ArrayList<ExchangeUser> recipients) {
		bucket_ = bucket;
		recipients_ = recipients;
	}

	@Override
	public String getTaskTitle() {
		return "Share STF files";
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
		updateTitle("Sharing STF files...");

		// save STF files to temporary archive
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

			// save STF file bucket
			Path tempDir = Files.createDirectory(getWorkingDirectory().resolve(Utility.correctFileName(bucket_.getParentItem().getName())));
			new SaveSTFBucketProcess(this, bucket_, tempDir).start(connection);

			// zip output directory
			zipFile = getWorkingDirectory().resolve(Utility.correctFileName(bucket_.getParentItem().getName()) + ".zip");
			Utility.zipDirectory(tempDir, zipFile.toFile(), this);
		}

		// return output file
		return zipFile;
	}
}
