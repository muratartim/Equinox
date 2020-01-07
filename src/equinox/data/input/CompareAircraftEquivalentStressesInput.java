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

import equinox.data.ElementTypeForStress;
import equinox.data.fileType.AircraftFatigueEquivalentStress;

/**
 * Class for compare A/C equivalent stresses input.
 *
 * @author Murat Artim
 * @date Sep 23, 2015
 * @time 12:57:15 PM
 */
public class CompareAircraftEquivalentStressesInput {

	/** Equivalent stress. */
	private final AircraftFatigueEquivalentStress equivalentStress_;

	/** Equivalent stress type. */
	private final AircraftEquivalentStressType stressType_;

	/** Element type. */
	private final ElementTypeForStress elementType_;

	/** Element groups. */
	private final ArrayList<String> groups_;

	/** Number of flights/events limit. */
	private int limit_ = 10;

	/** Order and data label options. */
	private boolean descending_ = true, showlabels_ = true;

	/**
	 * Creates compare A/C equivalent stresses input.
	 *
	 * @param equivalentStress
	 *            Equivalent stress.
	 * @param stressType
	 *            Equivalent stress type.
	 * @param elementType
	 *            Element type.
	 */
	public CompareAircraftEquivalentStressesInput(AircraftFatigueEquivalentStress equivalentStress, AircraftEquivalentStressType stressType,
			ElementTypeForStress elementType) {
		equivalentStress_ = equivalentStress;
		stressType_ = stressType;
		elementType_ = elementType;
		groups_ = new ArrayList<>();
	}

	/**
	 * Returns equivalent stress.
	 *
	 * @return Equivalent stress.
	 */
	public AircraftFatigueEquivalentStress getEquivalentStress() {
		return equivalentStress_;
	}

	/**
	 * Returns equivalent stress type.
	 *
	 * @return Equivalent stress type.
	 */
	public AircraftEquivalentStressType getStressType() {
		return stressType_;
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
