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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import equinox.Equinox;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.fileType.AircraftModel;
import equinox.data.fileType.DamageAngle;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.FlightDamageContributions;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.data.fileType.PilotPoint;
import equinox.data.fileType.PilotPoints;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.Rfort;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.fileType.StressSequence;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.SingleInputTask;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

/**
 * Class for delete files task.
 *
 * @author Murat Artim
 * @param <V>
 *            Type of file to delete.
 * @date Dec 12, 2013
 * @time 6:12:33 PM
 */
public class DeleteFiles<V extends SpectrumItem> extends InternalEquinoxTask<Void> implements LongRunningTask, SingleInputTask<V> {

	/** File items to delete. */
	private SpectrumItem[] files_;

	/**
	 * Creates delete files task.
	 *
	 * @param files
	 *            Files to delete. Can be null for automatic execution.
	 */
	public DeleteFiles(ObservableList<TreeItem<String>> files) {
		if (files != null) {
			files_ = new SpectrumItem[files.size()];
			for (int i = 0; i < files.size(); i++) {
				files_[i] = (SpectrumItem) files.get(i);
			}
		}
	}

	@Override
	public String getTaskTitle() {
		return "Delete files";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public void setAutomaticInput(V input) {
		files_ = new SpectrumItem[1];
		files_[0] = input;
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.DELETE_FILE);

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// create statement
				try (Statement statement = connection.createStatement()) {

					// loop over files
					int numFiles = files_.length;
					for (int i = 0; i < numFiles; i++) {

						// task cancelled
						if (isCancelled()) {
							connection.rollback();
							connection.setAutoCommit(true);
							return null;
						}

						// update progress info
						updateProgress(i, numFiles);
						updateTitle("Deleting file '" + files_[i].getName() + "'");

						// delete file
						deleteFile(connection, statement, files_[i]);
					}
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				if (connection != null) {
					connection.rollback();
					connection.setAutoCommit(true);
				}

				// propagate exception
				throw e;
			}
		}
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// remove files from the file tree
		Platform.runLater(() -> {

			// clear file selection
			taskPanel_.getOwner().getOwner().getInputPanel().clearFileSelection();

			// create list to store parents
			ArrayList<TreeItem<String>> parents = new ArrayList<>();

			// loop over files
			for (SpectrumItem file : files_) {

				// delete pilot point links
				if (file instanceof STFFile || file instanceof Spectrum) {
					deleteLinkedPilotPointsFromFileTree(file);
				}

				// get parent
				TreeItem<String> parent = file.getParent();

				// already contained in parents list
				if (parent == null || parents.contains(parent)) {
					continue;
				}

				// get list of to-be-removed items
				ArrayList<TreeItem<String>> toBeRemoved = new ArrayList<>();
				for (SpectrumItem file2 : files_) {
					TreeItem<String> parent2 = file2.getParent();
					if (parent2 != null || parent.equals(parent2)) {
						toBeRemoved.add(file2);
					}
				}

				// add to parents list
				parents.add(parent);

				// remove to-be-removed items
				parent.getChildren().removeAll(toBeRemoved);
			}
		});
	}

	/**
	 * Deletes linked pilot points from file tree. This method is called only if STF or Spectrum files are deleted.
	 *
	 * @param file
	 *            File to be deleted.
	 */
	private void deleteLinkedPilotPointsFromFileTree(SpectrumItem file) {

		// create list to store STF file IDs
		ArrayList<Integer> stfFileIDs = new ArrayList<>();

		// STF file
		if (file instanceof STFFile) {
			stfFileIDs.add(file.getID());
		}
		else if (file instanceof Spectrum) {
			ArrayList<STFFile> files = ((Spectrum) file).getSTFFiles();
			if (files != null) {
				for (STFFile file1 : files) {
					stfFileIDs.add(file1.getID());
				}
			}
		}

		// no STF file
		if (stfFileIDs.isEmpty())
			return;

		// initialize to-be-removed array list
		ArrayList<PilotPoint> toBeRemoved = new ArrayList<>();

		// get file tree root
		TreeItem<String> root = taskPanel_.getOwner().getOwner().getInputPanel().getFileTreeRoot();

		// loop over children
		for (TreeItem<String> item : root.getChildren())
			// aircraft model
			if (item instanceof AircraftModel) {

				// get pilot points folder
				PilotPoints folder = ((AircraftModel) item).getPilotPoints();

				// get pilot points
				ArrayList<PilotPoint> pps = folder.getPilotPoints();

				// no pilot points
				if (pps == null || pps.isEmpty()) {
					continue;
				}

				// reset to-be-removed list
				toBeRemoved.clear();

				// loop over pilot points
				for (PilotPoint pp : pps)
					if (stfFileIDs.contains(pp.getSTFFileID())) {
						toBeRemoved.add(pp);
					}

				// nothing to be removed
				if (toBeRemoved.isEmpty()) {
					continue;
				}

				// remove pilot points from folder
				folder.getChildren().removeAll(toBeRemoved);
			}
	}

	/**
	 * Removes input file from database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param file
	 *            File to delete.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void deleteFile(Connection connection, Statement statement, SpectrumItem file) throws Exception {

		// task cancelled
		if (isCancelled())
			return;

		// stress sequence
		if (file instanceof StressSequence) {
			removeStressSequence((StressSequence) file, connection, statement);
		}
		else if (file instanceof FatigueEquivalentStress) {
			removeFatigueEquivalentStress((FatigueEquivalentStress) file, statement);
		}
		else if (file instanceof PreffasEquivalentStress) {
			removePreffasEquivalentStress((PreffasEquivalentStress) file, statement);
		}
		else if (file instanceof LinearEquivalentStress) {
			removeLinearEquivalentStress((LinearEquivalentStress) file, statement);
		}
		else if (file instanceof FastFatigueEquivalentStress) {
			removeFastFatigueEquivalentStress((FastFatigueEquivalentStress) file, statement);
		}
		else if (file instanceof FastPreffasEquivalentStress) {
			removeFastPreffasEquivalentStress((FastPreffasEquivalentStress) file, statement);
		}
		else if (file instanceof FastLinearEquivalentStress) {
			removeFastLinearEquivalentStress((FastLinearEquivalentStress) file, statement);
		}
		else if (file instanceof DamageAngle) {
			removeDamageAngle((DamageAngle) file, statement);
		}
		else if (file instanceof LoadcaseDamageContributions) {
			removeDamageContribution((LoadcaseDamageContributions) file, statement);
		}
		else if (file instanceof FlightDamageContributions) {
			removeFlightDamageContribution((FlightDamageContributions) file, statement);
		}
		else if (file instanceof STFFile) {
			removeSTFFile((STFFile) file, connection, statement);
		}
		else if (file instanceof STFFileBucket) {
			removeSTFFiles(statement, connection, ((STFFileBucket) file).getParentItem().getID());
		}
		else if (file instanceof Spectrum) {

			// cast to spectrum
			Spectrum spectrum = (Spectrum) file;

			// remove ANA file
			removeANAFile(statement, spectrum.getANAFileID());

			// remove TXT file
			removeTXTFile(statement, spectrum.getTXTFileID());

			// remove FLS file
			removeFLSFile(statement, spectrum.getFLSFileID());

			// remove CVT file
			removeCVTFile(statement, spectrum.getCVTFileID());

			// remove conversion table file
			removeConversionTable(statement, spectrum.getConversionTableID());

			// remove STF files
			removeSTFFiles(statement, connection, spectrum.getID());

			// remove spectrum
			removeSpectrum(spectrum, statement);
		}

		// external stress sequence
		else if (file instanceof ExternalStressSequence) {
			removeExternalStressSequence((ExternalStressSequence) file, connection, statement);
		}
		else if (file instanceof ExternalFatigueEquivalentStress) {
			removeExternalFatigueEquivalentStress((ExternalFatigueEquivalentStress) file, statement);
		}
		else if (file instanceof ExternalPreffasEquivalentStress) {
			removeExternalPreffasEquivalentStress((ExternalPreffasEquivalentStress) file, statement);
		}
		else if (file instanceof ExternalLinearEquivalentStress) {
			removeExternalLinearEquivalentStress((ExternalLinearEquivalentStress) file, statement);
		}
		else if (file instanceof AircraftModel) {
			removeAircraftModel((AircraftModel) file, statement, connection);
		}
		else if (file instanceof AircraftLoadCase) {
			removeLoadCase((AircraftLoadCase) file, statement);
		}
		else if (file instanceof PilotPoint) {
			removePilotPoint((PilotPoint) file, statement);
		}
		else if (file instanceof AircraftFatigueEquivalentStress) {
			removeAircraftEquivalentStress((AircraftFatigueEquivalentStress) file, statement);
		}
		else if (file instanceof Rfort) {
			removeRfort((Rfort) file, statement);
		}
	}

	/**
	 * Removes pilot point from database.
	 *
	 * @param file
	 *            Load case to remove.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removePilotPoint(PilotPoint file, Statement statement) throws Exception {
		updateMessage("Deleting pilot point link from database...");
		String sql = "delete from pilot_points_" + file.getParentItem().getParentItem().getID() + " where pp_id = " + file.getID();
		statement.executeUpdate(sql);
	}

	/**
	 * Removes load case from database.
	 *
	 * @param file
	 *            Load case to remove.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeLoadCase(AircraftLoadCase file, Statement statement) throws Exception {

		// delete from load cases
		updateMessage("Deleting load case stresses from database...");
		String sql = "delete from load_cases_" + file.getParentItem().getParentItem().getID();
		sql += " where lc_id = " + file.getID();
		statement.executeUpdate(sql);

		// delete from load case names
		updateMessage("Deleting load case name from database...");
		sql = "delete from load_case_names_" + file.getParentItem().getParentItem().getID();
		sql += " where lc_id = " + file.getID();
		statement.executeUpdate(sql);
	}

	/**
	 * Removes A/C model equivalent stress from database.
	 *
	 * @param file
	 *            A/C model equivalent stress to remove.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeAircraftEquivalentStress(AircraftFatigueEquivalentStress file, Statement statement) throws Exception {
		updateMessage("Deleting equivalent stress from database...");
		String sql = "delete from AC_EQ_STRESSES_" + file.getID() + " where ";
		sql += "name = '" + file.getName() + "'";
		statement.executeUpdate(sql);
	}

	/**
	 * Removes A/C model from database.
	 *
	 * @param file
	 *            Aircraft model to remove.
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeAircraftModel(AircraftModel file, Statement statement, Connection connection) throws Exception {

		// delete grids and elements
		updateMessage("Deleting grids from database...");
		statement.executeUpdate("drop table AURORA.grids_" + file.getID());
		updateMessage("Deleting elements from database...");
		statement.executeUpdate("drop table AURORA.elements_" + file.getID());

		// delete element groups (if any)
		updateMessage("Deleting element groups from database...");
		DatabaseMetaData dbmtadta = connection.getMetaData();
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ELEMENT_GROUPS_" + file.getID(), null)) {
			while (resultSet.next()) {
				statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
			}
		}

		// delete element group names (if any)
		updateMessage("Deleting element group names from database...");
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ELEMENT_GROUP_NAMES_" + file.getID(), null)) {
			while (resultSet.next()) {
				statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
			}
		}

		// delete load cases (if any)
		updateMessage("Deleting load cases from database...");
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "LOAD_CASES_" + file.getID(), null)) {
			while (resultSet.next()) {
				statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
			}
		}

		// delete load case names (if any)
		updateMessage("Deleting load case names from database...");
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "LOAD_CASE_NAMES_" + file.getID(), null)) {
			while (resultSet.next()) {
				statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
			}
		}

		// delete equivalent stresses (if any)
		updateMessage("Deleting equivalent stresses from database...");
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "AC_EQ_STRESSES_" + file.getID(), null)) {
			while (resultSet.next()) {
				statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
			}
		}

		// delete pilot points (if any)
		updateMessage("Deleting pilot point links from database...");
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "PILOT_POINTS_" + file.getID(), null)) {
			while (resultSet.next()) {
				statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
			}
		}

		// delete aircraft model
		updateMessage("Deleting aircraft model from database...");
		statement.executeUpdate("delete from ac_models where model_id = " + file.getID());
	}

	/**
	 * Removes conversion table from database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param convID
	 *            Conversion table ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeConversionTable(Statement statement, int convID) throws Exception {
		updateMessage("Deleting conversion table comments from database...");
		statement.executeUpdate("delete from xls_comments where file_id = " + convID);
		updateMessage("Deleting conversion table info from database...");
		statement.executeUpdate("delete from xls_files where file_id = " + convID);
	}

	/**
	 * Removes ANA file from database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param anaID
	 *            ANA file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeANAFile(Statement statement, int anaID) throws Exception {
		updateMessage("Deleting ANA peaks table from database...");
		statement.executeUpdate("drop table AURORA.ana_peaks_" + anaID);
		updateMessage("Deleting ANA flight info from database...");
		statement.executeUpdate("delete from ana_flights where file_id = " + anaID);
		updateMessage("Deleting ANA file info from database...");
		statement.executeUpdate("delete from ana_files where file_id = " + anaID);
	}

	/**
	 * Removes TXT file from database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param txtID
	 *            TXT file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeTXTFile(Statement statement, int txtID) throws Exception {
		updateMessage("Deleting TXT codes from database...");
		statement.executeUpdate("delete from txt_codes where file_id = " + txtID);
		updateMessage("Deleting TXT file info from database...");
		statement.executeUpdate("delete from txt_files where file_id = " + txtID);
	}

	/**
	 * Removes FLS file from database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param flsID
	 *            FLS file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeFLSFile(Statement statement, int flsID) throws Exception {
		updateMessage("Deleting FLS flights from database...");
		statement.executeUpdate("delete from fls_flights where file_id = " + flsID);
		updateMessage("Deleting FLS file info from database...");
		statement.executeUpdate("delete from fls_files where file_id = " + flsID);
	}

	/**
	 * Removes CVT file from database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param cvtID
	 *            CVT file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeCVTFile(Statement statement, int cvtID) throws Exception {
		updateMessage("Deleting CVT file info from database...");
		statement.executeUpdate("delete from cvt_files where file_id = " + cvtID);
	}

	/**
	 * Removes linked STF files.
	 *
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @param spectrumID
	 *            Spectrum ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeSTFFiles(Statement statement, Connection connection, int spectrumID) throws Exception {

		// get linked pilot point table names
		updateMessage("Retrieving linked pilot point table names from database...");
		ArrayList<String> linkedPPTableNames = null;
		DatabaseMetaData dbmtadta = connection.getMetaData();
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "PILOT_POINTS_%", null)) {
			while (resultSet.next()) {
				if (linkedPPTableNames == null) {
					linkedPPTableNames = new ArrayList<>();
				}
				linkedPPTableNames.add("AURORA." + resultSet.getString(3));
			}
		}

		// prepare statements
		String sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from fatigue_equivalent_stresses inner join analysis_output_files on fatigue_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where fatigue_equivalent_stresses.output_file_id is not null and fatigue_equivalent_stresses.sth_id = ?)";
		try (PreparedStatement removeFatigueOutputFiles = connection.prepareStatement(sql)) {
			sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
			sql += "from preffas_equivalent_stresses inner join analysis_output_files on preffas_equivalent_stresses.output_file_id = analysis_output_files.id ";
			sql += "where preffas_equivalent_stresses.output_file_id is not null and preffas_equivalent_stresses.sth_id = ?)";
			try (PreparedStatement removePreffasOutputFiles = connection.prepareStatement(sql)) {
				sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
				sql += "from linear_equivalent_stresses inner join analysis_output_files on linear_equivalent_stresses.output_file_id = analysis_output_files.id ";
				sql += "where linear_equivalent_stresses.output_file_id is not null and linear_equivalent_stresses.sth_id = ?)";
				try (PreparedStatement removeLinearOutputFiles = connection.prepareStatement(sql)) {
					try (PreparedStatement removeFatigueRainflowCycles = connection.prepareStatement("delete from fatigue_rainflow_cycles where stress_id = ?")) {
						try (PreparedStatement removePreffasRainflowCycles = connection.prepareStatement("delete from preffas_rainflow_cycles where stress_id = ?")) {
							try (PreparedStatement removeLinearRainflowCycles = connection.prepareStatement("delete from linear_rainflow_cycles where stress_id = ?")) {
								try (PreparedStatement removeFatigueEqStresses = connection.prepareStatement("delete from fatigue_equivalent_stresses where sth_id = ?")) {
									try (PreparedStatement removePreffasEqStresses = connection.prepareStatement("delete from preffas_equivalent_stresses where sth_id = ?")) {
										try (PreparedStatement removeLinearEqStresses = connection.prepareStatement("delete from linear_equivalent_stresses where sth_id = ?")) {
											try (PreparedStatement removeEventModifiers = connection.prepareStatement("delete from event_modifiers where sth_id = ?")) {
												try (PreparedStatement removeSegmentModifiers = connection.prepareStatement("delete from segment_modifiers where sth_id = ?")) {
													try (PreparedStatement removeSTHFlights = connection.prepareStatement("delete from sth_flights where file_id = ?")) {
														try (PreparedStatement removeSTHFiles = connection.prepareStatement("delete from sth_files where file_id = ?")) {
															try (PreparedStatement removeDamageAngleEventModifiers = connection.prepareStatement("delete from dam_angle_event_modifiers where angle_id = ?")) {
																try (PreparedStatement removeDamageAngleSegmentModifiers = connection.prepareStatement("delete from dam_angle_segment_modifiers where angle_id = ?")) {
																	try (PreparedStatement removeDamageAngles = connection.prepareStatement("delete from damage_angles where angle_id = ?")) {
																		try (PreparedStatement removeMaxdamAngles = connection.prepareStatement("delete from maxdam_angles where angle_id = ?")) {
																			try (PreparedStatement removeDamContsGAGEvents = connection.prepareStatement("delete from dam_contributions_gag_events where contributions_id = ?")) {
																				try (PreparedStatement removeDamContsEventModifiers = connection.prepareStatement("delete from dam_contributions_event_modifiers where contributions_id = ?")) {
																					try (PreparedStatement removeDamContsSegmentModifiers = connection.prepareStatement("delete from dam_contributions_segment_modifiers where contributions_id = ?")) {
																						try (PreparedStatement removeDamContEventModifiers = connection.prepareStatement("delete from dam_contribution_event_modifiers where contributions_id = ?")) {
																							try (PreparedStatement removeDamCont = connection.prepareStatement("delete from dam_contribution where contributions_id = ?")) {
																								try (PreparedStatement removeDamConts = connection.prepareStatement("delete from dam_contributions where contributions_id = ?")) {
																									try (PreparedStatement removeFlightDamContWithOccurrences = connection.prepareStatement("delete from flight_dam_contribution_with_occurrences where id = ?")) {
																										try (PreparedStatement removeFlightDamContWithoutOccurrences = connection.prepareStatement("delete from flight_dam_contribution_without_occurrences where id = ?")) {
																											try (PreparedStatement removeFlightDamContsEventModifiers = connection.prepareStatement("delete from flight_dam_contributions_event_modifiers where id = ?")) {
																												try (PreparedStatement removeFlightDamContsSegmentModifiers = connection.prepareStatement("delete from flight_dam_contributions_segment_modifiers where id = ?")) {
																													try (PreparedStatement removeFlightDamConts = connection.prepareStatement("delete from flight_dam_contributions where id = ?")) {
																														try (PreparedStatement removeFastFatigueEquivalentStresses = connection.prepareStatement("delete from fast_fatigue_equivalent_stresses where stf_id = ?")) {
																															try (PreparedStatement removeFastPreffasEquivalentStresses = connection.prepareStatement("delete from fast_preffas_equivalent_stresses where stf_id = ?")) {
																																try (PreparedStatement removeFastLinearEquivalentStresses = connection.prepareStatement("delete from fast_linear_equivalent_stresses where stf_id = ?")) {
																																	try (PreparedStatement removeSTFMissionParameters = connection.prepareStatement("delete from stf_mission_parameters where stf_id = ?")) {
																																		try (PreparedStatement removeSTFFiles = connection.prepareStatement("delete from stf_files where file_id = ?")) {
																																			try (PreparedStatement getSTHIDs = connection.prepareStatement("select file_id from sth_files where stf_id = ?")) {
																																				try (PreparedStatement getFatigueEqStressIDs = connection.prepareStatement("select id from fatigue_equivalent_stresses where sth_id = ?")) {
																																					try (PreparedStatement getPreffasEqStressIDs = connection.prepareStatement("select id from preffas_equivalent_stresses where sth_id = ?")) {
																																						try (PreparedStatement getLinearEqStressIDs = connection.prepareStatement("select id from linear_equivalent_stresses where sth_id = ?")) {
																																							try (PreparedStatement getDamageAngleIDs = connection.prepareStatement("select angle_id from maxdam_angles where stf_id = ?")) {
																																								try (PreparedStatement getDamageContIDs = connection.prepareStatement("select contributions_id from dam_contributions where stf_id = ?")) {
																																									try (PreparedStatement getFlightDamageContIDs = connection
																																											.prepareStatement("select id from flight_dam_contributions where stf_id = ?")) {
																																										try (Statement statement2 = connection.createStatement()) {
																																											try (ResultSet getSTFIDs = statement2
																																													.executeQuery("select file_id, stress_table_id from stf_files where cdf_id = " + spectrumID)) {

																																												// loop over STF files
																																												while (getSTFIDs.next()) {

																																													// get STF ID
																																													int stfID = getSTFIDs.getInt("file_id");

																																													// remove linked pilot points
																																													if (linkedPPTableNames != null) {
																																														updateMessage("Deleting linked pilot points from database...");
																																														for (String ppTableName : linkedPPTableNames) {
																																															sql = "delete from " + ppTableName + " where stf_id = " + stfID;
																																															statement.executeUpdate(sql);
																																														}
																																													}

																																													// remove STH files
																																													removeStressSequences(connection, stfID, getFatigueEqStressIDs, getPreffasEqStressIDs, getLinearEqStressIDs, statement,
																																															getSTHIDs, removeSTHFlights, removeSTHFiles, removeEventModifiers, removeSegmentModifiers,
																																															removeFatigueRainflowCycles, removePreffasRainflowCycles, removeLinearRainflowCycles, removeFatigueEqStresses,
																																															removePreffasEqStresses, removeLinearEqStresses, removeFatigueOutputFiles, removePreffasOutputFiles,
																																															removeLinearOutputFiles);

																																													// remove damage angles
																																													removeDamageAngles(stfID, getDamageAngleIDs, removeDamageAngleEventModifiers, removeDamageAngleSegmentModifiers,
																																															removeDamageAngles, removeMaxdamAngles);

																																													// remove damage contributions
																																													removeDamageContributions(stfID, getDamageContIDs, removeDamContsGAGEvents, removeDamContsEventModifiers,
																																															removeDamContsSegmentModifiers, removeDamContEventModifiers, removeDamCont, removeDamConts);

																																													// remove flight damage contributions
																																													removeFlightDamageContributions(stfID, getFlightDamageContIDs, removeFlightDamContsEventModifiers,
																																															removeFlightDamContsSegmentModifiers, removeFlightDamContWithOccurrences, removeFlightDamContWithoutOccurrences,
																																															removeFlightDamConts);

																																													// remove fast fatigue equivalent stress output entries
																																													updateMessage("Deleting fast fatigue equivalent stress output file entries...");
																																													sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
																																													sql += "from fast_fatigue_equivalent_stresses inner join analysis_output_files on fast_fatigue_equivalent_stresses.output_file_id = analysis_output_files.id ";
																																													sql += "where fast_fatigue_equivalent_stresses.output_file_id is not null and fast_fatigue_equivalent_stresses.stf_id = "
																																															+ stfID + ")";
																																													statement.executeUpdate(sql);

																																													// remove fast preffas equivalent stress output entries
																																													updateMessage("Deleting fast preffas equivalent stress output file entries...");
																																													sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
																																													sql += "from fast_preffas_equivalent_stresses inner join analysis_output_files on fast_preffas_equivalent_stresses.output_file_id = analysis_output_files.id ";
																																													sql += "where fast_preffas_equivalent_stresses.output_file_id is not null and fast_preffas_equivalent_stresses.stf_id = "
																																															+ stfID + ")";
																																													statement.executeUpdate(sql);

																																													// remove fast linear propagation equivalent stress output entries
																																													updateMessage("Deleting fast linear propagation equivalent stress output file entries...");
																																													sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
																																													sql += "from fast_linear_equivalent_stresses inner join analysis_output_files on fast_linear_equivalent_stresses.output_file_id = analysis_output_files.id ";
																																													sql += "where fast_linear_equivalent_stresses.output_file_id is not null and fast_linear_equivalent_stresses.stf_id = "
																																															+ stfID + ")";
																																													statement.executeUpdate(sql);

																																													// remove fast equivalent stresses
																																													updateMessage("Deleting fast equivalent stresses from database...");
																																													removeFastFatigueEquivalentStresses.setInt(1, stfID);
																																													removeFastFatigueEquivalentStresses.executeUpdate();
																																													removeFastPreffasEquivalentStresses.setInt(1, stfID);
																																													removeFastPreffasEquivalentStresses.executeUpdate();
																																													removeFastLinearEquivalentStresses.setInt(1, stfID);
																																													removeFastLinearEquivalentStresses.executeUpdate();

																																													// remove mission parameters
																																													updateMessage("Deleting STF mission parameters from database...");
																																													removeSTFMissionParameters.setInt(1, stfID);
																																													removeSTFMissionParameters.executeUpdate();

																																													// remove stresses
																																													updateMessage("Deleting STF stresses from database...");
																																													int stressTableID = getSTFIDs.getInt("stress_table_id");
																																													statement.executeUpdate("delete from stf_stresses_" + stressTableID + " where file_id = " + stfID);

																																													// remove stress table if empty
																																													boolean isEmpty = false;
																																													try (ResultSet resultSet = statement.executeQuery("select 1 from stf_stresses_" + stressTableID)) {
																																														isEmpty = !resultSet.next();
																																													}
																																													if (isEmpty) {
																																														statement.executeUpdate("drop table AURORA.stf_stresses_" + stressTableID);
																																													}

																																													// remove pilot point images
																																													updateMessage("Deleting STF images from database...");
																																													for (PilotPointImageType imageType : PilotPointImageType.values()) {
																																														statement.executeUpdate("delete from " + imageType.getTableName() + " where id = " + stfID);
																																													}

																																													// remove file
																																													updateMessage("Deleting STF file from database...");
																																													removeSTFFiles.setInt(1, stfID);
																																													removeSTFFiles.executeUpdate();
																																												}
																																											}
																																										}
																																									}
																																								}
																																							}
																																						}
																																					}
																																				}
																																			}
																																		}
																																	}
																																}
																															}
																														}
																													}
																												}
																											}
																										}
																									}
																								}
																							}
																						}
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Removes damage contributions from the database.
	 *
	 * @param stfID
	 *            STF file ID.
	 * @param getFlightDamageContIDs
	 *            Database statement for getting damage contribution IDs.
	 * @param removeFlightDamContsEventModifiers
	 *            Database statement for removing event based stress modifiers.
	 * @param removeFlightDamContsSegmentModifiers
	 *            Database statement for removing segment based stress modifiers.
	 * @param removeFlightDamContWithOccurrences
	 *            Database statement for removing damage contributions with occurrences.
	 * @param removeFlightDamContWithoutOccurrences
	 *            Database statement for removing damage contributions without occurrences.
	 * @param removeFlightDamConts
	 *            Database statement for removing damage contribution inputs.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeFlightDamageContributions(int stfID, PreparedStatement getFlightDamageContIDs, PreparedStatement removeFlightDamContsEventModifiers, PreparedStatement removeFlightDamContsSegmentModifiers, PreparedStatement removeFlightDamContWithOccurrences,
			PreparedStatement removeFlightDamContWithoutOccurrences, PreparedStatement removeFlightDamConts) throws Exception {

		// get damage contribution IDs
		getFlightDamageContIDs.setInt(1, stfID);
		try (ResultSet damageContributions = getFlightDamageContIDs.executeQuery()) {

			// loop over damage contributions
			while (damageContributions.next()) {

				// get damage contribution ID
				int damContID = damageContributions.getInt("id");

				// remove event based stress modifiers
				updateMessage("Deleting flight damage contributions event stress modifiers from database...");
				removeFlightDamContsEventModifiers.setInt(1, damContID);
				removeFlightDamContsEventModifiers.executeUpdate();

				// remove segment based stress modifiers
				updateMessage("Deleting flight damage contributions segment based stress modifiers from database...");
				removeFlightDamContsSegmentModifiers.setInt(1, damContID);
				removeFlightDamContsSegmentModifiers.executeUpdate();

				// remove flight damage contributions with occurrences
				updateMessage("Deleting flight damage contributions with occurrences from database...");
				removeFlightDamContWithOccurrences.setInt(1, damContID);
				removeFlightDamContWithOccurrences.executeUpdate();

				// remove flight damage contributions without occurrences
				updateMessage("Deleting flight damage contributions without occurrences from database...");
				removeFlightDamContWithoutOccurrences.setInt(1, damContID);
				removeFlightDamContWithoutOccurrences.executeUpdate();

				// remove segment based stress modifiers
				updateMessage("Deleting flight damage contributions inputs from database...");
				removeFlightDamConts.setInt(1, damContID);
				removeFlightDamConts.executeUpdate();
			}
		}
	}

	/**
	 * Removes stress sequences from the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param stfID
	 *            STF file ID.
	 * @param getFatigueEqStressIDs
	 *            Database statement for getting fatigue equivalent stress IDs.
	 * @param getPreffasEqStressIDs
	 *            Database statement for getting preffas equivalent stress IDs.
	 * @param getLinearEqStressIDs
	 *            Database statement for getting linear equivalent stress IDs.
	 * @param removeSTHPeaks
	 *            Database statement for removing STH peaks.
	 * @param getSTHIDs
	 *            Database statement for getting STH file IDs.
	 * @param removeSTHFlights
	 *            Database statement for removing STH flights.
	 * @param removeSTHFiles
	 *            Database statement for removing STH files.
	 * @param removeEventModifiers
	 *            Database statement for removing event based stress modifiers.
	 * @param removeSegmentModifiers
	 *            Database statement for removing segment based stress modifiers.
	 * @param removeFatigueRainflowCycles
	 *            Database statement for removing fatigue rainflow cycles.
	 * @param removePreffasRainflowCycles
	 *            Database statement for removing preffas rainflow cycles.
	 * @param removeLinearRainflowCycles
	 *            Database statement for removing linear rainflow cycles.
	 * @param removeFatigueEqStresses
	 *            Database statement for removing fatigue equivalent stresses.
	 * @param removePreffasEqStresses
	 *            Database statement for removing preffas equivalent stresses.
	 * @param removeLinearEqStresses
	 *            Database statement for removing linear equivalent stresses.
	 * @param removeFatigueOutputFiles
	 *            Database statement for removing fatigue equivalent stress analysis output files.
	 * @param removePreffasOutputFiles
	 *            Database statement for removing preffas equivalent stress analysis output files.
	 * @param removeLinearOutputFiles
	 *            Database statement for removing linear equivalent stress analysis output files.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeStressSequences(Connection connection, int stfID, PreparedStatement getFatigueEqStressIDs, PreparedStatement getPreffasEqStressIDs, PreparedStatement getLinearEqStressIDs, Statement removeSTHPeaks, PreparedStatement getSTHIDs, PreparedStatement removeSTHFlights,
			PreparedStatement removeSTHFiles, PreparedStatement removeEventModifiers, PreparedStatement removeSegmentModifiers, PreparedStatement removeFatigueRainflowCycles, PreparedStatement removePreffasRainflowCycles, PreparedStatement removeLinearRainflowCycles,
			PreparedStatement removeFatigueEqStresses, PreparedStatement removePreffasEqStresses, PreparedStatement removeLinearEqStresses, PreparedStatement removeFatigueOutputFiles, PreparedStatement removePreffasOutputFiles, PreparedStatement removeLinearOutputFiles) throws Exception {

		// get STH file IDs
		getSTHIDs.setInt(1, stfID);
		try (ResultSet sthFiles = getSTHIDs.executeQuery()) {

			// loop over STH files
			while (sthFiles.next()) {

				// get STH file ID
				int sthID = sthFiles.getInt("file_id");

				// remove fatigue output files
				updateMessage("Deleting fatigue equivalent stress output file entries...");
				removeFatigueOutputFiles.setInt(1, sthID);
				removeFatigueOutputFiles.executeUpdate();

				// remove preffas output files
				updateMessage("Deleting Preffas equivalent stress output file entries...");
				removePreffasOutputFiles.setInt(1, sthID);
				removePreffasOutputFiles.executeUpdate();

				// remove linear propagation output files
				updateMessage("Deleting linear propagation equivalent stress output file entries...");
				removeLinearOutputFiles.setInt(1, sthID);
				removeLinearOutputFiles.executeUpdate();

				// remove fatigue rainflow cycles
				updateMessage("Deleting fatigue rainflow cycles from database...");
				getFatigueEqStressIDs.setInt(1, sthID);
				try (ResultSet fatigueEqStresses = getFatigueEqStressIDs.executeQuery()) {
					while (fatigueEqStresses.next()) {
						removeFatigueRainflowCycles.setInt(1, fatigueEqStresses.getInt("id"));
						removeFatigueRainflowCycles.executeUpdate();
					}
				}

				// remove preffas rainflow cycles
				updateMessage("Deleting preffas rainflow cycles from database...");
				getPreffasEqStressIDs.setInt(1, sthID);
				try (ResultSet preffasEqStresses = getPreffasEqStressIDs.executeQuery()) {
					while (preffasEqStresses.next()) {
						removePreffasRainflowCycles.setInt(1, preffasEqStresses.getInt("id"));
						removePreffasRainflowCycles.executeUpdate();
					}
				}

				// remove linear rainflow cycles
				updateMessage("Deleting linear rainflow cycles from database...");
				getLinearEqStressIDs.setInt(1, sthID);
				try (ResultSet linearEqStresses = getLinearEqStressIDs.executeQuery()) {
					while (linearEqStresses.next()) {
						removeLinearRainflowCycles.setInt(1, linearEqStresses.getInt("id"));
						removeLinearRainflowCycles.executeUpdate();
					}
				}

				// drop segment tables
				updateMessage("Dropping flight segment tables from database...");
				removeSTHPeaks.executeUpdate("drop table AURORA.segments_" + sthID);
				removeSTHPeaks.executeUpdate("drop table AURORA.segment_steady_stresses_" + sthID);
				removeSTHPeaks.executeUpdate("drop table AURORA.segment_increment_stresses_" + sthID);

				// remove fatigue equivalent stresses
				updateMessage("Deleting fatigue equivalent stresses from database...");
				removeFatigueEqStresses.setInt(1, sthID);
				removeFatigueEqStresses.executeUpdate();

				// remove preffas equivalent stresses
				updateMessage("Deleting preffas equivalent stresses from database...");
				removePreffasEqStresses.setInt(1, sthID);
				removePreffasEqStresses.executeUpdate();

				// remove linear equivalent stresses
				updateMessage("Deleting linear equivalent stresses from database...");
				removeLinearEqStresses.setInt(1, sthID);
				removeLinearEqStresses.executeUpdate();

				// remove event based stress modifiers
				updateMessage("Deleting event based stress modifiers from database...");
				removeEventModifiers.setInt(1, sthID);
				removeEventModifiers.executeUpdate();

				// remove segment based stress modifiers
				updateMessage("Deleting segment based stress modifiers from database...");
				removeSegmentModifiers.setInt(1, sthID);
				removeSegmentModifiers.executeUpdate();

				// remove peaks
				updateMessage("Dropping stress sequence peaks table from database...");
				removeSTHPeaks.executeUpdate("drop table AURORA.sth_peaks_" + sthID);

				// remove flights
				updateMessage("Deleting flight info from database...");
				removeSTHFlights.setInt(1, sthID);
				removeSTHFlights.executeUpdate();

				// remove file
				updateMessage("Deleting stress sequence info from database...");
				removeSTHFiles.setInt(1, sthID);
				removeSTHFiles.executeUpdate();
			}
		}
	}

	/**
	 * Removes spectrum from database.
	 *
	 * @param spectrum
	 *            Spectrum to remove.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeSpectrum(Spectrum spectrum, Statement statement) throws Exception {
		updateMessage("Deleting spectrum mission parameters from database...");
		statement.executeUpdate("delete from cdf_mission_parameters where cdf_id = " + spectrum.getID());
		updateMessage("Deleting spectrum from database...");
		statement.executeUpdate("delete from cdf_sets where set_id = " + spectrum.getID());

	}

	/**
	 * Removes external stress sequence from database.
	 *
	 * @param file
	 *            Spectrum file to remove.
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeExternalStressSequence(ExternalStressSequence file, Connection connection, Statement statement) throws Exception {

		// remove fatigue equivalent stress output file entries
		updateMessage("Deleting external fatigue equivalent stress output file entries...");
		String sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from ext_fatigue_equivalent_stresses inner join analysis_output_files on ext_fatigue_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where ext_fatigue_equivalent_stresses.output_file_id is not null and ext_fatigue_equivalent_stresses.sth_id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove preffas equivalent stress output file entries
		updateMessage("Deleting external preffas equivalent stress output file entries...");
		sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from ext_preffas_equivalent_stresses inner join analysis_output_files on ext_preffas_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where ext_preffas_equivalent_stresses.output_file_id is not null and ext_preffas_equivalent_stresses.sth_id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove linear equivalent stress output file entries
		updateMessage("Deleting external linear propagation equivalent stress output file entries...");
		sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from ext_linear_equivalent_stresses inner join analysis_output_files on ext_linear_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where ext_linear_equivalent_stresses.output_file_id is not null and ext_linear_equivalent_stresses.sth_id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove fatigue rainflow cycles
		updateMessage("Deleting external fatigue rainflow cycles from database...");
		ArrayList<ExternalFatigueEquivalentStress> fatigueEqStresses = file.getFatigueEquivalentStresses();
		if (fatigueEqStresses != null && !fatigueEqStresses.isEmpty()) {
			try (PreparedStatement removeRainflowCycles = connection.prepareStatement("delete from ext_fatigue_rainflow_cycles where stress_id = ?")) {
				for (ExternalFatigueEquivalentStress eqStress : fatigueEqStresses) {
					removeRainflowCycles.setInt(1, eqStress.getID());
					removeRainflowCycles.executeUpdate();
				}
			}
		}

		// remove preffas rainflow cycles
		updateMessage("Deleting external preffas rainflow cycles from database...");
		ArrayList<ExternalPreffasEquivalentStress> preffasEqStresses = file.getPreffasEquivalentStresses();
		if (preffasEqStresses != null && !preffasEqStresses.isEmpty()) {
			try (PreparedStatement removeRainflowCycles = connection.prepareStatement("delete from ext_preffas_rainflow_cycles where stress_id = ?")) {
				for (ExternalPreffasEquivalentStress eqStress : preffasEqStresses) {
					removeRainflowCycles.setInt(1, eqStress.getID());
					removeRainflowCycles.executeUpdate();
				}
			}
		}

		// remove linear rainflow cycles
		updateMessage("Deleting external linear rainflow cycles from database...");
		ArrayList<ExternalLinearEquivalentStress> linearEqStresses = file.getLinearEquivalentStresses();
		if (linearEqStresses != null && !linearEqStresses.isEmpty()) {
			try (PreparedStatement removeRainflowCycles = connection.prepareStatement("delete from ext_linear_rainflow_cycles where stress_id = ?")) {
				for (ExternalLinearEquivalentStress eqStress : linearEqStresses) {
					removeRainflowCycles.setInt(1, eqStress.getID());
					removeRainflowCycles.executeUpdate();
				}
			}
		}

		// remove other spectrum tables
		updateMessage("Deleting external stress sequence mission parameters from database...");
		statement.executeUpdate("delete from ext_sth_mission_parameters where sth_id = " + file.getID());
		updateMessage("Deleting external FLS flights from database...");
		statement.executeUpdate("delete from ext_fls_flights where sth_id = " + file.getID());
		updateMessage("Deleting external fatigue equivalent stresses from database...");
		statement.executeUpdate("delete from ext_fatigue_equivalent_stresses where sth_id = " + file.getID());
		updateMessage("Deleting external preffas equivalent stresses from database...");
		statement.executeUpdate("delete from ext_preffas_equivalent_stresses where sth_id = " + file.getID());
		updateMessage("Deleting external linear equivalent stresses from database...");
		statement.executeUpdate("delete from ext_linear_equivalent_stresses where sth_id = " + file.getID());
		updateMessage("Dropping spectrum peaks from database...");
		statement.executeUpdate("drop table AURORA.ext_sth_peaks_" + file.getID());
		updateMessage("Deleting flight info from database...");
		statement.executeUpdate("delete from ext_sth_flights where file_id = " + file.getID());
		updateMessage("Deleting spectrum info from database...");
		statement.executeUpdate("delete from ext_sth_files where file_id = " + file.getID());
	}

	/**
	 * Removes stress sequence from database.
	 *
	 * @param file
	 *            Spectrum file to remove.
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeStressSequence(StressSequence file, Connection connection, Statement statement) throws Exception {

		// remove fatigue equivalent stress output file entries
		updateMessage("Deleting fatigue equivalent stress output file entries...");
		String sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from fatigue_equivalent_stresses inner join analysis_output_files on fatigue_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where fatigue_equivalent_stresses.output_file_id is not null and fatigue_equivalent_stresses.sth_id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove preffas equivalent stress output file entries
		updateMessage("Deleting preffas equivalent stress output file entries...");
		sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from preffas_equivalent_stresses inner join analysis_output_files on preffas_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where preffas_equivalent_stresses.output_file_id is not null and preffas_equivalent_stresses.sth_id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove linear equivalent stress output file entries
		updateMessage("Deleting linear propagation equivalent stress output file entries...");
		sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from linear_equivalent_stresses inner join analysis_output_files on linear_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where linear_equivalent_stresses.output_file_id is not null and linear_equivalent_stresses.sth_id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove fatigue rainflow cycles
		updateMessage("Deleting fatigue rainflow cycles...");
		ArrayList<FatigueEquivalentStress> fatigueEqStresses = file.getFatigueEquivalentStresses();
		if (fatigueEqStresses != null && !fatigueEqStresses.isEmpty()) {
			try (PreparedStatement removeRainflowCycles = connection.prepareStatement("delete from fatigue_rainflow_cycles where stress_id = ?")) {
				for (FatigueEquivalentStress eqStress : fatigueEqStresses) {
					removeRainflowCycles.setInt(1, eqStress.getID());
					removeRainflowCycles.executeUpdate();
				}
			}
		}

		// remove Preffas rainflow cycles
		updateMessage("Deleting preffas rainflow cycles...");
		ArrayList<PreffasEquivalentStress> preffasEqStresses = file.getPreffasEquivalentStresses();
		if (preffasEqStresses != null && !preffasEqStresses.isEmpty()) {
			try (PreparedStatement removeRainflowCycles = connection.prepareStatement("delete from preffas_rainflow_cycles where stress_id = ?")) {
				for (PreffasEquivalentStress eqStress : preffasEqStresses) {
					removeRainflowCycles.setInt(1, eqStress.getID());
					removeRainflowCycles.executeUpdate();
				}
			}
		}

		// remove linear rainflow cycles
		updateMessage("Deleting linear rainflow cycles...");
		ArrayList<LinearEquivalentStress> linearEqStresses = file.getLinearEquivalentStresses();
		if (linearEqStresses != null && !linearEqStresses.isEmpty()) {
			try (PreparedStatement removeRainflowCycles = connection.prepareStatement("delete from linear_rainflow_cycles where stress_id = ?")) {
				for (LinearEquivalentStress eqStress : linearEqStresses) {
					removeRainflowCycles.setInt(1, eqStress.getID());
					removeRainflowCycles.executeUpdate();
				}
			}
		}

		// remove segment info
		updateMessage("Deleting segment info from database...");
		statement.executeUpdate("drop table AURORA.segments_" + file.getID());
		statement.executeUpdate("drop table AURORA.segment_steady_stresses_" + file.getID());
		statement.executeUpdate("drop table AURORA.segment_increment_stresses_" + file.getID());

		// remove equivalent stresses
		updateMessage("Deleting equivalent stresses from database...");
		statement.executeUpdate("delete from fatigue_equivalent_stresses where sth_id = " + file.getID());
		statement.executeUpdate("delete from preffas_equivalent_stresses where sth_id = " + file.getID());
		statement.executeUpdate("delete from linear_equivalent_stresses where sth_id = " + file.getID());

		// remove stress modifiers
		updateMessage("Deleting event based stress modifiers from database...");
		statement.executeUpdate("delete from event_modifiers where sth_id = " + file.getID());
		updateMessage("Deleting segment based stress modifiers from database...");
		statement.executeUpdate("delete from segment_modifiers where sth_id = " + file.getID());

		// remove stress sequence info
		updateMessage("Dropping stress sequence peaks from database...");
		statement.executeUpdate("drop table AURORA.sth_peaks_" + file.getID());
		updateMessage("Deleting stress sequence flight info from database...");
		statement.executeUpdate("delete from sth_flights where file_id = " + file.getID());
		updateMessage("Deleting stress sequence info from database...");
		statement.executeUpdate("delete from sth_files where file_id = " + file.getID());
	}

	/**
	 * Removes STF file from the database.
	 *
	 * @param file
	 *            STF file to remove.
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeSTFFile(STFFile file, Connection connection, Statement statement) throws Exception {
		String sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from fatigue_equivalent_stresses inner join analysis_output_files on fatigue_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where fatigue_equivalent_stresses.output_file_id is not null and fatigue_equivalent_stresses.sth_id = ?)";
		try (PreparedStatement removeFatigueOutputFiles = connection.prepareStatement(sql)) {
			sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
			sql += "from preffas_equivalent_stresses inner join analysis_output_files on preffas_equivalent_stresses.output_file_id = analysis_output_files.id ";
			sql += "where preffas_equivalent_stresses.output_file_id is not null and preffas_equivalent_stresses.sth_id = ?)";
			try (PreparedStatement removePreffasOutputFiles = connection.prepareStatement(sql)) {
				sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
				sql += "from linear_equivalent_stresses inner join analysis_output_files on linear_equivalent_stresses.output_file_id = analysis_output_files.id ";
				sql += "where linear_equivalent_stresses.output_file_id is not null and linear_equivalent_stresses.sth_id = ?)";
				try (PreparedStatement removeLinearOutputFiles = connection.prepareStatement(sql)) {
					try (PreparedStatement removeFatigueRainflowCycles = connection.prepareStatement("delete from fatigue_rainflow_cycles where stress_id = ?")) {
						try (PreparedStatement removePreffasRainflowCycles = connection.prepareStatement("delete from preffas_rainflow_cycles where stress_id = ?")) {
							try (PreparedStatement removeLinearRainflowCycles = connection.prepareStatement("delete from linear_rainflow_cycles where stress_id = ?")) {
								try (PreparedStatement removeFatigueEqStresses = connection.prepareStatement("delete from fatigue_equivalent_stresses where sth_id = ?")) {
									try (PreparedStatement removePreffasEqStresses = connection.prepareStatement("delete from preffas_equivalent_stresses where sth_id = ?")) {
										try (PreparedStatement removeLinearEqStresses = connection.prepareStatement("delete from linear_equivalent_stresses where sth_id = ?")) {
											try (PreparedStatement removeEventModifiers = connection.prepareStatement("delete from event_modifiers where sth_id = ?")) {
												try (PreparedStatement removeSegmentModifiers = connection.prepareStatement("delete from segment_modifiers where sth_id = ?")) {
													try (PreparedStatement removeSTHFlights = connection.prepareStatement("delete from sth_flights where file_id = ?")) {
														try (PreparedStatement removeSTHFiles = connection.prepareStatement("delete from sth_files where file_id = ?")) {
															try (PreparedStatement removeDamageAngleEventModifiers = connection.prepareStatement("delete from dam_angle_event_modifiers where angle_id = ?")) {
																try (PreparedStatement removeDamageAngleSegmentModifiers = connection.prepareStatement("delete from dam_angle_segment_modifiers where angle_id = ?")) {
																	try (PreparedStatement removeDamageAngles = connection.prepareStatement("delete from damage_angles where angle_id = ?")) {
																		try (PreparedStatement removeMaxdamAngles = connection.prepareStatement("delete from maxdam_angles where angle_id = ?")) {
																			try (PreparedStatement removeDamContsGAGEvents = connection.prepareStatement("delete from dam_contributions_gag_events where contributions_id = ?")) {
																				try (PreparedStatement removeDamContsEventModifiers = connection.prepareStatement("delete from dam_contributions_event_modifiers where contributions_id = ?")) {
																					try (PreparedStatement removeDamContsSegmentModifiers = connection.prepareStatement("delete from dam_contributions_segment_modifiers where contributions_id = ?")) {
																						try (PreparedStatement removeDamContEventModifiers = connection.prepareStatement("delete from dam_contribution_event_modifiers where contributions_id = ?")) {
																							try (PreparedStatement removeDamCont = connection.prepareStatement("delete from dam_contribution where contributions_id = ?")) {
																								try (PreparedStatement removeDamConts = connection.prepareStatement("delete from dam_contributions where contributions_id = ?")) {
																									try (PreparedStatement removeFlightDamContsEventModifiers = connection.prepareStatement("delete from flight_dam_contributions_event_modifiers where id = ?")) {
																										try (PreparedStatement removeFlightDamContsSegmentModifiers = connection.prepareStatement("delete from flight_dam_contributions_segment_modifiers where id = ?")) {
																											try (PreparedStatement removeFlightDamContWithOccurrences = connection.prepareStatement("delete from flight_dam_contribution_with_occurrences where id = ?")) {
																												try (PreparedStatement removeFlightDamContWithoutOccurrences = connection.prepareStatement("delete from flight_dam_contribution_without_occurrences where id = ?")) {
																													try (PreparedStatement removeFlightDamConts = connection.prepareStatement("delete from flight_dam_contributions where id = ?")) {
																														try (PreparedStatement getSTHIDs = connection.prepareStatement("select file_id from sth_files where stf_id = ?")) {
																															try (PreparedStatement getFatigueEqStressIDs = connection.prepareStatement("select id from fatigue_equivalent_stresses where sth_id = ?")) {
																																try (PreparedStatement getPreffasEqStressIDs = connection.prepareStatement("select id from preffas_equivalent_stresses where sth_id = ?")) {
																																	try (PreparedStatement getLinearEqStressIDs = connection.prepareStatement("select id from linear_equivalent_stresses where sth_id = ?")) {
																																		try (PreparedStatement getDamageAngleIDs = connection.prepareStatement("select angle_id from maxdam_angles where stf_id = ?")) {
																																			try (PreparedStatement getDamageContIDs = connection.prepareStatement("select contributions_id from dam_contributions where stf_id = ?")) {
																																				try (PreparedStatement getFlightDamageContIDs = connection.prepareStatement("select id from flight_dam_contributions where stf_id = ?")) {

																																					// remove linked pilot points
																																					updateMessage("Deleting linked pilot points from database...");
																																					DatabaseMetaData dbmtadta = connection.getMetaData();
																																					try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "PILOT_POINTS_%", null)) {

																																						// loop over pilot point tables
																																						while (resultSet.next()) {

																																							// get table name
																																							String tableName = "AURORA." + resultSet.getString(3);

																																							// remove linked pilot points
																																							statement.executeUpdate("delete from " + tableName + " where stf_id = " + file.getID());
																																						}
																																					}

																																					// remove STH files
																																					removeStressSequences(connection, file.getID(), getFatigueEqStressIDs, getPreffasEqStressIDs, getLinearEqStressIDs, statement, getSTHIDs,
																																							removeSTHFlights, removeSTHFiles, removeEventModifiers, removeSegmentModifiers, removeFatigueRainflowCycles, removePreffasRainflowCycles,
																																							removeLinearRainflowCycles, removeFatigueEqStresses, removePreffasEqStresses, removeLinearEqStresses, removeFatigueOutputFiles,
																																							removePreffasOutputFiles, removeLinearOutputFiles);

																																					// remove damage angles
																																					removeDamageAngles(file.getID(), getDamageAngleIDs, removeDamageAngleEventModifiers, removeDamageAngleSegmentModifiers, removeDamageAngles,
																																							removeMaxdamAngles);

																																					// remove damage contributions
																																					removeDamageContributions(file.getID(), getDamageContIDs, removeDamContsGAGEvents, removeDamContsEventModifiers, removeDamContsSegmentModifiers,
																																							removeDamContEventModifiers, removeDamCont, removeDamConts);

																																					// remove flight damage contributions
																																					removeFlightDamageContributions(file.getID(), getFlightDamageContIDs, removeFlightDamContsEventModifiers, removeFlightDamContsSegmentModifiers,
																																							removeFlightDamContWithOccurrences, removeFlightDamContWithoutOccurrences, removeFlightDamConts);

																																					// remove fast fatigue equivalent stress output entries
																																					updateMessage("Deleting fast fatigue equivalent stress output file entries...");
																																					sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
																																					sql += "from fast_fatigue_equivalent_stresses inner join analysis_output_files on fast_fatigue_equivalent_stresses.output_file_id = analysis_output_files.id ";
																																					sql += "where fast_fatigue_equivalent_stresses.output_file_id is not null and fast_fatigue_equivalent_stresses.stf_id = " + file.getID() + ")";
																																					statement.executeUpdate(sql);

																																					// remove fast preffas equivalent stress output entries
																																					updateMessage("Deleting fast preffas equivalent stress output file entries...");
																																					sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
																																					sql += "from fast_preffas_equivalent_stresses inner join analysis_output_files on fast_preffas_equivalent_stresses.output_file_id = analysis_output_files.id ";
																																					sql += "where fast_preffas_equivalent_stresses.output_file_id is not null and fast_preffas_equivalent_stresses.stf_id = " + file.getID() + ")";
																																					statement.executeUpdate(sql);

																																					// remove fast linear propagation equivalent stress output entries
																																					updateMessage("Deleting fast linear propagation equivalent stress output file entries...");
																																					sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
																																					sql += "from fast_linear_equivalent_stresses inner join analysis_output_files on fast_linear_equivalent_stresses.output_file_id = analysis_output_files.id ";
																																					sql += "where fast_linear_equivalent_stresses.output_file_id is not null and fast_linear_equivalent_stresses.stf_id = " + file.getID() + ")";
																																					statement.executeUpdate(sql);

																																					// remove fast equivalent stresses
																																					updateMessage("Deleting fast equivalet stresses from database...");
																																					statement.executeUpdate("delete from fast_fatigue_equivalent_stresses where stf_id = " + file.getID());
																																					statement.executeUpdate("delete from fast_preffas_equivalent_stresses where stf_id = " + file.getID());
																																					statement.executeUpdate("delete from fast_linear_equivalent_stresses where stf_id = " + file.getID());

																																					// remove mission parameters
																																					updateMessage("Deleting STF mission parameters from database...");
																																					statement.executeUpdate("delete from stf_mission_parameters where stf_id = " + file.getID());

																																					// remove stresses
																																					updateMessage("Deleting STF stresses from database...");
																																					statement.executeUpdate("delete from stf_stresses_" + file.getStressTableID() + " where file_id = " + file.getID());

																																					// drop stress table if empty
																																					boolean isEmpty = false;
																																					try (ResultSet resultSet = statement.executeQuery("select 1 from stf_stresses_" + file.getStressTableID())) {
																																						isEmpty = !resultSet.next();
																																					}
																																					if (isEmpty) {
																																						statement.executeUpdate("drop table AURORA.stf_stresses_" + file.getStressTableID());
																																					}

																																					// remove pilot point images
																																					updateMessage("Deleting STF images from database...");
																																					for (PilotPointImageType imageType : PilotPointImageType.values()) {
																																						statement.executeUpdate("delete from " + imageType.getTableName() + " where id = " + file.getID());
																																					}

																																					// remove file
																																					updateMessage("Deleting STF file from database...");
																																					statement.executeUpdate("delete from stf_files where file_id = " + file.getID());
																																				}
																																			}
																																		}
																																	}
																																}
																															}
																														}
																													}
																												}
																											}
																										}
																									}
																								}
																							}
																						}
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Removes damage angles from the database.
	 *
	 * @param stfID
	 *            STF file ID.
	 * @param getDamageAngleIDs
	 *            Database statement for getting damage angle IDs.
	 * @param removeDamageAngleEventModifiers
	 *            Database statement for removing event based stress modifiers.
	 * @param removeDamageAngleSegmentModifiers
	 *            Database statement for removing segment based stress modifiers.
	 * @param removeDamageAngles
	 *            Database statement for removing damage angles.
	 * @param removeMaxdamAngles
	 *            Database statement for removing maximum damage angles.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeDamageAngles(int stfID, PreparedStatement getDamageAngleIDs, PreparedStatement removeDamageAngleEventModifiers, PreparedStatement removeDamageAngleSegmentModifiers, PreparedStatement removeDamageAngles, PreparedStatement removeMaxdamAngles) throws Exception {

		// get damage angle IDs
		getDamageAngleIDs.setInt(1, stfID);
		try (ResultSet damageAngles = getDamageAngleIDs.executeQuery()) {

			// loop over damage angles
			while (damageAngles.next()) {

				// get damage angle ID
				int damageAngleID = damageAngles.getInt("angle_id");

				// remove event based stress modifiers
				updateMessage("Deleting damage angle event based stress modifiers from database...");
				removeDamageAngleEventModifiers.setInt(1, damageAngleID);
				removeDamageAngleEventModifiers.executeUpdate();

				// remove segment based stress modifiers
				updateMessage("Deleting damage angle segment based stress modifiers from database...");
				removeDamageAngleSegmentModifiers.setInt(1, damageAngleID);
				removeDamageAngleSegmentModifiers.executeUpdate();

				// remove damage angles
				updateMessage("Deleting damage angles from database...");
				removeDamageAngles.setInt(1, damageAngleID);
				removeDamageAngles.executeUpdate();

				// remove maximum damage angle
				updateMessage("Deleting maximum damage angle from database...");
				removeMaxdamAngles.setInt(1, damageAngleID);
				removeMaxdamAngles.executeUpdate();
			}
		}
	}

	/**
	 * Removes damage contributions from the database.
	 *
	 * @param stfID
	 *            STF file ID.
	 * @param getDamageContIDs
	 *            Database statement for getting damage contribution IDs.
	 * @param removeDamContsGAGEvents
	 *            Database statement for removing GAG events.
	 * @param removeDamContsEventModifiers
	 *            Database statement for removing event based stress modifiers.
	 * @param removeDamContsSegmentModifiers
	 *            Database statement for removing segment based stress modifiers.
	 * @param removeDamContEventModifiers
	 *            Database statement for removing event based stress modifiers.
	 * @param removeDamCont
	 *            Database statement for removing damage contributions.
	 * @param removeDamConts
	 *            Database statement for removing damage contribution inputs.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeDamageContributions(int stfID, PreparedStatement getDamageContIDs, PreparedStatement removeDamContsGAGEvents, PreparedStatement removeDamContsEventModifiers, PreparedStatement removeDamContsSegmentModifiers, PreparedStatement removeDamContEventModifiers,
			PreparedStatement removeDamCont, PreparedStatement removeDamConts) throws Exception {

		// get damage contribution IDs
		getDamageContIDs.setInt(1, stfID);
		try (ResultSet damageContributions = getDamageContIDs.executeQuery()) {

			// loop over damage contributions
			while (damageContributions.next()) {

				// get damage contribution ID
				int damContID = damageContributions.getInt("contributions_id");

				// remove GAG events
				updateMessage("Deleting damage contributions GAG events from database...");
				removeDamContsGAGEvents.setInt(1, damContID);
				removeDamContsGAGEvents.executeUpdate();

				// remove event based stress modifiers
				updateMessage("Deleting damage contributions event stress modifiers from database...");
				removeDamContsEventModifiers.setInt(1, damContID);
				removeDamContsEventModifiers.executeUpdate();

				// remove segment based stress modifiers
				updateMessage("Deleting damage contributions segment based stress modifiers from database...");
				removeDamContsSegmentModifiers.setInt(1, damContID);
				removeDamContsSegmentModifiers.executeUpdate();

				// remove segment based stress modifiers
				updateMessage("Deleting damage contribution event stress modifiers from database...");
				removeDamContEventModifiers.setInt(1, damContID);
				removeDamContEventModifiers.executeUpdate();

				// remove segment based stress modifiers
				updateMessage("Deleting damage contributions from database...");
				removeDamCont.setInt(1, damContID);
				removeDamCont.executeUpdate();

				// remove segment based stress modifiers
				updateMessage("Deleting damage contributions inputs from database...");
				removeDamConts.setInt(1, damContID);
				removeDamConts.executeUpdate();
			}
		}
	}

	/**
	 * Removes fast fatigue equivalent stress from the database.
	 *
	 * @param file
	 *            Equivalent stress file.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeFastFatigueEquivalentStress(FastFatigueEquivalentStress file, Statement statement) throws Exception {

		// remove fatigue equivalent stress output file entry
		updateMessage("Deleting fast fatigue equivalent stress output file entry...");
		String sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from fast_fatigue_equivalent_stresses inner join analysis_output_files on fast_fatigue_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where fast_fatigue_equivalent_stresses.output_file_id is not null and fast_fatigue_equivalent_stresses.id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove rainflow cycles and equivalent stress
		updateMessage("Deleting fast fatigue equivalent stress from database...");
		statement.executeUpdate("delete from fast_fatigue_equivalent_stresses where id = " + file.getID());
	}

	/**
	 * Removes fast preffas equivalent stress from the database.
	 *
	 * @param file
	 *            Equivalent stress file.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeFastPreffasEquivalentStress(FastPreffasEquivalentStress file, Statement statement) throws Exception {

		// remove preffas equivalent stress output file entry
		updateMessage("Deleting fast preffas equivalent stress output file entry...");
		String sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from fast_preffas_equivalent_stresses inner join analysis_output_files on fast_preffas_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where fast_preffas_equivalent_stresses.output_file_id is not null and fast_preffas_equivalent_stresses.id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove rainflow cycles and equivalent stress
		updateMessage("Deleting fast preffas equivalent stress from database...");
		statement.executeUpdate("delete from fast_preffas_equivalent_stresses where id = " + file.getID());
	}

	/**
	 * Removes fast linear equivalent stress from the database.
	 *
	 * @param file
	 *            Equivalent stress file.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeFastLinearEquivalentStress(FastLinearEquivalentStress file, Statement statement) throws Exception {

		// remove linear equivalent stress output file entry
		updateMessage("Deleting fast linear equivalent stress output file entry...");
		String sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from fast_linear_equivalent_stresses inner join analysis_output_files on fast_linear_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where fast_linear_equivalent_stresses.output_file_id is not null and fast_linear_equivalent_stresses.id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove rainflow cycles and equivalent stress
		updateMessage("Deleting fast linear equivalent stress from database...");
		statement.executeUpdate("delete from fast_linear_equivalent_stresses where id = " + file.getID());
	}

	/**
	 * Removes fatigue equivalent stress from the database.
	 *
	 * @param file
	 *            Equivalent stress file.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeFatigueEquivalentStress(FatigueEquivalentStress file, Statement statement) throws Exception {

		// remove fatigue equivalent stress output file entry
		updateMessage("Deleting fatigue equivalent stress output file entry...");
		String sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from fatigue_equivalent_stresses inner join analysis_output_files on fatigue_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where fatigue_equivalent_stresses.output_file_id is not null and fatigue_equivalent_stresses.id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove rainflow cycles and equivalent stress
		updateMessage("Deleting fatigue rainflow cycles from database...");
		statement.executeUpdate("delete from fatigue_rainflow_cycles where stress_id = " + file.getID());
		updateMessage("Deleting fatigue equivalent stress from database...");
		statement.executeUpdate("delete from fatigue_equivalent_stresses where id = " + file.getID());
	}

	/**
	 * Removes preffas equivalent stress from the database.
	 *
	 * @param file
	 *            Equivalent stress file.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removePreffasEquivalentStress(PreffasEquivalentStress file, Statement statement) throws Exception {

		// remove preffas equivalent stress output file entry
		updateMessage("Deleting preffas equivalent stress output file entry...");
		String sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from preffas_equivalent_stresses inner join analysis_output_files on preffas_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where preffas_equivalent_stresses.output_file_id is not null and preffas_equivalent_stresses.id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove rainflow cycles and equivalent stress
		updateMessage("Deleting preffas rainflow cycles from database...");
		statement.executeUpdate("delete from preffas_rainflow_cycles where stress_id = " + file.getID());
		updateMessage("Deleting preffas equivalent stress from database...");
		statement.executeUpdate("delete from preffas_equivalent_stresses where id = " + file.getID());
	}

	/**
	 * Removes linear equivalent stress from the database.
	 *
	 * @param file
	 *            Equivalent stress file.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeLinearEquivalentStress(LinearEquivalentStress file, Statement statement) throws Exception {

		// remove linear equivalent stress output file entry
		updateMessage("Deleting linear equivalent stress output file entry...");
		String sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from linear_equivalent_stresses inner join analysis_output_files on linear_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where linear_equivalent_stresses.output_file_id is not null and linear_equivalent_stresses.id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove rainflow cycles and equivalent stress
		updateMessage("Deleting linear rainflow cycles from database...");
		statement.executeUpdate("delete from linear_rainflow_cycles where stress_id = " + file.getID());
		updateMessage("Deleting linear equivalent stress from database...");
		statement.executeUpdate("delete from linear_equivalent_stresses where id = " + file.getID());
	}

	/**
	 * Removes external fatigue equivalent stress from the database.
	 *
	 * @param file
	 *            Equivalent stress file.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeExternalFatigueEquivalentStress(ExternalFatigueEquivalentStress file, Statement statement) throws Exception {

		// remove fatigue equivalent stress output file entry
		updateMessage("Deleting fatigue equivalent stress output file entry...");
		String sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from ext_fatigue_equivalent_stresses inner join analysis_output_files on ext_fatigue_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where ext_fatigue_equivalent_stresses.output_file_id is not null and ext_fatigue_equivalent_stresses.id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove rainflow cycles and equivalent stress
		updateMessage("Deleting fatigue rainflow cycles from database...");
		statement.executeUpdate("delete from ext_fatigue_rainflow_cycles where stress_id = " + file.getID());
		updateMessage("Deleting fatigue equivalent stress from database...");
		statement.executeUpdate("delete from ext_fatigue_equivalent_stresses where id = " + file.getID());
	}

	/**
	 * Removes external preffas equivalent stress from the database.
	 *
	 * @param file
	 *            Equivalent stress file.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeExternalPreffasEquivalentStress(ExternalPreffasEquivalentStress file, Statement statement) throws Exception {

		// remove preffas equivalent stress output file entry
		updateMessage("Deleting preffas equivalent stress output file entry...");
		String sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from ext_preffas_equivalent_stresses inner join analysis_output_files on ext_preffas_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where ext_preffas_equivalent_stresses.output_file_id is not null and ext_preffas_equivalent_stresses.id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove rainflow cycles and equivalent stress
		updateMessage("Deleting preffas rainflow cycles from database...");
		statement.executeUpdate("delete from ext_preffas_rainflow_cycles where stress_id = " + file.getID());
		updateMessage("Deleting preffas equivalent stress from database...");
		statement.executeUpdate("delete from ext_preffas_equivalent_stresses where id = " + file.getID());
	}

	/**
	 * Removes external linear equivalent stress from the database.
	 *
	 * @param file
	 *            Equivalent stress file.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeExternalLinearEquivalentStress(ExternalLinearEquivalentStress file, Statement statement) throws Exception {

		// remove linear equivalent stress output file entry
		updateMessage("Deleting linear equivalent stress output file entry...");
		String sql = "delete from analysis_output_files where analysis_output_files.id in (select analysis_output_files.id ";
		sql += "from ext_linear_equivalent_stresses inner join analysis_output_files on ext_linear_equivalent_stresses.output_file_id = analysis_output_files.id ";
		sql += "where ext_linear_equivalent_stresses.output_file_id is not null and ext_linear_equivalent_stresses.id = " + file.getID() + ")";
		statement.executeUpdate(sql);

		// remove rainflow cycles and equivalent stress
		updateMessage("Deleting linear rainflow cycles from database...");
		statement.executeUpdate("delete from ext_linear_rainflow_cycles where stress_id = " + file.getID());
		updateMessage("Deleting linear equivalent stress from database...");
		statement.executeUpdate("delete from ext_linear_equivalent_stresses where id = " + file.getID());
	}

	/**
	 * Removes damage angle from the database.
	 *
	 * @param file
	 *            Damage angle.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeDamageAngle(DamageAngle file, Statement statement) throws Exception {
		updateMessage("Deleting damage angle event based stress modifiers from database...");
		statement.executeUpdate("delete from dam_angle_event_modifiers where angle_id = " + file.getID());
		updateMessage("Deleting damage angle segment based stress modifiers from database...");
		statement.executeUpdate("delete from dam_angle_segment_modifiers where angle_id = " + file.getID());
		updateMessage("Deleting damage angles from database...");
		statement.executeUpdate("delete from damage_angles where angle_id = " + file.getID());
		updateMessage("Deleting maximum damage angle from database...");
		statement.executeUpdate("delete from maxdam_angles where angle_id = " + file.getID());
	}

	/**
	 * Removes damage contributions from the database.
	 *
	 * @param file
	 *            Damage contributions.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeDamageContribution(LoadcaseDamageContributions file, Statement statement) throws Exception {
		updateMessage("Deleting damage contributions GAG events from database...");
		statement.executeUpdate("delete from dam_contributions_gag_events where contributions_id = " + file.getID());
		updateMessage("Deleting damage contributions event stress modifiers from database...");
		statement.executeUpdate("delete from dam_contributions_event_modifiers where contributions_id = " + file.getID());
		updateMessage("Deleting damage contributions segment based stress modifiers from database...");
		statement.executeUpdate("delete from dam_contributions_segment_modifiers where contributions_id = " + file.getID());
		updateMessage("Deleting damage contribution increment event stress modifiers from database...");
		statement.executeUpdate("delete from dam_contribution_event_modifiers where contributions_id = " + file.getID());
		updateMessage("Deleting damage contributions from database...");
		statement.executeUpdate("delete from dam_contribution where contributions_id = " + file.getID());
		updateMessage("Deleting damage contributions inputs from database...");
		statement.executeUpdate("delete from dam_contributions where contributions_id = " + file.getID());
	}

	/**
	 * Removes typical flight damage contributions from the database.
	 *
	 * @param file
	 *            Damage contributions.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeFlightDamageContribution(FlightDamageContributions file, Statement statement) throws Exception {
		updateMessage("Deleting flight damage contributions event stress modifiers from database...");
		statement.executeUpdate("delete from flight_dam_contributions_event_modifiers where id = " + file.getID());
		updateMessage("Deleting flight damage contributions segment based stress modifiers from database...");
		statement.executeUpdate("delete from flight_dam_contributions_segment_modifiers where id = " + file.getID());
		updateMessage("Deleting flight damage contribution increment event stress modifiers from database...");
		statement.executeUpdate("delete from flight_dam_contributions_event_modifiers where id = " + file.getID());
		updateMessage("Deleting flight damage contributions with occurrences from database...");
		statement.executeUpdate("delete from flight_dam_contribution_with_occurrences where id = " + file.getID());
		updateMessage("Deleting flight damage contributions without occurrences from database...");
		statement.executeUpdate("delete from flight_dam_contribution_without_occurrences where id = " + file.getID());
		updateMessage("Deleting flight damage contributions inputs from database...");
		statement.executeUpdate("delete from flight_dam_contributions where id = " + file.getID());
	}

	/**
	 * Removes RFORT file from database.
	 *
	 * @param file
	 *            RFORT file.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeRfort(Rfort file, Statement statement) throws Exception {
		updateMessage("Deleting RFORT outputs from database...");
		statement.executeUpdate("delete from rfort_outputs where analysis_id = " + file.getID());
		updateMessage("Deleting RFORT analysis info from database...");
		statement.executeUpdate("delete from rfort_analyses where id = " + file.getID());
	}
}
