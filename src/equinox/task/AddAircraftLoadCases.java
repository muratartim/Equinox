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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.InputPanel;
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.fileType.AircraftLoadCases;
import equinox.plugin.FileType;
import equinox.process.LoadAircraftLoadCasesFromF07;
import equinox.process.LoadAircraftLoadCasesFromLCS;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;

/**
 * Class for add load cases task.
 *
 * @author Murat Artim
 * @date Aug 5, 2015
 * @time 1:05:04 PM
 */
public class AddAircraftLoadCases extends TemporaryFileCreatingTask<ArrayList<AircraftLoadCase>> implements LongRunningTask {

	/** Input file. */
	private final Path inputFile_;

	/** Load cases folder. */
	private final AircraftLoadCases folder_;

	/**
	 * Creates add load cases task.
	 *
	 * @param inputFile
	 *            Input F07 file.
	 * @param folder
	 *            Load cases folder.
	 */
	public AddAircraftLoadCases(Path inputFile, AircraftLoadCases folder) {
		inputFile_ = inputFile;
		folder_ = folder;
	}

	@Override
	public String getTaskTitle() {
		return "Add load cases";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected ArrayList<AircraftLoadCase> call() throws Exception {

		// initialize list
		ArrayList<AircraftLoadCase> loadCases = new ArrayList<>();

		// update progress info
		updateTitle("Adding load cases...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// get file type
				FileType type = FileType.getFileType(inputFile_.toFile());

				// input file is a ZIP file
				if (type.equals(FileType.ZIP)) {

					// extract files
					updateMessage("Extracting zipped files...");

					// extract F07 files
					ArrayList<Path> inputFiles = Utility.extractFilesFromZIP(inputFile_, this, FileType.F07);

					// no F07 file found, extract LCS files
					if (inputFiles == null) {
						inputFiles = Utility.extractFilesFromZIP(inputFile_, this, FileType.LCS);
					}

					// no file found
					if (inputFiles == null)
						return null;

					// loop over F07 files
					for (Path inputFile : inputFiles) {

						// load element stresses
						ArrayList<AircraftLoadCase> loadedCases = null;

						// F07
						if (FileType.getFileType(inputFile.toFile()).equals(FileType.F07)) {
							loadedCases = new LoadAircraftLoadCasesFromF07(this, inputFile, folder_).start(connection);
						}
						else if (FileType.getFileType(inputFile.toFile()).equals(FileType.LCS)) {
							loadedCases = new LoadAircraftLoadCasesFromLCS(this, inputFile, folder_).start(connection);
						}

						// task canceled
						if (loadedCases == null) {
							connection.rollback();
							connection.setAutoCommit(true);
							return null;
						}

						// add load cases
						loadCases.addAll(loadedCases);
					}
				}

				// input file is a GZIP file
				else if (type.equals(FileType.GZ)) {

					// get file name
					String fileName = FileType.getNameWithoutExtension(inputFile_);

					// initialize file path
					Path filePath = null;

					// F07
					if (fileName.toUpperCase().endsWith(".F07")) {
						filePath = getWorkingDirectory().resolve(FileType.appendExtension(fileName, FileType.F07));
					}
					else if (fileName.toUpperCase().endsWith(".LCS")) {
						filePath = getWorkingDirectory().resolve(FileType.appendExtension(fileName, FileType.LCS));
					}

					// no file found
					if (filePath == null)
						return null;

					// extract
					updateMessage("Extracting zipped file...");
					Utility.extractFileFromGZIP(inputFile_, filePath);

					// load element stresses
					ArrayList<AircraftLoadCase> loadedCases = null;

					// F07
					if (FileType.getFileType(filePath.toFile()).equals(FileType.F07)) {
						loadedCases = new LoadAircraftLoadCasesFromF07(this, filePath, folder_).start(connection);
					}
					else if (FileType.getFileType(filePath.toFile()).equals(FileType.LCS)) {
						loadedCases = new LoadAircraftLoadCasesFromLCS(this, filePath, folder_).start(connection);
					}

					// task canceled
					if (loadedCases == null) {
						connection.rollback();
						connection.setAutoCommit(true);
						return null;
					}

					// add load cases
					loadCases.addAll(loadedCases);
				}

				// input file is F07 file
				else if (type.equals(FileType.F07)) {

					// load element stresses
					ArrayList<AircraftLoadCase> loadedCases = new LoadAircraftLoadCasesFromF07(this, inputFile_, folder_).start(connection);
					if (loadedCases == null) {
						connection.rollback();
						connection.setAutoCommit(true);
						return null;
					}

					// add load cases
					loadCases.addAll(loadedCases);
				}

				// input file is LCS file
				else if (type.equals(FileType.LCS)) {

					// load element stresses
					ArrayList<AircraftLoadCase> loadedCases = new LoadAircraftLoadCasesFromLCS(this, inputFile_, folder_).start(connection);
					if (loadedCases == null) {
						connection.rollback();
						connection.setAutoCommit(true);
						return null;
					}

					// add load cases
					loadCases.addAll(loadedCases);
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

		// return load cases
		return loadCases;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// show file view panel
			taskPanel_.getOwner().getOwner().getInputPanel().showSubPanel(InputPanel.FILE_VIEW_PANEL);

			// add load cases to load cases folder
			folder_.getChildren().addAll(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Creates load cases table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param modelID
	 *            A/C model ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void createLoadCasesTable(Connection connection, int modelID) throws Exception {

		// check if table already exists
		DatabaseMetaData dbmtadta = connection.getMetaData();
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "LOAD_CASE_NAMES_" + modelID, null)) {
			while (resultSet.next())
				return;
		}

		// create table and index
		try (Statement statement = connection.createStatement()) {

			// create load case names table
			String sql = "CREATE TABLE AURORA.LOAD_CASE_NAMES_" + modelID;
			sql += "(LC_ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), LC_NAME VARCHAR(50) NOT NULL, LC_NUM INT NOT NULL, ";
			sql += "LC_COMMENTS VARCHAR(500), UNIQUE(LC_NAME, LC_NUM), PRIMARY KEY(LC_ID))";
			statement.executeUpdate(sql);

			// create load cases table
			sql = "CREATE TABLE AURORA.LOAD_CASES_" + modelID;
			sql += "(LC_ID INT NOT NULL, EID INT NOT NULL, SX DOUBLE, SY DOUBLE, SXY DOUBLE)";
			statement.executeUpdate(sql);

			// create load case table indices
			statement.executeUpdate("CREATE INDEX LOAD_CASES_INDEX_" + modelID + " ON AURORA.LOAD_CASES_" + modelID + "(LC_ID)");
			statement.executeUpdate("CREATE INDEX LOAD_CASES_EID_INDEX_" + modelID + " ON AURORA.LOAD_CASES_" + modelID + "(EID)");
		}
	}
}
