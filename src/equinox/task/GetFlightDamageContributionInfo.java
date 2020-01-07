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

import java.math.RoundingMode;
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
import equinox.data.fileType.FlightDamageContributions;
import equinox.data.ui.TableItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;

/**
 * Class for get typical flight damage contributions info task.
 *
 * @author Murat Artim
 * @date 20 Oct 2016
 * @time 14:16:12
 */
public class GetFlightDamageContributionInfo extends InternalEquinoxTask<ArrayList<TreeItem<TableItem>>> implements ShortRunningTask {

	/** Damage angle item. */
	private final FlightDamageContributions contributions_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.##"), format2_ = new DecimalFormat("0");

	/**
	 * Creates get damage contributions info task.
	 *
	 * @param contributions
	 *            Damage contributions item.
	 */
	public GetFlightDamageContributionInfo(FlightDamageContributions contributions) {
		contributions_ = contributions;
		format2_.setRoundingMode(RoundingMode.FLOOR);
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get typical flight damage contributions info for '" + contributions_.getName() + "'";
	}

	@Override
	protected ArrayList<TreeItem<TableItem>> call() throws Exception {

		// update progress info
		updateTitle("Retrieving contributions info '" + contributions_.getName() + "'");

		// create list
		ArrayList<TreeItem<TableItem>> list = new ArrayList<>();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// create spectrum generation input item
				TreeItem<TableItem> specGen = new TreeItem<>(new TableItem("Spectrum generation inputs", ""));

				// get all lines of the conversion table
				String sql = "select * from flight_dam_contributions where id = " + contributions_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {

						// STF file name
						list.add(new TreeItem<>(new TableItem("STF file", contributions_.getParentItem().getName())));

						// spectrum info
						TreeItem<TableItem> spectrum = new TreeItem<>(new TableItem("Spectrum info", ""));
						spectrum.getChildren().add(new TreeItem<>(new TableItem("A/C program", contributions_.getParentItem().getParentItem().getProgram())));
						spectrum.getChildren().add(new TreeItem<>(new TableItem("A/C section", contributions_.getParentItem().getParentItem().getSection())));
						spectrum.getChildren().add(new TreeItem<>(new TableItem("Fatigue mission", contributions_.getParentItem().getMission())));
						list.add(spectrum);

						// stress rotation
						String stressComp = resultSet.getString("stress_comp");
						TreeItem<TableItem> stress = new TreeItem<>(new TableItem("Stress component", stressComp));
						specGen.getChildren().add(stress);
						if (stressComp.equals(StressComponent.ROTATED.toString())) {
							stress.getChildren().add(new TreeItem<>(new TableItem("Rotation angle", format_.format(Math.toDegrees(resultSet.getDouble("rotation_angle"))))));
						}

						// delta-p pressure info
						TreeItem<TableItem> deltaP = new TreeItem<>(new TableItem("Pressure", ""));
						String value = resultSet.getString("dp_lc");
						deltaP.getChildren().add(new TreeItem<>(new TableItem("DP load case", value == null ? "N/A" : value)));
						deltaP.getChildren().add(new TreeItem<>(new TableItem("Reference DP", format_.format(resultSet.getDouble("ref_dp")))));
						specGen.getChildren().add(deltaP);

						// delta-t temperature info
						TreeItem<TableItem> deltaT = new TreeItem<>(new TableItem("Temperature", ""));
						value = resultSet.getString("dt_lc_sup");
						deltaT.getChildren().add(new TreeItem<>(new TableItem("DT load case (sup.)", value == null ? "N/A" : value)));
						deltaT.getChildren().add(new TreeItem<>(new TableItem("Reference DT (sup.)", value == null ? "N/A" : format_.format(resultSet.getDouble("ref_dt_sup")))));
						value = resultSet.getString("dt_lc_inf");
						deltaT.getChildren().add(new TreeItem<>(new TableItem("DT load case (inf.)", value == null ? "N/A" : value)));
						deltaT.getChildren().add(new TreeItem<>(new TableItem("Reference DT (inf.)", value == null ? "N/A" : format_.format(resultSet.getDouble("ref_dt_inf")))));
						specGen.getChildren().add(deltaT);

						// overall stress factors
						TreeItem<TableItem> overallStressModifiers = new TreeItem<>(new TableItem("Overall stress factors", ""));
						overallStressModifiers.getChildren().add(new TreeItem<>(new TableItem("1g stress modifier", resultSet.getString("oneg_fac"))));
						overallStressModifiers.getChildren().add(new TreeItem<>(new TableItem("Increment stress modifier", resultSet.getString("inc_fac"))));
						overallStressModifiers.getChildren().add(new TreeItem<>(new TableItem("Delta-p stress modifier", resultSet.getString("dp_fac"))));
						overallStressModifiers.getChildren().add(new TreeItem<>(new TableItem("Delta-t stress modifier", resultSet.getString("dt_fac"))));
						specGen.getChildren().add(overallStressModifiers);
						list.add(specGen);

						// rainflow info
						TreeItem<TableItem> rainflow = new TreeItem<>(new TableItem("Rainflow info", ""));
						rainflow.getChildren().add(new TreeItem<>(new TableItem("Remove negative stresses", resultSet.getBoolean("remove_negative") ? "Yes" : "No")));
						double omissionLevel = resultSet.getDouble("omission_level");
						rainflow.getChildren().add(new TreeItem<>(new TableItem("Omission level", omissionLevel == -1 ? "No omission applied." : format_.format(omissionLevel))));
						rainflow.getChildren().add(new TreeItem<>(new TableItem("Total number of flights", (int) resultSet.getDouble("validity") + "")));
						list.add(rainflow);

						// material info
						TreeItem<TableItem> material = new TreeItem<>(new TableItem("Material info", ""));
						material.getChildren().add(new TreeItem<>(new TableItem("Name", resultSet.getString("material_name"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Specification", resultSet.getString("material_specification"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Library version", resultSet.getString("material_library_version"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Family", resultSet.getString("material_family"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Orientation", resultSet.getString("material_orientation"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Configuration", resultSet.getString("material_configuration"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material slope (p)", format_.format(resultSet.getDouble("material_p")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material constant (q)", format_.format(resultSet.getDouble("material_q")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material constant (M)", format_.format(resultSet.getDouble("material_m")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material ISAMI version", resultSet.getString("material_isami_version"))));
						list.add(material);
					}
				}

				// get loadcase factors
				sql = "select * from flight_dam_contributions_event_modifiers where id = " + contributions_.getID() + " order by loadcase_number asc";
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					TreeItem<TableItem> eventModifiers = new TreeItem<>(new TableItem("Loadcase factors", ""));
					while (resultSet.next()) {
						String loadcaseNumber = resultSet.getString("loadcase_number");
						String eventName = resultSet.getString("event_name");
						String comments = resultSet.getString("comment");
						TreeItem<TableItem> loadcase = new TreeItem<>(new TableItem("Loadcase '" + loadcaseNumber + "'", eventName + (comments == null ? "" : " (" + comments + ")")));
						loadcase.getChildren().add(new TreeItem<>(new TableItem("Stress modifier", format_.format(resultSet.getDouble("value")) + " (" + resultSet.getString("method") + ")")));
						eventModifiers.getChildren().add(loadcase);
					}
					if (!eventModifiers.getChildren().isEmpty()) {
						specGen.getChildren().add(eventModifiers);
					}
				}

				// get segment factors
				sql = "select * from flight_dam_contributions_segment_modifiers where id = " + contributions_.getID() + " order by segment_number asc";
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					TreeItem<TableItem> segmentModifiers = new TreeItem<>(new TableItem("Segment factors", ""));
					while (resultSet.next()) {
						String segmentName = resultSet.getString("segment_name") + " (" + resultSet.getInt("segment_number") + ")";
						TreeItem<TableItem> segment = new TreeItem<>(new TableItem("Segment '" + segmentName + "'", ""));
						segment.getChildren().add(new TreeItem<>(new TableItem("1G stress modifier", format_.format(resultSet.getDouble("oneg_value")) + " (" + resultSet.getString("oneg_method") + ")")));
						segment.getChildren().add(new TreeItem<>(new TableItem("Increment stress modifier", format_.format(resultSet.getDouble("inc_value")) + " (" + resultSet.getString("inc_method") + ")")));
						segment.getChildren().add(new TreeItem<>(new TableItem("Delta-p stress modifier", format_.format(resultSet.getDouble("dp_value")) + " (" + resultSet.getString("dp_method") + ")")));
						segment.getChildren().add(new TreeItem<>(new TableItem("Delta-t stress modifier", format_.format(resultSet.getDouble("dt_value")) + " (" + resultSet.getString("dt_method") + ")")));
						segmentModifiers.getChildren().add(segment);
					}
					if (!segmentModifiers.getChildren().isEmpty()) {
						specGen.getChildren().add(segmentModifiers);
					}
				}

				// create damage contributions input item
				TreeItem<TableItem> continfo = new TreeItem<>(new TableItem("Damage Contributions", ""));

				// with flight occurrences
				TreeItem<TableItem> withOccurrences = new TreeItem<>(new TableItem("With Flight Occurrences", ""));
				sql = "select flight_name, dam_percent from flight_dam_contribution_with_occurrences where id = " + contributions_.getID() + " order by dam_percent desc";
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						withOccurrences.getChildren().add(new TreeItem<>(new TableItem(resultSet.getString("flight_name"), format2_.format(resultSet.getDouble("dam_percent")) + "%")));
					}
					continfo.getChildren().add(withOccurrences);
				}

				// without flight occurrences
				TreeItem<TableItem> withoutOccurrences = new TreeItem<>(new TableItem("Without Flight Occurrences", ""));
				sql = "select flight_name, dam_percent from flight_dam_contribution_without_occurrences where id = " + contributions_.getID() + " order by dam_percent desc";
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						withoutOccurrences.getChildren().add(new TreeItem<>(new TableItem(resultSet.getString("flight_name"), format2_.format(resultSet.getDouble("dam_percent")) + "%")));
					}
					continfo.getChildren().add(withoutOccurrences);
				}

				// add to list
				list.add(1, continfo);
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
			panel.showInfoView(false, contributions_);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
