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

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.data.fileType.Rfort;
import equinox.task.InternalEquinoxTask;

/**
 * Class for plot RFORT average number of peaks process.
 *
 * @author Murat Artim
 * @date Apr 7, 2016
 * @time 12:00:56 PM
 */
public class PlotRfortPeaksProcess implements EquinoxProcess<CategoryDataset> {

	/** The owner task. */
	private final InternalEquinoxTask<?> task_;

	/** RFORT file. */
	private final Rfort rfort_;

	/**
	 * Creates plot RFORT average number of peaks process.
	 *
	 * @param task
	 *            The owner task.
	 * @param rfort
	 *            RFORT file.
	 */
	public PlotRfortPeaksProcess(InternalEquinoxTask<?> task, Rfort rfort) {
		task_ = task;
		rfort_ = rfort;
	}

	@Override
	public CategoryDataset start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update info
		task_.updateMessage("Plotting RFORT average number of peaks...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// create statement
		try (Statement statement = connection.createStatement()) {

			// create and execute statement
			String sql = "select distinct num_peaks, omission_name from rfort_outputs where analysis_id = " + rfort_.getID();
			sql += " order by num_peaks desc";
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					String ol = resultSet.getString("omission_name");
					int avg = resultSet.getInt("num_peaks");
					dataset.addValue(avg, "Num. Peaks", ol);
				}
			}
		}

		// return dataset
		return dataset;
	}
}
