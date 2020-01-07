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

import equinox.Equinox;
import equinox.data.fileType.STFFileBucket;
import equinox.process.SaveBucketOutputFilesProcess;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;

/**
 * Class for save and zip bucket analysis output files task.
 *
 * @author Murat Artim
 * @date 27 Apr 2017
 * @time 11:31:37
 *
 */
public class SaveAndZipBucketOutputFiles extends TemporaryFileCreatingTask<Void> implements LongRunningTask {

	/** File item to save. */
	private final STFFileBucket bucket_;

	/** Output file. */
	private final Path output_;

	/** Equivalent stress type. */
	private final int stressType_;

	/**
	 * Creates save and zip STF file bucket output files task.
	 *
	 * @param bucket
	 *            File item to save.
	 * @param output
	 *            Output file.
	 * @param stressType
	 *            Equivalent stress type.
	 */
	public SaveAndZipBucketOutputFiles(STFFileBucket bucket, Path output, int stressType) {
		bucket_ = bucket;
		output_ = output;
		stressType_ = stressType;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		Path fileNamePath = output_.getFileName();
		if (fileNamePath == null)
			return "Save and zip analysis output files";
		return "Save and zip analysis output files to '" + fileNamePath.toString() + "'";
	}

	@Override
	protected Void call() throws Exception {

		// create directory to store downloaded output files
		Path outputDir = getWorkingDirectory().resolve("OutputFiles");
		Files.createDirectory(outputDir);

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			new SaveBucketOutputFilesProcess(this, outputDir, stressType_, bucket_).start(connection);
		}

		// zip all files
		Utility.zipDirectory(outputDir, output_.toFile(), this);

		// return
		return null;
	}
}
