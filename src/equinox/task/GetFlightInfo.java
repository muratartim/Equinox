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
import equinox.data.fileType.Flight;
import equinox.data.ui.TableItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;

/**
 * Class for get STH flight info task.
 *
 * @author Murat Artim
 * @date Dec 26, 2013
 * @time 12:49:55 PM
 */
public class GetFlightInfo extends InternalEquinoxTask<ArrayList<TreeItem<TableItem>>> implements ShortRunningTask {

	/** File item to get info. */
	private final Flight flight_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("#.###");

	/**
	 * Creates get STH flight info task.
	 *
	 * @param flight
	 *            Currently selected flight.
	 */
	public GetFlightInfo(Flight flight) {
		flight_ = flight;
	}

	@Override
	public String getTaskTitle() {
		return "Get info for flight '" + flight_.getName() + "'";
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
		updateTitle("Retrieving info for flight '" + flight_.getName() + "'");

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
		String sql = "select * from sth_flights where flight_id = " + flight_.getID() + " and name = '" + flight_.getName() + "'";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				list.add(new TreeItem<>(new TableItem("Flight name", resultSet.getString("name"))));
				list.add(new TreeItem<>(new TableItem("A/C program", flight_.getParentItem().getParentItem().getParentItem().getParentItem().getProgram())));
				list.add(new TreeItem<>(new TableItem("A/C section", flight_.getParentItem().getParentItem().getParentItem().getParentItem().getSection())));
				list.add(new TreeItem<>(new TableItem("Fatigue mission", flight_.getParentItem().getParentItem().getParentItem().getMission())));
				list.add(new TreeItem<>(new TableItem("Severity", resultSet.getString("severity"))));
				list.add(new TreeItem<>(new TableItem("Number of peaks", Integer.toString(resultSet.getInt("num_peaks")))));
				list.add(new TreeItem<>(new TableItem("Occurrence", format_.format(resultSet.getDouble("validity")))));
				list.add(new TreeItem<>(new TableItem("Block size", format_.format(resultSet.getDouble("block_size")))));
				TreeItem<TableItem> stress = new TreeItem<>(new TableItem("Stress info", ""));
				stress.getChildren().add(new TreeItem<>(new TableItem("Maximum total stress", format_.format(resultSet.getDouble("max_val")))));
				stress.getChildren().add(new TreeItem<>(new TableItem("Minimum total stress", format_.format(resultSet.getDouble("min_val")))));
				stress.getChildren().add(new TreeItem<>(new TableItem("Maximum 1g stress", format_.format(resultSet.getDouble("max_1g")))));
				stress.getChildren().add(new TreeItem<>(new TableItem("Minimum 1g stress", format_.format(resultSet.getDouble("min_1g")))));
				stress.getChildren().add(new TreeItem<>(new TableItem("Maximum increment stress", format_.format(resultSet.getDouble("max_inc")))));
				stress.getChildren().add(new TreeItem<>(new TableItem("Minimum increment stress", format_.format(resultSet.getDouble("min_inc")))));
				stress.getChildren().add(new TreeItem<>(new TableItem("Maximum delta-p stress", format_.format(resultSet.getDouble("max_dp")))));
				stress.getChildren().add(new TreeItem<>(new TableItem("Minimum delta-p stress", format_.format(resultSet.getDouble("min_dp")))));
				stress.getChildren().add(new TreeItem<>(new TableItem("Maximum delta-t stress", format_.format(resultSet.getDouble("max_dt")))));
				stress.getChildren().add(new TreeItem<>(new TableItem("Minimum delta-t stress", format_.format(resultSet.getDouble("min_dt")))));
				list.add(stress);
			}
		}
	}
}
