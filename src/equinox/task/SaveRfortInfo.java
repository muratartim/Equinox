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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import equinox.Equinox;
import equinox.data.AnalysisEngine;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.data.fileType.StressSequence;
import equinox.data.input.RfortExtendedInput;
import equinox.data.ui.RfortDirectOmission;
import equinox.data.ui.RfortOmission;
import equinox.data.ui.RfortPercentOmission;
import equinox.data.ui.SerializableRfortPilotPoint;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.SingleInputTask;

/**
 * Class for save RFORT info task.
 *
 * @author Murat Artim
 * @date Mar 7, 2016
 * @time 1:56:19 PM
 */
public class SaveRfortInfo extends InternalEquinoxTask<HashMap<String, Double>> implements ShortRunningTask, SingleInputTask<SpectrumItem> {

	/** Equivalent stress type. */
	public static final String FATIGUE = "Fatigue", PREFFAS = "Preffas", LINEAR = "Linear";

	/** RFORT input. */
	private final RfortExtendedInput input_;

	/** Analysis ID. */
	private final int analysisID_;

	/** Omission name and type. */
	private final String omissionName_, omissionType_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/** ISAMI version. */
	private IsamiVersion isamiVersion_;

	/** ISAMI sub version. */
	private IsamiSubVersion isamiSubVersion_;

	/** True compression should be applied in propagation analysis. */
	private boolean applyCompression_;

	/** Equivalent stress. */
	private SpectrumItem eqStress_;

