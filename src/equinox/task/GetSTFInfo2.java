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
import equinox.data.fileType.STFFile;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get STF info task. This used for displaying STF info on edit STF info and Export STF panels.
 *
 * @author Murat Artim
 * @date Feb 2, 2016
 * @time 4:15:54 PM
 */
public class GetSTFInfo2 extends InternalEquinoxTask<String[]> implements ShortRunningTask {

	/** Info index. */
	public static final int DESCRIPTION = 0, ELEMENT_TYPE = 1, FRAME_RIB_POS = 2, STRINGER_POS = 3, DATA_SOURCE = 4, GEN_SOURCE = 5, DELIVERY_REF = 6, ISSUE = 7, EID = 8, FATIGUE_MATERIAL = 9, PREFFAS_MATERIAL = 10, LINEAR_MATERIAL = 11;

	/** STF file. */
	private final STFFile stfFile_;

	/** Requesting panel. */
	private final STFInfoRequestingPanel panel_;

	/**
	 * Creates get STF info task.
	 *
	 * @param stfFile
	 *            STF file.
	 * @param panel
	 *            Requesting panel.
	 */
	public GetSTFInfo2(STFFile stfFile, STFInfoRequestingPanel panel) {
		stfFile_ = stfFile;
		panel_ = panel;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get STF info for '" + stfFile_.getName() + "'";
	}

	@Override
	protected String[] call() throws Exception {

		// update progress info
		updateMessage("Getting STF info from database");

		// create info list
		String[] info = new String[12];

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get info
				String sql = "select description, element_type, frame_rib_position, stringer_position, data_source, ";
				sql += "generation_source, delivery_ref_num, issue, eid, fatigue_material, preffas_material, linear_material from stf_files where file_id = " + stfFile_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						info[DESCRIPTION] = resultSet.getString("description");
						info[ELEMENT_TYPE] = resultSet.getString("element_type");
						info[FRAME_RIB_POS] = resultSet.getString("frame_rib_position");
						info[STRINGER_POS] = resultSet.getString("stringer_position");
						info[DATA_SOURCE] = resultSet.getString("data_source");
						info[GEN_SOURCE] = resultSet.getString("generation_source");
						info[DELIVERY_REF] = resultSet.getString("delivery_ref_num");
						info[ISSUE] = resultSet.getString("issue");
						info[EID] = resultSet.getString("eid");
						info[FATIGUE_MATERIAL] = resultSet.getString("fatigue_material");
						info[PREFFAS_MATERIAL] = resultSet.getString("preffas_material");
						info[LINEAR_MATERIAL] = resultSet.getString("linear_material");
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
			panel_.setSTFInfo(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for STF info requesting panels.
	 *
	 * @author Murat Artim
	 * @date Feb 2, 2016
	 * @time 4:18:25 PM
	 */
	public interface STFInfoRequestingPanel {

		/**
		 * Sets STF info to this panel.
		 *
		 * @param info
		 *            STF info to set.
		 */
		void setSTFInfo(String[] info);
	}
}
