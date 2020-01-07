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

import equinox.data.fileType.AircraftModel;
import equinox.task.TemporaryFileCreatingTask;
import equinox.utility.Utility;

/**
 * Class for load grid data file (*.f07) process.
 *
 * @author Murat Artim
 * @date Jul 6, 2015
 * @time 11:04:57 AM
 */
public class LoadGridDataFile implements EquinoxProcess<ArrayList<Integer>> {

	/** Constants holding ignored coordinates. Grids with any of these coordinates will not be added to database. */
	private static final double NaN1 = 0.0, NaN2 = -99999.0;

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** A/C model for which the grid data to load. */
	private final AircraftModel model_;

	/** Grid data file. */
	private final Path f07File_;

	/** Parameters. */
	private int readLines_, allLines_, numGrids_ = 0;

	/**
	 * Creates load grid data file (*.f07) process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param model
	 *            A/C model for which the grid data to load.
	 * @param f07File
	 *            Grid data file.
	 */
	public LoadGridDataFile(TemporaryFileCreatingTask<?> task, AircraftModel model, Path f07File) {
		task_ = task;
		model_ = model;
		f07File_ = f07File;
	}

	@Override
	public ArrayList<Integer> start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// create list of ignored grid IDs
		ArrayList<Integer> ignoredIDs = new ArrayList<>();

		// prepare statement for adding grids
		task_.updateMessage("Creating grids table...");
		String sql = "insert into " + createGridsTable(connection, model_.getID());
		sql += "(gid, x_coord, y_coord, z_coord, qv_pos, lv_pos) values(?, ?, ?, ?, ?, ?)";
		try (PreparedStatement addGrid = connection.prepareStatement(sql)) {

			// get number of lines of file
			task_.updateMessage("Getting F07 file size...");
			allLines_ = Utility.countLines(f07File_, task_);
			readLines_ = 0;

			// create file reader
			task_.updateMessage("Loading grid data file '" + f07File_.getFileName() + "'...");
			try (BufferedReader reader = Files.newBufferedReader(f07File_, Charset.defaultCharset())) {

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
					if (line.startsWith(" List of Grid GDT data")) {
						line = reader.readLine();
						line = reader.readLine();
						loadGrids(line, reader, addGrid, ignoredIDs);
					}
				}
			}
		}

		// update number of grids of A/C model
		updateNumberOfGrids(connection);

		// return list of ignored grid IDs
		return ignoredIDs;
	}

	/**
	 * Updates number of grids of the A/C model in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void updateNumberOfGrids(Connection connection) throws Exception {

		// update info
		task_.updateMessage("Updating number of grids...");

		// create and execute statement
		String sql = "update ac_models set num_grids = ? where model_id = " + model_.getID();
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			update.setInt(1, numGrids_);
			update.executeUpdate();
		}
	}

	/**
	 * Loads grid data to database.
	 *
	 * @param line
	 *            Current line of F07 file.
	 * @param reader
	 *            File reader.
	 * @param addGrid
	 *            Database statement to add grid data.
	 * @param ignoredIDs
	 *            List of ignored grid IDs.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadGrids(String line, BufferedReader reader, PreparedStatement addGrid, ArrayList<Integer> ignoredIDs) throws Exception {

		// read next line
		while ((line = reader.readLine()) != null) {

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
			int gid = 0;
			double xCoord = 0.0, yCoord = 0.0, zCoord = 0.0;
			String qvPos = null, lvPos = null;

			// get values
			try {

				// get grid ID
				gid = Integer.parseInt(line.substring(9, 17).trim());

				// get x coordinate
				String val = line.substring(18, 26).trim();
				if (val.equals("NaN") || val.substring(1).contains("-")) {
					ignoredIDs.add(gid);
					continue;
				}
				xCoord = Double.parseDouble(val);
				if (xCoord == NaN1 || xCoord == NaN2) {
					ignoredIDs.add(gid);
					continue;
				}

				// get y coordinate
				val = line.substring(27, 35).trim();
				if (val.equals("NaN") || val.substring(1).contains("-")) {
					ignoredIDs.add(gid);
					continue;
				}
				yCoord = Double.parseDouble(val);
				if (yCoord == NaN1 || yCoord == NaN2) {
					ignoredIDs.add(gid);
					continue;
				}

				// get z coordinate
				val = line.substring(36, 44).trim();
				if (val.equals("NaN") || val.substring(1).contains("-")) {
					ignoredIDs.add(gid);
					continue;
				}
				zCoord = Double.parseDouble(val);
				if (zCoord == NaN1 || zCoord == NaN2) {
					ignoredIDs.add(gid);
					continue;
				}

				// get frame-stringer positions
				qvPos = line.substring(45, 53).trim();
				lvPos = line.substring(54, 62).trim();
			}

			// invalid value
			catch (NumberFormatException e) {

				// add warning
				String message = "Could not read grid info for GID = " + gid + " due to following exception.\n";
				message += "Skipping this grid.\n";
				message += e.getMessage() + "\n";
				for (StackTraceElement ste : e.getStackTrace())
					message += ste.toString() + "\n";
				task_.addWarning(message);
				ignoredIDs.add(gid);

				// continue
				continue;
			}

			// add to database
			addGrid.setInt(1, gid);
			addGrid.setDouble(2, xCoord);
			addGrid.setDouble(3, yCoord);
			addGrid.setDouble(4, zCoord);
			if (qvPos.isEmpty())
				addGrid.setNull(5, java.sql.Types.VARCHAR);
			else
				addGrid.setString(5, qvPos);
			if (lvPos.isEmpty())
				addGrid.setNull(6, java.sql.Types.VARCHAR);
			else
				addGrid.setString(6, lvPos);
			addGrid.executeUpdate();
			numGrids_++;
		}
	}

	/**
	 * Creates grids table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param modelID
	 *            A/C model ID.
	 * @return Newly created grids table name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String createGridsTable(Connection connection, int modelID) throws Exception {

		// generate temporary table and index names
		String tableName = "GRIDS_" + modelID;
		String indexName = "GRID_ID_" + modelID;

		// create statement
		try (Statement statement = connection.createStatement()) {

			// create table
			String sql = "CREATE TABLE AURORA." + tableName;
			sql += "(GID INT NOT NULL, X_COORD DOUBLE NOT NULL, Y_COORD DOUBLE NOT NULL, Z_COORD DOUBLE NOT NULL, QV_POS VARCHAR(20), LV_POS VARCHAR(20), PRIMARY KEY(GID))";
			statement.executeUpdate(sql);

			// create index
			statement.executeUpdate("CREATE INDEX " + indexName + " ON AURORA." + tableName + "(GID)");
		}

		// return table name
		return tableName;
	}
}
