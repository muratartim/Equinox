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

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import equinox.Equinox;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;

/**
 * Class for backup workspace task.
 *
 * @author Murat Artim
 * @date Mar 10, 2015
 * @time 5:06:18 PM
 */
public class BackupWorkspace extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** Path to save the workspace. */
	private final Path path_;

	/**
	 * Creates backup workspace task.
	 *
	 * @param path
	 *            Path to save the workspace.
	 */
	public BackupWorkspace(Path path) {
		path_ = path;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		String title = "Backup workspace";
		return title + " to '" + path_.toString() + "'";
	}

	@Override
	protected Void call() throws Exception {

		// create list of files to be zipped
		ArrayList<Path> files = new ArrayList<>();

		// local database
		String freeze = "call syscs_util.syscs_freeze_database()";
		String unfreeze = "call syscs_util.syscs_unfreeze_database()";

		// get connection to workspace
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement s = connection.createStatement()) {

				// freeze workspace statement for local workspace
				s.executeUpdate(freeze);

				// copy files
				try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Equinox.WORKSPACE_PATHS.getCurrentPath())) {

					// get iterator
					Iterator<Path> iterator = dirStream.iterator();

					// loop over files
					while (iterator.hasNext()) {
						files.add(iterator.next());
					}
				}

				// unfreeze workspace statement for local workspace
				s.executeUpdate(unfreeze);
			}
		}

		// zip file
		Utility.zipFiles(files, path_.toFile(), this);

		// return
		return null;
	}
}
