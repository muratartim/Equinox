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

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import equinox.Equinox;
import equinox.controller.MissionProfileViewPanel;
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
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.FastEquivalentStressInput;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.plugin.FileType;
import equinox.process.PlotMissionProfileProcess;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.utility.CrosshairListenerXYPlot;
import equinox.utility.Utility;

/**
 * Class for generate mission profile plot task.
 *
 * @author Murat Artim
 * @date Jul 4, 2016
 * @time 3:49:21 PM
 */
public class GenerateMissionProfilePlot extends TemporaryFileCreatingTask<Path> implements LongRunningTask, SingleInputTask<SpectrumItem>, AutomaticTaskOwner<Path> {

	/** Equivalent stress. */
	private SpectrumItem eqStress_;

	/** True to plot after generation. */
	private final boolean plot_;

	/** Path to output file. */
	private final Path output_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates generate mission profile plot task.
	 *
	 * @param eqStress
	 *            Equivalent stress. Can be null for automatic execution.
	 * @param plot
	 *            True to plot after generation.
	 * @param output
	 *            Path to output file. Can be null if plot should not be saved to output file.
	 */
	public GenerateMissionProfilePlot(SpectrumItem eqStress, boolean plot, Path output) {
		eqStress_ = eqStress;
		plot_ = plot;
		output_ = output;
	}

	@Override
	public void setAutomaticInput(SpectrumItem input) {
		eqStress_ = input;
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
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Generate mission profile plot";
	}

