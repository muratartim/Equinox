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
 * Class for external stress sequence comparison input.
 *
 * @author Murat Artim
 * @date Mar 14, 2015
 * @time 8:37:43 PM
 */
public class ExternalStressSequenceComparisonInput {

	/**
	 * Enumeration for comparison criteria.
	 *
	 * @author Murat Artim
	 * @date Apr 22, 2014
	 * @time 11:03:26 AM
	 */
	public enum ExternalComparisonCriteria {

	/** Statistic. */
	NUM_FLIGHT_TYPES("Number of flight types"), NUM_PEAKS_WITH_OCCURRENCE("Number of peaks w/ occurrences"), NUM_PEAKS_WITHOUT_OCCURRENCE("Number of peaks w/o occurrences"), VALIDITY("Number of flights"), MAX_PEAK("Maximum stress"), MIN_PEAK("Minimum stress");

		/** Name of comparison criteria. */
		private final String name_;

		/**
		 * Creates comparison criteria constant.
		 *
		 * @param name
		 *            Name of comparison criteria.
		 */
		ExternalComparisonCriteria(String name) {
			name_ = name;
		}

		/**
		 * Returns the name of comparison criteria.
		 * 
		 * @return The name of comparison criteria.
		 */
		public String getName() {
			return name_;
		}
	}

	/** Order and data label options. */
	private boolean descending_ = true, showlabels_ = true, includeSequenceName_ = true, includeEID_ = false, includeProgram_ = false, includeSection_ = false, includeMission_ = false;

	/** Criteria type. */
	private ExternalComparisonCriteria criteria_ = ExternalComparisonCriteria.NUM_FLIGHT_TYPES;

	/**
	 * Sets the criteria.
	 *
	 * @param criteria
	 *            Criteria to set.
	 */
	public void setCriteria(ExternalComparisonCriteria criteria) {
		criteria_ = criteria;
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
	 * Returns criteria.
	 *
	 * @return Criteria.
	 */
	public ExternalComparisonCriteria getCriteria() {
		return criteria_;
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
	 * Returns true if data labels are shown.
	 *
	 * @return True if data labels are shown.
	 */
	public boolean getLabelDisplay() {
		return showlabels_;
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
