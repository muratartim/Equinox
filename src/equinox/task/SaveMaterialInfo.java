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

import equinox.Equinox;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.STFFileBucket;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

/**
 * Class for save STF files material info task.
 *
 * @author Murat Artim
 * @date 30 Aug 2017
 * @time 11:11:31
 *
 */
public class SaveMaterialInfo extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** Input files. */
	private final ObservableList<TreeItem<String>> items_;

	/** Info array. */
	private final String[] materials_;

	/**
	 * Creates save material info task.
	 *
	 * @param items
	 *            STF files.
	 * @param materials
	 *            Info array.
	 */
	public SaveMaterialInfo(ObservableList<TreeItem<String>> items, String[] materials) {
		items_ = items;
		materials_ = materials;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save material info";
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateMessage("Saving material info to database");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// get first item
				TreeItem<String> item = items_.get(0);

				// STF file
				if (item instanceof STFFile) {
					saveSTFMaterialInfo(connection);
				}

				// STF file bucket
				else if (item instanceof STFFileBucket) {
					saveSTFBucketMaterialInfo(connection);
				}

				// external stress sequence
				else if (item instanceof ExternalStressSequence) {
					saveExternalStressSequenceMaterialInfo(connection);
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return
				return null;
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
	}

	/**
	 * Saves external stress sequence material info in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveExternalStressSequenceMaterialInfo(Connection connection) throws Exception {

		// prepare statement
		String sql = "update ext_sth_files set fatigue_material = ?, preffas_material = ?, linear_material = ? where file_id = ?";
		try (PreparedStatement update = connection.prepareStatement(sql)) {

			// loop over STF files
			for (TreeItem<String> item : items_) {

				// set fatigue material
				if ((materials_[GetMaterials.FATIGUE_MATERIAL] == null) || materials_[GetMaterials.FATIGUE_MATERIAL].trim().isEmpty()) {
					update.setNull(1, java.sql.Types.VARCHAR);
				}
				else {
					update.setString(1, materials_[GetMaterials.FATIGUE_MATERIAL].trim());
				}

				// set preffas material
				if ((materials_[GetMaterials.PREFFAS_MATERIAL] == null) || materials_[GetMaterials.PREFFAS_MATERIAL].trim().isEmpty()) {
					update.setNull(2, java.sql.Types.VARCHAR);
				}
				else {
					update.setString(2, materials_[GetMaterials.PREFFAS_MATERIAL].trim());
				}

				// set linear material
				if ((materials_[GetMaterials.LINEAR_MATERIAL] == null) || materials_[GetMaterials.LINEAR_MATERIAL].trim().isEmpty()) {
					update.setNull(3, java.sql.Types.VARCHAR);
				}
				else {
					update.setString(3, materials_[GetMaterials.LINEAR_MATERIAL].trim());
				}

				// set file ID
				update.setInt(4, ((ExternalStressSequence) item).getID());

				// execute update
				update.executeUpdate();
			}
		}
	}

	/**
	 * Saves STF bucket material info in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveSTFBucketMaterialInfo(Connection connection) throws Exception {

		// prepare statement
		String sql = "update stf_files set fatigue_material = ?, preffas_material = ?, linear_material = ? where cdf_id = ?";
		try (PreparedStatement update = connection.prepareStatement(sql)) {

			// loop over STF buckets
			for (TreeItem<String> item : items_) {

				// set fatigue material
				if ((materials_[GetMaterials.FATIGUE_MATERIAL] == null) || materials_[GetMaterials.FATIGUE_MATERIAL].trim().isEmpty()) {
					update.setNull(1, java.sql.Types.VARCHAR);
				}
				else {
					update.setString(1, materials_[GetMaterials.FATIGUE_MATERIAL].trim());
				}

				// set preffas material
				if ((materials_[GetMaterials.PREFFAS_MATERIAL] == null) || materials_[GetMaterials.PREFFAS_MATERIAL].trim().isEmpty()) {
					update.setNull(2, java.sql.Types.VARCHAR);
				}
				else {
					update.setString(2, materials_[GetMaterials.PREFFAS_MATERIAL].trim());
				}

				// set linear material
				if ((materials_[GetMaterials.LINEAR_MATERIAL] == null) || materials_[GetMaterials.LINEAR_MATERIAL].trim().isEmpty()) {
					update.setNull(3, java.sql.Types.VARCHAR);
				}
				else {
					update.setString(3, materials_[GetMaterials.LINEAR_MATERIAL].trim());
				}

				// set file ID
				update.setInt(4, ((STFFileBucket) item).getParentItem().getID());

				// execute update
				update.executeUpdate();
			}
		}
	}

	/**
	 * Saves STF material info in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveSTFMaterialInfo(Connection connection) throws Exception {

		// prepare statement
		String sql = "update stf_files set fatigue_material = ?, preffas_material = ?, linear_material = ? where file_id = ?";
		try (PreparedStatement update = connection.prepareStatement(sql)) {

			// loop over STF files
			for (TreeItem<String> item : items_) {

				// set fatigue material
				if ((materials_[GetMaterials.FATIGUE_MATERIAL] == null) || materials_[GetMaterials.FATIGUE_MATERIAL].trim().isEmpty()) {
					update.setNull(1, java.sql.Types.VARCHAR);
				}
				else {
					update.setString(1, materials_[GetMaterials.FATIGUE_MATERIAL].trim());
				}

				// set preffas material
				if ((materials_[GetMaterials.PREFFAS_MATERIAL] == null) || materials_[GetMaterials.PREFFAS_MATERIAL].trim().isEmpty()) {
					update.setNull(2, java.sql.Types.VARCHAR);
				}
				else {
					update.setString(2, materials_[GetMaterials.PREFFAS_MATERIAL].trim());
				}

				// set linear material
				if ((materials_[GetMaterials.LINEAR_MATERIAL] == null) || materials_[GetMaterials.LINEAR_MATERIAL].trim().isEmpty()) {
					update.setNull(3, java.sql.Types.VARCHAR);
				}
				else {
					update.setString(3, materials_[GetMaterials.LINEAR_MATERIAL].trim());
				}

				// set file ID
				update.setInt(4, ((STFFile) item).getID());

				// execute update
				update.executeUpdate();
			}
		}
	}
}
