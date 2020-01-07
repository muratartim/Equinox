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
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.ViewPanel;
import equinox.controller.WebViewPanel;
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

/**
 * Class for show output file task.
 *
 * @author Murat Artim
 * @date 21 Apr 2017
 * @time 14:13:15
 *
 */
public class ShowOutputFile extends TemporaryFileCreatingTask<Path> implements ShortRunningTask {

	/** Spectrum item to show the output file. */
	private final SpectrumItem item_;

	/** Web view panel header. */
	private String header_;

	/**
	 * Creates show output file task.
	 *
	 * @param item
	 *            Spectrum item to show the output file.
	 */
	public ShowOutputFile(SpectrumItem item) {
		item_ = item;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Show output file for '" + item_.getName() + "'";
	}

	@Override
	protected Path call() throws Exception {

		// update progress info
		updateTitle("Showing output file for '" + item_.getName() + "'");

		// initialize output file
		Path outputFile = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create path to output file
			outputFile = createOutputFilePath(connection);

			// no output file
			if (outputFile == null)
				return null;

			// save output file
			outputFile = new SaveOutputFileProcess(this, item_, outputFile).start(connection);
		}

		// no output file
		if (outputFile == null)
			return null;

		// set output file as permanent
		setFileAsPermanent(outputFile);

		// return output file
		return outputFile;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// show output file
		try {

			// get output file
			Path outputFile = get();

			// show output file
			WebViewPanel panel = (WebViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.WEB_VIEW);
			panel.showOutputFile(outputFile, header_);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.WEB_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Creates and returns path to output file, or null if no output file exists.
	 *
	 * @param connection
	 *            Database connection.
	 * @return Path to output file, or null if no output file exists.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path createOutputFilePath(Connection connection) throws Exception {

		// initialize output file
		Path outputFile = null;

		// create statement
		try (Statement statement = connection.createStatement()) {

			// set table name and file name
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
			String sql = "select analysis_output_files.file_extension, analysis_output_files.file_name from " + tableName + " inner join analysis_output_files on " + tableName + ".output_file_id = analysis_output_files.id ";
			sql += "where " + tableName + ".output_file_id is not null and " + tableName + ".id = " + item_.getID();
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// get data
				if (resultSet.next()) {
					String extension = resultSet.getString("file_extension");
					String fileName = resultSet.getString("file_name");
					header_ = fileName;
					if (!extension.equalsIgnoreCase(FileType.HTML.getExtension())) {
						fileName = FileType.getNameWithoutExtension(fileName) + ".txt";
					}
					outputFile = getWorkingDirectory().resolve(fileName);
				}

				// no output file
				else
					return null;
			}
		}

		// return output file
		return outputFile;
	}
}
