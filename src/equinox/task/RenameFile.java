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
import java.sql.Statement;
import java.util.ArrayList;

import equinox.Equinox;
import equinox.controller.InputPanel;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
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
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.fileType.StressSequence;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;

/**
 * Class for rename file task.
 *
 * @author Murat Artim
 * @date Jul 13, 2014
 * @time 8:02:24 PM
 */
public class RenameFile extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** Item to rename. */
	private final SpectrumItem item_;

	/** The new name. */
	private final String name_;

	/**
	 * Creates rename file task.
	 *
	 * @param item
	 *            Item to rename.
	 * @param name
	 *            The new name.
	 */
	public RenameFile(SpectrumItem item, String name) {
		item_ = item;
		if (item_ instanceof STFFile) {
			name_ = FileType.appendExtension(name, FileType.STF);
		}
		else {
			name_ = name;
		}
	}

	@Override
	public String getTaskTitle() {
		return "Rename file";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.RENAME_FILE);

		// update progress info
		updateTitle("Renaming file...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// rename file
				renameFile(connection);

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				connection.rollback();
				connection.setAutoCommit(true);

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

		// show file view panel
		taskPanel_.getOwner().getOwner().getInputPanel().showSubPanel(InputPanel.FILE_VIEW_PANEL);

		// rename item
		item_.setName(name_);

		// STF file
		if (item_ instanceof STFFile) {

			// cast to STF file
			STFFile stfFile = (STFFile) item_;

			// rename linked pilot points
			renameLinkedPilotPoints(stfFile);
		}
	}

	/**
	 * Renames input file in database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void renameFile(Connection connection) throws Exception {

		// update info
		updateMessage("Renaming file '" + item_.getName() + "'...");

		// A/C model equivalent stresses
		if (item_ instanceof AircraftFatigueEquivalentStress) {
			renameAircraftEquivalentStress(connection);
			return;
		}

		// set database column name
		String tableName = null, idColumn = "file_id", nameColumn = "name";
		if (item_ instanceof Spectrum) {
			tableName = "cdf_sets";
			idColumn = "set_id";
		}
		else if (item_ instanceof STFFile) {
			tableName = "stf_files";
		}
		else if (item_ instanceof StressSequence) {
			tableName = "sth_files";
		}
		else if (item_ instanceof ExternalStressSequence) {
			tableName = "ext_sth_files";
		}
		else if (item_ instanceof FatigueEquivalentStress) {
			tableName = "fatigue_equivalent_stresses";
			idColumn = "id";
			nameColumn = "name";
		}
		else if (item_ instanceof PreffasEquivalentStress) {
			tableName = "preffas_equivalent_stresses";
			idColumn = "id";
			nameColumn = "name";
		}
		else if (item_ instanceof LinearEquivalentStress) {
			tableName = "linear_equivalent_stresses";
			idColumn = "id";
			nameColumn = "name";
		}
		else if (item_ instanceof ExternalFatigueEquivalentStress) {
			tableName = "ext_fatigue_equivalent_stresses";
			idColumn = "id";
			nameColumn = "name";
		}
		else if (item_ instanceof ExternalPreffasEquivalentStress) {
			tableName = "ext_preffas_equivalent_stresses";
			idColumn = "id";
			nameColumn = "name";
		}
		else if (item_ instanceof ExternalLinearEquivalentStress) {
			tableName = "ext_linear_equivalent_stresses";
			idColumn = "id";
			nameColumn = "name";
		}
		else if (item_ instanceof FastFatigueEquivalentStress) {
			tableName = "fast_fatigue_equivalent_stresses";
			idColumn = "id";
			nameColumn = "name";
		}
		else if (item_ instanceof FastPreffasEquivalentStress) {
			tableName = "fast_preffas_equivalent_stresses";
			idColumn = "id";
			nameColumn = "name";
		}
		else if (item_ instanceof FastLinearEquivalentStress) {
			tableName = "fast_linear_equivalent_stresses";
			idColumn = "id";
			nameColumn = "name";
		}
		else if (item_ instanceof LoadcaseDamageContributions) {
			tableName = "dam_contributions";
			idColumn = "contributions_id";
			nameColumn = "name";
		}
		else if (item_ instanceof FlightDamageContributions) {
			tableName = "flight_dam_contributions";
			idColumn = "id";
			nameColumn = "name";
		}
		else if (item_ instanceof DamageAngle) {
			tableName = "maxdam_angles";
			idColumn = "angle_id";
			nameColumn = "name";
		}
		else if (item_ instanceof Rfort) {
			tableName = "rfort_analyses";
			idColumn = "id";
			nameColumn = "input_spectrum_name";
		}

		// create query
		String sql = "update " + tableName + " set " + nameColumn + " = '" + name_ + "' where " + idColumn + " = " + item_.getID();

		// create statement
		try (Statement statement = connection.createStatement()) {

			// execute update
			statement.executeUpdate(sql);
		}
	}

	/**
	 * Renames A/C model equivalent stress in database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void renameAircraftEquivalentStress(Connection connection) throws Exception {
		String sql = "update AC_EQ_STRESSES_" + item_.getID() + " set name = '" + name_ + "' where name = '" + item_.getName() + "'";
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}
	}

	/**
	 * Renames linked pilot points in the file tree.
	 *
	 * @param stfFile
	 *            STF file which was renamed.
	 */
	private void renameLinkedPilotPoints(STFFile stfFile) {

		// get file tree root
		TreeItem<String> root = taskPanel_.getOwner().getOwner().getInputPanel().getFileTreeRoot();

		// loop over children
		for (TreeItem<String> item : root.getChildren()) {

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

				// loop over pilot points
				for (PilotPoint pp : pps) {
					if (stfFile.getID() == pp.getSTFFileID()) {
						pp.setName(stfFile.getName());
					}
				}
			}
		}
	}
}
