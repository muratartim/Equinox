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

import java.util.List;

/**
 * Class for level crossing input.
 *
 * @author Murat Artim
 * @date Jul 21, 2014
 * @time 10:40:18 AM
 */
public class LevelCrossingInput {

	/** True if the DSGs should be normalized with spectrum validities. */
	private final boolean normalize_;

	/** DSGs. */
	private final List<Integer> dsgs_;

	/** Spectrum naming parameters. */
	private boolean includeSpectrumName_ = false, includeSTFName_ = true, includeEID_ = false, includeSequenceName_ = false, includeMaterialName_ = true, includeOmissionLevel_ = false, includeProgram_ = false, includeSection_ = false, includeMission_ = false;

	/**
	 * Creates level crossing input.
	 *
	 * @param normalize
	 *            True if the DSGs should be normalized with spectrum validities.
	 * @param dsgs
	 *            DSGs. Can be null for normalizing.
	 */
	public LevelCrossingInput(boolean normalize, List<Integer> dsgs) {
		normalize_ = normalize;
		dsgs_ = dsgs;
	}

	/**
	 * Returns true if the DSGs should be normalized with spectrum validities.
	 *
	 * @return True if the DSGs should be normalized with spectrum validities.
	 */
	public boolean isNormalize() {
		return normalize_;
	}

	/**
	 * Returns DSGs.
	 * 
	 * @return DSGs.
	 */
	public List<Integer> getDsgs() {
		return dsgs_;
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
	 * Sets whether the material name should be included in the equivalent stress comparison.
	 *
	 * @param includeMaterialName
	 *            True to include material name.
	 */
	public void setIncludeMaterialName(boolean includeMaterialName) {
		includeMaterialName_ = includeMaterialName;
	}

	/**
	 * Sets whether the omission level should be included in the equivalent stress comparison.
	 *
	 * @param includeOmissionLevel
	 *            True to include omission level.
	 */
	public void setIncludeOmissionLevel(boolean includeOmissionLevel) {
		includeOmissionLevel_ = includeOmissionLevel;
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
	 * Returns true if the material name should be included in the equivalent stress comparison.
	 *
	 * @return True if the material name should be included in the equivalent stress comparison.
	 */
	public boolean getIncludeMaterialName() {
		return includeMaterialName_;
	}

	/**
	 * Returns true if the omission level should be included in the equivalent stress comparison.
	 *
	 * @return True if the omission level should be included in the equivalent stress comparison.
	 */
	public boolean getIncludeOmissionLevel() {
		return includeOmissionLevel_;
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
