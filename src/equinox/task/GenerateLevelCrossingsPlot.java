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
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import equinox.Equinox;
import equinox.controller.DamageContributionViewPanel;
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
import equinox.data.input.HistogramInput;
import equinox.data.input.HistogramInput.HistogramDataType;
import equinox.data.input.LevelCrossingInput;
import equinox.plugin.FileType;
import equinox.process.ESAProcess;
import equinox.process.PlotHistogramProcess;
import equinox.process.PlotLevelCrossingProcess;
import equinox.process.Rainflow;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.ServerUtility;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.utility.Utility;

/**
 * Class for generate level crossings plot task.
 *
 * @author Murat Artim
 * @date Jul 6, 2016
 * @time 3:05:32 PM
 */
public class GenerateLevelCrossingsPlot extends TemporaryFileCreatingTask<Path> implements LongRunningTask, SingleInputTask<SpectrumItem>, AutomaticTaskOwner<Path> {

	/** Equivalent stress. */
	private SpectrumItem eqStress_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.00");

	/** Number of columns. */
	private static final int NUM_COLS = 8;

	/** STH file indices. */
	private int rowIndex_ = 0, colIndex_ = 0;

	/** STH line. */
	private String line_;

	/** Omission process. */
	private Process omission_;

	/** Rainflow process. */
	private ESAProcess<Void> rainflow_;

	/** True to plot the level crossings after the process is completed. */
	private final boolean plot_, plotLevelCrossings_;

	/** Path to output file. */
	private final Path output_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates generate level crossings plot task.
	 *
	 * @param eqStress
	 *            Equivalent stress. Can be null for automatic execution.
	 * @param plot
	 *            True to plot after generation.
	 * @param plotLevelCrossings
	 *            True to plot the level crossings after the process is completed. False to plot rainflow histogram.
	 * @param output
	 *            Path to output file. Can be null if plot should not be saved to output file.
	 */
	public GenerateLevelCrossingsPlot(SpectrumItem eqStress, boolean plot, boolean plotLevelCrossings, Path output) {
		eqStress_ = eqStress;
		plot_ = plot;
		plotLevelCrossings_ = plotLevelCrossings;
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
		return "Generate level crossings plot";
	}

