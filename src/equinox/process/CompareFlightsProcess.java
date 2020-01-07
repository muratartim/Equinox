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
package equinox.process;

import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import equinox.data.SeriesKey;
import equinox.data.fileType.Flight;
import equinox.data.input.FlightComparisonInput;
import equinox.data.input.FlightPlotInput;
import equinox.task.InternalEquinoxTask;

/**
 * Class for compare flights process.
 *
 * @author Murat Artim
 * @date Apr 8, 2016
 * @time 11:52:06 AM
 */
public class CompareFlightsProcess implements EquinoxProcess<JFreeChart> {

	/** The owner task. */
	private final InternalEquinoxTask<?> task_;

	/** Comparison input. */
	private final FlightComparisonInput input_;

	/** Typical flights. */
	private final List<Flight> flights_;

	/** Dataset count. */
	private int datasetCount_ = 0;

	/**
	 * Creates compare flights task.
	 *
	 * @param task
	 *            The owner task.
	 * @param input
	 *            Comparison input.
	 * @param flights
	 *            Typical flights to compare.
	 */
	public CompareFlightsProcess(InternalEquinoxTask<?> task, FlightComparisonInput input, List<Flight> flights) {
		task_ = task;
		input_ = input;
		flights_ = flights;
	}

	@Override
	public JFreeChart start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update progress info
		task_.updateMessage("Comparing flights...");

		// create chart
		JFreeChart chart = createChart();
		chart.setTitle("Flight Comparison - " + (input_.getSegment() == null ? "All Segments" : "Segment " + input_.getSegment().toString()));

		// check whether the STH file is linked to a CDF set
		boolean isTotalStress = input_.isPlotTotalStress();
		boolean isPlotOnTotalStress = input_.isPlotOnTotalStress();

		// plot stresses
		plot(connection, chart.getXYPlot(), isTotalStress);

		// plot total stresses
		if (!isTotalStress && isPlotOnTotalStress) {
			plot(connection, chart.getXYPlot(), true);
		}

