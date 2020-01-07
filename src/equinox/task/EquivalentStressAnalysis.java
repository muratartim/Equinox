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

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.AnalysisEngine;
import equinox.data.EmbeddedTask;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.Settings;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.data.fileType.StressSequence;
import equinox.data.input.EquivalentStressInput;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.LinearMaterial;
import equinox.dataServer.remote.data.Material;
import equinox.dataServer.remote.data.PreffasMaterial;
import equinox.plugin.FileType;
import equinox.process.ESAProcess;
import equinox.process.InbuiltESA;
import equinox.process.IsamiESA;
import equinox.process.Rainflow;
import equinox.process.SafeESA;
import equinox.process.SaveExternalSTH;
import equinox.process.SaveSTH;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.ServerUtility;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.task.serializableTask.SerializableEquivalentStressAnalysis;
import equinox.utility.Utility;

/**
 * Class for equivalent stress analysis task.
 *
 * @author Murat Artim
 * @date Jul 7, 2014
 * @time 11:11:39 AM
 */
public class EquivalentStressAnalysis extends TemporaryFileCreatingTask<SpectrumItem> implements LongRunningTask, SavableTask, SingleInputTask<SpectrumItem>, AutomaticTaskOwner<SpectrumItem> {

	/** Maximum number of allowed peaks per typical flight for standard in-build/server analysis engine. */
	public static final int MAX_PEAKS = 100000;

	/** The owner stress sequence. */
	private SpectrumItem stressSequence_ = null;

	/** Analysis input. */
	private final EquivalentStressInput input_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/** ISAMI version. */
	private IsamiVersion isamiVersion_;

	/** ISAMI sub version. */
	private IsamiSubVersion isamiSubVersion_;

	/** True compression should be applied in propagation analysis. */
	private boolean applyCompression_;

	/** Omission process. */
	private Process omission_;

	/** Rainflow process. */
	private ESAProcess<Void> rainflow_;

	/** Equivalent stress analysis process. */
	private ESAProcess<Void> equivalentStressAnalysis_;

	/** Sub process completion indicators. */
	private volatile boolean rainflowCompleted_ = false, equivalentStressAnalysisCompleted_ = false;

	/** Sub process exception messages. */
	private volatile Exception rainflowException_, equivalentStressAnalysisException_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<SpectrumItem>> automaticTasks_ = null;

