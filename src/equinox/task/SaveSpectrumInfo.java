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
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.fileType.Spectrum;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for save spectrum info task.
 *
 * @author Murat Artim
 * @date Feb 3, 2016
 * @time 1:57:31 PM
 */
public class SaveSpectrumInfo extends InternalEquinoxTask<Spectrum> implements ShortRunningTask, SingleInputTask<Spectrum>, AutomaticTaskOwner<Spectrum> {

	/** Spectrum. */
	private Spectrum spectrum_;

	/** Info array. */
	private final String[] info_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Spectrum>> automaticTasks_ = null;

	/**
	 * Creates save spectrum info task.
	 *
	 * @param spectrum
	 *            Spectrum. Can be null for automatic execution.
	 * @param info
	 *            Info array.
	 */
	public SaveSpectrumInfo(Spectrum spectrum, String[] info) {
		spectrum_ = spectrum;
		info_ = info;
	}

	@Override
	public void setAutomaticInput(Spectrum input) {
		spectrum_ = input;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Spectrum> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Spectrum>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save spectrum info";
	}

	@Override
	protected Spectrum call() throws Exception {

		// check permission
		checkPermission(Permission.EDIT_SPECTRUM_INFO);

		// update progress info
		updateMessage("Saving spectrum info to database");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// update info
				updateSpectrumInfo(connection);

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return
				return spectrum_;
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

		try {

			// get spectrum
			Spectrum spectrum = get();

			// update spectrum info
			if (automaticTasks_ == null) {
				spectrum.setProgram(info_[GetSpectrumEditInfo.PROGRAM]);
				spectrum.setSection(info_[GetSpectrumEditInfo.SECTION]);
				spectrum.setMission(info_[GetSpectrumEditInfo.MISSION]);
			}

			// manage automatic tasks
			else {
				automaticTaskOwnerSucceeded(spectrum, automaticTasks_, taskPanel_);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	/**
	 * Updates spectrum info in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void updateSpectrumInfo(Connection connection) throws Exception {

		// prepare statement
		String sql = "update cdf_sets set delivery_ref = ?, description = ? ";
		sql += "where set_id = " + spectrum_.getID();
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			if (info_[GetSpectrumEditInfo.DELIVERY_REF] == null || info_[GetSpectrumEditInfo.DELIVERY_REF].trim().isEmpty()) {
				update.setNull(1, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(1, info_[GetSpectrumEditInfo.DELIVERY_REF].trim());
			}
			if (info_[GetSpectrumEditInfo.DESCRIPTION] == null || info_[GetSpectrumEditInfo.DESCRIPTION].trim().isEmpty()) {
				update.setNull(2, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(2, info_[GetSpectrumEditInfo.DESCRIPTION].trim());
			}
			update.executeUpdate();
		}
	}
}
