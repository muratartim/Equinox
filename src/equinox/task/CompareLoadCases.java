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
import java.util.concurrent.ExecutionException;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.Equinox;
import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.ElementStress;
import equinox.data.ElementType;
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.input.CompareLoadCasesInput;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for compare load cases task.
 *
 * @author Murat Artim
 * @date Apr 7, 2016
 * @time 11:06:19 AM
 */
public class CompareLoadCases extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask {

	/** Input. */
	private final CompareLoadCasesInput input_;

	/**
	 * Creates compare load cases task.
	 *
	 * @param input
	 *            Input.
	 */
	public CompareLoadCases(CompareLoadCasesInput input) {
		input_ = input;
	}

	@Override
	public String getTaskTitle() {
		return "Compare load cases";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_LOADCASE_COMPARISON);

		// update progress info
		updateTitle("Comparing load cases...");

		// get stress component
		ElementStress stressComp = input_.getStressComponent();

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement to get element stresses
			int modelID = input_.getLoadCases().get(0).getParentItem().getParentItem().getID();
			String sql = "select " + stressComp.getDBSelectString() + " from load_cases_" + modelID;
			sql += " where lc_id = ? and eid = ?";
			try (PreparedStatement statement = connection.prepareStatement(sql)) {

				// prepare statement to get element type
				sql = "select el_type from elements_" + modelID + " where eid = ?";
				try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

					// loop over load cases
					for (AircraftLoadCase loadCase : input_.getLoadCases()) {

						// create series
						String loadCaseName = loadCase.getName();

						// set load case ID
						statement.setInt(1, loadCase.getID());

						// loop over element IDs
						nextElement: for (int eid : input_.getEIDs()) {

							// check element type
							if (stressComp.equals(ElementStress.MAX_PRINCIPAL) || stressComp.equals(ElementStress.MIN_PRINCIPAL)) {
								statement2.setInt(1, eid);
								try (ResultSet resultSet = statement2.executeQuery()) {
									while (resultSet.next()) {
										String elType = resultSet.getString("el_type");
										if (elType.equals(ElementType.BEAM) || elType.equals(ElementType.ROD)) {
											continue nextElement;
										}
									}
								}
							}

							// execute statement to get stresses
							statement.setInt(2, eid);
							try (ResultSet resultSet = statement.executeQuery()) {
								while (resultSet.next()) {
									double stress = resultSet.getDouble(stressComp.getResultSetColName());
									dataset.addValue(stress, loadCaseName, Integer.toString(eid));
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
			panel.setPlotData(dataset, "Load Case Comparison", null, "Element", input_.getStressComponent().getName(), true, input_.showDataLabels(), false);

			// show column chart plot panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
