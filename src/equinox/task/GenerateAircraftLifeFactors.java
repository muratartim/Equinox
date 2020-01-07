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
import equinox.data.input.CompareAircraftLifeFactorsInput;
import equinox.data.input.LifeFactorType;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for generate A/C model life factors task.
 *
 * @author Murat Artim
 * @date Apr 7, 2016
 * @time 11:29:28 AM
 */
public class GenerateAircraftLifeFactors extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask {

	/** Input. */
	private final CompareAircraftLifeFactorsInput input_;

	/**
	 * Creates generate A/C model life factors task.
	 *
	 * @param input
	 *            Input.
	 */
	public GenerateAircraftLifeFactors(CompareAircraftLifeFactorsInput input) {
		input_ = input;
	}

	@Override
	public String getTaskTitle() {
		return "Generate life factors of '" + input_.getEquivalentStress().getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// check permission
		checkPermission(Permission.GENERATE_LIFE_FACTORS);

		// update progress info
		updateTitle("Generating life factors of '" + input_.getEquivalentStress().getName() + "'...");

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
							double matpar = getBasisMission.getDouble("matpar");

							// execute query for other missions
							statement1.setDouble(1, stress);
							statement1.setDouble(2, matpar);
							statement1.setInt(3, eid);
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
									double lifeFactor = getOtherMission.getDouble("lifefactor");
									dataset.addValue(lifeFactor, mission, eidString);
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
			String title = input_.getFactorType().toString() + " Comparison";
			String subTitle = "Based on mission '" + input_.getBasisMission() + "'";
			panel.setPlotData(dataset, title, subTitle, "Element", input_.getFactorType().toString(), true, input_.getLabelDisplay(), false);

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
		LifeFactorType factorType = input_.getFactorType();
		if (factorType.equals(LifeFactorType.FATIGUE_LIFE_FACTOR)) {
			sql += "(power(?/fat_stress, ?)) as lifeFactor";
		}
		else if (factorType.equals(LifeFactorType.PROPAGATION_LIFE_FACTOR)) {
			sql += "(power(?/prop_stress, ?)) as lifeFactor";
		}
		sql += " from " + stressTable;

		// add equivalent stress name and mission criteria
		sql += " where name = '" + input_.getEquivalentStress().getName() + "'";
		sql += " and eid = ?";

		// order factors
		sql += " order by lifeFactor";
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
		LifeFactorType factorType = input_.getFactorType();
		if (factorType.equals(LifeFactorType.FATIGUE_LIFE_FACTOR)) {
			sql += "fat_stress as stress, fat_p as matpar";
		}
		else if (factorType.equals(LifeFactorType.PROPAGATION_LIFE_FACTOR)) {
			sql += "prop_stress as stress, elber_m as matpar";
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
		LifeFactorType factorType = input_.getFactorType();
		if (factorType.equals(LifeFactorType.FATIGUE_LIFE_FACTOR)) {
			sql += "fat_stress as stress, fat_p as matpar";
		}
		else if (factorType.equals(LifeFactorType.PROPAGATION_LIFE_FACTOR)) {
			sql += "prop_stress as stress, elber_m as matpar";
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
