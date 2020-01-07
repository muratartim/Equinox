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

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.Equinox;
import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.ElementTypeForStress;
import equinox.data.input.CompareElementStressesInput;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for compare element stresses task.
 *
 * @author Murat Artim
 * @date Apr 7, 2016
 * @time 10:54:28 AM
 */
public class CompareElementStresses extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask {

	/** Input. */
	private final CompareElementStressesInput input_;

	/**
	 * Creates generate statistics task.
	 *
	 * @param input
	 *            Input.
	 */
	public CompareElementStresses(CompareElementStressesInput input) {
		input_ = input;
	}

	@Override
	public String getTaskTitle() {
		return "Compare element stresses for load case '" + input_.getLoadCase().getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_ELEMENT_STRESS_COMPARISON);

		// update progress info
		updateTitle("Comparing element stresses for load case '" + input_.getLoadCase().getName() + "'...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get query
				String sql = input_.getGroups().isEmpty() ? createQueryWithoutGrouping() : createQueryWithGrouping(statement);

				// set limit
				statement.setMaxRows(input_.getLimit());

				// create and execute query
				try (ResultSet resultSet = statement.executeQuery(sql)) {

					// add data to series
					while (resultSet.next()) {
						String eid = Integer.toString(resultSet.getInt("eid"));
						double stress = resultSet.getDouble(input_.getStressComponent().getResultSetColName());
						dataset.addValue(stress, "Element Stresses", eid);
					}
				}

				// reset statement
				statement.setMaxRows(0);
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
			String title = input_.getLoadCase().getName();
			title += ", " + input_.getElementType().getName();
			panel.setPlotData(dataset, title, null, "Element", input_.getStressComponent().getName(), false, input_.getLabelDisplay(), false);

			// show column chart plot panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Creates query with element grouping.
	 *
	 * @return Query.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String createQueryWithoutGrouping() throws Exception {

		// get table names
		int modelID = input_.getLoadCase().getParentItem().getParentItem().getID();
		String elTable = "elements_" + modelID;
		String lcTable = "load_cases_" + modelID;

		// select stresses
		String sql = "select " + lcTable + ".eid, " + input_.getStressComponent().getDBSelectString();
		sql += " from " + lcTable;

		// join with elements
		sql += " inner join " + elTable + " on ";
		sql += lcTable + ".eid = ";
		sql += elTable + ".eid";

		// add load case criteria
		sql += " where lc_id = " + input_.getLoadCase().getID();

		// add element type criteria
		if (input_.getElementType().equals(ElementTypeForStress.SKIN)) {
			sql += " and (el_type = 'QUAD' or el_type = 'TRIA')";
		}
		else if (input_.getElementType().equals(ElementTypeForStress.BEAM)) {
			sql += " and (el_type = 'BEAM')";
		}
		else if (input_.getElementType().equals(ElementTypeForStress.ROD)) {
			sql += " and (el_type = 'ROD')";
		}

		// order stresses
		sql += " order by " + input_.getStressComponent().getResultSetColName();
		sql += input_.getOrder() ? " desc" : " asc";

		// return query
		return sql;
	}

	/**
	 * Creates query with element grouping.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Query.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String createQueryWithGrouping(Statement statement) throws Exception {

		// get element group IDs
		ArrayList<Integer> groupIDs = getGroupIDs(statement);

		// get table names
		int modelID = input_.getLoadCase().getParentItem().getParentItem().getID();
		String elTable = "elements_" + modelID;
		String grpTable = "element_groups_" + modelID;
		String lcTable = "load_cases_" + modelID;

		// select stresses
		String sql = "select " + lcTable + ".eid, " + input_.getStressComponent().getDBSelectString();
		sql += " from " + lcTable;

		// join with groups
		sql += " inner join " + grpTable + " on ";
		sql += lcTable + ".eid = ";
		sql += grpTable + ".eid";

		// join with elements
		sql += " inner join " + elTable + " on ";
		sql += lcTable + ".eid = ";
		sql += elTable + ".eid";

		// add load case criteria
		sql += " where lc_id = " + input_.getLoadCase().getID();

		// add group name criteria
		sql += " and (";
		for (int groupID : groupIDs) {
			sql += grpTable + ".group_id = " + groupID + " or ";
		}
		sql = sql.substring(0, sql.length() - " or ".length()) + ")";

		// add element type criteria
		if (input_.getElementType().equals(ElementTypeForStress.SKIN)) {
			sql += " and (el_type = 'QUAD' or el_type = 'TRIA')";
		}
		else if (input_.getElementType().equals(ElementTypeForStress.BEAM)) {
			sql += " and (el_type = 'BEAM')";
		}
		else if (input_.getElementType().equals(ElementTypeForStress.ROD)) {
			sql += " and (el_type = 'ROD')";
		}

		// order stresses
		sql += " order by " + input_.getStressComponent().getResultSetColName();
		sql += input_.getOrder() ? " desc" : " asc";

		// return query
		return sql;
	}

	/**
	 * Retrieves and returns list containing element group IDs.
	 *
	 * @param statement
	 *            Database statement.
	 * @return List containing element group IDs.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<Integer> getGroupIDs(Statement statement) throws Exception {

		// initialize list
		ArrayList<Integer> groupIDs = new ArrayList<>();

		// get element group IDs
		ArrayList<String> groups = input_.getGroups();
		String sql = "select group_id from element_group_names_" + input_.getLoadCase().getParentItem().getParentItem().getID();
		sql += " where (";
		for (String group : groups) {
			sql += "name = '" + group + "' or ";
		}
		sql = sql.substring(0, sql.length() - " or ".length()) + ")";

		// execute query
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				groupIDs.add(resultSet.getInt("group_id"));
			}
		}

		// return list
		return groupIDs;
	}
}
