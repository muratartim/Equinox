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
 * Enumeration for element stresses.
 *
 * @author Murat Artim
 * @date Aug 25, 2015
 * @time 1:32:34 PM
 */
public enum ElementStress {

	/** Element stress type. */
	SX("Normal stress X", "sx", "sx"), SY("Normal stress Y", "sy", "sy"), SXY("Shear stress XY", "sxy", "sxy"), MAX_PRINCIPAL(
			"Maximum principal stress", getPrincipal(true), "maxprin"), MIN_PRINCIPAL("Minimum principal stress", getPrincipal(false), "minprin");

	/** Name of element stress. */
	private final String name_, dbSelectString_, resultSetColName_;

	/**
	 * Creates element stress.
	 *
	 * @param name
	 *            Name of element stress.
	 * @param dbSelectString
	 *            Database select string.
	 * @param resultSetColName
	 *            Result set column name.
	 */
	ElementStress(String name, String dbSelectString, String resultSetColName) {
		name_ = name;
		dbSelectString_ = dbSelectString;
		resultSetColName_ = resultSetColName;
	}

	/**
	 * Returns the name of element stress.
	 *
	 * @return The name of element stress.
	 */
	public String getName() {
		return name_;
	}

	/**
	 * Returns database select string.
	 *
	 * @return Database select string.
	 */
	public String getDBSelectString() {
		return dbSelectString_;
	}

	/**
	 * Returns result set column name.
	 *
	 * @return Result set column name.
	 */
	public String getResultSetColName() {
		return resultSetColName_;
	}

	@Override
	public String toString() {
		return name_;
	}

	/**
	 * Returns maximum or minimum principal stress.
	 *
	 * @param isMax
	 *            True if maximum principal stress is requested.
	 *
	 * @return Maximum or minimum principal stress.
	 */
	private static String getPrincipal(boolean isMax) {
		String a = "0.5 * (sx + sy)";
		String b = "sqrt(power(0.5 * (sx - sy), 2.0) + power(sxy, 2.0))";
		String sigma1 = a + " + " + b;
		String sigma2 = a + " - " + b;
		String max = "maximum(" + sigma1 + ", " + sigma2 + ") as maxprin";
		String min = "minimum(" + sigma1 + ", " + sigma2 + ") as minprin";
		return isMax ? max : min;
	}
}
