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
import equinox.data.MissionParameter;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get mission parameters task.
 *
 * @author Murat Artim
 * @date Nov 26, 2014
 * @time 11:19:43 AM
 */
public class GetMissionParameters extends InternalEquinoxTask<ArrayList<MissionParameter>> implements ShortRunningTask {

	/** Requesting panel. */
	private final MissionParameterRequestingPanel panel_;

	/** Spectrum item to get the parameters for. */
	private final SpectrumItem spectrumItem_;

	/**
	 * Creates get mission parameters task.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param spectrumItem
	 *            Spectrum item to get the parameters for.
	 */
	public GetMissionParameters(MissionParameterRequestingPanel panel, SpectrumItem spectrumItem) {
		panel_ = panel;
		spectrumItem_ = spectrumItem;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get mission parameters";
	}

	@Override
	protected ArrayList<MissionParameter> call() throws Exception {

		// update progress info
		updateTitle("Retrieving mission parameters...");
		updateMessage("Please wait...");

		// initialize list
		ArrayList<MissionParameter> parameters = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			try (Statement statement = connection.createStatement()) {
				getMissionParameters(statement, parameters);
			}
		}

		// return list
		return parameters;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set mission parameters
		try {
			panel_.setMissionParameters(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Gets the mission parameters from the database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param parameters
	 *            List containing the mission parameters.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getMissionParameters(Statement statement, ArrayList<MissionParameter> parameters) throws Exception {

		// spectrum
		if (spectrumItem_ instanceof Spectrum) {
			String sql = "select name, val from cdf_mission_parameters where cdf_id = " + spectrumItem_.getID() + " order by name";
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					parameters.add(new MissionParameter(resultSet.getString("name"), resultSet.getDouble("val")));
				}
			}
		}

		// external stress sequence
		else if (spectrumItem_ instanceof ExternalStressSequence) {
			String sql = "select name, val from ext_sth_mission_parameters where sth_id = " + spectrumItem_.getID() + " order by name";
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					parameters.add(new MissionParameter(resultSet.getString("name"), resultSet.getDouble("val")));
				}
			}
		}

		// other file
		else {

			// get STF file
			STFFile stfFile = getSTFFile(spectrumItem_);

			// no STF file found
			if (stfFile == null) {
				String msg = "Mission parameters cannot be retrieved for spectrum item '" + spectrumItem_.getName() + "'.";
				msg += " Only Spectra, STF Files or external stress sequences can have mission parameters.";
				throw new Exception(msg);
			}

			// get parameters of STF file
			boolean found = false;
			String sql = "select name, val from stf_mission_parameters where stf_id = " + stfFile.getID() + " order by name";
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					parameters.add(new MissionParameter(resultSet.getString("name"), resultSet.getDouble("val")));
					found = true;
				}
			}

			// no parameter found
			if (!found) {

				// get parameters of the owner spectrum
				sql = "select name, val from cdf_mission_parameters where cdf_id = " + stfFile.getParentItem().getID() + " order by name";
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						parameters.add(new MissionParameter(resultSet.getString("name"), resultSet.getDouble("val")));
					}
				}
			}
		}
	}

	/**
	 * Returns the STF file for the given spectrum item.
	 *
	 * @param item
	 *            Spectrum item.
	 * @return STF file for the given spectrum item.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static STFFile getSTFFile(SpectrumItem item) throws Exception {

		// null item
		if (item == null)
			return null;

		// STF file found
		if (item instanceof STFFile)
			return (STFFile) item;

		// search parent item
		return getSTFFile(item.getParentItem());
	}

	/**
	 * Interface for mission parameter requesting panels.
	 *
	 * @author Murat Artim
	 * @date Nov 26, 2014
	 * @time 11:21:56 AM
	 */
	public interface MissionParameterRequestingPanel {

		/**
		 * Sets mission parameters to this panel.
		 *
		 * @param parameters
		 *            Mission parameters.
		 */
		void setMissionParameters(ArrayList<MissionParameter> parameters);
	}
}
