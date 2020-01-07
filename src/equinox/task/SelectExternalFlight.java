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
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.FileViewPanel;
import equinox.controller.InputPanel;
import equinox.data.EmbeddedTask;
import equinox.data.fileType.ExternalFlight;
import equinox.data.fileType.ExternalStressSequence;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for select external flight task.
 *
 * @author Murat Artim
 * @date Mar 13, 2015
 * @time 3:44:47 PM
 */
public class SelectExternalFlight extends InternalEquinoxTask<ExternalFlight> implements ShortRunningTask, SingleInputTask<ExternalStressSequence>, AutomaticTaskOwner<ExternalFlight> {

	/** Selection criteria. */
	public static final int LONGEST_FLIGHT = 0, SHORTEST_FLIGHT = 1, MAX_VALIDITY = 2, MIN_VALIDITY = 3, MAX_PEAK = 4, MIN_PEAK = 5;

	/** External stress sequence. */
	private ExternalStressSequence file_;

	/** Selection criteria. */
	private final int criteria_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<ExternalFlight>> automaticTasks_ = null;

	/**
	 * Creates select external flight task.
	 *
	 * @param file
	 *            External stress sequence. Can be null for automatic execution.
	 * @param criteria
	 *            Selection criteria.
	 */
	public SelectExternalFlight(ExternalStressSequence file, int criteria) {
		file_ = file;
		criteria_ = criteria;
	}

	@Override
	public void setAutomaticInput(ExternalStressSequence input) {
		file_ = input;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<ExternalFlight> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<ExternalFlight>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public String getTaskTitle() {
		return "Search external flight";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected ExternalFlight call() throws Exception {

		// initialize flight
		ExternalFlight flight = null;

		// update progress info
		updateTitle("Searching external flight within '" + file_.getName() + "'");

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
				else if (criteria_ == MAX_PEAK) {
					flight = getFlight(statement, "max_val", true, flight);
				}
				else if (criteria_ == MIN_PEAK) {
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

			// get flight
			ExternalFlight flight = get();

			// user initiated task
			if (automaticTasks_ == null) {

				// select flight
				FileViewPanel panel = (FileViewPanel) taskPanel_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.FILE_VIEW_PANEL);
				panel.selectFile(flight, panel.getFileTreeRoot());
			}

			// automatic task
			else {
				automaticTaskOwnerSucceeded(flight, automaticTasks_, taskPanel_);
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
	private ExternalFlight getFlight(Statement statement, String colName, boolean isDesc, ExternalFlight flight) throws Exception {
		String sql = "select flight_id, name from ext_sth_flights where file_id = " + file_.getID() + " order by " + colName + (isDesc ? " desc" : "");
		statement.setMaxRows(1);
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				flight = new ExternalFlight(resultSet.getString("name"), resultSet.getInt("flight_id"));
			}
		}
		return flight;
	}
}
