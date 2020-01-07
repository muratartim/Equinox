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

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import equinox.data.fileType.STFFileBucket;
import equinox.task.InternalEquinoxTask;

/**
 * Class for save STF file bucket process.
 *
 * @author Murat Artim
 * @date 6 Sep 2016
 * @time 10:10:23
 */
public class SaveSTFBucketProcess implements EquinoxProcess<STFFileBucket> {

	/** The owner task of this process. */
	private final InternalEquinoxTask<?> task_;

	/** STF file bucket to save. */
	private final STFFileBucket bucket_;

	/** Output directory. */
	private final Path outputDir_;

	/**
	 * Creates save STF file bucket process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param bucket
	 *            STF file bucket to save.
	 * @param outputDir
	 *            Output directory.
	 */
	public SaveSTFBucketProcess(InternalEquinoxTask<?> task, STFFileBucket bucket, Path outputDir) {
		task_ = task;
		bucket_ = bucket;
		outputDir_ = outputDir;
	}

	@Override
	public STFFileBucket start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// progress info
		task_.updateMessage("Saving STF files...");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// get STF file IDs
			String sql = "select file_id, stress_table_id, name, is_2d from stf_files where cdf_id = " + bucket_.getParentItem().getID();
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// loop over STF files
				int count = 0;
				while (resultSet.next()) {

					// task cancelled
					if (task_.isCancelled())
						break;

					// get STF file info
					String name = resultSet.getString("name");
					int stfID = resultSet.getInt("file_id");
					int stressTableID = resultSet.getInt("stress_table_id");
					boolean is2D = resultSet.getBoolean("is_2d");

					// update progress info
					task_.updateMessage("Saving STF file '" + name + "'...");
					task_.updateProgress(count, bucket_.getNumberOfSTFs());
					count++;

					// add file path
					Path output = outputDir_.resolve(name);
					new SaveSTFFile(task_, stfID, stressTableID, is2D, output).start(connection);
				}
			}
		}

		// return bucket
		return bucket_;
	}
}
