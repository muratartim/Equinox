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
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import equinox.Equinox;
import equinox.data.DPRatio;
import equinox.data.DT1PointInterpolator;
import equinox.data.DT2PointsInterpolator;
import equinox.data.DTInterpolation;
import equinox.data.DTInterpolator;
import equinox.data.EmbeddedTask;
import equinox.data.LoadcaseFactor;
import equinox.data.OnegStress;
import equinox.data.Segment;
import equinox.data.SegmentFactor;
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
import equinox.process.PlotFlightProcess;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.utility.CrosshairListenerXYPlot;
import equinox.utility.Utility;

/**
 * Class for generate longest typical flight plot task.
 *
 * @author Murat Artim
 * @date Jul 8, 2016
 * @time 3:29:54 PM
 */
public class GenerateLongestFlightPlot extends TemporaryFileCreatingTask<Path> implements LongRunningTask, SingleInputTask<SpectrumItem>, AutomaticTaskOwner<Path> {

	/** Equivalent stress. */
	private SpectrumItem eqStress_;

	/** True to plot after generation. */
	private final boolean plot_;

	/** Path to output file. */
	private final Path output_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates generate longest typical flight plot task.
	 *
	 * @param eqStress
	 *            Equivalent stress. Can be null for automatic execution.
	 * @param plot
	 *            True to plot after generation.
	 * @param output
	 *            Path to output file. Can be null if plot should not be saved to output file.
	 */
	public GenerateLongestFlightPlot(SpectrumItem eqStress, boolean plot, Path output) {
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
		return "Generate longest typical flight plot";
	}

