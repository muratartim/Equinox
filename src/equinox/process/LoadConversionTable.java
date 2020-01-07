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

import java.io.InputStream;
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
import jxl.Sheet;
import jxl.Workbook;

/**
 * Class for load conversion table task.
 *
 * @author Murat Artim
 * @date Feb 20, 2014
 * @time 11:52:37 PM
 */
public class LoadConversionTable implements EquinoxProcess<Integer[]> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** Input conversion table file. */
	private final Path inputFile_;

	/** CDF set. */
	private final Spectrum cdfSet_;

	/** Conversion table sheet name. */
	private final String sheet_;

	/**
	 * Creates load conversion table process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input conversion table.
	 * @param cdfSet
	 *            CDF set.
	 * @param sheet
	 *            Sheet name to extract the data.
	 */
	public LoadConversionTable(TemporaryFileCreatingTask<?> task, Path inputFile, Spectrum cdfSet, String sheet) {
		task_ = task;
		inputFile_ = inputFile;
		cdfSet_ = cdfSet;
		sheet_ = sheet;
	}

	@Override
	public Integer[] start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// initialize input file and type
		Path excelFile = inputFile_;
		FileType type = FileType.getFileType(inputFile_.toFile());

		// get excel file name
		Path excelFileName = excelFile.getFileName();
		if (excelFileName == null)
			throw new Exception("Cannot get conversion table file name.");

		// input file is a ZIP file
		if (type.equals(FileType.ZIP)) {
			task_.updateMessage("Extracting zipped Conversion table...");
			excelFile = Utility.extractFileFromZIP(inputFile_, task_, FileType.XLS, null);
		}

		// input file is a GZIP file
		else if (type.equals(FileType.GZ)) {
			excelFile = task_.getWorkingDirectory()
					.resolve(FileType.appendExtension(FileType.getNameWithoutExtension(inputFile_), FileType.XLS));
			task_.updateMessage("Extracting zipped Conversion table...");
			Utility.extractFileFromGZIP(inputFile_, excelFile);
		}

		// update info
		task_.updateMessage("Reading conversion table...");

		// initialize variables
		Workbook workbook = null;

		try {

			// get workbook
			workbook = Workbook.getWorkbook(excelFile.toFile());

			// get sheet
			Sheet sheet = sheet_ == null ? workbook.getSheet(0) : workbook.getSheet(sheet_);

			// null sheet
			if (sheet == null)
				throw new Exception("Cannot find worksheet '" + sheet_ + "' in conversion table excel file '"
						+ excelFileName.toString() + "'.");

			// add to files table
			Integer[] output = addToFilesTable(connection, sheet, excelFile);

			// add to comments table
			addToCommentsTable(connection, output[0], sheet);

			// task cancelled
			if (task_.isCancelled())
				return null;

			// update CDF set info
			updateCDFInfo(connection, sheet, excelFileName.toString());

			// return output array
			return output;
		}

		// close workbook
		finally {
			if (workbook != null)
				workbook.close();
		}
	}

	/**
	 * Adds file info to files table in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param sheet
	 *            Worksheet.
	 * @param inputFile
	 *            Input conversion table.
	 * @return An array containing the file ID and delta-p loadcase. Note that,
	 *         the delta-p loadcase may be null if it could not be found in the
	 *         conversion table.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Integer[] addToFilesTable(Connection connection, Sheet sheet, Path inputFile) throws Exception {

		// get input file name
		Path inputFileName = inputFile.getFileName();
		if (inputFileName == null)
			throw new Exception("Cannot get input conversion table file name.");

		// initialize output array
		Integer[] output = new Integer[2];

		// zip conversion file
		Path zipFile = task_.getWorkingDirectory().resolve(inputFileName.toString() + FileType.ZIP.getExtension());
		Utility.zipFile(inputFile, zipFile.toFile(), task_);

		// update info
		task_.updateMessage("Saving Conversion table file info to database...");

		// get reference delta-p
		Double[] dpInfo = getDPInfo(sheet, inputFileName.toString());

		// set delta-p loadcase
		output[1] = dpInfo[1] == null ? null : dpInfo[1].intValue();

		// create query
		String sql = "insert into xls_files(cdf_id, name, mission, data, ref_dp) values(?, ?, ?, ?, ?)";

		// create statement
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// create input stream
			try (InputStream inputStream = Files.newInputStream(zipFile)) {

				// execute update
				update.setInt(1, cdfSet_.getID()); // CDF set ID
				update.setString(2, inputFileName.toString()); // file name
				update.setString(3, sheet_); // mission
				update.setBlob(4, inputStream, zipFile.toFile().length());
				if (dpInfo[0] == null)
					update.setNull(5, java.sql.Types.DOUBLE);
				else
					update.setDouble(5, dpInfo[0]);
				update.executeUpdate();
			}

			// get result set
			try (ResultSet resultSet = update.getGeneratedKeys()) {

				// return file ID
				resultSet.next();
				output[0] = resultSet.getBigDecimal(1).intValue();
			}
		}

		// return output
		return output;
	}

	/**
	 * Searches and returns an array containing reference delta-p value and
	 * delta-p loadcase from the conversion table. Note that, the values will be
	 * null if could not be found.
	 *
	 * @param sheet
	 *            Conversion table sheet.
	 * @param fileName
	 *            Conversion table file name.
	 * @return An array containing reference delta-p value and delta-p loadcase.
	 */
	private Double[] getDPInfo(Sheet sheet, String fileName) {

		// initialize info
		Double[] dpInfo = new Double[2];

		// get delta-p row number
		int dpRow = -1;
		for (int i = 0; i <= (sheet.getRows() - 1); i++) {

			// get first column content
			String firstCol = sheet.getCell(0, i).getContents();

			// delta-p row
			if ((firstCol != null) && firstCol.trim().contains("Pressure")) {
				dpRow = i;
				break;
			}
		}

		// delta-p row could not be found
		if (dpRow == -1) {
			task_.addWarning("Delta-P row could not be found in conversion table '" + fileName + "'");
			return dpInfo;
		}

		// get reference delta-p
		String refDP = sheet.getCell(1, dpRow).getContents();

		// empty cell
		if ((refDP == null) || refDP.trim().isEmpty())
			task_.addWarning("Reference Delta-P value could not be found in conversion table '" + fileName + "'");
		else // contains unit
		if (refDP.contains("mbar")) {
			String[] split = refDP.split("mbar");
			try {
				dpInfo[0] = Double.parseDouble(split[0].trim());
			}

			// cannot parse reference delta-p
			catch (NumberFormatException e) {
				task_.addWarning("Reference Delta-P value could not be parsed in conversion table '" + fileName + "'");
			}
		} else
			try {
				dpInfo[0] = Double.parseDouble(refDP.trim());
			}

			// cannot parse reference delta-p
			catch (NumberFormatException e) {
				task_.addWarning("Reference Delta-P value could not be parsed in conversion table '" + fileName + "'");
			}

		// get delta-p loadcase
		String dpLoadcase = sheet.getCell(3, dpRow).getContents();

		// empty cell
		if ((dpLoadcase == null) || dpLoadcase.trim().isEmpty())
			task_.addWarning("Delta-P loadcase could not be found in conversion table '" + fileName + "'");
		else
			try {
				dpInfo[1] = (double) Integer.parseInt(dpLoadcase.trim());
			}

			// cannot parse reference delta-p
			catch (NumberFormatException e) {
				task_.addWarning("Delta-P loadcase could not be parsed in conversion table '" + fileName + "'");
			}

		// return delta-p info
		return dpInfo;
	}

	/**
	 * Adds conversion table comments to comments able.
	 *
	 * @param connection
	 *            Database connection.
	 * @param fileID
	 *            Conversion table file ID.
	 * @param sheet
	 *            Worksheet.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void addToCommentsTable(Connection connection, int fileID, Sheet sheet) throws Exception {

		// update info
		task_.updateMessage("Saving Conversion table data to database...");

		// create query
		String sql = "insert into xls_comments(file_id, ref_intensity, issy_code, flight_type, fue_translated, comment) values(?, ?, ?, ?, ?, ?)";

		// create statement
		try (PreparedStatement update = connection.prepareStatement(sql)) {

			// set location
			int startRow = 7, endRow = sheet.getRows() - 1;

			// loop over rows
			for (int i = startRow; i <= endRow; i++) {

				// task cancelled
				if (task_.isCancelled())
					break;

				// get issy code
				String issyCode = sheet.getCell(3, i).getContents();

				// no issy code found
				if ((issyCode == null) || issyCode.isEmpty())
					continue;

				// get other content
				String flightType = sheet.getCell(4, i).getContents();
				String fueTranslated = sheet.getCell(5, i).getContents();
				String comment = sheet.getCell(6, i).getContents();
				String refIntensity = sheet.getCell(1, i).getContents();

				// execute update
				update.setInt(1, fileID); // file ID
				if ((refIntensity == null) || refIntensity.isEmpty()) // reference
																		// intensity
					update.setNull(2, java.sql.Types.VARCHAR);
				else
					update.setString(2, refIntensity);
				update.setString(3, issyCode); // issy code
				if ((flightType == null) || flightType.isEmpty()) // flight type
					update.setNull(4, java.sql.Types.VARCHAR);
				else
					update.setString(4, flightType);
				if ((fueTranslated == null) || fueTranslated.isEmpty()) // fue
																		// translated
					update.setNull(5, java.sql.Types.VARCHAR);
				else
					update.setString(5, fueTranslated);
				if ((comment == null) || comment.isEmpty()) // comment
					update.setNull(6, java.sql.Types.VARCHAR);
				else
					update.setString(6, comment);
				update.executeUpdate();
			}
		}
	}

	/**
	 * Updates CDF set info according to conversion table contents.
	 *
	 * @param connection
	 *            Database connection.
	 * @param sheet
	 *            Worksheet.
	 * @param fileName
	 *            Conversion table file name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void updateCDFInfo(Connection connection, Sheet sheet, String fileName) throws Exception {

		// update info
		task_.updateMessage("Updating CDF set info according to conversion table contents...");

		// create query
		String sql = "update cdf_sets set ac_program = ?, ac_section = ?, fat_mission = ?, fat_mission_issue = ?, flp_issue = ?, iflp_issue = ?, cdf_issue = ? where set_id = "
				+ cdfSet_.getID();

		// create statement
		try (PreparedStatement update = connection.prepareStatement(sql)) {

			// A/C program
			String acProgram = sheet.getCell(1, 1).getContents();
			if ((acProgram == null) || acProgram.isEmpty()) {
				task_.addWarning(
						"No A/C Program information found at cell B2 (or R2C2) in Conversion Table '" + fileName + "'");
				acProgram = "Not specified";
			}
			update.setString(1, acProgram);
			cdfSet_.setProgram(acProgram);

			// A/C section
			String acSection = sheet.getCell(6, 3).getContents();
			if ((acSection == null) || acSection.isEmpty()) {
				task_.addWarning(
						"No A/C Section information found at cell G4 (or R4C7) in Conversion Table '" + fileName + "'");
				acSection = "Not specified";
			}
			update.setString(2, acSection);
			cdfSet_.setSection(acSection);

			// fatigue mission
			update.setString(3, sheet_);
			cdfSet_.setMission(sheet_);

			// fatigue mission issue
			update.setString(4, sheet.getCell(6, 2).getContents().trim());

			// FLP issue
			update.setString(5, sheet.getCell(1, 2).getContents().trim());

			// IFLP issue
			update.setString(6, sheet.getCell(1, 3).getContents().trim());

			// CDF issue
			update.setString(7, sheet.getCell(1, 4).getContents().trim());

			// execute update
			update.executeUpdate();
		}
	}
}
