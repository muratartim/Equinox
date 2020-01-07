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
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.SingleInputTask;

/**
 * Class for set STF file mission task.
 *
 * @author Murat Artim
 * @date Nov 12, 2015
 * @time 4:43:45 PM
 */
public class SetSTFMission extends InternalEquinoxTask<Void> implements ShortRunningTask, SingleInputTask<STFFile> {

	/** STF file. */
	private STFFile stfFile_;

	/** Mission to set. */
	private final String mission_;

	/**
	 * Creates set STF file mission task.
	 *
	 * @param stfFile
	 *            STF file. Can be null for automatic execution.
	 * @param mission
	 *            Mission.
	 */
	public SetSTFMission(STFFile stfFile, String mission) {
		stfFile_ = stfFile;
		mission_ = mission;
	}

	@Override
	public String getTaskTitle() {
		return "Set STF file mission";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public void setAutomaticInput(STFFile input) {
		stfFile_ = input;
	}

	@Override
	protected Void call() throws Exception {

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// set description
				String sql = "update stf_files set fat_mission = ? where file_id = " + stfFile_.getID();
				try (PreparedStatement statement = connection.prepareStatement(sql)) {
					if (mission_.trim().isEmpty()) {
						statement.setNull(1, java.sql.Types.VARCHAR);
					}
					else {
						statement.setString(1, mission_.trim());
					}
					statement.executeUpdate();
				}

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

		// set mission to STF file
		stfFile_.setMission(mission_.trim().isEmpty() ? null : mission_.trim());
	}
}
