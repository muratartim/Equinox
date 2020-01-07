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
import equinox.data.ui.QVLVPosition;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.AlphanumComparator2;

/**
 * Class for get frame/stringer positions task.
 *
 * @author Murat Artim
 * @date Aug 4, 2015
 * @time 12:33:55 PM
 */
public class GetQVLVPositions extends InternalEquinoxTask<ArrayList<QVLVPosition>> implements ShortRunningTask {

	/** Requesting panel. */
	private final QVLVPositionRequestingPanel panel_;

	/** Aircraft model. */
	private final AircraftModel model_;

	/**
	 * Creates get frame/stringer positions task.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param model
	 *            Aircraft model.
	 */
	public GetQVLVPositions(QVLVPositionRequestingPanel panel, AircraftModel model) {
		panel_ = panel;
		model_ = model;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get frame/stringer positions";
	}

	@Override
	protected ArrayList<QVLVPosition> call() throws Exception {

		// update progress info
		updateTitle("Retrieving frame/stringer positions...");
		updateMessage("Please wait...");

		// initialize list
		ArrayList<QVLVPosition> positions = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// create query
				String sql = "select distinct qv_pos, lv_pos from grids_" + model_.getID();
				sql += " where qv_pos is not null and lv_pos is not null order by qv_pos, lv_pos";

				// execute query
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						positions.add(new QVLVPosition(resultSet.getString("qv_pos"), resultSet.getString("lv_pos")));
					}
				}
			}
		}

		// sort positions
		Collections.sort(positions, new AlphanumComparator2());

		// return list
		return positions;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set angles
		try {
			panel_.setQVLVPositions(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for frame/stringer position requesting panels.
	 *
	 * @author Murat Artim
	 * @date Nov 28, 2014
	 * @time 2:18:37 PM
	 */
	public interface QVLVPositionRequestingPanel {

		/**
		 * Sets element group names to this panel.
		 *
		 * @param positions
		 *            Frame/stringer positions.
		 */
		void setQVLVPositions(ArrayList<QVLVPosition> positions);
	}
}
