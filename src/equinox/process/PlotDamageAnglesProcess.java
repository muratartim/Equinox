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
import java.text.DecimalFormat;
import java.util.List;

import org.jfree.data.category.DefaultCategoryDataset;

import equinox.data.fileType.DamageAngle;
import equinox.plugin.FileType;
import equinox.task.InternalEquinoxTask;
import equinox.task.PlotDamageAngles.ResultOrdering;

/**
 * Class for plot damage angles process.
 *
 * @author Murat Artim
 * @date 26 Jul 2016
 * @time 10:42:07
 */
public class PlotDamageAnglesProcess implements EquinoxProcess<DefaultCategoryDataset> {

	/** The owner task. */
	private final InternalEquinoxTask<?> task_;

	/** Damage angles. */
	private final List<DamageAngle> damageAngles_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0");

	/** Results ordering. */
	private final ResultOrdering order_;

	/**
	 * Creates plot damage angles task.
	 *
	 * @param task
	 *            The owner task.
	 * @param damageAngles
	 *            Damage angles.
	 * @param order
	 *            Results ordering.
	 */
	public PlotDamageAnglesProcess(InternalEquinoxTask<?> task, List<DamageAngle> damageAngles, ResultOrdering order) {
		task_ = task;
		damageAngles_ = damageAngles;
		order_ = order;
	}

	@Override
	public DefaultCategoryDataset start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update progress info
		task_.updateMessage("Plotting damage angles...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// create query
		String sql = "select stress, (to_degrees(angle)) as damageAngle from damage_angles where angle_id = ?";
		if (order_.equals(ResultOrdering.DESCENDING)) {
			sql += " order by stress desc";
		}
		else if (order_.equals(ResultOrdering.ASCENDING)) {
			sql += " order by stress asc";
		}
		else if (order_.equals(ResultOrdering.ANGLE)) {
			sql += " order by damageAngle asc";
		}
		try (PreparedStatement statement = connection.prepareStatement(sql)) {

			// loop over damage angles
			for (DamageAngle angle : damageAngles_) {

				// create series
				String name = FileType.getNameWithoutExtension(angle.getParentItem().getName());

				// set angle ID
				statement.setInt(1, angle.getID());
				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						double yValue = resultSet.getDouble("stress");
						String xValue = format_.format(resultSet.getDouble("damageAngle"));
						dataset.addValue(yValue, name, xValue);
					}
				}
			}
		}

		// return dataset
		return dataset;
	}
}
