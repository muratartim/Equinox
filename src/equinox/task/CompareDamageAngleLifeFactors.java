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
import java.text.DecimalFormat;
import java.util.concurrent.ExecutionException;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.Equinox;
import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.DamageAngle;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for compare damage angle life factors task.
 *
 * @author Murat Artim
 * @date Apr 7, 2016
 * @time 10:13:22 AM
 */
public class CompareDamageAngleLifeFactors extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask {

	/**
	 * Enumeration for results ordering.
	 *
	 * @author Murat Artim
	 * @date Jul 10, 2014
	 * @time 1:05:06 PM
	 */
	public enum ResultOrdering {

		/** Ordering. */
		ANGLE("Order by angles"), DESCENDING("Order by descending life factors"), ASCENDING("Order by ascending life factors");

		/** Name of ordering. */
		private final String name_;

		/**
		 * Creates ordering constant.
		 *
		 * @param name
		 *            Name of ordering.
		 */
		ResultOrdering(String name) {
			name_ = name;
		}

		@Override
		public String toString() {
			return name_;
		}
	}

	/** Tolerance to search for basis equivalent stress. */
	private static final double TOLERANCE = 0.0001;

	/** Damage angle to compare. */
	private final DamageAngle angle_;

	/** Data label option. */
	private final boolean showLabels_;

	/** Result ordering. */
	private final ResultOrdering order_;

	/** Basis damage angle for comparison. */
	private final double basisAngle_;

	/** Decimal formats. */
	private final DecimalFormat angleFormat_ = new DecimalFormat("0");

	/**
	 * Creates compare damage angle life factors task.
	 *
	 * @param damageAngle
	 *            Damage angle.
	 * @param basisAngle
	 *            Basis damage angle for comparison.
	 * @param order
	 *            Results ordering.
	 * @param showlabels
	 *            True to show labels.
	 */
	public CompareDamageAngleLifeFactors(DamageAngle damageAngle, double basisAngle, ResultOrdering order, boolean showlabels) {
		angle_ = damageAngle;
		basisAngle_ = basisAngle;
		order_ = order;
		showLabels_ = showlabels;
	}

	@Override
	public String getTaskTitle() {
		return "Compare life factors";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// update progress info
		updateTitle("Comparing life factors...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get material parameter
				double p = getMaterialParameter(statement);

				// get basis stress
				double basisStress = getBasisStress(statement);

				// create query
				String sql = "select stress, (to_degrees(angle)) as damageAngle from damage_angles where angle_id = " + angle_.getID();
				if (order_.equals(ResultOrdering.DESCENDING)) {
					sql += " order by stress desc";
				}
				else if (order_.equals(ResultOrdering.ASCENDING)) {
					sql += " order by stress asc";
				}
				else if (order_.equals(ResultOrdering.ANGLE)) {
					sql += " order by damageAngle asc";
				}

				// execute query
				try (ResultSet resultSet = statement.executeQuery(sql)) {

					// add data to series
					while (resultSet.next()) {
						String angle = angleFormat_.format(resultSet.getDouble("damageAngle"));
						double lifeFactor = Math.pow(basisStress / resultSet.getDouble("stress"), p);
						dataset.addValue(lifeFactor, "Life Factors", angle);
					}
				}
			}
		}

		// return dataset
		return dataset;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get dataset
			CategoryDataset dataset = get();

			// get column plot panel
			StatisticsViewPanel panel = (StatisticsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);

			// set chart data to panel
			String xAxisLabel = "Angle (in degrees)";
			String yAxisLabel = "Fatigue Life Factor";
			String title = "Life Factor Comparison";
			String subTitle = "Based on angle '" + angleFormat_.format(basisAngle_) + "'";
			panel.setPlotData(dataset, title, subTitle, xAxisLabel, yAxisLabel, false, showLabels_, false);

			// show column chart plot panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Returns the basis equivalent stress for comparison.
	 *
	 * @param statement
	 *            Database statement.
	 * @return The basis equivalent stress for comparison.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double getBasisStress(Statement statement) throws Exception {

		// update progress info
		updateMessage("Getting basis equivalent stress...");
		double stress = 0.0;

		// create query to get material slope for the basis
		double radians = Math.toRadians(basisAngle_);
		double lower = radians - TOLERANCE;
		double upper = radians + TOLERANCE;
		String sql = "select stress from damage_angles where angle_id = " + angle_.getID() + " and angle >= " + lower + " and angle <= " + upper;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				stress = resultSet.getDouble("stress");
			}
		}

		// return basis stress
		return stress;
	}

	/**
	 * Returns the material parameter of the basis damage angle.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Material parameter of the basis damage angle.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double getMaterialParameter(Statement statement) throws Exception {

		// update progress info
		updateMessage("Getting material slope...");
		double p = 0.0;

		// create query to get material parameter for the basis
		String sql = "select material_p from maxdam_angles where angle_id = " + angle_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				p = resultSet.getDouble("material_p");
			}
		}

		// return parameter
		return p;
	}
}
