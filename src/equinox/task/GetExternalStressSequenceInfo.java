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
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.ui.TableItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;

/**
 * Class for get external stress sequence info task.
 *
 * @author Murat Artim
 * @date Mar 13, 2015
 * @time 10:47:18 AM
 */
public class GetExternalStressSequenceInfo extends InternalEquinoxTask<ArrayList<TreeItem<TableItem>>> implements ShortRunningTask {

	/** File item to get info. */
	private final ExternalStressSequence file_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("#.###");

	/**
	 * Creates get external stress sequence info task.
	 *
	 * @param file
	 *            Input external stress sequence.
	 */
	public GetExternalStressSequenceInfo(ExternalStressSequence file) {
		file_ = file;
	}

	@Override
	public String getTaskTitle() {
		return "Get info for external stress sequence '" + file_.getName() + "'";
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
		updateTitle("Retrieving info for external stress sequence '" + file_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// add file name
				list.add(new TreeItem<>(new TableItem("Stress sequence name", file_.getName())));

				// add EID
				String eid = ExternalStressSequence.getEID(file_.getName());
				list.add(new TreeItem<>(new TableItem("Element ID", eid == null ? "No EID found in file name" : eid)));

				// get number of flights of file
				getBasicInfo(statement, list);

				// get total number of peaks of file
				getTotalNumberOfPeaks(statement, list);

				// get mission parameters
				getMissionParameters(statement, list);

				// flight info
				TreeItem<TableItem> flightInfo = new TreeItem<>(new TableItem("Typical flight info", ""));

				// get longest flight
				getHighestLowest(statement, flightInfo, "Longest flight", "Number of peaks", "num_peaks", true);

				// get shortest flight
				getHighestLowest(statement, flightInfo, "Shortest flight", "Number of peaks", "num_peaks", false);

				// get highest peak flight
				getHighestLowest(statement, flightInfo, "Flight with highest peak", "Highest peak", "max_val", true);

				// get lowest peak flight
				getHighestLowest(statement, flightInfo, "Flight with lowest peak", "Lowest peak", "min_val", false);

				// get highest validity flight
				getHighestLowest(statement, flightInfo, "Flight with highest occurrence", "Highest occurrence", "validity", true);

				// get lowest validity flight
				getHighestLowest(statement, flightInfo, "Flight with lowest occurrence", "Lowest occurrence", "validity", false);

				// add flight info
				list.add(flightInfo);

				// reset statement
				statement.setMaxRows(0);

				// get material info
				getMaterialInfo(statement, list);
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
			panel.showInfoView(false, file_);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Gets mission parameter info (if any).
	 *
	 * @param statement
	 *            Database statement.
	 * @param list
	 *            Info list.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getMissionParameters(Statement statement, ArrayList<TreeItem<TableItem>> list) throws Exception {
		TreeItem<TableItem> missionParameters = new TreeItem<>(new TableItem("Fatigue mission parameters", ""));
		String sql = "select name, val from ext_sth_mission_parameters where sth_id = " + file_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				missionParameters.getChildren().add(new TreeItem<>(new TableItem(resultSet.getString("name"), format_.format(resultSet.getDouble("val")))));
			}
		}
		if (!missionParameters.getChildren().isEmpty()) {
			list.add(missionParameters);
		}
	}

	/**
	 * Gets the material info of the input STH file from the database.
	 *
	 * @param statement
	 *            SQL statement.
	 * @param list
	 *            File info table list.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getMaterialInfo(Statement statement, ArrayList<TreeItem<TableItem>> list) throws Exception {

		// update message
		updateMessage("Getting material info...");

		// get file information
		TreeItem<TableItem> materials = new TreeItem<>(new TableItem("Material info", ""));
		String sql = "select fatigue_material, preffas_material, linear_material from ext_sth_files where file_id = " + file_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				String info = resultSet.getString("fatigue_material");
				materials.getChildren().add(new TreeItem<>(new TableItem("Fatigue", info == null ? "-" : info)));
				info = resultSet.getString("preffas_material");
				materials.getChildren().add(new TreeItem<>(new TableItem("Preffas", info == null ? "-" : info)));
				info = resultSet.getString("linear_material");
				materials.getChildren().add(new TreeItem<>(new TableItem("Linear", info == null ? "-" : info)));
			}
		}
		list.add(materials);
	}

	/**
	 * Gets the basic info of the input STH file from the database.
	 *
	 * @param statement
	 *            SQL statement.
	 * @param list
	 *            File info table list.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getBasicInfo(Statement statement, ArrayList<TreeItem<TableItem>> list) throws Exception {

		// update message
		updateMessage("Getting basic file info...");

		// get file information
		String sql = "select num_flights from ext_sth_files where file_id = " + file_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {

				// general info
				list.add(new TreeItem<>(new TableItem("A/C program", file_.getProgram())));
				list.add(new TreeItem<>(new TableItem("A/C section", file_.getSection())));
				list.add(new TreeItem<>(new TableItem("Fatigue mission", file_.getMission())));
				list.add(new TreeItem<>(new TableItem("Number of flight types", Integer.toString(resultSet.getInt("num_flights")))));
			}
		}
	}

	/**
	 * Gets the total number of peaks of the input STH file from the database.
	 *
	 * @param statement
	 *            SQL statement.
	 * @param list
	 *            File info table list.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getTotalNumberOfPeaks(Statement statement, ArrayList<TreeItem<TableItem>> list) throws Exception {
		updateMessage("Getting number of peaks...");
		TreeItem<TableItem> peaks = new TreeItem<>(new TableItem("Number of peaks", ""));
		try (ResultSet resultSet = statement.executeQuery("select sum(num_peaks) as peaks, sum(num_peaks*validity) as totalPeaks from ext_sth_flights where file_id = " + file_.getID())) {
			resultSet.next();
			peaks.getChildren().add(new TreeItem<>(new TableItem("With flight occurrences", Integer.toString(resultSet.getInt("totalPeaks")))));
			peaks.getChildren().add(new TreeItem<>(new TableItem("Without flight occurrences", Integer.toString(resultSet.getInt("peaks")))));
		}
		list.add(peaks);
	}

	/**
	 * Gets the flight with the highest or lowest peak of the input STH file from the database.
	 *
	 * @param statement
	 *            SQL statement.
	 * @param flightInfo
	 *            File info table list.
	 * @param label1
	 *            First label.
	 * @param label2
	 *            Second label.
	 * @param colName
	 *            Database column name.
	 * @param isDesc
	 *            True if descending order.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getHighestLowest(Statement statement, TreeItem<TableItem> flightInfo, String label1, String label2, String colName, boolean isDesc) throws Exception {
		updateMessage("Getting " + label1 + "...");
		String sql = "select name, " + colName + " from ext_sth_flights where file_id = " + file_.getID() + " order by " + colName + (isDesc ? " desc" : "");
		statement.setMaxRows(1);
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				TreeItem<TableItem> flight = new TreeItem<>(new TableItem(label1, resultSet.getString("name")));
				flight.getChildren().add(new TreeItem<>(new TableItem(label2, format_.format(resultSet.getDouble(colName)))));
				flightInfo.getChildren().add(flight);
			}
		}
	}
}
