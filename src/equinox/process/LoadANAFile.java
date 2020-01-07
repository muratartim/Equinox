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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import equinox.data.fileType.Spectrum;
import equinox.plugin.FileType;
import equinox.task.TemporaryFileCreatingTask;
import equinox.utility.Utility;

/**
 * Class for load ANA file process.
 *
 * @author Murat Artim
 * @date Jan 21, 2014
 * @time 11:39:21 PM
 */
public class LoadANAFile implements EquinoxProcess<Integer> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** Input ANA file. */
	private final Path inputFile_;

	/** CDF set. */
	private final Spectrum cdfSet_;

	/** Parameters. */
	private int readLines_, allLines_, flightNumber_, numPeaks_;

	/** Update message header. */
	private String line_;

	/**
	 * Creates load ANA file process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input ANA file.
	 * @param cdfSet
	 *            CDF set.
	 */
	public LoadANAFile(TemporaryFileCreatingTask<?> task, Path inputFile, Spectrum cdfSet) {
		task_ = task;
		inputFile_ = inputFile;
		cdfSet_ = cdfSet;
	}

	/**
	 * Returns the CDF set of the process.
	 *
	 * @return The CDF set of the process.
	 */
	public Spectrum getCDFSet() {
		return cdfSet_;
	}

	/**
	 * Returns the input file of the process.
	 *
	 * @return The input file of the process.
	 */
	public Path getInputFile() {
		return inputFile_;
	}

	@Override
	public Integer start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// initialize input file and type
		Path anaFile = inputFile_;
		FileType type = FileType.getFileType(inputFile_.toFile());

		// input file is a ZIP file
		if (type.equals(FileType.ZIP)) {
			task_.updateMessage("Extracting zipped ANA file...");
			anaFile = Utility.extractFileFromZIP(inputFile_, task_, FileType.ANA, null);
		}

		// input file is a GZIP file
		else if (type.equals(FileType.GZ)) {
			anaFile = task_.getWorkingDirectory()
					.resolve(FileType.appendExtension(FileType.getNameWithoutExtension(inputFile_), FileType.ANA));
			task_.updateMessage("Extracting zipped ANA file...");
			Utility.extractFileFromGZIP(inputFile_, anaFile);
		}

		// get number of lines of file
		task_.updateMessage("Getting ANA file size...");
		allLines_ = Utility.countLines(anaFile, task_);
		readLines_ = 0;

		// add to files table
		int fileID = addToFilesTable(connection, anaFile);

		// create temporary ANA peaks table
		task_.updateMessage("Creating ANA peaks table...");
		String peaksTableName = createANAPeaksTable(connection, fileID);

		// prepare statement for adding flights
		String sql = "insert into ana_flights(file_id, flight_num, name, severity, num_peaks, validity, block_size, long_code, max_dp, min_dp, max_dt, min_dt) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement addFlight = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			addFlight.setInt(1, fileID); // file ID

			// prepare statement for adding peaks
			sql = "insert into " + peaksTableName;
			sql += "(flight_id, peak_num, four_digit_code, fourteen_digit_code, delta_p, delta_t) values(?, ?, ?, ?, ?, ?)";
			try (PreparedStatement addPeaks = connection.prepareStatement(sql)) {

				// prepare statement for updating flight
				sql = "update ana_flights set max_dp = ?, min_dp = ?, max_dt = ?, min_dt = ? where file_ID = " + fileID
						+ " and flight_id = ?";
				try (PreparedStatement updateFlight = connection.prepareStatement(sql)) {

					// create file reader
					try (BufferedReader reader = Files.newBufferedReader(anaFile, Charset.defaultCharset())) {

						// read file till the end
						while ((line_ = reader.readLine()) != null) {

							// task cancelled
							if (task_.isCancelled())
								return null;

							// increment read lines
							readLines_++;

							// update progress
							task_.updateProgress(readLines_, allLines_);

							// comment line
							if (line_.startsWith("#"))
								continue;

							// add flight to flights table
							int flightID = addToFlightsTable(reader, addFlight);

							// add peaks to peaks table
							addToPeaksTable(reader, flightID, addPeaks, updateFlight);
						}
					}
				}
			}
		}

		// task cancelled
		if (task_.isCancelled())
			return null;

		// set number of flights
		setNumberOfFlights(connection, fileID);

		// return
		return fileID;
	}

	/**
	 * Creates ANA peaks table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param fileID
	 *            ANA file ID. This is used to generate unique table name.
	 * @return Name of newly created ANA peaks table.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String createANAPeaksTable(Connection connection, int fileID) throws Exception {

		// generate temporary table and index names
		String tableName = "ANA_PEAKS_" + fileID;
		String indexName = "ANA_PEAK_" + fileID;

		// create statement
		try (Statement statement = connection.createStatement()) {

			// create table
			statement.executeUpdate("CREATE TABLE AURORA." + tableName
					+ "(FLIGHT_ID INT NOT NULL, PEAK_NUM INT NOT NULL, FOUR_DIGIT_CODE VARCHAR(4) NOT NULL, FOURTEEN_DIGIT_CODE VARCHAR(14) NOT NULL, DELTA_P DOUBLE NOT NULL, DELTA_T DOUBLE NOT NULL)");

			// create index
			statement.executeUpdate("CREATE INDEX " + indexName + " ON AURORA." + tableName + "(FLIGHT_ID)");
		}

		// return table name
		return tableName;
	}

	/**
	 * Adds input ANA file to files table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param inputFile
	 *            Input ANA file.
	 * @return The file ID of the added file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int addToFilesTable(Connection connection, Path inputFile) throws Exception {

		// get input file name
		Path inputFileName = inputFile.getFileName();
		if (inputFileName == null)
			throw new Exception("Cannot get input ANA file name.");

		// zip ANA file
		Path zipFile = task_.getWorkingDirectory().resolve(inputFileName.toString() + FileType.ZIP.getExtension());
		Utility.zipFile(inputFile, zipFile.toFile(), task_);

		// update info
		task_.updateMessage("Saving ANA file info to database...");

		// create query
		String sql = "insert into ana_files(cdf_id, name, num_flights, data, num_lines) values(?, ?, ?, ?, ?)";

		// create statement
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// create input stream
			try (InputStream inputStream = Files.newInputStream(zipFile)) {

				// execute update
				update.setInt(1, cdfSet_.getID()); // CDF set ID
				update.setString(2, inputFileName.toString()); // file name
				update.setInt(3, 0); // number of flights (0 for now)
				update.setBlob(4, inputStream, zipFile.toFile().length());
				update.setInt(5, allLines_); // number of lines
				update.executeUpdate();
			}

			// get result set
			try (ResultSet resultSet = update.getGeneratedKeys()) {

				// return file ID
				resultSet.next();
				return resultSet.getBigDecimal(1).intValue();
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
	 * @return The flight ID of the added flight.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int addToFlightsTable(BufferedReader reader, PreparedStatement addFlight) throws Exception {

		// update info
		task_.updateMessage("Saving ANA flight info for flight " + flightNumber_ + " to database...");

		// initialize variables
		String flightName = null, severity = "", longCode = "";
		double validity = 0.0, blockSize = 0.0;

		// split line
		String[] split = line_.trim().split(" ");

		// loop over columns
		int index = 0;
		for (String col : split) {

			// invalid value
			if ((col == null) || col.isEmpty())
				continue;

			// trim spaces
			col = col.trim();

			// invalid value
			if (col.isEmpty())
				continue;

			// flight name
			if (index == 0)
				flightName = col;

			// severity
			else if (index == 1)
				severity = col;

			// long code
			else if (index == 2)
				longCode = col.substring(1, col.length() - 2);

			// increment index
			index++;
		}

		// read next line
		line_ = reader.readLine();
		readLines_++;
		task_.updateProgress(readLines_, allLines_);

		// null line
		if (line_ == null)
			throw new Exception("Null line encountered during reading ANA file.");

		// set number of peaks
		numPeaks_ = Integer.parseInt(line_.trim());

		// read next line
		line_ = reader.readLine();
		readLines_++;
		task_.updateProgress(readLines_, allLines_);

		// null line
		if (line_ == null)
			throw new Exception("Null line encountered during reading ANA file.");

		// split line
		split = line_.trim().split(" ");

		// loop over columns
		index = 0;
		for (String col : split) {

			// invalid value
			if ((col == null) || col.isEmpty())
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

		// check severity and long code character limits
		if (severity.length() > 500)
			severity = severity.substring(0, 100) + "... (truncated due to character limit)";
		if (longCode.length() > 500)
			longCode = longCode.substring(0, 100) + "... (truncated due to character limit)";

		// execute update
		addFlight.setInt(2, flightNumber_); // flight number
		addFlight.setString(3, flightName); // flight name
		addFlight.setString(4, severity); // severity
		addFlight.setInt(5, numPeaks_); // number of peaks
		addFlight.setDouble(6, validity); // validity
		addFlight.setDouble(7, blockSize); // block size
		addFlight.setString(8, longCode); // long code
		addFlight.setDouble(9, 0.0); // max DP (0 for now)
		addFlight.setDouble(10, 0.0); // min DP (0 for now)
		addFlight.setDouble(11, 0.0); // max DT (0 for now)
		addFlight.setDouble(12, 0.0); // min DT (0 for now)
		addFlight.executeUpdate();

		// increment flight number
		flightNumber_++;

		// get result set
		try (ResultSet resultSet = addFlight.getGeneratedKeys()) {

			// return flight ID
			resultSet.next();
			return resultSet.getBigDecimal(1).intValue();
		}
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
	private void addToPeaksTable(BufferedReader reader, int flightID, PreparedStatement addPeaks,
			PreparedStatement updateFlight) throws Exception {

		// update info
		task_.updateMessage("Saving ANA peaks for flight " + (flightNumber_ - 1) + " to database...");

		// initialize number of read peaks
		int readPeaks = 0;
		double maxDP = Double.NEGATIVE_INFINITY;
		double minDP = Double.POSITIVE_INFINITY;
		double maxDT = Double.NEGATIVE_INFINITY;
		double minDT = Double.POSITIVE_INFINITY;
		double dp = 0.0, dt = 0.0;

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

			// set first 2 columns
			addPeaks.setInt(1, flightID); // flight ID
			addPeaks.setInt(2, readPeaks); // peak number

			// loop over columns
			int index = 0;
			for (String col : split) {

				// invalid value
				if ((col == null) || col.isEmpty())
					continue;

				// trim spaces
				col = col.trim();

				// invalid value
				if (col.isEmpty())
					continue;

				// 4 digit code
				if (index == 0)
					addPeaks.setString(3, col);

				// 14 digit code
				else if (index == 1)
					addPeaks.setString(4, col);

				// delta P
				else if (index == 2) {
					dp = Double.parseDouble(col);
					addPeaks.setDouble(5, dp);
				}

				// delta T
				else if (index == 3) {
					dt = Double.parseDouble(col);
					addPeaks.setDouble(6, dt);
				}

				// increment index
				index++;
			}

			// execute update
			addPeaks.executeUpdate();

			// update max-min values
			if (dp >= maxDP)
				maxDP = dp;
			if (dp <= minDP)
				minDP = dp;
			if (dt >= maxDT)
				maxDT = dt;
			if (dt <= minDT)
				minDT = dt;

			// increment read peaks
			readPeaks++;

			// all peaks read
			if (readPeaks == numPeaks_) {

				// update max-min values of the flight
				updateFlight.setDouble(1, maxDP); // max DP
				updateFlight.setDouble(2, minDP); // min DP
				updateFlight.setDouble(3, maxDT); // max DT
				updateFlight.setDouble(4, minDT); // min DT
				updateFlight.setDouble(5, flightID); // flight ID
				updateFlight.executeUpdate();

				// return
				return;
			}
		}
	}

	/**
	 * Sets number of flights to ANA file table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param fileID
	 *            ANA file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void setNumberOfFlights(Connection connection, int fileID) throws Exception {

		// update info
		task_.updateMessage("Saving ANA number of flights to database...");

		// create query
		String sql = "update ana_files set num_flights = ? where file_ID = " + fileID;

		// create statement
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			update.setInt(1, flightNumber_); // number of flights
			update.executeUpdate();
		}
	}
}
