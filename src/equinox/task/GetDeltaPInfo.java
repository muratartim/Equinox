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

import equinox.Equinox;
import equinox.data.fileType.Spectrum;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get delta-p info task.
 *
 * @author Murat Artim
 * @date Jun 14, 2016
 * @time 11:33:33 AM
 */
public class GetDeltaPInfo extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** Requesting panel. */
	private final DeltaPInfoRequestingPanel panel_;

	/** Spectrum. */
	private final Spectrum spectrum_;

	/** Reference delta-p value. */
	private Double refDP_ = null;

	/** Delta-p loadcase. */
	private String dpLoadcase_ = null;

	/**
	 * Creates get delta-p info task.
	 *
	 * @param spectrum
	 *            Spectrum to get the info for.
	 * @param panel
	 *            Requesting panel.
	 */
	public GetDeltaPInfo(Spectrum spectrum, DeltaPInfoRequestingPanel panel) {
		spectrum_ = spectrum;
		panel_ = panel;
	}

	@Override
	public String getTaskTitle() {
		return "Get delta-p info";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Retrieving delta-p info...");
		updateMessage("Please wait...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get reference delta-p
				refDP_ = getRefDP(statement);

				// get delta-p loadcase
				dpLoadcase_ = getDPLoadcase(statement);
			}
		}

		// return
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set delta-p info to panel
		panel_.setDeltaPInfo(dpLoadcase_, refDP_);
	}

	/**
	 * Retrieves and returns delta-p pressure value.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Delta-p pressure value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Double getRefDP(Statement statement) throws Exception {

		// initialize reference pressure
		Double refPressure = null;

		// get reference pressure from conversion table
		String sql = "select ref_dp from xls_files where file_id = " + spectrum_.getConversionTableID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				refPressure = resultSet.getDouble("ref_dp");
			}
		}

		// reference pressure is zero
		if ((refPressure == null) || (refPressure == 0.0)) {

			// get maximum pressure from ANA file
			sql = "select max_dp from ana_flights where file_id = " + spectrum_.getANAFileID() + " order by max_dp desc";
			statement.setMaxRows(1);
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					refPressure = resultSet.getDouble("max_dp");
				}
			}
			statement.setMaxRows(0);
		}

		// return reference pressure
		return refPressure;
	}

	/**
	 * Retrieves and returns delta-p loadcase.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Delta-p loadcase.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getDPLoadcase(Statement statement) throws Exception {

		// initialize loadcase
		String dpLoadcase = null;

		// create and execute statement
		String sql = "select issy_code from txt_codes where file_id = " + spectrum_.getTXTFileID() + " and dp_case = 1";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				dpLoadcase = resultSet.getString("issy_code");
			}
		}

		// return delta-p loadcase
		return dpLoadcase;
	}

	/**
	 * Interface for delta-p info requesting panels.
	 *
	 * @author Murat Artim
	 * @date Apr 25, 2014
	 * @time 10:10:06 AM
	 */
	public interface DeltaPInfoRequestingPanel {

		/**
		 * Sets delta-p info to this panel.
		 *
		 * @param dpLoadcase
		 *            Delta-p loadcase to set.
		 * @param refDP
		 *            Reference delta-p pressure value.
		 */
		void setDeltaPInfo(String dpLoadcase, Double refDP);
	}
}
