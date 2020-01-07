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
import equinox.data.ElementTypeForStress;
import equinox.data.fileType.AircraftLoadCase;

/**
 * Class for compare element stresses input data.
 *
 * @author Murat Artim
 * @date Aug 26, 2015
 * @time 10:56:35 AM
 */
public class CompareElementStressesInput {

	/** Load case. */
	private final AircraftLoadCase loadCase_;

	/** Element groups. */
	private final ArrayList<String> groups_;

	/** Element type and stress component. */
	private final ElementTypeForStress elementType_;

	/** Element stress component. */
	private final ElementStress stressComponent_;

	/** Number of flights/events limit. */
	private int limit_ = 10;

	/** Order and data label options. */
	private boolean descending_ = true, showlabels_ = true;

	/**
	 * Creates compare element stresses input.
	 *
	 * @param loadCase
	 *            Load case.
	 * @param elementType
	 *            Element type.
	 * @param stressComponent
	 *            Stress component.
	 */
	public CompareElementStressesInput(AircraftLoadCase loadCase, ElementTypeForStress elementType, ElementStress stressComponent) {
		loadCase_ = loadCase;
		elementType_ = elementType;
		stressComponent_ = stressComponent;
		groups_ = new ArrayList<>();
	}

	/**
	 * Returns load case.
	 *
	 * @return Load case.
	 */
	public AircraftLoadCase getLoadCase() {
		return loadCase_;
	}

	/**
	 * Returns element type.
	 *
	 * @return Element type.
	 */
	public ElementTypeForStress getElementType() {
		return elementType_;
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
	 * Returns element groups.
	 *
	 * @return Element groups.
	 */
	public ArrayList<String> getGroups() {
		return groups_;
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
	 * Sets number of flights/events limit.
	 *
	 * @param limit
	 *            Number of flights/events limit.
	 */
	public void setLimit(int limit) {
		limit_ = limit;
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
	 * Adds element group.
	 *
	 * @param group
	 *            Element group.
	 */
	public void addGroup(String group) {
		groups_.add(group);
	}
}
