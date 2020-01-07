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
import java.util.ArrayList;

import equinox.Equinox;
import equinox.data.ElementType;
import equinox.data.Grid;
import equinox.data.input.PlotStructureInput;
import inf.v3d.obj.PolygonOutlines;
import inf.v3d.obj.Polygons;
import javafx.scene.paint.Color;

/**
 * Class for plot A/C structure task.
 *
 * @author Murat Artim
 * @date Jul 10, 2015
 * @time 3:07:22 PM
 */
public class PlotAircraftStructure extends Plot3DTask<Void> {

	/** Plot input. */
	private final PlotStructureInput input_;

	/**
	 * Creates plot A/C structure task.
	 *
	 * @param input
	 *            Plot input.
	 */
	public PlotAircraftStructure(PlotStructureInput input) {
		input_ = input;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Plot A/C structure";
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Plotting " + input_.getModel().getName() + " structure...");

		// start task
		startTask();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// without grouping
				if (input_.getGroups().isEmpty()) {
					plotWithoutGrouping(connection, statement);
				}
				else {
					plotWithGrouping(connection, statement);
				}
			}
		}

		// return
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// show viewer
		String title = input_.getModel().getName();
		String subTitle = "Element types: ";
		for (ElementType type : input_.getTypes()) {
			if (type.getSelected()) {
				subTitle += type.getName() + ", ";
			}
		}
		subTitle = subTitle.substring(0, subTitle.length() - ", ".length());
		endTask(title, subTitle, false, 0, 0);
	}

	/**
	 * Plots all elements with element grouping.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotWithGrouping(Connection connection, Statement statement) throws Exception {

		// initialize variables
		Grid[] quadShearGrids = { new Grid(), new Grid(), new Grid(), new Grid() };
		Grid[] triaGrids = { new Grid(), new Grid(), new Grid() };
		Grid[] beamRodGrids = { new Grid(), new Grid() };

		// create polygons for element types
		Polygons quads = new Polygons();
		Polygons trias = new Polygons();
		Polygons beams = new Polygons();
		Polygons rods = new Polygons();
		Polygons shears = new Polygons();

		// no contour colors
		quads.setColored(false);
		trias.setColored(false);
		beams.setColored(false);
		rods.setColored(false);
		shears.setColored(false);

		// set solid colors
		Color quadColor = input_.getType(ElementType.QUAD).getColor();
		Color triaColor = input_.getType(ElementType.TRIA).getColor();
		Color beamColor = input_.getType(ElementType.BEAM).getColor();
		Color rodColor = input_.getType(ElementType.ROD).getColor();
		Color shearColor = input_.getType(ElementType.SHEAR).getColor();
		quads.setColor(quadColor.getRed(), quadColor.getGreen(), quadColor.getBlue());
		trias.setColor(triaColor.getRed(), triaColor.getGreen(), triaColor.getBlue());
		beams.setColor(beamColor.getRed(), beamColor.getGreen(), beamColor.getBlue());
		rods.setColor(rodColor.getRed(), rodColor.getGreen(), rodColor.getBlue());
		shears.setColor(shearColor.getRed(), shearColor.getGreen(), shearColor.getBlue());

		// set opacities
		quads.setOpacity(input_.getType(ElementType.QUAD).getOpacity());
		trias.setOpacity(input_.getType(ElementType.TRIA).getOpacity());
		beams.setOpacity(input_.getType(ElementType.BEAM).getOpacity());
		rods.setOpacity(input_.getType(ElementType.ROD).getOpacity());
		shears.setOpacity(input_.getType(ElementType.SHEAR).getOpacity());

		// set outlines
		if (input_.getType(ElementType.QUAD).getOutlines()) {
			new PolygonOutlines().setPolygons(quads);
		}
		if (input_.getType(ElementType.TRIA).getOutlines()) {
			new PolygonOutlines().setPolygons(trias);
		}
		if (input_.getType(ElementType.BEAM).getOutlines()) {
			new PolygonOutlines().setPolygons(beams);
		}
		if (input_.getType(ElementType.ROD).getOutlines()) {
			new PolygonOutlines().setPolygons(rods);
		}
		if (input_.getType(ElementType.SHEAR).getOutlines()) {
			new PolygonOutlines().setPolygons(shears);
		}

		// initialize variables
		Grid[] grids = null;
		Polygons polygons = null;
		@SuppressWarnings("resource")
		PreparedStatement query = null;

		// get element group IDs
		ArrayList<Integer> groupIDs = getGroupIDs(statement);

		// get number of elements
		updateMessage("Getting number of elements to plot...");
		int numel = 0;
		try (ResultSet getNumel = statement.executeQuery(getElementGIDQueryWithGrouping(groupIDs, true))) {
			while (getNumel.next()) {
				numel = getNumel.getInt("numel");
			}
		}

		try {

			// prepare statement to get grid coordinates of QUADs
			String sql = "select gid, x_coord, y_coord, z_coord from grids_" + input_.getModel().getID();
			sql += " where (gid = ? or gid = ? or gid = ? or gid = ?)";
			try (PreparedStatement queryQuadShearCoords = connection.prepareStatement(sql)) {

				// prepare statement to get grid coordinates of TRIAs
				sql = "select gid, x_coord, y_coord, z_coord from grids_" + input_.getModel().getID();
				sql += " where (gid = ? or gid = ? or gid = ?)";
				try (PreparedStatement queryTriaCoords = connection.prepareStatement(sql)) {

					// prepare statement to get grid coordinates of BEAMs and RODs
					sql = "select gid, x_coord, y_coord, z_coord from grids_" + input_.getModel().getID();
					sql += " where (gid = ? or gid = ?)";
					try (PreparedStatement queryBeamRodCoords = connection.prepareStatement(sql)) {

						// execute query to get element grid IDs
						try (ResultSet getGIDs = statement.executeQuery(getElementGIDQueryWithGrouping(groupIDs, false))) {

							// loop over elements
							int count = 0;
							updateMessage("Plotting elements...");
							while (getGIDs.next()) {

								// task cancelled
								if (isCancelled())
									return;

								// update progress
								updateProgress(count, numel);
								count++;

								// get element type
								String eType = getGIDs.getString("el_type");

								// QUAD
								if (eType.equals(ElementType.QUAD)) {
									grids = quadShearGrids;
									polygons = quads;
									query = queryQuadShearCoords;
								}

								// TRIA
								else if (eType.equals(ElementType.TRIA)) {
									grids = triaGrids;
									polygons = trias;
									query = queryTriaCoords;
								}

								// BEAM
								else if (eType.equals(ElementType.BEAM)) {
									grids = beamRodGrids;
									polygons = beams;
									query = queryBeamRodCoords;
								}

								// ROD
								else if (eType.equals(ElementType.ROD)) {
									grids = beamRodGrids;
									polygons = rods;
									query = queryBeamRodCoords;
								}

								// SHEAR
								else if (eType.equals(ElementType.SHEAR)) {
									grids = quadShearGrids;
									polygons = shears;
									query = queryQuadShearCoords;
								}
								else {
									continue;
								}

								// set grid IDs
								for (int i = 0; i < grids.length; i++) {
									grids[i].setID(getGIDs.getInt("g" + (i + 1)));
									query.setInt(i + 1, grids[i].getID());
								}

								// get grid coordinates
								try (ResultSet getCoords = query.executeQuery()) {
									while (getCoords.next()) {
										Grid.setCoords(grids, getCoords.getInt("gid"), getCoords.getDouble("x_coord"), getCoords.getDouble("y_coord"), getCoords.getDouble("z_coord"));
									}
								}

								// insert grid coordinates to polygon for BEAM elements
								if (eType.equals(ElementType.BEAM)) {
									polygons.insertNextCell(4);
									polygons.insertCellPoint(grids[0].getX(), grids[0].getY() - input_.getBeamExtrusionWidth(), grids[0].getZ(), 0.0);
									polygons.insertCellPoint(grids[0].getX(), grids[0].getY() + input_.getBeamExtrusionWidth(), grids[0].getZ(), 0.0);
									polygons.insertCellPoint(grids[1].getX(), grids[1].getY() + input_.getBeamExtrusionWidth(), grids[1].getZ(), 0.0);
									polygons.insertCellPoint(grids[1].getX(), grids[1].getY() - input_.getBeamExtrusionWidth(), grids[1].getZ(), 0.0);
								}

								// insert grid coordinates to polygon for ROD elements
								else if (eType.equals(ElementType.ROD)) {
									polygons.insertNextCell(4);
									polygons.insertCellPoint(grids[0].getX(), grids[0].getY() - input_.getRodExtrusionWidth(), grids[0].getZ(), 0.0);
									polygons.insertCellPoint(grids[0].getX(), grids[0].getY() + input_.getRodExtrusionWidth(), grids[0].getZ(), 0.0);
									polygons.insertCellPoint(grids[1].getX(), grids[1].getY() + input_.getRodExtrusionWidth(), grids[1].getZ(), 0.0);
									polygons.insertCellPoint(grids[1].getX(), grids[1].getY() - input_.getRodExtrusionWidth(), grids[1].getZ(), 0.0);
								}

								// insert grid coordinates to polygon for other elements
								else {
									polygons.insertNextCell(grids.length);
									for (Grid grid : grids) {
										polygons.insertCellPoint(grid.getCoords(), 0.0);
									}
								}
							}
						}
					}
				}
			}
		}

		// close query if not closed
		finally {
			if (query != null) {
				query.close();
			}
		}
	}

	/**
	 * Retrieves and returns list containing element group IDs.
	 *
	 * @param statement
	 *            Database statement.
	 * @return List containing element group IDs.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<Integer> getGroupIDs(Statement statement) throws Exception {

		// initialize list
		ArrayList<Integer> groupIDs = new ArrayList<>();

		// get element group IDs
		ArrayList<String> groups = input_.getGroups();
		String sql = "select group_id from element_group_names_" + input_.getModel().getID();
		sql += " where (";
		for (String group : groups) {
			sql += "name = '" + group + "' or ";
		}
		sql = sql.substring(0, sql.length() - " or ".length()) + ")";

		// execute query
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				groupIDs.add(resultSet.getInt("group_id"));
			}
		}

		// return list
		return groupIDs;
	}

	/**
	 * Plots all elements without element grouping.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotWithoutGrouping(Connection connection, Statement statement) throws Exception {

		// initialize variables
		Grid[] quadShearGrids = { new Grid(), new Grid(), new Grid(), new Grid() };
		Grid[] triaGrids = { new Grid(), new Grid(), new Grid() };
		Grid[] beamRodGrids = { new Grid(), new Grid() };

		// create polygons for element types
		Polygons quads = new Polygons();
		Polygons trias = new Polygons();
		Polygons beams = new Polygons();
		Polygons rods = new Polygons();
		Polygons shears = new Polygons();

		// no contour colors
		quads.setColored(false);
		trias.setColored(false);
		beams.setColored(false);
		rods.setColored(false);
		shears.setColored(false);

		// set solid colors
		Color quadColor = input_.getType(ElementType.QUAD).getColor();
		Color triaColor = input_.getType(ElementType.TRIA).getColor();
		Color beamColor = input_.getType(ElementType.BEAM).getColor();
		Color rodColor = input_.getType(ElementType.ROD).getColor();
		Color shearColor = input_.getType(ElementType.SHEAR).getColor();
		quads.setColor(quadColor.getRed(), quadColor.getGreen(), quadColor.getBlue());
		trias.setColor(triaColor.getRed(), triaColor.getGreen(), triaColor.getBlue());
		beams.setColor(beamColor.getRed(), beamColor.getGreen(), beamColor.getBlue());
		rods.setColor(rodColor.getRed(), rodColor.getGreen(), rodColor.getBlue());
		shears.setColor(shearColor.getRed(), shearColor.getGreen(), shearColor.getBlue());

		// set opacities
		quads.setOpacity(input_.getType(ElementType.QUAD).getOpacity());
		trias.setOpacity(input_.getType(ElementType.TRIA).getOpacity());
		beams.setOpacity(input_.getType(ElementType.BEAM).getOpacity());
		rods.setOpacity(input_.getType(ElementType.ROD).getOpacity());
		shears.setOpacity(input_.getType(ElementType.SHEAR).getOpacity());

		// set outlines
		if (input_.getType(ElementType.QUAD).getOutlines()) {
			new PolygonOutlines().setPolygons(quads);
		}
		if (input_.getType(ElementType.TRIA).getOutlines()) {
			new PolygonOutlines().setPolygons(trias);
		}
		if (input_.getType(ElementType.BEAM).getOutlines()) {
			new PolygonOutlines().setPolygons(beams);
		}
		if (input_.getType(ElementType.ROD).getOutlines()) {
			new PolygonOutlines().setPolygons(rods);
		}
		if (input_.getType(ElementType.SHEAR).getOutlines()) {
			new PolygonOutlines().setPolygons(shears);
		}

		// initialize variables
		Grid[] grids = null;
		Polygons polygons = null;
		@SuppressWarnings("resource")
		PreparedStatement query = null;

		// get number of elements
		updateMessage("Getting number of elements to plot...");
		int numel = 0;
		try (ResultSet getNumel = statement.executeQuery(getElementGIDQueryWithoutGrouping(true))) {
			while (getNumel.next()) {
				numel = getNumel.getInt("numel");
			}
		}

		try {

			// prepare statement to get grid coordinates of QUADs
			String sql = "select gid, x_coord, y_coord, z_coord from grids_" + input_.getModel().getID();
			sql += " where (gid = ? or gid = ? or gid = ? or gid = ?)";
			try (PreparedStatement queryQuadShearCoords = connection.prepareStatement(sql)) {

				// prepare statement to get grid coordinates of TRIAs
				sql = "select gid, x_coord, y_coord, z_coord from grids_" + input_.getModel().getID();
				sql += " where (gid = ? or gid = ? or gid = ?)";
				try (PreparedStatement queryTriaCoords = connection.prepareStatement(sql)) {

					// prepare statement to get grid coordinates of BEAMs and RODs
					sql = "select gid, x_coord, y_coord, z_coord from grids_" + input_.getModel().getID();
					sql += " where (gid = ? or gid = ?)";
					try (PreparedStatement queryBeamRodCoords = connection.prepareStatement(sql)) {

						// execute query to get element grid IDs
						try (ResultSet getGIDs = statement.executeQuery(getElementGIDQueryWithoutGrouping(false))) {

							// loop over elements
							int count = 0;
							updateMessage("Plotting elements...");
							while (getGIDs.next()) {

								// task cancelled
								if (isCancelled())
									return;

								// update progress
								updateProgress(count, numel);
								count++;

								// get element type
								String eType = getGIDs.getString("el_type");

								// QUAD
								if (eType.equals(ElementType.QUAD)) {
									grids = quadShearGrids;
									polygons = quads;
									query = queryQuadShearCoords;
								}

								// TRIA
								else if (eType.equals(ElementType.TRIA)) {
									grids = triaGrids;
									polygons = trias;
									query = queryTriaCoords;
								}

								// BEAM
								else if (eType.equals(ElementType.BEAM)) {
									grids = beamRodGrids;
									polygons = beams;
									query = queryBeamRodCoords;
								}

								// ROD
								else if (eType.equals(ElementType.ROD)) {
									grids = beamRodGrids;
									polygons = rods;
									query = queryBeamRodCoords;
								}

								// SHEAR
								else if (eType.equals(ElementType.SHEAR)) {
									grids = quadShearGrids;
									polygons = shears;
									query = queryQuadShearCoords;
								}
								else {
									continue;
								}

								// set grid IDs
								for (int i = 0; i < grids.length; i++) {
									grids[i].setID(getGIDs.getInt("g" + (i + 1)));
									query.setInt(i + 1, grids[i].getID());
								}

								// get grid coordinates
								try (ResultSet getCoords = query.executeQuery()) {
									while (getCoords.next()) {
										Grid.setCoords(grids, getCoords.getInt("gid"), getCoords.getDouble("x_coord"), getCoords.getDouble("y_coord"), getCoords.getDouble("z_coord"));
									}
								}

								// insert grid coordinates to polygon for BEAM elements
								if (eType.equals(ElementType.BEAM)) {
									polygons.insertNextCell(4);
									polygons.insertCellPoint(grids[0].getX(), grids[0].getY() - input_.getBeamExtrusionWidth(), grids[0].getZ(), 0.0);
									polygons.insertCellPoint(grids[0].getX(), grids[0].getY() + input_.getBeamExtrusionWidth(), grids[0].getZ(), 0.0);
									polygons.insertCellPoint(grids[1].getX(), grids[1].getY() + input_.getBeamExtrusionWidth(), grids[1].getZ(), 0.0);
									polygons.insertCellPoint(grids[1].getX(), grids[1].getY() - input_.getBeamExtrusionWidth(), grids[1].getZ(), 0.0);
								}

								// insert grid coordinates to polygon for ROD elements
								else if (eType.equals(ElementType.ROD)) {
									polygons.insertNextCell(4);
									polygons.insertCellPoint(grids[0].getX(), grids[0].getY() - input_.getRodExtrusionWidth(), grids[0].getZ(), 0.0);
									polygons.insertCellPoint(grids[0].getX(), grids[0].getY() + input_.getRodExtrusionWidth(), grids[0].getZ(), 0.0);
									polygons.insertCellPoint(grids[1].getX(), grids[1].getY() + input_.getRodExtrusionWidth(), grids[1].getZ(), 0.0);
									polygons.insertCellPoint(grids[1].getX(), grids[1].getY() - input_.getRodExtrusionWidth(), grids[1].getZ(), 0.0);
								}

								// insert grid coordinates to polygon for other elements
								else {
									polygons.insertNextCell(grids.length);
									for (Grid grid : grids) {
										polygons.insertCellPoint(grid.getCoords(), 0.0);
									}
								}
							}
						}
					}
				}
			}
		}

		// close query if not closed
		finally {
			if (query != null) {
				query.close();
			}
		}
	}

	/**
	 * Returns SQL query for getting element GIDs with element grouping.
	 *
	 * @param groupIDs
	 *            Element group IDs.
	 * @param isNumel
	 *            True if number of elements query is requested.
	 * @return SQL query for getting element GIDs.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getElementGIDQueryWithGrouping(ArrayList<Integer> groupIDs, boolean isNumel) throws Exception {

		// get table names
		String elTable = "elements_" + input_.getModel().getID();
		String grpTable = "element_groups_" + input_.getModel().getID();

		// initialize query
		String sql = "select ";
		sql += isNumel ? "count(" + elTable + ".eid) as numel" : "el_type, g1, g2, g3, g4";
		sql += " from " + elTable + " inner join " + grpTable + " on " + elTable + ".eid = " + grpTable + ".eid";

		// add group names
		sql += " where (";
		for (int groupID : groupIDs) {
			sql += grpTable + ".group_id = " + groupID + " or ";
		}
		sql = sql.substring(0, sql.length() - " or ".length()) + ")";

		// add element types
		ArrayList<ElementType> types = input_.getTypes();

		// get number of selected elements
		int numSelected = 0;
		for (ElementType type : types) {
			if (type.getSelected()) {
				numSelected++;
			}
		}

		// all selected
		if (numSelected == types.size())
			return sql;

		// some selected
		sql += " and (";
		for (ElementType type : types) {
			if (type.getSelected()) {
				sql += elTable + ".el_type = '" + type.getName() + "' or ";
			}
		}
		sql = sql.substring(0, sql.length() - " or ".length()) + ")";
		return sql;
	}

	/**
	 * Returns SQL query for getting element GIDs without element grouping.
	 *
	 * @param isNumel
	 *            True if number of elements query is requested.
	 * @return SQL query for getting element GIDs.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getElementGIDQueryWithoutGrouping(boolean isNumel) throws Exception {

		// get element types
		ArrayList<ElementType> types = input_.getTypes();

		// initialize query
		String sql = "select ";
		sql += isNumel ? "count(eid) as numel" : "el_type, g1, g2, g3, g4";
		sql += " from elements_" + input_.getModel().getID();

		int numSelected = 0;
		for (ElementType type : types) {
			if (type.getSelected()) {
				numSelected++;
			}
		}

		// all selected
		if (numSelected == types.size())
			return sql;

		// some selected
		sql += " where ";
		for (ElementType type : types) {
			if (type.getSelected()) {
				sql += "el_type = '" + type.getName() + "' or ";
			}
		}
		sql = sql.substring(0, sql.length() - " or ".length());
		return sql;
	}
}
