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
import java.nio.file.StandardCopyOption;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.AnalysisEngine;
import equinox.data.DPRatio;
import equinox.data.DT1PointInterpolator;
import equinox.data.DT2PointsInterpolator;
import equinox.data.DTInterpolation;
import equinox.data.DTInterpolator;
import equinox.data.DamageContribution;
import equinox.data.DamageContributionResult;
import equinox.data.EmbeddedTask;
import equinox.data.GAGEvent;
import equinox.data.GAGPeak;
import equinox.data.IncStress;
import equinox.data.LoadcaseFactor;
import equinox.data.Segment;
import equinox.data.SegmentFactor;
import equinox.data.Settings;
import equinox.data.Stress;
import equinox.data.StressComponent;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.data.input.LoadcaseDamageContributionInput;
import equinox.dataServer.remote.data.ContributionType;
import equinox.plugin.FileType;
import equinox.process.ESAProcess;
import equinox.process.InbuiltDCA;
import equinox.process.SafeDCA;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.task.serializableTask.SerializableLoadcaseDamageContributionAnalysis;
import equinox.utility.Utility;

/**
 * Class for loadcase damage contribution analysis task.
 *
 * @author Murat Artim
 * @date Apr 2, 2015
 * @time 4:25:54 PM
 */
public class LoadcaseDamageContributionAnalysis extends TemporaryFileCreatingTask<LoadcaseDamageContributions> implements LongRunningTask, SavableTask, SingleInputTask<STFFile>, AutomaticTaskOwner<LoadcaseDamageContributions> {

	/** Result index. */
	public static final int CONTRIBUTION_INDEX = 0, DAMAGE = 1;

	/** The owner STF file. */
	private STFFile stfFile_;

	/** STF file ID. */
	private final Integer stfID_, stressTableID_;

	/** STF file name. */
	private final String stfName_;

	/** The owner spectrum. */
	private final Spectrum spectrum_;

	/** Input. */
	private final LoadcaseDamageContributionInput input_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.00");

	/** Number of columns. */
	private static final int NUM_COLS = 8;

	/** STH lines. */
	private String[] lines_;

	/** STH file indices. */
	private int rowIndex_ = 0, colIndex_ = 0, maxPeaks_;

	/** Delta-P ratio. */
	private DPRatio dpRatio_;

	/** Delta-T interpolator. */
	private DTInterpolator dtInterpolator_;

	/** Damage analysis process. */
	private ESAProcess<List<DamageContributionResult>> damageAnalysis_;

	/** GAG events. */
	private ArrayList<GAGEvent> gagEvents_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<LoadcaseDamageContributions>> automaticTasks_ = null;

