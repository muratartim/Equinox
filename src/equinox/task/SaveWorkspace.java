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

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Iterator;

import equinox.Equinox;
import equinox.task.InternalEquinoxTask.LongRunningTask;

/**
 * Class for save workspace task.
 *
 * @author Murat Artim
 * @date Oct 8, 2014
 * @time 9:23:03 AM
 */
public class SaveWorkspace extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** Path to save the workspace. */
	private final Path path_;

	/**
	 * Creates save workspace task.
	 *
	 * @param path
	 *            Path to save the workspace.
	 */
	public SaveWorkspace(Path path) {
		path_ = path;
	}

	@Override
	public String getTaskTitle() {
		return "Save workspace to '" + path_.toString() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// freeze database
				statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_FREEZE_DATABASE()");

				// copy files
				try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Equinox.WORKSPACE_PATHS.getCurrentPath())) {

					// get iterator
					Iterator<Path> iterator = dirStream.iterator();

					// loop over files
					while (iterator.hasNext()) {

						// get file
						Path file = iterator.next();

						// copy file
						copyFile(file, path_);
					}
				}

				// unfreeze database
				statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_UNFREEZE_DATABASE()");
			}
		}

		// return
		return null;
	}

	/**
	 * Copies given file recursively.
	 *
	 * @param source
	 *            Source file or directory.
	 * @param targetDir
	 *            Target directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void copyFile(Path source, Path targetDir) throws Exception {

		// progress message
		updateMessage("Copying file '" + source.getFileName() + "'");

		// create target directory (if it doesn't exist)
		if (!Files.exists(targetDir)) {
			Files.createDirectory(targetDir);
		}

		// directory
		if (Files.isDirectory(source)) {

			// create parent path
			Path parentPath = targetDir.resolve(source.getFileName());

			// directory exists
			if (Files.exists(parentPath)) {
				deleteDirectory(parentPath);
			}

			// create directory stream
			try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(source)) {

				// get iterator
				Iterator<Path> iterator = dirStream.iterator();

				// loop over files
				while (iterator.hasNext()) {

					// get file
					Path file = iterator.next();

					// copy file
					copyFile(file, parentPath);
				}
			}
		}

		// file
		else {
			Files.copy(source, targetDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * Deletes the given directory. All files inside the given directory will be deleted recursively.
	 *
	 * @param path
	 *            Path to directory to delete.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void deleteDirectory(Path path) throws Exception {

		// progress message
		updateMessage("Deleting file '" + path.getFileName() + "'");

		// directory
		if (Files.isDirectory(path)) {

			// create directory stream
			try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {

				// get iterator
				Iterator<Path> iterator = dirStream.iterator();

				// loop over files
				while (iterator.hasNext()) {
					deleteDirectory(iterator.next());
				}
			}

			// delete directory
			try {
				Files.delete(path);
			}

			// directory not empty
			catch (DirectoryNotEmptyException e) {
				deleteDirectory(path);
			}
		}

		// file
		else {
			Files.delete(path);
		}
	}
}
