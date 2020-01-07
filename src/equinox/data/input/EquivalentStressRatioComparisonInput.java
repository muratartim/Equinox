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
 * Class for equivalent stress ratio comparison input.
 *
 * @author Murat Artim
 * @date Oct 9, 2014
 * @time 12:37:23 PM
 */
public class EquivalentStressRatioComparisonInput {

	/** Order and data label options. */
	private boolean showlabels_ = true, includeBasis_ = true, includeSpectrumName_ = false, includeSTFName_ = true, includeEID_ = false, includeSequenceName_ = false, includeMaterialName_ = true, includeOmissionLevel_ = false, includeProgram_ = false, includeSection_ = false,
			includeMission_ = false;

	/** Mission parameter name. */
	private String basisMission_, missionParameterName_;

	/**
	 * Sets basis mission.
	 *
	 * @param basisMission
	 *            Basis mission.
	 */
	public void setBasisMission(String basisMission) {
		basisMission_ = basisMission;
	}

	/**
	 * Sets mission parameter name.
	 *
	 * @param name
	 *            Mission parameter name.
	 */
	public void setMissionParameterName(String name) {
		missionParameterName_ = name;
	}

	/**
	 * Sets whether the spectrum name should be included in the life factor comparison.
	 *
	 * @param includeSpectrumName
	 *            True to include spectrum name.
	 */
	public void setIncludeSpectrumName(boolean includeSpectrumName) {
		includeSpectrumName_ = includeSpectrumName;
	}

	/**
	 * Sets whether the STF file name should be included in the life factor comparison.
	 *
	 * @param includeSTFName
	 *            True to include STF file name.
	 */
	public void setIncludeSTFName(boolean includeSTFName) {
		includeSTFName_ = includeSTFName;
	}

	/**
	 * Sets whether the stress sequence name should be included in the life factor comparison.
	 *
	 * @param includeSequenceName
	 *            True to include stress sequence name.
	 */
	public void setIncludeSequenceName(boolean includeSequenceName) {
		includeSequenceName_ = includeSequenceName;
	}

	/**
	 * Sets whether the element ID should be included in the life factor comparison.
	 *
	 * @param includeEID
	 *            True to include element ID.
	 */
	public void setIncludeEID(boolean includeEID) {
		includeEID_ = includeEID;
	}

	/**
	 * Sets whether the material name should be included in the life factor comparison.
	 *
	 * @param includeMaterialName
	 *            True to include material name.
	 */
	public void setIncludeMaterialName(boolean includeMaterialName) {
		includeMaterialName_ = includeMaterialName;
	}

	/**
	 * Sets whether the omission level should be included in the life factor comparison.
	 *
	 * @param includeOmissionLevel
	 *            True to include omission level.
	 */
	public void setIncludeOmissionLevel(boolean includeOmissionLevel) {
		includeOmissionLevel_ = includeOmissionLevel;
	}

	/**
	 * Sets whether the A/C program should be included in the life factor comparison.
	 *
	 * @param includeProgram
	 *            True to include A/C program.
	 */
	public void setIncludeProgram(boolean includeProgram) {
		includeProgram_ = includeProgram;
	}

	/**
	 * Sets whether the A/C section should be included in the life factor comparison.
	 *
	 * @param includeSection
	 *            True to include A/C section.
	 */
	public void setIncludeSection(boolean includeSection) {
		includeSection_ = includeSection;
	}

	/**
	 * Sets whether the fatigue mission should be included in the life factor comparison.
	 *
	 * @param includeMission
	 *            True to include fatigue mission.
	 */
	public void setIncludeMission(boolean includeMission) {
		includeMission_ = includeMission;
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
	 * Sets include basis mission option.
	 *
	 * @param includeBasis
	 *            True if the basis mission should be included in the plot.
	 */
	public void setIncludeBasisMission(boolean includeBasis) {
		includeBasis_ = includeBasis;
	}

	/**
	 * Returns basis mission.
	 *
	 * @return Basis mission.
	 */
	public String getBasisMission() {
		return basisMission_;
	}

	/**
	 * Returns mission parameter name.
	 *
	 * @return Mission parameter name.
	 */
	public String getMissionParameterName() {
		return missionParameterName_;
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
	 * Returns true if basis mission should be included in the plots.
	 *
	 * @return True if basis mission should be included in the plots.
	 */
	public boolean getIncludeBasisMission() {
		return includeBasis_;
	}

	/**
	 * Returns true if the spectrum name should be included in the life factor comparison.
	 *
	 * @return True if the spectrum name should be included in the life factor comparison.
	 */
	public boolean getIncludeSpectrumName() {
		return includeSpectrumName_;
	}

	/**
	 * Returns true if the STF file name should be included in the life factor comparison.
	 *
	 * @return True if the STF file name should be included in the life factor comparison.
	 */
	public boolean getIncludeSTFName() {
		return includeSTFName_;
	}

	/**
	 * Returns true if the stress sequence name should be included in the life factor comparison.
	 *
	 * @return True if the stress sequence name should be included in the life factor comparison.
	 */
	public boolean getIncludeSequenceName() {
		return includeSequenceName_;
	}

	/**
	 * Returns true if the element ID should be included in the life factor comparison.
	 *
	 * @return True if the element ID should be included in the life factor comparison.
	 */
	public boolean getIncludeEID() {
		return includeEID_;
	}

	/**
	 * Returns true if the material name should be included in the life factor comparison.
	 *
	 * @return True if the material name should be included in the life factor comparison.
	 */
	public boolean getIncludeMaterialName() {
		return includeMaterialName_;
	}

	/**
	 * Returns true if the omission level should be included in the life factor comparison.
	 *
	 * @return True if the omission level should be included in the life factor comparison.
	 */
	public boolean getIncludeOmissionLevel() {
		return includeOmissionLevel_;
	}

	/**
	 * Returns true if the A/C program should be included in the life factor comparison.
	 *
	 * @return True if the A/C program should be included in the life factor comparison.
	 */
	public boolean getIncludeProgram() {
		return includeProgram_;
	}

	/**
	 * Returns true if the A/C section should be included in the life factor comparison.
	 *
	 * @return True if the A/C section should be included in the life factor comparison.
	 */
	public boolean getIncludeSection() {
		return includeSection_;
	}

	/**
	 * Returns true if the fatigue mission should be included in the life factor comparison.
	 *
	 * @return True if the fatigue mission should be included in the life factor comparison.
	 */
	public boolean getIncludeMission() {
		return includeMission_;
	}
}
