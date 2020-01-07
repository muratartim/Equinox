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
import equinox.data.fileType.SpectrumItem;
import equinox.process.SaveOutputFilesProcess;
import equinox.task.InternalEquinoxTask.LongRunningTask;

/**
 * Class for save output files task.
 *
 * @author Murat Artim
 * @date 26 Apr 2017
 * @time 17:47:13
 *
 */
public class SaveOutputFiles extends TemporaryFileCreatingTask<Void> implements LongRunningTask {

	/** Spectrum item to save the output file for. */
	private final SpectrumItem[] items_;

	/** Output file. */
	private final Path outputDirectory_;

	/**
	 * Creates save output file task.
	 *
	 * @param items
	 *            Spectrum items to save the output file for.
	 * @param outputDirectory
	 *            Output directory.
	 */
	public SaveOutputFiles(SpectrumItem[] items, Path outputDirectory) {
		items_ = items;
		outputDirectory_ = outputDirectory;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Save output files to '" + outputDirectory_.getFileName().toString() + "'";
	}

	@Override
	protected Void call() throws Exception {

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			new SaveOutputFilesProcess(this, items_, outputDirectory_).start(connection);
		}

		// return
		return null;
	}
}
