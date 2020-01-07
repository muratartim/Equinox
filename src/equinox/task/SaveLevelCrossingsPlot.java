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
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

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
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.HistogramInput;
import equinox.data.input.HistogramInput.HistogramDataType;
import equinox.data.input.LevelCrossingInput;
import equinox.plugin.FileType;
import equinox.process.PlotHistogramProcess;
import equinox.process.PlotLevelCrossingProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.PostProcessingTask;

/**
 * Class for save level crossings plot task.
 *
 * @author Murat Artim
 * @date 25 Jul 2016
 * @time 17:04:26
 */
public class SaveLevelCrossingsPlot extends TemporaryFileCreatingTask<Void> implements ShortRunningTask, PostProcessingTask {

	/** Fatigue equivalent stress. */
	private final SpectrumItem eqStress_;

	/**
	 * Creates save level crossings plot task.
	 *
	 * @param eqStress
	 *            Fatigue equivalent stress.
	 */
	public SaveLevelCrossingsPlot(SpectrumItem eqStress) {
		eqStress_ = eqStress;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save level crossings plot for '" + eqStress_.getName() + "'";
	}

	@Override
	protected Void call() throws Exception {

		// update info
		updateMessage("Saving level crossings plot...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// plot level crossings
				Path levelCrossingsPlotFile = plotLevelCrossings(connection);

				// save level crossings plot
				saveLevelCrossingPlot(statement, connection, levelCrossingsPlotFile);

				// plot rainflow histogram
				Path rainflowHistogramPlotFile = plotRainflowHistogram(connection);

				// save rainflow histogram plot
				saveRainflowHistogramPlot(statement, connection, rainflowHistogramPlotFile);
			}
		}

		// return
		return null;
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
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveRainflowHistogramPlot(Statement statement, Connection connection, Path file) throws Exception {

		// update info
		updateMessage("Saving rainflow histogram plot to database...");

		// get pilot point id
		int id = eqStress_.getParentItem().getParentItem().getID();

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
	 * @return Path to plot image file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path plotRainflowHistogram(Connection connection) throws Exception {

		// update info
		updateMessage("Plotting rainflow histogram...");

		// create path to output image
		Path output = getWorkingDirectory().resolve("rainflowHistogram.png");

		// create input
		HistogramInput input = new HistogramInput();
		input.setDataType(HistogramDataType.MEAN_STRESS);
		input.setLimit(10);
		input.setLabelsVisible(true);
		input.setOrder(true);

		// create chart
		String title = "Rainflow Histogram";
		title += "\n(" + FileType.getNameWithoutExtension(eqStress_.getParentItem().getParentItem().getName()) + ")";
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
		CategoryDataset dataset = new PlotHistogramProcess(this, input, eqStress_).start(connection);

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
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveLevelCrossingPlot(Statement statement, Connection connection, Path file) throws Exception {

		// update info
		updateMessage("Saving level crossings plot to database...");

		// get pilot point id
		int id = eqStress_.getParentItem().getParentItem().getID();

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
	 * @return Path to plot image file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path plotLevelCrossings(Connection connection) throws Exception {

		// update info
		updateMessage("Plotting level crossings...");

		// create path to output image
		Path output = getWorkingDirectory().resolve("levelCrossings.png");

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
		String title = "Level Crossings\n(" + FileType.getNameWithoutExtension(eqStress_.getParentItem().getParentItem().getName()) + ")";
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
		XYSeriesCollection dataset = new PlotLevelCrossingProcess(this, input, Arrays.asList(eqStress_)).start(connection);

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
}