	@Override
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_TYPICAL_FLIGHT);

		// update info
		updateMessage("Generating longest typical flight plot...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// create statement
				try (Statement statement = connection.createStatement()) {

					// get STF file
					STFFile stfFile = (STFFile) eqStress_.getParentItem();

					// get analysis input
					FastEquivalentStressInput input = getAnalysisInput(statement);

					// task cancelled
					if (isCancelled()) {
						connection.rollback();
						connection.setAutoCommit(true);
						return null;
					}

					// get longest flight info
					LongestFlightInfo info = getLongestFlightInfo(statement, stfFile);

					// task cancelled
					if (isCancelled()) {
						connection.rollback();
						connection.setAutoCommit(true);
						return null;
					}

					// generate flight peaks
					String peaksTableName = generateFlightPeaks(connection, statement, input, info, stfFile);

					// task cancelled
					if (isCancelled()) {
						connection.rollback();
						connection.setAutoCommit(true);
						return null;
					}

					// plot typical flight
					Path file = plotFlight(connection, info, peaksTableName, stfFile);

					// task cancelled
					if (isCancelled()) {
						connection.rollback();
						connection.setAutoCommit(true);
						return null;
					}

					// save typical flight plot
					savePlot(statement, connection, file, stfFile);

					// task cancelled
					if (isCancelled()) {
						connection.rollback();
						connection.setAutoCommit(true);
						return null;
					}

					// remove peaks table
					statement.executeUpdate("drop table AURORA." + peaksTableName);

					// task cancelled
					if (isCancelled()) {
						connection.rollback();
						connection.setAutoCommit(true);
						return null;
					}
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
			taskPanel_.getOwner().runTaskInParallel(new PlotFastLongestFlight(eqStress_));
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
	 * Plots typical flight.
	 *
	 * @param connection
	 *            Database connection.
	 * @param info
	 *            Longest flight info.
	 * @param peaksTableName
	 *            Flight peaks table name.
	 * @param stfFile
	 *            STF file.
	 * @return Path to plot image file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path plotFlight(Connection connection, LongestFlightInfo info, String peaksTableName, STFFile stfFile) throws Exception {

		// update info
		updateMessage("Plotting typical flight...");

		// create path to output image
		Path output = output_ == null ? getWorkingDirectory().resolve("longestTypicalFlight.png") : output_;

		// create chart
		String title = info.getFlightName();
		title += "\n(" + FileType.getNameWithoutExtension(stfFile.getName()) + ")";
		JFreeChart chart = CrosshairListenerXYPlot.createXYLineChart(title, "Time", "Stress", null, PlotOrientation.VERTICAL, true, false, false, null);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		XYPlot plot = chart.getXYPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.setDomainCrosshairVisible(false);
		plot.setRangeCrosshairVisible(false);
		plot.setDomainPannable(false);
		plot.setRangePannable(false);
		plot.setShadowGenerator(null);
		plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

		// plot
		XYDataset dataset = new PlotFlightProcess(this, peaksTableName).start(connection);

		// set dataset
		plot.setDataset(dataset);

		// setup chart dimensions
		int width = 658;
		int height = 597;

		// plot
		ChartUtilities.saveChartAsPNG(output.toFile(), chart, width, height);

		// return path to output image
		return output;
	}

	/**
	 * Saves the level crossings plot to database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @param file
	 *            Path to level crossings plot.
	 * @param stfFile
	 *            STF file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void savePlot(Statement statement, Connection connection, Path file, STFFile stfFile) throws Exception {

		// update info
		updateMessage("Saving typical flight plot to database...");

		// get pilot point id
		int id = stfFile.getID();

		// check if any data exists in database
		boolean exists = false;
		String sql = "select image from pilot_point_tf_l where id = " + id;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				exists = true;
			}
		}

		// create statement
		if (exists) {
			sql = "update pilot_point_tf_l set image = ? where id = " + id;
		}
		else {
			sql = "insert into pilot_point_tf_l(id, image) values(?, ?)";
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
						update.setInt(1, id);
						update.setBlob(2, inputStream, imageBytes.length);
						update.executeUpdate();
					}
				}
			}
		}
	}

	/**
	 * Generates stress sequence.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param input
	 *            Analysis input.
	 * @param info
	 *            Longest flight info.
	 * @param stfFile
	 *            STF file.
	 * @return Name of peaks table.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String generateFlightPeaks(Connection connection, Statement statement, FastEquivalentStressInput input, LongestFlightInfo info, STFFile stfFile) throws Exception {

		// update info
		updateMessage("Generating flight peaks...");

		// get spectrum file IDs
		updateMessage("Getting spectrum file IDs from database...");
		Spectrum cdfSet = stfFile.getParentItem();
		int anaFileID = cdfSet.getANAFileID();
		int txtFileID = cdfSet.getTXTFileID();
		int convTableID = cdfSet.getConversionTableID();

		// get DP ratio
		updateMessage("Computing delta-p ratio...");
		DPRatio dpRatio = getDPRatio(connection, statement, anaFileID, txtFileID, convTableID, stfFile, input);

		// get DT parameters
		updateMessage("Computing delta-t interpolation...");
		DTInterpolator dtInterpolator = getDTInterpolator(connection, statement, txtFileID, stfFile, input);

		// create peaks table
		updateMessage("Creating flight peaks table...");
		String sthPeaksTableName = createPeaksTable(statement);

		// prepare statement for inserting STH peaks
		String sql = "insert into " + sthPeaksTableName + "(peak_num, peak_val, oneg, dp, dt) values(?, ?, ?, ?, ?)";
		try (PreparedStatement insertPeak = connection.prepareStatement(sql)) {

			// prepare statement for selecting 1g issy code
			sql = "select flight_phase, issy_code, oneg_order from txt_codes where file_id = " + txtFileID + " and one_g_code = ? and increment_num = 0";
			try (PreparedStatement select1GIssyCode = connection.prepareStatement(sql)) {

				// prepare statement for selecting increment issy code
				sql = "select flight_phase, issy_code, factor_1, factor_2, factor_3, factor_4, factor_5, factor_6, factor_7, factor_8 ";
				sql += " from txt_codes where file_id = " + txtFileID;
				sql += " and one_g_code = ? and increment_num = ? and direction_num = ? and (nl_factor_num is null or nl_factor_num = ?)";
				try (PreparedStatement selectIncrementIssyCode = connection.prepareStatement(sql)) {

					// prepare statement for selecting STF stress
					sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stfFile.getStressTableID() + " where file_id = " + stfFile.getID() + " and issy_code = ?";
					try (PreparedStatement selectSTFStress = connection.prepareStatement(sql)) {

						// execute statement for getting ANA peaks
						sql = "select peak_num, fourteen_digit_code, delta_p, delta_t from ana_peaks_" + anaFileID + " where flight_id = " + info.getFlightID();
						try (ResultSet anaPeaks = statement.executeQuery(sql)) {

							// loop over peaks
							HashMap<String, OnegStress> oneg = new HashMap<>();
							HashMap<String, Double> inc = new HashMap<>();
							int peakCount = 0;
							while (anaPeaks.next()) {

								// task cancelled
								if (isCancelled() || Thread.currentThread().isInterrupted())
									return null;

								// update progress
								updateProgress(peakCount, info.getNumberOfPeaks());
								peakCount++;

								// insert peak into peaks table
								insertPeak(anaPeaks, dpRatio, dtInterpolator, insertPeak, select1GIssyCode, selectSTFStress, selectIncrementIssyCode, oneg, inc, input);
							}
						}
					}
				}
			}
		}

		// return name of peaks table
		return sthPeaksTableName;
	}

	/**
	 * Inserts peak into peaks table.
	 *
	 * @param anaPeaks
	 *            Result set storing the ANA peaks.
	 * @param dpRatio
	 *            Delta-p ratio.
	 * @param dtInterpolator
	 *            Delta-t interpolator.
	 * @param insertPeak
	 *            Database statement for inserting peak.
	 * @param select1gIssyCode
	 *            Database statement for selecting 1g load case.
	 * @param selectSTFStress
	 *            Database statement for selecting STF stresses.
	 * @param selectIncrementIssyCode
	 *            Database statement for selecting increment load case.
	 * @param oneg
	 *            1g stress mapping.
	 * @param inc
	 *            Incremental stress mapping.
	 * @param input
	 *            Analysis input.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void insertPeak(ResultSet anaPeaks, DPRatio dpRatio, DTInterpolator dtInterpolator, PreparedStatement insertPeak, PreparedStatement select1gIssyCode, PreparedStatement selectSTFStress, PreparedStatement selectIncrementIssyCode, HashMap<String, OnegStress> oneg,
			HashMap<String, Double> inc, FastEquivalentStressInput input) throws Exception {

		// get class code
		String classCode = anaPeaks.getString("fourteen_digit_code");
		int peakNum = anaPeaks.getInt("peak_num");
		String onegCode = classCode.substring(0, 4);

		// get 1g stress
		OnegStress onegStress = oneg.get(onegCode);
		if (onegStress == null) {
			onegStress = get1GStress(selectSTFStress, select1gIssyCode, onegCode, input);
			oneg.put(onegCode, onegStress);
		}

		// get segment
		Segment segment = onegStress.getSegment();

		// get increment stress
		Double incStress = inc.get(classCode);
		if (incStress == null) {
			incStress = getIncStress(selectSTFStress, selectIncrementIssyCode, classCode, onegCode, segment, input);
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

		// compute and modify total stress
		double totalStress = onegStress.getStress() + incStress + dpStress + dtStress;

		// execute update
		insertPeak.setInt(1, peakNum); // peak number
		insertPeak.setDouble(2, totalStress); // peak value (total stress)
		insertPeak.setDouble(3, onegStress.getStress()); // 1g stress
		insertPeak.setDouble(4, dpStress); // delta-p stress
		insertPeak.setDouble(5, dtStress); // delta-t stress
		insertPeak.executeUpdate();
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
	 * @param input
	 *            Analysis input.
	 * @return Returns the increment stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getIncStress(PreparedStatement selectSTFStress, PreparedStatement selectIncrementIssyCode, String classCode, String onegCode, Segment segment, FastEquivalentStressInput input) throws Exception {

		// add default increment stress
		double totalIncrementStress = 0.0;

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
			selectIncrementIssyCode.setString(3, block.substring(1)); // direction
																		// number
			selectIncrementIssyCode.setString(4, block.substring(0, 1)); // factor
																			// number

			// query issy code, factor and event name
			try (ResultSet resultSet = selectIncrementIssyCode.executeQuery()) {

				// loop over increments
				while (resultSet.next()) {

					// get issy code, factor and event name
					String issyCode = resultSet.getString("issy_code");
					double factor = resultSet.getDouble("factor_" + block.substring(0, 1));

					// compute and modify increment stress
					double stress = factor * getSTFStress(selectSTFStress, issyCode, input);
					stress = modifyStress(issyCode, segment, GenerateStressSequenceInput.INCREMENT, stress, input);

					// add to total increment stress
					totalIncrementStress += stress;
				}
			}
		}

		// set increment stresses
		return totalIncrementStress;
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
	private static OnegStress get1GStress(PreparedStatement selectSTFStress, PreparedStatement select1gIssyCode, String onegCode, FastEquivalentStressInput input) throws Exception {

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
		return new OnegStress(segment, stress);
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
	 * Creates STH peaks table.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Name of newly created STH peaks table.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String createPeaksTable(Statement statement) throws Exception {

		// get stress type
		String tableName = null;
		if (eqStress_ instanceof FastFatigueEquivalentStress) {
			tableName = "FAST_FATIGUE_STH_PEAKS_TF_L_" + eqStress_.getID();
		}
		else if (eqStress_ instanceof FastPreffasEquivalentStress) {
			tableName = "FAST_PREFFAS_STH_PEAKS_TF_L_" + eqStress_.getID();
		}
		else if (eqStress_ instanceof FastLinearEquivalentStress) {
			tableName = "FAST_LINEAR_STH_PEAKS_TF_L_" + eqStress_.getID();
		}

		// create table
		statement.executeUpdate("CREATE TABLE AURORA." + tableName + "(PEAK_NUM INT NOT NULL, PEAK_VAL DOUBLE NOT NULL, ONEG DOUBLE NOT NULL, DP DOUBLE NOT NULL, DT DOUBLE NOT NULL)");

		// return table name
		return tableName;
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
	 * @param stfFile
	 *            STF file.
	 * @param input
	 *            Analysis input.
	 * @return Delta-t interpolation, or null if no delta-t interpolation is supplied.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DTInterpolator getDTInterpolator(Connection connection, Statement statement, int txtFileID, STFFile stfFile, FastEquivalentStressInput input) throws Exception {

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
	 * @param anaFileID
	 *            ANA file ID.
	 * @param txtFileID
	 *            TXT file ID.
	 * @param convTableID
	 *            Conversion table ID.
	 * @param stfFile
	 *            STF file.
	 * @param input
	 *            Analysis input.
	 * @return Delta-p ratio.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DPRatio getDPRatio(Connection connection, Statement statement, int anaFileID, int txtFileID, int convTableID, STFFile stfFile, FastEquivalentStressInput input) throws Exception {

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
	 * Retrieves and returns longest typical flight info.
	 *
	 * @param statement
	 *            Database statement.
	 * @param stfFile
	 *            STF file.
	 * @return Longest typical flight info.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private LongestFlightInfo getLongestFlightInfo(Statement statement, STFFile stfFile) throws Exception {

		// update info
		updateMessage("Getting longest flight info...");

		// initialize info
		LongestFlightInfo info = null;

		// get ANA file ID
		Spectrum cdfSet = stfFile.getParentItem();
		int anaFileID = cdfSet.getANAFileID();

		// create and execute query
		statement.setMaxRows(1);
		String sql = "select flight_id, name, num_peaks from ana_flights where ";
		sql += "file_id = " + anaFileID + " order by num_peaks desc";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				int flightID = resultSet.getInt("flight_id");
				String flightName = resultSet.getString("name");
				int numPeaks = resultSet.getInt("num_peaks");
				info = new LongestFlightInfo(flightName, flightID, numPeaks);
			}
		}

		// reset statement
		statement.setMaxRows(0);

		// return info
		return info;
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

	/**
	 * Private class for storing longest flight info.
	 *
	 * @author Murat Artim
	 * @date Jul 8, 2016
	 * @time 5:34:02 PM
	 */
	private class LongestFlightInfo {

		/** Name of longest flight. */
		private final String flightName_;

		/** Number of longest flight and number of peaks. */
		private final int flightID_, numPeaks_;

		/**
		 * Creates longest flight info.
		 *
		 * @param flightName
		 *            Flight name.
		 * @param flightID
		 *            Flight ID.
		 * @param numPeaks
		 *            Number of peaks.
		 */
		public LongestFlightInfo(String flightName, int flightID, int numPeaks) {
			flightName_ = flightName;
			flightID_ = flightID;
			numPeaks_ = numPeaks;
		}

		/**
		 * Returns flight name.
		 *
		 * @return Flight name.
		 */
		public String getFlightName() {
			return flightName_;
		}

		/**
		 * Returns flight ID.
		 *
		 * @return Flight ID.
		 */
		public int getFlightID() {
			return flightID_;
		}

		/**
		 * Returns number of peaks.
		 *
		 * @return Number of peaks.
		 */
		public int getNumberOfPeaks() {
			return numPeaks_;
		}
	}
}
