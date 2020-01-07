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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import equinox.Equinox;
import equinox.utility.Utility;

/**
 * Abstract class for temporary file creating tasks.
 *
 * @author Murat Artim
 * @param <V>
 *            Task output class.
 * @date May 13, 2014
 * @time 11:22:38 AM
 */
public abstract class TemporaryFileCreatingTask<V> extends InternalEquinoxTask<V> {

	/** Working directory. */
	private Path workingDirectory_;

	/** List containing the permanent files. */
	private ArrayList<Path> permanentFiles_;

	/**
	 * Returns the working directory of this task. If the directory doesn't exist, it will be created and returned.
	 *
	 * @return The working directory of this task.
	 * @throws IOException
	 *             If exception occurs during creating the directory.
	 */
	public Path getWorkingDirectory() throws IOException {
		return workingDirectory_ == null ? createWorkingDirectory() : workingDirectory_;
	}

	/**
	 * Sets given file as permanent. Permanent files are not deleted after task is completed.
	 *
	 * @param file
	 *            Temporary file to set as permanent.
	 */
	public void setFileAsPermanent(Path file) {
		if (permanentFiles_ == null) {
			permanentFiles_ = new ArrayList<>();
		}
		permanentFiles_.add(file);
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// delete temporary files
		deleteTemporaryFiles();
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// delete temporary files
		deleteTemporaryFiles();
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// delete temporary files
		deleteTemporaryFiles();
	}

	/**
	 * Creates and returns a working directory.
	 *
	 * @return Path to newly created working directory.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	private final Path createWorkingDirectory() throws IOException {
		workingDirectory_ = Utility.createWorkingDirectory(getClass().getSimpleName());
		return workingDirectory_;
	}

	/**
	 * Deletes temporary files.
	 */
	private final void deleteTemporaryFiles() {
		// ON delete temporary files
		if ((workingDirectory_ != null) && Files.exists(workingDirectory_)) {
			Equinox.CACHED_THREADPOOL.submit(new DeleteTemporaryFiles(workingDirectory_, permanentFiles_));
		}
	}
}
