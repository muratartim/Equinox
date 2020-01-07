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

import equinox.Equinox;
import equinox.data.fileType.AircraftLoadCase;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for set load case comments task.
 *
 * @author Murat Artim
 * @date Sep 22, 2015
 * @time 9:38:50 AM
 */
public class SetLoadCaseComments extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** Load case. */
	private final AircraftLoadCase loadCase_;

	/** Comments to set. */
	private final String comments_;

	/**
	 * Creates set load case comments task.
	 *
	 * @param loadCase
	 *            Load case.
	 * @param comments
	 *            Comments.
	 */
	public SetLoadCaseComments(AircraftLoadCase loadCase, String comments) {
		loadCase_ = loadCase;
		comments_ = comments;
	}

	@Override
	public String getTaskTitle() {
		return "Set load case comments";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// set comments
				setComments(connection);

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

	/**
	 * Sets load case comments.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void setComments(Connection connection) throws Exception {
		String sql = "update load_case_names_" + loadCase_.getParentItem().getParentItem().getID();
		sql += " set lc_comments = " + (comments_ == null ? "null" : "'" + comments_ + "'");
		sql += " where lc_id = " + loadCase_.getID();
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}
	}
}
