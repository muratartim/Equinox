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
package equinox.data.input;

/**
 * Class for histogram input.
 *
 * @author Murat Artim
 * @date Jul 4, 2014
 * @time 2:15:39 PM
 */
public class HistogramInput {

	/**
	 * Enumeration for histogram data type.
	 *
	 * @author Murat Artim
	 * @date Apr 22, 2014
	 * @time 11:03:26 AM
	 */
	public enum HistogramDataType {

	/** Histogram data type. */
	MEAN_STRESS("Mean stress", "mean_val"), STRESS_AMPLITUDE("Stress amplitude", "amp_val"), R_RATIO("R-ratio", "r_ratio"), MAX_STRESS("Maximum stress", "max_val"), MIN_STRESS("Minimum stress", "min_val"), STRESS_RANGE("Stress range", "range_val");

		/** Name of data type. */
		private final String name_, dbColName_;

		/**
		 * Creates histogram data type constant.
		 *
		 * @param name
		 *            Name of histogram data type.
		 * @param dbColName
		 *            Database column name.
		 */
		HistogramDataType(String name, String dbColName) {
			name_ = name;
			dbColName_ = dbColName;
		}

		/**
		 * Returns the name of histogram data type.
		 * 
		 * @return The name of histogram data type.
		 */
		public String getName() {
			return name_;
		}

		/**
		 * Returns the database column name.
		 *
		 * @return The database column name.
		 */
		public String getDBColumnName() {
			return dbColName_;
		}
	}

	/** Order and data label options. */
	private boolean descending_ = true, labelsVisible_ = true;

	/** Number of data limit. */
	private int limit_ = 10, digits_ = 2;

	/** Histogram data type. */
	private HistogramDataType dataType_ = HistogramDataType.MEAN_STRESS;

	/**
	 * Sets number of digits after comma.
	 *
	 * @param digits
	 *            Number of digits after comma.
	 */
	public void setDigits(int digits) {
		digits_ = digits;
	}

	/**
	 * Sets order.
	 *
	 * @param descending
	 *            True if descending.
	 */
	public void setOrder(boolean descending) {
		descending_ = descending;
	}

	/**
	 * Sets number of flights/events limit.
	 *
	 * @param limit
	 *            Number of flights/events limit.
	 */
	public void setLimit(int limit) {
		limit_ = limit;
	}

	/**
	 * Sets data label visibility.
	 *
	 * @param isVisible
	 *            True if visible.
	 */
	public void setLabelsVisible(boolean isVisible) {
		labelsVisible_ = isVisible;
	}

	/**
	 * Sets histogram data type.
	 *
	 * @param dataType
	 *            Data type to set.
	 */
	public void setDataType(HistogramDataType dataType) {
		dataType_ = dataType;
	}

	/**
	 * Returns number of digits after comma.
	 *
	 * @return Number of digits after comma.
	 */
	public int getDigits() {
		return digits_;
	}

	/**
	 * Returns true if descending order.
	 *
	 * @return True if descending order.
	 */
	public boolean getOrder() {
		return descending_;
	}

	/**
	 * Returns number of flights/events limit.
	 *
	 * @return Number of flights/events limit.
	 */
	public int getLimit() {
		return limit_;
	}

	/**
	 * Returns true if data labels are visible.
	 *
	 * @return True if data labels are visible.
	 */
	public boolean getLabelsVisible() {
		return labelsVisible_;
	}

	/**
	 * Returns histogram data type.
	 *
	 * @return Histogram data type.
	 */
	public HistogramDataType getDataType() {
		return dataType_;
	}
}
