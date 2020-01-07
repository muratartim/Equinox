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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.fileType.AircraftModel;
import equinox.data.fileType.PilotPoint;
import equinox.data.ui.PilotPointTableItem;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import javafx.collections.ObservableList;

/**
 * Class for link pilot points task.
 *
 * @author Murat Artim
 * @date Aug 28, 2015
 * @time 11:06:05 AM
 */
public class LinkPilotPoints extends InternalEquinoxTask<ArrayList<PilotPoint>> implements LongRunningTask {

	/** Pilot points. */
	private final ObservableList<PilotPointTableItem> pilotPoints_;

	/** Model. */
	private final AircraftModel model_;

	/**
	 * Creates link pilot points task.
	 *
	 * @param model
	 *            A/C model.
	 * @param pps
	 *            Pilot points to link.
	 */
	public LinkPilotPoints(AircraftModel model, ObservableList<PilotPointTableItem> pps) {
		model_ = model;
		pilotPoints_ = pps;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Link pilot points";
	}

	@Override
	protected ArrayList<PilotPoint> call() throws Exception {

		// update progress info
		updateTitle("Linking pilot points to aircraft model '" + model_.getName() + "'");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// get pilot points table
				String ppTable = getPilotPointsTable(connection);

				// link pilot points
				ArrayList<PilotPoint> pps = linkPilotPoints(ppTable, connection);

				// no pilot points linked
				if ((pps == null) || pps.isEmpty()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return pilot points
				return pps;
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
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get pilot points
			ArrayList<PilotPoint> pps = get();

			// add pilot points to pilot points folder
			if ((pps != null) && !pps.isEmpty()) {
				model_.getPilotPoints().getChildren().addAll(pps);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Links pilot points.
	 *
	 * @param ppTable
	 *            Pilot points table name.
	 * @param connection
	 *            Database connection.
	 * @return Linked pilot points.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<PilotPoint> linkPilotPoints(String ppTable, Connection connection) throws Exception {

		// update info
		updateMessage("Linking pilot points...");

		// create pilot points list
		ArrayList<PilotPoint> pps = new ArrayList<>();

		// prepare statement to check if EIDs exist in A/C model
		String sql = "select eid from elements_" + model_.getID() + " where eid = ?";
		try (PreparedStatement checkEID = connection.prepareStatement(sql)) {

			// prepare statement to check if STF ID is already contained in pilot points table
			sql = "select pp_id from " + ppTable + " where stf_id = ?";
			try (PreparedStatement checkSTFID = connection.prepareStatement(sql)) {

				// prepare statement to check if EID is already contained in pilot points table
				sql = "select stf_files.name from stf_files inner join pilot_points_" + model_.getID();
				sql += " on stf_files.file_id = pilot_points_" + model_.getID() + ".stf_id";
				sql += " inner join cdf_sets on stf_files.cdf_id = cdf_sets.set_id";
				sql += " where stf_files.name like ? and cdf_sets.fat_mission = ?";
				try (PreparedStatement checkEID2 = connection.prepareStatement(sql)) {

					// prepare statement to insert pilot point link
					sql = "insert into " + ppTable + "(stf_id) values (?)";
					try (PreparedStatement insertPP = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

						// loop over pilot points
						for (PilotPointTableItem pp : pilotPoints_) {

							// check EID
							boolean eidExists = false;
							checkEID.setInt(1, Integer.parseInt(pp.getEid()));
							try (ResultSet getEID = checkEID.executeQuery()) {
								while (getEID.next()) {
									eidExists = true;
								}
							}

							// EID doesn't exist
							if (!eidExists) {
								String warning = "EID: " + pp.getEid() + " doesn't exist in the A/C model. Skipping pilot point '";
								warning += pp.getStfname() + "'.";
								addWarning(warning);
								continue;
							}

							// check STF ID
							boolean stfIDExists = false;
							checkSTFID.setInt(1, pp.getSTFFileID());
							try (ResultSet getSTFID = checkSTFID.executeQuery()) {
								while (getSTFID.next()) {
									stfIDExists = true;
								}
							}

							// STF ID already exists
							if (stfIDExists) {
								addWarning("STF file '" + pp.getStfname() + "' is already linked to the A/C model. Skipping pilot point.");
								continue;
							}

							// check if EID already connected
							boolean eidConnected = false;
							checkEID2.setString(1, "%EID" + pp.getEid() + "%");
							checkEID2.setString(2, pp.getMission());
							try (ResultSet getName = checkEID2.executeQuery()) {
								while (getName.next()) {
									eidConnected = true;
								}
							}

							// EID already connected
							if (eidConnected) {
								String warning = "EID: '" + pp.getEid() + "' for fatigue mission: '" + pp.getMission();
								warning += "' is already linked to the A/C model. Skipping pilot point.";
								addWarning(warning);
								continue;
							}

							// insert pilot point link
							insertPP.setInt(1, pp.getSTFFileID());
							insertPP.executeUpdate();

							// add pilot point
							try (ResultSet resultSet = insertPP.getGeneratedKeys()) {
								resultSet.next();
								pps.add(new PilotPoint(resultSet.getBigDecimal(1).intValue(), pp.getStfname(), pp.getSTFFileID()));
							}
						}
					}
				}
			}
		}

		// return pilot points
		return pps;
	}

	/**
	 * Returns pilot points table name, or if table doesn't exist, creates a new table and returns name of it.
	 *
	 * @param connection
	 *            Database connection.
	 * @return Pilot points table name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getPilotPointsTable(Connection connection) throws Exception {

		// update info
		updateMessage("Getting pilot points table...");

		// table exists
		DatabaseMetaData dbmtadta = connection.getMetaData();
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "PILOT_POINTS_" + model_.getID(), null)) {
			while (resultSet.next())
				return "AURORA." + resultSet.getString(3);
		}

		// create new table
		try (Statement statement = connection.createStatement()) {

			// create group names table
			String sql = "CREATE TABLE AURORA.PILOT_POINTS_" + model_.getID();
			sql += "(PP_ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), STF_ID INT NOT NULL, ";
			sql += "UNIQUE(STF_ID), PRIMARY KEY(PP_ID))";
			statement.executeUpdate(sql);

			// create groups table indices
			statement.executeUpdate("CREATE INDEX PPID_INDEX_" + model_.getID() + " ON AURORA.PILOT_POINTS_" + model_.getID() + "(PP_ID)");
			statement.executeUpdate("CREATE INDEX STFID_INDEX_" + model_.getID() + " ON AURORA.PILOT_POINTS_" + model_.getID() + "(STF_ID)");
		}

		// return table name
		return "AURORA.PILOT_POINTS_" + model_.getID();
	}
}
