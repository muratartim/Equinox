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
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;

import equinox.Equinox;
import equinox.controller.DamageContributionViewPanel;
import equinox.data.fileType.DamageAngle;
import equinox.process.PlotDamageAnglesProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.PlotDamageAngles.ResultOrdering;
import equinox.task.automation.PostProcessingTask;

/**
 * Class for save damage angle plot task.
 *
 * @author Murat Artim
 * @date 26 Jul 2016
 * @time 10:34:25
 */
public class SaveDamageAnglePlot extends TemporaryFileCreatingTask<Void> implements ShortRunningTask, PostProcessingTask {

	/** Damage angle. */
	private final DamageAngle damageAngle_;

	/**
	 * Creates save damage angle plot task.
	 *
	 * @param damageAngle
	 *            Damage angle.
	 */
	public SaveDamageAnglePlot(DamageAngle damageAngle) {
		damageAngle_ = damageAngle;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save damage angle plot for '" + damageAngle_.getName() + "'";
	}

	@Override
	protected Void call() throws Exception {

		// update info
		updateMessage("Saving damage angle plot...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// plot
			Path file = plot(connection);

			// save plot
			savePlot(connection, file);
		}

		// return
		return null;
	}

	/**
	 * Plots damage angles on a file.
	 *
	 * @param connection
	 *            Database connection.
	 * @return Path to image file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path plot(Connection connection) throws Exception {

		// update info
		updateMessage("Plotting damage angles...");

		// create path to output image
		Path output = getWorkingDirectory().resolve("damageAngles.png");

		// create bar chart
		String title = "Damage Angles\n(" + damageAngle_.getParentItem().getName() + ")";
		String xAxisLabel = "Angle (in degrees)";
		String yAxisLabel = "Fatigue Equivalent Stress";
		JFreeChart chart = ChartFactory.createBarChart(title, xAxisLabel, yAxisLabel, null, PlotOrientation.VERTICAL, true, false, false);
		chart.getLegend().setVisible(false);
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
		plot.setRangePannable(true);

		// set item label generator
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardBarPainter());
		renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		renderer.setBaseToolTipGenerator(null);

		// set dataset
		CategoryDataset dataset = new PlotDamageAnglesProcess(this, Arrays.asList(damageAngle_), ResultOrdering.ANGLE).start(connection);
		plot.setDataset(dataset);

		// set label visibility
		for (int i = 0; i < dataset.getRowCount(); i++) {
			renderer.setSeriesItemLabelsVisible(i, true);
		}

		// set colors
		if (dataset.getRowCount() <= DamageContributionViewPanel.COLORS.length) {
			for (int i = 0; i < dataset.getRowCount(); i++) {
				renderer.setSeriesPaint(i, DamageContributionViewPanel.COLORS[i]);
			}
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
	 * @param connection
	 *            Database connection.
	 * @param file
	 *            Path to level crossings plot.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void savePlot(Connection connection, Path file) throws Exception {

		// update info
		updateMessage("Saving damage angle plot to database...");

		// get pilot point id
		int id = damageAngle_.getParentItem().getID();

		// check if any data exists in database
		boolean exists = false;
		String sql = "select image from pilot_point_da where id = " + id;
		try (Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					exists = true;
				}
			}
		}

		// create statement
		if (exists) {
			sql = "update pilot_point_da set image = ? where id = " + id;
		}
		else {
			sql = "insert into pilot_point_da(id, image) values(?, ?)";
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
}
