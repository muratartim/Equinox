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
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get missions task.
 *
 * @author Murat Artim
 * @date Sep 10, 2015
 * @time 4:55:38 PM
 */
public class GetMissions extends InternalEquinoxTask<ArrayList<String>> implements ShortRunningTask {

	/** Requesting panel. */
	private final MissionRequestingPanel panel_;

	/** Aircraft equivalent stress. */
	private final AircraftFatigueEquivalentStress stress_;

	/**
	 * Creates get missions task.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param stress
	 *            Aircraft equivalent stress.
	 */
	public GetMissions(MissionRequestingPanel panel, AircraftFatigueEquivalentStress stress) {
		panel_ = panel;
		stress_ = stress;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get fatigue missions";
	}

	@Override
	protected ArrayList<String> call() throws Exception {

		// update progress info
		updateTitle("Retrieving fatigue missions...");
		updateMessage("Please wait...");

		// initialize list
		ArrayList<String> missions = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery("select distinct mission from ac_eq_stresses_" + stress_.getID() + " where name = '" + stress_.getName() + "'")) {
					while (resultSet.next()) {
						missions.add(resultSet.getString("mission"));
					}
				}
			}
		}

		// return list
		return missions;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set angles
		try {
			panel_.setMissions(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for mission requesting panels.
	 *
	 * @author Murat Artim
	 * @date Nov 28, 2014
	 * @time 2:18:37 PM
	 */
	public interface MissionRequestingPanel {

		/**
		 * Sets fatigue missions to this panel.
		 *
		 * @param missions
		 *            Fatigue missions.
		 */
		void setMissions(ArrayList<String> missions);
	}
}
