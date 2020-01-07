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
import java.nio.file.StandardCopyOption;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.plugin.FileType;
import equinox.task.TemporaryFileCreatingTask;
import equinox.utility.Utility;

/**
 * Class for save output file process.
 *
 * @author Murat Artim
 * @date 21 Apr 2017
 * @time 14:24:07
 *
 */
public class SaveOutputFileProcess implements EquinoxProcess<Path> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** Spectrum item to save the output file for. */
	private final SpectrumItem item_;

	/** Output file. */
	private final Path output_;

	/**
	 * Creates save output file task.
	 *
	 * @param task
	 *            The owner task.
	 * @param item
	 *            Spectrum item to save the output file for.
	 * @param output
	 *            Output file. If directory is given, the default file name will be used.
	 */
	public SaveOutputFileProcess(TemporaryFileCreatingTask<?> task, SpectrumItem item, Path output) {
		task_ = task;
		item_ = item;
		output_ = output;
	}

	@Override
	public Path start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update progress info
		task_.updateMessage("Retrieving output file from database...");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// set table name
			String tableName = null;
			if (item_ instanceof FatigueEquivalentStress)
				tableName = "fatigue_equivalent_stresses";
			else if (item_ instanceof PreffasEquivalentStress)
				tableName = "preffas_equivalent_stresses";
			else if (item_ instanceof LinearEquivalentStress)
				tableName = "linear_equivalent_stresses";
			else if (item_ instanceof ExternalFatigueEquivalentStress)
				tableName = "ext_fatigue_equivalent_stresses";
			else if (item_ instanceof ExternalPreffasEquivalentStress)
				tableName = "ext_preffas_equivalent_stresses";
			else if (item_ instanceof ExternalLinearEquivalentStress)
				tableName = "ext_linear_equivalent_stresses";
			else if (item_ instanceof FastFatigueEquivalentStress)
				tableName = "fast_fatigue_equivalent_stresses";
			else if (item_ instanceof FastPreffasEquivalentStress)
				tableName = "fast_preffas_equivalent_stresses";
			else if (item_ instanceof FastLinearEquivalentStress)
				tableName = "fast_linear_equivalent_stresses";
			else
				return null;

			// output path is a directory (use default file name)
			if (Files.isDirectory(output_))
				return useDefaultFileName(statement, tableName);

			// output file supplied
			return useGivenFileName(statement, tableName);
		}
	}

	/**
	 * Uses default file name resolved against given directory path.
	 *
	 * @param statement
	 *            Database statement.
	 * @param tableName
	 *            Stress table name.
	 * @return Path to output file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path useDefaultFileName(Statement statement, String tableName) throws Exception {

		// initialize output path
		Path output = null;

		// execute query
		String sql = "select analysis_output_files.file_name, analysis_output_files.data from " + tableName + " inner join analysis_output_files on " + tableName + ".output_file_id = analysis_output_files.id ";
		sql += "where " + tableName + ".output_file_id is not null and " + tableName + ".id = " + item_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// get data
			if (resultSet.next()) {

				// get file name
				String fileName = resultSet.getString("file_name");
				output = output_.resolve(fileName);

				// get blob
				Blob blob = resultSet.getBlob("data");

				// copy BLOB data to file
				Path gzipFile = task_.getWorkingDirectory().resolve(fileName + FileType.GZ.getExtension());
				try (InputStream inputStream = blob.getBinaryStream()) {
					Files.copy(inputStream, gzipFile, StandardCopyOption.REPLACE_EXISTING);
				}

				// extract output file
				Utility.extractFileFromGZIP(gzipFile, output);

				// free blob
				blob.free();
			}

			// no output file
			else
				return null;
		}

		// return
		return output;
	}

	/**
	 * Uses given output file path.
	 *
	 * @param statement
	 *            Database statement.
	 * @param tableName
	 *            Stress table name.
	 * @return Path to output file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path useGivenFileName(Statement statement, String tableName) throws Exception {

		// execute query
		String sql = "select analysis_output_files.data from " + tableName + " inner join analysis_output_files on " + tableName + ".output_file_id = analysis_output_files.id ";
		sql += "where " + tableName + ".output_file_id is not null and " + tableName + ".id = " + item_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// get data
			if (resultSet.next()) {

				// get blob
				Blob blob = resultSet.getBlob("data");

				// copy BLOB data to file
				Path gzipFile = task_.getWorkingDirectory().resolve(output_.getFileName().toString() + FileType.GZ.getExtension());
				try (InputStream inputStream = blob.getBinaryStream()) {
					Files.copy(inputStream, gzipFile, StandardCopyOption.REPLACE_EXISTING);
				}

				// extract output file
				Utility.extractFileFromGZIP(gzipFile, output_);

				// free blob
				blob.free();
			}

			// no output file
			else
				return null;
		}

		// return
		return output_;
	}
}
