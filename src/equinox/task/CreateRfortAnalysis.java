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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import equinox.Equinox;
import equinox.data.AnalysisEngine;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.fileType.Rfort;
import equinox.data.input.EquivalentStressInput;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.data.input.RfortExtendedInput;
import equinox.data.ui.RfortOmission;
import equinox.data.ui.SerializableRfortPilotPoint;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.serializableTask.SerializableCreateRfortAnalysis;

/**
 * Class for create RFORT analysis task.
 *
 * @author Murat Artim
 * @date Mar 8, 2016
 * @time 12:59:31 PM
 */
public class CreateRfortAnalysis extends InternalEquinoxTask<Rfort> implements ShortRunningTask, SavableTask {

	/** Analysis input. */
	private final RfortExtendedInput input_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/** ISAMI version. */
	private IsamiVersion isamiVersion_;

	/** ISAMI sub version. */
	private IsamiSubVersion isamiSubVersion_;

	/** True compression should be applied in propagation analysis. */
	private boolean applyCompression_;

	/**
	 * Creates create RFORT analysis task.
	 *
	 * @param input
	 *            Analysis input.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public CreateRfortAnalysis(RfortExtendedInput input, AnalysisEngine analysisEngine) {
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
	 * @return This object.
	 */
	public CreateRfortAnalysis setIsamiEngineInputs(IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
		return this;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Create RFORT analysis";
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableCreateRfortAnalysis(input_, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
	}

	@Override
	protected Rfort call() throws Exception {

		// check permission
		checkPermission(Permission.RUN_RFORT_EXTENDED_PLUGIN);

		// update progress
		updateMessage("Creating RFORT analysis for '" + input_.getANAFile().getFileName() + "'...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// create RFORT tables (if necessary)
				createRfortTables(connection);

				// create RFORT analysis
				Rfort rfort = createRfortAnalysis(connection);

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return analysis ID
				return rfort;
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

		// start RFORT analysis
		try {

			// get RFORT file
			Rfort rfort = get();

			// add RFORT file to file tree
			taskPanel_.getOwner().getOwner().getInputPanel().getFileTreeRoot().getChildren().add(rfort);

			// start analysis
			startAnalysis(rfort.getID());
		}

		// exception occurred
		catch (Exception e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Creates RFORT tables if necessary.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void createRfortTables(Connection connection) throws Exception {

		// check if RFORT tables exist
		boolean analysisTableExists = false, outputsTableExists = false;
		DatabaseMetaData dbmtadta = connection.getMetaData();
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "RFORT_%", null)) {
			while (resultSet.next()) {

				// get table name
				String tableName = resultSet.getString(3);

				// analysis table
				if (tableName.equalsIgnoreCase("rfort_analyses")) {
					analysisTableExists = true;
				}
				else if (tableName.equalsIgnoreCase("rfort_outputs")) {
					outputsTableExists = true;
				}
			}
		}

		// no need to create tables
		if (analysisTableExists && outputsTableExists)
			return;

		// create statement
		try (Statement statement = connection.createStatement()) {

			// create analysis table
			if (!analysisTableExists) {
				statement.executeUpdate(
						"CREATE TABLE AURORA.RFORT_ANALYSES(ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), INPUT_DATA BLOB(3M) NOT NULL, INPUT_SPECTRUM_NAME VARCHAR(100) NOT NULL, ADD_DP SMALLINT NOT NULL, REF_DP DOUBLE NOT NULL, DP_FACTOR DOUBLE NOT NULL, OVERALL_FACTOR DOUBLE NOT NULL, STRESS_COMP VARCHAR(50) NOT NULL, ROTATION_ANGLE INT NOT NULL, RUN_TILL_FLIGHT VARCHAR(20), TARGET_FLIGHTS VARCHAR(100), ENABLE_SLOG SMALLINT NOT NULL, FATIGUE_ANALYSIS SMALLINT NOT NULL, PREFFAS_ANALYSIS SMALLINT NOT NULL, LINEAR_ANALYSIS SMALLINT NOT NULL, PRIMARY KEY(ID))");
			}

			// create outputs table
			if (!outputsTableExists) {
				statement.executeUpdate(
						"CREATE TABLE AURORA.RFORT_OUTPUTS(ANALYSIS_ID INT NOT NULL, PP_NAME VARCHAR(100) NOT NULL, STRESS_ID INT NOT NULL, INCLUDED_IN_RFORT SMALLINT NOT NULL, STRESS_FACTOR DOUBLE NOT NULL, MATERIAL_NAME VARCHAR(500) NOT NULL, OMISSION_NAME VARCHAR(100) NOT NULL, OMISSION_TYPE VARCHAR(20) NOT NULL, NUM_PEAKS INT NOT NULL, STRESS_AMP DOUBLE NOT NULL, OMISSION_VALUE DOUBLE NOT NULL, EQ_STRESS DOUBLE NOT NULL, STRESS_TYPE VARCHAR(20) NOT NULL)");
			}
		}
	}

	/**
	 * Creates RFORT analysis.
	 *
	 * @param connection
	 *            Database connection.
	 * @return RFORT file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Rfort createRfortAnalysis(Connection connection) throws Exception {

		// initialize RFORT file
		Rfort rfort = null;

		// create statement
		String sql = "insert into rfort_analyses(input_data, input_spectrum_name, add_dp, ref_dp, dp_factor, overall_factor, stress_comp, rotation_angle, run_till_flight, target_flights, enable_slog, fatigue_analysis, preffas_analysis, linear_analysis) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// insert into database
			String spectrumName = FileType.getNameWithoutExtension(input_.getANAFile());

			// save input data
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
				try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
					oos.writeObject(input_);
					oos.flush();
					byte[] bytes = bos.toByteArray();
					try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
						statement.setBinaryStream(1, bais, bytes.length);
					}
				}
			}

			// set spectrum name
			statement.setString(2, spectrumName);

			// set other options
			statement.setBoolean(3, input_.getAddDP());
			statement.setDouble(4, input_.getRefDP());
			statement.setDouble(5, input_.getDPFactor());
			statement.setDouble(6, input_.getOverallFactor());
			statement.setString(7, input_.getComponent().toString());
			statement.setInt(8, input_.getRotation());
			Integer runTillFlight = input_.getRunTillFlight();
			if (runTillFlight == null) {
				statement.setNull(9, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(9, runTillFlight.toString());
			}
			String targetFlights = input_.getTargetFlights();
			if (targetFlights == null || targetFlights.trim().isEmpty()) {
				statement.setNull(10, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(10, targetFlights);
			}
			statement.setBoolean(11, input_.getEnableSlogMode());
			statement.setBoolean(12, input_.getRunFatigueAnalysis());
			statement.setBoolean(13, input_.getRunPreffasAnalysis());
			statement.setBoolean(14, input_.getRunLinearAnalysis());

			// execute update
			statement.executeUpdate();

			// get analysis ID
			try (ResultSet resultSet = statement.getGeneratedKeys()) {
				while (resultSet.next()) {
					rfort = new Rfort(spectrumName, resultSet.getBigDecimal(1).intValue(), input_.getRunFatigueAnalysis(), input_.getRunPreffasAnalysis(), input_.getRunLinearAnalysis());
				}
			}
		}

		// return RFORT file
		return rfort;
	}

	/**
	 * Creates and runs RFORT tasks.
	 *
	 * @param analysisID
	 *            RFORT analysis ID.
	 * @throws Exception
	 *             If user has insufficient privileges to add new spectrum.
	 */
	private void startAnalysis(int analysisID) throws Exception {

		// create add spectrum task
		Path ana = input_.getANAFile();
		Path txt = input_.getTXTFile();
		Path cvt = input_.getCVTFile();
		Path fls = input_.getFLSFile();
		Path xls = input_.getXLSFile();
		String sheet = input_.getConversionTableSheet();
		AddSpectrum addSpectrum = new AddSpectrum(ana, txt, cvt, fls, xls, sheet, null);

		// create add STF files tasks
		ArrayList<File> stfFiles = new ArrayList<>();
		for (SerializableRfortPilotPoint pp : input_.getPilotPoints()) {
			stfFiles.add(pp.getFile());
		}
		AddSTFFiles addSTFFiles = new AddSTFFiles(stfFiles, null, null);

		// loop over pilot points
		for (SerializableRfortPilotPoint pp : input_.getPilotPoints()) {

			// create generate stress sequence task
			GenerateStressSequenceInput generate = new GenerateStressSequenceInput();

			// set stress factors
			double ppFactor = Double.parseDouble(pp.getFactor());
			double stressFactor = input_.getOverallFactor() * ppFactor;
			double dpStressFactor = input_.getAddDP() ? input_.getDPFactor() * input_.getOverallFactor() * ppFactor : 0.0;
			if (input_.getAddDP()) {
				generate.setReferenceDP(input_.getRefDP());
			}
			generate.setStressModifier(GenerateStressSequenceInput.ONEG, stressFactor, GenerateStressSequenceInput.MULTIPLY);
			generate.setStressModifier(GenerateStressSequenceInput.INCREMENT, stressFactor, GenerateStressSequenceInput.MULTIPLY);
			generate.setStressModifier(GenerateStressSequenceInput.DELTAP, dpStressFactor, GenerateStressSequenceInput.MULTIPLY);
			generate.setStressModifier(GenerateStressSequenceInput.DELTAT, stressFactor, GenerateStressSequenceInput.MULTIPLY);
			generate.setFileName(null);
			generate.setStressComponent(input_.getComponent());
			generate.setRotationAngle(input_.getRotation());
			GenerateStressSequence generateStressSequence = new GenerateStressSequence(null, generate);

			// fatigue equivalent stress analysis
			if (input_.getRunFatigueAnalysis()) {
				EquivalentStressInput fatigueInput = new EquivalentStressInput(false, true, 0.0, pp.getFatigueMaterial());
				EquivalentStressAnalysis fatigueAnalysis = new EquivalentStressAnalysis(null, fatigueInput, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
				SaveRfortInfo saveFatigueInfo = new SaveRfortInfo(input_, null, analysisID, RfortOmission.INITIAL_ANALYSIS, RfortOmission.NO_OMISSION, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
				fatigueAnalysis.addAutomaticTask(Integer.toString(saveFatigueInfo.hashCode()), new EmbeddedTask<>(saveFatigueInfo, ExecutionMode.PARALLEL));
				generateStressSequence.addAutomaticTask(Integer.toString(fatigueAnalysis.hashCode()), new EmbeddedTask<>(fatigueAnalysis, ExecutionMode.PARALLEL));
			}

			// preffas equivalent stress analysis
			if (input_.getRunPreffasAnalysis()) {
				EquivalentStressInput preffasInput = new EquivalentStressInput(false, true, 0.0, pp.getPreffasMaterial());
				EquivalentStressAnalysis preffasAnalysis = new EquivalentStressAnalysis(null, preffasInput, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
				SaveRfortInfo savePreffasInfo = new SaveRfortInfo(input_, null, analysisID, RfortOmission.INITIAL_ANALYSIS, RfortOmission.NO_OMISSION, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
				preffasAnalysis.addAutomaticTask(Integer.toString(savePreffasInfo.hashCode()), new EmbeddedTask<>(savePreffasInfo, ExecutionMode.PARALLEL));
				generateStressSequence.addAutomaticTask(Integer.toString(preffasAnalysis.hashCode()), new EmbeddedTask<>(preffasAnalysis, ExecutionMode.PARALLEL));
			}

			// linear prop. equivalent stress analysis
			if (input_.getRunLinearAnalysis()) {
				EquivalentStressInput linearInput = new EquivalentStressInput(false, true, 0.0, pp.getLinearMaterial());
				EquivalentStressAnalysis linearAnalysis = new EquivalentStressAnalysis(null, linearInput, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
				SaveRfortInfo saveLinearInfo = new SaveRfortInfo(input_, null, analysisID, RfortOmission.INITIAL_ANALYSIS, RfortOmission.NO_OMISSION, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
				linearAnalysis.addAutomaticTask(Integer.toString(saveLinearInfo.hashCode()), new EmbeddedTask<>(saveLinearInfo, ExecutionMode.PARALLEL));
				generateStressSequence.addAutomaticTask(Integer.toString(linearAnalysis.hashCode()), new EmbeddedTask<>(linearAnalysis, ExecutionMode.PARALLEL));
			}

			// add generate stress sequence task to add STF files task
			addSTFFiles.addAutomaticTask(pp.getName(), new EmbeddedTask<>(generateStressSequence, ExecutionMode.PARALLEL));
		}

		// add add STF files task to add spectrum task
		addSpectrum.addAutomaticTask("Add STF Files", new EmbeddedTask<>(addSTFFiles, ExecutionMode.PARALLEL));

		// run task
		taskPanel_.getOwner().runTaskInParallel(addSpectrum);
	}
}
