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
 * Class for external statistics input data.
 *
 * @author Murat Artim
 * @date Mar 13, 2015
 * @time 4:37:21 PM
 */
public class ExternalStatisticsInput {

	/**
	 * Enumeration for statistics.
	 *
	 * @author Murat Artim
	 * @date Apr 22, 2014
	 * @time 11:03:26 AM
	 */
	public enum ExternalStatistic {

	/** Statistic. */
	NUM_PEAKS("Number of peaks"), FLIGHT_OCCURRENCE("Flight occurrence"), MAX_PEAK("Maximum stress"), MIN_PEAK("Minimum stress");

		/** Name of statistic. */
		private final String name_;

		/**
		 * Creates statistic constant.
		 *
		 * @param name
		 *            Name of statistic.
		 */
		ExternalStatistic(String name) {
			name_ = name;
		}

		@Override
		public String toString() {
			return name_;
		}
	}

	/** Order and data label options. */
	private boolean descending_ = true, showlabels_ = true;

	/** Number of flights limit. */
	private int limit_ = 10;

	/** Statistic type. */
	private ExternalStatistic statistic_ = ExternalStatistic.NUM_PEAKS;

	/**
	 * Sets the statistic.
	 *
	 * @param statistic
	 *            Statistic to set.
	 */
	public void setStatistic(ExternalStatistic statistic) {
		statistic_ = statistic;
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
	 * Sets label display option.
	 *
	 * @param showLabels
	 *            True to show labels.
	 */
	public void setLabelDisplay(boolean showLabels) {
		showlabels_ = showLabels;
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
	 * Returns statistic.
	 *
	 * @return Statistic.
	 */
	public ExternalStatistic getStatistic() {
		return statistic_;
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
	 * Returns true if data labels are shown.
	 *
	 * @return True if data labels are shown.
	 */
	public boolean getLabelDisplay() {
		return showlabels_;
	}
}
