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

import equinox.data.ElementType;
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.fileType.AircraftLoadCases;
import equinox.task.AddAircraftLoadCases;
import equinox.task.InternalEquinoxTask;
import equinox.utility.Utility;

/**
 * Class for load load cases process.
 *
 * @author Murat Artim
 * @date Aug 5, 2015
 * @time 1:19:29 PM
 */
public class LoadAircraftLoadCasesFromF07 implements EquinoxProcess<ArrayList<AircraftLoadCase>> {

	/** The owner task of this process. */
	private final InternalEquinoxTask<?> task_;

	/** Input F07 file. */
	private final Path inputFile_;

	/** Load cases folder. */
	private final AircraftLoadCases folder_;

	/**
	 * Creates load load cases process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input F07 file.
	 * @param folder
	 *            Load cases folder.
	 */
	public LoadAircraftLoadCasesFromF07(InternalEquinoxTask<?> task, Path inputFile, AircraftLoadCases folder) {
		task_ = task;
		inputFile_ = inputFile;
		folder_ = folder;
	}

	@Override
	public ArrayList<AircraftLoadCase> start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// create load case list
		ArrayList<AircraftLoadCase> loadCases = new ArrayList<>();

		// get number of lines of file
		task_.updateMessage("Getting F07 file size...");
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

		// prepare statement to check if the load case is already contained
		String sql = "select lc_id from LOAD_CASE_NAMES_" + folder_.getID() + " where lc_name = ? and lc_num = ?";
		try (PreparedStatement checkLoadCases = connection.prepareStatement(sql)) {

			// prepare statement for inserting into load case names
			sql = "insert into LOAD_CASE_NAMES_" + folder_.getID() + "(lc_name, lc_num) values(?, ?)";
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

								// table start
								if (line.equals("1")) {

									// read next line
									line = reader.readLine();

									// null line
									if (line == null)
										continue;

									// get load case name and number
									String loadCaseName = line.substring(0, 9).trim();
									int loadCaseNum = Integer.parseInt(line.substring(11, 15).trim());

									// check if load case already exists
									int loadCaseID = -1;
									checkLoadCases.setString(1, loadCaseName);
									checkLoadCases.setInt(2, loadCaseNum);
									try (ResultSet resultSet = checkLoadCases.executeQuery()) {
										if (resultSet.next())
											loadCaseID = resultSet.getInt("lc_id");
									}

									// load case not contained
									if (loadCaseID == -1) {

										// insert to load case names table
										insertToLoadCases.setString(1, loadCaseName);
										insertToLoadCases.setInt(2, loadCaseNum);
										insertToLoadCases.executeUpdate();
										try (ResultSet resultSet = insertToLoadCases.getGeneratedKeys()) {
											if (resultSet.next())
												loadCaseID = resultSet.getBigDecimal(1).intValue();
										}

										// add to load cases list
										loadCases.add(new AircraftLoadCase(loadCaseID, loadCaseName, loadCaseNum));
									}

									// set ID to load case statement
									insertToStresses.setInt(1, loadCaseID);

									// continue
									continue;
								}

								// element stress
								else if (line.startsWith(ElementType.BEAM) || line.startsWith(ElementType.QUAD) || line.startsWith(ElementType.ROD) || line.startsWith(ElementType.TRIA)) {

									// get element type
									String type = line.substring(0, 8).trim();

									// get element ID
									int eid = Integer.parseInt(line.substring(8, 17).trim());

									// check if element ID exists in the model
									selectElement.setInt(1, eid);
									try (ResultSet resultSet = selectElement.executeQuery()) {

										// unknown element ID
										if (!resultSet.last())
											continue;
									}

									// set EID
									insertToStresses.setInt(2, eid);

									// QUAD or TRIA
									if (type.equals(ElementType.QUAD) || type.equals(ElementType.TRIA)) {

										// extract stress values
										Double sx = getStress(line.substring(26, 35).trim());
										Double sy = getStress(line.substring(35, 44).trim());
										Double sxy = getStress(line.substring(44, 53).trim());

										// there is null stress
										if (sx == null || sy == null || sxy == null)
											continue;

										// set stresses
										insertToStresses.setDouble(3, sx);
										insertToStresses.setDouble(4, sy);
										insertToStresses.setDouble(5, sxy);
									}

									// BEAM or ROD
									else if (type.equals(ElementType.BEAM) || type.equals(ElementType.ROD)) {

										// extract stress values
										Double sx = getStress(line.substring(26, 35).trim());

										// there is null stress
										if (sx == null)
											continue;

										// set stresses
										insertToStresses.setDouble(3, sx);
										insertToStresses.setNull(4, java.sql.Types.DOUBLE);
										insertToStresses.setNull(5, java.sql.Types.DOUBLE);
									}

									// execute update
									insertToStresses.executeUpdate();
								}
							}
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

		// parse double
		try {
			return Double.parseDouble(text);
		}

		// unrecognized string
		catch (NumberFormatException e) {

			// no value
			if (text.length() == 1 && text.contains("-"))
				return null;

			// contains negative exponent
			int index = text.lastIndexOf("-");
			if (index > 0)
				return Double.parseDouble(text.substring(0, index) + "E" + text.substring(index, text.length()));

			// contains positive exponent
			index = text.lastIndexOf("+");
			if (index > 0)
				return Double.parseDouble(text.substring(0, index) + "E" + text.substring(index, text.length()));

			// cannot parse value
			throw e;
		}
	}
}
