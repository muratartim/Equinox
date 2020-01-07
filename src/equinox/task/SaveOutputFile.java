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
package equinox.task;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
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
import equinox.process.SaveOutputFileProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for save output file task.
 *
 * @author Murat Artim
 * @date 26 Apr 2017
 * @time 14:04:33
 *
 */
public class SaveOutputFile extends TemporaryFileCreatingTask<Path> implements ShortRunningTask, SingleInputTask<SpectrumItem>, AutomaticTaskOwner<Path> {

	/** Spectrum item to save the output file for. */
	private SpectrumItem item_;

	/** Output file. */
	private final Path output_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save output file task.
	 *
	 * @param item
	 *            Spectrum item to save the output file for. Can be null for automatic execution.
	 * @param output
	 *            Output file.
	 */
	public SaveOutputFile(SpectrumItem item, Path output) {
		item_ = item;
		output_ = output;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Path> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Path>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save output file to '" + output_.getFileName().toString() + "'";
	}

	@Override
	public void setAutomaticInput(SpectrumItem input) {
		item_ = input;
	}

	@Override
	protected Path call() throws Exception {

		// update progress info
		updateTitle("Saving output file to '" + output_.getFileName().toString() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// get file type
			FileType fileType = getFileType(connection);
			if (fileType == null) {
				addWarning("No analysis output file is associated with item '" + item_.getName() + "'.");
				return null;
			}

			// append file extension (if necessary)
			Path output = FileType.appendExtension(output_.toFile(), fileType).toPath();

			// save output file
			new SaveOutputFileProcess(this, item_, output).start(connection);

			// return output file
			return output;
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get output file
			Path file = get();

			// manage automatic tasks
			automaticTaskOwnerSucceeded(file, automaticTasks_, taskPanel_);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	/**
	 * Returns output file type, or null if no output file is available.
	 *
	 * @param connection
	 *            Database connection.
	 * @return Output file type, or null if no output file is available.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private FileType getFileType(Connection connection) throws Exception {

		// initialize file type
		FileType type = null;

		// create statement
		try (Statement statement = connection.createStatement()) {

			// set table name
			String tableName = null;
			if (item_ instanceof FatigueEquivalentStress) {
				tableName = "fatigue_equivalent_stresses";
			}
			else if (item_ instanceof PreffasEquivalentStress) {
				tableName = "preffas_equivalent_stresses";
			}
			else if (item_ instanceof LinearEquivalentStress) {
				tableName = "linear_equivalent_stresses";
			}
			else if (item_ instanceof ExternalFatigueEquivalentStress) {
				tableName = "ext_fatigue_equivalent_stresses";
			}
			else if (item_ instanceof ExternalPreffasEquivalentStress) {
				tableName = "ext_preffas_equivalent_stresses";
			}
			else if (item_ instanceof ExternalLinearEquivalentStress) {
				tableName = "ext_linear_equivalent_stresses";
			}
			else if (item_ instanceof FastFatigueEquivalentStress) {
				tableName = "fast_fatigue_equivalent_stresses";
			}
			else if (item_ instanceof FastPreffasEquivalentStress) {
				tableName = "fast_preffas_equivalent_stresses";
			}
			else if (item_ instanceof FastLinearEquivalentStress) {
				tableName = "fast_linear_equivalent_stresses";
			}
			else
				return null;

			// execute query
			String sql = "select analysis_output_files.file_extension from " + tableName + " inner join analysis_output_files on " + tableName + ".output_file_id = analysis_output_files.id ";
			sql += "where " + tableName + ".output_file_id is not null and " + tableName + ".id = " + item_.getID();
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// get data
				if (resultSet.next()) {
					String extension = resultSet.getString("file_extension");
					type = FileType.getFileTypeForExtension(extension);
				}

				// no output file
				else
					return null;
			}
		}

		// return file type
		return type;
	}
}
