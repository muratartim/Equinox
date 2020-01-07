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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.CommentLoadCasePanel;
import equinox.data.fileType.AircraftLoadCase;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get load case comments task.
 *
 * @author Murat Artim
 * @date Sep 22, 2015
 * @time 9:17:21 AM
 */
public class GetLoadCaseComments extends InternalEquinoxTask<String> implements ShortRunningTask {

	/** Load case. */
	private final AircraftLoadCase loadCase_;

	/** Requesting panel. */
	private final CommentLoadCasePanel panel_;

	/**
	 * Creates get load case comments task.
	 *
	 * @param loadCase
	 *            Load case.
	 * @param panel
	 *            Requesting panel.
	 */
	public GetLoadCaseComments(AircraftLoadCase loadCase, CommentLoadCasePanel panel) {
		loadCase_ = loadCase;
		panel_ = panel;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get load case comments for '" + loadCase_.getName() + "'";
	}

	@Override
	protected String call() throws Exception {

		// initialize comments
		String comments = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get load case name and number
				String sql = "select lc_comments from load_case_names_" + loadCase_.getParentItem().getParentItem().getID();
				sql += " where lc_id = " + loadCase_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						comments = resultSet.getString("lc_comments");
					}
				}
			}
		}

		// return comments
		return comments;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {
			panel_.setComments(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
