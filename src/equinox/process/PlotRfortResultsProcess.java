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
import java.util.ArrayList;
import java.util.HashMap;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import equinox.data.fileType.Rfort;
import equinox.data.ui.RfortOmission;
import equinox.task.InternalEquinoxTask;

/**
 * Class for plot RFORT fatigue results process.
 *
 * @author Murat Artim
 * @date Apr 7, 2016
 * @time 12:17:05 PM
 */
public class PlotRfortResultsProcess implements EquinoxProcess<XYSeriesCollection> {

	/** The owner task. */
	private final InternalEquinoxTask<?> task_;

	/** RFORT file. */
	private final Rfort rfort_;

	/** Stress type. */
	private final String stressType_;

	/** Visible pilot points and omissions. */
	private final ArrayList<String> visiblePPs_, visibleOmissions_;

	/** True to plot absolute deviations. */
	private final boolean plotAbsoluteDeviations_;

	/**
	 * Creates plot RFORT fatigue results process.
	 *
	 * @param task
	 *            The owner task.
	 * @param rfort
	 *            RFORT file.
	 * @param stressType
	 *            Stress type.
	 * @param visiblePPs
	 *            Visible pilot points. Null can be given for showing all.
	 * @param visibleOmissions
	 *            Visible omissions. Null can be given for showing all.
	 * @param plotAbsoluteDeviations
	 *            True to plot absolute deviations.
	 */
	public PlotRfortResultsProcess(InternalEquinoxTask<?> task, Rfort rfort, String stressType, ArrayList<String> visiblePPs, ArrayList<String> visibleOmissions, boolean plotAbsoluteDeviations) {
		task_ = task;
		rfort_ = rfort;
		stressType_ = stressType;
		visiblePPs_ = visiblePPs;
		visibleOmissions_ = visibleOmissions;
		plotAbsoluteDeviations_ = plotAbsoluteDeviations;
	}

	@Override
	public XYSeriesCollection start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update progress info
		task_.updateMessage("Plotting RFORT results...");

		// create data set
		XYSeriesCollection dataset = new XYSeriesCollection();

		// create statement
		try (Statement statement = connection.createStatement()) {

			// get initial equivalent stresses
			HashMap<String, Double> initialStresses = new HashMap<>();
			String sql = "select pp_name, eq_stress from rfort_outputs where ";
			sql += "analysis_id = " + rfort_.getID() + " and stress_type = '" + stressType_ + "' ";
			sql += "and omission_name = '" + RfortOmission.INITIAL_ANALYSIS + "'";
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {

					// get pilot point name
					String ppName = resultSet.getString("pp_name");

					// not visible
					if (visiblePPs_ != null && !visiblePPs_.contains(ppName))
						continue;

					// add to stresses
					initialStresses.put(ppName, resultSet.getDouble("eq_stress"));
				}
			}

			// get deviations
			sql = "select pp_name, num_peaks, eq_stress, omission_name from rfort_outputs where ";
			sql += "analysis_id = " + rfort_.getID() + " and stress_type = '" + stressType_ + "' ";
			sql += "order by num_peaks asc";
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {

					// get pilot point name
					String ppName = resultSet.getString("pp_name");

					// not visible
					if (visiblePPs_ != null && !visiblePPs_.contains(ppName))
						continue;

					// get omission name
					String omissionName = resultSet.getString("omission_name");

					// not visible
					if (visibleOmissions_ != null && !visibleOmissions_.contains(omissionName))
						continue;

					// get series
					XYSeries series = getSeries(ppName, dataset);

					// get equivalent stress
					double es = resultSet.getDouble("eq_stress");

					// compute deviation
					double initialES = initialStresses.get(ppName);
					double deviation = (es - initialES) * 100.0 / initialES;
					deviation = plotAbsoluteDeviations_ ? Math.abs(deviation) : deviation;

					// add to series
					series.add(resultSet.getInt("num_peaks"), deviation);
				}
			}
		}

		// return data set
		return dataset;
	}

	/**
	 * Gets the series for the given name.
	 *
	 * @param seriesName
	 *            Series name.
	 * @param dataset
	 *            List containing all the series.
	 * @return The series for the given name.
	 */
	private static XYSeries getSeries(String seriesName, XYSeriesCollection dataset) {

		// series already exists
		int index = dataset.indexOf(seriesName);
		if (index != -1)
			return dataset.getSeries(index);

		// create new series
		XYSeries series = new XYSeries(seriesName, true, false);
		dataset.addSeries(series);

		// return series
		return series;
	}
}
