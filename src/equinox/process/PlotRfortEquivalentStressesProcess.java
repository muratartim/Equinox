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

import org.jfree.data.category.DefaultCategoryDataset;

import equinox.data.fileType.Rfort;
import equinox.task.InternalEquinoxTask;

/**
 * Class for plot RFORT equivalent stresses process.
 *
 * @author Murat Artim
 * @date Apr 20, 2016
 * @time 7:59:15 AM
 */
public class PlotRfortEquivalentStressesProcess implements EquinoxProcess<DefaultCategoryDataset> {

	/** The owner task. */
	private final InternalEquinoxTask<?> task_;

	/** RFORT file. */
	private final Rfort rfort_;

	/** Stress type. */
	private final String stressType_;

	/** Visible pilot points and omissions. */
	private final ArrayList<String> visiblePPs_, visibleOmissions_;

	/**
	 * Creates plot RFORT equivalent stresses process.
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
	 */
	public PlotRfortEquivalentStressesProcess(InternalEquinoxTask<?> task, Rfort rfort, String stressType, ArrayList<String> visiblePPs, ArrayList<String> visibleOmissions) {
		task_ = task;
		rfort_ = rfort;
		stressType_ = stressType;
		visiblePPs_ = visiblePPs;
		visibleOmissions_ = visibleOmissions;
	}

	@Override
	public DefaultCategoryDataset start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update progress info
		task_.updateMessage("Plotting RFORT equivalent stresses...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// create statement
		try (Statement statement = connection.createStatement()) {

			// create and execute statement
			String sql = "select distinct pp_name, num_peaks, omission_name, eq_stress from rfort_outputs where analysis_id = " + rfort_.getID();
			sql += " and stress_type = '" + stressType_ + "' order by num_peaks desc";
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

					// add to dataset
					double eqStress = resultSet.getDouble("eq_stress");
					dataset.addValue(eqStress, omissionName, ppName);
				}
			}
		}

		// return dataset
		return dataset;
	}
}
