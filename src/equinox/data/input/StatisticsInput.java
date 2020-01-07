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

import java.util.ArrayList;

import equinox.data.LoadcaseItem;
import equinox.data.fileType.Flight;

/**
 * Class for statistics input.
 *
 * @author Murat Artim
 * @date Apr 22, 2014
 * @time 10:57:45 AM
 */
public class StatisticsInput {

	/**
	 * Enumeration for statistics.
	 *
	 * @author Murat Artim
	 * @date Apr 22, 2014
	 * @time 11:03:26 AM
	 */
	public enum Statistic {

		/** Statistic. */
		NUM_PEAKS("Number of peaks"), FLIGHT_OCCURRENCE("Flight occurrence"), LOADCASE_OCCURRENCE("Loadcase occurrence"), MAX_TOTAL("Maximum total stress"), MAX_1G("Maximum 1g stress"), MAX_INC("Maximum increment stress"), MAX_DP("Maximum delta-p stress"), MAX_DT("Maximum delta-t stress"), MIN_TOTAL("Minimum total stress"), MIN_1G("Minimum 1g stress"), MIN_INC("Minimum increment stress"), MIN_DP("Minimum delta-p stress"), MIN_DT("Minimum delta-t stress");

		/** Name of statistic. */
		private final String name_;

		/**
		 * Creates statistic constant.
		 *
		 * @param name
		 *            Name of statistic.
		 */
		Statistic(String name) {
			name_ = name;
		}

		@Override
		public String toString() {
			return name_;
		}
	}

	/** Flights to compare. */
	private final ArrayList<Flight> flights_ = new ArrayList<>();

	/** Order and data label options. */
	private boolean descending_ = true, showlabels_ = true, multiplyWithValidity_ = false, isOnegLoadcases_ = true;

	/** Number of flights/events limit. */
	private int limit_ = 10;

	/** Statistic type. */
	private Statistic statistic_ = Statistic.NUM_PEAKS;

	/** Events. */
	private final ArrayList<LoadcaseItem> loadcases_ = new ArrayList<>();

	/**
	 * Adds flight.
	 *
	 * @param flight
	 *            Flight to add.
	 */
	public void addFlight(Flight flight) {
		flights_.add(flight);
	}

	/**
	 * Adds loadcase.
	 *
	 * @param loadcase
	 *            Loadcase to add.
	 */
	public void addLoadcase(LoadcaseItem loadcase) {
		loadcases_.add(loadcase);
	}

	/**
	 * Sets the statistic.
	 *
	 * @param statistic
	 *            Statistic to set.
	 */
	public void setStatistic(Statistic statistic) {
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
	 * Sets validity multiplier option.
	 *
	 * @param multiplyWithValidity
	 *            True if the occurrences should be multiplied with validities.
	 */
	public void setValidityMultiplier(boolean multiplyWithValidity) {
		multiplyWithValidity_ = multiplyWithValidity;
	}

	/**
	 * Sets number of flights/loadcases limit.
	 *
	 * @param limit
	 *            Number of flights/loadcases limit.
	 */
	public void setLimit(int limit) {
		limit_ = limit;
	}

	/**
	 * Sets loadcase type.
	 *
	 * @param isOneg
	 *            True if 1g loadcases.
	 */
	public void setLoadcaseType(boolean isOneg) {
		isOnegLoadcases_ = isOneg;
	}

	/**
	 * Returns the flights to plot.
	 *
	 * @return The flights to plot.
	 */
	public ArrayList<Flight> getFlights() {
		return flights_;
	}

	/**
	 * Returns the loadcases.
	 *
	 * @return The loadcases.
	 */
	public ArrayList<LoadcaseItem> getLoadcases() {
		return loadcases_;
	}

	/**
	 * Returns statistic.
	 *
	 * @return Statistic.
	 */
	public Statistic getStatistic() {
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

	/**
	 * Returns true if the occurrences should be multiplied with validities.
	 *
	 * @return True if the occurrences should be multiplied with validities.
	 */
	public boolean getValidityMultiplier() {
		return multiplyWithValidity_;
	}

	/**
	 * Returns true if 1g loadcases.
	 *
	 * @return True if 1g loadcases.
	 */
	public boolean getLoadcaseType() {
		return isOnegLoadcases_;
	}
}
