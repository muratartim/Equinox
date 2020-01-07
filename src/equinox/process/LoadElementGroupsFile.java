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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import equinox.data.fileType.AircraftModel;
import equinox.task.InternalEquinoxTask;
import equinox.utility.Utility;

/**
 * Class for loading element groups data file.
 *
 * @author Murat Artim
 * @date Jul 8, 2015
 * @time 12:15:15 PM
 */
public class LoadElementGroupsFile implements EquinoxProcess<Void> {

	/** The owner task of this process. */
	private final InternalEquinoxTask<?> task_;

	/** A/C model for which the element data to load. */
	private final AircraftModel model_;

	/** Element groups data file. */
	private final Path grpFile_;

	/**
	 * Creates load element groups data file (*.grp) process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param model
	 *            A/C model for which the element groups data to load.
	 * @param grpFile
	 *            Element groups data file. Can be null (in this case, only element group tables will be created).
	 */
	public LoadElementGroupsFile(InternalEquinoxTask<?> task, AircraftModel model, Path grpFile) {
		task_ = task;
		model_ = model;
		grpFile_ = grpFile;
	}

	@Override
	public Void start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update info
		task_.updateMessage("Loading element groups data...");

		// create element groups tables (if necessary)
		createElementGroupsTables(connection, model_.getID());

		// no file given
		if (grpFile_ == null)
			return null;

		// get table name
		String elementsTableName = "ELEMENTS_" + model_.getID();

		// get number of lines of file
		task_.updateMessage("Getting GRP file size...");
		int allLines = Utility.countLines(grpFile_, task_);
		int readLines = 0;

		// prepare statement for inserting into element group names
		String sql = "insert into ELEMENT_GROUP_NAMES_" + model_.getID() + "(name) values(?)";
		try (PreparedStatement insertToGroupNames = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// prepare statement for updating number of elements in a group
			sql = "update ELEMENT_GROUP_NAMES_" + model_.getID() + " set numel = ? where group_id = ?";
			try (PreparedStatement updateNumEls = connection.prepareStatement(sql)) {

				// prepare statement for inserting into element groups
				sql = "insert into ELEMENT_GROUPS_" + model_.getID() + "(group_id, eid) values(?, ?)";
				try (PreparedStatement insertToGroups = connection.prepareStatement(sql)) {

					// prepare statement for getting interval of elements
					sql = "select eid from " + elementsTableName + " where eid >= ? and eid <= ? order by eid";
					try (PreparedStatement getInterval = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

						// prepare statement for selecting element IDs
						sql = "select eid from " + elementsTableName + " where eid = ?";
						try (PreparedStatement selectElement = connection.prepareStatement(sql)) {

							// create file reader
							try (BufferedReader reader = Files.newBufferedReader(grpFile_, Charset.defaultCharset())) {

								// read till the end of file
								String line;
								while ((line = reader.readLine()) != null) {

									// task cancelled
									if (task_.isCancelled())
										return null;

									// increment read lines
									readLines++;

									// update progress
									task_.updateProgress(readLines, allLines);

									// comment or empty line
									if (line.trim().isEmpty() || line.startsWith("#"))
										continue;

									// interval
									if (line.startsWith("Interval")) {

										// split line
										String[] split = line.split("\t");

										// get group name
										String name = split[1].trim();

										// update info
										task_.updateMessage("Loading group '" + name + "'...");

										// insert to group names
										int groupID = -1;
										insertToGroupNames.setString(1, name);
										insertToGroupNames.executeUpdate();
										try (ResultSet resultSet = insertToGroupNames.getGeneratedKeys()) {
											if (resultSet.next())
												groupID = resultSet.getBigDecimal(1).intValue();
										}

										// insert into groups
										getInterval.setInt(1, Integer.parseInt(split[2].trim()));
										getInterval.setInt(2, Integer.parseInt(split[3].trim()));
										int numEls = 0;
										try (ResultSet resultSet = getInterval.executeQuery()) {
											insertToGroups.setInt(1, groupID);
											while (resultSet.next()) {
												insertToGroups.setInt(2, resultSet.getInt("eid"));
												insertToGroups.executeUpdate();
												numEls++;
											}
										}

										// update number of elements in group names table
										updateNumEls.setInt(1, numEls);
										updateNumEls.setInt(2, groupID);
										updateNumEls.executeUpdate();
									}

									// group
									else if (line.startsWith("Group")) {

										// get group name
										String name = line.split("\t")[1].trim();

										// update info
										task_.updateMessage("Loading group '" + name + "'...");

										// insert to group names
										int groupID = -1;
										insertToGroupNames.setString(1, name);
										insertToGroupNames.executeUpdate();
										try (ResultSet resultSet = insertToGroupNames.getGeneratedKeys()) {
											if (resultSet.next())
												groupID = resultSet.getBigDecimal(1).intValue();
										}

										// set group ID
										insertToGroups.setInt(1, groupID);

										// loop till the end of group
										int numEls = 0;
										while ((line = reader.readLine()) != null) {

											// task cancelled
											if (task_.isCancelled())
												return null;

											// increment read lines
											readLines++;

											// update progress
											task_.updateProgress(readLines, allLines);

											// comment or empty line
											if (line.trim().isEmpty() || line.startsWith("#"))
												continue;

											// end of group
											if (line.startsWith("End"))
												break;

											// insert into groups
											selectElement.setInt(1, Integer.parseInt(line.trim()));
											try (ResultSet resultSet = selectElement.executeQuery()) {
												while (resultSet.next()) {
													insertToGroups.setInt(2, resultSet.getInt("eid"));
													insertToGroups.executeUpdate();
													numEls++;
												}
											}
										}

										// update number of elements in group names table
										updateNumEls.setInt(1, numEls);
										updateNumEls.setInt(2, groupID);
										updateNumEls.executeUpdate();
									}
								}
							}
						}
					}
				}
			}
		}

		// return
		return null;
	}

	/**
	 * Creates element groups table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param modelID
	 *            A/C model ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void createElementGroupsTables(Connection connection, int modelID) throws Exception {

		// update info
		task_.updateMessage("Creating element group tables...");

		// check if table already exists
		DatabaseMetaData dbmtadta = connection.getMetaData();
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ELEMENT_GROUP_NAMES_" + modelID, null)) {
			while (resultSet.next())
				return;
		}

		// create table and index
		try (Statement statement = connection.createStatement()) {

			// create group names table
			String sql = "CREATE TABLE AURORA.ELEMENT_GROUP_NAMES_" + modelID;
			sql += "(GROUP_ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), NAME VARCHAR(100) NOT NULL UNIQUE, NUMEL INT, PRIMARY KEY(GROUP_ID))";
			statement.executeUpdate(sql);

			// create groups table
			sql = "CREATE TABLE AURORA.ELEMENT_GROUPS_" + modelID;
			sql += "(GROUP_ID INT NOT NULL, EID INT NOT NULL)";
			statement.executeUpdate(sql);

			// create groups table index
			sql = "CREATE INDEX ELEMENT_GROUPS_INDEX_" + modelID;
			sql += " ON AURORA.ELEMENT_GROUPS_" + modelID + "(GROUP_ID)";
			statement.executeUpdate(sql);
		}
	}
}
