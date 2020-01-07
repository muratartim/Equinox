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
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get mission parameter names task.
 *
 * @author Murat Artim
 * @date Nov 26, 2014
 * @time 12:51:38 PM
 */
public class GetMissionParameterNames extends InternalEquinoxTask<ArrayList<String>> implements ShortRunningTask {

	/** Requesting panel. */
	private final MissionParameterNamesRequestingPanel panel_;

	/** Spectrum items to get the parameter names for. */
	private final ArrayList<SpectrumItem> spectrumItems_;

	/**
	 * Creates get mission parameter names task for all defined mission parameters.
	 *
	 * @param panel
	 *            Requesting panel.
	 */
	public GetMissionParameterNames(MissionParameterNamesRequestingPanel panel) {
		panel_ = panel;
		spectrumItems_ = null;
	}

	/**
	 * Creates get mission parameter names task for mission parameters defined for given spectrum items.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param spectrumItems
	 *            Spectrum items to get the parameter names for.
	 */
	public GetMissionParameterNames(MissionParameterNamesRequestingPanel panel, ArrayList<SpectrumItem> spectrumItems) {
		panel_ = panel;
		spectrumItems_ = spectrumItems;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get mission parameter names";
	}

	@Override
	protected ArrayList<String> call() throws Exception {

		// update progress info
		updateTitle("Retrieving mission parameter names...");
		updateMessage("Please wait...");

		// initialize list
		ArrayList<String> names = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// get parameter names for all defined parameters
			if (spectrumItems_ == null) {
				getForAllDefinedMissionParameters(connection, names);
			}
			else {
				getForGivenItems(connection, names);
			}
		}

		// sort names
		Collections.sort(names);

