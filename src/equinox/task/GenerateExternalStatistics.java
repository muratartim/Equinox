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
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.Equinox;
import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.Pair;
import equinox.data.StatisticsPlotAttributes;
import equinox.data.fileType.ExternalFlight;
import equinox.data.input.ExternalStatisticsInput;
import equinox.data.input.ExternalStatisticsInput.ExternalStatistic;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for generate external statistics task.
 *
 * @author Murat Artim
 * @date Apr 7, 2016
 * @time 9:19:54 AM
 */
public class GenerateExternalStatistics extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask, SingleInputTask<List<ExternalFlight>>, AutomaticTaskOwner<Pair<CategoryDataset, StatisticsPlotAttributes>> {

	/** Automatic inputs. */
	private final List<ExternalFlight> flights_;

	/** Input. */
	private final ExternalStatisticsInput input_;

	/** Chart labels. */
	private String xAxisLabel_, yAxisLabel_, title_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Pair<CategoryDataset, StatisticsPlotAttributes>>> automaticTasks_ = null;

	/**
	 * Creates generate external statistics task.
	 *
	 * @param input
	 *            Input.
	 */
	public GenerateExternalStatistics(ExternalStatisticsInput input) {
		input_ = input;
		flights_ = new ArrayList<>();
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
	public void setAutomaticInput(List<ExternalFlight> input) {
		flights_.addAll(input);
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Pair<CategoryDataset, StatisticsPlotAttributes>> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Pair<CategoryDataset, StatisticsPlotAttributes>>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public String getTaskTitle() {
		return "Generate external flight statistics";
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
		updateTitle("Generating external flight statistics...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {
				generateFlightStatistics(connection, statement, dataset);
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

			// user initiated task
			if (automaticTasks_ == null) {

				// get column plot panel
				StatisticsViewPanel panel = (StatisticsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);

				// set chart data to panel
				panel.setPlotData(dataset, title_, null, xAxisLabel_, yAxisLabel_, false, input_.getLabelDisplay(), false);

				// show column chart plot panel
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
			}

			// automatic task
			else {

				// create plot attributes
				StatisticsPlotAttributes plotAttributes = new StatisticsPlotAttributes();
				plotAttributes.setLabelsVisible(input_.getLabelDisplay());
				plotAttributes.setLayered(false);
				plotAttributes.setLegendVisible(false);
				plotAttributes.setSubTitle(null);
				plotAttributes.setTitle(title_);
				plotAttributes.setXAxisLabel(xAxisLabel_);
				plotAttributes.setYAxisLabel(yAxisLabel_);

				// manage automatic tasks
				automaticTaskOwnerSucceeded(new Pair<>(dataset, plotAttributes), automaticTasks_, taskPanel_);
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
	 * Generates statistics for input flights.
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
	private void generateFlightStatistics(Connection connection, Statement statement, DefaultCategoryDataset dataset) throws Exception {

		// get statistic
		ExternalStatistic statistic = input_.getStatistic();

		// number of peaks
		if (statistic.equals(ExternalStatistic.NUM_PEAKS)) {
			numberOfPeaks(statement, dataset);
		}
		else if (statistic.equals(ExternalStatistic.FLIGHT_OCCURRENCE)) {
			validity(statement, dataset);
		}
		else if (statistic.equals(ExternalStatistic.MAX_PEAK)) {
			getStress(statement, dataset, "peak_val", "max_val");
		}
		else if (statistic.equals(ExternalStatistic.MIN_PEAK)) {
			getStress(statement, dataset, "peak_val", "min_val");
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
		title_ = ExternalStatistic.NUM_PEAKS.toString();

		// create query
		String sql = "select name, num_peaks from ext_sth_flights where ";
		for (int i = 0; i < flights_.size(); i++) {
			sql += "flight_id = " + flights_.get(i).getID() + (i == flights_.size() - 1 ? "" : " or ");
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
		title_ = ExternalStatistic.FLIGHT_OCCURRENCE.toString();

		// create query
		String sql = "select name, validity from ext_sth_flights where ";
		for (int i = 0; i < flights_.size(); i++) {
			sql += "flight_id = " + flights_.get(i).getID() + (i == flights_.size() - 1 ? "" : " or ");
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
	 * Generates statistics for stress for multiple flights.
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
	private void getStress(Statement statement, DefaultCategoryDataset dataset, String peakCol, String flightCol) throws Exception {

		// update progress info
		updateMessage("Generating stress comparison statistics...");

		// set labels
		xAxisLabel_ = "Flight";
		yAxisLabel_ = "Stress";
		title_ = input_.getStatistic().toString();

		// create query
		String sql = "select name, " + flightCol + " from ext_sth_flights where ";
		for (int i = 0; i < flights_.size(); i++) {
			sql += "flight_id = " + flights_.get(i).getID() + (i == flights_.size() - 1 ? "" : " or ");
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