	@Override
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_MISSION_PROFILE);

		// update info
		updateMessage("Generating mission profile plot...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// plot mission profile
				start(connection);

				// task cancelled
				if (isCancelled() || Thread.currentThread().isInterrupted()) {
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
		return output_;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// plot
		if (plot_) {
			taskPanel_.getOwner().runTaskInParallel(new PlotFastMissionProfile(eqStress_));
		}

		// no automatic task
		if (automaticTasks_ == null)
			return;

		try {

			// get output
			Path output = get();

			// manage automatic tasks
			automaticTaskOwnerSucceeded(output, automaticTasks_, taskPanel_);
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
	 * Starts the process.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void start(Connection connection) throws Exception {

		// get file IDs
		STFFile stfFile = (STFFile) eqStress_.getParentItem();
		Spectrum spectrum = stfFile.getParentItem();
		int anaFileID = spectrum.getANAFileID();
		int txtFileID = spectrum.getTXTFileID();
		int convTableID = spectrum.getConversionTableID();

		// create statement
		try (Statement statement = connection.createStatement()) {

			// get analysis input
			FastEquivalentStressInput input = getAnalysisInput(statement);

			// get DP ratio
			updateMessage("Computing delta-p ratio...");
			DPRatio dpRatio = getDPRatio(connection, statement, stfFile, anaFileID, txtFileID, convTableID, input);

			// get DT parameters
			updateMessage("Computing delta-t interpolation...");
			DTInterpolator dtInterpolator = getDTInterpolator(connection, statement, stfFile, txtFileID, input);

			// get number of flights and peaks of the ANA file
			int numPeaks = getNumberOfPeaks(statement, anaFileID);

			// create segment tables
			updateMessage("Creating flight segment tables...");
			createFlightSegmentTables(statement);

			// create mappings to store incremental and steady stresses for each
			// segment
			HashMap<Segment, SteadyStress> steadyStresses = new HashMap<>();
			HashMap<Segment, IncrementStress> incStresses = new HashMap<>();

			// prepare statement for selecting ANA peaks
			String sql = "select peak_num, fourteen_digit_code, delta_p, delta_t from ana_peaks_" + anaFileID + " where flight_id = ?";
			try (PreparedStatement selectANAPeak = connection.prepareStatement(sql)) {

				// prepare statement for selecting 1g issy code
				sql = "select flight_phase, issy_code, oneg_order from txt_codes where file_id = " + txtFileID + " and one_g_code = ? and increment_num = 0";
				try (PreparedStatement select1GIssyCode = connection.prepareStatement(sql)) {

					// prepare statement for selecting increment issy code
					sql = "select flight_phase, issy_code, factor_1, factor_2, factor_3, factor_4, factor_5, factor_6, factor_7, factor_8 from txt_codes where file_id = " + txtFileID + " and one_g_code = ? and increment_num = ? and direction_num = ? and (nl_factor_num is null or nl_factor_num = ?)";
					try (PreparedStatement selectIncrementIssyCode = connection.prepareStatement(sql)) {

						// prepare statement for selecting STF stress
						sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stfFile.getStressTableID() + " where file_id = " + stfFile.getID() + " and issy_code = ?";
						try (PreparedStatement selectSTFStress = connection.prepareStatement(sql)) {

							// execute query for selecting ANA flights
							sql = "select * from ana_flights where file_id = " + anaFileID + " order by flight_num";
							try (ResultSet anaFlights = statement.executeQuery(sql)) {

								// loop over flights
								HashMap<String, Stress> oneg = new HashMap<>();
								HashMap<String, Stress> inc = new HashMap<>();
								ArrayList<Integer> factorNumbers = new ArrayList<>();
								int peakCount = 0;
								while (anaFlights.next()) {

									// task cancelled
									if (isCancelled())
										return;

									// execute statement for getting ANA peaks
									selectANAPeak.setInt(1, anaFlights.getInt("flight_id"));
									try (ResultSet anaPeaks = selectANAPeak.executeQuery()) {

										// loop over peaks
										while (anaPeaks.next()) {

											// task cancelled
											if (isCancelled())
												return;

											// update progress
											updateProgress(peakCount, numPeaks);
											peakCount++;

											// process peak
											processSTHPeak(anaPeaks, select1GIssyCode, selectSTFStress, selectIncrementIssyCode, oneg, inc, dpRatio, dtInterpolator, incStresses, steadyStresses, factorNumbers, input);
										}
									}
								}
							}
						}
					}
				}
			}

			// set segment information
			setSegmentInfo(connection, steadyStresses, incStresses);

			// plot mission profile
			Path file = plotMissionProfile(connection, stfFile);

			// save mission profile plot
			savePlot(statement, connection, file, stfFile.getID());

			// remove segment tables
			removeFlightSegmentTables(statement);
		}
	}

	/**
	 * Saves the profile plot to database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @param file
	 *            Path to profile plot.
	 * @param stfID
	 *            STF file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void savePlot(Statement statement, Connection connection, Path file, int stfID) throws Exception {

		// update info
		updateMessage("Saving mission profile plot to database...");

		// check if any data exists in database
		boolean exists = false;
		String sql = "select image from pilot_point_mp where id = " + stfID;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				exists = true;
			}
		}

		// create statement
		if (exists) {
			sql = "update pilot_point_mp set image = ? where id = " + stfID;
		}
		else {
			sql = "insert into pilot_point_mp(id, image) values(?, ?)";
		}
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			byte[] imageBytes = new byte[(int) file.toFile().length()];
			try (ImageInputStream imgStream = ImageIO.createImageInputStream(file.toFile())) {
				imgStream.read(imageBytes);
				try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
					if (exists) {
						update.setBlob(1, inputStream, imageBytes.length);
						update.executeUpdate();
					}
					else {
						update.setInt(1, stfID);
						update.setBlob(2, inputStream, imageBytes.length);
						update.executeUpdate();
					}
				}
			}
		}
	}

	/**
	 * Plots mission profile on an image and returns the path to the image.
	 *
	 * @param connection
	 *            Database connection.
	 * @param stfFile
	 *            STF file.
	 * @return Path to profile plot image.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path plotMissionProfile(Connection connection, STFFile stfFile) throws Exception {

		// update info
		updateMessage("Plotting mission profile...");

		// create path to output image
		Path output = output_ == null ? getWorkingDirectory().resolve("missionProfile.png") : output_;

		// create mission profile chart
		JFreeChart chart = CrosshairListenerXYPlot.createMissionProfileChart("Mission Profile", "Segment", "Stress", null, PlotOrientation.VERTICAL, true, false, false, null);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		XYPlot plot = chart.getXYPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(new Color(245, 245, 245, 0));
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.getDomainAxis().setTickLabelsVisible(false);
		plot.getDomainAxis().setTickMarksVisible(false);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);

		// get stress type
		String stressType = null;
		if (eqStress_ instanceof FastFatigueEquivalentStress) {
			stressType = "FATIGUE_" + eqStress_.getID();
		}
		else if (eqStress_ instanceof FastPreffasEquivalentStress) {
			stressType = "PREFFAS_" + eqStress_.getID();
		}
		else if (eqStress_ instanceof FastLinearEquivalentStress) {
			stressType = "LINEAR_" + eqStress_.getID();
		}

		// generate table names
		String segmentsTable = "SEGMENTS_" + stressType;
		String segmentSteadyStressesTable = "SEGMENT_STEADY_STRESSES_" + stressType;
		String segmentIncrementStressesTable = "SEGMENT_INCREMENT_STRESSES_" + stressType;

		// plot
		PlotMissionProfileProcess process = new PlotMissionProfileProcess(this, segmentsTable, segmentSteadyStressesTable, segmentIncrementStressesTable, true, true, null);
		XYDataset[] dataset = process.start(connection);
		double maxDiff = process.getMaxPositiveIncrement() - process.getMinNegativeIncrement();

		// set dataset
		plot.setDataset(dataset[0]);
		plot.setDataset(1, dataset[1]);
		plot.setDataset(2, dataset[2]);

		// set chart title
		String title = "Mission Profile";
		title += "\n(" + FileType.getNameWithoutExtension(stfFile.getName()) + ", " + stfFile.getMission() + ")";
		chart.setTitle(title);

		// set colors
		for (int i = 0; i < dataset[0].getSeriesCount(); i++) {
			String seriesName = (String) dataset[0].getSeriesKey(i);
			if (seriesName.equals("Positive Increments")) {
				plot.getRenderer().setSeriesPaint(i, MissionProfileViewPanel.POSITIVE_INCREMENTS);
			}
			else if (seriesName.equals("Negative Increments")) {
				plot.getRenderer().setSeriesPaint(i, MissionProfileViewPanel.NEGATIVE_INCREMENTS);
			}
		}
		plot.getRenderer(1).setSeriesPaint(0, Color.black);
		plot.getRenderer(1).setSeriesPaint(1, MissionProfileViewPanel.ONEG);
		plot.getRenderer(1).setSeriesPaint(2, MissionProfileViewPanel.DELTA_P);
		plot.getRenderer(1).setSeriesPaint(3, MissionProfileViewPanel.DELTA_T);

		// set auto range minimum size
		plot.getRangeAxis().setAutoRangeMinimumSize(maxDiff * MissionProfileViewPanel.RANGE_FACTOR, true);

		// remove shadow generator
		plot.setShadowGenerator(null);

		// setup chart dimensions
		int width = 658;
		int height = 597;

		// plot
		ChartUtilities.saveChartAsPNG(output.toFile(), chart, width, height);

		// return path to output image
		return output;
	}

	/**
	 * Sets segment information to database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param steadyStresses
	 *            Steady stress mapping for each segment.
	 * @param incStresses
	 *            Increment stress mapping for each segment.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void setSegmentInfo(Connection connection, HashMap<Segment, SteadyStress> steadyStresses, HashMap<Segment, IncrementStress> incStresses) throws Exception {

		// update info
		updateMessage("Saving segment information...");

		// get stress type
		String stressType = null;
		if (eqStress_ instanceof FastFatigueEquivalentStress) {
			stressType = "FATIGUE_" + eqStress_.getID();
		}
		else if (eqStress_ instanceof FastPreffasEquivalentStress) {
			stressType = "PREFFAS_" + eqStress_.getID();
		}
		else if (eqStress_ instanceof FastLinearEquivalentStress) {
			stressType = "LINEAR_" + eqStress_.getID();
		}

		// generate table names
		String segmentsTable = "SEGMENTS_" + stressType;
		String segmentSteadyStressesTable = "SEGMENT_STEADY_STRESSES_" + stressType;
		String segmentIncrementStressesTable = "SEGMENT_INCREMENT_STRESSES_" + stressType;

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
	 * Processes STH peak.
	 *
	 * @param anaPeaks
	 *            Result set containing the ANA peak info.
	 * @param select1GIssyCode
	 *            Database statement for selecting 1G issy code.
	 * @param selectSTFStress
	 *            Database statement for selecting STF stress.
	 * @param selectIncrementIssyCode
	 *            Database statement for selecting increment issy code.
	 * @param oneg
	 *            Array containing the 1g stresses.
	 * @param inc
	 *            Array containing the increment stresses.
	 * @param dpRatio
	 *            Delta-p ratio.
	 * @param dtInterpolator
	 *            Delta-t interpolator.
	 * @param incStresses
	 *            Incremental stress mapping for each segment.
	 * @param steadyStresses
	 *            Steady stress mapping for each segment.
	 * @param factorNumbers
	 *            Array list to store increment factor numbers of a segment.
	 * @param input
	 *            Analysis input.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void processSTHPeak(ResultSet anaPeaks, PreparedStatement select1GIssyCode, PreparedStatement selectSTFStress, PreparedStatement selectIncrementIssyCode, HashMap<String, Stress> oneg, HashMap<String, Stress> inc, DPRatio dpRatio, DTInterpolator dtInterpolator,
			HashMap<Segment, IncrementStress> incStresses, HashMap<Segment, SteadyStress> steadyStresses, ArrayList<Integer> factorNumbers, FastEquivalentStressInput input) throws Exception {

		// get class code
		String classCode = anaPeaks.getString("fourteen_digit_code");
		int peakNum = anaPeaks.getInt("peak_num");
		String onegCode = classCode.substring(0, 4);

		// get 1g stress
		Stress onegStress = oneg.get(onegCode);
		if (onegStress == null) {
			onegStress = get1GStress(selectSTFStress, select1GIssyCode, onegCode, input);
			oneg.put(onegCode, onegStress);
		}

		// get segment
		Segment segment = onegStress.getSegment();

		// get increment stress
		Stress incStress = inc.get(classCode);
		if (incStress == null) {
			incStress = getIncStress(selectSTFStress, selectIncrementIssyCode, classCode, onegCode, segment, incStresses, peakNum, factorNumbers, input);
			inc.put(classCode, incStress);
		}

		// compute and modify delta-p stress
		double dpStress = dpRatio == null ? 0.0 : dpRatio.getStress(anaPeaks.getDouble("delta_p"));
		if (dpRatio != null) {
			dpStress = modifyStress(dpRatio.getIssyCode(), segment, GenerateStressSequenceInput.DELTAP, dpStress, input);
		}

		// compute and modify delta-t stress
		double dtStress = dtInterpolator == null ? 0.0 : dtInterpolator.getStress(anaPeaks.getDouble("delta_t"));
		if (dtInterpolator != null && dtInterpolator instanceof DT1PointInterpolator) {
			DT1PointInterpolator onePoint = (DT1PointInterpolator) dtInterpolator;
			dtStress = modifyStress(onePoint.getIssyCode(), segment, GenerateStressSequenceInput.DELTAT, dtStress, input);
		}
		else if (dtInterpolator != null && dtInterpolator instanceof DT2PointsInterpolator) {
			DT2PointsInterpolator twoPoints = (DT2PointsInterpolator) dtInterpolator;
			dtStress = modify2PointDTStress(twoPoints, segment, dtStress, input);
		}

		// add steady stress if it doesn't exist
		if (steadyStresses.get(segment) == null) {
			steadyStresses.put(segment, new SteadyStress(onegStress.getStress(), dpStress, dtStress, -1, peakNum));
		}
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
	 * @param input
	 *            Analysis input.
	 * @return The modified stress value.
	 */
	private static double modify2PointDTStress(DT2PointsInterpolator interpolator, Segment segment, double stress, FastEquivalentStressInput input) {

		// apply overall factors
		String method = input.getStressModificationMethod(GenerateStressSequenceInput.DELTAT);
		if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
			stress *= input.getStressModificationValue(GenerateStressSequenceInput.DELTAT);
		}
		else if (method.equals(GenerateStressSequenceInput.ADD)) {
			stress += input.getStressModificationValue(GenerateStressSequenceInput.DELTAT);
		}
		else if (method.equals(GenerateStressSequenceInput.SET)) {
			stress = input.getStressModificationValue(GenerateStressSequenceInput.DELTAT);
		}

		// apply segment factors
		if (segment != null && input.getSegmentFactors() != null) {
			for (SegmentFactor sFactor : input.getSegmentFactors())
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
		if (input.getLoadcaseFactors() != null) {
			for (LoadcaseFactor eFactor : input.getLoadcaseFactors())
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
	 * @param incStresses
	 *            Increment stress mapping.
	 * @param peakNum
	 *            ANA peak number.
	 * @param factorNumbers
	 *            Array list to store increment factor numbers of a segment.
	 * @param input
	 *            Analysis input.
	 * @return Returns the increment stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Stress getIncStress(PreparedStatement selectSTFStress, PreparedStatement selectIncrementIssyCode, String classCode, String onegCode, Segment segment, HashMap<Segment, IncrementStress> incStresses, int peakNum, ArrayList<Integer> factorNumbers, FastEquivalentStressInput input)
			throws Exception {

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
					double stress = factor * getSTFStress(selectSTFStress, issyCode, input);
					stress = modifyStress(issyCode, segment, GenerateStressSequenceInput.INCREMENT, stress, input);

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
			incStress.setInfo(factorNum, totalIncrementStress, -1, peakNum);
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
	 * @param input
	 *            Analysis input.
	 * @return The 1g stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Stress get1GStress(PreparedStatement selectSTFStress, PreparedStatement select1gIssyCode, String onegCode, FastEquivalentStressInput input) throws Exception {

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
		double stress = getSTFStress(selectSTFStress, issyCode, input);
		stress = modifyStress(issyCode, segment, GenerateStressSequenceInput.ONEG, stress, input);

		// set to peak
		return new Stress(stress, event, issyCode, segment);
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
	 * @param input
	 *            Analysis input.
	 * @return The modified stress value.
	 */
	private static double modifyStress(String issyCode, Segment segment, int stressType, double stress, FastEquivalentStressInput input) {

		// apply overall factors
		String method = input.getStressModificationMethod(stressType);
		if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
			stress *= input.getStressModificationValue(stressType);
		}
		else if (method.equals(GenerateStressSequenceInput.ADD)) {
			stress += input.getStressModificationValue(stressType);
		}
		else if (method.equals(GenerateStressSequenceInput.SET)) {
			stress = input.getStressModificationValue(stressType);
		}

		// apply segment factors
		if (segment != null && input.getSegmentFactors() != null) {
			for (SegmentFactor sFactor : input.getSegmentFactors())
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
		if (input.getLoadcaseFactors() != null) {
			for (LoadcaseFactor eFactor : input.getLoadcaseFactors())
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
	 * Returns STF stress for given issy code.
	 *
	 * @param selectSTFStress
	 *            Database statement.
	 * @param issyCode
	 *            ISSY code.
	 * @param input
	 *            Analysis input.
	 * @return STF stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static double getSTFStress(PreparedStatement selectSTFStress, String issyCode, FastEquivalentStressInput input) throws Exception {
		StressComponent component = input.getStressComponent();
		double angle = input.getRotationAngle();
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
	 * Drops flight segment tables from the database.
	 *
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeFlightSegmentTables(Statement statement) throws Exception {

		// update info
		updateMessage("Removing flight segment tables...");

		// get stress type
		String stressType = null;
		if (eqStress_ instanceof FastFatigueEquivalentStress) {
			stressType = "FATIGUE_" + eqStress_.getID();
		}
		else if (eqStress_ instanceof FastPreffasEquivalentStress) {
			stressType = "PREFFAS_" + eqStress_.getID();
		}
		else if (eqStress_ instanceof FastLinearEquivalentStress) {
			stressType = "LINEAR_" + eqStress_.getID();
		}

		// generate table and index names
		String segmentsTable = "SEGMENTS_" + stressType;
		String segmentSteadyStressesTable = "SEGMENT_STEADY_STRESSES_" + stressType;
		String segmentIncrementStressesTable = "SEGMENT_INCREMENT_STRESSES_" + stressType;

		// drop tables
		statement.executeUpdate("drop table AURORA." + segmentsTable);
		statement.executeUpdate("drop table AURORA." + segmentSteadyStressesTable);
		statement.executeUpdate("drop table AURORA." + segmentIncrementStressesTable);
	}

	/**
	 * Creates flight segment tables.
	 *
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void createFlightSegmentTables(Statement statement) throws Exception {

		// get stress type
		String stressType = null;
		if (eqStress_ instanceof FastFatigueEquivalentStress) {
			stressType = "FATIGUE_" + eqStress_.getID();
		}
		else if (eqStress_ instanceof FastPreffasEquivalentStress) {
			stressType = "PREFFAS_" + eqStress_.getID();
		}
		else if (eqStress_ instanceof FastLinearEquivalentStress) {
			stressType = "LINEAR_" + eqStress_.getID();
		}

		// generate table and index names
		String segmentsTable = "SEGMENTS_" + stressType;
		String segmentSteadyStressesTable = "SEGMENT_STEADY_STRESSES_" + stressType;
		String segmentIncrementStressesTable = "SEGMENT_INCREMENT_STRESSES_" + stressType;
		String segmentsIndex = "SEGMENTS_ID_" + stressType;
		String segmentSteadyStressesIndex = "SEGMENT_STEADY_STRESSES_ID_" + stressType;
		String segmentIncrementStressesIndex = "SEGMENT_INCREMENT_STRESSES_ID_" + stressType;

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
	 * @param stfFile
	 *            STF file.
	 * @param txtFileID
	 *            TXT file ID.
	 * @param input
	 *            Analysis input.
	 * @return Delta-t interpolation, or null if no delta-t interpolation is supplied.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DTInterpolator getDTInterpolator(Connection connection, Statement statement, STFFile stfFile, int txtFileID, FastEquivalentStressInput input) throws Exception {

		// no delta-t interpolation
		DTInterpolation interpolation = input.getDTInterpolation();
		if (interpolation.equals(DTInterpolation.NONE))
			return null;

		// get reference temperatures
		double[] refTemp = new double[2];
		refTemp[0] = input.getReferenceDTSup() == null ? 0.0 : input.getReferenceDTSup().doubleValue();
		refTemp[1] = input.getReferenceDTInf() == null ? 0.0 : input.getReferenceDTInf().doubleValue();

		// set variables
		DTInterpolator dtInterpolator = null;
		StressComponent component = input.getStressComponent();
		double angle = input.getRotationAngle();

		// get delta-p issy code from TXT file
		boolean supLCFound = false, infLCFound = false;
		String sql = null;
		if (interpolation.equals(DTInterpolation.ONE_POINT)) {
			sql = "select flight_phase, issy_code from txt_codes where file_id = " + txtFileID + " and issy_code = '" + input.getDTLoadcaseSup() + "'";
		}
		else if (interpolation.equals(DTInterpolation.TWO_POINTS)) {
			sql = "select flight_phase, issy_code from txt_codes where file_id = " + txtFileID + " and (issy_code = '" + input.getDTLoadcaseSup() + "' or issy_code = '" + input.getDTLoadcaseInf() + "')";
		}
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// prepare statement to get STF stresses
			sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stfFile.getStressTableID() + " where file_id = " + stfFile.getID() + " and issy_code = ?";
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
						if (issyCode.equals(input.getDTLoadcaseSup())) {
							((DT2PointsInterpolator) dtInterpolator).setSupParameters(resultSet.getString("flight_phase"), issyCode, stress, refTemp[0]);
							supLCFound = true;
						}

						// inferior load case
						else if (issyCode.equals(input.getDTLoadcaseInf())) {
							((DT2PointsInterpolator) dtInterpolator).setInfParameters(resultSet.getString("flight_phase"), issyCode, stress, refTemp[1]);
							infLCFound = true;
						}
					}
				}
			}
		}

		// delta-t load case could not be found
		if (interpolation.equals(DTInterpolation.ONE_POINT) && !supLCFound) {
			warnings_ += "Delta-T superior load case '" + input.getDTLoadcaseSup() + "' could not be found.\n";
		}
		else if (interpolation.equals(DTInterpolation.TWO_POINTS)) {
			if (!supLCFound) {
				warnings_ += "Delta-T superior load case '" + input.getDTLoadcaseSup() + "' could not be found.\n";
			}
			if (!infLCFound) {
				warnings_ += "Delta-T inferior load case '" + input.getDTLoadcaseInf() + "' could not be found.\n";
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
	 * @param stfFile
	 *            STF file.
	 * @param anaFileID
	 *            ANA file ID.
	 * @param txtFileID
	 *            TXT file ID.
	 * @param convTableID
	 *            Conversion table ID.
	 * @param input
	 *            Analysis input.
	 * @return Delta-p ratio.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DPRatio getDPRatio(Connection connection, Statement statement, STFFile stfFile, int anaFileID, int txtFileID, int convTableID, FastEquivalentStressInput input) throws Exception {

		// get reference pressure
		double refDP = getRefDP(connection, convTableID, anaFileID, input);

		// set variables
		DPRatio dpRatio = null;
		StressComponent component = input.getStressComponent();
		double angle = input.getRotationAngle();

		// create statement to get delta-p event name and issy code
		String sql = null;
		if (input.getDPLoadcase() == null) {
			sql = "select flight_phase, issy_code from txt_codes where file_id = " + txtFileID + " and dp_case = 1";
		}
		else {
			sql = "select flight_phase from txt_codes where file_id = " + txtFileID + " and issy_code = '" + input.getDPLoadcase() + "'";
		}

		// execute statement
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// prepare statement to get STF stresses
			sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stfFile.getStressTableID() + " where file_id = " + stfFile.getID() + " and issy_code = ?";
			try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

				// loop over delta-p cases
				while (resultSet.next()) {

					// set issy code
					String issyCode = input.getDPLoadcase() == null ? resultSet.getString("issy_code") : input.getDPLoadcase();
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
		if (input.getDPLoadcase() != null && dpRatio == null) {
			warnings_ += "Delta-P load case '" + input.getDPLoadcase() + "' could not be found.\n";
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
	 * @param input
	 *            Analysis input.
	 * @return Reference delta-p pressure.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static double getRefDP(Connection connection, int convTableID, int anaFileID, FastEquivalentStressInput input) throws Exception {

		// initialize reference delta-p
		double refPressure = input.getReferenceDP() == null ? 0.0 : input.getReferenceDP().doubleValue();

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
	 * Retrieves and returns the analysis inputs from the database.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Analysis inputs from the database.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private FastEquivalentStressInput getAnalysisInput(Statement statement) throws Exception {

		// update info
		updateMessage("Getting analysis inputs from database...");

		// initialize variables
		FastEquivalentStressInput analysisInput = null;

		// get table name
		String tableName = null;
		if (eqStress_ instanceof FastFatigueEquivalentStress) {
			tableName = "fast_fatigue_equivalent_stresses";
		}
		else if (eqStress_ instanceof FastPreffasEquivalentStress) {
			tableName = "fast_preffas_equivalent_stresses";
		}
		else if (eqStress_ instanceof FastLinearEquivalentStress) {
			tableName = "fast_linear_equivalent_stresses";
		}

		// get input
		String sql = "select analysis_input from " + tableName + " where id = " + eqStress_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				Blob blob = resultSet.getBlob("analysis_input");
				if (blob != null) {
					byte[] bytes = blob.getBytes(1L, (int) blob.length());
					blob.free();
					try (ByteArrayInputStream bos = new ByteArrayInputStream(bytes)) {
						try (ObjectInputStream ois = new ObjectInputStream(bos)) {
							analysisInput = (FastEquivalentStressInput) ois.readObject();
						}
					}
				}
			}
		}

		// no input found
		if (analysisInput == null)
			throw new Exception("Cannot generate mission profile plot. No analysis input found for equivalent stress.");

		// return analysis input
		return analysisInput;
	}
}
