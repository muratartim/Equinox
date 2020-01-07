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
import equinox.controller.CompareFlightsPanel;
import equinox.controller.InputPanel;
import equinox.data.Segment;
import equinox.data.fileType.Flight;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get flight segments.
 *
 * @author Murat Artim
 * @date Sep 16, 2014
 * @time 10:04:11 AM
 */
public class GetFlightSegments extends InternalEquinoxTask<ArrayList<Segment>> implements ShortRunningTask {

	/** Flights. */
	private final Flight[] flights_;

	/**
	 * Creates get flight segments task.
	 *
	 * @param flights
	 *            Flights.
	 */
	public GetFlightSegments(Flight[] flights) {
		flights_ = flights;
	}

	@Override
	public String getTaskTitle() {
		return "Get flight segments";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected ArrayList<Segment> call() throws Exception {

		// update progress info
		updateTitle("Getting flight segments");
		updateMessage("Please wait...");

		// create list
		ArrayList<Segment> segments = new ArrayList<>();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// loop over flights
				for (Flight element : flights_) {

					// get STH file ID
					int sthID = element.getParentItem().getParentItem().getID();

					// create query
					String sql = "select distinct segment, segment_num from sth_peaks_" + sthID + " where ";
					sql += "flight_id = " + element.getID();
					sql += " order by segment_num asc";

					// execute query
					try (ResultSet resultSet = statement.executeQuery(sql)) {
						while (resultSet.next()) {
							String segmentName = resultSet.getString("segment");
							int segmentNumber = resultSet.getInt("segment_num");
							Segment segment = new Segment(segmentName, segmentNumber);
							if (!segments.contains(segment)) {
								segments.add(segment);
							}
						}
					}
				}
			}
		}

		// sort segments
		Collections.sort(segments);

		// return segments
		return segments;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get spectrum view panel
			CompareFlightsPanel panel = (CompareFlightsPanel) taskPanel_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.COMPARE_FLIGHTS_PANEL);

			// set data
			panel.setCommonSegments(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
