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
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.AnalysisEngine;
import equinox.data.EmbeddedTask;
import equinox.data.FastESAOutput;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.LoadcaseFactor;
import equinox.data.SegmentFactor;
import equinox.data.Settings;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FlightDamageContributions;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.FastEquivalentStressInput;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.LinearMaterial;
import equinox.dataServer.remote.data.Material;
import equinox.dataServer.remote.data.PreffasMaterial;
import equinox.plugin.FileType;
import equinox.process.ESAProcess;
import equinox.process.InbuiltFastESA;
import equinox.process.InbuiltFlightDCA;
import equinox.process.IsamiFastESA;
import equinox.process.SafeFastESA;
import equinox.process.SafeFlightDCA;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;

/**
 * Class for fast equivalent stress analysis task.
 *
 * @author Murat Artim
 * @date Jun 15, 2016
 * @time 3:27:36 PM
 */
public class FastEquivalentStressAnalysis extends TemporaryFileCreatingTask<SpectrumItem> implements LongRunningTask, AutomaticTaskOwner<SpectrumItem> {

	/** Maximum number of typical flights to store damage contributions. */
	private static final int MAX_TYPICAL_FLIGHT_DAMAGE_CONTRIBUTIONS = 5;

	/** Input. */
	private final FastEquivalentStressInput input_;

	/** The owner STF file. */
	private final STFFile stfFile_;

	/** STF file ID. */
	private final Integer stfID_, anaID_, flsID_;

	/** STF file name. */
	private final String stfName_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/** ISAMI version. */
	private IsamiVersion isamiVersion_;

	/** ISAMI sub version. */
	private IsamiSubVersion isamiSubVersion_;

	/** True compression should be applied in propagation analysis. */
	private boolean applyCompression_;

	/** Input files. */
	private Path sequenceFile_, flsFile_ = null;

	/** Material. */
	private final Material material_;

	/** Spectrum validity. */
	private final int validity_;

	/** True to use extended inbuilt analysis engine. Extended inbuilt engine has no limit on number of peaks per typical flight. */
	private final boolean isFlightDamageContributionAnalysis_, useExtended_, applyOmission_;

	/** Omission level. */
	private final double omissionLevel_;

	/** Equivalent stress analysis process. */
	private ESAProcess<?> analysisProcess_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<SpectrumItem>> automaticTasks_ = null;

	/**
	 * Creates fast equivalent stress analysis task.
	 *
	 * @param stfFile
	 *            The owner STF file.
	 * @param anaID
	 *            ANA file ID.
	 * @param flsID
	 *            FLS file ID.
	 * @param validity
	 *            Spectrum validity.
	 * @param material
	 *            Material.
	 * @param isFlightDamageContributionAnalysis
	 *            True if typical flight damage contribution analysis is requested.
	 * @param useExtended
	 *            True to use extended inbuilt analysis engine. Extended inbuilt engine has no limit on number of peaks per typical flight.
	 * @param applyOmission
	 *            True if omission was applied to input stress time history.
	 * @param omissionLevel
	 *            Omission level.
	 * @param input
	 *            Analysis input.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public FastEquivalentStressAnalysis(STFFile stfFile, int anaID, int flsID, int validity, Material material, boolean isFlightDamageContributionAnalysis, boolean useExtended, boolean applyOmission, double omissionLevel, FastEquivalentStressInput input, AnalysisEngine analysisEngine) {
		stfFile_ = stfFile;
		anaID_ = anaID;
		flsID_ = flsID;
		validity_ = validity;
		material_ = material;
		isFlightDamageContributionAnalysis_ = isFlightDamageContributionAnalysis;
		useExtended_ = useExtended;
		applyOmission_ = applyOmission;
		omissionLevel_ = omissionLevel;
		input_ = input;
		stfID_ = null;
		stfName_ = null;
		analysisEngine_ = analysisEngine;
	}

	/**
	 * Creates fast equivalent stress analysis task.
	 *
	 * @param stfID
	 *            STF file ID.
	 * @param anaID
	 *            ANA file ID.
	 * @param flsID
	 *            FLS file ID.
	 * @param stfName
	 *            STF file name.
	 * @param validity
	 *            Spectrum validity.
	 * @param material
	 *            Material.
	 * @param isFlightDamageContributionAnalysis
	 *            True if typical flight damage contribution analysis is requested.
	 * @param useExtended
	 *            True to use extended inbuilt analysis engine. Extended inbuilt engine has no limit on number of peaks per typical flight.
	 * @param applyOmission
	 *            True if omission was applied to input stress time history.
	 * @param omissionLevel
	 *            Omission level.
	 * @param input
	 *            Analysis input.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public FastEquivalentStressAnalysis(int stfID, int anaID, int flsID, String stfName, int validity, Material material, boolean isFlightDamageContributionAnalysis, boolean useExtended, boolean applyOmission, double omissionLevel, FastEquivalentStressInput input, AnalysisEngine analysisEngine) {
		stfID_ = stfID;
		anaID_ = anaID;
		flsID_ = flsID;
		stfName_ = stfName;
		validity_ = validity;
		material_ = material;
		isFlightDamageContributionAnalysis_ = isFlightDamageContributionAnalysis;
		useExtended_ = useExtended;
		applyOmission_ = applyOmission;
		omissionLevel_ = omissionLevel;
		input_ = input;
		stfFile_ = null;
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
	public FastEquivalentStressAnalysis setIsamiEngineInputs(IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
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

	/**
	 * Sets stress sequence file. This can be either SIGMA or STH file.
	 *
	 * @param sequenceFile
	 *            Stress sequence file. This can be either SIGMA or STH file.
	 */
	public void setSequenceFile(Path sequenceFile) {
		sequenceFile_ = sequenceFile;
	}

