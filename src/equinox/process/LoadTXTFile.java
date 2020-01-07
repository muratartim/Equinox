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
import java.util.ArrayList;

import equinox.data.NonlinearLC;
import equinox.data.fileType.Spectrum;
import equinox.plugin.FileType;
import equinox.task.TemporaryFileCreatingTask;
import equinox.utility.Utility;

/**
 * Class for load TXT file process.
 *
 * @author Murat Artim
 * @date Feb 6, 2014
 * @time 2:29:11 PM
 */
public class LoadTXTFile implements EquinoxProcess<Integer> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** Input TXT file. */
	private final Path txtFile_;

	/** CDF set. */
	private final Spectrum cdfSet_;

	/** Delta-p loadcase. */
	private final Integer dpLoadcase_;

	/** Parameters. */
	private int readLines_, allLines_, onegOrder_;

	/** Update message header. */
	private String line_;

	/**
	 * Creates load TXT file process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param txtFile
	 *            Input TXT file.
	 * @param cdfSet
	 *            CDF set.
	 * @param dpLoadcase
	 *            Delta-p loadcase. This can be null (in case it could not be found in conversion table).
	 */
	public LoadTXTFile(TemporaryFileCreatingTask<?> task, Path txtFile, Spectrum cdfSet, Integer dpLoadcase) {
		task_ = task;
		txtFile_ = txtFile;
		cdfSet_ = cdfSet;
		dpLoadcase_ = dpLoadcase;
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
		return txtFile_;
	}

	@Override
	public Integer start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// initialize input file and type
		Path txtFile = txtFile_;
		FileType type = FileType.getFileType(txtFile_.toFile());

		// input file is a ZIP file
		if (type.equals(FileType.ZIP)) {
			task_.updateMessage("Extracting zipped TXT file...");
			txtFile = Utility.extractFileFromZIP(txtFile_, task_, FileType.TXT, null);
		}

		// input file is a GZIP file
		else if (type.equals(FileType.GZ)) {
			txtFile = task_.getWorkingDirectory().resolve(FileType.appendExtension(FileType.getNameWithoutExtension(txtFile_), FileType.TXT));
			task_.updateMessage("Extracting zipped TXT file...");
			Utility.extractFileFromGZIP(txtFile_, txtFile);
		}

		// get number of lines of file
		task_.updateMessage("Getting TXT file size...");
		allLines_ = Utility.countLines(txtFile, task_);
		readLines_ = 0;
		onegOrder_ = 0;

		// add to files table
		int fileID = addToFilesTable(connection, txtFile);

		// add codes to codes table
		addToCodesTable(connection, fileID, txtFile);

		// set non linear load case factors
		setNonlinearLCFactors(connection, fileID);

		// return file ID
		return fileID;
	}

	/**
	 * Sets nonlinear load case factors.
	 *
	 * @param connection
	 *            Database connection.
	 * @param fileID
	 *            TXT file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void setNonlinearLCFactors(Connection connection, int fileID) throws Exception {

		// update info
		task_.updateMessage("Setting nonlinear load case factors...");

		// get nonlinear load cases
		ArrayList<NonlinearLC> nlCases = new ArrayList<>();
		String sql = "select one_g_code, direction_num, factor_1, factor_2, factor_3, factor_4, factor_5, factor_6, factor_7, factor_8, nl_factor_num from txt_codes where file_id = " + fileID + " and nl_factor_num is not null";
		try (Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					String facNum = resultSet.getString("nl_factor_num");
					nlCases.add(new NonlinearLC(resultSet.getString("one_g_code"), resultSet.getString("direction_num"), resultSet.getDouble("factor_" + facNum), facNum));
				}
			}
		}

		// prepare statement for update load case factors
		ArrayList<String> dirs = new ArrayList<>();
		sql = "update txt_codes set factor_1 = ?, factor_2 = ?, factor_3 = ?, factor_4 = ?, factor_5 = ?, factor_6 = ?, factor_7 = ?, factor_8 = ? where file_ID = " + fileID + " and nl_factor_num = ? and one_g_code = ? and direction_num = '2'";
		try (PreparedStatement update = connection.prepareStatement(sql)) {

			// loop over nonlinear load cases
			for (int i = 0; i < nlCases.size(); i++) {

				// get case
				NonlinearLC lc = nlCases.get(i);

				// second direction
				if (lc.getDirectionNumber().equals("2")) {

					// count number of directions
					dirs.clear();
					for (NonlinearLC lc2 : nlCases)
						if (lc2.get1gCode().equals(lc.get1gCode()) && !dirs.contains(lc2.getDirectionNumber())) {
							dirs.add(lc2.getDirectionNumber());
						}

					// only 2 directions
					if (dirs.size() == 2) {
						for (int j = 1; j < 9; j++) {
							update.setNull(j, java.sql.Types.DOUBLE);
						}
						update.setDouble(Integer.parseInt(lc.getFactorNumber()), lc.getFactorValue() * -1); // factor
																											// value
						update.setString(9, lc.getFactorNumber()); // factor
																	// number
						update.setString(10, lc.get1gCode()); // 1g code
						update.executeUpdate();
					}
				}
			}
		}
	}

	/**
	 * Adds input TXT file to files table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param inputFile
	 *            Input TXT file.
	 * @return The file ID of the added file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int addToFilesTable(Connection connection, Path inputFile) throws Exception {

		// get input file name
		Path inputFileName = inputFile.getFileName();
		if (inputFileName == null)
			throw new Exception("Cannot get input TXT file name.");

		// zip TXT file
		Path zipFile = task_.getWorkingDirectory().resolve(inputFileName.toString() + FileType.ZIP.getExtension());
		Utility.zipFile(inputFile, zipFile.toFile(), task_);

		// update info
		task_.updateMessage("Saving TXT file info to database...");

		// create query
		String sql = "insert into txt_files(cdf_id, name, data, num_lines) values(?, ?, ?, ?)";

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

	/**
	 * Adds TXT codes to codes table.
	 *
	 * @param connection
	 *            database connection.
	 * @param fileID
	 *            TXT file ID.
	 * @param inputFile
	 *            Input TXT file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void addToCodesTable(Connection connection, int fileID, Path inputFile) throws Exception {

		// update info
		task_.updateMessage("Saving TXT file codes to database...");

		// create query
		String sql = "insert into txt_codes(file_id, dp_case, flight_phase, one_g_code, increment_num, issy_code, direction_num, factor_1, factor_2, factor_3, factor_4, factor_5, factor_6, factor_7, factor_8, nl_factor_num, oneg_order) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		// create statement
		try (PreparedStatement update = connection.prepareStatement(sql)) {

			// create file reader
			try (BufferedReader reader = Files.newBufferedReader(inputFile, Charset.defaultCharset())) {

				// create array list to store TXT codes
				ArrayList<String> codes = new ArrayList<>();

				// read file till the end
				while ((line_ = reader.readLine()) != null) {

					// task cancelled
					if (task_.isCancelled()) {
						break;
					}

					// increment read lines
					readLines_++;

					// update progress
					task_.updateProgress(readLines_, allLines_);

					// comment line
					if (line_.startsWith("#")) {
						continue;
					}

					// split line
					String event = line_.substring(0, 21).trim();
					String[] split = line_.substring(21, line_.length()).trim().split(" ");
					readSplitColumns(fileID, event, split, update, codes);
				}
			}
		}
	}

	/**
	 * Reads split columns.
	 *
	 * @param fileID
	 *            File ID.
	 * @param event
	 *            Event name.
	 * @param split
	 *            Array containing the split columns.
	 * @param update
	 *            Database statement.
	 * @param codes
	 *            Array list containing the codes. This is used to obtain the direction numbers.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void readSplitColumns(int fileID, String event, String[] split, PreparedStatement update, ArrayList<String> codes) throws Exception {

		// set constant parameters
		update.setInt(1, fileID);
		update.setString(3, event);

		// set null to all factors
		for (int i = 8; i < 16; i++) {
			update.setNull(i, java.sql.Types.DOUBLE);
		}

		int nlFactorNumber = -1, incrementNumber = -1;
		int index = 0;
		boolean isDP;
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

			// 1g code and increment number
			if (index == 0) {

				// add to codes
				codes.add(col);

				// set 1g code
				update.setString(4, col.substring(0, 4));

				// set increment number
				incrementNumber = Integer.parseInt(col.substring(4, 5));
				update.setInt(5, incrementNumber);

				// linear load case
				if (col.length() == 5) {

					// set direction number
					update.setString(7, "" + getDirectionNumber(codes, col));

					// set non-linear factor number
					nlFactorNumber = -1;
					update.setNull(16, java.sql.Types.VARCHAR);
				}

				// non-linear load case
				else if (col.length() == 7) {

					// set direction number
					update.setString(7, "" + Integer.parseInt(col.substring(6, 7)));

					// set non-linear factor number
					nlFactorNumber = Integer.parseInt(col.substring(5, 6));
					update.setString(16, "" + nlFactorNumber);
				}
			}

			// issy code
			else if (index == 1) {

				// set issy code
				update.setString(6, col);

				// set if delta-p loadcase
				isDP = (dpLoadcase_ != null) && col.equals(dpLoadcase_.toString());
				update.setBoolean(2, isDP);

				// set 1g order
				if ((incrementNumber == 0) && !isDP) {
					update.setInt(17, onegOrder_);
					onegOrder_++;
				}
				else {
					update.setNull(17, java.sql.Types.INTEGER);
				}
			}

			// factors
			else if ((index >= 2) && (index <= 9))
				// non-linear case
				if (nlFactorNumber != -1) {
					update.setDouble(nlFactorNumber + 7, Double.parseDouble(col));
				}
				else {
					update.setDouble(index + 6, Double.parseDouble(col));
				}

			// increment index
			index++;
		}

		// add to database
		update.executeUpdate();
	}

	/**
	 * Returns the direction number of the given TXT code by counting the number of same codes in the given list.
	 *
	 * @param codes
	 *            Array list containing the codes.
	 * @param col
	 *            Code to get the direction number.
	 * @return The direction number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static int getDirectionNumber(ArrayList<String> codes, String col) throws Exception {
		int num = 0;
		for (String code : codes)
			if (code.equals(col)) {
				num++;
			}
		return num;
	}
}
