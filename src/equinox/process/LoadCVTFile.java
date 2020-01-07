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

/**
 * Class for loading CVT files.
 *
 * @author Murat Artim
 * @date Apr 25, 2014
 * @time 2:57:00 PM
 */
public class LoadCVTFile implements EquinoxProcess<Path> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** Input CVT file. */
	private final Path cvtFile_;

	/** CDF set. */
	private final Spectrum cdfSet_;

	/** Parameters. */
	private int allLines_;

	/**
	 * Creates load CVT file process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param cvtFile
	 *            Input CVT file.
	 * @param cdfSet
	 *            CDF set.
	 */
	public LoadCVTFile(TemporaryFileCreatingTask<?> task, Path cvtFile, Spectrum cdfSet) {
		task_ = task;
		cvtFile_ = cvtFile;
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
		return cvtFile_;
	}

	@Override
	public Path start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// initialize input file and type
		Path cvtFile = cvtFile_;
		FileType cvtType = FileType.getFileType(cvtFile_.toFile());

		// input file is a ZIP file
		if (cvtType.equals(FileType.ZIP)) {
			task_.updateMessage("Extracting zipped CVT file...");
			cvtFile = Utility.extractFileFromZIP(cvtFile_, task_, FileType.CVT, null);
		}

		// input file is a GZIP file
		else if (cvtType.equals(FileType.GZ)) {
			cvtFile = task_.getWorkingDirectory()
					.resolve(FileType.appendExtension(FileType.getNameWithoutExtension(cvtFile_), FileType.CVT));
			task_.updateMessage("Extracting zipped CVT file...");
			Utility.extractFileFromGZIP(cvtFile_, cvtFile);
		}

		// get number of lines of file
		task_.updateMessage("Getting CVT file size...");
		allLines_ = Utility.countLines(cvtFile, task_);

		// add file to files table and return file ID
		cdfSet_.setCVTFileID(addToFilesTable(connection, cvtFile));
		return cvtFile;
	}

	/**
	 * Adds input CVT file to files table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param inputFile
	 *            Input CVT file.
	 * @return File ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int addToFilesTable(Connection connection, Path inputFile) throws Exception {

		// get input file name
		Path inputFileName = inputFile.getFileName();
		if (inputFileName == null)
			throw new Exception("Cannot get input CVT file name.");

		// zip CVT file
		Path zipFile = task_.getWorkingDirectory().resolve(inputFileName.toString() + FileType.ZIP.getExtension());
		Utility.zipFile(inputFile, zipFile.toFile(), task_);

		// update info
		task_.updateMessage("Saving CVT file info to database...");

		// create query
		String sql = "insert into cvt_files(cdf_id, name, data, num_lines) values(?, ?, ?, ?)";

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
