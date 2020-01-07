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
import equinox.process.SaveBucketOutputFilesProcess;
import equinox.task.InternalEquinoxTask.LongRunningTask;

/**
 * Class for save bucket analysis output files task.
 *
 * @author Murat Artim
 * @date 27 Apr 2017
 * @time 11:31:37
 *
 */
public class SaveBucketOutputFiles extends TemporaryFileCreatingTask<Path> implements LongRunningTask {

	/** File item to save. */
	private final STFFileBucket[] buckets_;

	/** Output directory. */
	private final Path outputDir_;

	/** Equivalent stress type. */
	private final int stressType_;

	/**
	 * Creates save STF file bucket task.
	 *
	 * @param buckets
	 *            File item to save.
	 * @param outputDir
	 *            Output directory.
	 * @param stressType
	 *            Equivalent stress type.
	 */
	public SaveBucketOutputFiles(STFFileBucket[] buckets, Path outputDir, int stressType) {
		buckets_ = buckets;
		outputDir_ = outputDir;
		stressType_ = stressType;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		Path directoryNamePath = outputDir_.getFileName();
		if (directoryNamePath == null)
			return "Save analysis output files";
		return "Save analysis output files to '" + directoryNamePath.toString() + "'";
	}

	@Override
	protected Path call() throws Exception {

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			new SaveBucketOutputFilesProcess(this, outputDir_, stressType_, buckets_).start(connection);
		}

		// return
		return null;
	}
}
