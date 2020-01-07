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

import equinox.data.fileType.Flight;

/**
 * Class for STH flight plot input.
 *
 * @author Murat Artim
 * @date Sep 2, 2014
 * @time 5:45:56 PM
 */
public class FlightPlotInput {

	/** Plot component option index. */
	public static final int INCREMENT_STRESS_COMP = 0, DP_STRESS_COMP = 1, DT_STRESS_COMP = 2, ONE_G_STRESS_COMP = 3;

	/** Flights to plot. */
	private final Flight[] flights_;

	/** Peak option. */
	private boolean plotOnTotalStress_ = false;

	/** Plot component options. */
	private boolean[] plotComponentOptions_ = { true, true, true, true }, namingOptions_ = { false, false, false, false, true, false, false, false };

	/**
	 * Creates plot input.
	 *
	 * @param flights
	 *            Flights to plot.
	 */
	public FlightPlotInput(Flight[] flights) {
		flights_ = flights;
	}

	/**
	 * Sets plot component options.
	 *
	 * @param options
	 *            Component plot options.
	 * @param plotOnTotalStress
	 *            True if the components should be plotted on total stresses.
	 */
	public void setPlotComponentOptions(boolean[] options, boolean plotOnTotalStress) {
		plotComponentOptions_ = options;
		plotOnTotalStress_ = plotOnTotalStress;
	}

	/**
	 * Sets series naming options.
	 *
	 * @param options
	 *            Series naming options.
	 */
	public void setNamingOptions(boolean[] options) {
		namingOptions_ = options;
	}

	/**
	 * Returns the flights to plot.
	 *
	 * @return The flights to plot.
	 */
	public Flight[] getFlights() {
		return flights_;
	}

	/**
	 * Returns demanded plot component option.
	 *
	 * @param index
	 *            Index of demanded plot component option.
	 * @return The demanded plot component option.
	 */
	public boolean getPlotComponentOption(int index) {
		return plotComponentOptions_[index];
	}

	/**
	 * Returns all plot component options.
	 *
	 * @return All plot component options.
	 */
	public boolean[] getPlotComponentOptions() {
		return plotComponentOptions_;
	}

	/**
	 * Returns true if total stresses should be plotted.
	 *
	 * @return True if total stresses should be plotted.
	 */
	public boolean isPlotTotalStress() {
		for (boolean option : plotComponentOptions_) {
			if (!option)
				return false;
		}
		return true;
	}

	/**
	 * Returns true if plot components on total stresses is selected.
	 *
	 * @return True if plot components on total stresses is selected.
	 */
	public boolean isPlotOnTotalStress() {
		return plotOnTotalStress_;
	}

	/**
	 * Returns series naming options.
	 *
	 * @return Series naming options.
	 */
	public boolean[] getNamingOptions() {
		return namingOptions_;
	}
}
