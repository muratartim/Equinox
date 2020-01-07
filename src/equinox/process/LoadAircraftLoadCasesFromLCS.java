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
package equinox.process;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import equinox.data.fileType.AircraftLoadCase;
import equinox.data.fileType.AircraftLoadCases;
import equinox.task.AddAircraftLoadCases;
import equinox.task.InternalEquinoxTask;
import equinox.utility.Utility;

/**
 * Class for load load cases from LCS file process.
 *
 * @author Murat Artim
 * @date Sep 9, 2015
 * @time 1:53:17 PM
 */
public class LoadAircraftLoadCasesFromLCS implements EquinoxProcess<ArrayList<AircraftLoadCase>> {

	/** The owner task of this process. */
	private final InternalEquinoxTask<?> task_;

	/** Input LCS file. */
	private final Path inputFile_;

	/** Load cases folder. */
	private final AircraftLoadCases folder_;

	/**
	 * Creates load load cases process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input LCS file.
	 * @param folder
	 *            Load cases folder.
	 */
	public LoadAircraftLoadCasesFromLCS(InternalEquinoxTask<?> task, Path inputFile, AircraftLoadCases folder) {
		task_ = task;
		inputFile_ = inputFile;
		folder_ = folder;
	}

	@Override
	public ArrayList<AircraftLoadCase> start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// create load case list
		ArrayList<AircraftLoadCase> loadCases = new ArrayList<>();

		// get number of lines of file
		task_.updateMessage("Getting LCS file size...");
		int allLines = Utility.countLines(inputFile_, task_);
		int readLines = 0;

		// task cancelled
		if (task_.isCancelled())
			return null;

		// create load cases table (if necessary)
		task_.updateMessage("Creating load cases table...");
		AddAircraftLoadCases.createLoadCasesTable(connection, folder_.getID());

		// task cancelled
		if (task_.isCancelled())
			return null;

		// prepare statement for inserting into load case names
		String sql = "insert into LOAD_CASE_NAMES_" + folder_.getID() + "(lc_name, lc_num, lc_comments) values(?, ?, ?)";
		try (PreparedStatement insertToLoadCases = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// prepare statement for inserting into load cases
			sql = "insert into LOAD_CASES_" + folder_.getID() + "(lc_id, eid, sx, sy, sxy) values(?, ?, ?, ?, ?)";
			try (PreparedStatement insertToStresses = connection.prepareStatement(sql)) {

				// prepare statement for selecting element IDs
				sql = "select eid from ELEMENTS_" + folder_.getID() + " where eid = ?";
				try (PreparedStatement selectElement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

					// create file reader
					try (BufferedReader reader = Files.newBufferedReader(inputFile_, Charset.defaultCharset())) {

						// read file till the end
						task_.updateMessage("Loading element stresses...");
						boolean loadCaseNameSet = false;
						String line;
						while ((line = reader.readLine()) != null) {

							// task cancelled
							if (task_.isCancelled())
								return null;

							// update info
							task_.updateProgress(readLines, allLines);
							readLines++;

							// trim line
							line = line.trim();

							// empty line
							if (line.isEmpty())
								continue;

							// load case name and number
							if (line.startsWith("Load case name:")) {

								// get load case name
								String[] split = line.split(":");
								String loadCaseName = split[1].trim();

								// read next line
								line = reader.readLine();
								if (line == null)
									continue;

								// get load case number
								split = line.split(":");
								int loadCaseNum = Integer.parseInt(split[1].trim());

								// read next line
								line = reader.readLine();
								if (line == null)
									continue;

								// get load case comment
								split = line.split(":");
								String loadCaseComment = null;
								if (split.length > 1)
									loadCaseComment = split[1].trim();

								// insert to load case names
								int loadCaseID = -1;
								insertToLoadCases.setString(1, loadCaseName);
								insertToLoadCases.setInt(2, loadCaseNum);
								if (loadCaseComment == null)
									insertToLoadCases.setNull(3, java.sql.Types.VARCHAR);
								else
									insertToLoadCases.setString(3, loadCaseComment);
								insertToLoadCases.executeUpdate();
								try (ResultSet resultSet = insertToLoadCases.getGeneratedKeys()) {
									if (resultSet.next())
										loadCaseID = resultSet.getBigDecimal(1).intValue();
								}

								// set ID to load case statement
								insertToStresses.setInt(1, loadCaseID);

								// add to load cases list if not contained
								addToLoadCases(loadCases, loadCaseID, loadCaseName, loadCaseNum);

								// read till start of element stresses
								line = reader.readLine();
								line = reader.readLine();
								loadCaseNameSet = true;
								continue;
							}

							// load case name not set
							if (!loadCaseNameSet)
								continue;

							// get element ID
							int eid = Integer.parseInt(line.substring(0, 12).trim());

							// check if element ID exists in the model
							selectElement.setInt(1, eid);
							try (ResultSet resultSet = selectElement.executeQuery()) {

								// unknown element ID
								if (!resultSet.last())
									continue;
							}

							// set EID
							insertToStresses.setInt(2, eid);

							// extract stress values
							Double sx = getStress(line.substring(12, 24).trim());
							Double sy = getStress(line.substring(24, 36).trim());
							Double sxy = getStress(line.substring(36).trim());

							// there is null stress
							if (sx == null && sy == null && sxy == null)
								continue;

							// set stresses
							if (sx == null)
								insertToStresses.setNull(3, java.sql.Types.DOUBLE);
							else
								insertToStresses.setDouble(3, sx);
							if (sy == null)
								insertToStresses.setNull(4, java.sql.Types.DOUBLE);
							else
								insertToStresses.setDouble(4, sy);
							if (sxy == null)
								insertToStresses.setNull(5, java.sql.Types.DOUBLE);
							else
								insertToStresses.setDouble(5, sxy);

							// execute update
							insertToStresses.executeUpdate();
						}
					}
				}
			}
		}

		// return load cases
		return loadCases;
	}

	/**
	 * Returns the element stress value from the given text, or null if no stress value found.
	 *
	 * @param text
	 *            Text.
	 * @return Element stress value, or null if no stress value found.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getStress(String text) throws Exception {
		if (text.equals("-"))
			return null;
		return Double.parseDouble(text);
	}

	/**
	 * Adds new load case with given name and number if not already contained.
	 *
	 * @param loadCases
	 *            Load cases list.
	 * @param id
	 *            Load case ID.
	 * @param loadCaseName
	 *            Load case name.
	 * @param loadCaseNum
	 *            Load case number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void addToLoadCases(ArrayList<AircraftLoadCase> loadCases, int id, String loadCaseName, int loadCaseNum) throws Exception {
		for (AircraftLoadCase loadCase : loadCases) {
			if (loadCase.getID() == id)
				return;
		}
		loadCases.add(new AircraftLoadCase(id, loadCaseName, loadCaseNum));
	}
}
