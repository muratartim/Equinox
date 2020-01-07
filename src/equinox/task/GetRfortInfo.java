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
import equinox.data.StressComponent;
import equinox.data.fileType.Rfort;
import equinox.data.ui.RfortOmission;
import equinox.data.ui.TableItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;

/**
 * Class for get RFORT info task.
 *
 * @author Murat Artim
 * @date Mar 10, 2016
 * @time 12:16:18 PM
 */
public class GetRfortInfo extends InternalEquinoxTask<ArrayList<TreeItem<TableItem>>> implements ShortRunningTask {

	/** RFORT file. */
	private final Rfort rfort_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("#.#####"), format2_ = new DecimalFormat("#.##");

	/**
	 * Creates get RFORT info task.
	 *
	 * @param rfort
	 *            RFORT file to get info.
	 */
	public GetRfortInfo(Rfort rfort) {
		rfort_ = rfort;
	}

	@Override
	public String getTaskTitle() {
		return "Get RFORT info for '" + rfort_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected ArrayList<TreeItem<TableItem>> call() throws Exception {

		// create info list
		ArrayList<TreeItem<TableItem>> list = new ArrayList<>();

		// update progress info
		updateTitle("Retrieving RFORT info for '" + rfort_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get basic RFORT info
				String sql = "select * from rfort_analyses where id = " + rfort_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {

						// spectrum name
						list.add(new TreeItem<>(new TableItem("Input spectrum", resultSet.getString("input_spectrum_name"))));

						// equivalent stress analyses
						String analyses = resultSet.getBoolean("fatigue_analysis") ? "Fatigue" : "";
						analyses += resultSet.getBoolean("preffas_analysis") ? ", Preffas" : "";
						analyses += resultSet.getBoolean("linear_analysis") ? ", Linear prop." : "";
						list.add(new TreeItem<>(new TableItem("Eq. stress analyses", analyses)));

						// input options
						TreeItem<TableItem> options = new TreeItem<>(new TableItem("Input options", ""));
						list.add(options);
						options.getChildren().add(new TreeItem<>(new TableItem("Add Delta-P to stress sequences", resultSet.getBoolean("add_dp") ? "Yes" : "No")));
						options.getChildren().add(new TreeItem<>(new TableItem("Reference Delta-P", format_.format(resultSet.getDouble("ref_dp")))));
						options.getChildren().add(new TreeItem<>(new TableItem("Delta-P factor", format_.format(resultSet.getDouble("dp_factor")))));
						options.getChildren().add(new TreeItem<>(new TableItem("Overall factor", format_.format(resultSet.getDouble("overall_factor")))));
						String stressComp = resultSet.getString("stress_comp");
						options.getChildren().add(new TreeItem<>(new TableItem("Stress component", stressComp)));
						if (stressComp.equals(StressComponent.ROTATED.toString())) {
							options.getChildren().add(new TreeItem<>(new TableItem("Rotation angle", Integer.toString(resultSet.getInt("rotation_angle")))));
						}
						String runTillFlight = resultSet.getString("run_till_flight");
						options.getChildren().add(new TreeItem<>(new TableItem("Run till flight", runTillFlight == null ? "All flights" : runTillFlight)));
						String targetFlights = resultSet.getString("target_flights");
						options.getChildren().add(new TreeItem<>(new TableItem("Target flights", targetFlights == null ? "All flights" : targetFlights)));
					}
				}

				// get pilot point info
				TreeItem<TableItem> ppInfo = new TreeItem<>(new TableItem("Pilot points", ""));
				list.add(ppInfo);
				sql = "select pp_name, included_in_rfort, stress_factor, material_name, stress_type from rfort_outputs where omission_name = '" + RfortOmission.INITIAL_ANALYSIS + "' and analysis_id = " + rfort_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {

						// get pilot point name
						String ppName = resultSet.getString("pp_name");

						// get pilot point
						TreeItem<TableItem> pp = null;
						for (TreeItem<TableItem> item : ppInfo.getChildren()) {
							if (item.getValue().getLabel().equals(ppName)) {
								pp = item;
								break;
							}
						}
						if (pp == null) {
							pp = new TreeItem<>(new TableItem(ppName, ""));
							ppInfo.getChildren().add(pp);
							pp.getChildren().add(new TreeItem<>(new TableItem("Included in RFORT", resultSet.getBoolean("included_in_rfort") ? "Yes" : "No")));
							pp.getChildren().add(new TreeItem<>(new TableItem("Stress factor", format_.format(resultSet.getDouble("stress_factor")))));
						}

						// add material name
						String header = null;
						String stressType = resultSet.getString("stress_type");
						if (stressType.equals(SaveRfortInfo.FATIGUE)) {
							header = "Fatigue material";
						}
						else if (stressType.equals(SaveRfortInfo.PREFFAS)) {
							header = "Preffas material";
						}
						else if (stressType.equals(SaveRfortInfo.LINEAR)) {
							header = "Linear propagation material";
						}
						pp.getChildren().add(new TreeItem<>(new TableItem(header, resultSet.getString("material_name"))));
					}
				}

				// get omission info
				TreeItem<TableItem> omissionInfo = new TreeItem<>(new TableItem("Omissions", ""));
				list.add(omissionInfo);
				sql = "select omission_name, omission_value, pp_name, included_in_rfort from rfort_outputs where analysis_id = " + rfort_.getID() + " and stress_type = '" + SaveRfortInfo.FATIGUE + "'";
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {

						// get omission name
						String omissionName = resultSet.getString("omission_name");

						// initial analysis
						if (omissionName.equals(RfortOmission.INITIAL_ANALYSIS)) {
							continue;
						}

						// not included in RFORT
						if (!resultSet.getBoolean("included_in_rfort")) {
							continue;
						}

						// get omission
						TreeItem<TableItem> omission = null;
						for (TreeItem<TableItem> item : omissionInfo.getChildren()) {
							if (item.getValue().getLabel().equals(omissionName)) {
								omission = item;
								break;
							}
						}
						if (omission == null) {
							omission = new TreeItem<>(new TableItem(omissionName, ""));
							omissionInfo.getChildren().add(omission);
						}

						// add pilot point omission value
						omission.getChildren().add(new TreeItem<>(new TableItem(resultSet.getString("pp_name"), format2_.format(resultSet.getDouble("omission_value")))));
					}
				}
			}
		}

		// return list
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
			panel.showInfoView(false, rfort_);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
