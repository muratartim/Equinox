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
import java.sql.Statement;
import java.util.ArrayList;

import equinox.data.ElementType;
import equinox.data.fileType.AircraftModel;
import equinox.task.TemporaryFileCreatingTask;
import equinox.utility.Utility;

/**
 * Class for load element data file (*.f06) process.
 *
 * @author Murat Artim
 * @date Jul 6, 2015
 * @time 4:00:09 PM
 */
public class LoadElementDataFile implements EquinoxProcess<Void> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** A/C model for which the element data to load. */
	private final AircraftModel model_;

	/** Element data file. */
	private final Path f06File_;

	/** Parameters. */
	private int readLines_, allLines_, numElements_ = 0, numQuads_ = 0, numBeams_ = 0, numRods_ = 0, numTrias_ = 0, numShears_ = 0;

	/** Ignored grid IDs. */
	private final ArrayList<Integer> ignoredGridIDs_;

	/**
	 * Creates load element data file (*.f06) process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param model
	 *            A/C model for which the element data to load.
	 * @param f06File
	 *            Element data file.
	 * @param ignoredGridIDs
	 *            Ignored grid IDs.
	 */
	public LoadElementDataFile(TemporaryFileCreatingTask<?> task, AircraftModel model, Path f06File, ArrayList<Integer> ignoredGridIDs) {
		task_ = task;
		model_ = model;
		f06File_ = f06File;
		ignoredGridIDs_ = ignoredGridIDs;
	}

	@Override
	public Void start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// prepare statement for adding grids
		task_.updateMessage("Creating elements table...");
		String sql = "insert into " + createElementsTable(connection, model_.getID());
		sql += "(eid, el_type, pid, g1, g2, g3, g4) values(?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement addElement = connection.prepareStatement(sql)) {

			// get number of lines of file
			task_.updateMessage("Getting F06 file size...");
			allLines_ = Utility.countLines(f06File_, task_);
			readLines_ = 0;

			// create file reader
			task_.updateMessage("Loading element data file '" + f06File_.getFileName() + "'...");
			try (BufferedReader reader = Files.newBufferedReader(f06File_, Charset.defaultCharset())) {

				// read till the end of file
				String line;
				while ((line = reader.readLine()) != null) {

					// task cancelled
					if (task_.isCancelled())
						return null;

					// increment read lines
					readLines_++;

					// update progress
					task_.updateProgress(readLines_, allLines_);

					// empty line
					if (line.isEmpty())
						continue;

					// start of grid data
					if (line.startsWith(" List of Elem. Def data")) {
						line = reader.readLine();
						line = reader.readLine();
						loadElements(line, reader, addElement);
					}
				}
			}
		}

		// update number of elements of A/C model
		updateNumberOfElements(connection);

		// return
		return null;
	}

	/**
	 * Updates number of elements of the A/C model in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void updateNumberOfElements(Connection connection) throws Exception {

		// update info
		task_.updateMessage("Updating number of elements...");

		// create and execute statement
		String sql = "update ac_models set num_elems = ?, num_quads = ?, num_rods = ?, num_beams = ?, num_trias = ?, num_shears = ? where model_id = " + model_.getID();
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			update.setInt(1, numElements_);
			update.setInt(2, numQuads_);
			update.setInt(3, numRods_);
			update.setInt(4, numBeams_);
			update.setInt(5, numTrias_);
			update.setInt(6, numShears_);
			update.executeUpdate();
		}
	}

	/**
	 * Loads element data to database.
	 *
	 * @param line
	 *            Current line of F06 file.
	 * @param reader
	 *            File reader.
	 * @param addElement
	 *            Database statement to add grid data.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadElements(String line, BufferedReader reader, PreparedStatement addElement) throws Exception {

		// read next line
		nextLine: while ((line = reader.readLine()) != null) {

			// task cancelled
			if (task_.isCancelled())
				return;

			// increment read lines
			readLines_++;

			// update progress
			task_.updateProgress(readLines_, allLines_);

			// end of grid data
			if (line.trim().isEmpty() || line.trim().equals("1"))
				return;

			// initialize variables
			String eType = null;
			int eid = 0, pid = 0;
			Integer g1 = null, g2 = null, g3 = null, g4 = null;

			// get values
			try {

				// get element type, ID and PID
				eType = line.substring(0, 8).trim();
				eid = Integer.parseInt(line.substring(9, 17).trim());
				pid = Integer.parseInt(line.substring(18, 26).trim());

				// BEAM
				if (eType.equals(ElementType.BEAM)) {
					g1 = Integer.parseInt(line.substring(27, 35).trim());
					g2 = Integer.parseInt(line.substring(36, 44).trim());
				}

				// QUAD
				else if (eType.equals(ElementType.QUAD)) {
					g1 = Integer.parseInt(line.substring(27, 35).trim());
					g2 = Integer.parseInt(line.substring(36, 44).trim());
					g3 = Integer.parseInt(line.substring(45, 53).trim());
					g4 = Integer.parseInt(line.substring(54, 62).trim());
				}

				// ROD
				else if (eType.equals(ElementType.ROD)) {
					g1 = Integer.parseInt(line.substring(27, 35).trim());
					g2 = Integer.parseInt(line.substring(36, 44).trim());
				}

				// TRIA
				else if (eType.equals(ElementType.TRIA)) {
					g1 = Integer.parseInt(line.substring(27, 35).trim());
					g2 = Integer.parseInt(line.substring(36, 44).trim());
					g3 = Integer.parseInt(line.substring(45, 53).trim());
				}

				// SHEAR
				else if (eType.equals(ElementType.SHEAR)) {
					g1 = Integer.parseInt(line.substring(27, 35).trim());
					g2 = Integer.parseInt(line.substring(36, 44).trim());
					g3 = Integer.parseInt(line.substring(45, 53).trim());
					g4 = Integer.parseInt(line.substring(54, 62).trim());
				}
			}

			// invalid value
			catch (NumberFormatException e) {

				// add warning
				String message = "Could not read element info for EID = " + eid + " due to following exception.\n";
				message += "Skipping this element.\n";
				message += e.getMessage() + "\n";
				for (StackTraceElement ste : e.getStackTrace())
					message += ste.toString() + "\n";
				task_.addWarning(message);

				// continue
				continue;
			}

			// check if any of element nodes are contained in ignored list
			for (Integer ignoredID : ignoredGridIDs_) {
				if (g1 != null && g1.intValue() == ignoredID.intValue())
					continue nextLine;
				if (g2 != null && g2.intValue() == ignoredID.intValue())
					continue nextLine;
				if (g3 != null && g3.intValue() == ignoredID.intValue())
					continue nextLine;
				if (g4 != null && g4.intValue() == ignoredID.intValue())
					continue nextLine;
			}

			// add to database
			addElement.setInt(1, eid);
			addElement.setString(2, eType);
			addElement.setInt(3, pid);
			if (g1 == null)
				addElement.setNull(4, java.sql.Types.INTEGER);
			else
				addElement.setInt(4, g1);
			if (g2 == null)
				addElement.setNull(5, java.sql.Types.INTEGER);
			else
				addElement.setInt(5, g2);
			if (g3 == null)
				addElement.setNull(6, java.sql.Types.INTEGER);
			else
				addElement.setInt(6, g3);
			if (g4 == null)
				addElement.setNull(7, java.sql.Types.INTEGER);
			else
				addElement.setInt(7, g4);
			addElement.executeUpdate();

			// increment counts
			numElements_++;
			if (eType.equals(ElementType.BEAM))
				numBeams_++;
			else if (eType.equals(ElementType.QUAD))
				numQuads_++;
			else if (eType.equals(ElementType.ROD))
				numRods_++;
			else if (eType.equals(ElementType.TRIA))
				numTrias_++;
			else if (eType.equals(ElementType.SHEAR))
				numShears_++;
		}
	}

	/**
	 * Creates elements table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param modelID
	 *            A/C model ID.
	 * @return Newly created elements table name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String createElementsTable(Connection connection, int modelID) throws Exception {

		// generate temporary table and index names
		String tableName = "ELEMENTS_" + modelID;
		String indexName1 = "ELEM_ID_" + modelID;
		String indexName2 = "ELEM_TYPE_" + modelID;

		// create statement
		try (Statement statement = connection.createStatement()) {

			// create table
			String sql = "CREATE TABLE AURORA." + tableName;
			sql += "(EID INT NOT NULL, EL_TYPE VARCHAR(20) NOT NULL, PID INT NOT NULL, G1 INT, G2 INT, G3 INT, G4 INT, PRIMARY KEY(EID))";
			statement.executeUpdate(sql);

			// create indices
			statement.executeUpdate("CREATE INDEX " + indexName1 + " ON AURORA." + tableName + "(EID)");
			statement.executeUpdate("CREATE INDEX " + indexName2 + " ON AURORA." + tableName + "(EL_TYPE)");
		}

		// return table name
		return tableName;
	}
}