	@Override
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_LEVEL_CROSSINGS);

		// update info
		updateMessage("Generating level crossings plot...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// plot level crossings
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
			taskPanel_.getOwner().runTaskInParallel(plotLevelCrossings_ ? new PlotFastLevelCrossings(eqStress_) : new PlotFastHistogram(eqStress_));
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
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// destroy sub processes (if still running)
		if (omission_ != null && omission_.isAlive()) {
			omission_.destroyForcibly();
		}
		if (rainflow_ != null) {
			rainflow_.cancel();
		}

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// destroy sub processes (if still running)
		if (omission_ != null && omission_.isAlive()) {
			omission_.destroyForcibly();
		}
		if (rainflow_ != null) {
			rainflow_.cancel();
		}

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

		// get spectrum file IDs
		STFFile stfFile = (STFFile) eqStress_.getParentItem();

		// create statement
		try (Statement statement = connection.createStatement()) {

			// get analysis input
			FastEquivalentStressInput input = getAnalysisInput(statement);

			// generate stress sequence
			Path sthFile = generateStressSequence(connection, statement, input, stfFile);

			// task cancelled
			if (isCancelled() || sthFile == null)
				return;

			// apply omission
			if (input.isApplyOmission()) {
				sthFile = applyOmission(sthFile, input);
			}

			// task cancelled
			if (isCancelled())
				return;

			// create rainflow cycles table
			String rainflowCyclesTableName = createRainflowCyclesTable(statement);

			// set progress indeterminate
			updateProgress(-1, 100);

			// run rainflow process
			rainflow_ = new Rainflow(this, sthFile, eqStress_, rainflowCyclesTableName);
			rainflow_.start(connection);

			// plot level crossings
			Path levelCrossingsPlotFile = plotLevelCrossings(connection, stfFile, rainflowCyclesTableName);

			// save level crossings plot
			saveLevelCrossingPlot(statement, connection, levelCrossingsPlotFile, stfFile);

			// plot rainflow histogram
			Path rainflowHistogramPlotFile = plotRainflowHistogram(connection, stfFile, rainflowCyclesTableName);

			// save rainflow histogram plot
			saveRainflowHistogramPlot(statement, connection, rainflowHistogramPlotFile, stfFile);

			// remove rainflow cycles table
			statement.executeUpdate("drop table AURORA." + rainflowCyclesTableName);
		}
	}

	/**
	 * Saves the rainflow histogram plot to database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @param file
	 *            Path to rainflow histogram plot.
	 * @param stfFile
	 *            STF file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveRainflowHistogramPlot(Statement statement, Connection connection, Path file, STFFile stfFile) throws Exception {

		// update info
		updateMessage("Saving rainflow histogram plot to database...");

		// get pilot point id
		int id = stfFile.getID();

		// check if any data exists in database
		boolean exists = false;
		String sql = "select image from pilot_point_st_rh where id = " + id;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				exists = true;
			}
		}

		// create statement
		if (exists) {
			sql = "update pilot_point_st_rh set image = ? where id = " + id;
		}
		else {
			sql = "insert into pilot_point_st_rh(id, image) values(?, ?)";
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
	 * Plots rainflow histogram.
	 *
	 * @param connection
	 *            Database connection.
	 * @param stfFile
	 *            STF file.
	 * @param rainflowCyclesTableName
	 *            Rainflow cycles table name.
	 * @return Path to plot image file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path plotRainflowHistogram(Connection connection, STFFile stfFile, String rainflowCyclesTableName) throws Exception {

		// update info
		updateMessage("Plotting rainflow histogram...");

		// create path to output image
		Path output = getWorkingDirectory().resolve("rainflowHistogram.png");
		if (output_ != null && !plotLevelCrossings_) {
			output = output_;
		}

		// create input
		HistogramInput input = new HistogramInput();
		input.setDataType(HistogramDataType.MEAN_STRESS);
		input.setLimit(10);
		input.setLabelsVisible(true);
		input.setOrder(true);

		// create chart
		String title = "Rainflow Histogram";
		title += "\n(" + FileType.getNameWithoutExtension(stfFile.getName()) + ")";
		JFreeChart chart = ChartFactory.createBarChart(title, "Mean stress", "Number of cycles", null, PlotOrientation.VERTICAL, false, false, false);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.getDomainAxis().setMaximumCategoryLabelLines(10);
		plot.setRangePannable(false);

		// set item label generator
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardBarPainter());
		renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		renderer.setBaseToolTipGenerator(null);

		// plot
		CategoryDataset dataset = new PlotHistogramProcess(this, input, rainflowCyclesTableName, eqStress_).start(connection);

		// set dataset
		plot.setDataset(dataset);

		// set colors
		if (dataset.getRowCount() <= DamageContributionViewPanel.COLORS.length) {
			for (int i = 0; i < dataset.getRowCount(); i++) {
				renderer.setSeriesPaint(i, DamageContributionViewPanel.COLORS[i]);
			}
		}

		// set label visibility
		for (int i = 0; i < dataset.getRowCount(); i++) {
			renderer.setSeriesItemLabelsVisible(i, true);
		}

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
	private void saveLevelCrossingPlot(Statement statement, Connection connection, Path file, STFFile stfFile) throws Exception {

		// update info
		updateMessage("Saving level crossings plot to database...");

		// get pilot point id
		int id = stfFile.getID();

		// check if any data exists in database
		boolean exists = false;
		String sql = "select image from pilot_point_lc where id = " + id;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				exists = true;
			}
		}

		// create statement
		if (exists) {
			sql = "update pilot_point_lc set image = ? where id = " + id;
		}
		else {
			sql = "insert into pilot_point_lc(id, image) values(?, ?)";
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
	 * Plots level crossings.
	 *
	 * @param connection
	 *            Database connection.
	 * @param stfFile
	 *            STF file.
	 * @param rainflowCyclesTableName
	 *            Rainflow cycles table name.
	 * @return Path to plot image file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path plotLevelCrossings(Connection connection, STFFile stfFile, String rainflowCyclesTableName) throws Exception {

		// update info
		updateMessage("Plotting level crossings...");

		// create path to output image
		Path output = getWorkingDirectory().resolve("levelCrossings.png");
		if (output_ != null && plotLevelCrossings_) {
			output = output_;
		}

		// create input
		LevelCrossingInput input = new LevelCrossingInput(true, null);

		// set naming parameters
		input.setIncludeSpectrumName(false);
		input.setIncludeSTFName(false);
		input.setIncludeEID(false);
		input.setIncludeSequenceName(false);
		input.setIncludeMaterialName(false);
		input.setIncludeOmissionLevel(false);
		input.setIncludeProgram(false);
		input.setIncludeSection(false);
		input.setIncludeMission(true);

		// create empty chart
		String title = "Level Crossings\n(" + FileType.getNameWithoutExtension(stfFile.getName()) + ")";
		JFreeChart chart = ChartFactory.createXYLineChart(title, "", "", null);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		XYPlot plot = chart.getXYPlot();
		LogarithmicAxis xAxis = new LogarithmicAxis("Number of Cycles (Normalized by spectrum validities)");
		xAxis.setAllowNegativesFlag(true);
		plot.setDomainAxis(xAxis);
		plot.setRangeAxis(new NumberAxis("Stress"));
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);

		// plot
		XYSeriesCollection dataset = new PlotLevelCrossingProcess(this, input, rainflowCyclesTableName, Arrays.asList(eqStress_)).start(connection);

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
	 * Creates rainflow cycles table.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Rainflow cycles table name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String createRainflowCyclesTable(Statement statement) throws Exception {

		// get stress type
		String tableName = null;
		if (eqStress_ instanceof FastFatigueEquivalentStress) {
			tableName = "FAST_FATIGUE_RAINFLOW_CYCLES_" + eqStress_.getID();
		}
		else if (eqStress_ instanceof FastPreffasEquivalentStress) {
			tableName = "FAST_PREFFAS_RAINFLOW_CYCLES_" + eqStress_.getID();
		}
		else if (eqStress_ instanceof FastLinearEquivalentStress) {
			tableName = "FAST_LINEAR_RAINFLOW_CYCLES_" + eqStress_.getID();
		}

		// create table
		statement.executeUpdate(
				"CREATE TABLE AURORA." + tableName + "(STRESS_ID INT NOT NULL, CYCLE_NUM INT NOT NULL, NUM_CYCLES DOUBLE NOT NULL, MAX_VAL DOUBLE NOT NULL, MIN_VAL DOUBLE NOT NULL, MEAN_VAL DOUBLE NOT NULL, R_RATIO DOUBLE NOT NULL, AMP_VAL DOUBLE NOT NULL, RANGE_VAL DOUBLE NOT NULL)");

		// return table name
		return tableName;
	}

	/**
	 * Runs omission script on a separate process. This method will wait till the process has ended.
	 *
	 * @param inputSTH
	 *            Input STH file.
	 * @param input
	 *            Analysis input.
	 * @return Path to omission output file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path applyOmission(Path inputSTH, FastEquivalentStressInput input) throws Exception {

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
			pb = new ProcessBuilder(perl.toAbsolutePath().toString(), script.toAbsolutePath().toString(), inputSTHFileName.toString(), Double.toString(input.getOmissionLevel()));
		}

		// create process builder for macOSX
		else if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), inputSTHFileName.toString(), Double.toString(input.getOmissionLevel()));
		}
		else if (Equinox.OS_TYPE.equals(ServerUtility.LINUX)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), inputSTHFileName.toString(), Double.toString(input.getOmissionLevel()));
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
	 * Generates stress sequence.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param input
	 *            Analysis inputs.
	 * @param stfFile
	 *            STF file.
	 * @return Path to generated stress sequence file (STH).
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path generateStressSequence(Connection connection, Statement statement, FastEquivalentStressInput input, STFFile stfFile) throws Exception {

		// update info
		updateMessage("Generating stress sequence...");

		// create path to output file
		Path sthFile = getWorkingDirectory().resolve(FileType.appendExtension(Utility.correctFileName(FileType.getNameWithoutExtension(stfFile.getName())), FileType.STH));

		// create file writer
		try (BufferedWriter writer = Files.newBufferedWriter(sthFile, Charset.defaultCharset())) {

			// write file header
			writer.write(" # STH Generated by Equinox Version " + Equinox.VERSION.toString() + ", Date: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
			writer.newLine();
			writer.write(" #");
			writer.newLine();
			writer.write(" #");
			writer.newLine();
			writer.write(" #");
			writer.newLine();

			// get spectrum file IDs
			Spectrum cdfSet = stfFile.getParentItem();
			int anaFileID = cdfSet.getANAFileID();
			int txtFileID = cdfSet.getTXTFileID();
			int convTableID = cdfSet.getConversionTableID();

			// get DP ratio
			updateMessage("Computing delta-p ratio...");
			DPRatio dpRatio = getDPRatio(connection, statement, anaFileID, txtFileID, convTableID, input, stfFile);

			// get DT parameters
			updateMessage("Computing delta-t interpolation...");
			DTInterpolator dtInterpolator = getDTInterpolator(connection, statement, txtFileID, input, stfFile);

			// get number of flights and peaks of the ANA file
			int numPeaks = getNumberOfPeaks(statement, anaFileID);

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
								HashMap<String, OnegStress> oneg = new HashMap<>();
								HashMap<String, Double> inc = new HashMap<>();
								int peakCount = 0;
								while (anaFlights.next()) {

									// task cancelled
									if (isCancelled())
										return null;

									// write flight header
									int flightPeaks = anaFlights.getInt("num_peaks");
									writeFlightHeader(writer, anaFlights, flightPeaks);

									// initialize variables
									int rem = flightPeaks % NUM_COLS;
									int numRows = flightPeaks / NUM_COLS + (rem == 0 ? 0 : 1);
									rowIndex_ = 0;
									colIndex_ = 0;
									line_ = "";

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
											writeSTHPeak(writer, anaPeaks, select1GIssyCode, selectSTFStress, selectIncrementIssyCode, oneg, inc, dpRatio, dtInterpolator, rem, numRows, input);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		// return STH file
		return sthFile;
	}

	/**
	 * Writes STH peaks to output file.
	 *
	 * @param writer
	 *            File writer.
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
	 * @param input
	 *            Analysis input.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeSTHPeak(BufferedWriter writer, ResultSet anaPeaks, PreparedStatement select1GIssyCode, PreparedStatement selectSTFStress, PreparedStatement selectIncrementIssyCode, HashMap<String, OnegStress> oneg, HashMap<String, Double> inc, DPRatio dpRatio, DTInterpolator dtInterpolator,
			int rem, int numRows, FastEquivalentStressInput input) throws Exception {

		// get class code
		String classCode = anaPeaks.getString("fourteen_digit_code");
		String onegCode = classCode.substring(0, 4);

		// get 1g stress
		OnegStress onegStress = oneg.get(onegCode);
		if (onegStress == null) {
			onegStress = get1GStress(selectSTFStress, select1GIssyCode, onegCode, input);
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

		// last row
		if (rowIndex_ == numRows - 1) {

			// add peaks
			line_ += String.format("%10s", format_.format(totalStress));
			colIndex_++;

			// last column
			if (colIndex_ == (rem == 0 ? NUM_COLS : rem)) {
				writer.write(line_);
				writer.newLine();
				line_ = "";
				colIndex_ = 0;
				rowIndex_++;
			}
		}

		// other rows
		else {

			// add peaks
			line_ += String.format("%10s", format_.format(totalStress));
			colIndex_++;

			// last column
			if (colIndex_ == NUM_COLS) {
				writer.write(line_);
				writer.newLine();
				line_ = "";
				colIndex_ = 0;
				rowIndex_++;
			}
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
	 * Writes out flight header to output STH file.
	 *
	 * @param writer
	 *            File writer.
	 * @param anaFlights
	 *            ANA flights.
	 * @param flightPeaks
	 *            Number of peaks of the flight.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeFlightHeader(BufferedWriter writer, ResultSet anaFlights, int flightPeaks) throws Exception {

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
		writer.write(line1);
		writer.newLine();
		writer.write(line2);
		writer.newLine();
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
	 * @param input
	 *            Analysis input.
	 * @param stfFile
	 *            STF file.
	 * @return Delta-t interpolation, or null if no delta-t interpolation is supplied.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DTInterpolator getDTInterpolator(Connection connection, Statement statement, int txtFileID, FastEquivalentStressInput input, STFFile stfFile) throws Exception {

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
	 * @param input
	 *            Analysis input.
	 * @param stfFile
	 *            STF file.
	 * @return Delta-p ratio.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DPRatio getDPRatio(Connection connection, Statement statement, int anaFileID, int txtFileID, int convTableID, FastEquivalentStressInput input, STFFile stfFile) throws Exception {

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
