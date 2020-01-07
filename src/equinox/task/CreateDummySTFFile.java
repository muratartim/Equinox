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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.RandomUtils;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.plugin.FileType;
import equinox.process.LoadSTFFile;
import equinox.serverUtilities.Permission;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for creating dummy STF files.
 *
 * @author Murat Artim
 * @date May 20, 2014
 * @time 6:25:06 PM
 */
public class CreateDummySTFFile extends TemporaryFileCreatingTask<STFFile> implements SingleInputTask<Spectrum>, AutomaticTaskOwner<STFFile> {

	/** The owner spectrum. */
	private Spectrum spectrum_;

	/** File name. */
	private final String name_;

	/** True if 1D stress state. */
	private final boolean is1D_;

	/** Stress values. */
	private final double[] oneGStress_ = new double[3], incStress_ = new double[3], deltaPStress_ = new double[3], deltaTInfStress_ = new double[3], deltaTSupStress_ = new double[3];

	/** Delta-p and delta-t load cases. */
	private String dpLC_ = null, dtInfLC_ = null, dtSupLC_ = null;

	/** Automatic tasks. The key is the STF file name and the value is the task. */
	private HashMap<String, EmbeddedTask<STFFile>> automaticTasks_ = null;

	/**
	 * Creates dummy STF file task.
	 *
	 * @param spectrum
	 *            Spectrum. Can be null for automatic execution.
	 * @param name
	 *            File name.
	 * @param is1D
	 *            True if 1D stress state.
	 */
	public CreateDummySTFFile(Spectrum spectrum, String name, boolean is1D) {
		spectrum_ = spectrum;
		name_ = name;
		is1D_ = is1D;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<STFFile> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<STFFile>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public void setAutomaticInput(Spectrum spectrum) {
		spectrum_ = spectrum;
	}

	@Override
	public String getTaskTitle() {
		return "Create dummy STF file";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	/**
	 * Sets 1g stress values.
	 *
	 * @param x
	 *            Normal X stress.
	 * @param y
	 *            Normal Y stress.
	 * @param xy
	 *            Shear XY stress.
	 */
	public void setOneGStresses(double x, double y, double xy) {
		oneGStress_[0] = x;
		oneGStress_[1] = y;
		oneGStress_[2] = xy;
	}

	/**
	 * Sets increment stress values.
	 *
	 * @param x
	 *            Normal X stress.
	 * @param y
	 *            Normal Y stress.
	 * @param xy
	 *            Shear XY stress.
	 */
	public void setIncrementStresses(double x, double y, double xy) {
		incStress_[0] = x;
		incStress_[1] = y;
		incStress_[2] = xy;
	}

	/**
	 * Sets delta-p stress values.
	 *
	 * @param dpLC
	 *            Delta-P load case. Can be null for automatic determination.
	 * @param x
	 *            Normal X stress.
	 * @param y
	 *            Normal Y stress.
	 * @param xy
	 *            Shear XY stress.
	 */
	public void setDeltaPStresses(String dpLC, double x, double y, double xy) {
		dpLC_ = dpLC;
		deltaPStress_[0] = x;
		deltaPStress_[1] = y;
		deltaPStress_[2] = xy;
	}

	/**
	 * Sets delta-t superior stress values.
	 *
	 * @param dtSupLC
	 *            Delta-T superior load case.
	 * @param x
	 *            Normal X stress.
	 * @param y
	 *            Normal Y stress.
	 * @param xy
	 *            Shear XY stress.
	 */
	public void setDeltaTSupStresses(String dtSupLC, double x, double y, double xy) {
		dtSupLC_ = dtSupLC;
		deltaTSupStress_[0] = x;
		deltaTSupStress_[1] = y;
		deltaTSupStress_[2] = xy;
	}

	/**
	 * Sets delta-t inferior stress values.
	 *
	 * @param dtInfLC
	 *            Delta-T inferior load case.
	 * @param x
	 *            Normal X stress.
	 * @param y
	 *            Normal Y stress.
	 * @param xy
	 *            Shear XY stress.
	 */
	public void setDeltaTInfStresses(String dtInfLC, double x, double y, double xy) {
		dtInfLC_ = dtInfLC;
		deltaTInfStress_[0] = x;
		deltaTInfStress_[1] = y;
		deltaTInfStress_[2] = xy;
	}

	@Override
	protected STFFile call() throws Exception {

		// check permission
		checkPermission(Permission.CREATE_DUMMY_STF_FILE);

		// update progress info
		updateTitle("Creating dummy STF file");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create temporary STF file
			Path stf = createSTFFile(connection);

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// create stress table
				Object[] tableInfo = createStressTable(connection);

				// prepare statement for inserting STF files
				String sql = "insert into stf_files(cdf_id, stress_table_id, name, is_2d, description, element_type, frame_rib_position, stringer_position, data_source, generation_source, delivery_ref_num, issue, eid, fatigue_material, preffas_material, linear_material) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				try (PreparedStatement insertFile = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

					// prepare statement for inserting stresses
					sql = "insert into " + (String) tableInfo[1] + "(file_id, issy_code, stress_x, stress_y, stress_xy) values(?, ?, ?, ?, ?)";
					try (PreparedStatement insertStresses = connection.prepareStatement(sql)) {

						// prepare statement for updating stress state
						sql = "update stf_files set is_2d = ? where file_id = ?";
						try (PreparedStatement updateStressState = connection.prepareStatement(sql)) {

							// load and add STF file
							STFFile stfFile = new LoadSTFFile(this, stf, spectrum_, null, true, (int) tableInfo[0]).start(connection, insertFile, insertStresses, updateStressState);

							// cannot load
							if (stfFile == null) {
								connection.rollback();
								connection.setAutoCommit(true);
								return null;
							}

							// commit updates
							connection.commit();
							connection.setAutoCommit(true);

							// return STF file
							return stfFile;
						}
					}
				}
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				if (connection != null) {
					connection.rollback();
					connection.setAutoCommit(true);
				}

				// propagate exception
				throw e;
			}
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// add STF file to CDF set
		try {

			// get STF file
			STFFile stfFile = get();

			// add to spectrum
			spectrum_.getChildren().add(stfFile);

			// manage automatic tasks
			automaticTaskOwnerSucceeded(stfFile, automaticTasks_, taskPanel_);
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
	 * Creates STF stress table.
	 *
	 * @param connection
	 *            Database connection.
	 * @return An array containing table ID and table name, respectively.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Object[] createStressTable(Connection connection) throws Exception {

		// initialize table ID
		int tableID = -1;
		String tableName = null;

		// create statement to create STF stress tables
		try (Statement createStressTable = connection.createStatement()) {

			// prepare statement to check if generated table ID already exists
			String sql = "select 1 from stf_files where stress_table_id = ?";
			try (PreparedStatement checkTableID = connection.prepareStatement(sql)) {

				// generate table name
				generate: while (tableName == null) {
					tableID = RandomUtils.nextInt(0, AddSTFFiles.MAX_STRESS_TABLES);
					checkTableID.setInt(1, tableID);
					try (ResultSet resultSet = checkTableID.executeQuery()) {
						if (resultSet.next()) {
							continue generate;
						}
					}
					tableName = "stf_stresses_" + tableID;
				}

				// create STF stress table
				createStressTable.executeUpdate("CREATE TABLE AURORA." + tableName + "(FILE_ID INT NOT NULL, ISSY_CODE VARCHAR(4) NOT NULL, STRESS_X DOUBLE NOT NULL, STRESS_Y DOUBLE NOT NULL, STRESS_XY DOUBLE NOT NULL)");
				createStressTable.executeUpdate("CREATE INDEX STF_SELSTRESS_" + tableID + " ON AURORA." + tableName + "(FILE_ID, ISSY_CODE)");
			}
		}

		// return table ID
		return new Object[] { tableID, tableName };
	}

	/**
	 * Creates and returns a temporary STF file.
	 *
	 * @param connection
	 *            Database connection.
	 * @return A temporary STF file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path createSTFFile(Connection connection) throws Exception {

		// update info
		updateMessage("Generating dummy STF file...");

		// initialize variables
		String line;
		int index = is1D_ ? 1 : 3;

		// create output path
		Path output = getWorkingDirectory().resolve(FileType.appendExtension(name_, FileType.STF));

		// create output file writer
		try (BufferedWriter writer = Files.newBufferedWriter(output, Charset.defaultCharset())) {

			// write comment lines
			writer.write("# Dummy stress input file generated by Equinox, " + new Date().toString());
			writer.newLine();
			writer.write("# Columns: Load case, normalX" + (is1D_ ? "" : ", normalY, shearXY"));
			writer.newLine();

			// create database statement
			try (Statement statement = connection.createStatement()) {

				// create SQL query
				String sql = "select distinct issy_code, increment_num, dp_case from txt_codes where file_id = " + spectrum_.getTXTFileID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {

					// loop over issy codes
					boolean dpLCFound = false, dtInfLCFound = false, dtSupLCFound = false;
					while (resultSet.next()) {

						// get load case specifications
						line = resultSet.getString("issy_code");

						// automatic delta-p load case
						if (dpLC_ == null && resultSet.getShort("dp_case") == 1) {
							for (int i = 0; i < index; i++) {
								line += "\t" + deltaPStress_[i];
							}
							writer.write(line);
							writer.newLine();
							continue;
						}

						// given delta-p load case
						else if (dpLC_ != null && dpLC_.equals(line)) {
							for (int i = 0; i < index; i++) {
								line += "\t" + deltaPStress_[i];
							}
							writer.write(line);
							writer.newLine();
							dpLCFound = true;
							continue;
						}

						// delta-t superior load case
						else if (dtSupLC_ != null && dtSupLC_.equals(line)) {
							for (int i = 0; i < index; i++) {
								line += "\t" + deltaTSupStress_[i];
							}
							writer.write(line);
							writer.newLine();
							dtSupLCFound = true;
							continue;
						}

						// delta-t inferior load case
						else if (dtInfLC_ != null && dtInfLC_.equals(line)) {
							for (int i = 0; i < index; i++) {
								line += "\t" + deltaTInfStress_[i];
							}
							writer.write(line);
							writer.newLine();
							dtInfLCFound = true;
							continue;
						}

						// 1G load case
						else if (resultSet.getInt("increment_num") == 0) {
							for (int i = 0; i < index; i++) {
								line += "\t" + oneGStress_[i];
							}
							writer.write(line);
							writer.newLine();
							continue;
						}

						// increment load case
						else {
							for (int i = 0; i < index; i++) {
								line += "\t" + incStress_[i];
							}
							writer.write(line);
							writer.newLine();
							continue;
						}
					}

					// given load cases could not be found
					if (dpLC_ != null && !dpLCFound) {
						warnings_ += "Delta-P load case '" + dpLC_ + "' could not be found.\n";
					}
					if (dtSupLC_ != null && !dtSupLCFound) {
						warnings_ += "Delta-T superior load case '" + dtSupLC_ + "' could not be found.\n";
					}
					if (dtInfLC_ != null && !dtInfLCFound) {
						warnings_ += "Delta-T inferior load case '" + dtInfLC_ + "' could not be found.\n";
					}
				}
			}
		}

		// return output file
		return output;
	}
}
