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
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.ui.TableItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;

/**
 * Class for getting linear equivalent stress info.
 *
 * @author Murat Artim
 * @date Jul 7, 2014
 * @time 2:57:21 PM
 */
public class GetLinearEquivalentStressInfo extends InternalEquinoxTask<ArrayList<TreeItem<TableItem>>> implements ShortRunningTask {

	/** Linear equivalent stress item. */
	private final LinearEquivalentStress equivalentStress_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.##"), format2_ = new DecimalFormat("0.##E0");

	/**
	 * Creates get linear equivalent stress info task.
	 *
	 * @param equivalentStress
	 *            Linear equivalent stress item.
	 */
	public GetLinearEquivalentStressInfo(LinearEquivalentStress equivalentStress) {
		equivalentStress_ = equivalentStress;
	}

	@Override
	public String getTaskTitle() {
		return "Get linear equivalent stress info for '" + equivalentStress_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected ArrayList<TreeItem<TableItem>> call() throws Exception {

		// update progress info
		updateTitle("Retrieving linear equivalent stress info '" + equivalentStress_.getName() + "'");

		// create list
		ArrayList<TreeItem<TableItem>> list = new ArrayList<>();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get all lines of the conversion table1
				String sql = "select * from linear_equivalent_stresses where id = " + equivalentStress_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {

						// equivalent stress
						list.add(new TreeItem<>(new TableItem("Linear prop. equivalent stress", format_.format(resultSet.getDouble("stress")))));

						// spectrum info
						TreeItem<TableItem> spectrum = new TreeItem<>(new TableItem("Spectrum info", ""));
						spectrum.getChildren().add(new TreeItem<>(new TableItem("A/C program", equivalentStress_.getParentItem().getParentItem().getParentItem().getProgram())));
						spectrum.getChildren().add(new TreeItem<>(new TableItem("A/C section", equivalentStress_.getParentItem().getParentItem().getParentItem().getSection())));
						spectrum.getChildren().add(new TreeItem<>(new TableItem("Fatigue mission", equivalentStress_.getParentItem().getParentItem().getMission())));
						list.add(spectrum);

						// rainflow info
						TreeItem<TableItem> rainflow = new TreeItem<>(new TableItem("Rainflow info", ""));
						rainflow.getChildren().add(new TreeItem<>(new TableItem("Remove negative stresses", resultSet.getBoolean("remove_negative") ? "Yes" : "No")));
						double omissionLevel = resultSet.getDouble("omission_level");
						rainflow.getChildren().add(new TreeItem<>(new TableItem("Omission level", omissionLevel == -1 ? "No omission applied." : format_.format(omissionLevel))));
						rainflow.getChildren().add(new TreeItem<>(new TableItem("Total number of flights", (int) resultSet.getDouble("validity") + "")));
						rainflow.getChildren().add(new TreeItem<>(new TableItem("Maximum stress", format_.format(resultSet.getDouble("max_stress")))));
						rainflow.getChildren().add(new TreeItem<>(new TableItem("Minimum stress", format_.format(resultSet.getDouble("min_stress")))));
						rainflow.getChildren().add(new TreeItem<>(new TableItem("R-ratio", format_.format(resultSet.getDouble("r_ratio")))));
						rainflow.getChildren().add(new TreeItem<>(new TableItem("Total number of peaks", (int) resultSet.getDouble("total_cycles") + "")));
						list.add(rainflow);

						// material info
						TreeItem<TableItem> material = new TreeItem<>(new TableItem("Material info", ""));
						material.getChildren().add(new TreeItem<>(new TableItem("Name", resultSet.getString("material_name"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Specification", resultSet.getString("material_specification"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Library version", resultSet.getString("material_library_version"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Family", resultSet.getString("material_family"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Orientation", resultSet.getString("material_orientation"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Configuration", resultSet.getString("material_configuration"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material constant (Ceff)", format2_.format(resultSet.getDouble("material_ceff")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material constant (m)", format_.format(resultSet.getDouble("material_m")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material constant (A)", format_.format(resultSet.getDouble("material_a")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material constant (B)", format_.format(resultSet.getDouble("material_b")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material constant (C)", format_.format(resultSet.getDouble("material_c")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Ftu", format_.format(resultSet.getDouble("material_ftu")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Fty", format_.format(resultSet.getDouble("material_fty")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material ISAMI version", resultSet.getString("material_isami_version"))));
						list.add(material);
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
			panel.showInfoView(true, equivalentStress_);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
