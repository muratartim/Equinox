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
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.FileViewPanel;
import equinox.controller.InputPanel;
import equinox.data.fileType.Flight;
import equinox.data.fileType.StressSequence;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for select flight task.
 *
 * @author Murat Artim
 * @date Apr 19, 2014
 * @time 11:52:05 AM
 */
public class SelectFlight extends InternalEquinoxTask<Flight> implements ShortRunningTask {

	/** Selection criteria. */
	public static final int LONGEST_FLIGHT = 0, SHORTEST_FLIGHT = 1, MAX_VALIDITY = 2, MIN_VALIDITY = 3, MAX_1G = 4, MIN_1G = 5, MAX_INC = 6, MIN_INC = 7, MAX_DP = 8, MIN_DP = 9, MAX_DT = 10, MIN_DT = 11, MAX_TOTAL = 12, MIN_TOTAL = 13;

	/** Input STH file. */
	private final StressSequence file_;

	/** Selection criteria. */
	private final int criteria_;

	/**
	 * Creates select flight task.
	 *
	 * @param file
	 *            STH file.
	 * @param criteria
	 *            Selection criteria.
	 */
	public SelectFlight(StressSequence file, int criteria) {
		file_ = file;
		criteria_ = criteria;
	}

	@Override
	public String getTaskTitle() {
		return "Search flight within '" + file_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Flight call() throws Exception {

		// initialize flight
		Flight flight = null;

		// update progress info
		updateTitle("Searching flight within '" + file_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get longest flight
				if (criteria_ == LONGEST_FLIGHT) {
					flight = getFlight(statement, "num_peaks", true, flight);
				}
				else if (criteria_ == SHORTEST_FLIGHT) {
					flight = getFlight(statement, "num_peaks", false, flight);
				}
				else if (criteria_ == MAX_VALIDITY) {
					flight = getFlight(statement, "validity", true, flight);
				}
				else if (criteria_ == MIN_VALIDITY) {
					flight = getFlight(statement, "validity", false, flight);
				}
				else if (criteria_ == MAX_1G) {
					flight = getFlight(statement, "max_1g", true, flight);
				}
				else if (criteria_ == MIN_1G) {
					flight = getFlight(statement, "min_1g", false, flight);
				}
				else if (criteria_ == MAX_INC) {
					flight = getFlight(statement, "max_inc", true, flight);
				}
				else if (criteria_ == MIN_INC) {
					flight = getFlight(statement, "min_inc", false, flight);
				}
				else if (criteria_ == MAX_DP) {
					flight = getFlight(statement, "max_dp", true, flight);
				}
				else if (criteria_ == MIN_DP) {
					flight = getFlight(statement, "min_dp", false, flight);
				}
				else if (criteria_ == MAX_DT) {
					flight = getFlight(statement, "max_dt", true, flight);
				}
				else if (criteria_ == MIN_DT) {
					flight = getFlight(statement, "min_dt", false, flight);
				}
				else if (criteria_ == MAX_TOTAL) {
					flight = getFlight(statement, "max_val", true, flight);
				}
				else if (criteria_ == MIN_TOTAL) {
					flight = getFlight(statement, "min_val", false, flight);
				}

				// reset statement
				statement.setMaxRows(0);
			}
		}

		// return flight
		return flight;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// select file
		try {
			FileViewPanel panel = (FileViewPanel) taskPanel_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.FILE_VIEW_PANEL);
			panel.selectFile(get(), panel.getFileTreeRoot());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
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
	 * @param flight
	 *            Flight to return.
	 * @return The selected flight.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Flight getFlight(Statement statement, String colName, boolean isDesc, Flight flight) throws Exception {
		String sql = "select flight_id, name from sth_flights where file_id = " + file_.getID() + " order by " + colName + (isDesc ? " desc" : "");
		statement.setMaxRows(1);
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				flight = new Flight(resultSet.getString("name"), resultSet.getInt("flight_id"));
			}
		}
		return flight;
	}
}
