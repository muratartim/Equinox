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
import equinox.data.fileType.Spectrum;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.SingleInputTask;

/**
 * Class for set spectrum mission task.
 *
 * @author Murat Artim
 * @date Mar 9, 2016
 * @time 2:49:03 PM
 */
public class SetSpectrumMission extends InternalEquinoxTask<Void> implements ShortRunningTask, SingleInputTask<Spectrum> {

	/** Spectrum. */
	private Spectrum spectrum_;

	/** Mission to set. */
	private final String mission_;

	/**
	 * Creates set spectrum mission task.
	 *
	 * @param spectrum
	 *            Spectrum. Can be null for automatic execution.
	 * @param mission
	 *            Mission to set.
	 */
	public SetSpectrumMission(Spectrum spectrum, String mission) {
		spectrum_ = spectrum;
		mission_ = mission;
	}

	@Override
	public void setAutomaticInput(Spectrum spectrum) {
		spectrum_ = spectrum;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Set spectrum mission";
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateMessage("Setting spectrum mission...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// update info
				updateSpectrumMission(connection);

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

		// update spectrum mission
		spectrum_.setMission(mission_);
	}

	/**
	 * Updates spectrum mission in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void updateSpectrumMission(Connection connection) throws Exception {

		// prepare statement
		String sql = "update cdf_sets set fat_mission = ? where set_id = " + spectrum_.getID();
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			update.setString(1, mission_);
			update.executeUpdate();
		}
	}
}
