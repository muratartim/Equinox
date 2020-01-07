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

import org.jfree.data.category.DefaultCategoryDataset;

import equinox.data.fileType.FlightDamageContributions;
import equinox.task.InternalEquinoxTask;

/**
 * Class for plot typical flight damage contributions process.
 *
 * @author Murat Artim
 * @date 18 Oct 2016
 * @time 15:00:02
 */
public class PlotFlightDamageContributionsProcess implements EquinoxProcess<DefaultCategoryDataset> {

	/** The owner task. */
	private final InternalEquinoxTask<?> task_;

	/** Damage contributions item. */
	private final FlightDamageContributions contributions_;

	/**
	 * Creates plot typical flight damage contributions process.
	 *
	 * @param task
	 *            The owner task.
	 * @param contributions
	 *            Damage contributions to plot.
	 */
	public PlotFlightDamageContributionsProcess(InternalEquinoxTask<?> task, FlightDamageContributions contributions) {
		task_ = task;
		contributions_ = contributions;
	}

	@Override
	public DefaultCategoryDataset start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update progress info
		task_.updateMessage("Plotting damage contributions...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// create statement
		try (Statement statement = connection.createStatement()) {

			// plot contributions with occurrences
			double restPercentWithOccurrences = plotContributions("Damage Contributions\nWith Flight Occurrences", true, dataset, statement);

			// plot contributions without occurrences
			double restPercentWithoutOccurrences = plotContributions("Damage Contributions\nWithout Flight Occurrences", false, dataset, statement);

			// add the rest percentages
			dataset.addValue(restPercentWithOccurrences, "Rest", "Damage Contributions\nWith Flight Occurrences");
			dataset.addValue(restPercentWithoutOccurrences, "Rest", "Damage Contributions\nWithout Flight Occurrences");
		}

		// return dataset
		return dataset;
	}

	/**
	 * Plots typical flight contributions.
	 *
	 * @param columnKey
	 *            Column key of the plot.
	 * @param withOccurrences
	 *            True if typical flight occurrences should be taken into account.
	 * @param dataset
	 *            Plot dataset.
	 * @param statement
	 *            Database statement.
	 * @return The rest percentage.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double plotContributions(String columnKey, boolean withOccurrences, DefaultCategoryDataset dataset, Statement statement) throws Exception {

		// set table name
		String tableName = withOccurrences ? "flight_dam_contribution_with_occurrences" : "flight_dam_contribution_without_occurrences";

		// add percentages of each typical flight
		double totalPercentage = 0.0;
		String sql = "select flight_name, dam_percent from " + tableName + " where id = " + contributions_.getID() + " order by dam_percent desc";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				String flightName = resultSet.getString("flight_name");
				double damPercent = Math.floor(resultSet.getDouble("dam_percent"));
				totalPercentage += damPercent;
				dataset.addValue(damPercent, flightName, columnKey);
			}
		}

		// return rest percentage
		return Math.floor(100.0 - totalPercentage);
	}
}
