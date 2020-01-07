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
 * Class for loading FLS files.
 *
 * @author Murat Artim
 * @date Apr 25, 2014
 * @time 2:15:31 PM
 */
public class LoadFLSFile implements EquinoxProcess<Integer> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** Input FLS file. */
	private final Path inputFile_;

	/** CDF set. */
	private final Spectrum cdfSet_;

	/** Parameters. */
	private int readLines_, allLines_;

	/** Update message header. */
	private String line_;

	/**
	 * Creates load FLS file process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input FLS file.
	 * @param cdfSet
	 *            CDF set.
	 */
	public LoadFLSFile(TemporaryFileCreatingTask<?> task, Path inputFile, Spectrum cdfSet) {
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
		Path flsFile = inputFile_;
		FileType type = FileType.getFileType(inputFile_.toFile());

		// input file is a ZIP file
		if (type.equals(FileType.ZIP)) {
			task_.updateMessage("Extracting zipped FLS file...");
			flsFile = Utility.extractFileFromZIP(inputFile_, task_, FileType.FLS, null);
		}

		// input file is a GZIP file
		else if (type.equals(FileType.GZ)) {
			flsFile = task_.getWorkingDirectory().resolve(FileType.appendExtension(FileType.getNameWithoutExtension(inputFile_), FileType.FLS));
			task_.updateMessage("Extracting zipped FLS file...");
			Utility.extractFileFromGZIP(inputFile_, flsFile);
		}

		// get number of lines of file
		task_.updateMessage("Getting FLS file size...");
		allLines_ = Utility.countLines(flsFile, task_);
		readLines_ = 0;
		boolean warningAdded = false;

		// add file to files table
		int fileID = addToFilesTable(connection, flsFile);

		// create file reader
		try (BufferedReader reader = Files.newBufferedReader(flsFile, Charset.defaultCharset())) {

			// create statement for inserting flights
			task_.updateMessage("Saving FLS flights to database...");
			String sql = "insert into fls_flights(file_id, flight_num, name, severity) values(?, ?, ?, ?)";
			try (PreparedStatement update = connection.prepareStatement(sql)) {

				// read file till the end
				String delimiter = null, name = null, severity = null;
				Integer flightNumber = null;
				while ((line_ = reader.readLine()) != null) {

					// task cancelled
					if (task_.isCancelled())
						return null;

					// increment read lines
					readLines_++;

					// update progress
					task_.updateProgress(readLines_, allLines_);

					// comment line
					if (line_.startsWith("#")) {
						continue;
					}

					// set column delimiter
					if (delimiter == null) {
						delimiter = line_.trim().contains("\t") ? "\t" : " ";
					}

					// split line
					String[] split = line_.trim().split(delimiter);

					// set file ID
					update.setInt(1, fileID);

					// loop over columns
					int index = 0;
					flightNumber = null;
					name = null;
					severity = null;
					for (String col : split) {

						// invalid value
						if ((col == null) || col.isEmpty()) {
							continue;
						}

						// trim spaces
						col = col.trim();

						// invalid value
						if (col.isEmpty()) {
							continue;
						}

						// flight number
						if (index == 0) {
							flightNumber = Integer.parseInt(col);
						}

						// flight name
						else if (index == 1) {
							name = col;
						}

						// severity
						else if (index == 2) {
							severity = col;
						}

						// increment index
						index++;
					}

					// null column encountered
					if ((flightNumber == null) || (name == null) || (severity == null)) {
						warningAdded = checkLine(flightNumber, name, severity, warningAdded, split, flsFile, update);
					}

					// no null column
					else {
						update.setInt(2, flightNumber);
						update.setString(3, name);
						update.setString(4, severity);
						update.executeUpdate();
					}
				}
			}
		}

		// return file ID
		return fileID;
	}

	/**
	 * Checks line whether flight severities are contained in the flight name column.
	 *
	 * @param flightNumber
	 *            Flight number.
	 * @param name
	 *            Flight name.
	 * @param severity
	 *            Flight severity.
	 * @param warningAdded
	 *            True warning is already added.
	 * @param split
	 *            Array for line splitting.
	 * @param flsFile
	 *            FLS file path.
	 * @param update
	 *            Database statement.
	 * @return True if unconventional format warning is already added.
	 * @throws Exception
	 *             If invalid format is encountered.
	 */
	private boolean checkLine(Integer flightNumber, String name, String severity, boolean warningAdded, String[] split, Path flsFile, PreparedStatement update) throws Exception {

		// add warning if not already added
		if (!warningAdded) {
			task_.addWarning("Unconventional file format encountered for the FLS file '" + flsFile.getFileName() + "'.");
			warningAdded = true;
		}

		// only severity is missing
		if ((flightNumber != null) && (name != null) && (severity == null)) {

			// check if severity is contained in flight name
			if (name.contains("s")) {
				name = name.replaceFirst("s", "s ");
				split = name.split(" ");
				name = split[0].trim();
				severity = split[1].trim();
			}
			else if (name.contains("t")) {
				name = name.replaceFirst("t", "t ");
				split = name.split(" ");
				name = split[0].trim();
				severity = split[1].trim();
			}
			else if (name.contains("p")) {
				name = name.replaceFirst("p", "p ");
				split = name.split(" ");
				name = split[0].trim();
				severity = split[1].trim();
			}

			// severity not found
			else
				throw new Exception("Unrecognized file format encountered for the FLS file '" + flsFile.getFileName() + "'. Severities not found.");
		}

		// invalid format
		else
			throw new Exception("Unrecognized file format encountered for the FLS file '" + flsFile.getFileName() + "'.");

		// execute update
		update.setInt(2, flightNumber);
		update.setString(3, name);
		update.setString(4, severity);
		update.executeUpdate();

		// return warning indicator
		return warningAdded;
	}

	/**
	 * Adds input FLS file to files table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param inputFile
	 *            Input FLS file.
	 * @return The file ID of the added file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int addToFilesTable(Connection connection, Path inputFile) throws Exception {

		// get input file name
		Path inputFileName = inputFile.getFileName();
		if (inputFileName == null)
			throw new Exception("Cannot get input FLS file name.");

		// zip FLS file
		Path zipFile = task_.getWorkingDirectory().resolve(inputFileName.toString() + FileType.ZIP.getExtension());
		Utility.zipFile(inputFile, zipFile.toFile(), task_);

		// update info
		task_.updateMessage("Saving FLS file info to database...");

		// create query
		String sql = "insert into fls_files(cdf_id, name, data, num_lines) values(?, ?, ?, ?)";

		// create statement
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// create input stream
			try (InputStream inputStream = Files.newInputStream(zipFile)) {

				// execute update
				update.setInt(1, cdfSet_.getID()); // CDF set ID
				update.setString(2, inputFileName.toString()); // file name
				update.setBlob(3, inputStream, zipFile.toFile().length());
				update.setInt(4, allLines_); // number of lines
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
}
