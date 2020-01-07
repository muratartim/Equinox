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

import java.util.HashMap;

import equinox.data.Segment;

/**
 * Class for flight comparison input.
 *
 * @author Murat Artim
 * @date Sep 16, 2014
 * @time 10:30:56 AM
 */
public class FlightComparisonInput {

	/** Plot component option index. */
	public static final int INCREMENT_STRESS_COMP = 0, DP_STRESS_COMP = 1, DT_STRESS_COMP = 2, ONE_G_STRESS_COMP = 3;

	/** Segment to plot. */
	private final Segment segment_;

	/** Peak option. */
	private boolean plotOnTotalStress_ = false, showMarkers_ = false, includeFlightName_ = true, includeSpectrumName_ = false, includeSTFName_ = false, includeEID_ = false, includeSequenceName_ = false, includeProgram_ = false, includeSection_ = false, includeMission_ = false;

	/** Plot component options. */
	private boolean[] plotComponentOptions_ = { true, true, true, true };

	/** Show/hide flights. */
	private final HashMap<Integer, Boolean> showFlights_;

	/**
	 * Creates plot input.
	 *
	 * @param segment
	 *            Segment to plot. Null should be given for all segments.
	 */
	public FlightComparisonInput(Segment segment) {
		segment_ = segment;
		showFlights_ = new HashMap<>();
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
	 * Sets the flight with the given ID visible or hidden.
	 *
	 * @param flightID
	 *            Flight ID.
	 * @param visible
	 *            True for visible.
	 */
	public void setFlightVisible(int flightID, boolean visible) {
		showFlights_.put(flightID, visible);
	}

	/**
	 * Sets marker display.
	 *
	 * @param showMarkers
	 *            True to show markers.
	 */
	public void setShowMarkers(boolean showMarkers) {
		showMarkers_ = showMarkers;
	}

	/**
	 * Sets whether the flight name should be included in the equivalent stress comparison.
	 *
	 * @param includeFlightName
	 *            True to include flight name.
	 */
	public void setIncludeFlightName(boolean includeFlightName) {
		includeFlightName_ = includeFlightName;
	}

	/**
	 * Sets whether the spectrum name should be included in the equivalent stress comparison.
	 *
	 * @param includeSpectrumName
	 *            True to include spectrum name.
	 */
	public void setIncludeSpectrumName(boolean includeSpectrumName) {
		includeSpectrumName_ = includeSpectrumName;
	}

	/**
	 * Sets whether the STF file name should be included in the equivalent stress comparison.
	 *
	 * @param includeSTFName
	 *            True to include STF file name.
	 */
	public void setIncludeSTFName(boolean includeSTFName) {
		includeSTFName_ = includeSTFName;
	}

	/**
	 * Sets whether the element ID should be included in the equivalent stress comparison.
	 *
	 * @param includeEID
	 *            True to include element ID.
	 */
	public void setIncludeEID(boolean includeEID) {
		includeEID_ = includeEID;
	}

	/**
	 * Sets whether the stress sequence name should be included in the equivalent stress comparison.
	 *
	 * @param includeSequenceName
	 *            True to include stress sequence name.
	 */
	public void setIncludeSequenceName(boolean includeSequenceName) {
		includeSequenceName_ = includeSequenceName;
	}

	/**
	 * Sets whether the A/C program should be included in the equivalent stress comparison.
	 *
	 * @param includeProgram
	 *            True to include A/C program.
	 */
	public void setIncludeProgram(boolean includeProgram) {
		includeProgram_ = includeProgram;
	}

	/**
	 * Sets whether the A/C section should be included in the equivalent stress comparison.
	 *
	 * @param includeSection
	 *            True to include A/C section.
	 */
	public void setIncludeSection(boolean includeSection) {
		includeSection_ = includeSection;
	}

	/**
	 * Sets whether the fatigue mission should be included in the equivalent stress comparison.
	 *
	 * @param includeMission
	 *            True to include fatigue mission.
	 */
	public void setIncludeMission(boolean includeMission) {
		includeMission_ = includeMission;
	}

	/**
	 * Returns segment.
	 *
	 * @return Segment.
	 */
	public Segment getSegment() {
		return segment_;
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
	 * Returns true if markers should be shown.
	 *
	 * @return True if markers should be shown.
	 */
	public boolean isShowMarkers() {
		return showMarkers_;
	}

	/**
	 * Returns true if the flight with the given ID is visible.
	 *
	 * @param flightID
	 *            Flight ID.
	 * @return True if the flight with the given ID is visible.
	 */
	public boolean isFlightVisible(int flightID) {
		return showFlights_.containsKey(flightID) ? showFlights_.get(flightID) : true;
	}

	/**
	 * Returns true if the flight name should be included in the equivalent stress comparison.
	 *
	 * @return True if the flight name should be included in the equivalent stress comparison.
	 */
	public boolean getIncludeFlightName() {
		return includeFlightName_;
	}

	/**
	 * Returns true if the spectrum name should be included in the equivalent stress comparison.
	 *
	 * @return True if the spectrum name should be included in the equivalent stress comparison.
	 */
	public boolean getIncludeSpectrumName() {
		return includeSpectrumName_;
	}

	/**
	 * Returns true if the STF file name should be included in the equivalent stress comparison.
	 *
	 * @return True if the STF file name should be included in the equivalent stress comparison.
	 */
	public boolean getIncludeSTFName() {
		return includeSTFName_;
	}

	/**
	 * Returns true if the element ID should be included in the equivalent stress comparison.
	 *
	 * @return True if the element ID should be included in the equivalent stress comparison.
	 */
	public boolean getIncludeEID() {
		return includeEID_;
	}

	/**
	 * Returns true if the stress sequence name should be included in the equivalent stress comparison.
	 *
	 * @return True if the stress sequence name should be included in the equivalent stress comparison.
	 */
	public boolean getIncludeSequenceName() {
		return includeSequenceName_;
	}

	/**
	 * Returns true if the A/C program should be included in the equivalent stress comparison.
	 *
	 * @return True if the A/C program should be included in the equivalent stress comparison.
	 */
	public boolean getIncludeProgram() {
		return includeProgram_;
	}

	/**
	 * Returns true if the A/C section should be included in the equivalent stress comparison.
	 *
	 * @return True if the A/C section should be included in the equivalent stress comparison.
	 */
	public boolean getIncludeSection() {
		return includeSection_;
	}

	/**
	 * Returns true if the fatigue mission should be included in the equivalent stress comparison.
	 *
	 * @return True if the fatigue mission should be included in the equivalent stress comparison.
	 */
	public boolean getIncludeMission() {
		return includeMission_;
	}
}
