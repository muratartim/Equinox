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
import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import equinox.data.fileType.Spectrum;
import equinox.plugin.FileType;
import equinox.task.TemporaryFileCreatingTask;
import equinox.utility.Utility;

/**
 * Class for generate TXT file process.
 *
 * @author Murat Artim
 * @date Sep 24, 2014
 * @time 11:59:54 AM
 */
public class GenerateTXTFile implements EquinoxProcess<Integer> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** Input CVT file. */
	private final Path cvtFile_;

	/** Conversion table info. */
	private final Integer[] convTableInfo_;

	/** CDF set. */
	private final Spectrum cdfSet_;

	/**
	 * Creates load TXT file process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param cvtFile
	 *            Input CVT file.
	 * @param convTableInfo
	 *            Array containing conversion table ID and delta-p loadcase.
	 * @param cdfSet
	 *            CDF set.
	 */
	public GenerateTXTFile(TemporaryFileCreatingTask<?> task, Path cvtFile, Integer[] convTableInfo, Spectrum cdfSet) {
		task_ = task;
		cvtFile_ = cvtFile;
		convTableInfo_ = convTableInfo;
		cdfSet_ = cdfSet;
	}

	@Override
	public Integer start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// generate TXT file
		Path txtFile = generateTXTFile(connection, convTableInfo_[0]);

		// load and return TXT file
		return new LoadTXTFile(task_, txtFile, cdfSet_, convTableInfo_[1]).start(connection);
	}

	/**
	 * Reads through CVT file and generates TXT file by the use of the conversion table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param conversionTableID
	 *            Conversion table ID.
	 * @return Path to generated TXT file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path generateTXTFile(Connection connection, int conversionTableID) throws Exception {

		// create path to output TXT file
		Path txtFile = task_.getWorkingDirectory().resolve(FileType.getNameWithoutExtension(cvtFile_) + FileType.TXT.getExtension());

		// get number of lines of CVT file
		task_.updateMessage("Getting CVT file size...");
		int allLines = Utility.countLines(cvtFile_, task_);

		// prepare statement to get event names
		task_.updateMessage("Generating TXT file...");
		String sql = "select fue_translated from xls_comments where file_id = " + conversionTableID + " and issy_code = ?";
		try (PreparedStatement getEventName = connection.prepareStatement(sql)) {

			// create writer to write to TXT file
			try (BufferedWriter writer = Files.newBufferedWriter(txtFile, Charset.defaultCharset())) {

				// create reader to read CVT file
				try (BufferedReader reader = Files.newBufferedReader(cvtFile_, Charset.defaultCharset())) {

					// read file till the end
					String line = null;
					int readLines = 0;
					while ((line = reader.readLine()) != null) {

						// task cancelled
						if (task_.isCancelled())
							break;

						// increment read lines
						readLines++;

						// update progress
						task_.updateProgress(readLines, allLines);

						// comment line
						if (line.startsWith("#")) {
							writer.write(line);
							writer.newLine();
							continue;
						}

						// get ISSY code
						String[] split = line.split(" ");
						String issyCode = split[1].trim();

						// replace flight type with TXT formatting
						String flightType = String.format("%-7s", split[0].trim());
						line = line.replaceFirst(split[0].trim(), flightType);

						// get event name from conversion table
						String eventName = getEventName(getEventName, issyCode);

						// write out
						writer.write(eventName + line);
						writer.newLine();
					}
				}
			}
		}

		// return TXT file
		return txtFile;
	}

	/**
	 * Returns the event name for the given ISSY code.
	 *
	 * @param getEventName
	 *            Database statement.
	 * @param issyCode
	 *            ISSY code.
	 * @return The event name for the given ISSY code.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String getEventName(PreparedStatement getEventName, String issyCode) throws Exception {
		getEventName.setString(1, issyCode);
		try (ResultSet resultSet = getEventName.executeQuery()) {
			while (resultSet.next())
				return String.format("%-21s", resultSet.getString("fue_translated"));
		}
		throw new Exception("No event name could be found in the conversion table for ISSY code: " + issyCode);
	}
}
