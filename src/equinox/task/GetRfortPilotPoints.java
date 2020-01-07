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
import equinox.data.fileType.Rfort;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get RFORT pilot points task.
 *
 * @author Murat Artim
 * @date Apr 19, 2016
 * @time 1:47:25 PM
 */
public class GetRfortPilotPoints extends InternalEquinoxTask<ArrayList<String>> implements ShortRunningTask {

	/** RFORT file. */
	private final Rfort rfort_;

	/** Requesting panel. */
	private final RfortPilotPointsRequestingPanel panel_;

	/** True if only the pilot points that are included in RFORT omission should be returned. */
	private final boolean includedInRfort_;

	/**
	 * Creates get RFORT pilot points task.
	 *
	 * @param rfort
	 *            RFORT file to get pilot points.
	 * @param panel
	 *            Requesting panel.
	 * @param includedInRfort
	 *            True if only the pilot points that are included in RFORT omission should be returned.
	 */
	public GetRfortPilotPoints(Rfort rfort, RfortPilotPointsRequestingPanel panel, boolean includedInRfort) {
		rfort_ = rfort;
		panel_ = panel;
		includedInRfort_ = includedInRfort;
	}

	@Override
	public String getTaskTitle() {
		return "Get RFORT pilot points for '" + rfort_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected ArrayList<String> call() throws Exception {

		// update info
		updateMessage("Retrieving RFORT pilot points for '" + rfort_.getName() + "'...");

		// create list
		ArrayList<String> ppNames = new ArrayList<>();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get pilot points
				String sql = "select distinct pp_name, included_in_rfort from rfort_outputs where analysis_id = " + rfort_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {

						// only RFORT ones
						if (includedInRfort_) {
							if (resultSet.getBoolean("included_in_rfort")) {
								ppNames.add(resultSet.getString("pp_name"));
							}
						}
						else {
							ppNames.add(resultSet.getString("pp_name"));
						}
					}

				}
			}
		}

		// return pilot points
		return ppNames;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {
			panel_.setPilotPoints(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for RFORT pilot point requesting panel.
	 *
	 * @author Murat Artim
	 * @date Apr 19, 2016
	 * @time 2:51:26 PM
	 */
	public interface RfortPilotPointsRequestingPanel {

		/**
		 * Sets RFORT pilot points.
		 *
		 * @param ppNames
		 *            Pilot point names.
		 */
		void setPilotPoints(ArrayList<String> ppNames);
	}
}
