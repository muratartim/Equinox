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
import equinox.data.fileType.AircraftModel;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get element groups task.
 *
 * @author Murat Artim
 * @date Jul 10, 2015
 * @time 11:53:01 AM
 */
public class GetElementGroups extends InternalEquinoxTask<ArrayList<String>> implements ShortRunningTask {

	/** Requesting panel. */
	private final ElementGroupsRequestingPanel panel_;

	/** Aircraft model. */
	private final AircraftModel model_;

	/**
	 * Creates get element groups task.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param model
	 *            Aircraft model.
	 */
	public GetElementGroups(ElementGroupsRequestingPanel panel, AircraftModel model) {
		panel_ = panel;
		model_ = model;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get element groups";
	}

	@Override
	protected ArrayList<String> call() throws Exception {

		// update progress info
		updateTitle("Retrieving element groups...");
		updateMessage("Please wait...");

		// initialize list
		ArrayList<String> groups = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery("select name from element_group_names_" + model_.getID() + " order by name")) {
					while (resultSet.next()) {
						groups.add(resultSet.getString("name"));
					}
				}
			}
		}

		// return list
		return groups;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set angles
		try {
			panel_.setElementGroups(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for element groups requesting panels.
	 *
	 * @author Murat Artim
	 * @date Nov 28, 2014
	 * @time 2:18:37 PM
	 */
	public interface ElementGroupsRequestingPanel {

		/**
		 * Sets element group names to this panel.
		 *
		 * @param groups
		 *            Element group names.
		 */
		void setElementGroups(ArrayList<String> groups);
	}
}
