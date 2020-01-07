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
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.jfree.data.category.DefaultCategoryDataset;

import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.HistogramInput;
import equinox.task.InternalEquinoxTask;

/**
 * Class for plot histogram process.
 *
 * @author Murat Artim
 * @date 22 Jul 2016
 * @time 12:34:23
 */
public class PlotHistogramProcess implements EquinoxProcess<DefaultCategoryDataset> {

	/** The owner task. */
	private final InternalEquinoxTask<?> task_;

	/** Histogram input. */
	private final HistogramInput input_;

	/** Decimal format. */
	private final DecimalFormat format_;

	/** Rainflow cycles table name. */
	private final String rainflowCyclesTableName_;

	/** Equivalent stress. */
	private final SpectrumItem equivalentStress_;

	/**
	 * Creates plot histogram process.
	 *
	 * @param task
	 *            The owner task.
	 * @param input
	 *            Input.
	 * @param equivalentStress
	 *            Equivalent stress.
	 */
	public PlotHistogramProcess(InternalEquinoxTask<?> task, HistogramInput input, SpectrumItem equivalentStress) {
		task_ = task;
		input_ = input;
		equivalentStress_ = equivalentStress;
		rainflowCyclesTableName_ = null;
		String pattern = "0.";
		for (int i = 0; i < input.getDigits(); i++) {
			pattern += "0";
		}
		format_ = new DecimalFormat(pattern);
	}

	/**
	 * Creates plot histogram process.
	 *
	 * @param task
	 *            The owner task.
	 * @param input
	 *            Input.
	 * @param rainflowCyclesTableName
	 *            Rainflow cycles table name.
	 * @param equivalentStress
	 *            Equivalent stress.
	 */
	public PlotHistogramProcess(InternalEquinoxTask<?> task, HistogramInput input, String rainflowCyclesTableName, SpectrumItem equivalentStress) {
		task_ = task;
		input_ = input;
		equivalentStress_ = equivalentStress;
		rainflowCyclesTableName_ = rainflowCyclesTableName;
		String pattern = "0.";
		for (int i = 0; i < input.getDigits(); i++) {
			pattern += "0";
		}
		format_ = new DecimalFormat(pattern);
	}

	@Override
	public DefaultCategoryDataset start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update progress info
		task_.updateMessage("Plotting rainflow histogram...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// create statement
		try (Statement statement = connection.createStatement()) {

			// set table name
			String tableName = rainflowCyclesTableName_ == null ? getTableName() : rainflowCyclesTableName_;

			// create query
			String sql = "select " + input_.getDataType().getDBColumnName() + " as data, sum(num_cycles) as ncyc from " + tableName + " where stress_id = " + equivalentStress_.getID();
			sql += " group by " + input_.getDataType().getDBColumnName();
			sql += " order by ncyc " + (input_.getOrder() ? "desc" : "asc");
			statement.setMaxRows(input_.getLimit());

			// execute query
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// add data to series
				ArrayList<String> dataList = new ArrayList<>();
				while (resultSet.next()) {
					Double yValue = resultSet.getDouble("ncyc");
					String xValue = checkData(format_.format(resultSet.getDouble("data")), dataList);
					dataset.addValue(yValue.intValue(), "Histogram", xValue);
				}
			}

			// reset statement
			statement.setMaxRows(0);
		}

		// return dataset
		return dataset;
	}

	/**
	 * Checks data for uniqueness.
	 *
	 * @param data
	 *            Data to check.
	 * @param dataList
	 *            List containing the data.
	 * @return The data.
	 */
	private String checkData(String data, ArrayList<String> dataList) {

		// data already exists
		if (dataList.contains(data)) {
			data = " " + data + " ";
			return checkData(data, dataList);
		}

		// add to list
		dataList.add(data);

		// return data
		return data;
	}

	/**
	 * Returns rainflow table name.
	 *
	 * @return Rainflow table name.
	 */
	private String getTableName() {

		// initialize table name
		String tableName = null;

		// set table name
		if (equivalentStress_ instanceof FatigueEquivalentStress) {
			tableName = "fatigue_rainflow_cycles";
		}
		else if (equivalentStress_ instanceof PreffasEquivalentStress) {
			tableName = "preffas_rainflow_cycles";
		}
		else if (equivalentStress_ instanceof LinearEquivalentStress) {
			tableName = "linear_rainflow_cycles";
		}
		else if (equivalentStress_ instanceof ExternalFatigueEquivalentStress) {
			tableName = "ext_fatigue_rainflow_cycles";
		}
		else if (equivalentStress_ instanceof ExternalPreffasEquivalentStress) {
			tableName = "ext_preffas_rainflow_cycles";
		}
		else if (equivalentStress_ instanceof ExternalLinearEquivalentStress) {
			tableName = "ext_linear_rainflow_cycles";
		}

		// return table name
		return tableName;
	}
}