		// return chart
		return chart;
	}

	/**
	 * Plots input flights on the given chart.
	 *
	 * @param connection
	 *            Database connection.
	 * @param plot
	 *            XY plot.
	 * @param isTotalStress
	 *            True if total stresses are to be plotted.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plot(Connection connection, XYPlot plot, boolean isTotalStress) throws Exception {

		// update info
		task_.updateMessage("Plotting stresses...");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// loop over flights
			for (Flight flight : flights_) {

				// update info
				task_.updateMessage("Getting peaks for flight '" + flight.getName() + "' from database...");

				// create series name
				String name = getFlightName(flight);
				if (!isTotalStress) {
					String comp = " (";
					if (input_.getPlotComponentOption(FlightPlotInput.INCREMENT_STRESS_COMP)) {
						comp += "INC";
					}
					if (input_.getPlotComponentOption(FlightPlotInput.DP_STRESS_COMP)) {
						comp += comp.equals(" (") ? "DP" : " + DP";
					}
					if (input_.getPlotComponentOption(FlightPlotInput.DT_STRESS_COMP)) {
						comp += comp.equals(" (") ? "DT" : " + DT";
					}
					if (input_.getPlotComponentOption(FlightPlotInput.ONE_G_STRESS_COMP)) {
						comp += comp.equals(" (") ? "1G" : " + 1G";
					}
					comp += ")";
					name += comp;
				}

				// create series
				XYSeries series = new XYSeries(new SeriesKey(name, flight.getID()));

				// create query
				String sql = "select peak_num";
				if (isTotalStress) {
					sql += ", peak_val";
				}
				else {
					sql += input_.getPlotComponentOption(FlightPlotInput.ONE_G_STRESS_COMP) ? ", oneg_stress" : "";
					sql += input_.getPlotComponentOption(FlightPlotInput.INCREMENT_STRESS_COMP) ? ", inc_stress" : "";
					sql += input_.getPlotComponentOption(FlightPlotInput.DP_STRESS_COMP) ? ", dp_stress" : "";
					sql += input_.getPlotComponentOption(FlightPlotInput.DT_STRESS_COMP) ? ", dt_stress" : "";
				}
				sql += " from sth_peaks_" + flight.getParentItem().getParentItem().getID() + " where flight_id = " + flight.getID();
				if (input_.getSegment() != null) {
					sql += " and segment = '" + input_.getSegment().getName() + "' and segment_num = " + input_.getSegment().getSegmentNumber();
				}
				sql += " order by peak_num";

				// add chart data to series
				try (ResultSet resultSet = statement.executeQuery(sql)) {

					// loop over peaks
					while (resultSet.next()) {

						// get peak number
						int peakNum = resultSet.getInt("peak_num");

						// get stress
						double stress = 0.0;
						if (isTotalStress) {
							stress = resultSet.getDouble("peak_val");
						}
						else {
							if (input_.getPlotComponentOption(FlightPlotInput.INCREMENT_STRESS_COMP)) {
								stress += resultSet.getDouble("inc_stress");
							}
							if (input_.getPlotComponentOption(FlightPlotInput.DP_STRESS_COMP)) {
								stress += resultSet.getDouble("dp_stress");
							}
							if (input_.getPlotComponentOption(FlightPlotInput.DT_STRESS_COMP)) {
								stress += resultSet.getDouble("dt_stress");
							}
							if (input_.getPlotComponentOption(FlightPlotInput.ONE_G_STRESS_COMP)) {
								stress += resultSet.getDouble("oneg_stress");
							}
						}

						// add to series
						series.add(peakNum, stress);
					}
				}

				// add dataset to plot
				plot.setDataset(datasetCount_, new XYSeriesCollection(series));
				NumberAxis axis = new NumberAxis(name);
				axis.setAutoRangeIncludesZero(false);
				plot.setDomainAxis(datasetCount_, axis);
				plot.setDomainAxisLocation(datasetCount_, AxisLocation.BOTTOM_OR_LEFT);
				plot.mapDatasetToDomainAxis(datasetCount_, datasetCount_);
				XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, input_.isShowMarkers());
				renderer.setSeriesVisible(0, input_.isFlightVisible(flight.getID()));
				plot.setRenderer(datasetCount_, renderer);
				datasetCount_++;
			}
		}
	}

	/**
	 * Returns flight name.
	 *
	 * @param flight
	 *            Flight.
	 * @return Flight name.
	 */
	private String getFlightName(Flight flight) {

		// initialize name
		String name = "";

		// include flight name
		if (input_.getIncludeFlightName()) {
			name += flight.getName() + ", ";
		}

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += flight.getParentItem().getParentItem().getParentItem().getParentItem().getName() + ", ";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += flight.getParentItem().getParentItem().getParentItem().getName() + ", ";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += flight.getParentItem().getParentItem().getParentItem().getEID() + ", ";
		}

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += flight.getParentItem().getParentItem().getName() + ", ";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += flight.getParentItem().getParentItem().getParentItem().getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += flight.getParentItem().getParentItem().getParentItem().getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += flight.getParentItem().getParentItem().getParentItem().getMission() + ", ";
		}

		// return name
		return name.substring(0, name.lastIndexOf(", "));
	}

	/**
	 * Creates and returns chart.
	 *
	 * @return Chart.
	 */
	public static JFreeChart createChart() {

		// create XY line chart
		JFreeChart chart = ChartFactory.createXYLineChart("Flight Comparison", "Time", "Stress", null, PlotOrientation.VERTICAL, true, false, false);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		XYPlot plot = chart.getXYPlot();
		((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(false);
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.setDomainCrosshairVisible(false);
		plot.setRangeCrosshairVisible(false);
		plot.setDomainPannable(true);
		plot.setRangePannable(true);

		// return chart
		return chart;
	}
}
