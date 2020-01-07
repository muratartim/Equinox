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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import equinox.Equinox;
import equinox.data.fileType.AircraftLoadCase;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;

/**
 * Class for save load case task.
 *
 * @author Murat Artim
 * @date Aug 20, 2015
 * @time 12:45:48 PM
 */
public class SaveLoadCase extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** File item to save. */
	private final AircraftLoadCase loadCase_;

	/** Output file. */
	private final File output_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.00");

	/**
	 * Creates save load case task.
	 *
	 * @param loadCase
	 *            File item to save.
	 * @param output
	 *            Output file.
	 */
	public SaveLoadCase(AircraftLoadCase loadCase, File output) {
		loadCase_ = loadCase;
		output_ = output;
	}

	@Override
	public String getTaskTitle() {
		return "Save load case '" + loadCase_.getName() + "' to '" + output_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.SAVE_FILE);

		// update progress info
		updateTitle("Saving load case '" + loadCase_.getName() + "' to '" + output_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// create output file writer
				try (BufferedWriter writer = Files.newBufferedWriter(output_.toPath(), Charset.defaultCharset())) {

					// write header
					writeHeader(statement, writer);

					// write stresses
					writeStresses(statement, writer);
				}
			}
		}

		// return
		return null;
	}

	/**
	 * Writes out element stresses.
	 *
	 * @param statement
	 *            Database statement.
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeStresses(Statement statement, BufferedWriter writer) throws Exception {

		// update progress info
		updateMessage("Writing element stresses...");

		// write
		String sql = "select eid, sx, sy, sxy, sx is null as sxnull, sy is null as synull, sxy is null as sxynull from load_cases_" + loadCase_.getParentItem().getParentItem().getID();
		sql += " where lc_id = " + loadCase_.getID();
		sql += " order by eid";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			String line = null;
			while (resultSet.next()) {
				line = String.format("%-12s", resultSet.getInt("eid"));
				if (resultSet.getBoolean("sxnull")) {
					line += String.format("%-12s", "-");
				}
				else {
					line += String.format("%-12s", format_.format(resultSet.getDouble("sx")));
				}
				if (resultSet.getBoolean("synull")) {
					line += String.format("%-12s", "-");
				}
				else {
					line += String.format("%-12s", format_.format(resultSet.getDouble("sy")));
				}
				if (resultSet.getBoolean("sxynull")) {
					line += String.format("%-12s", "-");
				}
				else {
					line += String.format("%-12s", format_.format(resultSet.getDouble("sxy")));
				}
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
		updateMessage("Writing file header...");

		// get load case name and number
		String loadCaseName = null, loadCaseComments = null;
		int loadCaseNumber = -1;
		String sql = "select lc_name, lc_num, lc_comments from load_case_names_" + loadCase_.getParentItem().getParentItem().getID();
		sql += " where lc_id = " + loadCase_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				loadCaseName = resultSet.getString("lc_name");
				loadCaseNumber = resultSet.getInt("lc_num");
				loadCaseComments = resultSet.getString("lc_comments");
			}
		}

		// write info
		writer.write("Load case file generated by Equinox Version " + Equinox.VERSION.toString() + ", Date: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
		writer.newLine();
		writer.write("A/C program: " + loadCase_.getParentItem().getParentItem().getProgram());
		writer.newLine();
		writer.write("A/C model name: " + loadCase_.getParentItem().getParentItem().getModelName());
		writer.newLine();
		writer.write("Load case name: " + loadCaseName);
		writer.newLine();
		writer.write("Load case number: " + loadCaseNumber);
		writer.newLine();
		writer.write("Load case comments: " + (loadCaseComments == null ? "" : loadCaseComments));
		writer.newLine();
		String line = String.format("%-12s", "EID");
		line += String.format("%-12s", "SX");
		line += String.format("%-12s", "SY");
		line += String.format("%-12s", "SXY");
		writer.write(line);
		writer.newLine();
		line = String.format("%-12s", "===");
		line += String.format("%-12s", "===");
		line += String.format("%-12s", "===");
		line += String.format("%-12s", "===");
		writer.write(line);
		writer.newLine();
	}
}
