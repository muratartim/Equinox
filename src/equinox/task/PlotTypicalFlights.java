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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import equinox.Equinox;
import equinox.controller.InputPanel;
import equinox.controller.PlotViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.Segment;
import equinox.data.SeriesKey;
import equinox.data.fileType.Flight;
import equinox.data.input.FlightPlotInput;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for plot STH flights task.
 *
 * @author Murat Artim
 * @date Sep 2, 2014
 * @time 5:49:38 PM
 */
public class PlotTypicalFlights extends InternalEquinoxTask<XYDataset> implements ShortRunningTask {

	/** Plot input. */
	private final FlightPlotInput input_;

	/** Flight segments. */
	private final HashMap<Flight, ArrayList<Segment>> segments_;

	/**
	 * Creates plot STH flights task.
	 *
	 * @param input
	 *            Plot input.
	 */
	public PlotTypicalFlights(FlightPlotInput input) {
		input_ = input;
		segments_ = new HashMap<>();
	}

	@Override
	public String getTaskTitle() {
		return "Plot typical flights";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected XYDataset call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_TYPICAL_FLIGHT);

		// update progress info
		updateTitle("Plotting typical flights...");

		// create dataset
		XYSeriesCollection dataset = new XYSeriesCollection();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// check whether the STH file is linked to a CDF set
			boolean isTotalStress = input_.isPlotTotalStress();
			boolean isPlotOnTotalStress = input_.isPlotOnTotalStress();

			// plot stresses
			plot(connection, dataset, isTotalStress);

			// plot total stresses
			if (!isTotalStress && isPlotOnTotalStress) {
				plot(connection, dataset, true);
			}
		}

		// return dataset
		return dataset;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get spectrum view panel
			PlotViewPanel panel = (PlotViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.PLOT_VIEW);

			// set data
			panel.plottingCompleted(get(), segments_);

			// show spectrum plot panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.PLOT_VIEW);
			taskPanel_.getOwner().getOwner().getInputPanel().showSubPanel(InputPanel.PLOT_FLIGHTS_PANEL);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Plots input flights on the given chart.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            Dataset.
	 * @param isTotalStress
	 *            True if total stresses are to be plotted.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plot(Connection connection, XYSeriesCollection dataset, boolean isTotalStress) throws Exception {

		// get flight IDs
		updateMessage("Plotting stresses...");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// loop over flights
			for (Flight flight : input_.getFlights()) {

				// update info
				updateMessage("Getting peaks for flight '" + flight.getName() + "' from database...");

				// create series name
				String name = getFlightName(flight);
				if (!isTotalStress) {
					String comp = ", (";
					if (input_.getPlotComponentOption(FlightPlotInput.INCREMENT_STRESS_COMP)) {
						comp += "INC";
					}
					if (input_.getPlotComponentOption(FlightPlotInput.DP_STRESS_COMP)) {
						comp += comp.equals(", (") ? "DP" : " + DP";
					}
					if (input_.getPlotComponentOption(FlightPlotInput.DT_STRESS_COMP)) {
						comp += comp.equals(", (") ? "DT" : " + DT";
					}
					if (input_.getPlotComponentOption(FlightPlotInput.ONE_G_STRESS_COMP)) {
						comp += comp.equals(", (") ? "1G" : " + 1G";
					}
					comp += ")";
					name += comp;
				}

				// create series
				XYSeries series = new XYSeries(new SeriesKey(checkName(name, dataset), flight.getID()));

				// create segments array for flight
				ArrayList<Segment> segments = new ArrayList<>();

				// create query
				String sql = "select peak_num, segment, segment_num";
				if (isTotalStress) {
					sql += ", peak_val";
				}
				else {
					sql += input_.getPlotComponentOption(FlightPlotInput.ONE_G_STRESS_COMP) ? ", oneg_stress" : "";
					sql += input_.getPlotComponentOption(FlightPlotInput.INCREMENT_STRESS_COMP) ? ", inc_stress" : "";
					sql += input_.getPlotComponentOption(FlightPlotInput.DP_STRESS_COMP) ? ", dp_stress" : "";
					sql += input_.getPlotComponentOption(FlightPlotInput.DT_STRESS_COMP) ? ", dt_stress" : "";
				}
				sql += " from sth_peaks_" + flight.getParentItem().getParentItem().getID() + " where flight_id = " + flight.getID() + " order by peak_num";

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

						// add to data
						series.add(peakNum, stress);

						// add to segments
						addToSegments(resultSet.getString("segment"), resultSet.getInt("segment_num"), peakNum, segments);
					}
				}

				// add series to dataset
				dataset.addSeries(series);

				// add to segments
				segments_.put(flight, segments);
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

		// get naming options
		boolean[] options = input_.getNamingOptions();

		// include flight name
		if (options[4]) {
			name += flight.getName() + ", ";
		}

		// include spectrum name
		if (options[0]) {
			name += flight.getParentItem().getParentItem().getParentItem().getParentItem().getName() + ", ";
		}

		// include STF file name
		if (options[1]) {
			name += flight.getParentItem().getParentItem().getParentItem().getName() + ", ";
		}

		// include EID
		if (options[2]) {
			name += flight.getParentItem().getParentItem().getParentItem().getEID() + ", ";
		}

		// include stress sequence name
		if (options[3]) {
			name += flight.getParentItem().getParentItem().getName() + ", ";
		}

		// include A/C program
		if (options[5]) {
			name += flight.getParentItem().getParentItem().getParentItem().getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (options[6]) {
			name += flight.getParentItem().getParentItem().getParentItem().getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (options[7]) {
			name += flight.getParentItem().getParentItem().getParentItem().getMission() + ", ";
		}

		// return name
		return name.substring(0, name.lastIndexOf(", "));
	}

	/**
	 * Checks given series name for uniqueness.
	 *
	 * @param name
	 *            Series name.
	 * @param dataset
	 *            Dataset.
	 * @return Modified name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String checkName(String name, XYSeriesCollection dataset) throws Exception {
		if (dataset.indexOf(name) == -1)
			return name;
		return checkName(name + " ", dataset);
	}

	/**
	 * Adds segment data.
	 *
	 * @param segmentName
	 *            Segment name.
	 * @param segmentNum
	 *            Segment number.
	 * @param peakNum
	 *            Peak number.
	 * @param segments
	 *            Segments array.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void addToSegments(String segmentName, int segmentNum, int peakNum, ArrayList<Segment> segments) throws Exception {

		// segment already exists
		boolean found = false;
		for (Segment segment : segments) {
			if (segment.getName().equals(segmentName) && segment.getSegmentNumber() == segmentNum) {
				if (segment.getEndPeak() == peakNum - 1) {
					segment.setEndPeak(peakNum);
					found = true;
					break;
				}
			}
		}

		// new segment
		if (!found) {
			Segment segment = new Segment(segmentName, segmentNum);
			segment.setStartPeak(peakNum);
			segment.setEndPeak(peakNum);
			segments.add(segment);
		}
	}
}