	/**
	 * Creates loadcase damage contribution analysis task.
	 *
	 * @param stfFile
	 *            The owner STF file. Can be null for automatic execution.
	 * @param input
	 *            Analysis input.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public LoadcaseDamageContributionAnalysis(STFFile stfFile, LoadcaseDamageContributionInput input, AnalysisEngine analysisEngine) {
		stfFile_ = stfFile;
		input_ = input;
		stfID_ = null;
		stressTableID_ = null;
		stfName_ = null;
		spectrum_ = null;
		analysisEngine_ = analysisEngine;
	}

	/**
	 * Creates loadcase damage contribution analysis task.
	 *
	 * @param stfID
	 *            STF file ID.
	 * @param stressTableID
	 *            STF stress table ID.
	 * @param stfName
	 *            STF file name.
	 * @param spectrum
	 *            The owner spectrum.
	 * @param input
	 *            Analysis input.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public LoadcaseDamageContributionAnalysis(int stfID, int stressTableID, String stfName, Spectrum spectrum, LoadcaseDamageContributionInput input, AnalysisEngine analysisEngine) {
		stfID_ = stfID;
		stressTableID_ = stressTableID;
		stfName_ = stfName;
		spectrum_ = spectrum;
		input_ = input;
		stfFile_ = null;
		analysisEngine_ = analysisEngine;
	}

	@Override
	public void setAutomaticInput(STFFile input) {
		stfFile_ = input;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<LoadcaseDamageContributions> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<LoadcaseDamageContributions>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Loadcase damage contribution analysis";
	}

	@Override
	public SerializableTask getSerializableTask() {
		if (stfFile_ != null)
			return new SerializableLoadcaseDamageContributionAnalysis(stfFile_, input_, analysisEngine_);
		return new SerializableLoadcaseDamageContributionAnalysis(stfID_, stressTableID_, stfName_, spectrum_, input_, analysisEngine_);
	}

	@Override
	protected LoadcaseDamageContributions call() throws Exception {

		// check permission
		checkPermission(Permission.DAMAGE_CONTRIBUTION_ANALYSIS);

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// get validity
				int validity = getValidity(connection);

				// generate stress sequences
				Path[] sthFiles = generateStressSequences(connection, validity);

				// task cancelled
				if (isCancelled() || sthFiles == null) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// save FLS file
				Path flsFile = saveFLSFile(getWorkingDirectory().resolve("input.fls"), connection);

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// run damage analyses
				List<DamageContributionResult> results = damageAnalysis(sthFiles, flsFile, connection, validity);

				// task cancelled
				if (isCancelled() || results == null || results.isEmpty()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// create damage contributions
				LoadcaseDamageContributions contributions = createDamageContributions(connection, results, validity);

				// task cancelled
				if (isCancelled() || contributions == null) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return contributions
				return contributions;
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

		// add damage angle to file tree
		try {

			// there is STF file
			if (stfFile_ != null) {

				// get damage contributions
				LoadcaseDamageContributions contributions = get();

				// add to file tree
				stfFile_.getChildren().add(0, contributions);

				// plot and save contributions
				taskPanel_.getOwner().runTaskInParallel(new SaveLoadcaseDamageContributionPlot(contributions, true, null));

				// manage automatic tasks
				automaticTaskOwnerSucceeded(contributions, automaticTasks_, taskPanel_);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// destroy sub processes (if still running)
		if (damageAnalysis_ != null) {
			damageAnalysis_.cancel();
		}

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// destroy sub processes (if still running)
		if (damageAnalysis_ != null) {
			damageAnalysis_.cancel();
		}

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	/**
	 * Retrieves and returns the validity of spectrum from FLS file.
	 *
	 * @param connection
	 *            Database connection.
	 * @return Validity.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int getValidity(Connection connection) throws Exception {

		// progress info
		updateMessage("Getting spectrum validity from database...");

		// initialize validity
		int validity = 0;
		int flsFileID = stfFile_ == null ? spectrum_.getFLSFileID() : stfFile_.getParentItem().getFLSFileID();

		// create statement
		try (Statement statement = connection.createStatement()) {

			// execute query
			statement.setMaxRows(1);
			try (ResultSet resultSet = statement.executeQuery("select flight_num from fls_flights where file_id = " + flsFileID + " order by flight_num desc")) {
				while (resultSet.next()) {
					validity = resultSet.getInt("flight_num");
				}
			}

			// reset statement
			statement.setMaxRows(0);
		}

		// return validity
		return validity;
	}

	/**
	 * Creates damage contributions in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param results
	 *            Results.
	 * @param validity
	 *            Spectrum validity.
	 * @return Damage contributions item.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private LoadcaseDamageContributions createDamageContributions(Connection connection, List<DamageContributionResult> results, int validity) throws Exception {

		// update info
		updateMessage("Saving damage contributions to database...");

		// check for full results
		DamageContributionResult full = null;
		for (DamageContributionResult result : results) {
			if (result == null) {
				continue;
			}
			if (result.getContributionIndex() == 0) {
				full = result;
				break;
			}
		}

		// no total damage available
		if (full == null)
			throw new Exception("Total damage could not be calculated. Analysis aborted.");

		// save material name to STF files table
		int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
		String sql = "update stf_files set fatigue_material = '" + input_.getMaterial().toString() + "' where file_id = " + stfID;
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}

		// initialize damage contributions
		LoadcaseDamageContributions contributions = null;

		// create statement for inserting damage contributions to database
		sql = "insert into dam_contributions(stf_id, name, oneg_fac, inc_fac, dp_fac, dt_fac, ref_dp, dp_lc, dt_lc_inf, dt_lc_sup, ref_dt_inf, ref_dt_sup, stress_comp, rotation_angle, validity, total_damage, stress, remove_negative, omission_level, material_name, material_specification, material_library_version, material_family, material_orientation, material_configuration, material_p, material_q, material_m, material_isami_version) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// STF ID and name
			update.setInt(1, stfID); // STF ID
			String stfName = stfFile_ == null ? stfName_ : stfFile_.getName();
			String contributionName = "Loadcase Damage Contributions (" + stfName + ")";
			update.setString(2, contributionName);

			// overall stress factors
			String stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.ONEG) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.ONEG) + ")";
			update.setString(3, stressModifier); // oneg_fac
			stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.INCREMENT) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.INCREMENT) + ")";
			update.setString(4, stressModifier); // inc_fac
			stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.DELTAP) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.DELTAP) + ")";
			update.setString(5, stressModifier); // dp_fac
			stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.DELTAT) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.DELTAT) + ")";
			update.setString(6, stressModifier); // dt_fac

			// reference DP and DP load case
			update.setDouble(7, dpRatio_ == null ? 0.0 : dpRatio_.getReferencePressure()); // ref_dp
			if (dpRatio_ == null) {
				update.setNull(8, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(8, dpRatio_.getIssyCode()); // dp_lc
			}

			// no delta-t interpolation
			if (dtInterpolator_ == null) {
				update.setNull(9, java.sql.Types.VARCHAR); // dt_lc_inf
				update.setNull(10, java.sql.Types.VARCHAR); // dt_lc_sup
				update.setNull(11, java.sql.Types.DOUBLE); // ref_dt_inf
				update.setNull(12, java.sql.Types.DOUBLE); // ref_dt_sup
			}

			// 1 point interpolation
			else if (dtInterpolator_ instanceof DT1PointInterpolator) {
				DT1PointInterpolator onePoint = (DT1PointInterpolator) dtInterpolator_;
				update.setString(10, onePoint.getIssyCode()); // dt_lc_sup
				update.setDouble(12, onePoint.getReferenceTemperature()); // ref_dt_sup
				update.setNull(9, java.sql.Types.VARCHAR); // dt_lc_inf
				update.setNull(11, java.sql.Types.DOUBLE); // ref_dt_inf
			}

			// 2 points interpolation
			else if (dtInterpolator_ instanceof DT2PointsInterpolator) {
				DT2PointsInterpolator twoPoints = (DT2PointsInterpolator) dtInterpolator_;
				update.setString(10, twoPoints.getIssyCodeSup()); // dt_lc_sup
				update.setDouble(12, twoPoints.getReferenceTemperatureSup()); // ref_dt_sup
				update.setString(9, twoPoints.getIssyCodeInf()); // dt_lc_inf
				update.setDouble(11, twoPoints.getReferenceTemperatureInf()); // ref_dt_inf
			}

			// rotation
			update.setString(13, input_.getStressComponent().toString()); // stress_comp
			update.setDouble(14, input_.getRotationAngle()); // rotation_angle

			// validity, total damage and omission
			update.setDouble(15, validity);
			update.setDouble(16, full.getDamage());
			update.setDouble(17, full.getStress());
			update.setBoolean(18, input_.isRemoveNegativeStresses()); // remove negative stresses
			update.setDouble(19, input_.isApplyOmission() ? input_.getOmissionLevel() : -1.0); // omission level

			// material
			update.setString(20, input_.getMaterial().getName());
			if (input_.getMaterial().getSpecification() == null || input_.getMaterial().getSpecification().isEmpty()) {
				update.setNull(21, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(21, input_.getMaterial().getSpecification());
			}
			if (input_.getMaterial().getLibraryVersion() == null || input_.getMaterial().getLibraryVersion().isEmpty()) {
				update.setNull(22, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(22, input_.getMaterial().getLibraryVersion());
			}
			if (input_.getMaterial().getFamily() == null || input_.getMaterial().getFamily().isEmpty()) {
				update.setNull(23, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(23, input_.getMaterial().getFamily());
			}
			if (input_.getMaterial().getOrientation() == null || input_.getMaterial().getOrientation().isEmpty()) {
				update.setNull(24, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(24, input_.getMaterial().getOrientation());
			}
			if (input_.getMaterial().getConfiguration() == null || input_.getMaterial().getConfiguration().isEmpty()) {
				update.setNull(25, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(25, input_.getMaterial().getConfiguration());
			}
			update.setDouble(26, input_.getMaterial().getP());
			update.setDouble(27, input_.getMaterial().getQ());
			update.setDouble(28, input_.getMaterial().getM());
			if (input_.getMaterial().getIsamiVersion() == null || input_.getMaterial().getIsamiVersion().isEmpty()) {
				update.setNull(29, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(29, input_.getMaterial().getIsamiVersion());
			}

			// execute update
			update.executeUpdate();

			// create damage contributions
			try (ResultSet resultSet = update.getGeneratedKeys()) {
				while (resultSet.next()) {
					contributions = new LoadcaseDamageContributions(contributionName, resultSet.getBigDecimal(1).intValue(), input_.getMaterial().toString());
				}
			}
		}

		// null contributions
		if (contributions == null)
			throw new Exception("Exception occurred during creating damage contributions.");

		// add loadcase factor information
		if (input_.getLoadcaseFactors() != null) {
			sql = "insert into dam_contributions_event_modifiers(contributions_id, loadcase_number, event_name, comment, value, method) values(?, ?, ?, ?, ?, ?)";
			try (PreparedStatement update = connection.prepareStatement(sql)) {
				for (LoadcaseFactor eFactor : input_.getLoadcaseFactors()) {
					update.setInt(1, contributions.getID()); // contributions ID
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
			sql = "insert into dam_contributions_segment_modifiers(contributions_id, segment_name, segment_number, oneg_value, inc_value, dp_value, oneg_method, inc_method, dp_method, dt_value, dt_method) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement update = connection.prepareStatement(sql)) {
				for (SegmentFactor sFactor : input_.getSegmentFactors()) {
					update.setInt(1, contributions.getID()); // contributions ID
					update.setString(2, sFactor.getSegment().getName()); // segment name
					update.setInt(3, sFactor.getSegment().getSegmentNumber()); // segment number
					update.setDouble(4, sFactor.getModifierValue(GenerateStressSequenceInput.ONEG)); // 1g value
					update.setDouble(5, sFactor.getModifierValue(GenerateStressSequenceInput.INCREMENT)); // increment value
					update.setDouble(6, sFactor.getModifierValue(GenerateStressSequenceInput.DELTAP)); // delta-p value
					update.setString(7, sFactor.getModifierMethod(GenerateStressSequenceInput.ONEG)); // 1g method
					update.setString(8, sFactor.getModifierMethod(GenerateStressSequenceInput.INCREMENT)); // increment method
					update.setString(9, sFactor.getModifierMethod(GenerateStressSequenceInput.DELTAP)); // delta-p method
					update.setDouble(10, sFactor.getModifierValue(GenerateStressSequenceInput.DELTAT)); // delta-t value
					update.setString(11, sFactor.getModifierMethod(GenerateStressSequenceInput.DELTAT)); // delta-t method
					update.executeUpdate();
				}
			}
		}

		// add GAG events
		sql = "insert into dam_contributions_gag_events(contributions_id, event_name, issy_code, comment, rating, ismax, segment_name, segment_number) values(?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement insertGAG = connection.prepareStatement(sql)) {
			int convTableID = stfFile_ == null ? spectrum_.getConversionTableID() : stfFile_.getParentItem().getConversionTableID();
			sql = "select comment from xls_comments where file_id = " + convTableID + " and issy_code = ? and fue_translated like ?";
			try (PreparedStatement getComment = connection.prepareStatement(sql)) {

				// loop over GAG events
				for (GAGEvent event : gagEvents_) {

					// get comment
					getComment.setString(1, event.getIssyCode()); // issy code
					getComment.setString(2, "%" + event.getEvent() + "%"); // FUE translated
					String comment = null;
					try (ResultSet resultSet = getComment.executeQuery()) {
						while (resultSet.next()) {
							comment = resultSet.getString("comment");
						}
					}

					// insert into GAG events
					insertGAG.setInt(1, contributions.getID()); // contributions ID
					insertGAG.setString(2, event.getEvent()); // event name
					insertGAG.setString(3, event.getIssyCode()); // issy code
					if (comment != null) {
						insertGAG.setString(4, comment);
					}
					else {
						insertGAG.setNull(4, java.sql.Types.VARCHAR);
					}
					insertGAG.setDouble(5, event.getRating()); // rating
					insertGAG.setBoolean(6, event.getType()); // contributes to max or min
					insertGAG.setString(7, event.getSegment().getName()); // segment name
					insertGAG.setInt(8, event.getSegment().getSegmentNumber()); // segment number
					insertGAG.executeUpdate();
				}
			}
		}

		// prepare statement for inserting damage contributions to database
		sql = "insert into dam_contribution(contributions_id, name, cont_type, damage, stress) values(?, ?, ?, ?, ?)";
		try (PreparedStatement insertContribution = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// prepare statement for inserting increment event stress modifiers
			sql = "insert into dam_contribution_event_modifiers(contribution_id, contributions_id, loadcase_number, event_name, comment) values(?, ?, ?, ?, ?)";
			try (PreparedStatement insertEventModifiers = connection.prepareStatement(sql)) {

				// get damage contributions
				ArrayList<DamageContribution> contribution = input_.getContributions();

				// loop over results
				for (DamageContributionResult result : results) {

					// null
					if (result == null) {
						continue;
					}

					// full
					int index = result.getContributionIndex();
					if (index == 0) {
						continue;
					}

					// get contribution type
					ContributionType type = contribution.get(index - 1).getType();

					// insert contribution
					insertContribution.setInt(1, contributions.getID()); // contributions ID
					insertContribution.setString(2, contribution.get(index - 1).getName()); // name
					insertContribution.setString(3, contribution.get(index - 1).getType().getName()); // contribution type
					insertContribution.setDouble(4, result.getDamage()); // damage
					insertContribution.setDouble(5, result.getStress()); // stress
					insertContribution.executeUpdate();

					// not increment
					if (!type.equals(ContributionType.INCREMENT)) {
						continue;
					}

					// get id
					int contID = -1;
					try (ResultSet resultSet = insertContribution.getGeneratedKeys()) {
						while (resultSet.next()) {
							contID = resultSet.getBigDecimal(1).intValue();
						}
					}

					// loop over event modifiers
					for (LoadcaseFactor modifier : contribution.get(index - 1).getLoadcaseFactors()) {
						insertEventModifiers.setInt(1, contID);
						insertEventModifiers.setInt(2, contributions.getID());
						insertEventModifiers.setString(3, modifier.getLoadcaseNumber()); // loadcase number
						if (modifier.getEventName() != null) {
							insertEventModifiers.setString(4, modifier.getEventName());
						}
						else {
							insertEventModifiers.setNull(4, java.sql.Types.VARCHAR);
						}
						if (modifier.getComments() != null) {
							insertEventModifiers.setString(5, modifier.getComments());
						}
						else {
							insertEventModifiers.setNull(5, java.sql.Types.VARCHAR);
						}
						insertEventModifiers.executeUpdate();
					}
				}
			}
		}

		// return contributions
		return contributions;
	}

	/**
	 * Runs damage analyses.
	 *
	 * @param sthFiles
	 *            Paths to input STH files.
	 * @param flsFile
	 *            Path to input FLS file.
	 * @param connection
	 *            Database connection.
	 * @param validity
	 *            Spectrum validity.
	 * @return Fatigue damages.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private List<DamageContributionResult> damageAnalysis(Path[] sthFiles, Path flsFile, Connection connection, int validity) throws Exception {

		// get selected analysis engine
		boolean isFallback = (boolean) taskPanel_.getOwner().getOwner().getSettings().getValue(Settings.FALLBACK_TO_INBUILT);

		// ISAMI engine
		if (analysisEngine_.equals(AnalysisEngine.ISAMI)) {
			damageAnalysis_ = new InbuiltDCA(this, sthFiles, flsFile, input_.getContributions(), input_.getMaterial(), validity, maxPeaks_ > EquivalentStressAnalysis.MAX_PEAKS, input_.isApplyOmission(), input_.getOmissionLevel());
		}

		// SAFE engine
		else if (analysisEngine_.equals(AnalysisEngine.SAFE)) {

			// maximum allowed number of peaks exceeded
			if (maxPeaks_ > EquivalentStressAnalysis.MAX_PEAKS) {
				addWarning("Maximum number of allowed peaks per typical flight (100000) is exceeded. Executing extended inbuilt analysis engine.");
				damageAnalysis_ = new InbuiltDCA(this, sthFiles, flsFile, input_.getContributions(), input_.getMaterial(), validity, true, input_.isApplyOmission(), input_.getOmissionLevel());
			}

			// number of peaks within limits
			else {

				// connected to server (SAFE engine)
				if (taskPanel_.getOwner().getOwner().getAnalysisServerManager().isConnected()) {
					damageAnalysis_ = new SafeDCA(this, sthFiles, flsFile, input_.getContributions(), input_.getMaterial(), input_.isApplyOmission(), input_.getOmissionLevel());
				}

				// not connected (inbuilt engine)
				else if (isFallback) {
					addWarning("Cannot connect to analysis server. Falling back to inbuilt analysis engine.");
					damageAnalysis_ = new InbuiltDCA(this, sthFiles, flsFile, input_.getContributions(), input_.getMaterial(), validity, false, input_.isApplyOmission(), input_.getOmissionLevel());
				}

				// fallback not selected
				else
					throw new Exception("Cannot connect to analysis server.");
			}
		}

		// inbuilt engine
		else {
			damageAnalysis_ = new InbuiltDCA(this, sthFiles, flsFile, input_.getContributions(), input_.getMaterial(), validity, maxPeaks_ > EquivalentStressAnalysis.MAX_PEAKS, input_.isApplyOmission(), input_.getOmissionLevel());
		}

		// run and return results of process
		return damageAnalysis_.start(connection);
	}

	/**
	 * Saves FLS file into output path.
	 *
	 * @param output
	 *            Output FLS file.
	 * @param connection
	 *            Database connection.
	 * @return Output FLS file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path saveFLSFile(Path output, Connection connection) throws Exception {

		// update info
		updateMessage("Saving input FLS file...");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// execute query
			int flsFileID = stfFile_ == null ? spectrum_.getFLSFileID() : stfFile_.getParentItem().getFLSFileID();
			try (ResultSet resultSet = statement.executeQuery("select name, data from fls_files where file_id = " + flsFileID)) {

				// get data
				if (resultSet.next()) {

					// get file name
					String name = resultSet.getString("name");

					// get blob
					Blob blob = resultSet.getBlob("data");

					// FLS file format
					Path zipFile = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
					Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
					Path flsFile = Utility.extractFileFromZIP(zipFile, this, FileType.FLS, null);
					Files.copy(flsFile, output, StandardCopyOption.REPLACE_EXISTING);

					// free blob
					blob.free();
				}
			}
		}

		// return output path
		return output;
	}

	/**
	 * Generates stress sequences for each damage contribution and returns paths to STH files.
	 *
	 * @param connection
	 *            Database connection.
	 * @param spectrumValidity
	 *            Spectrum validity.
	 * @return Paths to STH files.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path[] generateStressSequences(Connection connection, int spectrumValidity) throws Exception {

		// update info
		updateMessage("Generating stress sequences for damage contributions...");

		// get damage contributions
		ArrayList<DamageContribution> contributions = input_.getContributions();

		// initialize mapping
		Path[] paths = new Path[contributions.size() + 1];
		BufferedWriter[] writers = new BufferedWriter[paths.length];

		try {

			// create file writers
			updateMessage("Creating STH file writers...");
			Path rootDir = getWorkingDirectory();
			String baseFileName = FileType.getNameWithoutExtension(stfFile_ == null ? stfName_ : stfFile_.getName());
			lines_ = new String[paths.length];
			for (int i = 0; i < paths.length; i++) {
				String name = i == 0 ? "full" : contributions.get(i - 1).getName();
				paths[i] = rootDir.resolve(FileType.appendExtension(Utility.correctFileName(baseFileName) + "_" + name, FileType.STH));
				writers[i] = Files.newBufferedWriter(paths[i], Charset.defaultCharset());
				writeSTHHeader(writers[i], name);
			}

			// get CDF set file IDs
			updateMessage("Getting spectrum file IDs from database...");
			Spectrum cdfSet = stfFile_ == null ? spectrum_ : stfFile_.getParentItem();
			int anaFileID = cdfSet.getANAFileID();
			int txtFileID = cdfSet.getTXTFileID();
			int convTableID = cdfSet.getConversionTableID();
			int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
			int stressTableID = stfFile_ == null ? stressTableID_ : stfFile_.getStressTableID();

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get DP ratio
				updateMessage("Computing delta-p ratio...");
				dpRatio_ = getDPRatio(connection, statement, anaFileID, txtFileID, convTableID);

				// get DT parameters
				updateMessage("Computing delta-t interpolation...");
				dtInterpolator_ = getDTInterpolator(connection, statement, txtFileID);

				// get number of flights and peaks of the ANA file
				int numPeaks = getNumberOfPeaks(statement, anaFileID);

				// get maximum number of peaks per typical flight
				maxPeaks_ = getMaxPeaksPerFlight(statement, anaFileID);

				// prepare statement for selecting ANA peaks
				String sql = "select peak_num, fourteen_digit_code, delta_p, delta_t from ana_peaks_" + anaFileID + " where flight_id = ?";
				try (PreparedStatement selectANAPeak = connection.prepareStatement(sql)) {

					// prepare statement for selecting 1g issy code
					sql = "select flight_phase, issy_code, oneg_order from txt_codes where file_id = " + txtFileID + " and one_g_code = ? and increment_num = 0";
					try (PreparedStatement select1GIssyCode = connection.prepareStatement(sql)) {

						// prepare statement for selecting increment issy code
						sql = "select flight_phase, issy_code, factor_1, factor_2, factor_3, factor_4, factor_5, factor_6, factor_7, factor_8 from txt_codes where file_id = " + txtFileID
								+ " and one_g_code = ? and increment_num = ? and direction_num = ? and (nl_factor_num is null or nl_factor_num = ?)";
						try (PreparedStatement selectIncrementIssyCode = connection.prepareStatement(sql)) {

							// prepare statement for selecting STF stress
							sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stressTableID + " where file_id = " + stfID + " and issy_code = ?";
							try (PreparedStatement selectSTFStress = connection.prepareStatement(sql)) {

								// execute query for selecting ANA flights
								sql = "select * from ana_flights where file_id = " + anaFileID + " order by flight_num";
								try (ResultSet anaFlights = statement.executeQuery(sql)) {

									// loop over flights
									HashMap<String, Stress> oneg = new HashMap<>();
									HashMap<String, IncStress> inc = new HashMap<>();
									GAGPeak[] flightGAGPeaks = new GAGPeak[] { new GAGPeak(), new GAGPeak() };
									int peakCount = 0;
									while (anaFlights.next()) {

										// task cancelled
										if (isCancelled())
											return null;

										// reset max-min values
										flightGAGPeaks[0].setStress(Double.NEGATIVE_INFINITY); // max peak
										flightGAGPeaks[1].setStress(Double.POSITIVE_INFINITY); // min peak
										flightGAGPeaks[0].setEvents(null);
										flightGAGPeaks[0].setIssyCodes(null);
										flightGAGPeaks[1].setEvents(null);
										flightGAGPeaks[1].setIssyCodes(null);

										// write flight header
										int flightPeaks = anaFlights.getInt("num_peaks");
										double flightValidity = writeFlightHeaders(writers, anaFlights, flightPeaks);

										// initialize variables
										int rem = flightPeaks % NUM_COLS;
										int numRows = flightPeaks / NUM_COLS + (rem == 0 ? 0 : 1);
										rowIndex_ = 0;
										colIndex_ = 0;
										for (int i = 0; i < lines_.length; i++) {
											lines_[i] = "";
										}

										// execute statement for getting ANA peaks
										selectANAPeak.setInt(1, anaFlights.getInt("flight_id"));
										try (ResultSet anaPeaks = selectANAPeak.executeQuery()) {

											// loop over peaks
											while (anaPeaks.next()) {

												// task cancelled
												if (isCancelled())
													return null;

												// update progress
												updateProgress(peakCount, numPeaks);
												peakCount++;

												// insert peak into STH peaks table
												flightGAGPeaks = writeSTHPeak(writers, anaPeaks, select1GIssyCode, selectSTFStress, selectIncrementIssyCode, oneg, inc, dpRatio_, dtInterpolator_, rem, numRows, contributions, flightGAGPeaks);
											}
										}

										// write GAG peaks
										if (input_.getGAGContributionIndex() != -1) {
											writeGAGPeaks(writers[input_.getGAGContributionIndex() + 1], flightGAGPeaks);
										}

										// add to GAG peaks
										addToGAGEvents(flightGAGPeaks, spectrumValidity, flightValidity);
									}
								}
							}
						}
					}
				}
			}

			// return paths
			return paths;
		}

		// close file writers
		finally {
			for (BufferedWriter writer : writers)
				if (writer != null) {
					writer.close();
				}
		}
	}

	/**
	 * Adds flight GAG events to overall GAG events.
	 *
	 * @param flightGAGPeaks
	 *            Flight GAG peaks.
	 * @param spectrumValidity
	 *            Spectrum validity.
	 * @param flightValidity
	 *            Flight validity.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void addToGAGEvents(GAGPeak[] flightGAGPeaks, double spectrumValidity, double flightValidity) throws Exception {

		// null list
		if (gagEvents_ == null) {
			gagEvents_ = new ArrayList<>();
		}

		// loop over flight GAG peaks
		for (int i = 0; i < flightGAGPeaks.length; i++) {

			// get events and load cases
			String events = flightGAGPeaks[i].getEvents();
			String issyCodes = flightGAGPeaks[i].getIssyCodes();

			// null events or issy codes
			if (events == null || issyCodes == null) {
				continue;
			}

			// multiple events and load cases
			if (events.contains(",") && issyCodes.contains(",")) {

				// split events and load cases
				String[] splitEvents = events.split(",");
				String[] splitIssyCodes = issyCodes.split(",");

				// loop over events
				for (int j = 0; j < splitEvents.length; j++) {
					GAGEvent gagEvent = new GAGEvent(splitEvents[j].trim(), splitIssyCodes[j].trim());
					gagEvent.setRating(flightValidity / spectrumValidity);
					gagEvent.setType(i == 0);
					gagEvent.setSegment(flightGAGPeaks[i].getSegment());
					int index = gagEvents_.indexOf(gagEvent);
					if (index != -1) {
						gagEvents_.get(index).setRating(gagEvents_.get(index).getRating() + gagEvent.getRating());
					}
					else {
						gagEvents_.add(gagEvent);
					}
				}
			}

			// single event and load case
			else {
				GAGEvent gagEvent = new GAGEvent(events.trim(), issyCodes.trim());
				gagEvent.setRating(flightValidity / spectrumValidity);
				gagEvent.setType(i == 0);
				gagEvent.setSegment(flightGAGPeaks[i].getSegment());
				int index = gagEvents_.indexOf(gagEvent);
				if (index != -1) {
					gagEvents_.get(index).setRating(gagEvents_.get(index).getRating() + gagEvent.getRating());
				}
				else {
					gagEvents_.add(gagEvent);
				}
			}
		}
	}

	/**
	 * Writes GAG peaks.
	 *
	 * @param gagWriter
	 *            GAG contribution writer.
	 * @param gagPeaks
	 *            Array containing the max-min values of the flight.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeGAGPeaks(BufferedWriter gagWriter, GAGPeak[] gagPeaks) throws Exception {
		String line = String.format("%10s", format_.format(gagPeaks[1].getStress()));
		line += String.format("%10s", format_.format(gagPeaks[0].getStress()));
		line += String.format("%10s", format_.format(gagPeaks[1].getStress()));
		gagWriter.write(line);
		gagWriter.newLine();
	}

	/**
	 * Writes STH peaks to output files.
	 *
	 * @param writers
	 *            File writers.
	 * @param anaPeaks
	 *            ANA peaks.
	 * @param select1GIssyCode
	 *            Database statement for selecting 1g issy codes.
	 * @param selectSTFStress
	 *            Database statement for selecting STF stresses.
	 * @param selectIncrementIssyCode
	 *            Database statement for selecting increment issy codes.
	 * @param oneg
	 *            Mapping containing 1g codes versus the stresses.
	 * @param inc
	 *            Mapping containing class codes versus the stresses.
	 * @param dpRatio
	 *            Delta-p ratio.
	 * @param dtInterpolator
	 *            Delta-t interpolator.
	 * @param numRows
	 *            Number of rows to write.
	 * @param rem
	 *            Remaining number of columns in the STH output file.
	 * @param contributions
	 *            Damage contributions.
	 * @param gagPeaks
	 *            Array containing the GAG peaks.
	 * @return Array containing the max-min values.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private GAGPeak[] writeSTHPeak(BufferedWriter[] writers, ResultSet anaPeaks, PreparedStatement select1GIssyCode, PreparedStatement selectSTFStress, PreparedStatement selectIncrementIssyCode, HashMap<String, Stress> oneg, HashMap<String, IncStress> inc, DPRatio dpRatio,
			DTInterpolator dtInterpolator, int rem, int numRows, ArrayList<DamageContribution> contributions, GAGPeak[] gagPeaks) throws Exception {

		// get class code
		String classCode = anaPeaks.getString("fourteen_digit_code");
		String onegCode = classCode.substring(0, 4);

		// get 1g stress
		Stress onegStress = oneg.get(onegCode);
		if (onegStress == null) {
			onegStress = get1GStress(selectSTFStress, select1GIssyCode, onegCode);
			oneg.put(onegCode, onegStress);
		}

		// get segment
		Segment segment = onegStress.getSegment();

		// get increment stresses
		IncStress incStress = inc.get(classCode);
		if (incStress == null) {
			incStress = getIncStress(selectSTFStress, selectIncrementIssyCode, classCode, onegCode, segment, contributions);
			inc.put(classCode, incStress);
		}

		// compute and modify delta-p stress
		double dpStress = dpRatio == null ? 0.0 : dpRatio.getStress(anaPeaks.getDouble("delta_p"));
		if (dpRatio != null) {
			dpStress = modify1GStress(dpRatio.getIssyCode(), segment, GenerateStressSequenceInput.DELTAP, dpStress);
		}

		// compute and modify delta-t stress
		double dtStress = dtInterpolator == null ? 0.0 : dtInterpolator.getStress(anaPeaks.getDouble("delta_t"));
		if (dtInterpolator != null && dtInterpolator instanceof DT1PointInterpolator) {
			DT1PointInterpolator onePoint = (DT1PointInterpolator) dtInterpolator;
			dtStress = modify1GStress(onePoint.getIssyCode(), segment, GenerateStressSequenceInput.DELTAT, dtStress);
		}
		else if (dtInterpolator != null && dtInterpolator instanceof DT2PointsInterpolator) {
			DT2PointsInterpolator twoPoints = (DT2PointsInterpolator) dtInterpolator;
			dtStress = modify2PointDTStress(twoPoints, segment, dtStress);
		}

		// calculate total stresses
		double[] incStresses = incStress.getStress();
		double[] totalStresses = new double[incStresses.length];
		totalStresses[0] = onegStress.getStress() + incStresses[0] + dpStress + dtStress;
		for (int i = 1; i < incStresses.length; i++)
			if (contributions.get(i - 1).getType().equals(ContributionType.ONEG)) {
				totalStresses[i] = incStresses[i] + dpStress + dtStress;
			}
			else if (contributions.get(i - 1).getType().equals(ContributionType.DELTA_P)) {
				totalStresses[i] = onegStress.getStress() + incStresses[i] + dtStress;
			}
			else if (contributions.get(i - 1).getType().equals(ContributionType.DELTA_T)) {
				totalStresses[i] = onegStress.getStress() + incStresses[i] + dpStress;
			}
			else {
				totalStresses[i] = onegStress.getStress() + incStresses[i] + dpStress + dtStress;
			}

		// remove negative stresses
		if (input_.isRemoveNegativeStresses()) {
			for (int i = 0; i < totalStresses.length; i++)
				if (totalStresses[i] < 0) {
					totalStresses[i] = 0.0;
				}
		}

		// last row
		if (rowIndex_ == numRows - 1) {

			// add peaks
			for (int i = 0; i < lines_.length; i++) {
				if (input_.getGAGContributionIndex() != -1 && i == input_.getGAGContributionIndex() + 1) {
					continue;
				}
				lines_[i] += String.format("%10s", format_.format(totalStresses[i]));
			}
			colIndex_++;

			// last column
			if (colIndex_ == (rem == 0 ? NUM_COLS : rem)) {
				for (int i = 0; i < lines_.length; i++) {
					if (input_.getGAGContributionIndex() != -1 && i == input_.getGAGContributionIndex() + 1) {
						continue;
					}
					writers[i].write(lines_[i]);
					writers[i].newLine();
					lines_[i] = "";
				}
				colIndex_ = 0;
				rowIndex_++;
			}
		}

		// other rows
		else {

			// add peaks
			for (int i = 0; i < lines_.length; i++) {
				if (input_.getGAGContributionIndex() != -1 && i == input_.getGAGContributionIndex() + 1) {
					continue;
				}
				lines_[i] += String.format("%10s", format_.format(totalStresses[i]));
			}
			colIndex_++;

			// last column
			if (colIndex_ == NUM_COLS) {
				for (int i = 0; i < lines_.length; i++) {
					if (input_.getGAGContributionIndex() != -1 && i == input_.getGAGContributionIndex() + 1) {
						continue;
					}
					writers[i].write(lines_[i]);
					writers[i].newLine();
					lines_[i] = "";
				}
				colIndex_ = 0;
				rowIndex_++;
			}
		}

		// update GAG peaks
		if (totalStresses[0] >= gagPeaks[0].getStress()) {
			gagPeaks[0].setStress(totalStresses[0]);
			gagPeaks[0].setEvents(incStress.getEvent());
			gagPeaks[0].setIssyCodes(incStress.getIssyCode());
			gagPeaks[0].setSegment(segment);
		}
		if (totalStresses[0] <= gagPeaks[1].getStress()) {
			gagPeaks[1].setStress(totalStresses[0]);
			gagPeaks[1].setEvents(incStress.getEvent());
			gagPeaks[1].setIssyCodes(incStress.getIssyCode());
			gagPeaks[1].setSegment(segment);
		}

		// return GAG peaks
		return gagPeaks;
	}

	/**
	 * Modifies and returns stress according to event, segment and stress type.
	 *
	 * @param interpolator
	 *            2 points delta-t interpolator.
	 * @param segment
	 *            Segment.
	 * @param stress
	 *            Stress value extracted from STF file.
	 * @return The modified stress value.
	 */
	private double modify2PointDTStress(DT2PointsInterpolator interpolator, Segment segment, double stress) {

		// apply overall factors
		String method = input_.getStressModificationMethod(GenerateStressSequenceInput.DELTAT);
		if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
			stress *= input_.getStressModificationValue(GenerateStressSequenceInput.DELTAT);
		}
		else if (method.equals(GenerateStressSequenceInput.ADD)) {
			stress += input_.getStressModificationValue(GenerateStressSequenceInput.DELTAT);
		}
		else if (method.equals(GenerateStressSequenceInput.SET)) {
			stress = input_.getStressModificationValue(GenerateStressSequenceInput.DELTAT);
		}

