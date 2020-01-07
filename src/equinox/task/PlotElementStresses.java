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
import java.text.DecimalFormat;
import java.util.ArrayList;

import equinox.Equinox;
import equinox.data.ElementStress;
import equinox.data.ElementType;
import equinox.data.ElementTypeForStress;
import equinox.data.Grid;
import equinox.data.input.PlotElementStressesInput;
import equinox.data.ui.LoadCaseFactorTableItem;
import equinox.data.ui.PlotContour;
import equinox.serverUtilities.Permission;
import inf.v3d.obj.PolygonOutlines;
import inf.v3d.obj.Polygons;

/**
 * Class for plot element stresses task.
 *
 * @author Murat Artim
 * @date Aug 6, 2015
 * @time 4:07:40 PM
 */
public class PlotElementStresses extends Plot3DTask<Void> {

	/** Input. */
	private final PlotElementStressesInput input_;

	/** Min-Max values for color legend. */
	private double minVal_ = Double.POSITIVE_INFINITY, maxVal_ = Double.NEGATIVE_INFINITY;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.##");

	/**
	 * Creates plot element stresses task.
	 *
	 * @param input
	 *            Plot input.
	 */
	public PlotElementStresses(PlotElementStressesInput input) {
		input_ = input;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Plot element stresses";
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_ELEMENT_STRESSES);

		// update progress info
		updateTitle("Plotting element stresses...");

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
		String title = "";
		for (LoadCaseFactorTableItem loadCase : input_.getLoadCases()) {
			double factor = Double.parseDouble(loadCase.getFactor());
			if (factor == 0.0) {
				continue;
			}
			if (factor < 0.0) {
				title += (title.isEmpty() ? "- " : " - ") + format_.format(-factor) + "*" + loadCase.getName();
			}
			else if (factor == 1.0) {
				title += (title.isEmpty() ? "" : " + ") + loadCase.getName() + " ";
			}
			else {
				title += (title.isEmpty() ? "" : " + ") + format_.format(factor) + "*" + loadCase.getName() + " ";
			}
		}
		String subTitle = input_.getStressComponent().getName() + ", " + input_.getElementType().getName();
		endTask(title, subTitle, true, minVal_, maxVal_);
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
		Polygons valuedPoly = new Polygons();
		Polygons nullPoly = new Polygons();

		// no contour colors
		if (input_.getPlotContour().equals(PlotContour.DISCRETE)) {
			valuedPoly.setColored(true);
		}
		nullPoly.setColored(false);
		nullPoly.setColor("black");

		// set opacities
		valuedPoly.setOpacity(input_.getOpacity());
		nullPoly.setOpacity(input_.getOpacity());

		// set outlines
		if (input_.getOutlines()) {
			new PolygonOutlines().setPolygons(valuedPoly);
			new PolygonOutlines().setPolygons(nullPoly);
		}

		// initialize variables
		Grid[] grids = null;
		int modelID = input_.getLoadCases().get(0).getLoadCase().getParentItem().getParentItem().getID();
		ElementStress stressComp = input_.getStressComponent();
		int bew = input_.getBeamExtrusionWidth();
		int rew = input_.getRodExtrusionWidth();
		Double lowerBound = input_.getLowerBound();
		Double upperBound = input_.getUpperBound();
		@SuppressWarnings("resource")
		PreparedStatement query = null;

		// get element group IDs
		ArrayList<Integer> groupIDs = getGroupIDs(statement, modelID);

		// get number of elements
		updateMessage("Getting number of elements to plot...");
		int numel = 0;
		try (ResultSet getNumel = statement.executeQuery(getElementGIDQueryWithGrouping(groupIDs, true, modelID))) {
			while (getNumel.next()) {
				numel = getNumel.getInt("numel");
			}
		}

