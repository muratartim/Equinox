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

import equinox.data.fileType.STFFileBucket;
import equinox.plugin.FileType;
import equinox.task.TemporaryFileCreatingTask;
import equinox.utility.Utility;

/**
 * Class for save bucket analysis output files process.
 *
 * @author Murat Artim
 * @date 27 Apr 2017
 * @time 11:43:03
 *
 */
public class SaveBucketOutputFilesProcess implements EquinoxProcess<Void> {

	/** Bucket equivalent stress type. */
	public static final int FATIGUE = 0, PREFFAS = 1, LINEAR = 2;

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** File item to save. */
	private final STFFileBucket[] buckets_;

	/** Output directory. */
	private final Path outputDir_;

	/** Equivalent stress type. */
	private final int stressType_;

	/**
	 * Creates save STF file bucket process.
	 *
	 * @param task
	 *            The owner task.
	 * @param outputDir
	 *            Output directory.
	 * @param stressType
	 *            Equivalent stress type.
	 * @param buckets
	 *            File item to save.
	 */
	public SaveBucketOutputFilesProcess(TemporaryFileCreatingTask<?> task, Path outputDir, int stressType, STFFileBucket... buckets) {
		task_ = task;
		buckets_ = buckets;
		outputDir_ = outputDir;
		stressType_ = stressType;
	}

	@Override
	public Void start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update info
		task_.updateMessage("Saving analysis output files...");

		// set table names
		String outputTable = "analysis_output_files", stfTable = "stf_files", stressTable = null;
		if (stressType_ == FATIGUE) {
			stressTable = "fast_fatigue_equivalent_stresses";
		}
		else if (stressType_ == PREFFAS) {
			stressTable = "fast_preffas_equivalent_stresses";
		}
		else if (stressType_ == LINEAR) {
			stressTable = "fast_linear_equivalent_stresses";
		}

		// prepare statement
		String sql = "select " + outputTable + ".file_name, " + outputTable + ".data from " + stressTable + " inner join " + outputTable + " on " + stressTable + ".output_file_id = " + outputTable + ".id ";
		sql += "where " + stressTable + ".output_file_id is not null and " + stressTable + ".stf_id in (select " + stfTable + ".file_id from " + stfTable + " where " + stfTable + ".cdf_id = ?)";
		try (PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

			// loop over buckets
			for (STFFileBucket bucket : buckets_) {

				// task cancelled
				if (task_.isCancelled())
					return null;

				// execute query
				statement.setInt(1, bucket.getID());
				try (ResultSet resultSet = statement.executeQuery()) {

					// move to last row
					if (resultSet.last()) {

						// get number of files
						int numFiles = resultSet.getRow();

						// move to beginning
						resultSet.beforeFirst();

						// loop over results
						int fileCount = 0;
						while (resultSet.next()) {

							// task cancelled
							if (task_.isCancelled())
								return null;

							// update progress
							task_.updateProgress(fileCount, numFiles);
							fileCount++;

							// get file extension
							String fileName = resultSet.getString("file_name");

							// create path output file
							Path outputFile = outputDir_.resolve(fileName);

							// get blob
							Blob blob = resultSet.getBlob("data");

							// copy BLOB data to file
							Path gzipFile = task_.getWorkingDirectory().resolve(fileName + FileType.GZ.getExtension());
							try (InputStream inputStream = blob.getBinaryStream()) {
								Files.copy(inputStream, gzipFile, StandardCopyOption.REPLACE_EXISTING);
							}

							// extract output file
							Utility.extractFileFromGZIP(gzipFile, outputFile);

							// free blob
							blob.free();
						}
					}
				}
			}
		}

		// return
		return null;
	}
}
