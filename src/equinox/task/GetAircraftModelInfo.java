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

import equinox.Equinox;
import equinox.controller.InfoViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.AircraftModel;
import equinox.data.ui.TableItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;

/**
 * Class for get aircraft model info task.
 *
 * @author Murat Artim
 * @date Jul 7, 2015
 * @time 4:56:06 PM
 */
public class GetAircraftModelInfo extends InternalEquinoxTask<ArrayList<TreeItem<TableItem>>> implements ShortRunningTask {

	/** A/C model. */
	private final AircraftModel model_;

	/**
	 * Creates get aircraft model info task.
	 *
	 * @param model
	 *            A/C model.
	 */
	public GetAircraftModelInfo(AircraftModel model) {
		model_ = model;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get aircraft model info for '" + model_.getName() + "'";
	}

	@Override
	protected ArrayList<TreeItem<TableItem>> call() throws Exception {

		// create info list
		ArrayList<TreeItem<TableItem>> list = new ArrayList<>();

		// update progress info
		updateTitle("Retrieving aircraft model info for '" + model_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get basic CDF info
				String sql = "select ac_program, name, delivery_ref, description, num_elems, num_grids, num_quads, num_rods, num_beams, num_trias, num_shears from ac_models where model_id = " + model_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {

						// add program and number of grids
						list.add(new TreeItem<>(new TableItem("A/C program", resultSet.getString("ac_program"))));
						list.add(new TreeItem<>(new TableItem("A/C model name", resultSet.getString("name"))));
						String item = resultSet.getString("delivery_ref");
						list.add(new TreeItem<>(new TableItem("Delivery reference", (item == null) || item.isEmpty() ? "-" : item)));
						item = resultSet.getString("description");
						list.add(new TreeItem<>(new TableItem("Description", (item == null) || item.isEmpty() ? "-" : item)));
						list.add(new TreeItem<>(new TableItem("Number of grids", Integer.toString(resultSet.getInt("num_grids")))));

						// add number of elements
						TreeItem<TableItem> numElements = new TreeItem<>(new TableItem("Number of elements", Integer.toString(resultSet.getInt("num_elems"))));
						numElements.getChildren().add(new TreeItem<>(new TableItem("Number of QUAD elements", Integer.toString(resultSet.getInt("num_quads")))));
						numElements.getChildren().add(new TreeItem<>(new TableItem("Number of TRIA elements", Integer.toString(resultSet.getInt("num_trias")))));
						numElements.getChildren().add(new TreeItem<>(new TableItem("Number of BEAM elements", Integer.toString(resultSet.getInt("num_beams")))));
						numElements.getChildren().add(new TreeItem<>(new TableItem("Number of ROD elements", Integer.toString(resultSet.getInt("num_rods")))));
						numElements.getChildren().add(new TreeItem<>(new TableItem("Number of SHEAR elements", Integer.toString(resultSet.getInt("num_shears")))));
						list.add(numElements);
					}
				}

				// get element groups
				sql = "select * from element_group_names_" + model_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					TreeItem<TableItem> groups = new TreeItem<>(new TableItem("Element groups", ""));
					list.add(groups);
					while (resultSet.next()) {
						String name = resultSet.getString("name");
						groups.getChildren().add(new TreeItem<>(new TableItem(name, Integer.toString(resultSet.getInt("numel")))));
					}
				}
			}
		}

		// return info
		return list;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {
			InfoViewPanel panel = (InfoViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getInfoTable().getRoot().getChildren().setAll(get());
			panel.showInfoView(false, model_);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
