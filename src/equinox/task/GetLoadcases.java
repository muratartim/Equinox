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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.LoadcaseItem;
import equinox.data.fileType.Spectrum;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get loadcases task.
 *
 * @author Murat Artim
 * @date Apr 21, 2014
 * @time 11:50:26 PM
 */
public class GetLoadcases extends InternalEquinoxTask<ArrayList<LoadcaseItem>> implements ShortRunningTask {

	/** Loadcase type index. */
	public static final int ALL = 0, ONEG = 1, INCREMENT = 2;

	/** Requesting panel. */
	private final LoadcaseRequestingPanel panel_;

	/** Spectrum. */
	private final Spectrum spectrum_;

	/** Requested loadcase type. */
	private final int loadcaseType_;

	/**
	 * Creates get loadcases task.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param spectrum
	 *            Spectrum.
	 * @param loadcaseType
	 *            Requested loadcase type.
	 */
	public GetLoadcases(LoadcaseRequestingPanel panel, Spectrum spectrum, int loadcaseType) {
		panel_ = panel;
		spectrum_ = spectrum;
		loadcaseType_ = loadcaseType;
	}

	@Override
	public String getTaskTitle() {
		return "Get loadcases";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected ArrayList<LoadcaseItem> call() throws Exception {

		// update progress info
		updateTitle("Retrieving loadcases...");
		updateMessage("Please wait...");

		// initialize list
		ArrayList<LoadcaseItem> loadcases = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			getLoadcases(connection, loadcases);
		}

		// return list
		return loadcases;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set events
		try {
			panel_.setLoadcases(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Retrieves loadcases from the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param loadcases
	 *            List containing the loadcases.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getLoadcases(Connection connection, ArrayList<LoadcaseItem> loadcases) throws Exception {

		// get loadcases
		int txtID = spectrum_.getTXTFileID();
		String sql = "select distinct flight_phase, increment_num, issy_code from txt_codes where file_id = " + txtID;
		if (loadcaseType_ == ONEG) {
			sql += " and increment_num = 0";
		}
		else if (loadcaseType_ == INCREMENT) {
			sql += " and increment_num <> 0";
		}
		sql += " order by issy_code asc";
		try (Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					LoadcaseItem loadcase = new LoadcaseItem();
					loadcase.setLoadcaseNumber(resultSet.getString("issy_code"));
					loadcase.setEventName(resultSet.getString("flight_phase"));
					loadcase.setIsOneg(resultSet.getInt("increment_num") == 0);
					loadcases.add(loadcase);
				}
			}
		}

		// get comments
		int convID = spectrum_.getConversionTableID();
		sql = "select comment from xls_comments where file_id = " + convID + " and issy_code = ? and fue_translated like ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			for (LoadcaseItem loadcase : loadcases) {
				statement.setString(1, loadcase.getLoadcaseNumber());
				statement.setString(2, "%" + loadcase.getEventName() + "%");
				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						loadcase.setComments(resultSet.getString("comment"));
					}
				}
			}
		}
	}

	/**
	 * Interface for loadcase requesting panels.
	 *
	 * @author Murat Artim
	 * @date Apr 25, 2014
	 * @time 10:10:06 AM
	 */
	public interface LoadcaseRequestingPanel {

		/**
		 * Sets loadcases to this panel.
		 *
		 * @param loadcases
		 *            Loadcases to set.
		 */
		void setLoadcases(ArrayList<LoadcaseItem> loadcases);
	}
}
