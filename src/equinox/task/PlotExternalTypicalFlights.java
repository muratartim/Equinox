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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import equinox.Equinox;
import equinox.controller.ExternalPlotViewPanel;
import equinox.controller.InputPanel;
import equinox.controller.ViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.SeriesKey;
import equinox.data.fileType.ExternalFlight;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.input.ExternalFlightPlotInput;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;

/**
 * Class for plot external typical flights task.
 *
 * @author Murat Artim
 * @date Mar 15, 2015
 * @time 5:10:42 PM
 */
public class PlotExternalTypicalFlights extends InternalEquinoxTask<XYDataset> implements ShortRunningTask, MultipleInputTask<ExternalFlight>, AutomaticTaskOwner<XYDataset> {

	/** Automatic inputs. */
	private final List<ExternalFlight> flights_;

	/** Plot input. */
	private final ExternalFlightPlotInput input_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<XYDataset>> automaticTasks_ = null;

	/**
	 * Creates plot STH flights task.
	 *
	 * @param input
	 *            Plot input.
	 */
	public PlotExternalTypicalFlights(ExternalFlightPlotInput input) {
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
	public void addAutomaticTask(String taskID, EmbeddedTask<XYDataset> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<XYDataset>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public String getTaskTitle() {
		return "Plot external typical flights";
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
		updateTitle("Plotting external typical flights...");

		// create dataset
		XYSeriesCollection dataset = new XYSeriesCollection();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// plot stresses
			plot(connection, dataset);
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

			// get chart data
			XYDataset dataset = get();

			// user initiated task
			if (automaticTasks_ == null) {

				// get spectrum view panel
				ExternalPlotViewPanel panel = (ExternalPlotViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.EXTERNAL_PLOT_VIEW);

				// set data
				panel.plottingCompleted(dataset);

				// show spectrum plot panel
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.EXTERNAL_PLOT_VIEW);
				taskPanel_.getOwner().getOwner().getInputPanel().showSubPanel(InputPanel.PLOT_EXTERNAL_FLIGHTS_PANEL);
			}

			// automatic task
			else {
				automaticTaskOwnerSucceeded(dataset, automaticTasks_, taskPanel_);
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
	 * @param dataset
	 *            Dataset.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plot(Connection connection, XYSeriesCollection dataset) throws Exception {

		// get flight IDs
		updateMessage("Plotting stresses...");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// prepare statement to get file id
			String sql = "select file_id from ext_sth_flights where flight_id = ?";
			try (PreparedStatement getFileId = connection.prepareStatement(sql)) {

				// loop over flights
				for (ExternalFlight flight : flights_) {

					// update info
					updateMessage("Getting peaks for flight '" + flight.getName() + "' from database...");

					// create series name
					String name = getFlightName(flight);

					// create series
					XYSeries series = new XYSeries(new SeriesKey(checkName(name, dataset), flight.getID()));

					// get file id
					int fileId = -1;
					getFileId.setInt(1, flight.getID());
					try (ResultSet resultSet = getFileId.executeQuery()) {
						if (resultSet.next()) {
							fileId = resultSet.getInt("file_id");
						}
					}

					// create query
					sql = "select peak_num, peak_val from ext_sth_peaks_" + fileId + " where flight_id = " + flight.getID() + " order by peak_num";

					// add chart data to series
					try (ResultSet resultSet = statement.executeQuery(sql)) {

						// loop over peaks
						while (resultSet.next()) {

							// get peak number
							int peakNum = resultSet.getInt("peak_num");

							// get stress
							double stress = resultSet.getDouble("peak_val");

							// add to data
							series.add(peakNum, stress);
						}
					}

					// add series to dataset
					dataset.addSeries(series);
				}
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

		// get naming options
		boolean[] options = input_.getNamingOptions();

		// include stress sequence name
		if (options[0]) {
			name += flight.getParentItem().getParentItem().getName() + ", ";
		}

		// include EID
		if (options[1]) {
			name += ExternalStressSequence.getEID(flight.getParentItem().getParentItem().getName()) + ", ";
		}

		// include flight name
		if (options[2]) {
			name += flight.getName() + ", ";
		}

		// include A/C program
		if (options[3]) {
			name += flight.getParentItem().getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (options[4]) {
			name += flight.getParentItem().getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (options[5]) {
			name += flight.getParentItem().getParentItem().getMission() + ", ";
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
}
