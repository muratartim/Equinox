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

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.PostProcessingTask;
import equinox.task.automation.SingleInputTask;

/**
 * Class for save rainflow task.
 *
 * @author Murat Artim
 * @date Jun 19, 2014
 * @time 9:55:18 PM
 */
public class SaveRainflow extends InternalEquinoxTask<Path> implements LongRunningTask, PostProcessingTask, SingleInputTask<SpectrumItem>, AutomaticTaskOwner<Path> {

	/** File item to save. */
	private SpectrumItem file_;

	/** Output file. */
	private final File output_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.00");

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save rainflow task.
	 *
	 * @param file
	 *            File item to save. Can be null for automatic execution.
	 * @param output
	 *            Output file.
	 */
	public SaveRainflow(SpectrumItem file, File output) {
		file_ = file;
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
	public void setAutomaticInput(SpectrumItem input) {
		file_ = input;
	}

	@Override
	public String getTaskTitle() {
		return "Save rainflow cycles to '" + output_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.SAVE_FILE);

		// update progress info
		updateTitle("Saving rainflow cycles to '" + output_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// create output file writer
				try (BufferedWriter writer = Files.newBufferedWriter(output_.toPath(), Charset.defaultCharset())) {

					// write header
					writeHeader(statement, writer);

					// write rainflow cycles
					writeCycles(statement, writer);
				}
			}
		}

		// return
		return output_.toPath();
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
	 * Writes out rainflow cycles.
	 *
	 * @param statement
	 *            Database statement.
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeCycles(Statement statement, BufferedWriter writer) throws Exception {

		// update progress info
		updateMessage("Writing rainflow cycles...");

		// write column headers
		String line = String.format("%16s", "Number of Cycles");
		line += String.format("%16s", "Maximum Stress");
		line += String.format("%16s", "Minimum Stress");
		line += String.format("%16s", "Mean Stress");
		line += String.format("%16s", "R-Ratio");
		writer.write(line);
		writer.newLine();

		// get table name
		String tableName = null;
		if (file_ instanceof FatigueEquivalentStress) {
			tableName = "fatigue_rainflow_cycles";
		}
		else if (file_ instanceof PreffasEquivalentStress) {
			tableName = "preffas_rainflow_cycles";
		}
		else if (file_ instanceof LinearEquivalentStress) {
			tableName = "linear_rainflow_cycles";
		}
		else if (file_ instanceof ExternalFatigueEquivalentStress) {
			tableName = "ext_fatigue_rainflow_cycles";
		}
		else if (file_ instanceof ExternalPreffasEquivalentStress) {
			tableName = "ext_preffas_rainflow_cycles";
		}
		else if (file_ instanceof ExternalLinearEquivalentStress) {
			tableName = "ext_linear_rainflow_cycles";
		}

		// get all lines of the conversion table
		String sql = "select * from " + tableName + " where stress_id = " + file_.getID() + " order by cycle_num asc";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				line = String.format("%16s", format_.format(resultSet.getDouble("num_cycles")));
				line += String.format("%16s", format_.format(resultSet.getDouble("max_val")));
				line += String.format("%16s", format_.format(resultSet.getDouble("min_val")));
				line += String.format("%16s", format_.format(resultSet.getDouble("mean_val")));
				line += String.format("%16s", format_.format(resultSet.getDouble("r_ratio")));
				writer.write(line);
				writer.newLine();
			}
		}
	}

	/**
	 * Writes out file header information.
	 *
	 * @param statement
	 *            Database statement.
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeHeader(Statement statement, BufferedWriter writer) throws Exception {

		// update progress info
		updateMessage("Writing rainflow file header...");

		// write info
		writer.write("Spectrum Rainflowed by Equinox Version " + Equinox.VERSION.toString() + ", Date: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
		writer.newLine();

		// get table name
		String tableName = null;
		if (file_ instanceof FatigueEquivalentStress) {
			tableName = "fatigue_equivalent_stresses";
		}
		else if (file_ instanceof PreffasEquivalentStress) {
			tableName = "preffas_equivalent_stresses";
		}
		else if (file_ instanceof LinearEquivalentStress) {
			tableName = "linear_equivalent_stresses";
		}
		else if (file_ instanceof ExternalFatigueEquivalentStress) {
			tableName = "ext_fatigue_equivalent_stresses";
		}
		else if (file_ instanceof ExternalPreffasEquivalentStress) {
			tableName = "ext_preffas_equivalent_stresses";
		}
		else if (file_ instanceof ExternalLinearEquivalentStress) {
			tableName = "ext_linear_equivalent_stresses";
		}

		// write validity
		String sql = "select * from " + tableName + " where id = " + file_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				writer.write("Stress sequence: " + file_.getParentItem().getName());
				writer.newLine();
				String line = "Validity: " + format_.format(resultSet.getDouble("validity"));
				line += ", Omission level: " + format_.format(resultSet.getDouble("omission_level"));
				line += ", Total number of cycles: " + format_.format(resultSet.getDouble("total_cycles"));
				line += ", Maximum stress: " + format_.format(resultSet.getDouble("max_stress"));
				line += ", Minimum stress: " + format_.format(resultSet.getDouble("min_stress"));
				line += ", Seq R-ratio: " + format_.format(resultSet.getDouble("r_ratio"));
				writer.write(line);
				writer.newLine();
			}
		}
	}
}
