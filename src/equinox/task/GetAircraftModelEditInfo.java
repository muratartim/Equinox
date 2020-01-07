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
import equinox.data.fileType.AircraftModel;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for A/C model edit info task.
 *
 * @author Murat Artim
 * @date 15 Aug 2016
 * @time 15:09:24
 */
public class GetAircraftModelEditInfo extends InternalEquinoxTask<String[]> implements ShortRunningTask {

	/** Info index. */
	public static final int PROGRAM = 0, MODEL_NAME = 1, DELIVERY_REF = 2, DESCRIPTION = 3;

	/** A/C model. */
	private final AircraftModel model_;

	/** Requesting panel. */
	private final AircraftModelInfoRequestingPanel panel_;

	/**
	 * Creates get A/C model edit info task.
	 *
	 * @param model
	 *            A/C model.
	 * @param panel
	 *            Requesting panel.
	 */
	public GetAircraftModelEditInfo(AircraftModel model, AircraftModelInfoRequestingPanel panel) {
		model_ = model;
		panel_ = panel;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get A/C model info for '" + model_.getName() + "'";
	}

	@Override
	protected String[] call() throws Exception {

		// update progress info
		updateMessage("Getting A/C model info from database");

		// create info list
		String[] info = new String[4];

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get info
				String sql = "select ac_program, name, delivery_ref, description from ac_models where model_id = " + model_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						info[PROGRAM] = resultSet.getString("ac_program");
						info[MODEL_NAME] = resultSet.getString("name");
						info[DELIVERY_REF] = resultSet.getString("delivery_ref");
						info[DESCRIPTION] = resultSet.getString("description");
					}
				}
			}
		}

		// return info
		return info;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {
			panel_.setAircraftModelInfo(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for A/C model info requesting panels.
	 *
	 * @author Murat Artim
	 * @date 15 Aug 2016
	 * @time 15:13:06
	 */
	public interface AircraftModelInfoRequestingPanel {

		/**
		 * Sets A/C model info to this panel.
		 *
		 * @param info
		 *            A/C model info to set.
		 */
		void setAircraftModelInfo(String[] info);
	}
}
