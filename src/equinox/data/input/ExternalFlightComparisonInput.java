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

/**
 * Class for external flight comparison input.
 *
 * @author Murat Artim
 * @date Mar 15, 2015
 * @time 5:37:54 PM
 */
public class ExternalFlightComparisonInput {

	/** Peak option. */
	private boolean showMarkers_ = false, includeFlightName_ = true, includeSequenceName_ = false, includeEID_ = false, includeProgram_ = false, includeSection_ = false, includeMission_ = false;

	/** Show/hide flights. */
	private final HashMap<Integer, Boolean> showFlights_;

	/**
	 * Creates plot input.
	 */
	public ExternalFlightComparisonInput() {
		showFlights_ = new HashMap<>();
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
	 * Sets whether the stress sequence name should be included in the equivalent stress comparison.
	 *
	 * @param includeSequenceName
	 *            True to include stress sequence name.
	 */
	public void setIncludeSequenceName(boolean includeSequenceName) {
		includeSequenceName_ = includeSequenceName;
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
	 * Returns true if the stress sequence name should be included in the equivalent stress comparison.
	 *
	 * @return True if the stress sequence name should be included in the equivalent stress comparison.
	 */
	public boolean getIncludeSequenceName() {
		return includeSequenceName_;
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
