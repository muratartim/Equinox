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

import equinox.data.ElementStress;
import equinox.data.fileType.AircraftLoadCase;

/**
 * Class for compare load cases input.
 *
 * @author Murat Artim
 * @date Sep 10, 2015
 * @time 10:56:57 AM
 */
public class CompareLoadCasesInput {

	/** Load cases. */
	private final ArrayList<AircraftLoadCase> loadCases_;

	/** Element IDs. */
	private final int[] eids_;

	/** Stress component. */
	private final ElementStress stressComponent_;

	/** True if the data labels should be shown. */
	private final boolean showDataLabels_;

	/**
	 * Creates compare load cases input.
	 *
	 * @param loadCases
	 *            Load cases.
	 * @param eids
	 *            Element IDs.
	 * @param stressComponent
	 *            Stress component.
	 * @param showDataLabels
	 *            True if the data labels should be shown.
	 */
	public CompareLoadCasesInput(ArrayList<AircraftLoadCase> loadCases, int[] eids, ElementStress stressComponent, boolean showDataLabels) {
		loadCases_ = loadCases;
		eids_ = eids;
		stressComponent_ = stressComponent;
		showDataLabels_ = showDataLabels;
	}

	/**
	 * Returns load cases.
	 *
	 * @return Load cases.
	 */
	public ArrayList<AircraftLoadCase> getLoadCases() {
		return loadCases_;
	}

	/**
	 * Returns element IDs.
	 *
	 * @return Element IDs.
	 */
	public int[] getEIDs() {
		return eids_;
	}

	/**
	 * Returns stress component.
	 *
	 * @return Stress component.
	 */
	public ElementStress getStressComponent() {
		return stressComponent_;
	}

	/**
	 * Returns true if data labels should be shown.
	 *
	 * @return True if data labels should be shown.
	 */
	public boolean showDataLabels() {
		return showDataLabels_;
	}
}
