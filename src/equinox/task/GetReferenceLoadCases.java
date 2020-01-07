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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.ReferenceLoadCase;
import equinox.data.fileType.AircraftModel;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get reference load cases task.
 *
 * @author Murat Artim
 * @date Sep 10, 2015
 * @time 3:39:21 PM
 */
public class GetReferenceLoadCases extends InternalEquinoxTask<ArrayList<ReferenceLoadCase>> implements ShortRunningTask {

	/** Requesting panel. */
	private final ReferenceLoadCaseRequestingPanel panel_;

	/** Aircraft model. */
	private final AircraftModel model_;

	/**
	 * Creates get reference load cases task.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param model
	 *            Aircraft model.
	 */
	public GetReferenceLoadCases(ReferenceLoadCaseRequestingPanel panel, AircraftModel model) {
		panel_ = panel;
		model_ = model;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get reference load cases";
	}

	@Override
	protected ArrayList<ReferenceLoadCase> call() throws Exception {

		// update progress info
		updateTitle("Retrieving load cases...");
		updateMessage("Please wait...");

		// initialize list
		ArrayList<ReferenceLoadCase> cases = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery("select lc_id, lc_name, lc_num from load_case_names_" + model_.getID() + " order by lc_name, lc_num")) {
					while (resultSet.next()) {
						cases.add(new ReferenceLoadCase(resultSet.getString("lc_name"), resultSet.getInt("lc_num"), resultSet.getInt("lc_id")));
					}
				}
			}
		}

		// return list
		return cases;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set angles
		try {
			panel_.setReferenceLoadCases(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for reference load case requesting panels.
	 *
	 * @author Murat Artim
	 * @date Nov 28, 2014
	 * @time 2:18:37 PM
	 */
	public interface ReferenceLoadCaseRequestingPanel {

		/**
		 * Sets reference load cases to this panel.
		 *
		 * @param refCases
		 *            Reference load cases
		 */
		void setReferenceLoadCases(ArrayList<ReferenceLoadCase> refCases);
	}
}
