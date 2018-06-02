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

import equinox.data.fileType.ExternalFlight;
import equinox.data.fileType.ExternalFlights;
import equinox.data.fileType.ExternalStressSequence;
import equinox.plugin.FileType;
import equinox.task.TemporaryFileCreatingTask;
import equinox.utility.Utility;

/**
 * Class for load STH file process.
 *
 * @author Murat Artim
 * @date Mar 12, 2015
 * @time 9:47:03 AM
 */
public class LoadSTHFile implements EquinoxProcess<ExternalStressSequence> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** Path to input STH and FLS files. */
	private final Path sthFile_, flsFile_;

	/** Parameters. */
	private int readLines_, allLines_, flightNumber_, numPeaks_;

	/** Update message header. */
	private String line_;

	/**
	 * Creates load STH file process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param sthFile
	 *            Path to input STH file.
	 * @param flsFile
	 *            Path to input FLS file.
	 */
	public LoadSTHFile(TemporaryFileCreatingTask<?> task, Path sthFile, Path flsFile) {
		task_ = task;
		sthFile_ = sthFile;
		flsFile_ = flsFile;
	}

	@Override
	public ExternalStressSequence start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// create stress sequence
		ExternalStressSequence sequence = createStressSequence(connection);
		ExternalFlights flights = new ExternalFlights(sequence.getID());

		// task cancelled
		if (task_.isCancelled())
			return null;

		// create STH peaks table
		String sthPeaksTableName = createPeaksTable(connection, sequence.getID());

		// task cancelled
		if (task_.isCancelled())
			return null;

		// load STH file
		loadSTHFile(sequence, flights, sthPeaksTableName, connection);

		// add flights folder to sequence
		sequence.getChildren().add(flights);

		// task cancelled
		if (task_.isCancelled())
			return null;

		// load FLS file
		loadFLSFile(sequence, connection);

		// task cancelled
		if (task_.isCancelled())
			return null;

		// return sequence
		return sequence;
	}

	/**
	 * Loads input FLS file into database.
	 *
	 * @param sequence
	 *            The owner stress sequence.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadFLSFile(ExternalStressSequence sequence, Connection connection) throws Exception {

		// get number of lines of file
		task_.updateMessage("Getting FLS file size...");
		allLines_ = Utility.countLines(flsFile_, task_);
		readLines_ = 0;

		// create file reader
		try (BufferedReader reader = Files.newBufferedReader(flsFile_, Charset.defaultCharset())) {

			// create statement for inserting flights
			task_.updateMessage("Loading FLS flights to database...");
			String sql = "insert into ext_fls_flights(sth_id, flight_num, name, severity) values(?, ?, ?, ?)";
			try (PreparedStatement update = connection.prepareStatement(sql)) {

				// set file ID
				update.setInt(1, sequence.getID());

				// read file till the end
				String delimiter = null;
				while ((line_ = reader.readLine()) != null) {

					// task cancelled
					if (task_.isCancelled())
						return;

					// increment read lines
					readLines_++;

					// update progress
					task_.updateProgress(readLines_, allLines_);

					// comment line
					if (line_.startsWith("#"))
						continue;

					// set column delimiter
					if (delimiter == null)
						delimiter = line_.trim().contains("\t") ? "\t" : " ";

					// split line
					String[] split = line_.trim().split(delimiter);

					// loop over columns
					int index = 0;
					for (String col : split) {

						// invalid value
						if (col == null || col.isEmpty())
							continue;

						// trim spaces
						col = col.trim();

						// invalid value
						if (col.isEmpty())
							continue;

						// flight number
						if (index == 0)
							update.setInt(2, Integer.parseInt(col));

						// name
						else if (index == 1)
							update.setString(3, col);

						// severity
						else if (index == 2)
							update.setString(4, col);

						// increment index
						index++;
					}

					// execute update
					update.executeUpdate();
				}
			}
		}
	}

	/**
	 * Loads stress sequence from STH file.
	 *
	 * @param sequence
	 *            Stress sequence.
	 * @param flights
	 *            Typical flights folder.
	 * @param sthPeaksTableName
	 *            Name of STH peaks table.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadSTHFile(ExternalStressSequence sequence, ExternalFlights flights, String sthPeaksTableName, Connection connection) throws Exception {

		// get number of lines of file
		task_.updateMessage("Getting STH file size...");
		allLines_ = Utility.countLines(sthFile_, task_);
		readLines_ = 0;

		// prepare statement for adding flights
		String sql = "insert into ext_sth_flights(file_id, flight_num, name, severity, num_peaks, validity, block_size, max_val, min_val) values(?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement addFlight = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			addFlight.setInt(1, sequence.getID()); // file ID

			// prepare statement for adding peaks
			sql = "insert into " + sthPeaksTableName + "(flight_id, peak_num, peak_val) values(?, ?, ?)";
			try (PreparedStatement addPeaks = connection.prepareStatement(sql)) {

				// prepare statement for updating flight
				sql = "update ext_sth_flights set max_val = ?, min_val = ? where file_ID = " + sequence.getID() + " and flight_id = ?";
				try (PreparedStatement updateFlight = connection.prepareStatement(sql)) {

					// create file reader
					try (BufferedReader reader = Files.newBufferedReader(sthFile_, Charset.defaultCharset())) {

						// read file till the end
						while ((line_ = reader.readLine()) != null) {

							// task cancelled
							if (task_.isCancelled())
								return;

							// increment read lines
							readLines_++;

							// update progress
							task_.updateProgress(readLines_, allLines_);

							// comment line
							if (readLines_ < 5)
								continue;

							// add flight to flights table
							ExternalFlight flight = addToFlightsTable(reader, addFlight);

							// task cancelled
							if (task_.isCancelled())
								return;

							// add flight to flights folder
							flights.getChildren().add(flight);

							// add peaks to peaks table
							addToPeaksTable(reader, flight.getID(), addPeaks, updateFlight);
						}
					}
				}
			}
		}

		// task cancelled
		if (task_.isCancelled())
			return;

		// set number of flights
		setNumberOfFlights(connection, sequence.getID());
	}

	/**
	 * Adds input peaks to peaks table.
	 *
	 * @param reader
	 *            File reader.
	 * @param flightID
	 *            Flight ID.
	 * @param addPeaks
	 *            Database statement for adding peaks.
	 * @param updateFlight
	 *            Database statement for updating flight.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void addToPeaksTable(BufferedReader reader, int flightID, PreparedStatement addPeaks, PreparedStatement updateFlight) throws Exception {

		// update info
		task_.updateMessage("Saving peaks for flight " + (flightNumber_ - 1) + " to database...");

		// set flight ID
		addPeaks.setInt(1, flightID);
		updateFlight.setInt(3, flightID);

		// initialize number of read peaks
		int readPeaks = 0;
		double maxVal = Double.NEGATIVE_INFINITY, minVal = Double.POSITIVE_INFINITY, peakVal = 0.0;

		// read till the end
		while ((line_ = reader.readLine()) != null) {

			// task cancelled
			if (task_.isCancelled())
				break;

			// increment read lines
			readLines_++;

			// update progress
			task_.updateProgress(readLines_, allLines_);

			// split line
			String[] split = line_.trim().split(" ");

			// loop over columns
			for (String col : split) {

				// invalid value
				if (col == null || col.isEmpty())
					continue;

				// trim spaces
				col = col.trim();

				// invalid value
				if (col.isEmpty())
					continue;

				// get peak value
				peakVal = Double.parseDouble(col);

				// set peak number and value
				addPeaks.setInt(2, readPeaks);
				addPeaks.setDouble(3, peakVal);
				addPeaks.executeUpdate();

				// update max-min values
				if (peakVal >= maxVal)
					maxVal = peakVal;
				if (peakVal <= minVal)
					minVal = peakVal;

				// increment peak number
				readPeaks++;
			}

			// all peaks read
			if (readPeaks == numPeaks_) {

				// update max-min values of the flight
				updateFlight.setDouble(1, maxVal); // max value
				updateFlight.setDouble(2, minVal); // min value
				updateFlight.executeUpdate();

				// return
				return;
			}
		}
	}

	/**
	 * Adds input flight to flights table.
	 *
	 * @param reader
	 *            File reader.
	 * @param addFlight
	 *            Database statement for adding flight.
	 * @return The added flight.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ExternalFlight addToFlightsTable(BufferedReader reader, PreparedStatement addFlight) throws Exception {

		// update info
		task_.updateMessage("Saving flight info for flight " + flightNumber_ + " to database...");

		// initialize variables
		String flightName = null, severity = "";
		double validity = 0.0, blockSize = 0.0;

		// split line
		String[] split = line_.trim().split(" ");

		// loop over columns
		int index = 0;
		for (String col : split) {

			// invalid value
			if (col == null || col.isEmpty())
				continue;

			// trim spaces
			col = col.trim();

			// invalid value
			if (col.isEmpty())
				continue;

			// validity
			if (index == 0)
				validity = Double.parseDouble(col);

			// block size
			else if (index == 1)
				blockSize = Double.parseDouble(col);

			// increment index
			index++;
		}

		// read next line
		line_ = reader.readLine();
		readLines_++;
		task_.updateProgress(readLines_, allLines_);

		// null line
		if (line_ == null)
			throw new Exception("Null line encountered during reading STH file.");

		// split line
		split = line_.trim().split(" ");

		// loop over columns
		index = 0;
		for (String col : split) {

			// invalid value
			if (col == null || col.isEmpty())
				continue;

			// trim spaces
			col = col.trim();

			// invalid value
			if (col.isEmpty())
				continue;

			// number of peaks
			if (index == 0)
				numPeaks_ = Integer.parseInt(col);

			// flight name
			else if (index == 1)
				flightName = col.startsWith("TF_") ? col : ("TF_" + col);

			// severity
			else if (index == 2)
				severity = col;

			// increment index
			index++;
		}

		// check severity character limits
		if (severity.length() > 500)
			severity = severity.substring(0, 100) + "... (truncated due to character limit)";

		// execute update
		addFlight.setInt(2, flightNumber_); // flight number
		addFlight.setString(3, flightName); // flight name
		addFlight.setString(4, severity); // severity
		addFlight.setInt(5, numPeaks_); // number of peaks
		addFlight.setDouble(6, validity); // validity
		addFlight.setDouble(7, blockSize); // block size
		addFlight.setDouble(8, 0.0); // max peak (0 for now)
		addFlight.setDouble(9, 0.0); // min peak (0 for now)
		addFlight.executeUpdate();

		// increment flight number
		flightNumber_++;

		// get result set
		try (ResultSet resultSet = addFlight.getGeneratedKeys()) {

			// return flight ID
			resultSet.next();
			return new ExternalFlight(flightName, resultSet.getBigDecimal(1).intValue());
		}
	}

	/**
	 * Sets number of flights to STH file table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param fileID
	 *            STH file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void setNumberOfFlights(Connection connection, int fileID) throws Exception {

		// update info
		task_.updateMessage("Saving number of flights to database...");

		// create query
		String sql = "update ext_sth_files set num_flights = ? where file_ID = " + fileID;

		// create statement
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			update.setInt(1, flightNumber_); // number of flights
			update.executeUpdate();
		}
	}

	/**
	 * Creates STH peaks table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param sequenceID
	 *            Stress sequence ID. This is used to generate unique table name.
	 * @return Name of newly created STH peaks table.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String createPeaksTable(Connection connection, int sequenceID) throws Exception {

		// update info
		task_.updateMessage("Creating stress peaks table...");

		// generate table and index names
		String tableName = "EXT_STH_PEAKS_" + sequenceID;
		String indexName = "EXT_STH_PEAK_" + sequenceID;

		// create statement
		try (Statement statement = connection.createStatement()) {

			// create table
			statement.executeUpdate("CREATE TABLE AURORA." + tableName + "(FLIGHT_ID INT NOT NULL, PEAK_NUM INT NOT NULL, PEAK_VAL DOUBLE NOT NULL)");

			// create index
			statement.executeUpdate("CREATE INDEX " + indexName + " ON AURORA." + tableName + "(FLIGHT_ID)");
		}

		// return table name
		return tableName;
	}

	/**
	 * Creates and returns external stress sequence in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @return The newly created external stress sequence.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ExternalStressSequence createStressSequence(Connection connection) throws Exception {

		// update info
		task_.updateMessage("Creating new stress sequence in database...");

		// create statement
		String sql = "insert into ext_sth_files(name, num_flights, ac_program, ac_section, fat_mission) values(?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// get name
			String name = FileType.getNameWithoutExtension(sthFile_);

			// set name
			update.setString(1, name);
			update.setInt(2, 0); // number of flights (0 for now)
			update.setString(3, "Not specified"); // A/C program
			update.setString(4, "Not specified"); // A/C section
			update.setString(5, "Not specified"); // Fatigue mission
			update.executeUpdate();

			// get result set
			try (ResultSet resultSet = update.getGeneratedKeys()) {

				// return file ID
				resultSet.next();
				ExternalStressSequence sequence = new ExternalStressSequence(name, resultSet.getBigDecimal(1).intValue());
				sequence.setProgram("Not specified");
				sequence.setSection("Not specified");
				sequence.setMission("Not specified");
				return sequence;
			}
		}
	}
}
