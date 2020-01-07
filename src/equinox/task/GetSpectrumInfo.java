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
import equinox.data.fileType.Spectrum;
import equinox.data.ui.TableItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;

/**
 * Class for get spectrum info task.
 *
 * @author Murat Artim
 * @date Apr 28, 2014
 * @time 3:45:45 PM
 */
public class GetSpectrumInfo extends InternalEquinoxTask<ArrayList<TreeItem<TableItem>>> implements ShortRunningTask {

	/** Spectrum. */
	private final Spectrum spectrum_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("#.###");

	/**
	 * Creates get spectrum info task.
	 *
	 * @param spectrum
	 *            Spectrum to get info.
	 */
	public GetSpectrumInfo(Spectrum spectrum) {
		spectrum_ = spectrum;
	}

	@Override
	public String getTaskTitle() {
		return "Get spectrum info for '" + spectrum_.getName() + "'";
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
		updateTitle("Retrieving spectrum info for '" + spectrum_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get basic CDF info
				String sql = "select * from cdf_sets where set_id = " + spectrum_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						list.add(new TreeItem<>(new TableItem("Spectrum name", resultSet.getString("name"))));
						list.add(new TreeItem<>(new TableItem("A/C program", resultSet.getString("ac_program"))));
						list.add(new TreeItem<>(new TableItem("A/C section", resultSet.getString("ac_section"))));
						list.add(new TreeItem<>(new TableItem("Fatigue mission", resultSet.getString("fat_mission"))));
						String item = resultSet.getString("fat_mission_issue");
						list.add(new TreeItem<>(new TableItem("Fatigue mission issue", (item == null) || item.isEmpty() ? "-" : item)));
						item = resultSet.getString("flp_issue");
						list.add(new TreeItem<>(new TableItem("FLP issue", (item == null) || item.isEmpty() ? "-" : item)));
						item = resultSet.getString("iflp_issue");
						list.add(new TreeItem<>(new TableItem("IFLP issue", (item == null) || item.isEmpty() ? "-" : item)));
						item = resultSet.getString("cdf_issue");
						list.add(new TreeItem<>(new TableItem("CDF issue", (item == null) || item.isEmpty() ? "-" : item)));
						item = resultSet.getString("delivery_ref");
						list.add(new TreeItem<>(new TableItem("Delivery reference", (item == null) || item.isEmpty() ? "-" : item)));
						item = resultSet.getString("description");
						list.add(new TreeItem<>(new TableItem("Description", (item == null) || item.isEmpty() ? "-" : item)));
					}
				}

				// get mission parameters
				TreeItem<TableItem> missionParameters = new TreeItem<>(new TableItem("Fatigue mission parameters", ""));
				sql = "select name, val from cdf_mission_parameters where cdf_id = " + spectrum_.getID() + " order by name";
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						missionParameters.getChildren().add(new TreeItem<>(new TableItem(resultSet.getString("name"), format_.format(resultSet.getDouble("val")))));
					}
				}
				if (!missionParameters.getChildren().isEmpty()) {
					list.add(8, missionParameters);
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
			panel.showInfoView(false, spectrum_);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