	/**
	 * Creates equivalent stress analysis task.
	 *
	 * @param stressSequence
	 *            The owner stress sequence. Can be null for automatic execution.
	 * @param input
	 *            Analysis input.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public EquivalentStressAnalysis(SpectrumItem stressSequence, EquivalentStressInput input, AnalysisEngine analysisEngine) {
		stressSequence_ = stressSequence;
		input_ = input;
		analysisEngine_ = analysisEngine;
	}

	/**
	 * Sets ISAMI engine inputs.
	 *
	 * @param isamiVersion
	 *            ISAMI version.
	 * @param isamiSubVersion
	 *            ISAMI sub version.
	 * @param applyCompression
	 *            True to apply compression for propagation analyses.
	 * @return This analysis.
	 */
	public EquivalentStressAnalysis setIsamiEngineInputs(IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
		return this;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<SpectrumItem> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<SpectrumItem>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public void setAutomaticInput(SpectrumItem stressSequence) {
		stressSequence_ = stressSequence;
	}

	@Override
	public String getTaskTitle() {
		String title = " analysis for '" + stressSequence_.getName() + "'";
		if (input_.getMaterial() instanceof FatigueMaterial) {
			title = "Fatigue" + title;
		}
		else if (input_.getMaterial() instanceof PreffasMaterial) {
			title = "Preffas" + title;
		}
		else if (input_.getMaterial() instanceof LinearMaterial) {
			title = "Linear prop." + title;
		}
		return title;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public SerializableEquivalentStressAnalysis getSerializableTask() {
		return new SerializableEquivalentStressAnalysis(stressSequence_, input_, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
	}

	@Override
	protected SpectrumItem call() throws Exception {

		// check permission
		checkPermission(Permission.EQUIVALENT_STRESS_ANALYSIS);

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// write out STH file
				Path inputSTH = getWorkingDirectory().resolve(FileType.appendExtension(Utility.correctFileName(stressSequence_.getName()), FileType.STH));

				// stress sequence
				if (stressSequence_ instanceof StressSequence) {

					// create process
					SaveSTH process = new SaveSTH(this, (StressSequence) stressSequence_, inputSTH.toFile());

					// set remove negative stresses option
					process.setRemoveNegativeStresses(input_.removeNegativeStresses());

					// start process
					process.start(connection);
				}

				// external stress sequence
				else if (stressSequence_ instanceof ExternalStressSequence) {

					// create process
					SaveExternalSTH process = new SaveExternalSTH(this, (ExternalStressSequence) stressSequence_, inputSTH.toFile());

					// set stress modifier
					process.setStressModifier(input_.getStressModificationValue(), input_.getStressModificationMethod());

					// set remove negative stresses option
					process.setRemoveNegativeStresses(input_.removeNegativeStresses());

					// start process
					process.start(connection);
				}

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// apply omission
				if (input_.applyOmission()) {
					inputSTH = applyOmission(inputSTH);
				}

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// create equivalent stress
				SpectrumItem eqStress = createEquivalentStress(connection);

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// get maximum number of peaks per typical flight
				int maxPeaks = getMaxPeaksPerFlight(connection);

				// set progress indeterminate
				updateProgress(-1, 100);

				// run rainflow process
				rainflow(connection, inputSTH, eqStress);

				// run equivalent stress analysis process
				equivalentStressAnalysis(connection, inputSTH, eqStress, maxPeaks);

				// wait for processes to complete
				waitForProcesses();

				// rainflow failed
				if (rainflowException_ != null)
					throw rainflowException_;

				// equivalent stress analysis failed
				if (equivalentStressAnalysisException_ != null)
					throw equivalentStressAnalysisException_;

				// task cancelled
				if (isCancelled()) {

					// cancel rainflow process
					rainflow_.cancel();

					// cancel equivalent stress analysis process
					equivalentStressAnalysis_.cancel();

					// roll back database updates
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return equivalent stress
				return eqStress;
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

		// add equivalent stress to file tree
		try {

			// get equivalent stress
			SpectrumItem eqStress = get();

			// add to stress sequence
			stressSequence_.getChildren().add(eqStress);

			// generate and save plots
			if (eqStress instanceof FatigueEquivalentStress || eqStress instanceof PreffasEquivalentStress || eqStress instanceof LinearEquivalentStress) {
				postProcess(eqStress);
			}

			// manage automatic tasks
			automaticTaskOwnerSucceeded(eqStress, automaticTasks_, taskPanel_);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Post-processes results of this task.
	 *
	 * @param eqStress
	 *            Equivalent stress.
	 */
	private void postProcess(SpectrumItem eqStress) {

		// generate and save plots
		try {
			taskPanel_.getOwner().runTaskSilently(new SaveLevelCrossingsPlot(eqStress), false).get();
		}

		// exception occurred (ignore since it is handled within the task)
		catch (Exception e) {
			// ignore
		}
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);

		// destroy sub processes (if still running)
		if (omission_ != null && omission_.isAlive()) {
			omission_.destroyForcibly();
		}
		if (rainflow_ != null) {
			rainflow_.cancel();
		}
		if (equivalentStressAnalysis_ != null) {
			equivalentStressAnalysis_.cancel();
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);

		// destroy sub processes (if still running)
		if (omission_ != null && omission_.isAlive()) {
			omission_.destroyForcibly();
		}
		if (rainflow_ != null) {
			rainflow_.cancel();
		}
		if (equivalentStressAnalysis_ != null) {
			equivalentStressAnalysis_.cancel();
		}
	}

	/**
	 * Runs omission script on a separate process. This method will wait till the process has ended.
	 *
	 * @param inputSTH
	 *            Input STH file.
	 * @return Path to omission output file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path applyOmission(Path inputSTH) throws Exception {

		// update info
		updateMessage("Applying omission to stress sequence '" + inputSTH.getFileName() + "'...");
		updateProgress(-1, 100);

		// get path to perl script and perl executable
		Path script = Equinox.SCRIPTS_DIR.resolve("omission.pl");

		// get input STH file name
		Path inputSTHFileName = inputSTH.getFileName();
		if (inputSTHFileName == null)
			throw new Exception("Cannot get input STH file name.");

		// create process builder for windows
		ProcessBuilder pb = null;
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			Path perl = Equinox.SCRIPTS_DIR.resolve("perl").resolve("bin").resolve("perl.exe");
			pb = new ProcessBuilder(perl.toAbsolutePath().toString(), script.toAbsolutePath().toString(), inputSTHFileName.toString(), Double.toString(input_.getOmissionLevel()));
		}

		// create process builder for macOSX
		else if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), inputSTHFileName.toString(), Double.toString(input_.getOmissionLevel()));
		}
		else if (Equinox.OS_TYPE.equals(ServerUtility.LINUX)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), inputSTHFileName.toString(), Double.toString(input_.getOmissionLevel()));
		}

		// unsupported OS
		if (pb == null)
			throw new Exception("Unsupported operating system.");

		// execute process and wait till it ends
		Path workingDir = getWorkingDirectory();
		pb.directory(workingDir.toFile());
		File log = workingDir.resolve("omission.log").toFile();
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		omission_ = pb.start();
		assert pb.redirectInput() == Redirect.PIPE;
		assert pb.redirectOutput().file() == log;
		assert omission_.getInputStream().read() == -1;

		// process failed
		if (omission_.waitFor() != 0)
			throw new Exception("Stress sequence omission failed! See omission log file for details.");

		// get output file
		Path output = Paths.get(inputSTH.toString() + FileType.RFORT.getExtension());

		// output file doesn't exist
		if (!Files.exists(output))
			throw new Exception("Stress sequence omission failed! See omission log file for details.");

		// return output path
		return output;
	}

	/**
	 * Creates equivalent stress in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @return ID of created equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private SpectrumItem createEquivalentStress(Connection connection) throws Exception {

		// update info
		updateMessage("Saving equivalent stress info to database...");

		// get material
		Material material = input_.getMaterial();

		// internal stress sequence
		if (stressSequence_ instanceof StressSequence) {

			// fatigue equivalent stress
			if (material instanceof FatigueMaterial)
				return createFatigueEquivalentStress(connection, (StressSequence) stressSequence_, (FatigueMaterial) material);

			// preffas equivalent stress
			else if (material instanceof PreffasMaterial)
				return createPreffasEquivalentStress(connection, (StressSequence) stressSequence_, (PreffasMaterial) material);

			// linear equivalent stress
			else if (material instanceof LinearMaterial)
				return createLinearEquivalentStress(connection, (StressSequence) stressSequence_, (LinearMaterial) material);
		}

		// external stress sequence
		else if (stressSequence_ instanceof ExternalStressSequence)
			// fatigue equivalent stress
			if (material instanceof FatigueMaterial)
				return createExternalFatigueEquivalentStress(connection, (ExternalStressSequence) stressSequence_, (FatigueMaterial) material);

			// preffas equivalent stress
			else if (material instanceof PreffasMaterial)
				return createExternalPreffasEquivalentStress(connection, (ExternalStressSequence) stressSequence_, (PreffasMaterial) material);

			// linear equivalent stress
			else if (material instanceof LinearMaterial)
				return createExternalLinearEquivalentStress(connection, (ExternalStressSequence) stressSequence_, (LinearMaterial) material);
		return null;
	}

	/**
	 * Creates and returns fatigue equivalent stress in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param stressSequence
	 *            Stress sequence.
	 * @param material
	 *            Fatigue material.
	 * @return Fatigue equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private FatigueEquivalentStress createFatigueEquivalentStress(Connection connection, StressSequence stressSequence, FatigueMaterial material) throws Exception {

		// save material name to STF files table
		String sql = "update stf_files set fatigue_material = '" + material.toString() + "' where file_id = " + stressSequence.getParentItem().getID();
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}

		// initialize equivalent stress
		FatigueEquivalentStress stress = null;

		// create statement
		sql = "insert into fatigue_equivalent_stresses";
		sql += "(sth_id, name, remove_negative, omission_level, material_name, material_specification, material_library_version, material_family, material_orientation, material_configuration, material_p, material_q, material_m, material_isami_version) ";
		sql += "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// set parameters
			update.setInt(1, stressSequence_.getID()); // stress sequence ID
			String stressName = "Fatigue Eq. Stress (" + stressSequence_.getName() + ")";
			update.setString(2, stressName);
			update.setBoolean(3, input_.removeNegativeStresses()); // remove negative stresses
			double omissionLevel = input_.applyOmission() ? input_.getOmissionLevel() : -1.0;
			update.setDouble(4, omissionLevel); // omission level
			update.setString(5, material.getName());
			if (material.getSpecification() == null || material.getSpecification().isEmpty()) {
				update.setNull(6, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(6, material.getSpecification());
			}
			if (material.getLibraryVersion() == null || material.getLibraryVersion().isEmpty()) {
				update.setNull(7, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(7, material.getLibraryVersion());
			}
			if (material.getFamily() == null || material.getFamily().isEmpty()) {
				update.setNull(8, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(8, material.getFamily());
			}
			if (material.getOrientation() == null || material.getOrientation().isEmpty()) {
				update.setNull(9, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(9, material.getOrientation());
			}
			if (material.getConfiguration() == null || material.getConfiguration().isEmpty()) {
				update.setNull(10, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(10, material.getConfiguration());
			}
			update.setDouble(11, material.getP());
			update.setDouble(12, material.getQ());
			update.setDouble(13, material.getM());
			if (material.getIsamiVersion() == null || material.getIsamiVersion().isEmpty()) {
				update.setNull(14, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(14, material.getIsamiVersion());
			}

			// execute update
			update.executeUpdate();

			// create equivalent stress
			try (ResultSet resultSet = update.getGeneratedKeys()) {
				while (resultSet.next()) {

					// get name and ID
					int id = resultSet.getBigDecimal(1).intValue();
					String materialName = material.toString();

					// create equivalent stress
					stress = new FatigueEquivalentStress(stressName, id, omissionLevel, materialName);
				}
			}
		}

		// return equivalent stress
		return stress;
	}

	/**
	 * Creates and returns preffas equivalent stress in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param stressSequence
	 *            Stress sequence.
	 * @param material
	 *            Preffas material.
	 * @return Preffas equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private PreffasEquivalentStress createPreffasEquivalentStress(Connection connection, StressSequence stressSequence, PreffasMaterial material) throws Exception {

		// save material name to STF files table
		String sql = "update stf_files set preffas_material = '" + material.toString() + "' where file_id = " + stressSequence.getParentItem().getID();
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}

		// initialize equivalent stress
		PreffasEquivalentStress stress = null;

		// create statement
		sql = "insert into preffas_equivalent_stresses";
		sql += "(sth_id, name, remove_negative, omission_level, material_name, material_specification, material_library_version, material_family, material_orientation, material_configuration, ";
		sql += "material_ceff, material_m, material_a, material_b, material_c, material_ftu, material_fty, material_isami_version) ";
		sql += "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// set parameters
			update.setInt(1, stressSequence_.getID()); // stress sequence ID
			String stressName = "Preffas Eq. Stress (" + stressSequence_.getName() + ")";
			update.setString(2, stressName);
			update.setBoolean(3, input_.removeNegativeStresses()); // remove negative stresses
			double omissionLevel = input_.applyOmission() ? input_.getOmissionLevel() : -1.0;
			update.setDouble(4, omissionLevel); // omission level
			update.setString(5, material.getName());
			if (material.getSpecification() == null || material.getSpecification().isEmpty()) {
				update.setNull(6, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(6, material.getSpecification());
			}
			if (material.getLibraryVersion() == null || material.getLibraryVersion().isEmpty()) {
				update.setNull(7, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(7, material.getLibraryVersion());
			}
			if (material.getFamily() == null || material.getFamily().isEmpty()) {
				update.setNull(8, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(8, material.getFamily());
			}
			if (material.getOrientation() == null || material.getOrientation().isEmpty()) {
				update.setNull(9, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(9, material.getOrientation());
			}
			if (material.getConfiguration() == null || material.getConfiguration().isEmpty()) {
				update.setNull(10, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(10, material.getConfiguration());
			}
			update.setDouble(11, material.getCeff());
			update.setDouble(12, material.getM());
			update.setDouble(13, material.getA());
			update.setDouble(14, material.getB());
			update.setDouble(15, material.getC());
			update.setDouble(16, material.getFtu());
			update.setDouble(17, material.getFty());
			if (material.getIsamiVersion() == null || material.getIsamiVersion().isEmpty()) {
				update.setNull(18, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(18, material.getIsamiVersion());
			}

			// execute update
			update.executeUpdate();

			// create equivalent stress
			try (ResultSet resultSet = update.getGeneratedKeys()) {
				while (resultSet.next()) {

					// get name and ID
					int id = resultSet.getBigDecimal(1).intValue();
					String materialName = material.toString();

					// create equivalent stress
					stress = new PreffasEquivalentStress(stressName, id, omissionLevel, materialName);
				}
			}
		}

		// return equivalent stress
		return stress;
	}

	/**
	 * Creates and returns linear equivalent stress in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param stressSequence
	 *            Stress sequence.
	 * @param material
	 *            Linear material.
	 * @return Linear equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private LinearEquivalentStress createLinearEquivalentStress(Connection connection, StressSequence stressSequence, LinearMaterial material) throws Exception {

		// save material name to STF files table
		String sql = "update stf_files set linear_material = '" + material.toString() + "' where file_id = " + stressSequence.getParentItem().getID();
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}

		// initialize equivalent stress
		LinearEquivalentStress stress = null;

		// create statement
		sql = "insert into linear_equivalent_stresses";
		sql += "(sth_id, name, remove_negative, omission_level, material_name, material_specification, material_library_version, material_family, material_orientation, material_configuration, ";
		sql += "material_ceff, material_m, material_a, material_b, material_c, material_ftu, material_fty, material_isami_version) ";
		sql += "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// set parameters
			update.setInt(1, stressSequence_.getID()); // stress sequence ID
			String stressName = "Linear Prop. Eq. Stress (" + stressSequence_.getName() + ")";
			update.setString(2, stressName);
			update.setBoolean(3, input_.removeNegativeStresses()); // remove negative stresses
			double omissionLevel = input_.applyOmission() ? input_.getOmissionLevel() : -1.0;
			update.setDouble(4, omissionLevel); // omission level
			update.setString(5, material.getName());
			if (material.getSpecification() == null || material.getSpecification().isEmpty()) {
				update.setNull(6, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(6, material.getSpecification());
			}
			if (material.getLibraryVersion() == null || material.getLibraryVersion().isEmpty()) {
				update.setNull(7, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(7, material.getLibraryVersion());
			}
			if (material.getFamily() == null || material.getFamily().isEmpty()) {
				update.setNull(8, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(8, material.getFamily());
			}
			if (material.getOrientation() == null || material.getOrientation().isEmpty()) {
				update.setNull(9, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(9, material.getOrientation());
			}
			if (material.getConfiguration() == null || material.getConfiguration().isEmpty()) {
				update.setNull(10, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(10, material.getConfiguration());
			}
			update.setDouble(11, material.getCeff());
			update.setDouble(12, material.getM());
			update.setDouble(13, material.getA());
			update.setDouble(14, material.getB());
			update.setDouble(15, material.getC());
			update.setDouble(16, material.getFtu());
			update.setDouble(17, material.getFty());
			if (material.getIsamiVersion() == null || material.getIsamiVersion().isEmpty()) {
				update.setNull(18, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(18, material.getIsamiVersion());
			}

			// execute update
			update.executeUpdate();

			// create equivalent stress
			try (ResultSet resultSet = update.getGeneratedKeys()) {
				while (resultSet.next()) {

					// get name and ID
					int id = resultSet.getBigDecimal(1).intValue();
					String materialName = material.toString();

					// create equivalent stress
					stress = new LinearEquivalentStress(stressName, id, omissionLevel, materialName);
				}
			}
		}

		// return equivalent stress
		return stress;
	}

	/**
	 * Creates and returns external fatigue equivalent stress in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param stressSequence
	 *            External stress sequence.
	 * @param material
	 *            Fatigue material.
	 * @return External fatigue equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ExternalFatigueEquivalentStress createExternalFatigueEquivalentStress(Connection connection, ExternalStressSequence stressSequence, FatigueMaterial material) throws Exception {

		// save material name to STH files table
		String sql = "update ext_sth_files set fatigue_material = '" + material.toString() + "' where file_id = " + stressSequence.getID();
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}

		// initialize equivalent stress
		ExternalFatigueEquivalentStress stress = null;

		// create statement
		sql = "insert into ext_fatigue_equivalent_stresses";
		sql += "(sth_id, name, remove_negative, omission_level, material_name, material_specification, material_library_version, material_family, material_orientation, material_configuration, ";
		sql += "material_p, material_q, material_m, overall_fac, material_isami_version) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// set parameters
			update.setInt(1, stressSequence_.getID()); // stress sequence ID
			String stressName = "Fatigue Eq. Stress (" + stressSequence_.getName() + ")";
			update.setString(2, stressName);
			update.setBoolean(3, input_.removeNegativeStresses()); // remove negative stresses
			double omissionLevel = input_.applyOmission() ? input_.getOmissionLevel() : -1.0;
			update.setDouble(4, omissionLevel); // omission level
			update.setString(5, material.getName());
			if (material.getSpecification() == null || material.getSpecification().isEmpty()) {
				update.setNull(6, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(6, material.getSpecification());
			}
			if (material.getLibraryVersion() == null || material.getLibraryVersion().isEmpty()) {
				update.setNull(7, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(7, material.getLibraryVersion());
			}
			if (material.getFamily() == null || material.getFamily().isEmpty()) {
				update.setNull(8, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(8, material.getFamily());
			}
			if (material.getOrientation() == null || material.getOrientation().isEmpty()) {
				update.setNull(9, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(9, material.getOrientation());
			}
			if (material.getConfiguration() == null || material.getConfiguration().isEmpty()) {
				update.setNull(10, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(10, material.getConfiguration());
			}
			update.setDouble(11, material.getP());
			update.setDouble(12, material.getQ());
			update.setDouble(13, material.getM());
			String stressModifier = input_.getStressModificationValue() + " (" + input_.getStressModificationMethod() + ")";
			update.setString(14, stressModifier);
			if (material.getIsamiVersion() == null || material.getIsamiVersion().isEmpty()) {
				update.setNull(15, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(15, material.getIsamiVersion());
			}

			// execute update
			update.executeUpdate();

			// create equivalent stress
			try (ResultSet resultSet = update.getGeneratedKeys()) {
				while (resultSet.next()) {

					// get name and ID
					int id = resultSet.getBigDecimal(1).intValue();
					String materialName = material.toString();

					// create equivalent stress
					stress = new ExternalFatigueEquivalentStress(stressName, id, omissionLevel, materialName);
				}

			}
		}

		// return equivalent stress
		return stress;
	}

	/**
	 * Creates and returns external preffas equivalent stress in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param stressSequence
	 *            External stress sequence.
	 * @param material
	 *            Preffas material.
	 * @return External preffas equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ExternalPreffasEquivalentStress createExternalPreffasEquivalentStress(Connection connection, ExternalStressSequence stressSequence, PreffasMaterial material) throws Exception {

		// save material name to STH files table
		String sql = "update ext_sth_files set preffas_material = '" + material.toString() + "' where file_id = " + stressSequence.getID();
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}

		// initialize equivalent stress
		ExternalPreffasEquivalentStress stress = null;

		// create statement
		sql = "insert into ext_preffas_equivalent_stresses";
		sql += "(sth_id, name, remove_negative, omission_level, material_name, material_specification, material_library_version, material_family, material_orientation, material_configuration, ";
		sql += "material_ceff, material_m, material_a, material_b, material_c, material_ftu, material_fty, overall_fac, material_isami_version) ";
		sql += "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// set parameters
			update.setInt(1, stressSequence_.getID()); // stress sequence ID
			String stressName = "Preffas Eq. Stress (" + stressSequence_.getName() + ")";
			update.setString(2, stressName);
			update.setBoolean(3, input_.removeNegativeStresses()); // remove negative stresses
			double omissionLevel = input_.applyOmission() ? input_.getOmissionLevel() : -1.0;
			update.setDouble(4, omissionLevel); // omission level
			update.setString(5, material.getName());
			if (material.getSpecification() == null || material.getSpecification().isEmpty()) {
				update.setNull(6, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(6, material.getSpecification());
			}
			if (material.getLibraryVersion() == null || material.getLibraryVersion().isEmpty()) {
				update.setNull(7, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(7, material.getLibraryVersion());
			}
			if (material.getFamily() == null || material.getFamily().isEmpty()) {
				update.setNull(8, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(8, material.getFamily());
			}
			if (material.getOrientation() == null || material.getOrientation().isEmpty()) {
				update.setNull(9, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(9, material.getOrientation());
			}
			if (material.getConfiguration() == null || material.getConfiguration().isEmpty()) {
				update.setNull(10, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(10, material.getConfiguration());
			}
			update.setDouble(11, material.getCeff());
			update.setDouble(12, material.getM());
			update.setDouble(13, material.getA());
			update.setDouble(14, material.getB());
			update.setDouble(15, material.getC());
			update.setDouble(16, material.getFtu());
			update.setDouble(17, material.getFty());
			String stressModifier = input_.getStressModificationValue() + " (" + input_.getStressModificationMethod() + ")";
			update.setString(18, stressModifier);
			if (material.getIsamiVersion() == null || material.getIsamiVersion().isEmpty()) {
				update.setNull(19, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(19, material.getIsamiVersion());
			}

			// execute update
			update.executeUpdate();

			// create equivalent stress
			try (ResultSet resultSet = update.getGeneratedKeys()) {
				while (resultSet.next()) {

					// get name and ID
					int id = resultSet.getBigDecimal(1).intValue();
					String materialName = material.toString();

					// create equivalent stress
					stress = new ExternalPreffasEquivalentStress(stressName, id, omissionLevel, materialName);
				}
			}
		}

		// return equivalent stress
		return stress;
	}

	/**
	 * Creates and returns external linear equivalent stress in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param stressSequence
	 *            External stress sequence.
	 * @param material
	 *            Linear material.
	 * @return External linear equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ExternalLinearEquivalentStress createExternalLinearEquivalentStress(Connection connection, ExternalStressSequence stressSequence, LinearMaterial material) throws Exception {

		// save material name to STH files table
		String sql = "update ext_sth_files set linear_material = '" + material.toString() + "' where file_id = " + stressSequence.getID();
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}

		// initialize equivalent stress
		ExternalLinearEquivalentStress stress = null;

		// create statement
		sql = "insert into ext_linear_equivalent_stresses";
		sql += "(sth_id, name, remove_negative, omission_level, material_name, material_specification, material_library_version, material_family, material_orientation, material_configuration, ";
		sql += "material_ceff, material_m, material_a, material_b, material_c, material_ftu, material_fty, overall_fac, material_isami_version) ";
		sql += "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// set parameters
			update.setInt(1, stressSequence_.getID()); // stress sequence ID
			String stressName = "Linear Prop. Eq. Stress (" + stressSequence_.getName() + ")";
			update.setString(2, stressName);
			update.setBoolean(3, input_.removeNegativeStresses()); // remove negative stresses
			double omissionLevel = input_.applyOmission() ? input_.getOmissionLevel() : -1.0;
			update.setDouble(4, omissionLevel); // omission level
			update.setString(5, material.getName());
			if (material.getSpecification() == null || material.getSpecification().isEmpty()) {
				update.setNull(6, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(6, material.getSpecification());
			}
			if (material.getLibraryVersion() == null || material.getLibraryVersion().isEmpty()) {
				update.setNull(7, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(7, material.getLibraryVersion());
			}
			if (material.getFamily() == null || material.getFamily().isEmpty()) {
				update.setNull(8, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(8, material.getFamily());
			}
			if (material.getOrientation() == null || material.getOrientation().isEmpty()) {
				update.setNull(9, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(9, material.getOrientation());
			}
			if (material.getConfiguration() == null || material.getConfiguration().isEmpty()) {
				update.setNull(10, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(10, material.getConfiguration());
			}
			update.setDouble(11, material.getCeff());
			update.setDouble(12, material.getM());
			update.setDouble(13, material.getA());
			update.setDouble(14, material.getB());
			update.setDouble(15, material.getC());
			update.setDouble(16, material.getFtu());
			update.setDouble(17, material.getFty());
			String stressModifier = input_.getStressModificationValue() + " (" + input_.getStressModificationMethod() + ")";
			update.setString(18, stressModifier);
			if (material.getIsamiVersion() == null || material.getIsamiVersion().isEmpty()) {
				update.setNull(19, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(19, material.getIsamiVersion());
			}

			// execute update
			update.executeUpdate();

			// create equivalent stress
			try (ResultSet resultSet = update.getGeneratedKeys()) {
				while (resultSet.next()) {

					// get name and ID
					int id = resultSet.getBigDecimal(1).intValue();
					String materialName = material.toString();

					// create equivalent stress
					stress = new ExternalLinearEquivalentStress(stressName, id, omissionLevel, materialName);
				}
			}
		}

		// return equivalent stress
		return stress;
	}

	/**
	 * Returns maximum number of peaks per typical flight. This is used to determine if the extended in-build engine should be used for analysis.
	 *
	 * @param connection
	 *            Database connection.
	 * @return Maximum number of peaks per typical flight.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int getMaxPeaksPerFlight(Connection connection) throws Exception {

		// progress info
		updateMessage("Getting maximum number of peaks per typical flight...");

		// initialize max peaks
		int maxPeaks = 0;

		// create statement
		try (Statement statement = connection.createStatement()) {

			// set column name
			String colName = null;
			if (stressSequence_ instanceof StressSequence) {
				colName = "sth_flights";
			}
			else if (stressSequence_ instanceof ExternalStressSequence) {
				colName = "ext_sth_flights";
			}

			// create and execute statement
			String sql = "select num_peaks from " + colName + " where file_id = " + stressSequence_.getID() + " order by num_peaks desc";
			statement.setMaxRows(1);
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					maxPeaks = resultSet.getInt("num_peaks");
				}
			}

			// reset statement
			statement.setMaxRows(0);
		}

		// return max peaks
		return maxPeaks;
	}

	/**
	 * Runs rainflow process in a separate thread.
	 *
	 * @param connection
	 *            Database connection.
	 * @param inputSTH
	 *            Input STH file.
	 * @param eqStress
	 *            Equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void rainflow(Connection connection, Path inputSTH, SpectrumItem eqStress) throws Exception {

		// create rainflow process
		rainflow_ = new Rainflow(this, inputSTH, eqStress);

		// run in a separate thread
		new Thread(() -> {

			// start process
			try {
				rainflow_.start(connection);
				rainflowCompleted(null);
			}

			// process failed
			catch (Exception e) {
				rainflowCompleted(e);
			}
		}).start();
	}

	/**
	 * Called when rainflow process has ended.
	 *
	 * @param e
	 *            Exception if rainflow process fails.
	 */
	private void rainflowCompleted(Exception e) {
		rainflowException_ = e;
		rainflowCompleted_ = true;
	}

	/**
	 * Runs equivalent stress analysis process in a separate thread.
	 *
	 * @param connection
	 *            Database connection.
	 * @param inputSTH
	 *            Input STH file.
	 * @param eqStress
	 *            Equivalent stress.
	 * @param maxPeaks
	 *            Maximum number of peaks per typical flight.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void equivalentStressAnalysis(Connection connection, Path inputSTH, SpectrumItem eqStress, int maxPeaks) throws Exception {

		// get selected analysis engine
		boolean isFallback = (boolean) taskPanel_.getOwner().getOwner().getSettings().getValue(Settings.FALLBACK_TO_INBUILT);
		boolean keepOutputs = (boolean) taskPanel_.getOwner().getOwner().getSettings().getValue(Settings.KEEP_ANALYSIS_OUTPUTS);

		// generate output file name
		String outputFileName = !keepOutputs ? null : generateOutputFileName(analysisEngine_.getOutputFileType(), stressSequence_, input_.getMaterial());

		// get FLS file ID
		int flsFileID = -1;
		if (stressSequence_ instanceof StressSequence) {
			StressSequence sequence = (StressSequence) stressSequence_;
			flsFileID = sequence.getParentItem().getParentItem().getFLSFileID();
		}
		else if (stressSequence_ instanceof ExternalStressSequence) {
			flsFileID = stressSequence_.getID();
		}

		// ISAMI engine
		if (analysisEngine_.equals(AnalysisEngine.ISAMI)) {

			// maximum allowed number of peaks exceeded
			if (maxPeaks > MAX_PEAKS) {
				addWarning("Maximum number of allowed peaks per typical flight (100000) is exceeded. Executing extended inbuilt analysis engine.");
				equivalentStressAnalysis_ = new InbuiltESA(this, inputSTH, eqStress, flsFileID, input_.getMaterial(), true, keepOutputs, outputFileName);
			}

			// number of peaks within limits
			else {

				// connected to server (ISAMI engine)
				if (taskPanel_.getOwner().getOwner().getAnalysisServerManager().isConnected()) {
					equivalentStressAnalysis_ = new IsamiESA(this, inputSTH, stressSequence_, eqStress, flsFileID, input_.getMaterial(), keepOutputs, outputFileName, isamiVersion_, isamiSubVersion_, applyCompression_);
				}

				// not connected (inbuilt engine)
				else if (isFallback) {
					addWarning("Cannot connect to analysis server. Falling back to inbuilt analysis engine.");
					equivalentStressAnalysis_ = new InbuiltESA(this, inputSTH, eqStress, flsFileID, input_.getMaterial(), false, keepOutputs, outputFileName);
				}

				// fallback not selected
				else
					throw new Exception("Cannot connect to analysis server.");
			}
		}

		// SAFE engine
		else if (analysisEngine_.equals(AnalysisEngine.SAFE)) {

			// maximum allowed number of peaks exceeded
			if (maxPeaks > MAX_PEAKS) {
				addWarning("Maximum number of allowed peaks per typical flight (100000) is exceeded. Executing extended inbuilt analysis engine.");
				equivalentStressAnalysis_ = new InbuiltESA(this, inputSTH, eqStress, flsFileID, input_.getMaterial(), true, keepOutputs, outputFileName);
			}

			// number of peaks within limits
			else {

				// connected to server (SAFE engine)
				if (taskPanel_.getOwner().getOwner().getAnalysisServerManager().isConnected()) {
					equivalentStressAnalysis_ = new SafeESA(this, inputSTH, eqStress, flsFileID, input_.getMaterial(), keepOutputs, outputFileName);
				}

				// not connected (inbuilt engine)
				else if (isFallback) {
					addWarning("Cannot connect to analysis server. Falling back to inbuilt analysis engine.");
					equivalentStressAnalysis_ = new InbuiltESA(this, inputSTH, eqStress, flsFileID, input_.getMaterial(), false, keepOutputs, outputFileName);
				}

				// fallback not selected
				else
					throw new Exception("Cannot connect to analysis server.");
			}
		}

		// inbuilt engine
		else {
			equivalentStressAnalysis_ = new InbuiltESA(this, inputSTH, eqStress, flsFileID, input_.getMaterial(), maxPeaks > MAX_PEAKS, keepOutputs, outputFileName);
		}

		// run in a separate thread
		new Thread(() -> {

			// start process
			try {
				equivalentStressAnalysis_.start(connection);
				equivalentStressAnalysisCompleted(null);
			}

			// process failed
			catch (Exception e) {
				equivalentStressAnalysisCompleted(e);
			}
		}).start();
	}

	/**
	 * Generates analysis output file name.
	 *
	 * @param outputFileType
	 *            Output file type.
	 * @param stressSequence
	 *            Stress sequence.
	 * @param material
	 *            Material.
	 * @return The generated analysis output file name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String generateOutputFileName(FileType outputFileType, SpectrumItem stressSequence, Material material) throws Exception {

		// add equivalent stress type
		String fileName = null;
		if (material instanceof FatigueMaterial) {
			fileName = "Fatigue_";
		}
		else if (material instanceof PreffasMaterial) {
			fileName = "Preffas_";
		}
		else if (material instanceof LinearMaterial) {
			fileName = "Linear_";
		}

		// add parent item name
		if (stressSequence instanceof StressSequence) {
			fileName += FileType.getNameWithoutExtension(((StressSequence) stressSequence).getParentItem().getName());
		}
		else if (stressSequence instanceof ExternalStressSequence) {
			fileName += FileType.getNameWithoutExtension(((ExternalStressSequence) stressSequence).getName());
		}

		// add file extension and return
		return FileType.appendExtension(fileName, outputFileType);
	}

	/**
	 * Called when equivalent stress analysis process has ended.
	 *
	 * @param e
	 *            Exception if equivalent stress analysis process fails.
	 */
	private void equivalentStressAnalysisCompleted(Exception e) {
		equivalentStressAnalysisException_ = e;
		equivalentStressAnalysisCompleted_ = true;
	}

	/**
	 * Waits for sub processes to complete.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void waitForProcesses() throws Exception {

		// loop while sub processes are running
		while (!rainflowCompleted_ || !equivalentStressAnalysisCompleted_) {

			// task cancelled
			if (isCancelled())
				return;

			// rainflow failed
			if (rainflowException_ != null)
				throw rainflowException_;

			// equivalent stress analysis failed
			if (equivalentStressAnalysisException_ != null)
				throw equivalentStressAnalysisException_;

			// sleep a bit
			try {
				Thread.sleep(500);
			}

			// task interrupted
			catch (InterruptedException e) {
				if (isCancelled())
					return;
			}
		}
	}
}
