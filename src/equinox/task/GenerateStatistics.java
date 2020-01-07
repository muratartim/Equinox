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
import java.util.concurrent.ExecutionException;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.Equinox;
import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.LoadcaseItem;
import equinox.data.fileType.Flight;
import equinox.data.input.StatisticsInput;
import equinox.data.input.StatisticsInput.Statistic;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for generate statistics task.
 *
 * @author Murat Artim
 * @date Apr 6, 2016
 * @time 10:48:05 PM
 */
public class GenerateStatistics extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask {

	/** Input. */
	private final StatisticsInput input_;

	/** Chart labels. */
	private String xAxisLabel_, yAxisLabel_, title_;

	/**
	 * Creates generate statistics task.
	 *
	 * @param input
	 *            Input.
	 */
	public GenerateStatistics(StatisticsInput input) {
		input_ = input;
	}

	@Override
	public String getTaskTitle() {
		return "Generate flight statistics";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_TYPICAL_FLIGHT_STATISTICS);

		// update progress info
		updateTitle("Generating flight statistics...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// single flight
				if (input_.getFlights().size() == 1) {
					singleFlightStatistics(connection, statement, dataset);
				}
				else {
					multipleFlightStatistics(connection, statement, dataset);
				}
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

			// get dataset
			CategoryDataset dataset = get();

			// get column plot panel
			StatisticsViewPanel panel = (StatisticsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);

			// set chart data to panel
			panel.setPlotData(dataset, title_, null, xAxisLabel_, yAxisLabel_, false, input_.getLabelDisplay(), false);

			// show column chart plot panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Generates statistics for multiple flights.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param dataset
	 *            Chart series.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void multipleFlightStatistics(Connection connection, Statement statement, DefaultCategoryDataset dataset) throws Exception {

		// get statistic
		Statistic statistic = input_.getStatistic();

		// number of peaks
		if (statistic.equals(Statistic.NUM_PEAKS)) {
			numberOfPeaks(statement, dataset);
		}
		else if (statistic.equals(Statistic.FLIGHT_OCCURRENCE)) {
			validity(statement, dataset);
		}
		else if (statistic.equals(Statistic.LOADCASE_OCCURRENCE)) {
			eventOccurrenceMF(connection, statement, dataset);
		}
		else if (statistic.equals(Statistic.MAX_TOTAL)) {
			getStressMF(statement, dataset, "peak_val", "max_val");
		}
		else if (statistic.equals(Statistic.MAX_1G)) {
			getStressMF(statement, dataset, "oneg_stress", "max_1g");
		}
		else if (statistic.equals(Statistic.MAX_INC)) {
			getStressMF(statement, dataset, "inc_stress", "max_inc");
		}
		else if (statistic.equals(Statistic.MAX_DP)) {
			getStressMF(statement, dataset, "dp_stress", "max_dp");
		}
		else if (statistic.equals(Statistic.MAX_DT)) {
			getStressMF(statement, dataset, "dt_stress", "max_dt");
		}
		else if (statistic.equals(Statistic.MIN_TOTAL)) {
			getStressMF(statement, dataset, "peak_val", "min_val");
		}
		else if (statistic.equals(Statistic.MIN_1G)) {
			getStressMF(statement, dataset, "oneg_stress", "min_1g");
		}
		else if (statistic.equals(Statistic.MIN_INC)) {
			getStressMF(statement, dataset, "inc_stress", "min_inc");
		}
		else if (statistic.equals(Statistic.MIN_DP)) {
			getStressMF(statement, dataset, "dp_stress", "min_dp");
		}
		else if (statistic.equals(Statistic.MIN_DT)) {
			getStressMF(statement, dataset, "dt_stress", "min_dt");
		}
	}

	/**
	 * Generates statistics for number of peaks.
	 *
	 * @param statement
	 *            Database statement.
	 * @param dataset
	 *            Chart series.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void numberOfPeaks(Statement statement, DefaultCategoryDataset dataset) throws Exception {

		// update progress info
		updateMessage("Generating flight length statistics...");

		// set labels
		xAxisLabel_ = "Flight";
		yAxisLabel_ = "Number of peaks";
		title_ = Statistic.NUM_PEAKS.toString();

		// create query
		String sql = "select name, num_peaks from sth_flights where ";
		ArrayList<Flight> flights = input_.getFlights();
		for (int i = 0; i < flights.size(); i++) {
			sql += "flight_id = " + flights.get(i).getID() + (i == flights.size() - 1 ? "" : " or ");
		}
		sql += " order by num_peaks " + (input_.getOrder() ? "desc" : "asc");
		statement.setMaxRows(input_.getLimit());

		// execute query
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// add data to series
			while (resultSet.next()) {

				// get flight name and peaks
				String name = resultSet.getString("name");
				int peaks = resultSet.getInt("num_peaks");

				// add chart series
				dataset.addValue(peaks, "Statistics", name);
			}
		}

		// reset statement
		statement.setMaxRows(0);
	}

	/**
	 * Generates statistics for validity.
	 *
	 * @param statement
	 *            Database statement.
	 * @param dataset
	 *            Chart series.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void validity(Statement statement, DefaultCategoryDataset dataset) throws Exception {

		// update progress info
		updateMessage("Generating flight occurrence statistics...");

		// set labels
		xAxisLabel_ = "Flight";
		yAxisLabel_ = "Occurrence";
		title_ = Statistic.FLIGHT_OCCURRENCE.toString();

		// create query
		String sql = "select name, validity from sth_flights where ";
		ArrayList<Flight> flights = input_.getFlights();
		for (int i = 0; i < flights.size(); i++) {
			sql += "flight_id = " + flights.get(i).getID() + (i == flights.size() - 1 ? "" : " or ");
		}
		sql += " order by validity " + (input_.getOrder() ? "desc" : "asc");
		statement.setMaxRows(input_.getLimit());

		// execute query
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// add data to series
			while (resultSet.next()) {

				// get flight name and peaks
				String name = resultSet.getString("name");
				double peaks = resultSet.getDouble("validity");

				// add chart series
				dataset.addValue(peaks, "Statistics", name);
			}
		}

		// reset statement
		statement.setMaxRows(0);
	}

	/**
	 * Generates statistics for event occurrences for multiple flights.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param dataset
	 *            Series.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void eventOccurrenceMF(Connection connection, Statement statement, DefaultCategoryDataset dataset) throws Exception {

		// update progress info
		updateMessage("Generating event occurrence statistics...");

		// set labels
		xAxisLabel_ = "Flight";
		yAxisLabel_ = "Occurrence" + (input_.getValidityMultiplier() ? " (considering flight occurrences)" : "");
		title_ = "Occurrence of " + input_.getLoadcases().get(0).toString();

		// get stress sequence ID
		int sthID = input_.getFlights().get(0).getParentItem().getParentItem().getID();

		// create query
		String column = input_.getLoadcases().get(0).isOneg() ? "oneg_event" : "inc_event";
		String sql = "select sth_flights.name, sth_flights.validity, count(" + column + ")";
		sql += (input_.getValidityMultiplier() ? "*sth_flights.validity" : "") + " as statistic from sth_peaks_" + sthID;
		sql += " inner join sth_flights on sth_peaks_" + sthID + ".flight_id=sth_flights.flight_id where ";
		sql += "upper(" + column + ") like upper('%" + input_.getLoadcases().get(0).getEventName() + "%')" + " and (";
		ArrayList<Flight> flights = input_.getFlights();
		for (int i = 0; i < flights.size(); i++) {
			sql += "sth_peaks_" + sthID + ".flight_id = " + flights.get(i).getID() + (i == flights.size() - 1 ? ") " : " or ");
		}
		sql += "group by sth_flights.name, sth_flights.validity order by statistic " + (input_.getOrder() ? "desc" : "asc");
		statement.setMaxRows(input_.getLimit());

		// execute query
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// add data to series
			while (resultSet.next()) {
				String name = resultSet.getString("name");
				int occurrence = resultSet.getInt("statistic");
				dataset.addValue(occurrence, "Statistics", name);
			}
		}

		// reset statement
		statement.setMaxRows(0);
	}

	/**
	 * Generates statistics for maximum stress for multiple flights.
	 *
	 * @param statement
	 *            Database statement.
	 * @param dataset
	 *            Series.
	 * @param peakCol
	 *            Column name for STH peaks table.
	 * @param flightCol
	 *            Column name for STH flights table.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getStressMF(Statement statement, DefaultCategoryDataset dataset, String peakCol, String flightCol) throws Exception {

		// update progress info
		updateMessage("Generating stress comparison statistics...");

		// get title and criteria
		String title = input_.getStatistic().toString();
		boolean isMax = title.startsWith("Maximum");

		// at event
		if (!input_.getLoadcases().isEmpty()) {

			// set labels
			xAxisLabel_ = "Flight";
			yAxisLabel_ = "Stress";
			title_ = title + " at " + input_.getLoadcases().get(0).toString();

			// get stress sequence ID
			int sthID = input_.getFlights().get(0).getParentItem().getParentItem().getID();

			// create query
			String column = input_.getLoadcases().get(0).isOneg() ? "oneg_event" : "inc_event";
			peakCol = (isMax ? "max(" : "min(") + peakCol + ")";
			String sql = "select sth_flights.name, " + peakCol + " as statistic from sth_peaks_" + sthID;
			sql += " inner join sth_flights on sth_peaks_" + sthID + ".flight_id=sth_flights.flight_id where ";
			sql += "upper(" + column + ") like upper('%" + input_.getLoadcases().get(0).getEventName() + "%')" + " and (";
			ArrayList<Flight> flights = input_.getFlights();
			for (int i = 0; i < flights.size(); i++) {
				sql += "sth_peaks_" + sthID + ".flight_id = " + flights.get(i).getID() + (i == flights.size() - 1 ? ") " : " or ");
			}
			sql += "group by sth_flights.name order by statistic " + (input_.getOrder() ? "desc" : "asc");
			statement.setMaxRows(input_.getLimit());

			// execute query
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// add data to series
				while (resultSet.next()) {
					String name = resultSet.getString("name");
					double stress = resultSet.getDouble("statistic");
					dataset.addValue(stress, "Statistics", name);
				}
			}

			// reset statement
			statement.setMaxRows(0);
		}

		// all events
		else {

			// set labels
			xAxisLabel_ = "Flight";
			yAxisLabel_ = "Stress";
			title_ = title;

			// create query
			String sql = "select name, " + flightCol + " from sth_flights where ";
			ArrayList<Flight> flights = input_.getFlights();
			for (int i = 0; i < flights.size(); i++) {
				sql += "flight_id = " + flights.get(i).getID() + (i == flights.size() - 1 ? "" : " or ");
			}
			sql += " order by " + flightCol + " " + (input_.getOrder() ? "desc" : "asc");
			statement.setMaxRows(input_.getLimit());

			// execute query
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// add data to series
				while (resultSet.next()) {

					// get flight name and peaks
					String name = resultSet.getString("name");
					double peaks = resultSet.getDouble(flightCol);

					// add chart series
					dataset.addValue(peaks, "Statistics", name);
				}
			}

			// reset statement
			statement.setMaxRows(0);
		}
	}

	/**
	 * Generates statistics for single flight.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param dataset
	 *            Chart series.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void singleFlightStatistics(Connection connection, Statement statement, DefaultCategoryDataset dataset) throws Exception {

		// get statistic
		Statistic statistic = input_.getStatistic();

		// event occurrence
		if (statistic.equals(Statistic.LOADCASE_OCCURRENCE)) {
			eventOccurrenceSF(connection, statement, dataset);
		}
		else if (statistic.equals(Statistic.MAX_TOTAL)) {
			getStressSF(statement, dataset, "peak_val");
		}
		else if (statistic.equals(Statistic.MAX_1G)) {
			getStressSF(statement, dataset, "oneg_stress");
		}
		else if (statistic.equals(Statistic.MAX_INC)) {
			getStressSF(statement, dataset, "inc_stress");
		}
		else if (statistic.equals(Statistic.MAX_DP)) {
			getStressSF(statement, dataset, "dp_stress");
		}
		else if (statistic.equals(Statistic.MAX_DT)) {
			getStressSF(statement, dataset, "dt_stress");
		}
		else if (statistic.equals(Statistic.MIN_TOTAL)) {
			getStressSF(statement, dataset, "peak_val");
		}
		else if (statistic.equals(Statistic.MIN_1G)) {
			getStressSF(statement, dataset, "oneg_stress");
		}
		else if (statistic.equals(Statistic.MIN_INC)) {
			getStressSF(statement, dataset, "inc_stress");
		}
		else if (statistic.equals(Statistic.MIN_DP)) {
			getStressSF(statement, dataset, "dp_stress");
		}
		else if (statistic.equals(Statistic.MIN_DT)) {
			getStressSF(statement, dataset, "dt_stress");
		}
	}

	/**
	 * Generates statistics for maximum stress for multiple flights.
	 *
	 * @param statement
	 *            Database statement.
	 * @param dataset
	 *            Series.
	 * @param peakCol
	 *            Column name for STH peaks table.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getStressSF(Statement statement, DefaultCategoryDataset dataset, String peakCol) throws Exception {

		// update progress info
		updateMessage("Generating stress comparison statistics...");

		// set labels
		xAxisLabel_ = "Event";
		yAxisLabel_ = "Stress";
		title_ = input_.getStatistic().toString();
		boolean isMax = title_.startsWith("Maximum");

		// get stress sequence ID
		int sthID = input_.getFlights().get(0).getParentItem().getParentItem().getID();

		// create query
		String column = input_.getLoadcaseType() ? "oneg_event" : "inc_event";
		peakCol = (isMax ? "max(" : "min(") + peakCol + ")";
		String sql = "select " + column + ", " + peakCol + " as statistic from sth_peaks_" + sthID;
		sql += " where flight_id = " + input_.getFlights().get(0).getID();
		ArrayList<LoadcaseItem> loadcases = input_.getLoadcases();
		if (!loadcases.isEmpty()) {
			sql += " and (";
			for (int i = 0; i < loadcases.size(); i++) {
				sql += "upper(" + column + ") like upper('%" + loadcases.get(i).getEventName() + "%')" + (i == loadcases.size() - 1 ? ")" : " or ");
			}
		}
		sql += " group by " + column + " order by statistic " + (input_.getOrder() ? "desc" : "asc");
		statement.setMaxRows(input_.getLimit());

		// execute query
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// add data to series
			while (resultSet.next()) {
				String name = resultSet.getString(column);
				if (name == null) {
					continue;
				}
				double stress = resultSet.getDouble("statistic");
				dataset.addValue(stress, "Statistics", name);
			}
		}

		// reset statement
		statement.setMaxRows(0);
	}

	/**
	 * Generates statistics for event occurrences for single flight.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param dataset
	 *            Series.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void eventOccurrenceSF(Connection connection, Statement statement, DefaultCategoryDataset dataset) throws Exception {

		// update progress info
		updateMessage("Generating event occurrence statistics...");

		// set labels
		xAxisLabel_ = "Event";
		yAxisLabel_ = "Occurrence" + (input_.getValidityMultiplier() ? " (considering flight occurrences)" : "");
		title_ = "Event occurrences";

		// get stress sequence ID
		int sthID = input_.getFlights().get(0).getParentItem().getParentItem().getID();

		// create query for 1g events
		String column = input_.getLoadcaseType() ? "oneg_event" : "inc_event";
		String sql = "select " + column + ", sth_flights.validity, count(" + column + ")";
		sql += (input_.getValidityMultiplier() ? "*sth_flights.validity" : "") + " as statistic from sth_peaks_" + sthID;
		sql += " inner join sth_flights on sth_peaks_" + sthID + ".flight_id=sth_flights.flight_id where ";
		sql += "sth_peaks_" + sthID + ".flight_id = " + input_.getFlights().get(0).getID();
		ArrayList<LoadcaseItem> loadcases = input_.getLoadcases();
		if (!loadcases.isEmpty()) {
			sql += " and (";
			for (int i = 0; i < loadcases.size(); i++) {
				sql += "upper(" + column + ") like upper('%" + loadcases.get(i).getEventName() + "%')" + (i == loadcases.size() - 1 ? ")" : " or ");
			}
		}
		sql += " group by " + column + ", sth_flights.validity order by statistic " + (input_.getOrder() ? "desc" : "asc");
		statement.setMaxRows(input_.getLimit());

		// execute query
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// add data to series
			while (resultSet.next()) {
				String event = resultSet.getString(column);
				int occurrence = resultSet.getInt("statistic");
				dataset.addValue(occurrence, "Statistics", event);
			}
		}

		// reset statement
		statement.setMaxRows(0);
	}
}
