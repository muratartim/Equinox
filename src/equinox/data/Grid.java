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
package equinox.data;

/**
 * Class for A/C structure grid data.
 *
 * @author Murat Artim
 * @date Jul 10, 2015
 * @time 2:42:57 PM
 */
public class Grid {

	/** Grid ID. */
	private int id_;

	/** Grid coordinates. */
	private final double[] coords_ = { 0.0, 0.0, 0.0 };

	/**
	 * Sets grid ID.
	 *
	 * @param id
	 *            Grid ID.
	 */
	public void setID(int id) {
		id_ = id;
	}

	/**
	 * Sets x coordinate.
	 *
	 * @param x
	 *            X coordinate.
	 */
	public void setX(double x) {
		coords_[0] = x;
	}

	/**
	 * Sets y coordinate.
	 *
	 * @param y
	 *            Y coordinate.
	 */
	public void setY(double y) {
		coords_[1] = y;
	}

	/**
	 * Sets z coordinate.
	 *
	 * @param z
	 *            Z coordinate.
	 */
	public void setZ(double z) {
		coords_[2] = z;
	}

	/**
	 * Returns grid ID.
	 *
	 * @return grid ID.
	 */
	public int getID() {
		return id_;
	}

	/**
	 * Returns grid coordinates.
	 *
	 * @return Grid coordinates.
	 */
	public double[] getCoords() {
		return coords_;
	}

	/**
	 * Returns X coordinate.
	 *
	 * @return X coordinate.
	 */
	public double getX() {
		return coords_[0];
	}

	/**
	 * Returns Y coordinate.
	 *
	 * @return Y coordinate.
	 */
	public double getY() {
		return coords_[1];
	}

	/**
	 * Returns Z coordinate.
	 *
	 * @return Z coordinate.
	 */
	public double getZ() {
		return coords_[2];
	}

	/**
	 * Sets coordinates of the grid with the given ID.
	 *
	 * @param grids
	 *            Array containing the grids to search from.
	 * @param id
	 *            Grid ID to search for.
	 * @param x
	 *            X coordinate.
	 * @param y
	 *            Y coordinate.
	 * @param z
	 *            Z coordinate.
	 */
	public static void setCoords(Grid[] grids, int id, double x, double y, double z) {
		for (Grid grid : grids) {
			if (grid.id_ == id) {
				grid.setX(x);
				grid.setY(y);
				grid.setZ(z);
				return;
			}
		}
	}

	/**
	 * Returns the distance between the centroids of given grid groups.
	 *
	 * @param grids1
	 *            First grid group.
	 * @param grids2
	 *            Second grid group.
	 * @return The distance between the centroids of given grid groups.
	 */
	public static double getCentroidDistance(Grid[] grids1, Grid[] grids2) {

		// get first centroid
		double x1 = 0.0, y1 = 0.0, z1 = 0.0;
		for (Grid grid : grids1) {
			x1 += grid.getX() / grids1.length;
			y1 += grid.getY() / grids1.length;
			z1 += grid.getZ() / grids1.length;
		}

		// get second centroid
		double x2 = 0.0, y2 = 0.0, z2 = 0.0;
		for (Grid grid : grids2) {
			x2 += grid.getX() / grids2.length;
			y2 += grid.getY() / grids2.length;
			z2 += grid.getZ() / grids2.length;
		}

		// return distance
		return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
	}
}
