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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import equinox.Equinox;
import equinox.data.DPRatio;
import equinox.data.DT1PointInterpolator;
import equinox.data.DT2PointsInterpolator;
import equinox.data.DTInterpolation;
import equinox.data.DTInterpolator;
import equinox.data.EmbeddedTask;
import equinox.data.IncrementStress;
import equinox.data.LoadcaseFactor;
import equinox.data.Segment;
import equinox.data.SegmentFactor;
import equinox.data.SteadyStress;
import equinox.data.Stress;
import equinox.data.StressComponent;
import equinox.data.fileType.Flight;
import equinox.data.fileType.Flights;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.fileType.StressSequence;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.task.serializableTask.SerializableGenerateStressSequence;
import equinox.utility.Utility;

/**
 * Class for generate stress sequence task.
 *
 * @author Murat Artim
 * @date Mar 24, 2014
 * @time 9:13:59 PM
 */
public class GenerateStressSequence extends InternalEquinoxTask<StressSequence> implements LongRunningTask, SavableTask, SingleInputTask<STFFile>, AutomaticTaskOwner<SpectrumItem> {

	/** The owner STF file. */
	private STFFile stfFile_ = null;

	/** Input. */
	private final GenerateStressSequenceInput input_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<SpectrumItem>> automaticTasks_ = null;

