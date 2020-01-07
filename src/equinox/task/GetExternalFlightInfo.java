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
import equinox.data.fileType.ExternalFlight;
import equinox.data.ui.TableItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;

/**
 * Class for external get flight info task.
 *
 * @author Murat Artim
 * @date Mar 13, 2015
 * @time 11:09:01 AM
 */
public class GetExternalFlightInfo extends InternalEquinoxTask<ArrayList<TreeItem<TableItem>>> implements ShortRunningTask {

	/** File item to get info. */
	private final ExternalFlight flight_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("#.###");

	/**
	 * Creates get external flight info task.
	 *
	 * @param flight
	 *            Currently selected external flight.
	 */
	public GetExternalFlightInfo(ExternalFlight flight) {
		flight_ = flight;
	}

	@Override
	public String getTaskTitle() {
		return "Get info for external flight '" + flight_.getName() + "'";
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
		updateTitle("Retrieving info for external flight '" + flight_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get flight info
				getFlightInfo(statement, list);
			}
		}

		// return list
		return list;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set flight info
		try {
			InfoViewPanel panel = (InfoViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getInfoTable().getRoot().getChildren().setAll(get());
			panel.showInfoView(false, flight_);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Retrieves and returns info for the selected flight.
	 *
	 * @param statement
	 *            SQL statement.
	 * @param list
	 *            List to store the flight info.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getFlightInfo(Statement statement, ArrayList<TreeItem<TableItem>> list) throws Exception {
		updateMessage("Please wait...");
		String sql = "select * from ext_sth_flights where flight_id = " + flight_.getID() + " and name = '" + flight_.getName() + "'";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				list.add(new TreeItem<>(new TableItem("Flight name", resultSet.getString("name"))));
				list.add(new TreeItem<>(new TableItem("A/C program", flight_.getParentItem().getParentItem().getProgram())));
				list.add(new TreeItem<>(new TableItem("A/C section", flight_.getParentItem().getParentItem().getSection())));
				list.add(new TreeItem<>(new TableItem("Fatigue mission", flight_.getParentItem().getParentItem().getMission())));
				String severity = resultSet.getString("severity");
				if (!severity.isEmpty()) {
					list.add(new TreeItem<>(new TableItem("Severity", severity)));
				}
				list.add(new TreeItem<>(new TableItem("Number of peaks", Integer.toString(resultSet.getInt("num_peaks")))));
				list.add(new TreeItem<>(new TableItem("Occurrence", format_.format(resultSet.getDouble("validity")))));
				list.add(new TreeItem<>(new TableItem("Block size", format_.format(resultSet.getDouble("block_size")))));
				TreeItem<TableItem> stress = new TreeItem<>(new TableItem("Stress info", ""));
				stress.getChildren().add(new TreeItem<>(new TableItem("Maximum stress", format_.format(resultSet.getDouble("max_val")))));
				stress.getChildren().add(new TreeItem<>(new TableItem("Minimum stress", format_.format(resultSet.getDouble("min_val")))));
				list.add(stress);
			}
		}
	}
}
