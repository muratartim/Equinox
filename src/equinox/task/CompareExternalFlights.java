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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

import equinox.Equinox;
import equinox.controller.CompareFlightsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.SeriesKey;
import equinox.data.fileType.ExternalFlight;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.input.ExternalFlightComparisonInput;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;

/**
 * Class for compare external flights task.
 *
 * @author Murat Artim
 * @date Mar 15, 2015
 * @time 5:42:21 PM
 */
public class CompareExternalFlights extends InternalEquinoxTask<JFreeChart> implements ShortRunningTask, MultipleInputTask<ExternalFlight>, AutomaticTaskOwner<JFreeChart> {

	/** Automatic inputs. */
	private final List<ExternalFlight> flights_;

	/** Comparison input. */
	private final ExternalFlightComparisonInput input_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<JFreeChart>> automaticTasks_ = null;

	/** Dataset count. */
	private int datasetCount_ = 0;

	/**
	 * Creates compare flights task.
	 *
	 * @param input
	 *            Comparison input.
	 */
	public CompareExternalFlights(ExternalFlightComparisonInput input) {
		input_ = input;
		flights_ = Collections.synchronizedList(new ArrayList<>());
	}

	/**
	 * Adds typical flight.
	 *
	 * @param flight
	 *            Typical flight to add.
	 */
	public void addTypicalFlight(ExternalFlight flight) {
		flights_.add(flight);
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<JFreeChart> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<JFreeChart>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	synchronized public void setInputThreshold(int inputThreshold) {
		inputThreshold_ = inputThreshold;
	}

	@Override
	synchronized public void addAutomaticInput(AutomaticTaskOwner<ExternalFlight> task, ExternalFlight input, ExecutionMode mode) {
		automaticInputAdded(task, input, mode, flights_, inputThreshold_);
	}

	@Override
	synchronized public void inputFailed(AutomaticTaskOwner<ExternalFlight> task, ExecutionMode mode) {
		inputThreshold_ = automaticInputFailed(task, mode, flights_, inputThreshold_);
	}

	@Override
	public String getTaskTitle() {
		return "Compare external flights";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected JFreeChart call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_TYPICAL_FLIGHT_COMPARISON);

		// update progress info
		updateTitle("Comparing external flights...");

		// create chart
		JFreeChart chart = createChart();
		chart.setTitle("Flight Comparison");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// plot stresses
			plot(connection, chart.getXYPlot());
		}

		// return chart
		return chart;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get chart
			JFreeChart chart = get();

			// user started task
			if (automaticTasks_ == null) {

				// get comparison view panel
				CompareFlightsViewPanel panel = (CompareFlightsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.COMPARE_FLIGHTS_VIEW);

				// set data
				panel.setFlightComparisonChart(chart, true);

				// show flight comparison panel
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.COMPARE_FLIGHTS_VIEW);
			}

			// automatic task
			else {
				automaticTaskOwnerSucceeded(chart, automaticTasks_, taskPanel_);
			}
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
	 * Plots input flights on the given chart.
	 *
	 * @param connection
	 *            Database connection.
	 * @param plot
	 *            XY plot.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plot(Connection connection, XYPlot plot) throws Exception {

		// update info
		updateMessage("Plotting stresses...");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// loop over flights
			for (ExternalFlight flight : flights_) {

				// update info
				updateMessage("Getting peaks for flight '" + flight.getName() + "' from database...");

				// create series name
				String name = getFlightName(flight);

				// create series
				XYSeries series = new XYSeries(new SeriesKey(name, flight.getID()));

				// create query
				String sql = "select peak_num, peak_val";
				sql += " from ext_sth_peaks_" + flight.getParentItem().getParentItem().getID() + " where flight_id = " + flight.getID();
				sql += " order by peak_num";

				// add chart data to series
				try (ResultSet resultSet = statement.executeQuery(sql)) {

					// loop over peaks
					while (resultSet.next()) {

						// get peak number
						int peakNum = resultSet.getInt("peak_num");

						// get stress
						double stress = resultSet.getDouble("peak_val");

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
	private String getFlightName(ExternalFlight flight) {

		// initialize name
		String name = "";

		// include flight name
		if (input_.getIncludeFlightName()) {
			name += flight.getName() + ", ";
		}

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += flight.getParentItem().getParentItem().getName() + ", ";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += ExternalStressSequence.getEID(flight.getParentItem().getParentItem().getName()) + ", ";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += flight.getParentItem().getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += flight.getParentItem().getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += flight.getParentItem().getParentItem().getMission() + ", ";
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

		// return chart
		return chart;
	}
}