		// return list
		return names;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set mission parameter names
		try {
			panel_.setMissionParameterNames(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Gets mission parameter names for all defined mission parameters.
	 *
	 * @param connection
	 *            Database connection.
	 * @param names
	 *            Names list.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void getForAllDefinedMissionParameters(Connection connection, ArrayList<String> names) throws Exception {

		// create statement
		try (Statement statement = connection.createStatement()) {

			// select all spectrum parameter names
			try (ResultSet resultSet = statement.executeQuery("select distinct name from cdf_mission_parameters order by name")) {
				while (resultSet.next()) {
					String name = resultSet.getString("name");
					if (!names.contains(name)) {
						names.add(name);
					}
				}
			}

			// select all STF file parameter names
			try (ResultSet resultSet = statement.executeQuery("select distinct name from stf_mission_parameters order by name")) {
				while (resultSet.next()) {
					String name = resultSet.getString("name");
					if (!names.contains(name)) {
						names.add(name);
					}
				}
			}

			// select all external stress sequence parameter names
			try (ResultSet resultSet = statement.executeQuery("select distinct name from ext_sth_mission_parameters order by name")) {
				while (resultSet.next()) {
					String name = resultSet.getString("name");
					if (!names.contains(name)) {
						names.add(name);
					}
				}
			}
		}
	}

	/**
	 * Gets mission parameter names for given spectrum items.
	 *
	 * @param connection
	 *            Database connection.
	 * @param names
	 *            Names list.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getForGivenItems(Connection connection, ArrayList<String> names) throws Exception {

		// prepare statement for getting mission parameter names of spectra
		String sql = "select name from cdf_mission_parameters where cdf_id = ? order by name";
		try (PreparedStatement getCDFMissionParameterNames = connection.prepareStatement(sql)) {

			// prepare statement for getting mission parameter names of STF files
			sql = "select name from stf_mission_parameters where stf_id = ? order by name";
			try (PreparedStatement getSTFMissionParameterNames = connection.prepareStatement(sql)) {

				// prepare statement for getting mission parameter names of external stress sequences
				sql = "select name from ext_sth_mission_parameters where sth_id = ? order by name";
				try (PreparedStatement getSTHMissionParameterNames = connection.prepareStatement(sql)) {

					// loop over spectrum items
					for (SpectrumItem item : spectrumItems_) {

						// spectrum
						if (item instanceof Spectrum) {
							processSpectrum((Spectrum) item, getCDFMissionParameterNames, names);
						}
						else if (item instanceof ExternalStressSequence) {
							processExternalStressSequence((ExternalStressSequence) item, getSTHMissionParameterNames, names);
						}
						else {

							// get STF file
							STFFile stfFile = getSTFFile(item);

							// no STF file found
							if (stfFile == null) {
								String msg = "Mission parameter names cannot be retrieved for spectrum item '" + item.getName() + "'.";
								msg += " Only Spectra, STF Files or external stress sequences can have mission parameters.";
								throw new Exception(msg);
							}

							// process STF file
							processSTFFile(stfFile, getSTFMissionParameterNames, getCDFMissionParameterNames, names);
						}
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
	 * Processes spectrum.
	 *
	 * @param spectrum
	 *            Spectrum.
	 * @param getCDFMissionParameterNames
	 *            Database statement.
	 * @param names
	 *            Names list.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void processSpectrum(Spectrum spectrum, PreparedStatement getCDFMissionParameterNames, ArrayList<String> names) throws Exception {
		getCDFMissionParameterNames.setInt(1, spectrum.getID());
		try (ResultSet resultSet = getCDFMissionParameterNames.executeQuery()) {
			while (resultSet.next()) {
				String name = resultSet.getString("name");
				if (!names.contains(name)) {
					names.add(name);
				}
			}
		}
	}

	/**
	 * Processes STF file.
	 *
	 * @param stfFile
	 *            STF file.
	 * @param getSTFMissionParameterNames
	 *            Database statement.
	 * @param getCDFMissionParameterNames
	 *            Database statement.
	 * @param names
	 *            Names list.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void processSTFFile(STFFile stfFile, PreparedStatement getSTFMissionParameterNames, PreparedStatement getCDFMissionParameterNames, ArrayList<String> names) throws Exception {

		// get mission parameter names of STF file
		boolean found = false;
		getSTFMissionParameterNames.setInt(1, stfFile.getID());
		try (ResultSet resultSet = getSTFMissionParameterNames.executeQuery()) {
			while (resultSet.next()) {
				String name = resultSet.getString("name");
				if (!names.contains(name)) {
					names.add(name);
				}
				found = true;
			}
		}

		// nothing found
		if (!found) {

			// get mission parameter names of owner spectrum
			getCDFMissionParameterNames.setInt(1, stfFile.getParentItem().getID());
			try (ResultSet resultSet = getCDFMissionParameterNames.executeQuery()) {
				while (resultSet.next()) {
					String name = resultSet.getString("name");
					if (!names.contains(name)) {
						names.add(name);
					}
				}
			}
		}
	}

	/**
	 * Processes external stress sequence.
	 *
	 * @param externalStressSequence
	 *            External stress sequence.
	 * @param getSTHMissionParameterNames
	 *            Database statement.
	 * @param names
	 *            Names list.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void processExternalStressSequence(ExternalStressSequence externalStressSequence, PreparedStatement getSTHMissionParameterNames, ArrayList<String> names) throws Exception {
		getSTHMissionParameterNames.setInt(1, externalStressSequence.getID());
		try (ResultSet resultSet = getSTHMissionParameterNames.executeQuery()) {
			while (resultSet.next()) {
				String name = resultSet.getString("name");
				if (!names.contains(name)) {
					names.add(name);
				}
			}
		}
	}

	/**
	 * Interface for mission parameter names requesting panels.
	 *
	 * @author Murat Artim
	 * @date Nov 26, 2014
	 * @time 11:21:56 AM
	 */
	public interface MissionParameterNamesRequestingPanel {

		/**
		 * Sets mission parameter names to this panel.
		 *
		 * @param names
		 *            Mission parameter names.
		 */
		void setMissionParameterNames(ArrayList<String> names);
	}
}
