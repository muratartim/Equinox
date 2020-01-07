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
package equinox.plugin;

import java.nio.file.Path;
import java.sql.Connection;

/**
 * Interface for plugin processes.
 *
 * @author Murat Artim
 * @date Mar 25, 2015
 * @time 6:21:47 PM
 */
public interface PluginProcess {

	/**
	 * Returns true if this process requires working directory.
	 *
	 * @return True if this process requires working directory.
	 */
	boolean needsWorkingDirectory();

	/**
	 * Returns true if this process requires database connection.
	 *
	 * @return True if this process requires database connection.
	 */
	boolean needsDatabaseConnection();

	/**
	 * Returns true if this process is long running.
	 *
	 * @return True if this process is long running.
	 */
	boolean isLongRunning();

	/**
	 * Returns true if this process is directory outputting.
	 *
	 * @return True if this process is directory outputting.
	 */
	boolean isDirectoryOutputting();

	/**
	 * Returns true if this process can be cancelled.
	 *
	 * @return True if this process can be cancelled.
	 */
	boolean canBeCancelled();

	/**
	 * Returns the process title.
	 *
	 * @return The process title.
	 */
	String getTitle();

	/**
	 * Returns warning messages of the process, or empty string if there is no warning.
	 *
	 * @return Warning messages of the process, or empty string if there is no warning.
	 */
	String getWarnings();

	/**
	 * Returns output directory of this process. Note that, this is only needed if <code>isDirectoryOutputting()</code> returns true. Otherwise return <code>null</code>.
	 *
	 * @return Output directory of this process.
	 */
	Path getOutputDirectory();

	/**
	 * Called when the process is successfully completed.
	 *
	 * @param result
	 *            Result of this process.
	 */
	void onSucceeded(Object result);

	/**
	 * Called when the process failed.
	 *
	 * @param e
	 *            Exception.
	 */
	void onFailed(Throwable e);

	/**
	 * Called when the process cancelled.
	 */
	void onCancelled();

	/**
	 * Starts this process.
	 *
	 * @param ownerTask
	 *            The owner task.
	 * @param workingDirectory
	 *            Working directory of this process. Note that, this will be <code>null</code> if <code>needsWorkingDirectory()</code> returns false.
	 * @param connection
	 *            Database connection. Note that, this will be <code>null</code> if <code>needsDatabaseConnection()</code> returns false.
	 * @return The result of this process.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	Object start(EquinoxTask<Object> ownerTask, Path workingDirectory, Connection connection) throws Exception;
}
