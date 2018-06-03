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
import equinox.data.fileType.Spectrum;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get spectrum edit info task.
 *
 * @author Murat Artim
 * @date Feb 3, 2016
 * @time 11:54:59 AM
 */
public class GetSpectrumEditInfo extends InternalEquinoxTask<String[]> implements ShortRunningTask {

	/** Info index. */
	public static final int PROGRAM = 0, SECTION = 1, MISSION = 2, MISSION_ISSUE = 3, FLP_ISSUE = 4, IFLP_ISSUE = 5, CDF_ISSUE = 6, DELIVERY_REF = 7, DESCRIPTION = 8;

	/** Spectrum. */
	private final Spectrum spectrum_;

	/** Requesting panel. */
	private final SpectrumInfoRequestingPanel panel_;

	/**
	 * Creates get spectrum edit info task.
	 *
	 * @param spectrum
	 *            Spectrum.
	 * @param panel
	 *            Requesting panel.
	 */
	public GetSpectrumEditInfo(Spectrum spectrum, SpectrumInfoRequestingPanel panel) {
		spectrum_ = spectrum;
		panel_ = panel;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get spectrum info for '" + spectrum_.getName() + "'";
	}

	@Override
	protected String[] call() throws Exception {

		// update progress info
		updateMessage("Getting spectrum info from database");

		// create info list
		String[] info = new String[9];

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get info
				String sql = "select ac_program, ac_section, fat_mission, fat_mission_issue, flp_issue, iflp_issue, cdf_issue, ";
				sql += "delivery_ref, description from cdf_sets where set_id = " + spectrum_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						info[PROGRAM] = resultSet.getString("ac_program");
						info[SECTION] = resultSet.getString("ac_section");
						info[MISSION] = resultSet.getString("fat_mission");
						info[MISSION_ISSUE] = resultSet.getString("fat_mission_issue");
						info[FLP_ISSUE] = resultSet.getString("flp_issue");
						info[IFLP_ISSUE] = resultSet.getString("iflp_issue");
						info[CDF_ISSUE] = resultSet.getString("cdf_issue");
						info[DELIVERY_REF] = resultSet.getString("delivery_ref");
						info[DESCRIPTION] = resultSet.getString("description");
					}
				}
			}
		}

		// return info
		return info;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {
			panel_.setSpectrumInfo(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for spectrum info requesting panels.
	 *
	 * @author Murat Artim
	 * @date Feb 2, 2016
	 * @time 4:18:25 PM
	 */
	public interface SpectrumInfoRequestingPanel {

		/**
		 * Sets spectrum info to this panel.
		 *
		 * @param info
		 *            Spectrum info to set.
		 */
		void setSpectrumInfo(String[] info);
	}
}