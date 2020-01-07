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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.InfoViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.fileType.AircraftLinearEquivalentStress;
import equinox.data.fileType.AircraftPreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.data.ui.TableItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;

/**
 * Class for get A/C model equivalent stress info task.
 *
 * @author Murat Artim
 * @date Sep 8, 2015
 * @time 11:50:14 AM
 */
public class GetAircraftEquivalentStressInfo extends InternalEquinoxTask<ArrayList<TreeItem<TableItem>>> implements ShortRunningTask {

	/** Equivalent stress. */
	private final SpectrumItem equivalentStress_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("#.###");

	/**
	 * Creates get A/C model equivalent stress info task.
	 *
	 * @param equivalentStress
	 *            Equivalent stress.
	 */
	public GetAircraftEquivalentStressInfo(SpectrumItem equivalentStress) {
		equivalentStress_ = equivalentStress;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get A/C equivalent stress info for '" + equivalentStress_.getName() + "'";
	}

	@Override
	protected ArrayList<TreeItem<TableItem>> call() throws Exception {

		// create info list
		ArrayList<TreeItem<TableItem>> list = new ArrayList<>();

		// update progress info
		updateTitle("Retrieving equivalent stress info for '" + equivalentStress_.getName() + "'");

		// add basic info
		list.add(new TreeItem<>(new TableItem("Equivalent stress name", equivalentStress_.getName())));

		// fatigue
		if (equivalentStress_ instanceof AircraftFatigueEquivalentStress) {
			list.add(new TreeItem<>(new TableItem("Equivalent stress type", "Fatigue")));
			list.add(new TreeItem<>(new TableItem("A/C program", ((AircraftFatigueEquivalentStress) equivalentStress_).getParentItem().getParentItem().getProgram())));
		}

		// preffas
		else if (equivalentStress_ instanceof AircraftPreffasEquivalentStress) {
			list.add(new TreeItem<>(new TableItem("Equivalent stress type", "Preffas propagation")));
			list.add(new TreeItem<>(new TableItem("A/C program", ((AircraftPreffasEquivalentStress) equivalentStress_).getParentItem().getParentItem().getProgram())));
		}

		// linear
		else if (equivalentStress_ instanceof AircraftLinearEquivalentStress) {
			list.add(new TreeItem<>(new TableItem("Equivalent stress type", "Linear propagation")));
			list.add(new TreeItem<>(new TableItem("A/C program", ((AircraftLinearEquivalentStress) equivalentStress_).getParentItem().getParentItem().getProgram())));
		}

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get number of elements
				String sql = "select count(distinct eid) as numel from ac_eq_stresses_" + equivalentStress_.getParentItem().getID();
				sql += " where id = " + equivalentStress_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						list.add(new TreeItem<>(new TableItem("Number of elements", Integer.toString(resultSet.getInt("numel")))));
					}
				}

				// create stress info node
				TreeItem<TableItem> stressInfo = new TreeItem<>(new TableItem("Equivalent stress info", ""));
				list.add(stressInfo);

				// prepare statement for getting equivalent stress info
				sql = "select mission, max(stress) as maxstress, min(stress) as minstress";
				sql += " from ac_eq_stresses_" + equivalentStress_.getParentItem().getID();
				sql += " where id = " + equivalentStress_.getID() + " group by mission";
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {

						// get values
						String mission = resultSet.getString("mission");
						double max = resultSet.getDouble("maxstress");
						double min = resultSet.getDouble("minstress");

						// add to list
						TreeItem<TableItem> missionInfo = new TreeItem<>(new TableItem(mission, ""));
						stressInfo.getChildren().add(missionInfo);
						missionInfo.getChildren().add(new TreeItem<>(new TableItem("Maximum eq. stress", format_.format(max))));
						missionInfo.getChildren().add(new TreeItem<>(new TableItem("Minimum eq. stress", format_.format(min))));
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
			panel.showInfoView(false, equivalentStress_);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
