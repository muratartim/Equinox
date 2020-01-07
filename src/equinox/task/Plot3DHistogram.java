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

import java.awt.Color;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import equinox.Equinox;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.Histogram3DInput;
import equinox.serverUtilities.Permission;
import equinox.viewer.Label;
import inf.v3d.obj.Arrow;
import inf.v3d.obj.BoundingBox;
import inf.v3d.obj.Box;
import inf.v3d.obj.Line;
import inf.v3d.obj.PolygonOutlines;
import inf.v3d.obj.Polygons;

/**
 * Class for plot 3D histogram task.
 *
 * @author Murat Artim
 * @date Jun 21, 2014
 * @time 2:45:44 PM
 */
public class Plot3DHistogram extends Plot3DTask<Void> {

	/** Static variable for default color levels. */
	private static final Color[] COLORS = { new Color(0, 0, 255), new Color(0, 127, 255), new Color(0, 255, 255), new Color(0, 255, 127), new Color(0, 255, 0), new Color(127, 255, 0), new Color(255, 255, 0), new Color(255, 127, 0), new Color(255, 0, 0) };

	/** Histogram input. */
	private final Histogram3DInput input_;

	/**
	 * Creates plot 3D histogram task.
	 *
	 * @param input
	 *            Input.
	 */
	public Plot3DHistogram(Histogram3DInput input) {
		input_ = input;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Plot 3D rainflow histogram";
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_TYPICAL_FLIGHT_STATISTICS);

		// update progress info
		updateTitle("Plotting 3D rainflow histogram...");

		// start task
		startTask();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get max-min
				double[][] maxMin = getMaxMin(statement);

				// draw base
				drawBase(maxMin);

				// set table name
				String tableName = getTableName();

