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
 * Class for stress sequence comparison input.
 *
 * @author Murat Artim
 * @date Jul 8, 2014
 * @time 11:36:53 AM
 */
public class StressSequenceComparisonInput {

	/**
	 * Enumeration for comparison criteria.
	 *
	 * @author Murat Artim
	 * @date Apr 22, 2014
	 * @time 11:03:26 AM
	 */
	public enum ComparisonCriteria {

	/** Statistic. */
	NUM_FLIGHT_TYPES("Number of flight types"), NUM_PEAKS_WITH_OCCURRENCE("Number of peaks with occurrences"), NUM_PEAKS_WITHOUT_OCCURRENCE("Number of peaks without occurrences"), VALIDITY("Number of flights"), AVG_NUM_PEAKS("Average number of peaks"), MAX_TOTAL_STRESS("Maximum total stress"),
	MAX_1G_STRESS("Maximum 1g stress"), MAX_INC_STRESS("Maximum increment stress"), MAX_DP_STRESS("Maximum delta-p stress"), MAX_DT_STRESS("Maximum delta-t stress"), MIN_TOTAL_STRESS("Minimum total stress"), MIN_1G_STRESS("Minimum 1g stress"), MIN_INC_STRESS("Minimum increment stress"), MIN_DP_STRESS("Minimum delta-p stress"),
	MIN_DT_STRESS("Minimum delta-t stress");

		/** Name of comparison criteria. */
		private final String name_;

		/**
		 * Creates comparison criteria constant.
		 *
		 * @param name
		 *            Name of comparison criteria.
		 */
		ComparisonCriteria(String name) {
			name_ = name;
		}

		/**
		 * Returns display name of comparison criteria.
		 * 
		 * @return Display name.
		 */
		public String getName() {
			return name_;
		}
	}

	/** Order and data label options. */
	private boolean descending_ = true, showlabels_ = true, includeSpectrumName_ = false, includeSTFName_ = false, includeEID_ = false, includeSequenceName_ = true, includeProgram_ = false, includeSection_ = false, includeMission_ = false;

	/** Criteria type. */
	private ComparisonCriteria criteria_ = ComparisonCriteria.NUM_FLIGHT_TYPES;

	/**
	 * Sets the criteria.
	 *
	 * @param criteria
	 *            Criteria to set.
	 */
	public void setCriteria(ComparisonCriteria criteria) {
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
	 * Returns criteria.
	 *
	 * @return Criteria.
	 */
	public ComparisonCriteria getCriteria() {
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