	/**
	 * Creates save RFORT info task.
	 *
	 * @param input
	 *            RFORT input.
	 * @param eqStress
	 *            Equivalent stress. Can be null for automatic execution.
	 * @param analysisID
	 *            Analysis ID.
	 * @param omissionName
	 *            Omission name.
	 * @param omissionType
	 *            Omission type.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public SaveRfortInfo(RfortExtendedInput input, SpectrumItem eqStress, int analysisID, String omissionName, String omissionType, AnalysisEngine analysisEngine) {
		input_ = input;
		eqStress_ = eqStress;
		analysisID_ = analysisID;
		omissionName_ = omissionName;
		omissionType_ = omissionType;
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
	public SaveRfortInfo setIsamiEngineInputs(IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
		return this;
	}

	@Override
	public void setAutomaticInput(SpectrumItem eqStress) {
		eqStress_ = eqStress;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save RFORT info for equivalent stress '" + eqStress_.getName() + "'";
	}

	@Override
	protected HashMap<String, Double> call() throws Exception {

		// progress info
		updateMessage("Saving RFORT info...");

		// initialize stress amplitudes
		HashMap<String, Double> stressAmplitudes = null;

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// create statement
				try (Statement statement = connection.createStatement()) {

					// get average number of peaks
					int numPeaks = getAverageNumberOfPeaks(statement);

					// get equivalent stress and maximum stress amplitude
					double[] stressMaxAmp = getEqStressAndMaxStressAmp(statement);

					// get pilot point name and equivalent stress type
					String[] ppNameStressType = getPPNameAndStressType();

					// get omission value
					double omissionValue = getOmissionValue(ppNameStressType[0], stressMaxAmp[1]);

					// save RFORT info to database
					saveRfortInfo(numPeaks, stressMaxAmp, ppNameStressType, omissionValue, connection);

					// get stress amplitudes
					stressAmplitudes = getStressAmplitudes(ppNameStressType[1], statement);
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return stress amplitudes
				return stressAmplitudes;
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

		// run RFORT processes (if necessary)
		try {

			// get stress amplitudes
			HashMap<String, Double> stressAmplitudes = get();

			// run
			if (stressAmplitudes != null) {
				runRfort(stressAmplitudes);
			}
		}

		// exception occurred
		catch (Exception e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Creates and runs RFORT analyses.
	 *
	 * @param stressAmplitudes
	 *            RFORT stress amplitudes.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void runRfort(HashMap<String, Double> stressAmplitudes) throws Exception {
		for (RfortOmission omission : input_.getOmissions()) {
			taskPanel_.getOwner().runTaskInParallel(new RfortAnalysis(input_, omission, analysisID_, stressAmplitudes, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_));
		}
	}

	/**
	 * Returns omission value.
	 *
	 * @param ppName
	 *            Pilot point name.
	 * @param maxStressAmp
	 *            Maximum stress amplitude.
	 * @return Omission value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double getOmissionValue(String ppName, double maxStressAmp) throws Exception {

		// initial analysis
		if (omissionName_.equals(RfortOmission.INITIAL_ANALYSIS) || omissionType_.equals(RfortOmission.NO_OMISSION))
			return 0.0;

		// loop over omissions
		for (RfortOmission omission : input_.getOmissions()) {

			// omission name doesn't match
			if (!omission.toString().equals(omissionName_)) {
				continue;
			}

			// omission type doesn't match
			if (!omission.getOmissionType().equals(omissionType_)) {
				continue;
			}

			// percent omission
			if (omission instanceof RfortPercentOmission) {
				RfortPercentOmission percentOmission = (RfortPercentOmission) omission;
				return (maxStressAmp * percentOmission.getPercentOmission()) / 100.0;
			}

			// direct omission
			else if (omission instanceof RfortDirectOmission) {
				RfortDirectOmission directOmission = (RfortDirectOmission) omission;
				Iterator<Entry<String, Double>> omissions = directOmission.getOmissions().entrySet().iterator();
				while (omissions.hasNext()) {
					Entry<String, Double> o = omissions.next();
					if (ppName.equals(o.getKey()))
						return o.getValue();
				}
			}
		}

		// no omission
		return 0.0;
	}

	/**
	 * Computes and returns average number of peaks.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Average number of peaks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int getAverageNumberOfPeaks(Statement statement) throws Exception {

		// progress info
		updateMessage("Getting average number of peaks...");

		// initialize average number of peaks
		int numPeaks = -1;

		// get stress sequence
		StressSequence sequence = null;
		if (eqStress_ instanceof FatigueEquivalentStress) {
			sequence = ((FatigueEquivalentStress) eqStress_).getParentItem();
		}
		else if (eqStress_ instanceof PreffasEquivalentStress) {
			sequence = ((PreffasEquivalentStress) eqStress_).getParentItem();
		}
		else if (eqStress_ instanceof LinearEquivalentStress) {
			sequence = ((LinearEquivalentStress) eqStress_).getParentItem();
		}

		// no stress sequence found
		if (sequence == null)
			throw new Exception("Cannot compute average number of peaks for equivalent stress '" + eqStress_.getName() + "'.");

		// create and execute statement
		String sql = "select sum(num_peaks*validity) as peaks, sum(validity) as validities";
		sql += " from sth_flights where file_id = " + sequence.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				numPeaks = (int) (resultSet.getInt("peaks") / resultSet.getDouble("validities"));
			}
		}

		// return number of peaks
		return numPeaks;
	}

	/**
	 * Returns an array containing equivalent stress and maximum stress amplitude.
	 *
	 * @param statement
	 *            Database statement.
	 * @return An array containing equivalent stress and maximum stress amplitude.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double[] getEqStressAndMaxStressAmp(Statement statement) throws Exception {

		// progress info
		updateMessage("Getting equivalent stress and maximum stress amplitude...");

		// create array to store equivalent stress and maximum stress amplitude
		double[] stressMaxAmp = new double[2];

		// set table name
		String tableName = "";
		if (eqStress_ instanceof FatigueEquivalentStress) {
			tableName = "fatigue_equivalent_stresses";
		}
		else if (eqStress_ instanceof PreffasEquivalentStress) {
			tableName = "preffas_equivalent_stresses";
		}
		else if (eqStress_ instanceof LinearEquivalentStress) {
			tableName = "linear_equivalent_stresses";
		}

		// execute statement
		String sql = "select stress, max_stress, min_stress from " + tableName + " where id = " + eqStress_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				stressMaxAmp[0] = resultSet.getDouble("stress");
				stressMaxAmp[1] = Math.abs(resultSet.getDouble("max_stress") - resultSet.getDouble("min_stress")) / 2.0;
			}
		}

		// return array
		return stressMaxAmp;
	}

	/**
	 * Returns an array containing pilot point name and equivalent stress type.
	 *
	 * @return An array containing pilot point name and equivalent stress type.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String[] getPPNameAndStressType() throws Exception {

		// progress info
		updateMessage("Getting pilot point name and equivalent stress type...");

		// create array to store pilot point name and equivalent stress type
		String[] ppNameStressType = new String[2];

		// set pilot point name and equivalent stress type
		if (eqStress_ instanceof FatigueEquivalentStress) {
			ppNameStressType[0] = ((FatigueEquivalentStress) eqStress_).getParentItem().getParentItem().getName();
			ppNameStressType[1] = FATIGUE;
		}
		else if (eqStress_ instanceof PreffasEquivalentStress) {
			ppNameStressType[0] = ((PreffasEquivalentStress) eqStress_).getParentItem().getParentItem().getName();
			ppNameStressType[1] = PREFFAS;
		}
		else if (eqStress_ instanceof LinearEquivalentStress) {
			ppNameStressType[0] = ((LinearEquivalentStress) eqStress_).getParentItem().getParentItem().getName();
			ppNameStressType[1] = LINEAR;
		}

		// return array
		return ppNameStressType;
	}

	/**
	 * Saves RFORT info to database.
	 *
	 * @param numPeaks
	 *            Average number of peaks.
	 * @param stressMaxAmp
	 *            Array containing equivalent stress and maximum stress amplitude.
	 * @param ppNameStressType
	 *            Array containing pilot point name and equivalent stress type.
	 * @param omissionValue
	 *            Omission value.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveRfortInfo(int numPeaks, double[] stressMaxAmp, String[] ppNameStressType, double omissionValue, Connection connection) throws Exception {

		// progress info
		updateMessage("Saving RFORT info into database...");

		// save
		String sql = "insert into rfort_outputs(analysis_id, pp_name, stress_id, included_in_rfort, stress_factor, material_name, omission_name, omission_type, num_peaks, stress_amp, omission_value, eq_stress, stress_type) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {

			// set analysis ID, pilot point name and STF file id
			statement.setInt(1, analysisID_);
			statement.setString(2, ppNameStressType[0]);
			statement.setInt(3, eqStress_.getID());

			// set stress factor and material name
			for (SerializableRfortPilotPoint pp : input_.getPilotPoints()) {
				if (pp.getName().equals(ppNameStressType[0])) {

					// set RFORT inclusion
					statement.setBoolean(4, pp.getIncludeInRfort());

					// set stress factor
					statement.setDouble(5, Double.parseDouble(pp.getFactor()));

					// set material name
					String materialName = null;
					if (ppNameStressType[1].equals(FATIGUE)) {
						materialName = pp.getFatigueMaterial().toString();
					}
					else if (ppNameStressType[1].equals(PREFFAS)) {
						materialName = pp.getPreffasMaterial().toString();
					}
					else if (ppNameStressType[1].equals(LINEAR)) {
						materialName = pp.getLinearMaterial().toString();
					}
					if ((materialName != null) && (materialName.length() > 500)) {
						materialName = materialName.substring(0, 499);
					}
					statement.setString(6, materialName);
					break;
				}
			}

			// set other outputs
			statement.setString(7, omissionName_);
			statement.setString(8, omissionType_);
			statement.setInt(9, numPeaks);
			statement.setDouble(10, stressMaxAmp[1]);
			statement.setDouble(11, omissionValue);
			statement.setDouble(12, stressMaxAmp[0]);
			statement.setString(13, ppNameStressType[1]);
			statement.executeUpdate();
		}
	}

	/**
	 * Returns maximum stress amplitudes of all pilot points or null if not all initial analyses are complete.
	 *
	 * @param stressType
	 *            Equivalent stress type.
	 * @param statement
	 *            Database statement.
	 * @return Maximum stress amplitudes of all pilot points or null if not all initial analyses are complete.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private HashMap<String, Double> getStressAmplitudes(String stressType, Statement statement) throws Exception {

		// progress info
		updateMessage("Getting maximum stress amplitudes of all pilot points...");

		// initialize stress amplitudes
		HashMap<String, Double> stressAmplitudes = null;

		// initial run and fatigue equivalent stress
		if (omissionName_.equals(RfortOmission.INITIAL_ANALYSIS) && stressType.equals(FATIGUE)) {

			// get pilot point name and maximum stress amplitudes for initial run
			String sql = "select pp_name, stress_amp from rfort_outputs ";
			sql += "where analysis_id = " + analysisID_ + " and omission_name = '" + omissionName_ + "' and stress_type = '" + FATIGUE + "'";
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					if (stressAmplitudes == null) {
						stressAmplitudes = new HashMap<>();
					}
					stressAmplitudes.put(resultSet.getString("pp_name"), resultSet.getDouble("stress_amp"));
				}
			}
		}

		// not all initial analyses are complete
		if ((stressAmplitudes != null) && (stressAmplitudes.size() < input_.getPilotPoints().size()))
			return null;

		// return stress amplitudes
		return stressAmplitudes;
	}
}
