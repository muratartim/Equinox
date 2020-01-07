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
 * Class for save analysis output files process.
 *
 * @author Murat Artim
 * @date 27 Apr 2017
 * @time 11:35:15
 *
 */
public class SaveOutputFilesProcess implements EquinoxProcess<Void> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** Spectrum item to save the output file for. */
	private final SpectrumItem[] items_;

	/** Output file. */
	private final Path outputDirectory_;

	/**
	 * Creates save output files process.
	 *
	 * @param task
	 *            The owner task.
	 * @param items
	 *            Spectrum items to save the output file for.
	 * @param outputDirectory
	 *            Output directory.
	 */
	public SaveOutputFilesProcess(TemporaryFileCreatingTask<?> task, SpectrumItem[] items, Path outputDirectory) {
		task_ = task;
		items_ = items;
		outputDirectory_ = outputDirectory;
	}

	@Override
	public Void start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// set table name
		String tableName = null;
		if (items_[0] instanceof FatigueEquivalentStress)
			tableName = "fatigue_equivalent_stresses";
		else if (items_[0] instanceof PreffasEquivalentStress)
			tableName = "preffas_equivalent_stresses";
		else if (items_[0] instanceof LinearEquivalentStress)
			tableName = "linear_equivalent_stresses";
		else if (items_[0] instanceof ExternalFatigueEquivalentStress)
			tableName = "ext_fatigue_equivalent_stresses";
		else if (items_[0] instanceof ExternalPreffasEquivalentStress)
			tableName = "ext_preffas_equivalent_stresses";
		else if (items_[0] instanceof ExternalLinearEquivalentStress)
			tableName = "ext_linear_equivalent_stresses";
		else if (items_[0] instanceof FastFatigueEquivalentStress)
			tableName = "fast_fatigue_equivalent_stresses";
		else if (items_[0] instanceof FastPreffasEquivalentStress)
			tableName = "fast_preffas_equivalent_stresses";
		else if (items_[0] instanceof FastLinearEquivalentStress)
			tableName = "fast_linear_equivalent_stresses";

		// prepare statement
		String sql = "select analysis_output_files.file_name, analysis_output_files.data from " + tableName + " inner join analysis_output_files on " + tableName + ".output_file_id = analysis_output_files.id ";
		sql += "where " + tableName + ".output_file_id is not null and " + tableName + ".id = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {

			// loop over items
			for (int i = 0; i < items_.length; i++) {

				// task cancelled
				if (task_.isCancelled())
					break;

				// get item
				SpectrumItem item = items_[i];

				// update info
				task_.updateMessage("Saving output file for item '" + item.getName() + "'...");
				task_.updateProgress(i, items_.length);

				// execute query
				statement.setInt(1, item.getID());
				try (ResultSet resultSet = statement.executeQuery()) {

					// there is output file
					if (resultSet.next()) {

						// get file name
						String fileName = resultSet.getString("file_name");

						// create path output file
						Path outputFile = outputDirectory_.resolve(fileName);

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

					// no output file
					else {
						task_.addWarning("No analysis output file is associated with item '" + item.getName() + "'.");
						continue;
					}
				}
			}
		}

		// return
		return null;
	}
}
