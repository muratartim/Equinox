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
import equinox.controller.SegmentFactorsPopup;
import equinox.data.Segment;
import equinox.data.fileType.Spectrum;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.Utility;

/**
 * Class for get segments task.
 *
 * @author Murat Artim
 * @date Oct 5, 2014
 * @time 3:37:31 PM
 */
public class GetSegments extends InternalEquinoxTask<ArrayList<Segment>> implements ShortRunningTask {

	/** Requesting panel. */
	private final SegmentFactorsPopup panel_;

	/** Spectrum. */
	private final Spectrum spectrum_;

	/**
	 * Creates get segments task.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param spectrum
	 *            Spectrum.
	 */
	public GetSegments(SegmentFactorsPopup panel, Spectrum spectrum) {
		panel_ = panel;
		spectrum_ = spectrum;
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
		updateTitle("Retrieving segments...");
		updateMessage("Please wait...");

		// initialize list
		ArrayList<Segment> segments = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			getSegments(connection, segments);
		}

		// return list
		return segments;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set events
		try {
			panel_.setSegments(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Gets the segments.
	 *
	 * @param connection
	 *            Database connection.
	 * @param segments
	 *            List containing the segments.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getSegments(Connection connection, ArrayList<Segment> segments) throws Exception {
		int txtID = spectrum_.getTXTFileID();
		String sql = "select distinct flight_phase, oneg_order from txt_codes where file_id = " + txtID + " and dp_case = 0 and increment_num = 0";
		sql += " order by oneg_order asc";
		try (Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {

					// get segment name and number
					String segmentName = Utility.extractSegmentName(resultSet.getString("flight_phase"));
					int segmentNumber = resultSet.getInt("oneg_order");

					// create segment
					Segment segment = new Segment(segmentName, segmentNumber);

					// add to segments (if not contained)
					if (!segments.contains(segment)) {
						segments.add(segment);
					}
				}
			}
		}
	}
}
