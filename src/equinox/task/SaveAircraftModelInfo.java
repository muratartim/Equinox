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

import equinox.Equinox;
import equinox.data.fileType.AircraftModel;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for save A/C model info task.
 *
 * @author Murat Artim
 * @date 15 Aug 2016
 * @time 16:35:28
 */
public class SaveAircraftModelInfo extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** A/C model. */
	private final AircraftModel model_;

	/** Info array. */
	private final String[] info_;

	/**
	 * Creates save A/C model info task.
	 *
	 * @param model
	 *            A/C model.
	 * @param info
	 *            Info array.
	 */
	public SaveAircraftModelInfo(AircraftModel model, String[] info) {
		model_ = model;
		info_ = info;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save A/C model info";
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateMessage("Saving A/C model info to database");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// update info
				updateAircraftModelInfo(connection);

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

		// edit item
		model_.setName(AircraftModel.createName(info_[GetAircraftModelEditInfo.PROGRAM], info_[GetAircraftModelEditInfo.MODEL_NAME]));
		model_.setProgram(info_[GetAircraftModelEditInfo.PROGRAM]);
		model_.setModelName(info_[GetAircraftModelEditInfo.MODEL_NAME]);
	}

	/**
	 * Updates A/C model info in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void updateAircraftModelInfo(Connection connection) throws Exception {

		// check if any model exists with same program and model name
		updateMessage("Checking new A/C program and model name for uniqueness...");
		String sql = "select model_id from ac_models where ac_program = '" + info_[GetAircraftModelEditInfo.PROGRAM] + "' and name = '" + info_[GetAircraftModelEditInfo.MODEL_NAME] + "'";
		try (Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					if (resultSet.getInt("model_id") != model_.getID())
						throw new Exception("Cannot edit A/C model info. An A/C model with same program and model name already exists in the database.");
				}
			}
		}

		// update info
		updateMessage("Updating A/C model info...");
		sql = "update ac_models set name = ?, delivery_ref = ?, description = ? ";
		sql += "where model_id = " + model_.getID();
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			update.setString(1, info_[GetAircraftModelEditInfo.MODEL_NAME].trim());
			if ((info_[GetAircraftModelEditInfo.DELIVERY_REF] == null) || info_[GetAircraftModelEditInfo.DELIVERY_REF].trim().isEmpty()) {
				update.setNull(2, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(2, info_[GetAircraftModelEditInfo.DELIVERY_REF].trim());
			}
			if ((info_[GetAircraftModelEditInfo.DESCRIPTION] == null) || info_[GetAircraftModelEditInfo.DESCRIPTION].trim().isEmpty()) {
				update.setNull(3, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(3, info_[GetAircraftModelEditInfo.DESCRIPTION].trim());
			}
			update.executeUpdate();
		}
	}
}
