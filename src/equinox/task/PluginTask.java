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
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.plugin.PluginProcess;
import equinox.utility.Utility;

/**
 * Class for plugin task.
 *
 * @author Murat Artim
 * @date Mar 27, 2015
 * @time 10:13:27 AM
 */
public class PluginTask extends InternalEquinoxTask<Object> {

	/** Plugin process. */
	private final PluginProcess process_;

	/** Working directory. */
	private Path workingDirectory_;

	/** List containing the permanent files. */
	private ArrayList<Path> permanentFiles_;

	/**
	 * Creates plugin task.
	 *
	 * @param process
	 *            Plugin process.
	 */
	public PluginTask(PluginProcess process) {
		process_ = process;
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

	/**
	 * Returns true if this task is long running.
	 *
	 * @return True if this task is long running.
	 */
	public boolean isLongRunning() {
		return process_.isLongRunning();
	}

	/**
	 * Returns true if this task is directory outputting.
	 *
	 * @return True if this task is directory outputting.
	 */
	public boolean isDirectoryOutputting() {
		return process_.isDirectoryOutputting();
	}

	@Override
	public boolean canBeCancelled() {
		return process_.canBeCancelled();
	}

	@Override
	public String getTaskTitle() {
		return process_.getTitle();
	}

	@Override
	public String getWarnings() {
		return process_.getWarnings();
	}

	/**
	 * Returns output directory of this process.
	 *
	 * @return Output directory of this process.
	 */
	public Path getOutputDirectory() {
		return process_.getOutputDirectory();
	}

	@Override
	protected Object call() throws Exception {

		// create working directory (if required)
		if (process_.needsWorkingDirectory()) {
			workingDirectory_ = Utility.createWorkingDirectory(process_.getClass().getSimpleName());
		}

		// initialize output
		Object output = null;

		// requires database connection
		if (process_.needsDatabaseConnection()) {

			// get connection to database
			try (Connection connection = Equinox.DBC_POOL.getConnection()) {

				// start process
				output = process_.start(this, workingDirectory_, connection);
			}
		}
		else {
			output = process_.start(this, workingDirectory_, null);
		}

		// return output
		return output;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set results to process
		try {
			process_.onSucceeded(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}

		// delete temporary files
		deleteTemporaryFiles();
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// notify process
		process_.onFailed(getException());

		// delete temporary files
		deleteTemporaryFiles();
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// notify process
		process_.onCancelled();

		// delete temporary files
		deleteTemporaryFiles();
	}

	/**
	 * Deletes temporary files.
	 */
	private final void deleteTemporaryFiles() {
		if ((workingDirectory_ != null) && Files.exists(workingDirectory_)) {
			Equinox.CACHED_THREADPOOL.submit(new DeleteTemporaryFiles(workingDirectory_, permanentFiles_));
		}
	}
}