		try {

			// prepare statement to get grid coordinates of QUADs
			String sql = "select gid, x_coord, y_coord, z_coord from grids_" + modelID;
			sql += " where (gid = ? or gid = ? or gid = ? or gid = ?)";
			try (PreparedStatement queryQuadShearCoords = connection.prepareStatement(sql)) {

				// prepare statement to get grid coordinates of TRIAs
				sql = "select gid, x_coord, y_coord, z_coord from grids_" + modelID;
				sql += " where (gid = ? or gid = ? or gid = ?)";
				try (PreparedStatement queryTriaCoords = connection.prepareStatement(sql)) {

					// prepare statement to get grid coordinates of BEAMs and RODs
					sql = "select gid, x_coord, y_coord, z_coord from grids_" + modelID;
					sql += " where (gid = ? or gid = ?)";
					try (PreparedStatement queryBeamRodCoords = connection.prepareStatement(sql)) {

						// execute query to get element grid IDs
						try (ResultSet getGIDs = statement.executeQuery(getElementGIDQueryWithGrouping(groupIDs, false, modelID))) {

							// prepare statement to query stress component
							try (PreparedStatement queryStress = connection.prepareStatement(getStressQuery(stressComp, modelID))) {

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

									// get element stress
									Double stress = null;
									queryStress.setInt(1, getGIDs.getInt("eid"));
									try (ResultSet s = queryStress.executeQuery()) {
										while (s.next()) {
											double factor = LoadCaseFactorTableItem.getFactorFromList(input_.getLoadCases(), s.getInt("lc_id"));
											if (stress == null) {
												stress = factor * s.getDouble(stressComp.getResultSetColName());
											}
											else {
												stress += factor * s.getDouble(stressComp.getResultSetColName());
											}
										}
									}

									// check against bounds
									if (lowerBound != null && stress != null && stress < lowerBound) {
										stress = null;
									}
									if (upperBound != null && stress != null && stress > upperBound) {
										stress = null;
									}

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
										if (stress == null) {
											nullPoly.insertNextCell(4);
											nullPoly.insertCellPoint(grids[0].getX(), grids[0].getY() - bew, grids[0].getZ(), 0.0);
											nullPoly.insertCellPoint(grids[0].getX(), grids[0].getY() + bew, grids[0].getZ(), 0.0);
											nullPoly.insertCellPoint(grids[1].getX(), grids[1].getY() + bew, grids[1].getZ(), 0.0);
											nullPoly.insertCellPoint(grids[1].getX(), grids[1].getY() - bew, grids[1].getZ(), 0.0);
										}
										else {
											valuedPoly.insertNextCell(4);
											valuedPoly.insertCellPoint(grids[0].getX(), grids[0].getY() - bew, grids[0].getZ(), stress);
											valuedPoly.insertCellPoint(grids[0].getX(), grids[0].getY() + bew, grids[0].getZ(), stress);
											valuedPoly.insertCellPoint(grids[1].getX(), grids[1].getY() + bew, grids[1].getZ(), stress);
											valuedPoly.insertCellPoint(grids[1].getX(), grids[1].getY() - bew, grids[1].getZ(), stress);
										}
									}

									// insert grid coordinates to polygon for ROD elements
									else if (eType.equals(ElementType.ROD)) {
										if (stress == null) {
											nullPoly.insertNextCell(4);
											nullPoly.insertCellPoint(grids[0].getX(), grids[0].getY() - rew, grids[0].getZ(), 0.0);
											nullPoly.insertCellPoint(grids[0].getX(), grids[0].getY() + rew, grids[0].getZ(), 0.0);
											nullPoly.insertCellPoint(grids[1].getX(), grids[1].getY() + rew, grids[1].getZ(), 0.0);
											nullPoly.insertCellPoint(grids[1].getX(), grids[1].getY() - rew, grids[1].getZ(), 0.0);
										}
										else {
											valuedPoly.insertNextCell(4);
											valuedPoly.insertCellPoint(grids[0].getX(), grids[0].getY() - rew, grids[0].getZ(), stress);
											valuedPoly.insertCellPoint(grids[0].getX(), grids[0].getY() + rew, grids[0].getZ(), stress);
											valuedPoly.insertCellPoint(grids[1].getX(), grids[1].getY() + rew, grids[1].getZ(), stress);
											valuedPoly.insertCellPoint(grids[1].getX(), grids[1].getY() - rew, grids[1].getZ(), stress);
										}
									}

									// insert grid coordinates to polygon for other elements
									else {
										if (stress == null) {
											nullPoly.insertNextCell(grids.length);
											for (Grid grid : grids) {
												nullPoly.insertCellPoint(grid.getCoords(), 0.0);
											}
										}
										else {
											valuedPoly.insertNextCell(grids.length);
											for (Grid grid : grids) {
												valuedPoly.insertCellPoint(grid.getCoords(), stress);
											}
										}
									}

									// update max-min values
									if (stress != null) {
										if (minVal_ >= stress) {
											minVal_ = stress;
										}
										if (maxVal_ <= stress) {
											maxVal_ = stress;
										}
									}
								}
							}
						}
					}
				}
			}

			// create colors
			if (input_.getPlotContour().equals(PlotContour.SMOOTHED)) {
				valuedPoly.createColors();
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
	 * Creates and returns stress query.
	 *
	 * @param stressComp
	 *            Required stress component.
	 * @param modelID
	 *            A/C model ID.
	 * @return Stress query.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressQuery(ElementStress stressComp, int modelID) throws Exception {

		// initialize query
		String sql = "select lc_id, " + stressComp.getDBSelectString();
		sql += " from load_cases_" + modelID + " where (";

		// add load cases
		for (LoadCaseFactorTableItem loadCase : input_.getLoadCases()) {
			sql += "lc_id = " + loadCase.getLoadCase().getID() + " or ";
		}
		sql = sql.substring(0, sql.length() - " or ".length()) + ")";

		// add EID
		sql += " and eid = ?";

		// return query
		return sql;
	}

	/**
	 * Retrieves and returns list containing element group IDs.
	 *
	 * @param statement
	 *            Database statement.
	 * @param modelID
	 *            A/C model ID.
	 * @return List containing element group IDs.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<Integer> getGroupIDs(Statement statement, int modelID) throws Exception {

		// initialize list
		ArrayList<Integer> groupIDs = new ArrayList<>();

		// get element group IDs
		ArrayList<String> groups = input_.getGroups();
		String sql = "select group_id from element_group_names_" + modelID;
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
		Polygons valuedPoly = new Polygons();
		Polygons nullPoly = new Polygons();

		// set contoured
		if (input_.getPlotContour().equals(PlotContour.DISCRETE)) {
			valuedPoly.setColored(true);
		}
		nullPoly.setColored(false);
		nullPoly.setColor("black");

		// set opacities
		valuedPoly.setOpacity(input_.getOpacity());
		nullPoly.setOpacity(input_.getOpacity());

		// set outlines
		if (input_.getOutlines()) {
			new PolygonOutlines().setPolygons(valuedPoly);
			new PolygonOutlines().setPolygons(nullPoly);
		}

		// initialize variables
		Grid[] grids = null;
		int modelID = input_.getLoadCases().get(0).getLoadCase().getParentItem().getParentItem().getID();
		ElementStress stressComp = input_.getStressComponent();
		int bew = input_.getBeamExtrusionWidth();
		int rew = input_.getRodExtrusionWidth();
		Double lowerBound = input_.getLowerBound();
		Double upperBound = input_.getUpperBound();
		@SuppressWarnings("resource")
		PreparedStatement query = null;

		// get number of elements
		updateMessage("Getting number of elements to plot...");
		int numel = 0;
		try (ResultSet getNumel = statement.executeQuery(getElementGIDQueryWithoutGrouping(true, modelID))) {
			while (getNumel.next()) {
				numel = getNumel.getInt("numel");
			}
		}

		try {

			// prepare statement to get grid coordinates of QUADs
			String sql = "select gid, x_coord, y_coord, z_coord from grids_" + modelID;
			sql += " where (gid = ? or gid = ? or gid = ? or gid = ?)";
			try (PreparedStatement queryQuadShearCoords = connection.prepareStatement(sql)) {

				// prepare statement to get grid coordinates of TRIAs
				sql = "select gid, x_coord, y_coord, z_coord from grids_" + modelID;
				sql += " where (gid = ? or gid = ? or gid = ?)";
				try (PreparedStatement queryTriaCoords = connection.prepareStatement(sql)) {

					// prepare statement to get grid coordinates of BEAMs and RODs
					sql = "select gid, x_coord, y_coord, z_coord from grids_" + modelID;
					sql += " where (gid = ? or gid = ?)";
					try (PreparedStatement queryBeamRodCoords = connection.prepareStatement(sql)) {

						// execute query to get element grid IDs
						try (ResultSet getGIDs = statement.executeQuery(getElementGIDQueryWithoutGrouping(false, modelID))) {

							// prepare statement to query stress component
							try (PreparedStatement queryStress = connection.prepareStatement(getStressQuery(stressComp, modelID))) {

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

									// get element stress
									Double stress = null;
									queryStress.setInt(1, getGIDs.getInt("eid"));
									try (ResultSet s = queryStress.executeQuery()) {
										while (s.next()) {
											double factor = LoadCaseFactorTableItem.getFactorFromList(input_.getLoadCases(), s.getInt("lc_id"));
											if (stress == null) {
												stress = factor * s.getDouble(stressComp.getResultSetColName());
											}
											else {
												stress += factor * s.getDouble(stressComp.getResultSetColName());
											}
										}
									}

									// check against bounds
									if (lowerBound != null && stress != null && stress < lowerBound) {
										stress = null;
									}
									if (upperBound != null && stress != null && stress > upperBound) {
										stress = null;
									}

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
										if (stress == null) {
											nullPoly.insertNextCell(4);
											nullPoly.insertCellPoint(grids[0].getX(), grids[0].getY() - bew, grids[0].getZ(), 0.0);
											nullPoly.insertCellPoint(grids[0].getX(), grids[0].getY() + bew, grids[0].getZ(), 0.0);
											nullPoly.insertCellPoint(grids[1].getX(), grids[1].getY() + bew, grids[1].getZ(), 0.0);
											nullPoly.insertCellPoint(grids[1].getX(), grids[1].getY() - bew, grids[1].getZ(), 0.0);
										}
										else {
											valuedPoly.insertNextCell(4);
											valuedPoly.insertCellPoint(grids[0].getX(), grids[0].getY() - bew, grids[0].getZ(), stress);
											valuedPoly.insertCellPoint(grids[0].getX(), grids[0].getY() + bew, grids[0].getZ(), stress);
											valuedPoly.insertCellPoint(grids[1].getX(), grids[1].getY() + bew, grids[1].getZ(), stress);
											valuedPoly.insertCellPoint(grids[1].getX(), grids[1].getY() - bew, grids[1].getZ(), stress);
										}
									}

									// insert grid coordinates to polygon for ROD elements
									else if (eType.equals(ElementType.ROD)) {
										if (stress == null) {
											nullPoly.insertNextCell(4);
											nullPoly.insertCellPoint(grids[0].getX(), grids[0].getY() - rew, grids[0].getZ(), 0.0);
											nullPoly.insertCellPoint(grids[0].getX(), grids[0].getY() + rew, grids[0].getZ(), 0.0);
											nullPoly.insertCellPoint(grids[1].getX(), grids[1].getY() + rew, grids[1].getZ(), 0.0);
											nullPoly.insertCellPoint(grids[1].getX(), grids[1].getY() - rew, grids[1].getZ(), 0.0);
										}
										else {
											valuedPoly.insertNextCell(4);
											valuedPoly.insertCellPoint(grids[0].getX(), grids[0].getY() - rew, grids[0].getZ(), stress);
											valuedPoly.insertCellPoint(grids[0].getX(), grids[0].getY() + rew, grids[0].getZ(), stress);
											valuedPoly.insertCellPoint(grids[1].getX(), grids[1].getY() + rew, grids[1].getZ(), stress);
											valuedPoly.insertCellPoint(grids[1].getX(), grids[1].getY() - rew, grids[1].getZ(), stress);
										}
									}

									// insert grid coordinates to polygon for other elements
									else {
										if (stress == null) {
											nullPoly.insertNextCell(grids.length);
											for (Grid grid : grids) {
												nullPoly.insertCellPoint(grid.getCoords(), 0.0);
											}
										}
										else {
											valuedPoly.insertNextCell(grids.length);
											for (Grid grid : grids) {
												valuedPoly.insertCellPoint(grid.getCoords(), stress);
											}
										}
									}

									// update max-min values
									if (stress != null) {
										if (minVal_ >= stress) {
											minVal_ = stress;
										}
										if (maxVal_ <= stress) {
											maxVal_ = stress;
										}
									}
								}
							}
						}
					}
				}
			}

			// create colors
			if (input_.getPlotContour().equals(PlotContour.SMOOTHED)) {
				valuedPoly.createColors();
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
	 * Returns SQL query for getting element GIDs without element grouping.
	 *
	 * @param isNumel
	 *            True if number of elements query is requested.
	 * @param modelID
	 *            A/C model ID.
	 * @return SQL query for getting element GIDs.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getElementGIDQueryWithoutGrouping(boolean isNumel, int modelID) throws Exception {

		// initialize query
		String sql = "select ";
		sql += isNumel ? "count(eid) as numel" : "eid, el_type, g1, g2, g3, g4";
		sql += " from elements_" + modelID;

		// add element type
		if (input_.getElementType().equals(ElementTypeForStress.SKIN)) {
			sql += " where el_type = 'QUAD' or el_type = 'TRIA'";
		}
		else if (input_.getElementType().equals(ElementTypeForStress.BEAM)) {
			sql += " where el_type = 'BEAM'";
		}
		else if (input_.getElementType().equals(ElementTypeForStress.ROD)) {
			sql += " where el_type = 'ROD'";
		}

		// return query
		return sql;
	}

	/**
	 * Returns SQL query for getting element GIDs with element grouping.
	 *
	 * @param groupIDs
	 *            Element group IDs.
	 * @param isNumel
	 *            True if number of elements query is requested.
	 * @param modelID
	 *            A/C model ID.
	 * @return SQL query for getting element GIDs.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getElementGIDQueryWithGrouping(ArrayList<Integer> groupIDs, boolean isNumel, int modelID) throws Exception {

		// get table names
		String elTable = "elements_" + modelID;
		String grpTable = "element_groups_" + modelID;

		// initialize query
		String sql = "select ";
		sql += isNumel ? "count(" + elTable + ".eid) as numel" : elTable + ".eid, el_type, g1, g2, g3, g4";
		sql += " from " + elTable + " inner join " + grpTable + " on " + elTable + ".eid = " + grpTable + ".eid";

		// add group names
		sql += " where (";
		for (int groupID : groupIDs) {
			sql += grpTable + ".group_id = " + groupID + " or ";
		}
		sql = sql.substring(0, sql.length() - " or ".length()) + ")";

		// add element type
		if (input_.getElementType().equals(ElementTypeForStress.SKIN)) {
			sql += " and (el_type = 'QUAD' or el_type = 'TRIA')";
		}
		else if (input_.getElementType().equals(ElementTypeForStress.BEAM)) {
			sql += " and (el_type = 'BEAM')";
		}
		else if (input_.getElementType().equals(ElementTypeForStress.ROD)) {
			sql += " and (el_type = 'ROD')";
		}

		// return query
		return sql;
	}
}