	/**
	 * Sets FLS file.
	 *
	 * @param flsFile
	 *            FLS file.
	 */
	public void setFLSFile(Path flsFile) {
		flsFile_ = flsFile;
	}

	@Override
	public String getTaskTitle() {
		String title = " analysis for '" + (stfFile_ == null ? stfName_ : stfFile_.getName()) + "'";
		if (material_ instanceof FatigueMaterial) {
			title = "Fatigue" + title;
		}
		else if (material_ instanceof PreffasMaterial) {
			title = "Preffas prop." + title;
		}
		else if (material_ instanceof LinearMaterial) {
			title = "Linear prop." + title;
		}
		return title;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected SpectrumItem call() throws Exception {

		// check permission
		checkPermission(Permission.EQUIVALENT_STRESS_ANALYSIS);

		// initialize spectrum item
		SpectrumItem spectrumItem = null;

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// equivalent stress analysis
			if (!isFlightDamageContributionAnalysis_) {
				spectrumItem = equivalentStressAnalysis(connection);
			}

			// flight damage contribution analysis
			else {
				spectrumItem = flightDamageContributionAnalysis(connection);
			}
		}

		// return spectrum item
		return spectrumItem;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// add equivalent stress or flight damage contribution to file tree
		try {

			// get item
			SpectrumItem item = get();

			// add to STF file
			if (stfFile_ != null) {
				stfFile_.getChildren().add(item);
			}

			// plot and save contributions
			if (item instanceof FlightDamageContributions) {
				postProcess((FlightDamageContributions) item);
			}

			// manage automatic tasks
			if (automaticTasks_ != null) {
				automaticTaskOwnerSucceeded(item, automaticTasks_, taskPanel_);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Post-processes results of this task.
	 *
	 * @param result
	 *            Task result.
	 */
	private void postProcess(FlightDamageContributions result) {

		// plot and save contributions
		try {
			taskPanel_.getOwner().runTaskSilently(new SaveFlightDamageContributionPlot(result, true, null), false).get();
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

		// destroy sub processes (if still running)
		if (analysisProcess_ != null) {
			analysisProcess_.cancel();
		}

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// destroy sub processes (if still running)
		if (analysisProcess_ != null) {
			analysisProcess_.cancel();
		}

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	/**
	 * Starts equivalent stress analysis.
	 *
	 * @param connection
	 *            Database connection.
	 * @return The resulting equivalent stress item.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private SpectrumItem flightDamageContributionAnalysis(Connection connection) throws Exception {

		// run analysis
		Object[] contributions = runFlightDamageContributionAnalysis(connection);

		// task cancelled
		if (isCancelled() || contributions == null || contributions.length == 0)
			return null;

		try {

			// disable auto-commit
			connection.setAutoCommit(false);

			// save equivalent stress
			FlightDamageContributions flightDamCont = createFlightDamageContribution(contributions, connection);

			// task cancelled
			if (isCancelled() || flightDamCont == null) {
				connection.rollback();
				connection.setAutoCommit(true);
				return null;
			}

			// commit updates
			connection.commit();
			connection.setAutoCommit(true);

			// return flight damage contributions
			return flightDamCont;
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

	/**
	 * Creates and returns the typical flight damage contributions.
	 *
	 * @param contributions
	 *            Mapping containing the typical flight names versus their contribution percentages.
	 * @param connection
	 *            Database connection.
	 * @return The newly created typical flight damage contributions.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	@SuppressWarnings("unchecked")
	private FlightDamageContributions createFlightDamageContribution(Object[] contributions, Connection connection) throws Exception {

		// update progress info
		updateMessage("Saving typicalflight damage contributions info to database...");

		// save material name to STF files table
		int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
		FatigueMaterial material = (FatigueMaterial) material_;
		String sql = "update stf_files set fatigue_material = '" + material.toString() + "' where file_id = " + stfID;
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}

		// insert damage contributions inputs
		FlightDamageContributions damConts = null;
		sql = "insert into flight_dam_contributions(stf_id, name, oneg_fac, inc_fac, dp_fac, dt_fac, ref_dp, dp_lc, dt_lc_inf, dt_lc_sup, ref_dt_inf, ";
		sql += "ref_dt_sup, stress_comp, rotation_angle, validity, remove_negative, omission_level, material_name, material_specification, material_library_version, ";
		sql += "material_family, material_orientation, material_configuration, material_p, material_q, material_m, material_isami_version) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement insertDamageConts = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// set parameters
			insertDamageConts.setInt(1, stfID);
			String stfName = stfFile_ == null ? stfName_ : stfFile_.getName();
			String contributionName = "Flight Damage Contributions (" + stfName + ")";
			insertDamageConts.setString(2, contributionName);
			String stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.ONEG) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.ONEG) + ")";
			insertDamageConts.setString(3, stressModifier); // oneg_fac
			stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.INCREMENT) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.INCREMENT) + ")";
			insertDamageConts.setString(4, stressModifier); // inc_fac
			stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.DELTAP) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.DELTAP) + ")";
			insertDamageConts.setString(5, stressModifier); // dp_fac
			stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.DELTAT) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.DELTAT) + ")";
			insertDamageConts.setString(6, stressModifier); // dt_fac
			insertDamageConts.setDouble(7, input_.getReferenceDP() == null ? 0.0 : input_.getReferenceDP()); // ref_dp
			if (input_.getDPLoadcase() == null) {
				insertDamageConts.setNull(8, java.sql.Types.VARCHAR);
			}
			else {
				insertDamageConts.setString(8, input_.getDPLoadcase()); // dp_lc
			}
			insertDamageConts.setString(13, input_.getStressComponent().toString()); // stress_comp
			insertDamageConts.setDouble(14, input_.getRotationAngle()); // rotation_angle
			insertDamageConts.setDouble(15, validity_); // validity
			insertDamageConts.setBoolean(16, input_.isRemoveNegativeStresses()); // remove negative stresses
			insertDamageConts.setDouble(17, omissionLevel_); // omission level

			// dt_lc_inf
			if (input_.getDTLoadcaseInf() == null) {
				insertDamageConts.setNull(9, java.sql.Types.VARCHAR);
			}
			else {
				insertDamageConts.setString(9, input_.getDTLoadcaseInf());
			}

			// dt_lc_sup
			if (input_.getDTLoadcaseSup() == null) {
				insertDamageConts.setNull(10, java.sql.Types.VARCHAR);
			}
			else {
				insertDamageConts.setString(10, input_.getDTLoadcaseSup());
			}

			// ref_dt_inf
			if (input_.getReferenceDTInf() == null) {
				insertDamageConts.setNull(11, java.sql.Types.DOUBLE);
			}
			else {
				insertDamageConts.setDouble(11, input_.getReferenceDTInf());
			}

			// ref_dt_sup
			if (input_.getReferenceDTSup() == null) {
				insertDamageConts.setNull(12, java.sql.Types.DOUBLE);
			}
			else {
				insertDamageConts.setDouble(12, input_.getReferenceDTSup());
			}

			// material info
			insertDamageConts.setString(18, material.getName());
			if (material.getSpecification() == null || material.getSpecification().isEmpty()) {
				insertDamageConts.setNull(19, java.sql.Types.VARCHAR);
			}
			else {
				insertDamageConts.setString(19, material.getSpecification());
			}
			if (material.getLibraryVersion() == null || material.getLibraryVersion().isEmpty()) {
				insertDamageConts.setNull(20, java.sql.Types.VARCHAR);
			}
			else {
				insertDamageConts.setString(20, material.getLibraryVersion());
			}
			if (material.getFamily() == null || material.getFamily().isEmpty()) {
				insertDamageConts.setNull(21, java.sql.Types.VARCHAR);
			}
			else {
				insertDamageConts.setString(21, material.getFamily());
			}
			if (material.getOrientation() == null || material.getOrientation().isEmpty()) {
				insertDamageConts.setNull(22, java.sql.Types.VARCHAR);
			}
			else {
				insertDamageConts.setString(22, material.getOrientation());
			}
			if (material.getConfiguration() == null || material.getConfiguration().isEmpty()) {
				insertDamageConts.setNull(23, java.sql.Types.VARCHAR);
			}
			else {
				insertDamageConts.setString(23, material.getConfiguration());
			}
			insertDamageConts.setDouble(24, material.getP());
			insertDamageConts.setDouble(25, material.getQ());
			insertDamageConts.setDouble(26, material.getM());
			if (material.getIsamiVersion() == null || material.getIsamiVersion().isEmpty()) {
				insertDamageConts.setNull(27, java.sql.Types.VARCHAR);
			}
			else {
				insertDamageConts.setString(27, material.getIsamiVersion());
			}
			insertDamageConts.executeUpdate();

			// create equivalent stress
			try (ResultSet resultSet = insertDamageConts.getGeneratedKeys()) {
				if (resultSet.next()) {
					damConts = new FlightDamageContributions(contributionName, resultSet.getBigDecimal(1).intValue(), material.toString());
				}
			}
		}

		// null contributions
		if (damConts == null)
			throw new Exception("Cannot save typical flight damage contribution inputs to database.");

		// insert loadcase factor information
		if (input_.getLoadcaseFactors() != null) {
			sql = "insert into flight_dam_contributions_event_modifiers(id, loadcase_number, event_name, comment, value, method) values(?, ?, ?, ?, ?, ?)";
			try (PreparedStatement update = connection.prepareStatement(sql)) {
				for (LoadcaseFactor eFactor : input_.getLoadcaseFactors()) {
					update.setInt(1, damConts.getID()); // contribution ID
					update.setString(2, eFactor.getLoadcaseNumber()); // loadcase number
					if (eFactor.getEventName() != null) {
						update.setString(3, eFactor.getEventName());
					}
					else {
						update.setNull(3, java.sql.Types.VARCHAR);
					}
					if (eFactor.getComments() != null) {
						update.setString(4, eFactor.getComments());
					}
					else {
						update.setNull(4, java.sql.Types.VARCHAR);
					}
					update.setDouble(5, eFactor.getModifierValue()); // value
					update.setString(6, eFactor.getModifierMethod()); // method
					update.executeUpdate();
				}
			}
		}

		// add segment factor information
		if (input_.getSegmentFactors() != null) {
			sql = "insert into flight_dam_contributions_segment_modifiers(id, segment_name, segment_number, oneg_value, inc_value, dp_value, dt_value, oneg_method, inc_method, dp_method, dt_method) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement update = connection.prepareStatement(sql)) {
				for (SegmentFactor sFactor : input_.getSegmentFactors()) {
					update.setInt(1, damConts.getID()); // contribution ID
					update.setString(2, sFactor.getSegment().getName()); // segment name
					update.setInt(3, sFactor.getSegment().getSegmentNumber()); // segment number
					update.setDouble(4, sFactor.getModifierValue(GenerateStressSequenceInput.ONEG)); // 1g value
					update.setDouble(5, sFactor.getModifierValue(GenerateStressSequenceInput.INCREMENT)); // increment value
					update.setDouble(6, sFactor.getModifierValue(GenerateStressSequenceInput.DELTAP)); // delta-p value
					update.setDouble(7, sFactor.getModifierValue(GenerateStressSequenceInput.DELTAT)); // delta-t value
					update.setString(8, sFactor.getModifierMethod(GenerateStressSequenceInput.ONEG)); // 1g method
					update.setString(9, sFactor.getModifierMethod(GenerateStressSequenceInput.INCREMENT)); // increment method
					update.setString(10, sFactor.getModifierMethod(GenerateStressSequenceInput.DELTAP)); // delta-p method
					update.setString(11, sFactor.getModifierMethod(GenerateStressSequenceInput.DELTAT)); // delta-t method
					update.executeUpdate();
				}
			}
		}

		// prepare statement to insert typical flight damage percentages with
		// occurrences
		Map<String, Double> contributionsWithOccurrences = (Map<String, Double>) contributions[0];
		sql = "insert into flight_dam_contribution_with_occurrences(id, flight_name, dam_percent) values(?, ?, ?)";
		try (PreparedStatement insertDamage = connection.prepareStatement(sql)) {

			// set equivalent stress ID
			insertDamage.setInt(1, damConts.getID());

			// insert typical flight damage percentages
			int k = 0;
			Iterator<Entry<String, Double>> iterator = contributionsWithOccurrences.entrySet().iterator();
			while (iterator.hasNext()) {
				if (k >= MAX_TYPICAL_FLIGHT_DAMAGE_CONTRIBUTIONS) {
					break;
				}
				Entry<String, Double> entry = iterator.next();
				insertDamage.setString(2, entry.getKey());
				insertDamage.setDouble(3, entry.getValue());
				insertDamage.executeUpdate();
				k++;
			}
		}

		// prepare statement to insert typical flight damage percentages without
		// occurrences
		Map<String, Double> contributionsWithoutOccurrences = (Map<String, Double>) contributions[1];
		sql = "insert into flight_dam_contribution_without_occurrences(id, flight_name, dam_percent) values(?, ?, ?)";
		try (PreparedStatement insertDamage = connection.prepareStatement(sql)) {

			// set equivalent stress ID
			insertDamage.setInt(1, damConts.getID());

			// insert typical flight damage percentages
			int k = 0;
			Iterator<Entry<String, Double>> iterator = contributionsWithoutOccurrences.entrySet().iterator();
			while (iterator.hasNext()) {
				if (k >= MAX_TYPICAL_FLIGHT_DAMAGE_CONTRIBUTIONS) {
					break;
				}
				Entry<String, Double> entry = iterator.next();
				insertDamage.setString(2, entry.getKey());
				insertDamage.setDouble(3, entry.getValue());
				insertDamage.executeUpdate();
				k++;
			}
		}

		// return damage contributions
		return damConts;
	}

	/**
	 * Runs typical flight damage contribution analysis.
	 *
	 * @param connection
	 *            Database connection.
	 * @return A mapping containing typical flight names versus their damage percentages in descending order.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Object[] runFlightDamageContributionAnalysis(Connection connection) throws Exception {

		// get selected analysis engine
		boolean isFallback = (boolean) taskPanel_.getOwner().getOwner().getSettings().getValue(Settings.FALLBACK_TO_INBUILT);
		boolean keepFailedOutputs = (boolean) taskPanel_.getOwner().getOwner().getSettings().getValue(Settings.KEEP_ANALYSIS_OUTPUTS);

		// ISAMI engine
		if (analysisEngine_.equals(AnalysisEngine.ISAMI)) {
			analysisProcess_ = new InbuiltFlightDCA(this, sequenceFile_, flsFile_, flsID_, (FatigueMaterial) material_, validity_, useExtended_, keepFailedOutputs);
		}

		// SAFE engine
		else if (analysisEngine_.equals(AnalysisEngine.SAFE)) {

			// maximum allowed number of peaks exceeded
			if (useExtended_) {
				addWarning("Maximum number of allowed peaks per typical flight (100000) is exceeded. Executing extended inbuilt Equinox analysis engine.");
				analysisProcess_ = new InbuiltFlightDCA(this, sequenceFile_, flsFile_, flsID_, (FatigueMaterial) material_, validity_, useExtended_, keepFailedOutputs);
			}

			// number of peaks within limits
			else {

				// connected to server (SAFE engine)
				if (taskPanel_.getOwner().getOwner().getAnalysisServerManager().isConnected()) {
					analysisProcess_ = new SafeFlightDCA(this, sequenceFile_, flsFile_, flsID_, (FatigueMaterial) material_, keepFailedOutputs);
				}

				// not connected (inbuilt engine)
				else if (isFallback) {
					addWarning("Cannot connect to analysis server. Falling back to inbuilt Equinox analysis engine.");
					analysisProcess_ = new InbuiltFlightDCA(this, sequenceFile_, flsFile_, flsID_, (FatigueMaterial) material_, validity_, useExtended_, keepFailedOutputs);
				}

				// fallback not selected
				else
					throw new Exception("Cannot connect to Equinox server.");
			}
		}

		// inbuilt engine
		else {
			analysisProcess_ = new InbuiltFlightDCA(this, sequenceFile_, flsFile_, flsID_, (FatigueMaterial) material_, validity_, useExtended_, keepFailedOutputs);
		}

		// run and return results of process
		return (Object[]) analysisProcess_.start(connection);
	}

	/**
	 * Starts equivalent stress analysis.
	 *
	 * @param connection
	 *            Database connection.
	 * @return The resulting equivalent stress item.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private SpectrumItem equivalentStressAnalysis(Connection connection) throws Exception {

		// run equivalent stress analysis
		FastESAOutput output = runEquivalentStressAnalysis(connection);

		// task cancelled
		if (isCancelled() || output == null || output.getStress() == null)
			return null;

		try {

			// disable auto-commit
			connection.setAutoCommit(false);

			// save equivalent stress
			SpectrumItem eqStress = createEquivalentStress(output, connection);

			// task cancelled
			if (isCancelled() || eqStress == null) {
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

	/**
	 * Creates equivalent stress in the database.
	 *
	 * @param output
	 *            Fast equivalent stress output.
	 * @param connection
	 *            Database connection.
	 * @return Newly created equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private SpectrumItem createEquivalentStress(FastESAOutput output, Connection connection) throws Exception {

		// update info
		updateMessage("Saving equivalent stress info to database...");

		// fatigue equivalent stress
		if (material_ instanceof FatigueMaterial)
			return createFatigueEquivalentStress(connection, output, (FatigueMaterial) material_);

		// preffas equivalent stress
		else if (material_ instanceof PreffasMaterial)
			return createPreffasEquivalentStress(connection, output, (PreffasMaterial) material_);

		// linear equivalent stress
		else if (material_ instanceof LinearMaterial)
			return createLinearEquivalentStress(connection, output, (LinearMaterial) material_);
		return null;
	}

	/**
	 * Creates and returns linear equivalent stress in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param output
	 *            Fast equivalent stress output.
	 * @param material
	 *            Linear material.
	 * @return Linear equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private FastLinearEquivalentStress createLinearEquivalentStress(Connection connection, FastESAOutput output, LinearMaterial material) throws Exception {

		// save material name to STF files table
		int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
		String sql = "update stf_files set linear_material = '" + material.toString() + "' where file_id = " + stfID;
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}

		// initialize equivalent stress
		FastLinearEquivalentStress eqStress = null;

		// create statement
		sql = "insert into fast_linear_equivalent_stresses";
		sql += "(stf_id, name, stress, validity, remove_negative, omission_level, material_name, material_specification, material_library_version, material_family, material_orientation, material_configuration, ";
		sql += "material_ceff, material_m, material_a, material_b, material_c, material_ftu, material_fty, analysis_input, output_file_id, material_isami_version) ";
		sql += "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// set parameters
			update.setInt(1, stfID); // STF ID
			String stfName = stfFile_ == null ? stfName_ : stfFile_.getName();
			String stressName = "Linear Prop. Eq. Stress (" + stfName + ")";
			update.setString(2, stressName);
			update.setDouble(3, output.getStress());
			update.setDouble(4, validity_);
			update.setBoolean(5, input_.isRemoveNegativeStresses()); // remove negative stresses
			double omissionLevel = applyOmission_ ? omissionLevel_ : -1.0;
			update.setDouble(6, omissionLevel); // omission level
			update.setString(7, material.getName());
			if (material.getSpecification() == null || material.getSpecification().isEmpty()) {
				update.setNull(8, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(8, material.getSpecification());
			}
			if (material.getLibraryVersion() == null || material.getLibraryVersion().isEmpty()) {
				update.setNull(9, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(9, material.getLibraryVersion());
			}
			if (material.getFamily() == null || material.getFamily().isEmpty()) {
				update.setNull(10, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(10, material.getFamily());
			}
			if (material.getOrientation() == null || material.getOrientation().isEmpty()) {
				update.setNull(11, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(11, material.getOrientation());
			}
			if (material.getConfiguration() == null || material.getConfiguration().isEmpty()) {
				update.setNull(12, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(12, material.getConfiguration());
			}
			update.setDouble(13, material.getCeff());
			update.setDouble(14, material.getM());
			update.setDouble(15, material.getA());
			update.setDouble(16, material.getB());
			update.setDouble(17, material.getC());
			update.setDouble(18, material.getFtu());
			update.setDouble(19, material.getFty());

			// save analysis input data
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
				try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
					oos.writeObject(input_);
					oos.flush();
					byte[] bytes = bos.toByteArray();
					try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
						update.setBinaryStream(20, bais, bytes.length);
					}
				}
			}

			// set output file id
			if (output.getOutputFileID() == null) {
				update.setNull(21, java.sql.Types.INTEGER);
			}
			else {
				update.setInt(21, output.getOutputFileID());
			}
			if (material.getIsamiVersion() == null || material.getIsamiVersion().isEmpty()) {
				update.setNull(22, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(22, material.getIsamiVersion());
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
					eqStress = new FastLinearEquivalentStress(stressName, id, omissionLevel, materialName);
				}
			}
		}

		// return equivalent stress
		return eqStress;
	}

	/**
	 * Creates and returns preffas equivalent stress in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param output
	 *            Fast equivalent stress output.
	 * @param material
	 *            Preffas material.
	 * @return Preffas equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private FastPreffasEquivalentStress createPreffasEquivalentStress(Connection connection, FastESAOutput output, PreffasMaterial material) throws Exception {

		// save material name to STF files table
		int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
		String sql = "update stf_files set preffas_material = '" + material.toString() + "' where file_id = " + stfID;
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}

		// initialize equivalent stress
		FastPreffasEquivalentStress eqStress = null;

		// create statement
		sql = "insert into fast_preffas_equivalent_stresses";
		sql += "(stf_id, name, stress, validity, remove_negative, omission_level, material_name, material_specification, material_library_version, material_family, material_orientation, material_configuration, ";
		sql += "material_ceff, material_m, material_a, material_b, material_c, material_ftu, material_fty, analysis_input, output_file_id, material_isami_version) ";
		sql += "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// set parameters
			update.setInt(1, stfID); // STF ID
			String stfName = stfFile_ == null ? stfName_ : stfFile_.getName();
			String stressName = "Preffas Eq. Stress (" + stfName + ")";
			update.setString(2, stressName);
			update.setDouble(3, output.getStress());
			update.setDouble(4, validity_);
			update.setBoolean(5, input_.isRemoveNegativeStresses()); // remove negative stresses
			double omissionLevel = applyOmission_ ? omissionLevel_ : -1.0;
			update.setDouble(6, omissionLevel); // omission level
			update.setString(7, material.getName());
			if (material.getSpecification() == null || material.getSpecification().isEmpty()) {
				update.setNull(8, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(8, material.getSpecification());
			}
			if (material.getLibraryVersion() == null || material.getLibraryVersion().isEmpty()) {
				update.setNull(9, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(9, material.getLibraryVersion());
			}
			if (material.getFamily() == null || material.getFamily().isEmpty()) {
				update.setNull(10, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(10, material.getFamily());
			}
			if (material.getOrientation() == null || material.getOrientation().isEmpty()) {
				update.setNull(11, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(11, material.getOrientation());
			}
			if (material.getConfiguration() == null || material.getConfiguration().isEmpty()) {
				update.setNull(12, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(12, material.getConfiguration());
			}
			update.setDouble(13, material.getCeff());
			update.setDouble(14, material.getM());
			update.setDouble(15, material.getA());
			update.setDouble(16, material.getB());
			update.setDouble(17, material.getC());
			update.setDouble(18, material.getFtu());
			update.setDouble(19, material.getFty());

			// save analysis input data
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
				try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
					oos.writeObject(input_);
					oos.flush();
					byte[] bytes = bos.toByteArray();
					try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
						update.setBinaryStream(20, bais, bytes.length);
					}
				}
			}

			// set output file id
			if (output.getOutputFileID() == null) {
				update.setNull(21, java.sql.Types.INTEGER);
			}
			else {
				update.setInt(21, output.getOutputFileID());
			}
			if (material.getIsamiVersion() == null || material.getIsamiVersion().isEmpty()) {
				update.setNull(22, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(22, material.getIsamiVersion());
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
					eqStress = new FastPreffasEquivalentStress(stressName, id, omissionLevel, materialName);
				}
			}
		}

		// return equivalent stress
		return eqStress;
	}

	/**
	 * Creates and returns fatigue equivalent stress in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param output
	 *            Fast equivalent stress output.
	 * @param material
	 *            Fatigue material.
	 * @return Fatigue equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private FastFatigueEquivalentStress createFatigueEquivalentStress(Connection connection, FastESAOutput output, FatigueMaterial material) throws Exception {

		// save material name to STF files table
		int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
		String sql = "update stf_files set fatigue_material = '" + material.toString() + "' where file_id = " + stfID;
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}

		// initialize equivalent stress
		FastFatigueEquivalentStress eqStress = null;

		// create statement
		sql = "insert into fast_fatigue_equivalent_stresses";
		sql += "(stf_id, name, stress, validity, remove_negative, omission_level, material_name, material_specification, material_library_version, material_family, material_orientation, material_configuration, material_p, material_q, material_m, analysis_input, output_file_id, material_isami_version) ";
		sql += "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// set parameters
			update.setInt(1, stfID); // STF ID
			String stfName = stfFile_ == null ? stfName_ : stfFile_.getName();
			String stressName = "Fatigue Eq. Stress (" + stfName + ")";
			update.setString(2, stressName);
			update.setDouble(3, output.getStress()); // stress
			update.setDouble(4, validity_); // validity
			update.setBoolean(5, input_.isRemoveNegativeStresses()); // remove negative stresses
			double omissionLevel = applyOmission_ ? omissionLevel_ : -1.0;
			update.setDouble(6, omissionLevel); // omission level
			update.setString(7, material.getName());
			if (material.getSpecification() == null || material.getSpecification().isEmpty()) {
				update.setNull(8, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(8, material.getSpecification());
			}
			if (material.getLibraryVersion() == null || material.getLibraryVersion().isEmpty()) {
				update.setNull(9, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(9, material.getLibraryVersion());
			}
			if (material.getFamily() == null || material.getFamily().isEmpty()) {
				update.setNull(10, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(10, material.getFamily());
			}
			if (material.getOrientation() == null || material.getOrientation().isEmpty()) {
				update.setNull(11, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(11, material.getOrientation());
			}
			if (material.getConfiguration() == null || material.getConfiguration().isEmpty()) {
				update.setNull(12, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(12, material.getConfiguration());
			}
			update.setDouble(13, material.getP());
			update.setDouble(14, material.getQ());
			update.setDouble(15, material.getM());

			// save analysis input data
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
				try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
					oos.writeObject(input_);
					oos.flush();
					byte[] bytes = bos.toByteArray();
					try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
						update.setBinaryStream(16, bais, bytes.length);
					}
				}
			}

			// set output file id
			if (output.getOutputFileID() == null) {
				update.setNull(17, java.sql.Types.INTEGER);
			}
			else {
				update.setInt(17, output.getOutputFileID());
			}
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
					eqStress = new FastFatigueEquivalentStress(stressName, id, omissionLevel, materialName);
				}
			}
		}

		// return equivalent stress
		return eqStress;
	}

	/**
	 * Runs equivalent stress analysis.
	 *
	 * @param connection
	 *            Database connection.
	 * @return Equivalent stress value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private FastESAOutput runEquivalentStressAnalysis(Connection connection) throws Exception {

		// get selected analysis engine
		boolean isFallback = (boolean) taskPanel_.getOwner().getOwner().getSettings().getValue(Settings.FALLBACK_TO_INBUILT);
		boolean keepOutputs = (boolean) taskPanel_.getOwner().getOwner().getSettings().getValue(Settings.KEEP_ANALYSIS_OUTPUTS);

		// generate output file name
		String outputFileName = !keepOutputs ? null : generateOutputFileName(analysisEngine_.getOutputFileType());

		// ISAMI engine
		if (analysisEngine_.equals(AnalysisEngine.ISAMI)) {

			// maximum allowed number of peaks exceeded
			if (useExtended_) {
				addWarning("Maximum number of allowed peaks per typical flight (100000) is exceeded. Executing extended inbuilt analysis engine.");
				analysisProcess_ = new InbuiltFastESA(this, sequenceFile_, flsFile_, material_, validity_, true, keepOutputs, outputFileName);
			}

			// number of peaks within limits
			else {

				// connected to server (ISAMI engine)
				if (taskPanel_.getOwner().getOwner().getAnalysisServerManager().isConnected()) {
					analysisProcess_ = new IsamiFastESA(this, sequenceFile_, material_, keepOutputs, outputFileName, anaID_, flsID_, validity_, isamiVersion_, isamiSubVersion_, applyCompression_);
				}

				// not connected (inbuilt engine)
				else if (isFallback) {
					addWarning("Cannot connect to analysis server. Falling back to inbuilt Equinox analysis engine.");
					analysisProcess_ = new InbuiltFastESA(this, sequenceFile_, flsFile_, material_, validity_, true, keepOutputs, outputFileName);
				}

				// fallback not selected
				else
					throw new Exception("Cannot connect to Equinox server.");
			}
		}

		// SAFE engine
		else if (analysisEngine_.equals(AnalysisEngine.SAFE)) {

			// maximum allowed number of peaks exceeded
			if (useExtended_) {
				addWarning("Maximum number of allowed peaks per typical flight (100000) is exceeded. Executing extended inbuilt analysis engine.");
				analysisProcess_ = new InbuiltFastESA(this, sequenceFile_, flsFile_, material_, validity_, true, keepOutputs, outputFileName);
			}

			// number of peaks within limits
			else {

				// connected to server (SAFE engine)
				if (taskPanel_.getOwner().getOwner().getAnalysisServerManager().isConnected()) {
					analysisProcess_ = new SafeFastESA(this, sequenceFile_, flsFile_, material_, keepOutputs, outputFileName);
				}

				// not connected (inbuilt engine)
				else if (isFallback) {
					addWarning("Cannot connect to analysis server. Falling back to inbuilt Equinox analysis engine.");
					analysisProcess_ = new InbuiltFastESA(this, sequenceFile_, flsFile_, material_, validity_, false, keepOutputs, outputFileName);
				}

				// fallback not selected
				else
					throw new Exception("Cannot connect to Equinox server.");
			}
		}

		// inbuilt engine
		else {
			analysisProcess_ = new InbuiltFastESA(this, sequenceFile_, flsFile_, material_, validity_, useExtended_, keepOutputs, outputFileName);
		}

		// run and return results of process
		return (FastESAOutput) analysisProcess_.start(connection);
	}

	/**
	 * Generates analysis output file name.
	 *
	 * @param outputFileType
	 *            Output file type.
	 * @return Generated analysis output file name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String generateOutputFileName(FileType outputFileType) throws Exception {

		// set STF file name
		String stfName = stfFile_ == null ? stfName_ : stfFile_.getName();

		// generate file name
		String fileName = null;
		if (material_ instanceof FatigueMaterial) {
			fileName = "Fatigue_" + FileType.getNameWithoutExtension(stfName);
		}
		else if (material_ instanceof PreffasMaterial) {
			fileName = "Preffas_" + FileType.getNameWithoutExtension(stfName);
		}
		else if (material_ instanceof LinearMaterial) {
			fileName = "Linear_" + FileType.getNameWithoutExtension(stfName);
		}

		// append extension
		return FileType.appendExtension(fileName, outputFileType);
	}
}
