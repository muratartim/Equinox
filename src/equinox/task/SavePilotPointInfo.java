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
import equinox.data.fileType.STFFile;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for save pilot point info task.
 *
 * @author Murat Artim
 * @date Feb 3, 2016
 * @time 9:24:17 AM
 */
public class SavePilotPointInfo extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** STF file. */
	private final STFFile stfFile_;

	/** Info array. */
	private final String[] info_;

	/**
	 * Creates save pilot point info task.
	 *
	 * @param stfFile
	 *            STF file.
	 * @param info
	 *            Info array.
	 */
	public SavePilotPointInfo(STFFile stfFile, String[] info) {
		stfFile_ = stfFile;
		info_ = info;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save pilot point info";
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.EDIT_PILOT_POINT_INFO);

		// update progress info
		updateMessage("Saving pilot point info to database");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// update info
				updateSTFInfo(connection);

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

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set element ID
		String eid = info_[GetSTFInfo2.EID];
		stfFile_.setEID(eid == null ? null : eid.trim());
	}

	/**
	 * Updates STF info in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void updateSTFInfo(Connection connection) throws Exception {

		// prepare statement
		String sql = "update stf_files set description = ?, element_type = ?, frame_rib_position = ?, ";
		sql += "stringer_position = ?, data_source = ?, generation_source = ?, delivery_ref_num = ?, issue = ?, eid = ? where file_id = " + stfFile_.getID();
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			if (info_[GetSTFInfo2.DESCRIPTION] == null || info_[GetSTFInfo2.DESCRIPTION].trim().isEmpty()) {
				update.setNull(1, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(1, info_[GetSTFInfo2.DESCRIPTION].trim());
			}
			if (info_[GetSTFInfo2.ELEMENT_TYPE] == null || info_[GetSTFInfo2.ELEMENT_TYPE].trim().isEmpty()) {
				update.setNull(2, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(2, info_[GetSTFInfo2.ELEMENT_TYPE].trim());
			}
			if (info_[GetSTFInfo2.FRAME_RIB_POS] == null || info_[GetSTFInfo2.FRAME_RIB_POS].trim().isEmpty()) {
				update.setNull(3, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(3, info_[GetSTFInfo2.FRAME_RIB_POS].trim());
			}
			if (info_[GetSTFInfo2.STRINGER_POS] == null || info_[GetSTFInfo2.STRINGER_POS].trim().isEmpty()) {
				update.setNull(4, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(4, info_[GetSTFInfo2.STRINGER_POS].trim());
			}
			if (info_[GetSTFInfo2.DATA_SOURCE] == null || info_[GetSTFInfo2.DATA_SOURCE].trim().isEmpty()) {
				update.setNull(5, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(5, info_[GetSTFInfo2.DATA_SOURCE].trim());
			}
			if (info_[GetSTFInfo2.GEN_SOURCE] == null || info_[GetSTFInfo2.GEN_SOURCE].trim().isEmpty()) {
				update.setNull(6, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(6, info_[GetSTFInfo2.GEN_SOURCE].trim());
			}
			if (info_[GetSTFInfo2.DELIVERY_REF] == null || info_[GetSTFInfo2.DELIVERY_REF].trim().isEmpty()) {
				update.setNull(7, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(7, info_[GetSTFInfo2.DELIVERY_REF].trim());
			}
			if (info_[GetSTFInfo2.ISSUE] == null || info_[GetSTFInfo2.ISSUE].trim().isEmpty()) {
				update.setNull(8, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(8, info_[GetSTFInfo2.ISSUE].trim());
			}
			if (info_[GetSTFInfo2.EID] == null || info_[GetSTFInfo2.EID].trim().isEmpty()) {
				update.setNull(9, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(9, info_[GetSTFInfo2.EID].trim());
			}
			update.executeUpdate();
		}
	}
}
