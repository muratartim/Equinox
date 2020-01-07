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

import equinox.Equinox;
import equinox.controller.RfortReportPanel;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.Flight;
import equinox.data.fileType.Rfort;
import equinox.data.ui.RfortOmission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.Utility;
import javafx.scene.control.TreeItem;

/**
 * Class for get RFORT typical flights task.
 *
 * @author Murat Artim
 * @date Apr 21, 2016
 * @time 12:09:38 PM
 */
public class GetRfortTypicalFlights extends InternalEquinoxTask<ArrayList<String>> implements ShortRunningTask {

	/** RFORT file. */
	private final Rfort rfort_;

	/** Requesting panel. */
	private final RfortReportPanel panel_;

	/**
	 * Creates get RFORT typical flights task.
	 *
	 * @param rfort
	 *            RFORT file to get pilot points.
	 * @param panel
	 *            Requesting panel.
	 */
	public GetRfortTypicalFlights(Rfort rfort, RfortReportPanel panel) {
		rfort_ = rfort;
		panel_ = panel;
	}

	@Override
	public String getTaskTitle() {
		return "Get RFORT typical flights for '" + rfort_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected ArrayList<String> call() throws Exception {

		// update info
		updateMessage("Retrieving RFORT typical flights for '" + rfort_.getName() + "'...");

		// create list
		ArrayList<String> flightNames = new ArrayList<>();

		// get file tree root
		TreeItem<String> root = taskPanel_.getOwner().getOwner().getInputPanel().getFileTreeRoot();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// get stress ID of 1 pilot point
			int stressID = -1;
			try (Statement statement = connection.createStatement()) {
				statement.setMaxRows(1);
				String sql = "select stress_id from rfort_outputs where analysis_id = " + rfort_.getID();
				sql += " and stress_type = '" + SaveRfortInfo.FATIGUE + "' and omission_name = '" + RfortOmission.INITIAL_ANALYSIS + "'";
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						stressID = resultSet.getInt("stress_id");
					}
				}
				statement.setMaxRows(0);
			}

			// no stress ID found
			if (stressID == -1)
				throw new Exception("Cannot retrieve stress ID from RFORT outputs to get typical flight names.");

			// get fatigue equivalent stress
			FatigueEquivalentStress stress = (FatigueEquivalentStress) Utility.searchFileTree(root, stressID, FatigueEquivalentStress.class);

			// no equivalent stress found
			if (stress == null)
				throw new Exception("Cannot retrieve stress ID from RFORT outputs to get typical flight names.");

			// get flights
			ArrayList<Flight> flights = stress.getParentItem().getFlights().getFlights();
			for (Flight flight : flights) {
				flightNames.add(flight.getName());
			}
		}

		// return flight names
		return flightNames;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {
			panel_.setTypicalFlights(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