		// apply segment factors
		if (segment != null && input_.getSegmentFactors() != null) {
			for (SegmentFactor sFactor : input_.getSegmentFactors())
				if (sFactor.getSegment().equals(segment)) {
					method = sFactor.getModifierMethod(GenerateStressSequenceInput.DELTAT);
					if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
						stress *= sFactor.getModifierValue(GenerateStressSequenceInput.DELTAT);
					}
					else if (method.equals(GenerateStressSequenceInput.ADD)) {
						stress += sFactor.getModifierValue(GenerateStressSequenceInput.DELTAT);
					}
					else if (method.equals(GenerateStressSequenceInput.SET)) {
						stress = sFactor.getModifierValue(GenerateStressSequenceInput.DELTAT);
					}
					break;
				}
		}

		// apply loadcase factors
		if (input_.getLoadcaseFactors() != null) {
			for (LoadcaseFactor eFactor : input_.getLoadcaseFactors())
				if (eFactor.getLoadcaseNumber().equals(interpolator.getIssyCodeSup()) || eFactor.getLoadcaseNumber().equals(interpolator.getIssyCodeInf())) {
					method = eFactor.getModifierMethod();
					if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
						stress *= eFactor.getModifierValue();
					}
					else if (method.equals(GenerateStressSequenceInput.ADD)) {
						stress += eFactor.getModifierValue();
					}
					else if (method.equals(GenerateStressSequenceInput.SET)) {
						stress = eFactor.getModifierValue();
					}
					break;
				}
		}

		// return modified stress
		return stress;
	}

	/**
	 * Sets increment stress to given ANA peak.
	 *
	 * @param selectSTFStress
	 *            Database statement for selecting stress from STF file.
	 * @param selectIncrementIssyCode
	 *            Database statement for selecting increment issy code.
	 * @param classCode
	 *            14 digit class code.
	 * @param onegCode
	 *            1g code.
	 * @param segment
	 *            Segment.
	 * @param contributions
	 *            Damage contributions.
	 * @return Returns the increment stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private IncStress getIncStress(PreparedStatement selectSTFStress, PreparedStatement selectIncrementIssyCode, String classCode, String onegCode, Segment segment, ArrayList<DamageContribution> contributions) throws Exception {

		// add default increment stress
		IncStress incStress = new IncStress();
		double[] totalIncrementStress = new double[contributions.size() + 1];

		// loop over increments
		for (int i = 0; i < 5; i++) {

			// get increment block
			String block = classCode.substring(2 * i + 4, 2 * i + 6);

			// no increment
			if (block.equals("00")) {
				continue;
			}

			// set parameters
			selectIncrementIssyCode.setString(1, onegCode); // 1g code
			selectIncrementIssyCode.setInt(2, i + 1); // increment number
			selectIncrementIssyCode.setString(3, block.substring(1)); // direction number
			selectIncrementIssyCode.setString(4, block.substring(0, 1)); // factor number

			// query issy code, factor and event name
			try (ResultSet resultSet = selectIncrementIssyCode.executeQuery()) {

				// loop over increments
				while (resultSet.next()) {

					// get issy code, factor and event name
					String issyCode = resultSet.getString("issy_code");
					double factor = resultSet.getDouble("factor_" + block.substring(0, 1));
					String event = resultSet.getString("flight_phase");

					// compute increment stresses
					double stress = factor * getSTFStress(selectSTFStress, issyCode);
					double[] stresses = modifyIncStress(issyCode, segment, stress, contributions);

					// add to total increment stress
					for (int j = 0; j < stresses.length; j++) {
						totalIncrementStress[j] += stresses[j];
					}

					// add event and load case
					incStress.addEvent(event);
					incStress.addIssyCode(issyCode);
				}
			}
		}

		// set increment stresses
		incStress.setStress(totalIncrementStress);
		return incStress;
	}

	/**
	 * Modifies and returns stress according to event, segment and stress type.
	 *
	 * @param issyCode
	 *            ISSY code.
	 * @param segment
	 *            Segment.
	 * @param stress
	 *            Stress value extracted from STF file.
	 * @param contributions
	 *            Damage contributions.
	 * @return The modified stress value.
	 */
	private double[] modifyIncStress(String issyCode, Segment segment, double stress, ArrayList<DamageContribution> contributions) {

		// apply overall factors
		String method = input_.getStressModificationMethod(GenerateStressSequenceInput.INCREMENT);
		if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
			stress *= input_.getStressModificationValue(GenerateStressSequenceInput.INCREMENT);
		}
		else if (method.equals(GenerateStressSequenceInput.ADD)) {
			stress += input_.getStressModificationValue(GenerateStressSequenceInput.INCREMENT);
		}
		else if (method.equals(GenerateStressSequenceInput.SET)) {
			stress = input_.getStressModificationValue(GenerateStressSequenceInput.INCREMENT);
		}

		// apply segment factors
		if (segment != null && input_.getSegmentFactors() != null) {
			for (SegmentFactor sFactor : input_.getSegmentFactors())
				if (sFactor.getSegment().equals(segment)) {
					method = sFactor.getModifierMethod(GenerateStressSequenceInput.INCREMENT);
					if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
						stress *= sFactor.getModifierValue(GenerateStressSequenceInput.INCREMENT);
					}
					else if (method.equals(GenerateStressSequenceInput.ADD)) {
						stress += sFactor.getModifierValue(GenerateStressSequenceInput.INCREMENT);
					}
					else if (method.equals(GenerateStressSequenceInput.SET)) {
						stress = sFactor.getModifierValue(GenerateStressSequenceInput.INCREMENT);
					}
					break;
				}
		}

		// apply loadcase factors
		if (input_.getLoadcaseFactors() != null) {
			for (LoadcaseFactor eFactor : input_.getLoadcaseFactors())
				if (eFactor.getLoadcaseNumber().equals(issyCode)) {
					method = eFactor.getModifierMethod();
					if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
						stress *= eFactor.getModifierValue();
					}
					else if (method.equals(GenerateStressSequenceInput.ADD)) {
						stress += eFactor.getModifierValue();
					}
					else if (method.equals(GenerateStressSequenceInput.SET)) {
						stress = eFactor.getModifierValue();
					}
					break;
				}
		}

		// create incremental stresses array
		double[] stresses = new double[contributions.size() + 1];
		for (int i = 0; i < stresses.length; i++) {
			stresses[i] = stress;
		}

		// loop over stresses
		for (int i = 1; i < stresses.length; i++) {
			DamageContribution contribution = contributions.get(i - 1);
			if (contribution.getType().equals(ContributionType.INCREMENT)) {
				for (LoadcaseFactor eFactor : contribution.getLoadcaseFactors())
					if (eFactor.getLoadcaseNumber().equals(issyCode)) {
						stresses[i] = 0.0;
						break;
					}
			}
		}

		// return modified stress
		return stresses;
	}

	/**
	 * Sets 1g stress to given ANA peak.
	 *
	 * @param selectSTFStress
	 *            Database statement for selecting stresses from STF file.
	 * @param select1gIssyCode
	 *            Database statement for selecting 1g issy code from TXT file.
	 * @param onegCode
	 *            1g code.
	 * @return The 1g stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Stress get1GStress(PreparedStatement selectSTFStress, PreparedStatement select1gIssyCode, String onegCode) throws Exception {

		// get 1G issy code and event name
		String issyCode = null, event = null, segmentName = null;
		int segmentNum = -1;
		select1gIssyCode.setString(1, onegCode); // 1g code
		try (ResultSet resultSet = select1gIssyCode.executeQuery()) {
			while (resultSet.next()) {

				// get issy code and event name
				issyCode = resultSet.getString("issy_code");
				event = resultSet.getString("flight_phase");
				segmentNum = resultSet.getInt("oneg_order");

				// extract segment name
				segmentName = Utility.extractSegmentName(event);
			}
		}

		// create segment
		Segment segment = new Segment(segmentName, segmentNum);

		// compute and modify 1g stress
		double stress = getSTFStress(selectSTFStress, issyCode);
		stress = modify1GStress(issyCode, segment, GenerateStressSequenceInput.ONEG, stress);

		// set to peak
		return new Stress(stress, event, issyCode, segment);
	}

	/**
	 * Returns STF stress for given issy code.
	 *
	 * @param selectSTFStress
	 *            Database statement.
	 * @param issyCode
	 *            ISSY code.
	 * @return STF stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double getSTFStress(PreparedStatement selectSTFStress, String issyCode) throws Exception {
		StressComponent component = input_.getStressComponent();
		double angle = input_.getRotationAngle();
		selectSTFStress.setString(1, issyCode); // issy code
		try (ResultSet resultSet = selectSTFStress.executeQuery()) {
			while (resultSet.next())
				if (component.equals(StressComponent.NORMAL_X))
					return resultSet.getDouble("stress_x");
				else if (component.equals(StressComponent.NORMAL_Y))
					return resultSet.getDouble("stress_y");
				else if (component.equals(StressComponent.SHEAR_XY))
					return resultSet.getDouble("stress_xy");
				else if (component.equals(StressComponent.ROTATED)) {
					double x = resultSet.getDouble("stress_x");
					double y = resultSet.getDouble("stress_y");
					double xy = resultSet.getDouble("stress_xy");
					return 0.5 * (x + y) + 0.5 * (x - y) * Math.cos(2 * angle) + xy * Math.sin(2 * angle);
				}
		}
		return 0.0;
	}

	/**
	 * Modifies and returns stress according to event, segment and stress type.
	 *
	 * @param issyCode
	 *            ISSY code.
	 * @param segment
	 *            Segment.
	 * @param stressType
	 *            Stress type (1g, increment, delta-p, delta-t or total stress).
	 * @param stress
	 *            Stress value extracted from STF file.
	 * @return The modified stress value.
	 */
	private double modify1GStress(String issyCode, Segment segment, int stressType, double stress) {

		// apply overall factors
		String method = input_.getStressModificationMethod(stressType);
		if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
			stress *= input_.getStressModificationValue(stressType);
		}
		else if (method.equals(GenerateStressSequenceInput.ADD)) {
			stress += input_.getStressModificationValue(stressType);
		}
		else if (method.equals(GenerateStressSequenceInput.SET)) {
			stress = input_.getStressModificationValue(stressType);
		}

		// apply segment factors
		if (segment != null && input_.getSegmentFactors() != null) {
			for (SegmentFactor sFactor : input_.getSegmentFactors())
				if (sFactor.getSegment().equals(segment)) {
					method = sFactor.getModifierMethod(stressType);
					if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
						stress *= sFactor.getModifierValue(stressType);
					}
					else if (method.equals(GenerateStressSequenceInput.ADD)) {
						stress += sFactor.getModifierValue(stressType);
					}
					else if (method.equals(GenerateStressSequenceInput.SET)) {
						stress = sFactor.getModifierValue(stressType);
					}
					break;
				}
		}

		// apply loadcase factors
		if (input_.getLoadcaseFactors() != null) {
			for (LoadcaseFactor eFactor : input_.getLoadcaseFactors())
				if (eFactor.getLoadcaseNumber().equals(issyCode)) {
					method = eFactor.getModifierMethod();
					if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
						stress *= eFactor.getModifierValue();
					}
					else if (method.equals(GenerateStressSequenceInput.ADD)) {
						stress += eFactor.getModifierValue();
					}
					else if (method.equals(GenerateStressSequenceInput.SET)) {
						stress = eFactor.getModifierValue();
					}
					break;
				}
		}

		// return modified stress
		return stress;
	}

	/**
	 * Writes out flight headers to output STH files.
	 *
	 * @param writers
	 *            File writers.
	 * @param anaFlights
	 *            ANA flights.
	 * @param flightPeaks
	 *            Number of peaks of the flight.
	 * @return Flight validity.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double writeFlightHeaders(BufferedWriter[] writers, ResultSet anaFlights, int flightPeaks) throws Exception {

		// update info
		String name = anaFlights.getString("name");
		updateMessage("Generating flight '" + name + "'...");

		// create first line of flight info
		double validity = anaFlights.getDouble("validity");
		String line1 = String.format("%10s", format_.format(validity));
		line1 += String.format("%10s", format_.format(anaFlights.getDouble("block_size")));

		// create second line of flight info
		String line2 = String.format("%10s", Integer.toString(flightPeaks));
		for (int i = 0; i < 62; i++) {
			line2 += " ";
		}
		line2 += (name.startsWith("TF_") ? name.substring(3) : name) + " " + anaFlights.getString("severity");

		// create second line of flight info for GAG contribution
		String line3 = null;
		if (input_.getGAGContributionIndex() != -1) {
			line3 = String.format("%10s", "3");
			for (int i = 0; i < 62; i++) {
				line3 += " ";
			}
			line3 += (name.startsWith("TF_") ? name.substring(3) : name) + " " + anaFlights.getString("severity");
		}

		// write headers
		for (int i = 0; i < writers.length; i++) {
			writers[i].write(line1);
			writers[i].newLine();
			if (line3 != null && i == input_.getGAGContributionIndex() + 1) {
				writers[i].write(line3);
			}
			else {
				writers[i].write(line2);
			}
			writers[i].newLine();
		}

		// return flight validity
		return validity;
	}

	/**
	 * Returns maximum number of peaks per typical flight. This is used to determine if the extended in-build engine should be used for analysis.
	 *
	 * @param statement
	 *            Database statement.
	 * @param anaFileID
	 *            ANA file ID.
	 * @return Maximum number of peaks per typical flight.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static int getMaxPeaksPerFlight(Statement statement, int anaFileID) throws Exception {
		int maxPeaks = 0;
		String sql = "select num_peaks from ana_flights where file_id = " + anaFileID + " order by num_peaks desc";
		statement.setMaxRows(1);
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				maxPeaks = resultSet.getInt("num_peaks");
			}
		}
		statement.setMaxRows(0);
		return maxPeaks;
	}

	/**
	 * Returns the number of peaks of the ANA file.
	 *
	 * @param statement
	 *            Database statement.
	 * @param anaFileID
	 *            ANA file ID.
	 * @return Number of peaks of the ANA file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static int getNumberOfPeaks(Statement statement, int anaFileID) throws Exception {
		String sql = "select sum(num_peaks) as totalPeaks from ana_flights where file_id = " + anaFileID;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next())
				return resultSet.getInt("totalPeaks");
		}
		return 0;
	}

	/**
	 * Returns delta-t interpolation, or null if no delta-t interpolation is supplied.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param txtFileID
	 *            TXT file ID.
	 * @return Delta-t interpolation, or null if no delta-t interpolation is supplied.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DTInterpolator getDTInterpolator(Connection connection, Statement statement, int txtFileID) throws Exception {

		// no delta-t interpolation
		DTInterpolation interpolation = input_.getDTInterpolation();
		if (interpolation.equals(DTInterpolation.NONE))
			return null;

		// get reference temperatures
		double[] refTemp = new double[2];
		refTemp[0] = input_.getReferenceDTSup() == null ? 0.0 : input_.getReferenceDTSup().doubleValue();
		refTemp[1] = input_.getReferenceDTInf() == null ? 0.0 : input_.getReferenceDTInf().doubleValue();

		// set variables
		DTInterpolator dtInterpolator = null;
		StressComponent component = input_.getStressComponent();
		double angle = input_.getRotationAngle();

		// get delta-p issy code from TXT file
		boolean supLCFound = false, infLCFound = false;
		String sql = null;
		if (interpolation.equals(DTInterpolation.ONE_POINT)) {
			sql = "select flight_phase, issy_code from txt_codes where file_id = " + txtFileID + " and issy_code = '" + input_.getDTLoadcaseSup() + "'";
		}
		else if (interpolation.equals(DTInterpolation.TWO_POINTS)) {
			sql = "select flight_phase, issy_code from txt_codes where file_id = " + txtFileID + " and (issy_code = '" + input_.getDTLoadcaseSup() + "' or issy_code = '" + input_.getDTLoadcaseInf() + "')";
		}
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// prepare statement to get STF stresses
			int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
			int stressTableID = stfFile_ == null ? stressTableID_ : stfFile_.getStressTableID();
			sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stressTableID + " where file_id = " + stfID + " and issy_code = ?";
			try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

				// loop over delta-t cases
				while (resultSet.next()) {

					// set issy code
					String issyCode = resultSet.getString("issy_code");
					statement2.setString(1, issyCode);

					// get delta-p stress from STF file
					double stress = 0.0;
					try (ResultSet resultSet2 = statement2.executeQuery()) {
						while (resultSet2.next())
							if (component.equals(StressComponent.NORMAL_X)) {
								stress = resultSet2.getDouble("stress_x");
							}
							else if (component.equals(StressComponent.NORMAL_Y)) {
								stress = resultSet2.getDouble("stress_y");
							}
							else if (component.equals(StressComponent.SHEAR_XY)) {
								stress = resultSet2.getDouble("stress_xy");
							}
							else if (component.equals(StressComponent.ROTATED)) {
								double x = resultSet2.getDouble("stress_x");
								double y = resultSet2.getDouble("stress_y");
								double xy = resultSet2.getDouble("stress_xy");
								stress = 0.5 * (x + y) + 0.5 * (x - y) * Math.cos(2 * angle) + xy * Math.sin(2 * angle);
							}
					}

					// 1 point interpolation
					if (interpolation.equals(DTInterpolation.ONE_POINT)) {
						dtInterpolator = new DT1PointInterpolator(resultSet.getString("flight_phase"), issyCode, stress, refTemp[0]);
						supLCFound = true;
						break;
					}

					// 2 points interpolation
					else if (interpolation.equals(DTInterpolation.TWO_POINTS)) {

						// create interpolator
						if (dtInterpolator == null) {
							dtInterpolator = new DT2PointsInterpolator();
						}

						// superior load case
						if (issyCode.equals(input_.getDTLoadcaseSup())) {
							((DT2PointsInterpolator) dtInterpolator).setSupParameters(resultSet.getString("flight_phase"), issyCode, stress, refTemp[0]);
							supLCFound = true;
						}

						// inferior load case
						else if (issyCode.equals(input_.getDTLoadcaseInf())) {
							((DT2PointsInterpolator) dtInterpolator).setInfParameters(resultSet.getString("flight_phase"), issyCode, stress, refTemp[1]);
							infLCFound = true;
						}
					}
				}
			}
		}

		// delta-t load case could not be found
		if (interpolation.equals(DTInterpolation.ONE_POINT) && !supLCFound) {
			warnings_ += "Delta-T superior load case '" + input_.getDTLoadcaseSup() + "' could not be found.\n";
		}
		else if (interpolation.equals(DTInterpolation.TWO_POINTS)) {
			if (!supLCFound) {
				warnings_ += "Delta-T superior load case '" + input_.getDTLoadcaseSup() + "' could not be found.\n";
			}
			if (!infLCFound) {
				warnings_ += "Delta-T inferior load case '" + input_.getDTLoadcaseInf() + "' could not be found.\n";
			}
		}

		// return interpolator
		return dtInterpolator;
	}

	/**
	 * Returns delta-p ratio.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param anaFileID
	 *            ANA file ID.
	 * @param txtFileID
	 *            TXT file ID.
	 * @param convTableID
	 *            Conversion table ID.
	 * @return Delta-p ratio.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DPRatio getDPRatio(Connection connection, Statement statement, int anaFileID, int txtFileID, int convTableID) throws Exception {

		// get reference pressure
		double refDP = getRefDP(connection, convTableID, anaFileID);

		// set variables
		DPRatio dpRatio = null;
		StressComponent component = input_.getStressComponent();
		double angle = input_.getRotationAngle();

		// create statement to get delta-p event name and issy code
		String sql = null;
		if (input_.getDPLoadcase() == null) {
			sql = "select flight_phase, issy_code from txt_codes where file_id = " + txtFileID + " and dp_case = 1";
		}
		else {
			sql = "select flight_phase from txt_codes where file_id = " + txtFileID + " and issy_code = '" + input_.getDPLoadcase() + "'";
		}

		// execute statement
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// prepare statement to get STF stresses
			int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
			int stressTableID = stfFile_ == null ? stressTableID_ : stfFile_.getStressTableID();
			sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stressTableID + " where file_id = " + stfID + " and issy_code = ?";
			try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

				// loop over delta-p cases
				while (resultSet.next()) {

					// set issy code
					String issyCode = input_.getDPLoadcase() == null ? resultSet.getString("issy_code") : input_.getDPLoadcase();
					statement2.setString(1, issyCode);

					// get delta-p stress from STF file
					double stress = 0.0;
					try (ResultSet resultSet2 = statement2.executeQuery()) {
						while (resultSet2.next())
							if (component.equals(StressComponent.NORMAL_X)) {
								stress = resultSet2.getDouble("stress_x");
							}
							else if (component.equals(StressComponent.NORMAL_Y)) {
								stress = resultSet2.getDouble("stress_y");
							}
							else if (component.equals(StressComponent.SHEAR_XY)) {
								stress = resultSet2.getDouble("stress_xy");
							}
							else if (component.equals(StressComponent.ROTATED)) {
								double x = resultSet2.getDouble("stress_x");
								double y = resultSet2.getDouble("stress_y");
								double xy = resultSet2.getDouble("stress_xy");
								stress = 0.5 * (x + y) + 0.5 * (x - y) * Math.cos(2 * angle) + xy * Math.sin(2 * angle);
							}
					}

					// create delta-p ratio
					dpRatio = new DPRatio(refDP, stress, resultSet.getString("flight_phase"), issyCode);
					break;
				}
			}
		}

		// delta-p load case could not be found
		if (input_.getDPLoadcase() != null && dpRatio == null) {
			warnings_ += "Delta-P load case '" + input_.getDPLoadcase() + "' could not be found.\n";
		}

		// return delta-p ratio
		return dpRatio;
	}

	/**
	 * Returns reference delta-p pressure. The process is composed of the following logic;
	 * <UL>
	 * <LI>If the reference delta-p pressure is supplied by the user, this value is returned. Otherwise, the process falls back to next step.
	 * <LI>If the reference delta-p pressure is supplied within the conversion table, this value is returned. Otherwise, the process falls back to next step.
	 * <LI>Maximum pressure value within the ANA file is returned.
	 * </UL>
	 *
	 * @param connection
	 *            Database connection.
	 * @param convTableID
	 *            Conversion table ID.
	 * @param anaFileID
	 *            ANA file ID.
	 * @return Reference delta-p pressure.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double getRefDP(Connection connection, int convTableID, int anaFileID) throws Exception {

		// initialize reference delta-p
		double refPressure = input_.getReferenceDP() == null ? 0.0 : input_.getReferenceDP().doubleValue();

		// no reference delta-p value given
		if (refPressure == 0.0) {
			// create statement
			try (Statement statement = connection.createStatement()) {

				// get reference pressure from conversion table
				String sql = "select ref_dp from xls_files where file_id = " + convTableID;
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						refPressure = resultSet.getDouble("ref_dp");
					}
				}

				// reference pressure is zero
				if (refPressure == 0.0) {

					// get maximum pressure from ANA file
					sql = "select max_dp from ana_flights where file_id = " + anaFileID + " order by max_dp desc";
					statement.setMaxRows(1);
					try (ResultSet resultSet = statement.executeQuery(sql)) {
						while (resultSet.next()) {
							refPressure = resultSet.getDouble("max_dp");
						}
					}
					statement.setMaxRows(0);
				}
			}
		}

		// return reference pressure
		return refPressure;
	}

	/**
	 * Writes STH file header.
	 *
	 * @param writer
	 *            File writer.
	 * @param name
	 *            Damage contribution name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void writeSTHHeader(BufferedWriter writer, String name) throws Exception {
		writer.write(" # STH file for damage contribution: " + name);
		writer.newLine();
		writer.write(" #");
		writer.newLine();
		writer.write(" #");
		writer.newLine();
		writer.write(" #");
		writer.newLine();
	}
}
