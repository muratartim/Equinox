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
import java.sql.PreparedStatement;
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
import equinox.data.input.CompareAircraftEquivalentStressRatiosInput;
import equinox.data.input.EquivalentStressRatioType;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for generate A/C model equivalent stress ratios task.
 *
 * @author Murat Artim
 * @date Apr 7, 2016
 * @time 11:36:12 AM
 */
public class GenerateAircraftEquivalentStressRatios extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask {

	/** Input. */
	private final CompareAircraftEquivalentStressRatiosInput input_;

	/**
	 * Creates generate A/C model equivalent stress ratios task.
	 *
	 * @param input
	 *            Input.
	 */
	public GenerateAircraftEquivalentStressRatios(CompareAircraftEquivalentStressRatiosInput input) {
		input_ = input;
	}

	@Override
	public String getTaskTitle() {
		return "Generate equivalent stress ratios of '" + input_.getEquivalentStress().getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// check permission
		checkPermission(Permission.GENERATE_EQUIVALENT_STRESS_RATIOS);

		// update progress info
		updateTitle("Generating equivalent stress ratios of '" + input_.getEquivalentStress().getName() + "'...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement to get other mission stresses
			try (PreparedStatement statement1 = connection.prepareStatement(createOtherMissionQuery())) {

				// create and execute query for basis mission
				try (Statement statement2 = connection.createStatement()) {
					String sql = input_.getGroups().isEmpty() ? createBasisMissionQueryWithoutGrouping() : createBasisMissionQueryWithGrouping(statement2);
					try (ResultSet getBasisMission = statement2.executeQuery(sql)) {

						// loop over basis mission stresses
						while (getBasisMission.next()) {

							// get data
							int eid = getBasisMission.getInt("eid");
							String eidString = Integer.toString(eid);
							double stress = getBasisMission.getDouble("stress");

							// execute query for other missions
							statement1.setDouble(1, stress);
							statement1.setInt(2, eid);
							try (ResultSet getOtherMission = statement1.executeQuery()) {

								// loop over other missions
								while (getOtherMission.next()) {

									// get mission
									String mission = getOtherMission.getString("mission");

									// skip basis mission
									if (!input_.getIncludeBasisMission() && mission.equals(input_.getBasisMission())) {
										continue;
									}

									// max. limit exceeded
									if (dataset.getColumnCount() >= input_.getLimit()) {
										continue;
									}

									// add data
									double ratio = getOtherMission.getDouble("ratio");
									dataset.addValue(ratio, mission, eidString);
								}
							}
						}
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
			String title = input_.getRatioType().toString() + " Comparison";
			String subTitle = "Based on mission '" + input_.getBasisMission() + "'";
			panel.setPlotData(dataset, title, subTitle, "Element", input_.getRatioType().toString(), true, input_.getLabelDisplay(), false);

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
	private String createOtherMissionQuery() throws Exception {

		// get table names
		int modelID = input_.getEquivalentStress().getParentItem().getParentItem().getID();
		String stressTable = "ac_eq_stresses_" + modelID;

		// select stresses
		String sql = "select mission, ";
		EquivalentStressRatioType ratioType = input_.getRatioType();
		if (ratioType.equals(EquivalentStressRatioType.FATIGUE_RATIO)) {
			sql += "fat_stress/? as ratio";
		}
		else if (ratioType.equals(EquivalentStressRatioType.PROPAGATION_RATIO)) {
			sql += "prop_stress/? as ratio";
		}
		sql += " from " + stressTable;

		// add equivalent stress name and mission criteria
		sql += " where name = '" + input_.getEquivalentStress().getName() + "'";
		sql += " and eid = ?";

		// order factors
		sql += " order by ratio";
		sql += input_.getOrder() ? " desc" : " asc";

		// return query
		return sql;
	}

	/**
	 * Creates query with element grouping.
	 *
	 * @return Query.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String createBasisMissionQueryWithoutGrouping() throws Exception {

		// get table names
		int modelID = input_.getEquivalentStress().getParentItem().getParentItem().getID();
		String elTable = "elements_" + modelID;
		String stressTable = "ac_eq_stresses_" + modelID;

		// select stresses
		String sql = "select " + stressTable + ".eid, ";
		EquivalentStressRatioType ratioType = input_.getRatioType();
		if (ratioType.equals(EquivalentStressRatioType.FATIGUE_RATIO)) {
			sql += "fat_stress as stress";
		}
		else if (ratioType.equals(EquivalentStressRatioType.PROPAGATION_RATIO)) {
			sql += "prop_stress as stress";
		}
		sql += " from " + stressTable;

		// join with elements
		sql += " inner join " + elTable + " on ";
		sql += stressTable + ".eid = ";
		sql += elTable + ".eid";

		// add equivalent stress name and mission criteria
		sql += " where name = '" + input_.getEquivalentStress().getName() + "'";
		sql += " and mission = '" + input_.getBasisMission() + "'";

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
		sql += " order by stress";
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
	private String createBasisMissionQueryWithGrouping(Statement statement) throws Exception {

		// get element group IDs
		ArrayList<Integer> groupIDs = getGroupIDs(statement);

		// get table names
		int modelID = input_.getEquivalentStress().getParentItem().getParentItem().getID();
		String elTable = "elements_" + modelID;
		String grpTable = "element_groups_" + modelID;
		String stressTable = "ac_eq_stresses_" + modelID;

		// select stresses
		String sql = "select " + stressTable + ".eid, ";
		EquivalentStressRatioType ratioType = input_.getRatioType();
		if (ratioType.equals(EquivalentStressRatioType.FATIGUE_RATIO)) {
			sql += "fat_stress as stress";
		}
		else if (ratioType.equals(EquivalentStressRatioType.PROPAGATION_RATIO)) {
			sql += "prop_stress as stress";
		}
		sql += " from " + stressTable;

		// join with groups
		sql += " inner join " + grpTable + " on ";
		sql += stressTable + ".eid = ";
		sql += grpTable + ".eid";

		// join with elements
		sql += " inner join " + elTable + " on ";
		sql += stressTable + ".eid = ";
		sql += elTable + ".eid";

		// add equivalent stress name and mission criteria
		sql += " where name = '" + input_.getEquivalentStress().getName() + "'";
		sql += " and mission = '" + input_.getBasisMission() + "'";

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
		sql += " order by stress";
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
		String sql = "select group_id from element_group_names_" + input_.getEquivalentStress().getParentItem().getParentItem().getID();
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
