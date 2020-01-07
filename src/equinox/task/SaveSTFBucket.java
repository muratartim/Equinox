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

import equinox.Equinox;
import equinox.data.fileType.STFFileBucket;
import equinox.process.SaveSTFBucketProcess;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;

/**
 * Class for save STF file bucket task.
 *
 * @author Murat Artim
 * @date 1 Sep 2016
 * @time 16:54:03
 */
public class SaveSTFBucket extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** File item to save. */
	private final STFFileBucket bucket_;

	/** Output directory. */
	private final Path outputDir_;

	/**
	 * Creates save STF file bucket task.
	 *
	 * @param bucket
	 *            File item to save.
	 * @param outputDir
	 *            Output directory.
	 */
	public SaveSTFBucket(STFFileBucket bucket, Path outputDir) {
		bucket_ = bucket;
		outputDir_ = outputDir;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		Path directoryNamePath = outputDir_.getFileName();
		if (directoryNamePath == null)
			return "Save STF files";
		return "Save STF files to '" + directoryNamePath.toString() + "'";
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.SAVE_FILE);

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			new SaveSTFBucketProcess(this, bucket_, outputDir_).start(connection);
		}

		// return
		return null;
	}
}