	/**
	 * Creates generate stress sequence task.
	 *
	 * @param stfFile
	 *            The owner STF file. Can be null for automatic execution.
	 * @param input
	 *            Generation input.
	 */
	public GenerateStressSequence(STFFile stfFile, GenerateStressSequenceInput input) {
		stfFile_ = stfFile;
		input_ = input;
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
	public void setAutomaticInput(STFFile stfFile) {
		stfFile_ = stfFile;
	}

	@Override
	public String getTaskTitle() {
		return "Generate stress sequence '" + input_.getFileName(stfFile_) + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableGenerateStressSequence(stfFile_, input_);
	}

	@Override
	protected StressSequence call() throws Exception {

		// check permission
		checkPermission(Permission.GENERATE_STRESS_SEQUENCE);

		// initialize stress sequence
		StressSequence stressSequence = null;

		// update progress info
		updateTitle("Generating stress sequence '" + input_.getFileName(stfFile_) + "'");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// generate STH file
				stressSequence = start(connection);

				// task cancelled
				if (isCancelled() || stressSequence == null || Thread.currentThread().isInterrupted()) {
					Thread.interrupted();
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				if (connection != null) {
					Thread.interrupted();
					connection.rollback();
					connection.setAutoCommit(true);
				}

				// propagate exception
				throw e;
			}
		}

		// return
		return stressSequence;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// add stress sequence to file tree
		try {

			// get stress sequence
			StressSequence sequence = get();

			// add to STF file
			stfFile_.getChildren().add(sequence);

			// post-process results
			postProcess(sequence);

			// manage automatic tasks
			automaticTaskOwnerSucceeded(sequence, automaticTasks_, taskPanel_);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Post-processes results of this task.
	 *
	 * @param sequence
	 *            Stress sequence.
	 */
	private void postProcess(StressSequence sequence) {

		// generate and save plots
		List<Future<?>> results = new ArrayList<>();
		results.add(taskPanel_.getOwner().runTaskSilently(new SaveMissionProfilePlot(sequence), false));
		results.add(taskPanel_.getOwner().runTaskSilently(new SaveLongestFlightPlot(sequence), false));
		results.add(taskPanel_.getOwner().runTaskSilently(new SaveHOFlightPlot(sequence), false));
		results.add(taskPanel_.getOwner().runTaskSilently(new SaveHSFlightPlot(sequence), false));
		results.add(taskPanel_.getOwner().runTaskSilently(new SaveNumPeaksPlot(sequence), false));
		results.add(taskPanel_.getOwner().runTaskSilently(new SaveFlightOccurrencePlot(sequence), false));

		// wait for results to complete
		for (Future<?> result : results) {

			try {
				result.get();
			}

			// exception occurred (ignore since it is handled within the task)
			catch (Exception e) {
				// ignore
			}
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
	 * Generates STH file in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @return The newly generated STH file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private StressSequence start(Connection connection) throws Exception {

		// update info
		updateMessage("Generating stress sequence from '" + stfFile_.getName() + "'");

		// get spectrum file IDs
		updateMessage("Getting spectrum file IDs from database...");
		Spectrum cdfSet = stfFile_.getParentItem();
		int anaFileID = cdfSet.getANAFileID();
		int txtFileID = cdfSet.getTXTFileID();
		int convTableID = cdfSet.getConversionTableID();

		// create statement
		try (Statement statement = connection.createStatement()) {

			// get DP ratio
			updateMessage("Computing delta-p ratio...");
			DPRatio dpRatio = getDPRatio(connection, statement, anaFileID, txtFileID, convTableID);

			// get DT parameters
			updateMessage("Computing delta-t interpolation...");
			DTInterpolator dtInterpolator = getDTInterpolator(connection, statement, txtFileID);

			// get number of flights and peaks of the ANA file
			int numPeaks = getNumberOfPeaks(statement, anaFileID);

			// insert file into STH files table
			updateMessage("Saving stress sequence info to database...");
			int sthFileID = insertSTHFile(connection, statement, anaFileID, dpRatio, dtInterpolator);

			// create STH peaks table
			updateMessage("Creating stress peaks table...");
			String sthPeaksTableName = createPeaksTable(statement, sthFileID);

			// create STH file
			StressSequence sthFile = new StressSequence(input_.getFileName(stfFile_), sthFileID);
			Flights flights = new Flights(sthFileID);

			// create segment tables
			updateMessage("Creating flight segment tables...");
			createFlightSegmentTables(statement, sthFileID);

			// create mappings to store incremental and steady stresses for each segment
			HashMap<Segment, SteadyStress> steadyStresses = new HashMap<>();
			HashMap<Segment, IncrementStress> incStresses = new HashMap<>();

			// insert STH flights
			ArrayList<Flight> sthFlights = insertSTHFlights(anaFileID, sthFileID, statement, connection);

			// prepare statement for selecting ANA peaks
			String sql = "select peak_num, fourteen_digit_code, delta_p, delta_t from ana_peaks_" + anaFileID + " where flight_id = ?";
			try (PreparedStatement selectANAPeak = connection.prepareStatement(sql)) {

				// prepare statement for inserting STH peaks
				sql = "insert into " + sthPeaksTableName + "(flight_id, peak_num, peak_val, oneg_stress, inc_stress, dp_stress, dt_stress, oneg_event, inc_event, segment, segment_num) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				try (PreparedStatement insertSTHPeak = connection.prepareStatement(sql)) {

					// prepare statement for selecting 1g issy code
					sql = "select flight_phase, issy_code, oneg_order from txt_codes where file_id = " + txtFileID + " and one_g_code = ? and increment_num = 0";
					try (PreparedStatement select1GIssyCode = connection.prepareStatement(sql)) {

						// prepare statement for selecting increment issy code
						sql = "select flight_phase, issy_code, factor_1, factor_2, factor_3, factor_4, factor_5, factor_6, factor_7, factor_8 ";
						sql += " from txt_codes where file_id = " + txtFileID;
						sql += " and one_g_code = ? and increment_num = ? and direction_num = ? and (nl_factor_num is null or nl_factor_num = ?)";
						try (PreparedStatement selectIncrementIssyCode = connection.prepareStatement(sql)) {

							// prepare statement for selecting STF stress
							sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stfFile_.getStressTableID() + " where file_id = " + stfFile_.getID() + " and issy_code = ?";
							try (PreparedStatement selectSTFStress = connection.prepareStatement(sql)) {

								// prepare statement for setting max-min values to flight
								sql = "update sth_flights set max_val = ?, min_val = ?, max_1g = ?, min_1g = ?, max_inc = ?, min_inc = ?, max_dp = ?, min_dp = ?, max_dt = ?, min_dt = ? where flight_id = ?";
								try (PreparedStatement setMaxMinToFlight = connection.prepareStatement(sql)) {

									// execute query for selecting ANA flights
									sql = "select flight_id from ana_flights where file_id = " + anaFileID + " order by flight_num";
									try (ResultSet anaFlights = statement.executeQuery(sql)) {

										// loop over flights
										HashMap<String, Stress> oneg = new HashMap<>();
										HashMap<String, Stress> inc = new HashMap<>();
										ArrayList<Integer> factorNumbers = new ArrayList<>();
										double[] maxMin = new double[10];
										int peakCount = 0, flightIndex = 0;
										while (anaFlights.next()) {

											// task cancelled
											if (isCancelled() || Thread.currentThread().isInterrupted())
												return null;

											// reset max-min values
											maxMin[0] = Double.NEGATIVE_INFINITY; // max peak
											maxMin[1] = Double.POSITIVE_INFINITY; // min peak
											maxMin[2] = Double.NEGATIVE_INFINITY; // max 1g
											maxMin[3] = Double.POSITIVE_INFINITY; // min 1g
											maxMin[4] = Double.NEGATIVE_INFINITY; // max inc
											maxMin[5] = Double.POSITIVE_INFINITY; // min inc
											maxMin[6] = Double.NEGATIVE_INFINITY; // max dp
											maxMin[7] = Double.POSITIVE_INFINITY; // min dp
											maxMin[8] = Double.NEGATIVE_INFINITY; // max dt
											maxMin[9] = Double.POSITIVE_INFINITY; // min dt

											// get STH flight
											Flight flight = sthFlights.get(flightIndex);
											updateMessage("Generating flight '" + flight.getName() + "'...");

											// execute statement for getting ANA peaks
											selectANAPeak.setInt(1, anaFlights.getInt("flight_id"));
											try (ResultSet anaPeaks = selectANAPeak.executeQuery()) {

												// loop over peaks
												while (anaPeaks.next()) {

													// task cancelled
													if (isCancelled() || Thread.currentThread().isInterrupted())
														return null;

													// update progress
													updateProgress(peakCount, numPeaks);
													peakCount++;

													// insert peak into STH peaks table
													maxMin = insertSTHPeak(anaPeaks, flight.getID(), dpRatio, dtInterpolator, insertSTHPeak, select1GIssyCode, selectSTFStress, selectIncrementIssyCode, maxMin, oneg, inc, incStresses, steadyStresses, factorNumbers);
												}
											}

											// set max-min values to flight
											setMaxMinToFlight(flight.getID(), setMaxMinToFlight, maxMin);

											// add flight to flights folder
											flights.getChildren().add(flight);

											// increment flight index
											flightIndex++;
										}

										// add flights folder to spectrum
										sthFile.getChildren().add(flights);
									}
								}
							}
						}
					}
				}
			}

			// set segment information
			setSegmentInfo(connection, sthFileID, steadyStresses, incStresses);

			// return STH file
			return sthFile;
		}
	}

	/**
	 * Inserts STH flights into database.
	 *
	 * @param anaFileID
	 *            ANA file id.
	 * @param sthFileID
	 *            STH file ID.
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @return List of STH flights.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static ArrayList<Flight> insertSTHFlights(int anaFileID, int sthFileID, Statement statement, Connection connection) throws Exception {

		// create list
		ArrayList<Flight> sthFlights = new ArrayList<>();

		// prepare statement for inserting STH flights
		String sql = "insert into sth_flights(file_id, flight_num, name, severity, num_peaks, validity, block_size) values(?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement insertSTHFlight = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// execute query for selecting ANA flights
			sql = "select name, flight_num, severity, num_peaks, validity, block_size from ana_flights where file_id = " + anaFileID + " order by flight_num";
			try (ResultSet anaFlights = statement.executeQuery(sql)) {

				// loop over ANA flights
				while (anaFlights.next()) {

					// insert flight into STH flights table
					sthFlights.add(insertSTHFlight(anaFlights, sthFileID, insertSTHFlight));
				}
			}
		}

		// return flights
		return sthFlights;
	}

	/**
	 * Sets segment information to database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param sthFileID
	 *            STH file ID.
	 * @param steadyStresses
	 *            Steady stress mapping for each segment.
	 * @param incStresses
	 *            Increment stress mapping for each segment.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void setSegmentInfo(Connection connection, int sthFileID, HashMap<Segment, SteadyStress> steadyStresses, HashMap<Segment, IncrementStress> incStresses) throws Exception {

		// update info
		updateMessage("Saving segment information...");

		// get table names
		String segmentsTable = "SEGMENTS_" + sthFileID;
		String segmentSteadyStressesTable = "SEGMENT_STEADY_STRESSES_" + sthFileID;
		String segmentIncrementStressesTable = "SEGMENT_INCREMENT_STRESSES_" + sthFileID;

		// prepare statement for inserting into segments table
		String sql = "insert into " + segmentsTable + "(segment_name, segment_num) values(?, ?)";
		try (PreparedStatement insertSegment = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// prepare statement for inserting into steady stress table
			sql = "insert into " + segmentSteadyStressesTable + "(segment_id, oneg_stress, dp_stress, dt_stress, flight_id, peak_num) values(?, ?, ?, ?, ?, ?)";
			try (PreparedStatement insertSteady = connection.prepareStatement(sql)) {

				// prepare statement for inserting into increment stress table
				sql = "insert into " + segmentIncrementStressesTable + "(segment_id, stress, factor_num, flight_id, peak_num) values(?, ?, ?, ?, ?)";
				try (PreparedStatement insertIncrement = connection.prepareStatement(sql)) {

					// loop over segments
					Iterator<Segment> segments = steadyStresses.keySet().iterator();
					while (segments.hasNext()) {

						// insert segment name and number
						Segment segment = segments.next();
						insertSegment.setString(1, segment.getName());
						insertSegment.setInt(2, segment.getSegmentNumber());
						insertSegment.executeUpdate();

						// get segment ID
						int segmentID = -1;
						try (ResultSet resultSet = insertSegment.getGeneratedKeys()) {
							if (resultSet.next()) {
								segmentID = resultSet.getBigDecimal(1).intValue();
							}
						}

						// insert steady stresses
						SteadyStress steadyStress = steadyStresses.get(segment);
						insertSteady.setInt(1, segmentID);
						insertSteady.setDouble(2, steadyStress.getOnegStress());
						insertSteady.setDouble(3, steadyStress.getDPStress());
						insertSteady.setDouble(4, steadyStress.getDTStress());
						insertSteady.setInt(5, steadyStress.getFlightID());
						insertSteady.setInt(6, steadyStress.getPeakNum());
						insertSteady.executeUpdate();

						// insert incremental stresses
						IncrementStress incStress = incStresses.get(segment);
						if (incStress != null) {
							insertIncrement.setInt(1, segmentID);
							Double[] stresses = incStress.getStresses();
							Integer[] flightIDs = incStress.getFlightIDs();
							Integer[] peakNumbers = incStress.getPeakNumbers();
							for (int i = 0; i < stresses.length; i++) {
								if (stresses[i] == null) {
									continue;
								}
								insertIncrement.setDouble(2, stresses[i]);
								insertIncrement.setInt(3, stresses[i] >= 0 ? i + 1 : i - 7);
								insertIncrement.setInt(4, flightIDs[i]);
								insertIncrement.setInt(5, peakNumbers[i]);
								insertIncrement.executeUpdate();
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Creates flight segment tables.
	 *
	 * @param statement
	 *            Database statement.
	 * @param sthFileID
	 *            STH file ID. This is used to generate unique table name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void createFlightSegmentTables(Statement statement, int sthFileID) throws Exception {

		// generate table and index names
		String segmentsTable = "SEGMENTS_" + sthFileID;
		String segmentSteadyStressesTable = "SEGMENT_STEADY_STRESSES_" + sthFileID;
		String segmentIncrementStressesTable = "SEGMENT_INCREMENT_STRESSES_" + sthFileID;
		String segmentsIndex = "SEGMENTS_ID_" + sthFileID;
		String segmentSteadyStressesIndex = "SEGMENT_STEADY_STRESSES_ID_" + sthFileID;
		String segmentIncrementStressesIndex = "SEGMENT_INCREMENT_STRESSES_ID_" + sthFileID;

		// create tables
		statement.executeUpdate("CREATE TABLE AURORA." + segmentsTable + "(SEGMENT_ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), SEGMENT_NAME VARCHAR(50) NOT NULL, SEGMENT_NUM INT NOT NULL, PRIMARY KEY(SEGMENT_ID))");
		statement.executeUpdate("CREATE TABLE AURORA." + segmentSteadyStressesTable + "(SEGMENT_ID INT NOT NULL, ONEG_STRESS DOUBLE NOT NULL, DP_STRESS DOUBLE NOT NULL, DT_STRESS DOUBLE NOT NULL, FLIGHT_ID INT NOT NULL, PEAK_NUM INT NOT NULL)");
		statement.executeUpdate("CREATE TABLE AURORA." + segmentIncrementStressesTable + "(SEGMENT_ID INT NOT NULL, STRESS DOUBLE NOT NULL, FACTOR_NUM INT NOT NULL, FLIGHT_ID INT NOT NULL, PEAK_NUM INT NOT NULL)");

		// create indices
		statement.executeUpdate("CREATE INDEX " + segmentsIndex + " ON AURORA." + segmentsTable + "(SEGMENT_ID)");
		statement.executeUpdate("CREATE INDEX " + segmentSteadyStressesIndex + " ON AURORA." + segmentSteadyStressesTable + "(SEGMENT_ID)");
		statement.executeUpdate("CREATE INDEX " + segmentIncrementStressesIndex + " ON AURORA." + segmentIncrementStressesTable + "(SEGMENT_ID)");
	}

	/**
	 * Creates STH peaks table.
	 *
	 * @param statement
	 *            Database statement.
	 * @param sthFileID
	 *            STH file ID. This is used to generate unique table name.
	 * @return Name of newly created STH peaks table.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String createPeaksTable(Statement statement, int sthFileID) throws Exception {

		// generate table and index names
		String tableName = "STH_PEAKS_" + sthFileID;
		String indexName = "STH_PEAK_" + sthFileID;

		// create table
		statement.executeUpdate("CREATE TABLE AURORA." + tableName
				+ "(FLIGHT_ID INT NOT NULL, PEAK_NUM INT NOT NULL, PEAK_VAL DOUBLE NOT NULL, ONEG_STRESS DOUBLE NOT NULL, INC_STRESS DOUBLE NOT NULL, DP_STRESS DOUBLE NOT NULL, DT_STRESS DOUBLE NOT NULL, ONEG_EVENT VARCHAR(50), INC_EVENT VARCHAR(100), SEGMENT VARCHAR(50), SEGMENT_NUM INT)");

		// create index
		statement.executeUpdate("CREATE INDEX " + indexName + " ON AURORA." + tableName + "(FLIGHT_ID)");

		// return table name
		return tableName;
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
	 * Sets max-min values to given STH flight.
	 *
	 * @param sthFlightID
	 *            STH flight ID.
	 * @param setMaxMinToFlight
	 *            Database statement for setting max/min values.
	 * @param maxMin
	 *            Array containing the max-min values.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void setMaxMinToFlight(int sthFlightID, PreparedStatement setMaxMinToFlight, double[] maxMin) throws Exception {
		for (int i = 1; i <= maxMin.length; i++) {
			setMaxMinToFlight.setDouble(i, maxMin[i - 1]);
		}
		setMaxMinToFlight.setInt(11, sthFlightID);
		setMaxMinToFlight.executeUpdate();
	}

	/**
	 * Inserts peak values into STH peaks table.
	 *
	 * @param anaPeaks
	 *            Result set containing the ANA peak info.
	 * @param sthFlightID
	 *            STH flight ID.
	 * @param dpRatio
	 *            Delta-p ratio.
	 * @param dtInterpolator
	 *            Delta-t interpolator.
	 * @param insertSTHPeak
	 *            Database statement for inserting STH peak.
	 * @param select1GIssyCode
	 *            Database statement for selecting 1G issy code.
	 * @param selectSTFStress
	 *            Database statement for selecting STF stress.
	 * @param selectIncrementIssyCode
	 *            Database statement for selecting increment issy code.
	 * @param maxMin
	 *            Array containing the max-min values.
	 * @param oneg
	 *            Array containing the 1g stresses.
	 * @param inc
	 *            Array containing the increment stresses.
	 * @param incStresses
	 *            Incremental stress mapping for each segment.
	 * @param steadyStresses
	 *            Steady stress mapping for each segment.
	 * @param factorNumbers
	 *            Array list to store increment factor numbers of a segment.
	 * @return Returns an array containing the max/min values of peak, 1g, increment, delta-p and delta-t stresses (i.e. array length is 10).
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double[] insertSTHPeak(ResultSet anaPeaks, int sthFlightID, DPRatio dpRatio, DTInterpolator dtInterpolator, PreparedStatement insertSTHPeak, PreparedStatement select1GIssyCode, PreparedStatement selectSTFStress, PreparedStatement selectIncrementIssyCode, double[] maxMin,
			HashMap<String, Stress> oneg, HashMap<String, Stress> inc, HashMap<Segment, IncrementStress> incStresses, HashMap<Segment, SteadyStress> steadyStresses, ArrayList<Integer> factorNumbers) throws Exception {

		// get class code
		String classCode = anaPeaks.getString("fourteen_digit_code");
		int peakNum = anaPeaks.getInt("peak_num");
		String onegCode = classCode.substring(0, 4);

		// get 1g stress
		Stress onegStress = oneg.get(onegCode);
		if (onegStress == null) {
			onegStress = get1GStress(selectSTFStress, select1GIssyCode, onegCode);
			oneg.put(onegCode, onegStress);
		}

		// get segment
		Segment segment = onegStress.getSegment();

		// get increment stress
		Stress incStress = inc.get(classCode);
		if (incStress == null) {
			incStress = getIncStress(selectSTFStress, selectIncrementIssyCode, classCode, onegCode, segment, incStresses, sthFlightID, peakNum, factorNumbers);
			inc.put(classCode, incStress);
		}

		// compute and modify delta-p stress
		double dpStress = dpRatio == null ? 0.0 : dpRatio.getStress(anaPeaks.getDouble("delta_p"));
		if (dpRatio != null) {
			dpStress = modifyStress(dpRatio.getIssyCode(), segment, GenerateStressSequenceInput.DELTAP, dpStress);
		}

		// compute and modify delta-t stress
		double dtStress = dtInterpolator == null ? 0.0 : dtInterpolator.getStress(anaPeaks.getDouble("delta_t"));
		if (dtInterpolator != null && dtInterpolator instanceof DT1PointInterpolator) {
			DT1PointInterpolator onePoint = (DT1PointInterpolator) dtInterpolator;
			dtStress = modifyStress(onePoint.getIssyCode(), segment, GenerateStressSequenceInput.DELTAT, dtStress);
		}
		else if (dtInterpolator != null && dtInterpolator instanceof DT2PointsInterpolator) {
			DT2PointsInterpolator twoPoints = (DT2PointsInterpolator) dtInterpolator;
			dtStress = modify2PointDTStress(twoPoints, segment, dtStress);
		}

		// compute and modify total stress
		double totalStress = onegStress.getStress() + incStress.getStress() + dpStress + dtStress;

		// execute update
		insertSTHPeak.setInt(1, sthFlightID); // flight ID
		insertSTHPeak.setInt(2, peakNum); // peak number
		insertSTHPeak.setDouble(3, totalStress); // peak value (total stress)
		insertSTHPeak.setDouble(4, onegStress.getStress()); // 1g stress
		insertSTHPeak.setDouble(5, incStress.getStress()); // increment stress
		insertSTHPeak.setDouble(6, dpStress); // delta-p stress
		insertSTHPeak.setDouble(7, dtStress); // delta-t stress
		if (onegStress.getEvent() != null) {
			insertSTHPeak.setString(8, onegStress.getEvent()); // 1g event
		}
		else {
			insertSTHPeak.setNull(8, java.sql.Types.VARCHAR);
		}
		if (incStress.getEvent() != null) {
			insertSTHPeak.setString(9, incStress.getEvent()); // increment event
		}
		else {
			insertSTHPeak.setNull(9, java.sql.Types.VARCHAR);
		}
		insertSTHPeak.setString(10, segment.getName()); // segment name
		insertSTHPeak.setInt(11, segment.getSegmentNumber()); // segment number
		insertSTHPeak.executeUpdate();

		// update max-min values
		if (totalStress >= maxMin[0]) {
			maxMin[0] = totalStress;
		}
		if (totalStress <= maxMin[1]) {
			maxMin[1] = totalStress;
		}
		if (onegStress.getStress() >= maxMin[2]) {
			maxMin[2] = onegStress.getStress();
		}
		if (onegStress.getStress() <= maxMin[3]) {
			maxMin[3] = onegStress.getStress();
		}
		if (incStress.getStress() >= maxMin[4]) {
			maxMin[4] = incStress.getStress();
		}
		if (incStress.getStress() <= maxMin[5]) {
			maxMin[5] = incStress.getStress();
		}
		if (dpStress >= maxMin[6]) {
			maxMin[6] = dpStress;
		}
		if (dpStress <= maxMin[7]) {
			maxMin[7] = dpStress;
		}
		if (dtStress >= maxMin[8]) {
			maxMin[8] = dtStress;
		}
		if (dtStress <= maxMin[9]) {
			maxMin[9] = dtStress;
		}

		// add steady stress if it doesn't exist
		if (steadyStresses.get(segment) == null) {
			steadyStresses.put(segment, new SteadyStress(onegStress.getStress(), dpStress, dtStress, sthFlightID, peakNum));
		}

		// return max-min values
		return maxMin;
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
	 * @param incStresses
	 *            Increment stress mapping.
	 * @param flightID
	 *            Typical flight ID.
	 * @param peakNum
	 *            ANA peak number.
	 * @param factorNumbers
	 *            Array list to store increment factor numbers of a segment.
	 * @return Returns the increment stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Stress getIncStress(PreparedStatement selectSTFStress, PreparedStatement selectIncrementIssyCode, String classCode, String onegCode, Segment segment, HashMap<Segment, IncrementStress> incStresses, int flightID, int peakNum, ArrayList<Integer> factorNumbers) throws Exception {

		// add default increment stress
		double totalIncrementStress = 0.0;
		String event = null;
		factorNumbers.clear();

		// loop over increments
		for (int i = 0; i < 5; i++) {

			// get increment block, direction number and factor number
			String block = classCode.substring(2 * i + 4, 2 * i + 6);

			// no increment
			if (block.equals("00")) {
				continue;
			}

			// get issy code and factor
			String dirNum = block.substring(1);
			String facNum = block.substring(0, 1);
			String issyCode = null;
			double factor = 0.0;
			selectIncrementIssyCode.setString(1, onegCode); // 1g code
			selectIncrementIssyCode.setInt(2, i + 1); // increment number
			selectIncrementIssyCode.setString(3, dirNum); // direction number
			selectIncrementIssyCode.setString(4, facNum); // factor number
			try (ResultSet resultSet = selectIncrementIssyCode.executeQuery()) {
				while (resultSet.next()) {
					issyCode = resultSet.getString("issy_code");
					factor = resultSet.getDouble("factor_" + facNum);
					String flightPhase = resultSet.getString("flight_phase");
					if (event == null) {
						event = flightPhase;
					}
					else {
						event += "," + flightPhase;
					}

					// compute and modify increment stress
					double stress = factor * getSTFStress(selectSTFStress, issyCode);
					stress = modifyStress(issyCode, segment, GenerateStressSequenceInput.INCREMENT, stress);

					// add to total increment stress
					totalIncrementStress += stress;
				}
			}

			// add factor number
			factorNumbers.add(Integer.parseInt(facNum) - 1);
		}

		// set segment incremental stresses
		IncrementStress incStress = incStresses.get(segment);
		if (incStress == null) {
			incStress = new IncrementStress();
		}
		for (Integer factorNum : factorNumbers) {
			incStress.setInfo(factorNum, totalIncrementStress, flightID, peakNum);
		}
		incStresses.put(segment, incStress);

		// set increment stress
		return new Stress(totalIncrementStress, event, null, segment);
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
		stress = modifyStress(issyCode, segment, GenerateStressSequenceInput.ONEG, stress);

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
	 * Inserts given ANA flight into STH flights table.
	 *
	 * @param anaFlights
	 *            Result set containing the ANA flights.
	 * @param sthFileID
	 *            STH file ID.
	 * @param insertSTHFlight
	 *            Prepared statement for inserting the STH flight.
	 * @return The generated STH flight ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Flight insertSTHFlight(ResultSet anaFlights, int sthFileID, PreparedStatement insertSTHFlight) throws Exception {

		// get flight name
		String name = anaFlights.getString("name");

		// execute update
		insertSTHFlight.setInt(1, sthFileID); // file ID
		insertSTHFlight.setInt(2, anaFlights.getInt("flight_num")); // flight number
		insertSTHFlight.setString(3, name); // flight name
		insertSTHFlight.setString(4, anaFlights.getString("severity")); // severity
		insertSTHFlight.setInt(5, anaFlights.getInt("num_peaks")); // number of peaks
		insertSTHFlight.setDouble(6, anaFlights.getDouble("validity")); // validity
		insertSTHFlight.setDouble(7, anaFlights.getDouble("block_size")); // block size
		insertSTHFlight.executeUpdate();

		// return flight ID
		try (ResultSet resultSet = insertSTHFlight.getGeneratedKeys()) {
			while (resultSet.next())
				return new Flight(name, resultSet.getBigDecimal(1).intValue());
		}
		return null;
	}

	/**
	 * Adds input STH file to files table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param anaFileID
	 *            ANA file ID.
	 * @param dpRatio
	 *            Delta-p ratio.
	 * @param dtInterpolator
	 *            Delta-t interpolator.
	 * @return The file ID of the added file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int insertSTHFile(Connection connection, Statement statement, int anaFileID, DPRatio dpRatio, DTInterpolator dtInterpolator) throws Exception {

		// initialize number of flights
		int numFlights = 0;

		// all flights
		String sql = "select num_flights from ana_files where file_id = " + anaFileID;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				numFlights = resultSet.getInt("num_flights");
			}
		}

		// create statement
		int sthFileID = -1;
		sql = "insert into sth_files(stf_id, name, num_flights, oneg_fac, inc_fac, dp_fac, dt_fac, ref_dp, dp_lc, dt_lc_inf, dt_lc_sup, ref_dt_inf, ref_dt_sup, stress_comp, rotation_angle) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// set parameters
			update.setInt(1, stfFile_.getID()); // STF file ID
			update.setString(2, input_.getFileName(stfFile_)); // file name
			update.setInt(3, numFlights); // number of flights
			String stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.ONEG) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.ONEG) + ")";
			update.setString(4, stressModifier); // oneg_fac
			stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.INCREMENT) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.INCREMENT) + ")";
			update.setString(5, stressModifier); // inc_fac
			stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.DELTAP) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.DELTAP) + ")";
			update.setString(6, stressModifier); // dp_fac
			stressModifier = input_.getStressModificationValue(GenerateStressSequenceInput.DELTAT) + " (" + input_.getStressModificationMethod(GenerateStressSequenceInput.DELTAT) + ")";
			update.setString(7, stressModifier); // dt_fac
			update.setDouble(8, dpRatio == null ? 0.0 : dpRatio.getReferencePressure()); // ref_dp
			if (dpRatio == null) {
				update.setNull(9, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(9, dpRatio.getIssyCode()); // dp_lc
			}
			update.setString(14, input_.getStressComponent().toString()); // stress_comp
			update.setDouble(15, input_.getRotationAngle()); // rotation_angle

			// no delta-t interpolation
			if (dtInterpolator == null) {
				update.setNull(10, java.sql.Types.VARCHAR); // dt_lc_inf
				update.setNull(11, java.sql.Types.VARCHAR); // dt_lc_sup
				update.setNull(12, java.sql.Types.DOUBLE); // ref_dt_inf
				update.setNull(13, java.sql.Types.DOUBLE); // ref_dt_sup
			}

			// 1 point interpolation
			else if (dtInterpolator instanceof DT1PointInterpolator) {
				DT1PointInterpolator onePoint = (DT1PointInterpolator) dtInterpolator;
				update.setString(11, onePoint.getIssyCode()); // dt_lc_sup
				update.setDouble(13, onePoint.getReferenceTemperature()); // ref_dt_sup
				update.setNull(10, java.sql.Types.VARCHAR); // dt_lc_inf
				update.setNull(12, java.sql.Types.DOUBLE); // ref_dt_inf
			}

			// 2 points interpolation
			else if (dtInterpolator instanceof DT2PointsInterpolator) {
				DT2PointsInterpolator twoPoints = (DT2PointsInterpolator) dtInterpolator;
				update.setString(11, twoPoints.getIssyCodeSup()); // dt_lc_sup
				update.setDouble(13, twoPoints.getReferenceTemperatureSup()); // ref_dt_sup
				update.setString(10, twoPoints.getIssyCodeInf()); // dt_lc_inf
				update.setDouble(12, twoPoints.getReferenceTemperatureInf()); // ref_dt_inf
			}

			// execute update
			update.executeUpdate();

			// return file ID
			try (ResultSet resultSet = update.getGeneratedKeys()) {
				while (resultSet.next()) {
					sthFileID = resultSet.getBigDecimal(1).intValue();
				}
			}
		}

		// add loadcase factor information
		if (input_.getLoadcaseFactors() != null) {
			sql = "insert into event_modifiers(sth_id, loadcase_number, event_name, comment, value, method) values(?, ?, ?, ?, ?, ?)";
			try (PreparedStatement update = connection.prepareStatement(sql)) {
				for (LoadcaseFactor eFactor : input_.getLoadcaseFactors()) {
					update.setInt(1, sthFileID); // STH file ID
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
			sql = "insert into segment_modifiers(sth_id, segment_name, segment_number, oneg_value, inc_value, dp_value, oneg_method, inc_method, dp_method, dt_value, dt_method) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement update = connection.prepareStatement(sql)) {
				for (SegmentFactor sFactor : input_.getSegmentFactors()) {
					update.setInt(1, sthFileID); // STH file ID
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

		// return STH file ID
		return sthFileID;
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
			sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stfFile_.getStressTableID() + " where file_id = " + stfFile_.getID() + " and issy_code = ?";
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
			sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stfFile_.getStressTableID() + " where file_id = " + stfFile_.getID() + " and issy_code = ?";
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
}
