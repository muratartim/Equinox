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
import equinox.controller.LinkPilotPointsPopup;
import equinox.data.ui.PilotPointTableItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get pilot points task.
 *
 * @author Murat Artim
 * @date Aug 27, 2015
 * @time 10:12:42 PM
 */
public class GetPilotPoints extends InternalEquinoxTask<ArrayList<PilotPointTableItem>> implements ShortRunningTask {

	/** The calling panel. */
	private final LinkPilotPointsPopup panel_;

	/**
	 * Creates get pilot points task.
	 *
	 * @param panel
	 *            The calling panel.
	 */
	public GetPilotPoints(LinkPilotPointsPopup panel) {
		panel_ = panel;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get pilot points";
	}

	@Override
	protected ArrayList<PilotPointTableItem> call() throws Exception {

		// update progress info
		updateTitle("Getting pilot points from database");

		// initialize list
		ArrayList<PilotPointTableItem> pilotPoints = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get pilot points
				getPilotPoints(statement, pilotPoints);
			}
		}

		// return list
		return pilotPoints;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set results to panel
		try {
			panel_.setPilotPoints(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Retrieves and returns pilot points from database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param pilotPoints
	 *            Pilot points.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void getPilotPoints(Statement statement, ArrayList<PilotPointTableItem> pilotPoints) throws Exception {
		String sql = "select file_id, stf_files.name, stf_files.eid, cdf_sets.ac_program, cdf_sets.ac_section, cdf_sets.fat_mission from stf_files ";
		sql += "inner join cdf_sets on stf_files.cdf_id = cdf_sets.set_id order by stf_files.name";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				PilotPointTableItem item = new PilotPointTableItem(resultSet.getInt("file_id"));
				item.setStfname(resultSet.getString("name"));
				item.setProgram(resultSet.getString("ac_program"));
				item.setSection(resultSet.getString("ac_section"));
				item.setMission(resultSet.getString("fat_mission"));
				String eid = resultSet.getString("eid");
				item.setEid(eid == null ? "N/A" : eid);
				pilotPoints.add(item);
			}
		}
	}
}
