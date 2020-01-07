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
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.fileType.AircraftModel;
import equinox.utility.AlphanumComparator;

/**
 * Class for get element positions task.
 *
 * @author Murat Artim
 * @date Jul 15, 2015
 * @time 1:26:56 PM
 */
public class GetElementPositions extends InternalEquinoxTask<ArrayList<String>> {

	/** Requesting panel. */
	private final ElementPositionsRequestingPanel panel_;

	/** Aircraft model. */
	private final AircraftModel model_;

	/**
	 * Creates get element positions task.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param model
	 *            Aircraft model.
	 */
	public GetElementPositions(ElementPositionsRequestingPanel panel, AircraftModel model) {
		panel_ = panel;
		model_ = model;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get element positions";
	}

	@Override
	protected ArrayList<String> call() throws Exception {

		// update progress info
		updateTitle("Retrieving element positions...");
		updateMessage("Please wait...");

		// initialize list
		ArrayList<String> positions = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery("select distinct qv_pos, lv_pos from grids_" + model_.getID() + " where qv_pos is not null and lv_pos is not null order by qv_pos, lv_pos")) {
					while (resultSet.next()) {
						positions.add(resultSet.getString("qv_pos") + " - " + resultSet.getString("lv_pos"));
					}
				}
			}
		}

		// sort
		Collections.sort(positions, new AlphanumComparator());

		// return list
		return positions;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set angles
		try {
			panel_.setElementPositions(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for element positions requesting panels.
	 *
	 * @author Murat Artim
	 * @date Nov 28, 2014
	 * @time 2:18:37 PM
	 */
	public interface ElementPositionsRequestingPanel {

		/**
		 * Sets element positions to this panel.
		 *
		 * @param positions
		 *            Element positions.
		 */
		void setElementPositions(ArrayList<String> positions);
	}
}
