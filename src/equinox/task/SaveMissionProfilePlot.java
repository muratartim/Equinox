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
import equinox.data.fileType.STFFile;
import equinox.data.fileType.StressSequence;
import equinox.plugin.FileType;
import equinox.process.PlotMissionProfileProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.PostProcessingTask;
import equinox.utility.CrosshairListenerXYPlot;

/**
 * Class for save mission profile plot task.
 *
 * @author Murat Artim
 * @date 25 Jul 2016
 * @time 14:39:31
 */
public class SaveMissionProfilePlot extends TemporaryFileCreatingTask<Void> implements ShortRunningTask, PostProcessingTask {

	/** Stress sequence. */
	private final StressSequence sequence_;

	/**
	 * Creates save mission profile plot task.
	 *
	 * @param sequence
	 *            Stress sequence.
	 */
	public SaveMissionProfilePlot(StressSequence sequence) {
		sequence_ = sequence;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save mission mission profile for '" + sequence_.getName() + "'";
	}

	@Override
	protected Void call() throws Exception {

		// update info
		updateMessage("Saving mission profile plot...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// plot mission profile
			Path file = plotMissionProfile(connection);

			// save mission profile plot
			savePlot(connection, file);
		}

		// return
		return null;
	}

	/**
	 * Saves the profile plot to database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param file
	 *            Path to profile plot.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void savePlot(Connection connection, Path file) throws Exception {

		// update info
		updateMessage("Saving mission profile plot to database...");

		// get pilot point id
		int id = sequence_.getParentItem().getID();

		// check if any data exists in database
		boolean exists = false;
		String sql = "select image from pilot_point_mp where id = " + id;
		try (Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					exists = true;
				}
			}
		}

		// create statement
		if (exists) {
			sql = "update pilot_point_mp set image = ? where id = " + id;
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
						update.setInt(1, id);
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
	 * @return Path to profile plot image.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path plotMissionProfile(Connection connection) throws Exception {

		// update info
		updateMessage("Plotting mission profile...");

		// create path to output image
		Path output = getWorkingDirectory().resolve("missionProfile.png");

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

		// plot
		PlotMissionProfileProcess process = new PlotMissionProfileProcess(this, sequence_, true, true, null);
		XYDataset[] dataset = process.start(connection);
		double maxDiff = process.getMaxPositiveIncrement() - process.getMinNegativeIncrement();

		// set dataset
		plot.setDataset(dataset[0]);
		plot.setDataset(1, dataset[1]);
		plot.setDataset(2, dataset[2]);

		// set chart title
		STFFile stfFile = sequence_.getParentItem();
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
}
