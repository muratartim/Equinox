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
 * Class for external level crossing input.
 *
 * @author Murat Artim
 * @date Mar 15, 2015
 * @time 3:53:44 PM
 */
public class ExternalLevelCrossingInput {

	/** True if the DSGs should be normalized with spectrum validities. */
	private final boolean normalize_;

	/** DSGs. */
	private final int[] dsgs_;

	/** Spectrum naming parameters. */
	private boolean includeSequenceName_ = false, includeEID_ = false, includeMaterialName_ = true, includeOmissionLevel_ = false, includeProgram_ = false, includeSection_ = false, includeMission_ = false;

	/**
	 * Creates external level crossing input.
	 *
	 * @param normalize
	 *            True if the DSGs should be normalized with spectrum validities.
	 * @param dsgs
	 *            DSGs. Null can be given if normalize option is selected.
	 */
	public ExternalLevelCrossingInput(boolean normalize, int[] dsgs) {
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
	 * @return DSGs, or null if normalize option is selected.
	 */
	public int[] getDSGs() {
		return dsgs_;
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
