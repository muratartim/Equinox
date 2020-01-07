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
import equinox.data.fileType.StressSequence;
import equinox.data.ui.TableItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;

/**
 * Class for get stress sequence info task.
 *
 * @author Murat Artim
 * @date Dec 13, 2013
 * @time 5:39:06 PM
 */
public class GetStressSequenceInfo extends InternalEquinoxTask<ArrayList<TreeItem<TableItem>>> implements ShortRunningTask {

	/** File item to get info. */
	private final StressSequence file_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("#.###");

	/**
	 * Creates get stress sequence info task.
	 *
	 * @param file
	 *            Input stress sequence.
	 */
	public GetStressSequenceInfo(StressSequence file) {
		file_ = file;
	}

	@Override
	public String getTaskTitle() {
		return "Get info for stress sequence '" + file_.getName() + "'";
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
		updateTitle("Retrieving info for stress sequence '" + file_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// add file name
				list.add(new TreeItem<>(new TableItem("Stress sequence name", file_.getName())));

				// get number of flights of file
				getBasicInfo(statement, list);

				// get total number of peaks of file
				getTotalNumberOfPeaks(statement, list);

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

				// get highest 1g stress flight
				getHighestLowest(statement, flightInfo, "Flight with highest 1g stress", "Highest 1g stress", "max_1g", true);

				// get lowest 1g stress flight
				getHighestLowest(statement, flightInfo, "Flight with lowest 1g stress", "Lowest 1g stress", "min_1g", false);

				// get highest increment stress flight
				getHighestLowest(statement, flightInfo, "Flight with highest increment stress", "Highest increment stress", "max_inc", true);

				// get lowest increment stress flight
				getHighestLowest(statement, flightInfo, "Flight with lowest increment stress", "Lowest increment stress", "min_inc", false);

				// get highest delta-p stress flight
				getHighestLowest(statement, flightInfo, "Flight with highest delta-p stress", "Highest delta-p stress", "max_dp", true);

				// get lowest delta-p stress flight
				getHighestLowest(statement, flightInfo, "Flight with lowest delta-p stress", "Lowest delta-p stress", "min_dp", false);

				// get highest delta-t stress flight
				getHighestLowest(statement, flightInfo, "Flight with highest delta-t stress", "Highest delta-t stress", "max_dt", true);

				// get lowest delta-t stress flight
				getHighestLowest(statement, flightInfo, "Flight with lowest delta-t stress", "Lowest delta-t stress", "min_dt", false);

				// add flight info
				list.add(flightInfo);

				// reset statement
				statement.setMaxRows(0);
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
		String sql = "select num_flights, oneg_fac, inc_fac, dp_fac, dt_fac, ref_dp, dp_lc, dt_lc_inf, dt_lc_sup, ref_dt_inf, ref_dt_sup, stress_comp, rotation_angle from sth_files where file_id = " + file_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {

				// general info
				list.add(new TreeItem<>(new TableItem("STF file", file_.getParentItem().getName())));
				list.add(new TreeItem<>(new TableItem("A/C program", file_.getParentItem().getParentItem().getProgram())));
				list.add(new TreeItem<>(new TableItem("A/C section", file_.getParentItem().getParentItem().getSection())));
				list.add(new TreeItem<>(new TableItem("Fatigue mission", file_.getParentItem().getMission())));
				list.add(new TreeItem<>(new TableItem("Number of flight types", Integer.toString(resultSet.getInt("num_flights")))));
				String stressComp = resultSet.getString("stress_comp");
				TreeItem<TableItem> stress = new TreeItem<>(new TableItem("Stress component", stressComp));
				list.add(stress);
				if (stressComp.equals(StressComponent.ROTATED.toString())) {
					stress.getChildren().add(new TreeItem<>(new TableItem("Rotation angle", format_.format(Math.toDegrees(resultSet.getDouble("rotation_angle"))))));
				}

				// delta-p pressure info
				TreeItem<TableItem> deltaP = new TreeItem<>(new TableItem("Pressure", ""));
				String value = resultSet.getString("dp_lc");
				deltaP.getChildren().add(new TreeItem<>(new TableItem("DP load case", value == null ? "N/A" : value)));
				deltaP.getChildren().add(new TreeItem<>(new TableItem("Reference DP", format_.format(resultSet.getDouble("ref_dp")))));
				list.add(deltaP);

				// delta-t temperature info
				TreeItem<TableItem> deltaT = new TreeItem<>(new TableItem("Temperature", ""));
				value = resultSet.getString("dt_lc_sup");
				deltaT.getChildren().add(new TreeItem<>(new TableItem("DT load case (sup.)", value == null ? "N/A" : value)));
				deltaT.getChildren().add(new TreeItem<>(new TableItem("Reference DT (sup.)", value == null ? "N/A" : format_.format(resultSet.getDouble("ref_dt_sup")))));
				value = resultSet.getString("dt_lc_inf");
				deltaT.getChildren().add(new TreeItem<>(new TableItem("DT load case (inf.)", value == null ? "N/A" : value)));
				deltaT.getChildren().add(new TreeItem<>(new TableItem("Reference DT (inf.)", value == null ? "N/A" : format_.format(resultSet.getDouble("ref_dt_inf")))));
				list.add(deltaT);

				// overall stress modifiers
				TreeItem<TableItem> overallStressModifiers = new TreeItem<>(new TableItem("Overall stress factors", ""));
				overallStressModifiers.getChildren().add(new TreeItem<>(new TableItem("1g stress modifier", resultSet.getString("oneg_fac"))));
				overallStressModifiers.getChildren().add(new TreeItem<>(new TableItem("Increment stress modifier", resultSet.getString("inc_fac"))));
				overallStressModifiers.getChildren().add(new TreeItem<>(new TableItem("Delta-p stress modifier", resultSet.getString("dp_fac"))));
				overallStressModifiers.getChildren().add(new TreeItem<>(new TableItem("Delta-t stress modifier", resultSet.getString("dt_fac"))));
				list.add(overallStressModifiers);
			}
		}

		// get loadcase modifiers
		sql = "select * from event_modifiers where sth_id = " + file_.getID() + " order by loadcase_number asc";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			TreeItem<TableItem> loadcaseModifiers = new TreeItem<>(new TableItem("Loadcase factors", ""));
			while (resultSet.next()) {
				String loadcaseNumber = resultSet.getString("loadcase_number");
				String eventName = resultSet.getString("event_name");
				String comments = resultSet.getString("comment");
				TreeItem<TableItem> loadcase = new TreeItem<>(new TableItem("Loadcase '" + loadcaseNumber + "'", (eventName == null ? "" : eventName) + (comments == null ? "" : " (" + comments + ")")));
				loadcase.getChildren().add(new TreeItem<>(new TableItem("Stress modifier", format_.format(resultSet.getDouble("value")) + " (" + resultSet.getString("method") + ")")));
				loadcaseModifiers.getChildren().add(loadcase);
			}
			if (!loadcaseModifiers.getChildren().isEmpty()) {
				list.add(loadcaseModifiers);
			}
		}

		// get segment modifiers
		sql = "select * from segment_modifiers where sth_id = " + file_.getID() + " order by segment_number asc";
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
				list.add(segmentModifiers);
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
		try (ResultSet resultSet = statement.executeQuery("select sum(num_peaks) as peaks, sum(num_peaks*validity) as totalPeaks from sth_flights where file_id = " + file_.getID())) {
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
		String sql = "select name, " + colName + " from sth_flights where file_id = " + file_.getID() + " order by " + colName + (isDesc ? " desc" : "");
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
