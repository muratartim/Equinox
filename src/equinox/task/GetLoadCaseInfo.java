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
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.ui.TableItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;

/**
 * Class for get load case info task.
 *
 * @author Murat Artim
 * @date Aug 6, 2015
 * @time 12:08:01 PM
 */
public class GetLoadCaseInfo extends InternalEquinoxTask<ArrayList<TreeItem<TableItem>>> implements ShortRunningTask {

	/** Load case. */
	private final AircraftLoadCase loadCase_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("#.###");

	/**
	 * Creates get load case info task.
	 *
	 * @param loadCase
	 *            Load case.
	 */
	public GetLoadCaseInfo(AircraftLoadCase loadCase) {
		loadCase_ = loadCase;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get load case info for '" + loadCase_.getName() + "'";
	}

	@Override
	protected ArrayList<TreeItem<TableItem>> call() throws Exception {

		// create info list
		ArrayList<TreeItem<TableItem>> list = new ArrayList<>();

		// update progress info
		updateTitle("Retrieving load case info for '" + loadCase_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get load case name and number
				String sql = "select lc_name, lc_num, lc_comments from load_case_names_" + loadCase_.getParentItem().getParentItem().getID();
				sql += " where lc_id = " + loadCase_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						list.add(new TreeItem<>(new TableItem("Load case name", resultSet.getString("lc_name"))));
						list.add(new TreeItem<>(new TableItem("Load case number", Integer.toString(resultSet.getInt("lc_num")))));
						list.add(new TreeItem<>(new TableItem("Comments", resultSet.getString("lc_comments"))));
						list.add(new TreeItem<>(new TableItem("A/C program", loadCase_.getParentItem().getParentItem().getProgram())));
					}
				}

				// get stress info
				sql = "select max(sx) as maxsx, min(sx) as minsx";
				sql += ", max(sy) as maxsy, min(sy) as minsy";
				sql += ", max(sxy) as maxsxy, min(sxy) as minsxy";
				sql += ", count(eid) as eidcount from load_cases_" + loadCase_.getParentItem().getParentItem().getID();
				sql += " where lc_id = " + loadCase_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {

						// add basic info
						list.add(new TreeItem<>(new TableItem("Number of stresses", Integer.toString(resultSet.getInt("eidcount")))));

						// add stress info
						TreeItem<TableItem> stressInfo = new TreeItem<>(new TableItem("Stress info", ""));
						stressInfo.getChildren().add(new TreeItem<>(new TableItem("Maximum SX", format_.format(resultSet.getDouble("maxsx")))));
						stressInfo.getChildren().add(new TreeItem<>(new TableItem("Minimum SX", format_.format(resultSet.getDouble("minsx")))));
						stressInfo.getChildren().add(new TreeItem<>(new TableItem("Maximum SY", format_.format(resultSet.getDouble("maxsy")))));
						stressInfo.getChildren().add(new TreeItem<>(new TableItem("Minimum SY", format_.format(resultSet.getDouble("minsy")))));
						stressInfo.getChildren().add(new TreeItem<>(new TableItem("Maximum SXY", format_.format(resultSet.getDouble("maxsxy")))));
						stressInfo.getChildren().add(new TreeItem<>(new TableItem("Minimum SXY", format_.format(resultSet.getDouble("minsxy")))));
						list.add(stressInfo);
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
			panel.showInfoView(false, loadCase_);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
