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
import java.util.ArrayList;

import equinox.Equinox;
import equinox.data.fileType.AircraftModel;
import equinox.plugin.FileType;
import equinox.process.LoadElementGroupsFile;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;

/**
 * Class for create element groups from file task.
 *
 * @author Murat Artim
 * @date Jul 30, 2015
 * @time 11:57:23 AM
 */
public class CreateElementGroupsFromFile extends TemporaryFileCreatingTask<Void> implements LongRunningTask {

	/** A/C model. */
	private final AircraftModel model_;

	/** Element groups file. */
	private final Path file_;

	/**
	 * Creates create element groups from file task.
	 *
	 * @param model
	 *            A/C model.
	 * @param file
	 *            Element groups file.
	 */
	public CreateElementGroupsFromFile(AircraftModel model, Path file) {
		model_ = model;
		file_ = file;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Create element groups from " + file_.getFileName();
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Creating element groups from " + file_.getFileName());

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// get file type
				FileType type = FileType.getFileType(file_.toFile());

				// input file is a ZIP file
				if (type.equals(FileType.ZIP)) {

					// extract files
					updateMessage("Extracting zipped STF files...");
					ArrayList<Path> inputFiles = Utility.extractFilesFromZIP(file_, this, FileType.GRP);

					// no file found
					if (inputFiles == null)
						return null;

					// loop over STF files
					for (Path inputFile : inputFiles) {

						// task cancelled
						if (isCancelled()) {
							connection.rollback();
							connection.setAutoCommit(true);
							return null;
						}

						// load element groups data file (*.grp)
						new LoadElementGroupsFile(this, model_, inputFile).start(connection);
					}
				}

				// input file is a GZIP file
				else if (type.equals(FileType.GZ)) {

					// extract
					Path grpFilePath = getWorkingDirectory().resolve(FileType.appendExtension(FileType.getNameWithoutExtension(file_), FileType.GRP));
					updateMessage("Extracting zipped GRP file...");
					Utility.extractFileFromGZIP(file_, grpFilePath);

					// load element groups data file (*.grp)
					new LoadElementGroupsFile(this, model_, grpFilePath).start(connection);

					// task cancelled
					if (isCancelled()) {
						connection.rollback();
						connection.setAutoCommit(true);
						return null;
					}
				}

				// input file is GRP file
				else {

					// load element groups data file (*.grp)
					new LoadElementGroupsFile(this, model_, file_).start(connection);

					// task cancelled
					if (isCancelled()) {
						connection.rollback();
						connection.setAutoCommit(true);
						return null;
					}
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				if (connection != null) {
					connection.rollback();
					connection.setAutoCommit(true);
				}

				// propagate exception
				throw e;
			}
		}
		return null;
	}
}
