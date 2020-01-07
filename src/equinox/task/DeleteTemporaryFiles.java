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

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;

import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for delete temporary files task.
 *
 * @author Murat Artim
 * @date 10 Aug 2016
 * @time 12:22:20
 */
public class DeleteTemporaryFiles extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** Directory containing the temporary files to be deleted. */
	private final Path directory_;

	/** List containing the files that should not be deleted. */
	private final ArrayList<Path> excludedFiles_;

	/**
	 * Creates delete temporary files task.
	 *
	 * @param directory
	 *            Directory containing the temporary files to be deleted.
	 * @param excludedFiles
	 *            List containing the files that should not be deleted. Can be null.
	 */
	public DeleteTemporaryFiles(Path directory, ArrayList<Path> excludedFiles) {
		directory_ = directory;
		excludedFiles_ = excludedFiles;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Delete temporary files";
	}

	@Override
	protected Void call() throws Exception {
		deleteFiles(directory_);
		return null;
	}

	/**
	 * Deletes files recursively.
	 *
	 * @param file
	 *            Path to directory containing the files to delete.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	private void deleteFiles(Path file) throws IOException {

		// directory
		if (Files.isDirectory(file)) {

			// create directory stream
			try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(file)) {

				// get iterator
				Iterator<Path> iterator = dirStream.iterator();

				// loop over files
				while (iterator.hasNext()) {
					deleteFiles(iterator.next());
				}
			}

			// delete directory (if not excluded)
			try {
				if (excludedFiles_ == null || excludedFiles_ != null && !excludedFiles_.contains(file)) {
					Files.delete(file);
				}
			}

			// directory not empty
			catch (DirectoryNotEmptyException e) {
				// ignore
			}
		}

		// delete file (if not excluded)
		else {
			if (excludedFiles_ == null || excludedFiles_ != null && !excludedFiles_.contains(file)) {
				Files.delete(file);
			}
		}
	}
}
