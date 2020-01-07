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
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import equinox.Equinox;
import equinox.data.fileType.Flight;
import equinox.data.fileType.StressSequence;
import equinox.plugin.FileType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.PostProcessingTask;
import equinox.utility.CrosshairListenerXYPlot;

/**
 * Class for save longest typical flight plot task.
 *
 * @author Murat Artim
 * @date 25 Jul 2016
 * @time 15:41:54
 */
public class SaveLongestFlightPlot extends TemporaryFileCreatingTask<Void> implements ShortRunningTask, PostProcessingTask {

	/** Stress sequence. */
	private final StressSequence sequence_;

	/**
	 * Creates save longest typical flight plot task.
	 *
	 * @param sequence
	 *            Stress sequence.
	 */
	public SaveLongestFlightPlot(StressSequence sequence) {
		sequence_ = sequence;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save longest typical flight for '" + sequence_.getName() + "'";
	}

	@Override
	protected Void call() throws Exception {

		// update info
		updateMessage("Saving longest typical flight plot...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get longest typical flight info
				Flight flight = getFlight(statement, "num_peaks", true);

				// plot typical flight
				Path file = plotFlight(statement, flight);

				// save typical flight plot
				savePlot(statement, connection, file);
			}
		}

		// return
		return null;
	}

	/**
	 * Finds and returns the flight with the given criteria.
	 *
	 * @param statement
	 *            SQL statement.
	 * @param colName
	 *            Database column name.
	 * @param isDesc
	 *            True if descending order.
	 * @return The selected flight.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Flight getFlight(Statement statement, String colName, boolean isDesc) throws Exception {
		Flight flight = null;
		String sql = "select flight_id, name from sth_flights where file_id = " + sequence_.getID() + " order by " + colName + (isDesc ? " desc" : "");
		statement.setMaxRows(1);
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				flight = new Flight(resultSet.getString("name"), resultSet.getInt("flight_id"));
			}
		}
		statement.setMaxRows(0);
		return flight;
	}

	/**
	 * Plots typical flight.
	 *
	 * @param statement
	 *            Database statement.
	 * @param flight
	 *            Typical flight.
	 * @return Path to plot image file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path plotFlight(Statement statement, Flight flight) throws Exception {

		// update info
		updateMessage("Plotting longest typical flight...");

		// create path to output image
		Path output = getWorkingDirectory().resolve("longestTypicalFlight.png");

		// create chart
		String title = flight.getName();
		title += "\n(" + FileType.getNameWithoutExtension(sequence_.getParentItem().getName()) + ")";
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
		XYDataset dataset = plot(statement, flight);

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
	 * Plots the longest typical flight.
	 *
	 * @param statement
	 *            Database statement.
	 * @param flight
	 *            Longest typical flight.
	 * @return Chart dataset.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private XYDataset plot(Statement statement, Flight flight) throws Exception {

		// create dataset
		XYSeriesCollection dataset = new XYSeriesCollection();

		// create series
		XYSeries totalStress = new XYSeries("Total Stress");
		XYSeries oneg = new XYSeries("1G");
		XYSeries dp = new XYSeries("Delta-P");
		XYSeries dt = new XYSeries("Delta-T");

		// create query
		String sql = "select peak_num, peak_val, oneg_stress, dp_stress, dt_stress ";
		sql += "from sth_peaks_" + sequence_.getID() + " where flight_id = " + flight.getID() + " order by peak_num asc";
		try (ResultSet peaks = statement.executeQuery(sql)) {

			// loop over peaks
			while (peaks.next()) {

				// get peak number
				int peakNum = peaks.getInt("peak_num");

				// add to data
				totalStress.add(peakNum, peaks.getDouble("peak_val"));
				oneg.add(peakNum, peaks.getDouble("oneg_stress"));
				dp.add(peakNum, peaks.getDouble("dp_stress"));
				dt.add(peakNum, peaks.getDouble("dt_stress"));
			}
		}

		// add series to dataset
		dataset.addSeries(totalStress);
		dataset.addSeries(oneg);
		dataset.addSeries(dp);
		dataset.addSeries(dt);

		// return dataset
		return dataset;
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
	private void savePlot(Statement statement, Connection connection, Path file) throws Exception {

		// update info
		updateMessage("Saving typical flight plot to database...");

		// get pilot point id
		int id = sequence_.getParentItem().getID();

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
}
