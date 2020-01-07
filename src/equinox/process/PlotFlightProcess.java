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
package equinox.process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import equinox.task.InternalEquinoxTask;

/**
 * Class for plot typical flight process.
 *
 * @author Murat Artim
 * @date Jul 9, 2016
 * @time 1:36:18 PM
 */
public class PlotFlightProcess implements EquinoxProcess<XYDataset> {

	/** The owner task. */
	private final InternalEquinoxTask<?> task_;

	/** Flight peaks table name. */
	private final String peaksTableName_;

	/**
	 * Creates plot typical flight process.
	 *
	 * @param task
	 *            The owner task.
	 * @param peaksTableName
	 *            Flight peaks table name.
	 */
	public PlotFlightProcess(InternalEquinoxTask<?> task, String peaksTableName) {
		task_ = task;
		peaksTableName_ = peaksTableName;
	}

	@Override
	public XYDataset start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update progress info
		task_.updateMessage("Plotting typical flight...");

		// create dataset
		XYSeriesCollection dataset = new XYSeriesCollection();

		// create statement
		try (Statement statement = connection.createStatement()) {

			// plot
			plot(dataset, statement);
		}

		// return dataset
		return dataset;
	}

	/**
	 * Plots typical flight.
	 *
	 * @param dataset
	 *            Chart dataset.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plot(XYSeriesCollection dataset, Statement statement) throws Exception {

		// create series
		XYSeries totalStress = new XYSeries("Total Stress");
		XYSeries oneg = new XYSeries("1G");
		XYSeries dp = new XYSeries("Delta-P");
		XYSeries dt = new XYSeries("Delta-T");

		// create query
		String sql = "select peak_num, peak_val, oneg, dp, dt from " + peaksTableName_;
		sql += " order by peak_num asc";
		try (ResultSet peaks = statement.executeQuery(sql)) {

			// loop over peaks
			while (peaks.next()) {

				// get peak number
				int peakNum = peaks.getInt("peak_num");

				// add to data
				totalStress.add(peakNum, peaks.getDouble("peak_val"));
				oneg.add(peakNum, peaks.getDouble("oneg"));
				dp.add(peakNum, peaks.getDouble("dp"));
				dt.add(peakNum, peaks.getDouble("dt"));
			}
		}

		// add series to dataset
		dataset.addSeries(totalStress);
		dataset.addSeries(oneg);
		dataset.addSeries(dp);
		dataset.addSeries(dt);
	}
}