				// create and execute query
				float[] rgb = null;
				String sql = "select num_cycles, ";
				sql += input_.getDataTypeX().getDBColumnName() + " as data_x, ";
				sql += input_.getDataTypeY().getDBColumnName() + " as data_y ";
				sql += "from " + tableName + " where stress_id = " + input_.getEquivalentStress().getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {

						// get x, y, z
						double x = resultSet.getDouble("data_x");
						double y = resultSet.getDouble("data_y");
						double z = resultSet.getDouble("num_cycles");

						// get color
						rgb = getColor(maxMin[2][0], maxMin[2][1], Math.log10(z));

						// normalize coordinates
						double nx = (x - maxMin[0][1]) / (maxMin[0][0] - maxMin[0][1]);
						double ny = (y - maxMin[1][1]) / (maxMin[1][0] - maxMin[1][1]);
						double nz = (Math.log10(z) - maxMin[2][1]) / (maxMin[2][0] - maxMin[2][1]);

						// draw column
						drawColumn(nx, ny, nz, rgb, x, y, z);
					}
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
		String title = "Rainflow Histogram";
		String subTitle = input_.getEquivalentStress().getName();
		endTask(title, subTitle, false, 0, 0);
	}

	/**
	 * Returns rainflow table name.
	 *
	 * @return Rainflow table name.
	 */
	private String getTableName() {

		// initialize table name
		String tableName = null;

		// get item
		SpectrumItem item = input_.getEquivalentStress();

		// set table name
		if (item instanceof FatigueEquivalentStress) {
			tableName = "fatigue_rainflow_cycles";
		}
		else if (item instanceof PreffasEquivalentStress) {
			tableName = "preffas_rainflow_cycles";
		}
		else if (item instanceof LinearEquivalentStress) {
			tableName = "linear_rainflow_cycles";
		}
		else if (item instanceof ExternalFatigueEquivalentStress) {
			tableName = "ext_fatigue_rainflow_cycles";
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			tableName = "ext_preffas_rainflow_cycles";
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			tableName = "ext_linear_rainflow_cycles";
		}

		// return table name
		return tableName;
	}

	/**
	 * Draws chart columns.
	 *
	 * @param x
	 *            X coordinate.
	 * @param y
	 *            Y coordinate.
	 * @param z
	 *            Z coordinate.
	 * @param rgb
	 *            Color scale.
	 * @param valx
	 *            Value X.
	 * @param valy
	 *            Value Y.
	 * @param valz
	 *            Value Z.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void drawColumn(double x, double y, double z, float[] rgb, double valx, double valy, double valz) throws Exception {

		// update progress info
		updateMessage("Drawing chart columns....");

		// initialize variables
		boolean isout = false;
		int res = input_.getResolution();
		double itv = 1.0 / res;
		double textScale = 1.0;

		// loop over grids in x direction
		for (int i = 0; i < res; i++) {

			// calculate interval in x direction
			double xi = i * itv;
			double xi1 = (i + 1) * itv;

			// coordinate is within the interval
			if (x >= xi && x <= xi1) {

				// loop over grids in y direction
				for (int j = 0; j < res; j++) {

					// calculate interval in y direction
					double yj = j * itv;
					double yj1 = (j + 1) * itv;

					// coordinate is within the interval
					if (y >= yj && y <= yj1) {

						// create box
						Box b = new Box(xi, yj, 0.0);
						b.setSize(itv, itv, z);

						// set color
						if (rgb != null) {
							b.setColor(rgb[0], rgb[1], rgb[2]);
						}

						// create bounding box
						new BoundingBox(b);

						// data label
						if (input_.getLabelDisplayX() || input_.getLabelDisplayY() || input_.getLabelDisplayZ()) {

							// setup text
							String text = "(";

							// x
							if (input_.getLabelDisplayX()) {
								text += formatDouble(valx) + ", ";
							}

							// y
							if (input_.getLabelDisplayY()) {
								text += formatDouble(valy) + ", ";
							}

							// z
							if (input_.getLabelDisplayZ()) {
								text += formatDouble(valz);
							}

							// finalize text
							text += ")";

							// create label
							Label label = new Label(text, xi + itv, yj + itv, z);
							label.setScale(textScale * 0.02);
							label.setColor(Color.red);
							addLabel(label);
						}

						// break
						isout = true;
						break;
					}
				}
			}

			// break
			if (isout) {
				break;
			}
		}
	}

	/**
	 * Draws chart bases.
	 *
	 * @param maxMin
	 *            Max min values.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void drawBase(double[][] maxMin) throws Exception {

		// update progress info
		updateMessage("Drawing chart base...");

		// set vertex coordinates
		double[] p1 = { 1, 0, 0 };
		double[] p2 = { 1, 1, 0 };
		double[] p3 = { 0, 1, 0 };
		double[] p4 = { 0, 0, 0 };
		double[] p5 = { 0, 0, 1 };
		double[] p6 = { 1, 0, 1 };
		double[] p7 = { 0, 1, 1 };

		// initialize polygon
		Polygons pl = new Polygons();
		pl.setColored(false);

		// insert bottom
		pl.insertNextCell(4);
		pl.insertCellPoint(p1, 0.0);
		pl.insertCellPoint(p2, 0.0);
		pl.insertCellPoint(p3, 0.0);
		pl.insertCellPoint(p4, 0.0);

		// insert left
		pl.insertNextCell(4);
		pl.insertCellPoint(p1, 0.0);
		pl.insertCellPoint(p4, 0.0);
		pl.insertCellPoint(p5, 0.0);
		pl.insertCellPoint(p6, 0.0);

		// insert right
		pl.insertNextCell(4);
		pl.insertCellPoint(p4, 0.0);
		pl.insertCellPoint(p3, 0.0);
		pl.insertCellPoint(p7, 0.0);
		pl.insertCellPoint(p5, 0.0);

		// set back ground color
		Color c = Color.LIGHT_GRAY;
		double r = c.getRed() / 255.0;
		double g = c.getGreen() / 255.0;
		double b = c.getBlue() / 255.0;
		pl.setColor(r, g, b);

		// set opacity
		pl.setOpacity(0.4);

		// set outline
		PolygonOutlines outlines = new PolygonOutlines();
		outlines.setPolygons(pl);

		// draw axis labels
		drawAxisLabels();

		// draw guidelines
		drawGuideLines(maxMin);
	}

	/**
	 * Draws axis labels.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void drawAxisLabels() throws Exception {

		// update progress info
		updateMessage("Drawing chart axis labels...");

		// create x axis
		Arrow ax = new Arrow();
		ax.setPoint1(1.0D, 0.0D, 0.0D);
		ax.setPoint2(1.3D, 0.0D, 0.0D);
		ax.setRadius(0.005);
		ax.setColor("red");
		ax.setOpacity(0.4);

		// text scale
		double textScale = 1.0;

		// create x label
		Label x = new Label(input_.getDataTypeX().toString(), 1.3D, 0.0D, 0.0D);
		x.setScale(textScale * 0.02);
		x.setColor(Color.red);
		addLabel(x);

		// create y axis
		Arrow ay = new Arrow();
		ay.setPoint1(0.0D, 1.0D, 0.0D);
		ay.setPoint2(0.0D, 1.3D, 0.0D);
		ay.setRadius(0.005);
		ay.setColor("green");
		ay.setOpacity(0.4);

		// create y label
		Label y = new Label(input_.getDataTypeY().toString(), 0.0D, 1.3D, 0.0D);
		y.setScale(textScale * 0.02);
		y.setColor(Color.red);
		addLabel(y);

		// create z axis
		Arrow az = new Arrow();
		az.setPoint1(0.0D, 0.0D, 1.0D);
		az.setPoint2(0.0D, 0.0D, 1.3D);
		az.setRadius(0.005);
		az.setColor("blue");
		az.setOpacity(0.4);

		// create z label
		Label z = new Label("Number of Cycles", 0.0D, 0.0D, 1.3D);
		z.setScale(textScale * 0.02);
		z.setColor(Color.red);
		addLabel(z);
	}

	/**
	 * Draws the guidelines.
	 *
	 * @param maxMin
	 *            Max-min values.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void drawGuideLines(double[][] maxMin) throws Exception {

		// update progress info
		updateMessage("Drawing chart guidelines...");

		// get guideline color
		Color c = Color.GRAY;
		double r = c.getRed() / 255.0;
		double g = c.getGreen() / 255.0;
		double b = c.getBlue() / 255.0;

		// text scale
		double textScale = 1.0;

		// draw guidelines along x axis in xy plane
		for (int i = 0; i < 11; i++) {

			// draw line
			Line gui = new Line(0, i / 10.0, 0, 1.0, i / 10.0, 0);
			gui.setColor(r, g, b);
			gui.setOpacity(0.4);

			// draw label
			String val = formatDouble(maxMin[1][1] + 0.1 * (maxMin[1][0] - maxMin[1][1]) * i);
			Label label = new Label(val, 1.0, i / 10.0, 0.0);
			label.setScale(textScale * 0.02);
			label.setColor(Color.red);
			addLabel(label);
		}

		// draw guidelines along x axis in xz plane
		for (int i = 0; i < 11; i++) {
			Line gui = new Line(0, 0, i / 10.0, 1.0, 0, i / 10.0);
			gui.setColor(r, g, b);
			gui.setOpacity(0.4);
		}

		// draw guidelines along y axis in yx plane
		for (int i = 0; i < 11; i++) {
			Line gui = new Line(i / 10.0, 0, 0, i / 10.0, 1.0, 0);
			gui.setColor(r, g, b);
			gui.setOpacity(0.4);
		}

		// draw guidelines along y axis in yz plane
		for (int i = 0; i < 11; i++) {
			Line gui = new Line(0, 0, i / 10.0, 0, 1.0, i / 10.0);
			gui.setColor(r, g, b);
			gui.setOpacity(0.4);

			// draw label
			double v = maxMin[2][1] + 0.1 * (maxMin[2][0] - maxMin[2][1]) * i;
			String val = formatDouble(Math.pow(10, v));
			Label label = new Label(val, 0.0, 1.0, i / 10.0);
			label.setScale(textScale * 0.02);
			label.setColor(Color.red);
			addLabel(label);
		}

		// draw guidelines along z axis in zx plane
		for (int i = 0; i < 11; i++) {
			Line gui = new Line(i / 10.0, 0, 0, i / 10.0, 0, 1.0);
			gui.setColor(r, g, b);
			gui.setOpacity(0.4);

			// draw label
			String val = formatDouble(maxMin[0][1] + 0.1 * (maxMin[0][0] - maxMin[0][1]) * i);
			Label label = new Label(val, i / 10.0, 0, 1.0);
			label.setScale(textScale * 0.02);
			label.setColor(Color.red);
			addLabel(label);
		}

		// draw guidelines along z axis in zy plane
		for (int i = 0; i < 11; i++) {
			Line gui = new Line(0, i / 10.0, 0, 0, i / 10.0, 1.0);
			gui.setColor(r, g, b);
			gui.setOpacity(0.4);
		}
	}

	/**
	 * Obtains and returns max-min values of all data axes.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Max-min values of all data axes.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double[][] getMaxMin(Statement statement) throws Exception {

		// update progress info
		updateMessage("Getting max-min values...");

		// initialize variables
		double[][] maxMin = { { 0, 0 }, { 0, 0 }, { 0, 0 } };

		// set table name
		String tableName = getTableName();

		// create and execute query
		String sql = "select max(num_cycles) as max_cycles, min(num_cycles) as min_cycles";
		sql += ", max(" + input_.getDataTypeX().getDBColumnName() + ") as max_x";
		sql += ", min(" + input_.getDataTypeX().getDBColumnName() + ") as min_x";
		sql += ", max(" + input_.getDataTypeY().getDBColumnName() + ") as max_y";
		sql += ", min(" + input_.getDataTypeY().getDBColumnName() + ") as min_y";
		sql += " from " + tableName + " where stress_id = " + input_.getEquivalentStress().getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				maxMin[0][0] = resultSet.getDouble("max_x");
				maxMin[0][1] = resultSet.getDouble("min_x");
				maxMin[1][0] = resultSet.getDouble("max_y");
				maxMin[1][1] = resultSet.getDouble("min_y");
				maxMin[2][0] = Math.log10(resultSet.getDouble("max_cycles"));
				maxMin[2][1] = Math.log10(resultSet.getDouble("min_cycles"));
			}
		}

		// return max-min values
		return maxMin;
	}

	/**
	 * Returns the contour color according to given values.
	 *
	 * @param max
	 *            Maximum z value.
	 * @param min
	 *            Minimum z value.
	 * @param z
	 *            Current z value.
	 * @return Array containing the RGB components.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static float[] getColor(double max, double min, double z) throws Exception {

		// get number of levels
		int num = COLORS.length;

		// loop over levels
		for (int i = 0; i < num; i++) {

			// compute lower and upper bounds
			double lower = min + (max - min) * i / num;
			double upper = min + (max - min) * (i + 1) / num;

			// compare
			if (z >= lower && z <= upper)
				return COLORS[i].getRGBColorComponents(null);
		}

		// value out of range
		return null;
	}

	/**
	 * Formats given double number.Formating rule: If the absolute value of the number is greater or equal to 1.00E-03 and smaller or equal to 1.00E+03, it is formatted as a decimal number with 4 digits after the decimal separator. Otherwise, it is formatted as a scientific number with 4 digits
	 * after the decimal separator.
	 *
	 * @param number
	 *            The number to be formatted.
	 * @return The formatted string.
	 */
	private static String formatDouble(double number) {
		if (Math.abs(number) >= 1.00E-03 && Math.abs(number) <= 1.00E+03)
			return String.format("%." + 2 + "f", number);
		return String.format("%." + 2 + "E", number);
	}
}
