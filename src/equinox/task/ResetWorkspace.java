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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import equinox.Equinox;
import equinox.controller.InfoViewPanel;
import equinox.controller.InputPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.SpectrumItem;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

/**
 * Class for reset workspace task.
 *
 * @author Murat Artim
 * @date Dec 17, 2013
 * @time 5:31:32 PM
 */
public class ResetWorkspace extends InternalEquinoxTask<Void> implements LongRunningTask {

	@Override
	public String getTaskTitle() {
		return "Reset workspace";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Reseting workspace");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// truncate permanent tables
				truncatePermanentTables(connection);

				// drop temporary tables
				dropTemporaryTables(connection);

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				if (connection != null) {
					connection.rollback();
					connection.setAutoCommit(true);
				}

				// propagate exception
				throw e;
			}
		}

		// return
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// show file list panel
		taskPanel_.getOwner().getOwner().getInputPanel().showSubPanel(InputPanel.FILE_VIEW_PANEL);

		// remove all local files
		ObservableList<TreeItem<String>> list = taskPanel_.getOwner().getOwner().getInputPanel().getFileTreeRoot().getChildren();
		ArrayList<TreeItem<String>> itemsToRemove = new ArrayList<>();
		for (TreeItem<String> item : list)
			if (item instanceof SpectrumItem) {
				itemsToRemove.add(item);
			}
		list.removeAll(itemsToRemove);

		// clear info view
		InfoViewPanel infoView = (InfoViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
		infoView.clearView();

		// disable selected items
		taskPanel_.getOwner().getOwner().getMenuBarPanel().disableSelectedItems(true);
	}

	/**
	 * Truncates the permanent tables in the local database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void truncatePermanentTables(Connection connection) throws Exception {

		// update info
		updateMessage("Truncating permanent tables...");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// truncate tables
			statement.executeUpdate("truncate table AURORA.EXCALIBUR_ANALYSES");
			statement.executeUpdate("truncate table AURORA.RFORT_ANALYSES");
			statement.executeUpdate("truncate table AURORA.RFORT_OUTPUTS");
			statement.executeUpdate("truncate table AURORA.SAVED_TASKS");
			statement.executeUpdate("truncate table AURORA.AC_MODELS");
			statement.executeUpdate("truncate table AURORA.EXT_STH_FILES");
			statement.executeUpdate("truncate table AURORA.EXT_STH_FLIGHTS");
			statement.executeUpdate("truncate table AURORA.EXT_FLS_FLIGHTS");
			statement.executeUpdate("truncate table AURORA.ANALYSIS_OUTPUT_FILES");
			statement.executeUpdate("truncate table AURORA.EXT_FATIGUE_EQUIVALENT_STRESSES");
			statement.executeUpdate("truncate table AURORA.EXT_PREFFAS_EQUIVALENT_STRESSES");
			statement.executeUpdate("truncate table AURORA.EXT_LINEAR_EQUIVALENT_STRESSES");
			statement.executeUpdate("truncate table AURORA.EXT_FATIGUE_RAINFLOW_CYCLES");
			statement.executeUpdate("truncate table AURORA.EXT_PREFFAS_RAINFLOW_CYCLES");
			statement.executeUpdate("truncate table AURORA.EXT_LINEAR_RAINFLOW_CYCLES");
			statement.executeUpdate("truncate table AURORA.CDF_SETS");
			statement.executeUpdate("truncate table AURORA.CDF_MISSION_PARAMETERS");
			statement.executeUpdate("truncate table AURORA.STF_MISSION_PARAMETERS");
			statement.executeUpdate("truncate table AURORA.EXT_STH_MISSION_PARAMETERS");
			statement.executeUpdate("truncate table AURORA.ANA_FILES");
			statement.executeUpdate("truncate table AURORA.ANA_FLIGHTS");
			statement.executeUpdate("truncate table AURORA.TXT_FILES");
			statement.executeUpdate("truncate table AURORA.TXT_CODES");
			statement.executeUpdate("truncate table AURORA.FLS_FILES");
			statement.executeUpdate("truncate table AURORA.FLS_FLIGHTS");
			statement.executeUpdate("truncate table AURORA.CVT_FILES");
			statement.executeUpdate("truncate table AURORA.XLS_FILES");
			statement.executeUpdate("truncate table AURORA.XLS_COMMENTS");
			statement.executeUpdate("truncate table AURORA.STF_FILES");
			statement.executeUpdate("truncate table AURORA.PILOT_POINT_IMAGE");
			statement.executeUpdate("truncate table AURORA.PILOT_POINT_MP");
			statement.executeUpdate("truncate table AURORA.PILOT_POINT_TF_L");
			statement.executeUpdate("truncate table AURORA.PILOT_POINT_TF_HO");
			statement.executeUpdate("truncate table AURORA.PILOT_POINT_TF_HS");
			statement.executeUpdate("truncate table AURORA.PILOT_POINT_LC");
			statement.executeUpdate("truncate table AURORA.PILOT_POINT_DA");
			statement.executeUpdate("truncate table AURORA.PILOT_POINT_ST_NOP");
			statement.executeUpdate("truncate table AURORA.PILOT_POINT_ST_FO");
			statement.executeUpdate("truncate table AURORA.PILOT_POINT_ST_RH");
			statement.executeUpdate("truncate table AURORA.PILOT_POINT_DC");
			statement.executeUpdate("truncate table AURORA.PILOT_POINT_TF_DC");
			statement.executeUpdate("truncate table AURORA.MAXDAM_ANGLES");
			statement.executeUpdate("truncate table AURORA.DAMAGE_ANGLES");
			statement.executeUpdate("truncate table AURORA.DAM_ANGLE_EVENT_MODIFIERS");
			statement.executeUpdate("truncate table AURORA.DAM_ANGLE_SEGMENT_MODIFIERS");
			statement.executeUpdate("truncate table AURORA.STH_FILES");
			statement.executeUpdate("truncate table AURORA.STH_FLIGHTS");
			statement.executeUpdate("truncate table AURORA.EVENT_MODIFIERS");
			statement.executeUpdate("truncate table AURORA.SEGMENT_MODIFIERS");
			statement.executeUpdate("truncate table AURORA.FATIGUE_EQUIVALENT_STRESSES");
			statement.executeUpdate("truncate table AURORA.PREFFAS_EQUIVALENT_STRESSES");
			statement.executeUpdate("truncate table AURORA.LINEAR_EQUIVALENT_STRESSES");
			statement.executeUpdate("truncate table AURORA.FATIGUE_RAINFLOW_CYCLES");
			statement.executeUpdate("truncate table AURORA.PREFFAS_RAINFLOW_CYCLES");
			statement.executeUpdate("truncate table AURORA.LINEAR_RAINFLOW_CYCLES");
			statement.executeUpdate("truncate table AURORA.FAST_FATIGUE_EQUIVALENT_STRESSES");
			statement.executeUpdate("truncate table AURORA.FAST_PREFFAS_EQUIVALENT_STRESSES");
			statement.executeUpdate("truncate table AURORA.FAST_LINEAR_EQUIVALENT_STRESSES");
			statement.executeUpdate("truncate table AURORA.DAM_CONTRIBUTIONS");
			statement.executeUpdate("truncate table AURORA.DAM_CONTRIBUTIONS_GAG_EVENTS");
			statement.executeUpdate("truncate table AURORA.DAM_CONTRIBUTIONS_EVENT_MODIFIERS");
			statement.executeUpdate("truncate table AURORA.DAM_CONTRIBUTIONS_SEGMENT_MODIFIERS");
			statement.executeUpdate("truncate table AURORA.DAM_CONTRIBUTION");
			statement.executeUpdate("truncate table AURORA.DAM_CONTRIBUTION_EVENT_MODIFIERS");
			statement.executeUpdate("truncate table AURORA.FLIGHT_DAM_CONTRIBUTIONS");
			statement.executeUpdate("truncate table AURORA.FLIGHT_DAM_CONTRIBUTIONS_EVENT_MODIFIERS");
			statement.executeUpdate("truncate table AURORA.FLIGHT_DAM_CONTRIBUTIONS_SEGMENT_MODIFIERS");
			statement.executeUpdate("truncate table AURORA.FLIGHT_DAM_CONTRIBUTION_WITH_OCCURRENCES");
			statement.executeUpdate("truncate table AURORA.FLIGHT_DAM_CONTRIBUTION_WITHOUT_OCCURRENCES");
		}
	}

	/**
	 * Drops all temporary tables from the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void dropTemporaryTables(Connection connection) throws Exception {

		// update info
		updateMessage("Dropping temporary tables...");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// get meta data
			DatabaseMetaData dbmtadta = connection.getMetaData();

			// drop tables
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "GRIDS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ELEMENTS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ELEMENT_GROUPS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ELEMENT_GROUP_NAMES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "LOAD_CASES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "LOAD_CASE_NAMES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "PILOT_POINTS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "AC_EQ_STRESSES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "AC_EQ_STRESS_NAMES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "SEGMENTS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "SEGMENT_STEADY_STRESSES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "SEGMENT_INCREMENT_STRESSES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "STH_PEAKS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ANA_PEAKS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "STF_STRESSES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "EXT_STH_PEAKS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "EXCALIBUR_XLS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "EXCALIBUR_LCK_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "EXCALIBUR_STF_STRESSES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "EXCALIBUR_STF_FILES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
		}
	}
}
