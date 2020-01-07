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
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.AnalysisEngine;
import equinox.data.DPRatio;
import equinox.data.DT1PointInterpolator;
import equinox.data.DT2PointsInterpolator;
import equinox.data.DTInterpolation;
import equinox.data.DTInterpolator;
import equinox.data.EmbeddedTask;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.LoadcaseFactor;
import equinox.data.OneGStresses;
import equinox.data.Segment;
import equinox.data.SegmentFactor;
import equinox.data.Settings;
import equinox.data.StressComponent;
import equinox.data.fileType.DamageAngle;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.input.DamageAngleInput;
import equinox.data.input.FastEquivalentStressInput;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.Material;
import equinox.plugin.FileType;
import equinox.process.ESAProcess;
import equinox.process.InbuiltDAA;
import equinox.process.SafeDAA;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.task.serializableTask.SerializableDamageAngleAnalysis;
import equinox.utility.Utility;

/**
 * Class for damage angle analysis task.
 *
 * @author Murat Artim
 * @date Aug 6, 2014
 * @time 4:30:36 PM
 */
public class DamageAngleAnalysis extends TemporaryFileCreatingTask<DamageAngle> implements LongRunningTask, SavableTask, SingleInputTask<STFFile>, AutomaticTaskOwner<DamageAngle> {

	/** Result index. */
	public static final int ANGLE_INDEX = 0, ANGLE = 1, STRESS = 2;

	/** The owner STF file. */
	private STFFile stfFile_;

	/** STF file ID. */
	private final Integer stfID_, stressTableID_;

	/** STF file name. */
	private final String stfName_;

	/** The owner spectrum. */
	private final Spectrum spectrum_;

	/** Input. */
	private final DamageAngleInput input_;

	/** Material. */
	private final FatigueMaterial material_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/** ISAMI version. */
	private IsamiVersion isamiVersion_;

	/** ISAMI sub version. */
	private IsamiSubVersion isamiSubVersion_;

	/** True compression should be applied in propagation analysis. */
	private boolean applyCompression_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.00");

	/** Number of columns. */
	private static final int NUM_COLS = 8;

	/** STH file indices. */
	private int rowIndex_ = 0, colIndex_ = 0, maxPeaks_;

	/** STH lines. */
	private String[] lines_;

	/** Delta-p ratios. */
	private DPRatio[] dpRatios_;

	/** Delta-t insterpolators. */
	private DTInterpolator[] dtInterpolators_;

	/** Reference DP. */
	private double maxdamAngle_;

	/** Equivalent stress analysis process. */
	private ESAProcess<Double[][]> equivalentStressAnalysis_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<DamageAngle>> automaticTasks_ = null;

	/**
	 * Creates damage angle analysis task.
	 *
	 * @param stfFile
	 *            The owner STF file. Can be null for automatic execution.
	 * @param input
	 *            Analysis input.
	 * @param material
	 *            Material.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public DamageAngleAnalysis(STFFile stfFile, DamageAngleInput input, FatigueMaterial material, AnalysisEngine analysisEngine) {
		stfFile_ = stfFile;
		input_ = input;
		material_ = material;
		analysisEngine_ = analysisEngine;
		stfID_ = null;
		stressTableID_ = null;
		stfName_ = null;
		spectrum_ = null;
	}

	/**
	 * Creates damage angle analysis task.
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
	 * @param material
	 *            Material.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public DamageAngleAnalysis(int stfID, int stressTableID, String stfName, Spectrum spectrum, DamageAngleInput input, FatigueMaterial material, AnalysisEngine analysisEngine) {
		stfID_ = stfID;
		stressTableID_ = stressTableID;
		stfName_ = stfName;
		spectrum_ = spectrum;
		input_ = input;
		material_ = material;
		stfFile_ = null;
		analysisEngine_ = analysisEngine;
	}

	@Override
	public void setAutomaticInput(STFFile input) {
		stfFile_ = input;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<DamageAngle> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<DamageAngle>> getAutomaticTasks() {
		return automaticTasks_;
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
	public DamageAngleAnalysis setIsamiEngineInputs(IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
		return this;
	}

	@Override
	public String getTaskTitle() {
		return "Damage angle analysis";
	}

	@Override
	public SerializableTask getSerializableTask() {
		if (stfFile_ != null)
			return new SerializableDamageAngleAnalysis(stfFile_, input_, material_, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
		return new SerializableDamageAngleAnalysis(stfID_, stressTableID_, stfName_, spectrum_, input_, material_, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected DamageAngle call() throws Exception {

		// check permission
		checkPermission(Permission.DAMAGE_ANGLE_ANALYSIS);

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// calculate incremental angles
				int[] incAngles = calculateIncrementalAngles();

				// task cancelled
				if (isCancelled() || incAngles == null) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// generate STH files
				Path[] sthFiles = generateStressSequences(connection, incAngles);

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

				// get spectrum validity
				int validity = getValidity(connection);

				// task cancelled
				if (isCancelled() || validity == -1) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// run equivalent stress analyses
				Double[][] stresses = equivalentStressAnalysis(sthFiles, flsFile, incAngles, connection, validity);

				// task cancelled
				if (isCancelled() || stresses == null) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// create damage angle
				DamageAngle angle = createDamageAngle(connection, stresses, incAngles, validity);

				// task cancelled
				if (isCancelled() || angle == null) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return damage angle
				return angle;
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

				// get damage angle
				DamageAngle angle = get();

				// add to file tree
				stfFile_.getChildren().add(0, angle);

				// post-process results
				postProcess(angle);

				// manage automatic tasks
				automaticTaskOwnerSucceeded(angle, automaticTasks_, taskPanel_);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}

		// generate stress sequence for maximum damaging angle
		if (input_.isGenerateMaxdamData()) {
			generateMaxdamData();
		}
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// destroy sub processes (if still running)
		if (equivalentStressAnalysis_ != null) {
			equivalentStressAnalysis_.cancel();
		}

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// destroy sub processes (if still running)
		if (equivalentStressAnalysis_ != null) {
			equivalentStressAnalysis_.cancel();
		}

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	/**
	 * Post-processes results of this task.
	 *
	 * @param angle
	 *            Damage angle.
	 */
	private void postProcess(DamageAngle angle) {

		// plot and save damage angles
		try {
			taskPanel_.getOwner().runTaskSilently(new SaveDamageAnglePlot(angle), false).get();
		}

		// exception occurred (ignore since it is handled within the task)
		catch (Exception e) {
			// ignore
		}
	}

