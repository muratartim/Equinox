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

import equinox.data.GAGEvent;
import equinox.data.Segment;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.dataServer.remote.data.ContributionType;
import equinox.task.InternalEquinoxTask;

/**
 * Class for plot damage contributions process.
 *
 * @author Murat Artim
 * @date 26 Jul 2016
 * @time 10:01:15
 */
public class PlotDamageContributionsProcess implements EquinoxProcess<DefaultCategoryDataset> {

	/** The owner task. */
	private final InternalEquinoxTask<?> task_;

	/** Damage contributions item. */
	private final LoadcaseDamageContributions contributions_;

	/** GAG events. */
	private final ArrayList<GAGEvent> gagEvents_;

	/**
	 * Creates plot damage contributions process.
	 *
	 * @param task
	 *            The owner task.
	 * @param contributions
	 *            Damage contributions to plot.
	 */
	public PlotDamageContributionsProcess(InternalEquinoxTask<?> task, LoadcaseDamageContributions contributions) {
		task_ = task;
		contributions_ = contributions;
		gagEvents_ = new ArrayList<>();
	}

	@Override
	public DefaultCategoryDataset start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update progress info
		task_.updateMessage("Plotting damage contributions...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// create statement
		try (Statement statement = connection.createStatement()) {

			// get total damage
			double totalDamage = 0.0;
			String sql = "select stress from dam_contributions where contributions_id = " + contributions_.getID();
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					totalDamage = resultSet.getDouble("stress");
				}
			}

			// get damage percentages
			double totalInc = 0.0, total1G = 0.0, totalGAG = 0.0, totalDP = 0.0, totalDT = 0.0;
			sql = "select name, cont_type, stress from dam_contribution where contributions_id = " + contributions_.getID() + " order by name";
			try (ResultSet resultSet1 = statement.executeQuery(sql)) {
				while (resultSet1.next()) {

					// get contribution type
					String type = resultSet1.getString("cont_type");

					// get damage percentage
					double damage = resultSet1.getDouble("stress");
					if (type.equals(ContributionType.GAG.getName())) {
						damage = damage * 100.0 / totalDamage;
					}
					else {
						damage = (totalDamage - damage) * 100.0 / totalDamage;
					}
					damage = Math.floor(damage);

					// add to dataset
					if (damage > 0.0) {

						// get contribution name
						String name = resultSet1.getString("name");

						// incremental
						if (type.equals(ContributionType.INCREMENT.getName())) {
							dataset.addValue(damage, name, type);
							totalInc += damage;
						}

						// 1g
						else if (type.equals(ContributionType.ONEG.getName())) {
							dataset.addValue(damage, name, type);
							total1G += damage;
						}

						// GAG
						else if (type.equals(ContributionType.GAG.getName())) {
							dataset.addValue(damage, name, type);
							totalGAG += damage;
						}

						// delta-p
						else if (type.equals(ContributionType.DELTA_P.getName())) {
							dataset.addValue(damage, name, type);
							totalDP += damage;
						}

						// delta-t
						else if (type.equals(ContributionType.DELTA_T.getName())) {
							dataset.addValue(damage, name, type);
							totalDT += damage;
						}
					}
				}
			}

			// set rest for incremental dataset
			if (dataset.getColumnKeys().contains(ContributionType.INCREMENT.getName())) {
				dataset.addValue(Math.floor(100.0 - totalInc), "Rest", ContributionType.INCREMENT.getName());
			}

			// set rest for 1g dataset
			if (dataset.getColumnKeys().contains(ContributionType.ONEG.getName())) {
				dataset.addValue(Math.floor(100.0 - total1G), "Rest", ContributionType.ONEG.getName());
			}

			// set rest for GAG dataset
			if (dataset.getColumnKeys().contains(ContributionType.GAG.getName())) {
				dataset.addValue(Math.floor(100.0 - totalGAG), "Rest", ContributionType.GAG.getName());
			}

			// set rest for delta-p dataset
			if (dataset.getColumnKeys().contains(ContributionType.DELTA_P.getName())) {
				dataset.addValue(Math.floor(100.0 - totalDP), "Rest", ContributionType.DELTA_P.getName());
			}

			// set rest for delta-t dataset
			if (dataset.getColumnKeys().contains(ContributionType.DELTA_T.getName())) {
				dataset.addValue(Math.floor(100.0 - totalDT), "Rest", ContributionType.DELTA_T.getName());
			}

			// create statement for getting GAG events
			sql = "select event_name, issy_code, comment, rating, ismax, segment_name, segment_number from dam_contributions_gag_events where contributions_id = " + contributions_.getID() + " order by rating desc";
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					String eventName = resultSet.getString("event_name");
					String issyCode = resultSet.getString("issy_code");
					String comment = resultSet.getString("comment");
					GAGEvent gagEvent = new GAGEvent(eventName, issyCode);
					gagEvent.setComment(comment);
					gagEvent.setRating(resultSet.getDouble("rating"));
					gagEvent.setType(resultSet.getBoolean("ismax"));
					gagEvent.setSegment(new Segment(resultSet.getString("segment_name"), resultSet.getInt("segment_number")));
					gagEvents_.add(gagEvent);
				}
			}
		}

		// return dataset
		return dataset;
	}

	/**
	 * Returns GAG events.
	 *
	 * @return GAG events.
	 */
	public ArrayList<GAGEvent> getGAGEvents() {
		return gagEvents_;
	}
}
