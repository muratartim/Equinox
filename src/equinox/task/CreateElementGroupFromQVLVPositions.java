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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import equinox.Equinox;
import equinox.data.ElementType;
import equinox.data.Grid;
import equinox.data.fileType.AircraftModel;
import equinox.data.ui.QVLVPosition;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import javafx.collections.ObservableList;

/**
 * Class for create element group from frame/stringer positions task.
 *
 * @author Murat Artim
 * @date Aug 4, 2015
 * @time 1:00:57 PM
 */
public class CreateElementGroupFromQVLVPositions extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** Model. */
	private final AircraftModel model_;

	/** Group name. */
	private final String name_;

	/** Frame/stringer positions. */
	private final ObservableList<QVLVPosition> positions_;

	/**
	 * Creates create element group from frame/stringer positions task.
	 *
	 * @param model
	 *            A/C model.
	 * @param name
	 *            Group name.
	 * @param positions
	 *            Frame/stringer positions.
	 */
	public CreateElementGroupFromQVLVPositions(AircraftModel model, String name, ObservableList<QVLVPosition> positions) {
		model_ = model;
		name_ = name;
		positions_ = positions;
	}

	@Override
	public String getTaskTitle() {
		return "Create element group from frame/stringer positions";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Creating element group from frame/stringer positions");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// create group
				try (Statement statement = connection.createStatement()) {
					createGroup(connection, statement);
				}

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
				connection.rollback();
				connection.setAutoCommit(true);

				// propagate exception
				throw e;
			}
		}
		return null;
	}

	/**
	 * Creates element group.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void createGroup(Connection connection, Statement statement) throws Exception {

		// add to group names
		updateMessage("Creating group '" + name_ + "'...");
		int groupID = -1;
		String sql = "insert into ELEMENT_GROUP_NAMES_" + model_.getID() + "(name) values('" + name_ + "')";
		statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
		try (ResultSet resultSet = statement.getGeneratedKeys()) {
			while (resultSet.next()) {
				groupID = resultSet.getBigDecimal(1).intValue();
			}
		}

		// cannot create group
		if ((groupID == -1) || isCancelled())
			return;

		// get total number of elements
		int numTotal = 0;
		sql = "select num_elems from ac_models where model_id = " + model_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				numTotal = resultSet.getInt("num_elems");
			}
		}

		// insert element IDs
		int numel = insertEIDs(groupID, numTotal, connection, statement);

		// task cancelled
		if ((numel == -1) || isCancelled())
			return;

		// update number of elements of group
		updateMessage("Updating number of elements of group...");
		sql = "update ELEMENT_GROUP_NAMES_" + model_.getID() + " set numel = " + numel + " where group_id = " + groupID;
		statement.executeUpdate(sql);
	}

	/**
	 * Extracts and inserts element IDs into group.
	 *
	 * @param groupID
	 *            Group ID.
	 * @param numTotal
	 *            Total number of elements.
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @return Number of elements of the group.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int insertEIDs(int groupID, int numTotal, Connection connection, Statement statement) throws Exception {

		// update info
		updateMessage("Adding elements to group...");

		// initialize variables
		int numel = 0;
		Grid[] quadShearGrids = { new Grid(), new Grid(), new Grid(), new Grid() };
		Grid[] triaGrids = { new Grid(), new Grid(), new Grid() };
		Grid[] beamRodGrids = { new Grid(), new Grid() };
		Grid[] grids = null;
		@SuppressWarnings("resource")
		PreparedStatement query = null;

		try {

			// get frame/stringer position query
			String positionQuery = getPositionQuery();

			// prepare statement to get grid coordinates of QUADs
			String sql = "select gid from grids_" + model_.getID();
			sql += " where (gid = ? or gid = ? or gid = ? or gid = ?)";
			sql += positionQuery;
			try (PreparedStatement queryQuadShearCoords = connection.prepareStatement(sql)) {

				// prepare statement to get grid coordinates of TRIAs
				sql = "select gid from grids_" + model_.getID();
				sql += " where (gid = ? or gid = ? or gid = ?)";
				sql += positionQuery;
				try (PreparedStatement queryTriaCoords = connection.prepareStatement(sql)) {

					// prepare statement to get grid coordinates of BEAMs and RODs
					sql = "select gid from grids_" + model_.getID();
					sql += " where (gid = ? or gid = ?)";
					sql += positionQuery;
					try (PreparedStatement queryBeamRodCoords = connection.prepareStatement(sql)) {

						// prepare statement for inserting into element groups
						sql = "insert into ELEMENT_GROUPS_" + model_.getID() + "(group_id, eid) values(?, ?)";
						try (PreparedStatement insertToGroups = connection.prepareStatement(sql)) {

							// set group ID
							insertToGroups.setInt(1, groupID);

							// execute query to get element grid IDs
							sql = "select eid, el_type, g1, g2, g3, g4 from elements_" + model_.getID();
							try (ResultSet getGIDs = statement.executeQuery(sql)) {

								// loop over elements
								int elems = 0;
								while (getGIDs.next()) {

									// task cancelled
									if (isCancelled())
										return -1;

									// update progress info
									updateProgress(elems, numTotal);
									elems++;

									// get element type
									String eType = getGIDs.getString("el_type");

									// QUAD
									if (eType.equals(ElementType.QUAD)) {
										grids = quadShearGrids;
										query = queryQuadShearCoords;
									}

									// TRIA
									else if (eType.equals(ElementType.TRIA)) {
										grids = triaGrids;
										query = queryTriaCoords;
									}

									// BEAM
									else if (eType.equals(ElementType.BEAM)) {
										grids = beamRodGrids;
										query = queryBeamRodCoords;
									}

									// ROD
									else if (eType.equals(ElementType.ROD)) {
										grids = beamRodGrids;
										query = queryBeamRodCoords;
									}

									// SHEAR
									else if (eType.equals(ElementType.SHEAR)) {
										grids = quadShearGrids;
										query = queryQuadShearCoords;
									}

									// invalid type
									else {
										addWarning("Invalid element type encountered in elements database. Type: " + eType + ", EID: " + getGIDs.getInt("eid"));
										continue;
									}

									// set grid IDs
									for (int i = 0; i < grids.length; i++) {
										grids[i].setID(getGIDs.getInt("g" + (i + 1)));
										query.setInt(i + 1, grids[i].getID());
									}

									// get grid coordinates
									try (ResultSet getCoords = query.executeQuery()) {
										int num = 0;
										while (getCoords.next()) {
											num++;
										}
										if (num != grids.length) {
											continue;
										}
									}

									// insert to group
									insertToGroups.setInt(2, getGIDs.getInt("eid"));
									insertToGroups.executeUpdate();
									numel++;
								}
							}
						}
					}
				}
			}

			// return number of elements
			return numel;
		}

		// close query if not closed
		finally {
			if (query != null) {
				query.close();
			}
		}
	}

	/**
	 * Creates and returns frame/stringer position query.
	 *
	 * @return Frame/stringer position query.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getPositionQuery() throws Exception {

		// initialize query
		String sql = "";

		// add frame positions
		sql += " and (";
		for (QVLVPosition position : positions_) {
			sql += "(qv_pos = '" + position.getFramepos().replaceAll("'", "''") + "' and ";
			sql += "lv_pos = '" + position.getStringerpos().replaceAll("'", "''") + "') or ";
		}
		sql = sql.substring(0, sql.length() - " or ".length()) + ")";

		// return query
		return sql;
	}
}