	/**
	 * Generates maxdam data.
	 */
	private void generateMaxdamData() {

		// create input
		FastEquivalentStressInput input = new FastEquivalentStressInput();

		// set overall factors
		for (int i = 0; i < input_.getNumberOfStressModifiers(); i++) {
			input.setStressModifier(i, input_.getStressModificationValue(i), input_.getStressModificationMethod(i));
		}

		// set loadcase factors
		input.setLoadcaseFactors(input_.getLoadcaseFactors());

		// set segment factors
		input.setSegmentFactors(input_.getSegmentFactors());

		// set delta-p values
		input.setDPLoadcase(input_.getDPLoadcase());
		input.setReferenceDP(input_.getReferenceDP());

		// set delta-t values
		input.setDTInterpolation(input_.getDTInterpolation());
		input.setDTLoadcaseSup(input_.getDTLoadcaseSup());
		input.setReferenceDTSup(input_.getReferenceDTSup());
		input.setDTLoadcaseInf(input_.getDTLoadcaseInf());
		input.setReferenceDTInf(input_.getReferenceDTInf());

		// set generation options
		input.setStressComponent(StressComponent.ROTATED);
		input.setRotationAngle(Math.toDegrees(maxdamAngle_));

		// set omission inputs
		boolean removeNegative = input_.isRemoveNegativeStresses();
		boolean applyOmission = input_.isApplyOmission();
		double omissionLevel = input_.getOmissionlevel();
		input.setRemoveNegativeStresses(removeNegative);
		input.setApplyOmission(applyOmission);
		input.setOmissionLevel(omissionLevel);

		// create material array
		ArrayList<Material> materials = new ArrayList<>();
		materials.add(material_);

		// run task
		if (stfFile_ != null) {
			taskPanel_.getOwner().runTaskInParallel(new FastGenerateStressSequence(stfFile_, input, materials, false, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_));
		}
		else {
			taskPanel_.getOwner().runTaskInParallel(new FastGenerateStressSequence(stfID_, stressTableID_, stfName_, spectrum_, input, materials, false, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_));
		}
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
		int validity = -1;
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
	 * Runs equivalent stress analyses.
	 *
	 * @param sthFiles
	 *            Paths to input STH files.
	 * @param flsFile
	 *            Path to input FLS file.
	 * @param incAngles
	 *            Increment angles.
	 * @param connection
	 *            Database connection.
	 * @param validity
	 *            Spectrum validity.
	 * @return Equivalent stresses.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Double[][] equivalentStressAnalysis(Path[] sthFiles, Path flsFile, int[] incAngles, Connection connection, int validity) throws Exception {

		// get selected analysis engine
		boolean isFallback = (boolean) taskPanel_.getOwner().getOwner().getSettings().getValue(Settings.FALLBACK_TO_INBUILT);

		// ISAMI engine
		if (analysisEngine_.equals(AnalysisEngine.ISAMI)) {
			equivalentStressAnalysis_ = new InbuiltDAA(this, sthFiles, flsFile, incAngles, material_, validity, maxPeaks_ > EquivalentStressAnalysis.MAX_PEAKS, input_.isApplyOmission(), input_.getOmissionlevel());
		}

		// SAFE engine
		else if (analysisEngine_.equals(AnalysisEngine.SAFE)) {

			// maximum allowed number of peaks exceeded
			if (maxPeaks_ > EquivalentStressAnalysis.MAX_PEAKS) {
				addWarning("Maximum number of allowed peaks per typical flight (100000) is exceeded. Executing extended inbuilt analysis engine.");
				equivalentStressAnalysis_ = new InbuiltDAA(this, sthFiles, flsFile, incAngles, material_, validity, true, input_.isApplyOmission(), input_.getOmissionlevel());
			}

			// number of peaks within limits
			else {

				// connected to server (SAFE engine)
				if (taskPanel_.getOwner().getOwner().getAnalysisServerManager().isConnected()) {
					equivalentStressAnalysis_ = new SafeDAA(this, sthFiles, flsFile, incAngles, material_, input_.isApplyOmission(), input_.getOmissionlevel());
				}

				// not connected (inbuilt engine)
				else if (isFallback) {
					addWarning("Cannot connect to analysis server. Falling back to inbuilt Equinox analysis engine.");
					equivalentStressAnalysis_ = new InbuiltDAA(this, sthFiles, flsFile, incAngles, material_, validity, false, input_.isApplyOmission(), input_.getOmissionlevel());
				}

				// fallback not selected
				else
					throw new Exception("Cannot connect to analysis server.");
			}
		}

		// inbuilt engine
		else {
			equivalentStressAnalysis_ = new InbuiltDAA(this, sthFiles, flsFile, incAngles, material_, validity, maxPeaks_ > EquivalentStressAnalysis.MAX_PEAKS, input_.isApplyOmission(), input_.getOmissionlevel());
		}

		// run and return results of process
		return equivalentStressAnalysis_.start(connection);
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
	 * Calculates and returns incremental angles.
	 *
	 * @return Incremental angles.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int[] calculateIncrementalAngles() throws Exception {
		updateMessage("Calculating incremental angles...");
		int start = input_.getStartAngle();
		int end = input_.getEndAngle();
		int inc = input_.getIncrementAngle();
		int[] angles = new int[(end - start) / inc + 1];
		int k = start;
		for (int i = 0; i < angles.length; i++) {
			angles[i] = k;
			k += inc;
		}
		return angles;
	}

	/**
	 * Generates spectra for each increment angle and writes out STH files.
	 *
	 * @param connection
	 *            Database connection.
	 * @param incAngles
	 *            Incremental angles.
	 * @return Array containing the paths to STH files.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path[] generateStressSequences(Connection connection, int[] incAngles) throws Exception {

		// update info
		updateMessage("Generating stress sequences for incremental angles...");

		// initialize mapping
		Path[] paths = new Path[incAngles.length];
		BufferedWriter[] writers = new BufferedWriter[incAngles.length];

		try {

			// create paths file writers
			updateMessage("Creating STH file writers...");
			Path rootDir = getWorkingDirectory();
			String baseFileName = FileType.getNameWithoutExtension(stfFile_ == null ? stfName_ : stfFile_.getName());
			double[] radians = new double[incAngles.length];
			lines_ = new String[incAngles.length];
			for (int i = 0; i < incAngles.length; i++) {
				paths[i] = rootDir.resolve(FileType.appendExtension(Utility.correctFileName(baseFileName) + "_" + incAngles[i], FileType.STH));
				writers[i] = Files.newBufferedWriter(paths[i], Charset.defaultCharset());
				writeSTHHeader(writers[i], incAngles[i]);
				radians[i] = Math.toRadians(incAngles[i]);
			}

			// get spectrum file IDs
			updateMessage("Getting spectrum file IDs from database...");
			Spectrum cdfSet = stfFile_ == null ? spectrum_ : stfFile_.getParentItem();
			int anaFileID = cdfSet.getANAFileID();
			int txtFileID = cdfSet.getTXTFileID();
			int convTableID = cdfSet.getConversionTableID();

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get DP ratio
				updateMessage("Computing delta-p ratio...");
				dpRatios_ = getDPRatios(connection, statement, anaFileID, txtFileID, convTableID, radians);

				// get DT parameters
				updateMessage("Computing delta-t interpolation...");
				dtInterpolators_ = getDTInterpolators(connection, statement, txtFileID, radians);

				// get number of flights and peaks of the ANA file
				int numPeaks = getNumberOfPeaks(statement, anaFileID);

				// get maximum number of peaks per typical flight
				maxPeaks_ = getMaxPeaksPerFlight(statement, anaFileID);

				// prepare statement for selecting ANA peaks
				int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
				int stressTableID = stfFile_ == null ? stressTableID_ : stfFile_.getStressTableID();
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
									HashMap<String, OneGStresses> oneg = new HashMap<>();
									HashMap<String, double[]> inc = new HashMap<>();
									int peakCount = 0;
									while (anaFlights.next()) {

										// task cancelled
										if (isCancelled())
											return null;

										// write flight header
										int flightPeaks = anaFlights.getInt("num_peaks");
										writeFlightHeaders(writers, anaFlights, flightPeaks);

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
												writeSTHPeak(radians, writers, anaPeaks, select1GIssyCode, selectSTFStress, selectIncrementIssyCode, oneg, inc, dpRatios_, dtInterpolators_, rem, numRows);
											}
										}
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
	 * Writes out flight headers to output STH files.
	 *
	 * @param writers
	 *            File writers.
	 * @param anaFlights
	 *            ANA flights.
	 * @param flightPeaks
	 *            Number of peaks of the flight.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeFlightHeaders(BufferedWriter[] writers, ResultSet anaFlights, int flightPeaks) throws Exception {

		// update info
		String name = anaFlights.getString("name");
		updateMessage("Generating flight '" + name + "'...");

		// create first line of flight info
		String line1 = String.format("%10s", format_.format(anaFlights.getDouble("validity")));
		line1 += String.format("%10s", format_.format(anaFlights.getDouble("block_size")));

		// create second line of flight info
		String line2 = String.format("%10s", Integer.toString(flightPeaks));
		for (int i = 0; i < 62; i++) {
			line2 += " ";
		}
		line2 += (name.startsWith("TF_") ? name.substring(3) : name) + " " + anaFlights.getString("severity");

		// write headers
		for (BufferedWriter writer : writers) {
			writer.write(line1);
			writer.newLine();
			writer.write(line2);
			writer.newLine();
		}
	}

	/**
	 * Writes STH peaks to output files.
	 *
	 * @param radians
	 *            Incremental angles.
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
	 * @param dpRatios
	 *            Delta-p ratios.
	 * @param dtInterpolators
	 *            Delta-t interpolators.
	 * @param numRows
	 *            Number of rows to write.
	 * @param rem
	 *            Remaining number of columns in the STH output file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeSTHPeak(double[] radians, BufferedWriter[] writers, ResultSet anaPeaks, PreparedStatement select1GIssyCode, PreparedStatement selectSTFStress, PreparedStatement selectIncrementIssyCode, HashMap<String, OneGStresses> oneg, HashMap<String, double[]> inc, DPRatio[] dpRatios,
			DTInterpolator[] dtInterpolators, int rem, int numRows) throws Exception {

		// get class code
		String classCode = anaPeaks.getString("fourteen_digit_code");
		String onegCode = classCode.substring(0, 4);

		// get 1g stresses
		OneGStresses onegStress = oneg.get(onegCode);
		if (onegStress == null) {
			onegStress = get1GStress(radians, selectSTFStress, select1GIssyCode, onegCode);
			oneg.put(onegCode, onegStress);
		}

		// get segment
		Segment segment = onegStress.getSegment();

		// get increment stresses
		double[] incStress = inc.get(classCode);
		if (incStress == null) {
			incStress = getIncStress(radians, selectSTFStress, selectIncrementIssyCode, classCode, onegCode, segment);
			inc.put(classCode, incStress);
		}

		// compute and modify delta-p stress
		double[] dpStress = new double[radians.length];
		if (dpRatios[0] != null) {
			double pressure = anaPeaks.getDouble("delta_p");
			for (int i = 0; i < dpStress.length; i++) {
				dpStress[i] = dpRatios[i].getStress(pressure);
				dpStress[i] = modifyStress(dpRatios[i].getIssyCode(), segment, GenerateStressSequenceInput.DELTAP, dpStress[i]);
			}
		}

		// compute and modify delta-t stress
		double[] dtStress = new double[radians.length];
		if (dtInterpolators != null) {

			// get temperature
			double temperature = anaPeaks.getDouble("delta_t");

			// loop over angles
			for (int i = 0; i < dtStress.length; i++) {

				// get delta-t stress
				dtStress[i] = dtInterpolators[i] == null ? 0.0 : dtInterpolators[i].getStress(temperature);

				// modify stress for 1 point interpolation
				if (dtInterpolators[i] != null && dtInterpolators[i] instanceof DT1PointInterpolator) {
					DT1PointInterpolator onePoint = (DT1PointInterpolator) dtInterpolators[i];
					dtStress[i] = modifyStress(onePoint.getIssyCode(), segment, GenerateStressSequenceInput.DELTAT, dtStress[i]);
				}

				// modify stress for 2 points interpolation
				else if (dtInterpolators[i] != null && dtInterpolators[i] instanceof DT2PointsInterpolator) {
					DT2PointsInterpolator twoPoints = (DT2PointsInterpolator) dtInterpolators[i];
					dtStress[i] = modify2PointDTStress(twoPoints, segment, dtStress[i]);
				}
			}
		}

		// calculate total stresses
		double[] totalStresses = new double[radians.length];
		for (int i = 0; i < radians.length; i++) {
			totalStresses[i] = onegStress.getStresses()[i] + incStress[i] + dpStress[i] + dtStress[i];
			if (totalStresses[i] < 0 && input_.isRemoveNegativeStresses()) {
				totalStresses[i] = 0.0;
			}
		}

		// last row
		if (rowIndex_ == numRows - 1) {

			// add peaks
			for (int i = 0; i < lines_.length; i++) {
				lines_[i] += String.format("%10s", format_.format(totalStresses[i]));
			}
			colIndex_++;

			// last column
			if (colIndex_ == (rem == 0 ? NUM_COLS : rem)) {
				for (int i = 0; i < lines_.length; i++) {
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
				lines_[i] += String.format("%10s", format_.format(totalStresses[i]));
			}
			colIndex_++;

			// last column
			if (colIndex_ == NUM_COLS) {
				for (int i = 0; i < lines_.length; i++) {
					writers[i].write(lines_[i]);
					writers[i].newLine();
					lines_[i] = "";
				}
				colIndex_ = 0;
				rowIndex_++;
			}
		}
	}

	/**
	 * Sets increment stress to given ANA peak.
	 *
	 * @param radians
	 *            Increment angles.
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
	 * @return Returns the increment stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double[] getIncStress(double[] radians, PreparedStatement selectSTFStress, PreparedStatement selectIncrementIssyCode, String classCode, String onegCode, Segment segment) throws Exception {

		// add default increment stress
		double[] totalIncrementStress = new double[radians.length];

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

					// compute increment stresses
					double[] stress = getSTFStress(segment, radians, selectSTFStress, issyCode, GenerateStressSequenceInput.INCREMENT);

					// add to total increment stress
					for (int j = 0; j < stress.length; j++) {
						totalIncrementStress[j] += stress[j] * factor;
					}
				}
			}
		}

		// set increment stresses
		return totalIncrementStress;
	}

	/**
	 * Sets 1g stress to given ANA peak.
	 *
	 * @param radians
	 *            Increment angles.
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
	private OneGStresses get1GStress(double[] radians, PreparedStatement selectSTFStress, PreparedStatement select1gIssyCode, String onegCode) throws Exception {

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

		// compute and return 1g stresses
		return new OneGStresses(segment, getSTFStress(segment, radians, selectSTFStress, issyCode, GenerateStressSequenceInput.ONEG));
	}

	/**
	 * Returns STF stress for given issy code.
	 *
	 * @param segment
	 *            Segment.
	 * @param radians
	 *            Incremental angles.
	 * @param selectSTFStress
	 *            Database statement.
	 * @param issyCode
	 *            ISSY code.
	 * @param stressType
	 *            Stress type (1g, increment, delta-p or total stress).
	 * @return STF stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double[] getSTFStress(Segment segment, double[] radians, PreparedStatement selectSTFStress, String issyCode, int stressType) throws Exception {

		// initialize stress array
		double[] stresses = new double[radians.length];

		// query stresses
		selectSTFStress.setString(1, issyCode);
		try (ResultSet resultSet = selectSTFStress.executeQuery()) {
			while (resultSet.next()) {

				// get stresses
				double x = resultSet.getDouble("stress_x");
				double y = resultSet.getDouble("stress_y");
				double xy = resultSet.getDouble("stress_xy");

				// rotate stresses
				for (int i = 0; i < stresses.length; i++) {
					stresses[i] = 0.5 * (x + y) + 0.5 * (x - y) * Math.cos(2 * radians[i]) + xy * Math.sin(2 * radians[i]);
					stresses[i] = modifyStress(issyCode, segment, stressType, stresses[i]);
				}
			}
		}
		return stresses;
	}

	/**
	 * Modifies and returns stress according to event name and stress type.
	 *
	 * @param issyCode
	 *            ISSY code.
	 * @param segment
	 *            Segment.
	 * @param stressType
	 *            Stress type (1g, increment, delta-p or total stress).
	 * @param stress
	 *            Stress value extracted from STF file.
	 * @return The modified stress value.
	 */
	private double modifyStress(String issyCode, Segment segment, int stressType, double stress) {

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
	 * Returns delta-t interpolations, or null if no delta-t interpolation is supplied.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param txtFileID
	 *            TXT file ID.
	 * @param radians
	 *            Increment angles.
	 * @return Delta-t interpolation, or null if no delta-t interpolation is supplied.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DTInterpolator[] getDTInterpolators(Connection connection, Statement statement, int txtFileID, double[] radians) throws Exception {

		// no delta-t interpolation
		DTInterpolation interpolation = input_.getDTInterpolation();
		if (interpolation.equals(DTInterpolation.NONE))
			return null;

		// get reference temperatures
		double[] refTemp = new double[2];
		refTemp[0] = input_.getReferenceDTSup() == null ? 0.0 : input_.getReferenceDTSup().doubleValue();
		refTemp[1] = input_.getReferenceDTInf() == null ? 0.0 : input_.getReferenceDTInf().doubleValue();

		// initialize delta-t interpolators array
		DTInterpolator[] dtInterpolators = new DTInterpolator[radians.length];

		// create statement to get delta-t event name and issy code
		boolean supLCFound = false, infLCFound = false;
		String sql = null;
		if (interpolation.equals(DTInterpolation.ONE_POINT)) {
			sql = "select flight_phase, issy_code from txt_codes where file_id = " + txtFileID + " and issy_code = '" + input_.getDTLoadcaseSup() + "'";
		}
		else if (interpolation.equals(DTInterpolation.TWO_POINTS)) {
			sql = "select flight_phase, issy_code from txt_codes where file_id = " + txtFileID + " and (issy_code = '" + input_.getDTLoadcaseSup() + "' or issy_code = '" + input_.getDTLoadcaseInf() + "')";
		}

		// execute statement
		int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
		int stressTableID = stfFile_ == null ? stressTableID_ : stfFile_.getStressTableID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// prepare statement to get STF stresses
			sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stressTableID + " where file_id = " + stfID + " and issy_code = ?";
			try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

				// loop over delta-t cases
				while (resultSet.next()) {

					// set issy code
					String issyCode = resultSet.getString("issy_code");
					statement2.setString(1, issyCode);

					// get delta-p stress from STF file
					double[] stresses = new double[radians.length];

					// query STF stresses
					try (ResultSet resultSet2 = statement2.executeQuery()) {
						while (resultSet2.next()) {

							// get stresses
							double x = resultSet2.getDouble("stress_x");
							double y = resultSet2.getDouble("stress_y");
							double xy = resultSet2.getDouble("stress_xy");

							// compute delta-p ratio
							for (int i = 0; i < radians.length; i++) {
								stresses[i] = 0.5 * (x + y) + 0.5 * (x - y) * Math.cos(2 * radians[i]) + xy * Math.sin(2 * radians[i]);
							}
						}
					}

					// get event name
					String event = resultSet.getString("flight_phase");

					// loop over stresses
					for (int i = 0; i < stresses.length; i++)
						// 1 point interpolation
						if (interpolation.equals(DTInterpolation.ONE_POINT)) {
							dtInterpolators[i] = new DT1PointInterpolator(event, issyCode, stresses[i], refTemp[0]);
							supLCFound = true;
						}

						// 2 points interpolation
						else if (interpolation.equals(DTInterpolation.TWO_POINTS)) {

							// create interpolator
							if (dtInterpolators[i] == null) {
								dtInterpolators[i] = new DT2PointsInterpolator();
							}

							// superior load case
							if (issyCode.equals(input_.getDTLoadcaseSup())) {
								((DT2PointsInterpolator) dtInterpolators[i]).setSupParameters(event, issyCode, stresses[i], refTemp[0]);
								supLCFound = true;
							}

							// inferior load case
							else if (issyCode.equals(input_.getDTLoadcaseInf())) {
								((DT2PointsInterpolator) dtInterpolators[i]).setInfParameters(event, issyCode, stresses[i], refTemp[1]);
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
		return dtInterpolators;
	}

	/**
	 * Returns delta-p ratios.
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
	 * @param radians
	 *            Increment angles.
	 * @return Delta-p ratios.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DPRatio[] getDPRatios(Connection connection, Statement statement, int anaFileID, int txtFileID, int convTableID, double[] radians) throws Exception {

		// get reference pressure
		double refDP = getRefDP(connection, convTableID, anaFileID);

		// initialize delta-p ratios array
		DPRatio[] dpRatios = new DPRatio[radians.length];

		// create statement to get delta-p event name and issy code
		String sql = null;
		if (input_.getDPLoadcase() == null) {
			sql = "select flight_phase, issy_code from txt_codes where file_id = " + txtFileID + " and dp_case = 1";
		}
		else {
			sql = "select flight_phase from txt_codes where file_id = " + txtFileID + " and issy_code = '" + input_.getDPLoadcase() + "'";
		}

		// execute statement
		int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
		int stressTableID = stfFile_ == null ? stressTableID_ : stfFile_.getStressTableID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// prepare statement to get STF stresses
			sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stressTableID + " where file_id = " + stfID + " and issy_code = ?";
			try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

				// loop over delta-p cases
				while (resultSet.next()) {

					// get ISSY code
					String issyCode = input_.getDPLoadcase() == null ? resultSet.getString("issy_code") : input_.getDPLoadcase();

					// get delta-p stress from STF file
					double[] stresses = new double[radians.length];

					// non-zero reference pressure
					if (refDP != 0.0) {

						// query STF stresses
						statement2.setString(1, issyCode);
						try (ResultSet resultSet2 = statement2.executeQuery()) {
							while (resultSet2.next()) {

								// get stresses
								double x = resultSet2.getDouble("stress_x");
								double y = resultSet2.getDouble("stress_y");
								double xy = resultSet2.getDouble("stress_xy");

								// compute delta-p ratio
								for (int i = 0; i < radians.length; i++) {
									stresses[i] = 0.5 * (x + y) + 0.5 * (x - y) * Math.cos(2 * radians[i]) + xy * Math.sin(2 * radians[i]);
								}
							}
						}
					}

					// create delta-p ratios
					String event = resultSet.getString("flight_phase");
					for (int i = 0; i < stresses.length; i++) {
						dpRatios[i] = new DPRatio(refDP, stresses[i], event, issyCode);
					}
					break;
				}
			}
		}

		// delta-p load case could not be found
		if (input_.getDPLoadcase() != null && dpRatios[0] == null) {
			warnings_ += "Delta-P load case '" + input_.getDPLoadcase() + "' could not be found.\n";
		}

		// return delta-p ratios
		return dpRatios;
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
	 * @param angle
	 *            Increment angle.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void writeSTHHeader(BufferedWriter writer, int angle) throws Exception {
		writer.write(" # STH file for incremental angle: " + angle);
		writer.newLine();
		writer.write(" #");
		writer.newLine();
		writer.write(" #");
		writer.newLine();
		writer.write(" #");
		writer.newLine();
	}

	/**
	 * Creates damage angle in the database for the given damage angles.
	 *
	 * @param connection
	 *            Database connection.
	 * @param stresses
	 *            Equivalent stresses.
	 * @param incAngles
	 *            Incremental angles.
	 * @param validity
	 *            Spectrum validity.
	 * @return Damage angle item.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DamageAngle createDamageAngle(Connection connection, Double[][] stresses, int[] incAngles, int validity) throws Exception {

		// update info
		updateMessage("Saving damage angles to database...");

		// get maximum damage angles and equivalent stresses
		Double fatAngle = 0.0, fatStress = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < incAngles.length; i++) {

			// null value
			if (stresses[i] == null) {
				continue;
			}
			if (stresses[i][STRESS] == null) {
				continue;
			}

			// fatigue
			if (fatStress <= stresses[i][STRESS]) {
				fatStress = stresses[i][STRESS];
				fatAngle = Math.toRadians(incAngles[i]);
			}
		}

		// check for valid values
		if (fatStress == Double.NEGATIVE_INFINITY)
			throw new Exception("No damage angle could be calculated. Analysis aborted.");

		// save material name to STF files table
		int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
		String sql = "update stf_files set fatigue_material = '" + material_.toString() + "' where file_id = " + stfID;
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}

		// set angle result to task panel
		maxdamAngle_ = fatAngle;

		// initialize damage angle
		DamageAngle maxdam = null;

		// create statement for inserting maximum damage angle to database
		sql = "insert into maxdam_angles(stf_id, name, angle, stress, oneg_fac, inc_fac, dp_fac, dt_fac, ref_dp, dp_lc, dt_lc_inf, dt_lc_sup, ref_dt_inf, ref_dt_sup, validity, remove_negative, omission_level, material_name, material_specification, material_library_version, material_family, material_orientation, material_configuration, material_p, material_q, material_m, material_isami_version) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// set parameters
			update.setInt(1, stfID); // STF ID
			String stfName = stfFile_ == null ? stfName_ : stfFile_.getName();
			String angleName = "Damage Angles (" + stfName + ")";
			update.setString(2, angleName);
			update.setDouble(3, fatAngle); // fatigue angle
			update.setDouble(4, fatStress); // fatigue stress
			String stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.ONEG) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.ONEG) + ")";
			update.setString(5, stressModifier); // oneg_fac
			stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.INCREMENT) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.INCREMENT) + ")";
			update.setString(6, stressModifier); // inc_fac
			stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.DELTAP) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.DELTAP) + ")";
			update.setString(7, stressModifier); // dp_fac
			stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.DELTAT) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.DELTAT) + ")";
			update.setString(8, stressModifier); // dt_fac
			update.setDouble(9, dpRatios_[0] == null ? 0.0 : dpRatios_[0].getReferencePressure()); // ref_dp
			if (dpRatios_[0] == null) {
				update.setNull(10, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(10, dpRatios_[0].getIssyCode()); // dp_lc
			}
			update.setDouble(15, validity); // validity
			update.setBoolean(16, input_.isRemoveNegativeStresses()); // remove negative stresses
			update.setDouble(17, input_.isApplyOmission() ? input_.getOmissionlevel() : -1.0); // omission level
			update.setString(18, material_.getName());
			if (material_.getSpecification() == null || material_.getSpecification().isEmpty()) {
				update.setNull(19, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(19, material_.getSpecification());
			}
			if (material_.getLibraryVersion() == null || material_.getLibraryVersion().isEmpty()) {
				update.setNull(20, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(20, material_.getLibraryVersion());
			}
			if (material_.getFamily() == null || material_.getFamily().isEmpty()) {
				update.setNull(21, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(21, material_.getFamily());
			}
			if (material_.getOrientation() == null || material_.getOrientation().isEmpty()) {
				update.setNull(22, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(22, material_.getOrientation());
			}
			if (material_.getConfiguration() == null || material_.getConfiguration().isEmpty()) {
				update.setNull(23, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(23, material_.getConfiguration());
			}
			update.setDouble(24, material_.getP());
			update.setDouble(25, material_.getQ());
			update.setDouble(26, material_.getM());
			if (material_.getIsamiVersion() == null || material_.getIsamiVersion().isEmpty()) {
				update.setNull(27, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(27, material_.getIsamiVersion());
			}

			// no delta-t interpolation
			if (dtInterpolators_ == null || dtInterpolators_[0] == null) {
				update.setNull(11, java.sql.Types.VARCHAR); // dt_lc_inf
				update.setNull(12, java.sql.Types.VARCHAR); // dt_lc_sup
				update.setNull(13, java.sql.Types.DOUBLE); // ref_dt_inf
				update.setNull(14, java.sql.Types.DOUBLE); // ref_dt_sup
			}

			// 1 point interpolation
			else if (dtInterpolators_[0] != null && dtInterpolators_[0] instanceof DT1PointInterpolator) {
				DT1PointInterpolator onePoint = (DT1PointInterpolator) dtInterpolators_[0];
				update.setString(12, onePoint.getIssyCode()); // dt_lc_sup
				update.setDouble(14, onePoint.getReferenceTemperature()); // ref_dt_sup
				update.setNull(11, java.sql.Types.VARCHAR); // dt_lc_inf
				update.setNull(13, java.sql.Types.DOUBLE); // ref_dt_inf
			}

			// 2 points interpolation
			else if (dtInterpolators_[0] != null && dtInterpolators_[0] instanceof DT2PointsInterpolator) {
				DT2PointsInterpolator twoPoints = (DT2PointsInterpolator) dtInterpolators_[0];
				update.setString(12, twoPoints.getIssyCodeSup()); // dt_lc_sup
				update.setDouble(14, twoPoints.getReferenceTemperatureSup()); // ref_dt_sup
				update.setString(11, twoPoints.getIssyCodeInf()); // dt_lc_inf
				update.setDouble(13, twoPoints.getReferenceTemperatureInf()); // ref_dt_inf
			}

			// execute update
			update.executeUpdate();

			// create damage angle
			try (ResultSet resultSet = update.getGeneratedKeys()) {
				while (resultSet.next()) {
					maxdam = new DamageAngle(angleName, resultSet.getBigDecimal(1).intValue(), material_.toString());
				}
			}
		}

		// null angle
		if (maxdam == null)
			throw new Exception("Exception occurred during creating maximum damage angle.");

		// create statement for inserting damage angles to database
		sql = "insert into damage_angles(angle_id, angle, stress) values(?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql)) {

			// loop over angles
			for (int i = 0; i < incAngles.length; i++) {

				// null value
				if (stresses[i] == null) {
					continue;
				}
				if (stresses[i][STRESS] == null) {
					continue;
				}

				// insert
				update.setInt(1, maxdam.getID()); // angle ID
				update.setDouble(2, Math.toRadians(incAngles[i])); // angle
				update.setDouble(3, stresses[i][STRESS]); // fatigue stress
				update.executeUpdate();
			}
		}

		// add loadcase factor information
		if (input_.getLoadcaseFactors() != null) {
			sql = "insert into dam_angle_event_modifiers(angle_id, loadcase_number, event_name, comment, value, method) values(?, ?, ?, ?, ?, ?)";
			try (PreparedStatement update = connection.prepareStatement(sql)) {
				for (LoadcaseFactor eFactor : input_.getLoadcaseFactors()) {
					update.setInt(1, maxdam.getID()); // angle ID
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
			sql = "insert into dam_angle_segment_modifiers(angle_id, segment_name, segment_number, oneg_value, inc_value, dp_value, oneg_method, inc_method, dp_method, dt_value, dt_method) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement update = connection.prepareStatement(sql)) {
				for (SegmentFactor sFactor : input_.getSegmentFactors()) {
					update.setInt(1, maxdam.getID()); // angle ID
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

		// return angle
		return maxdam;
	}
}
