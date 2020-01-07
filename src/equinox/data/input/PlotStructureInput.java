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

import equinox.data.ElementType;
import equinox.data.fileType.AircraftModel;

/**
 * Class for plot A/C structure input data.
 *
 * @author Murat Artim
 * @date Jul 10, 2015
 * @time 2:42:57 PM
 */
public class PlotStructureInput {

	/** A/C model. */
	private final AircraftModel model_;

	/** Element types. */
	private final ArrayList<ElementType> types_;

	/** Element groups and positions. */
	private final ArrayList<String> groups_;

	/** 1D element extrusion widths. */
	private int beamExtrusion_, rodExtrusion_;

	/**
	 * Creates plot A/C structure input.
	 *
	 * @param model
	 *            A/C model.
	 */
	public PlotStructureInput(AircraftModel model) {
		model_ = model;
		types_ = new ArrayList<>();
		groups_ = new ArrayList<>();
	}

	/**
	 * Returns A/C model.
	 *
	 * @return A/C model.
	 */
	public AircraftModel getModel() {
		return model_;
	}

	/**
	 * Returns element types.
	 *
	 * @return Element types.
	 */
	public ArrayList<ElementType> getTypes() {
		return types_;
	}

	/**
	 * Returns demanded element type.
	 *
	 * @param name
	 *            Name of type.
	 * @return Demanded element type.
	 */
	public ElementType getType(String name) {
		for (ElementType type : types_) {
			if (type.getName().equals(name))
				return type;
		}
		return null;
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
	 * Returns BEAM element extrusion width.
	 *
	 * @return BEAM element extrusion width.
	 */
	public int getBeamExtrusionWidth() {
		return beamExtrusion_;
	}

	/**
	 * Returns ROD element extrusion width.
	 *
	 * @return ROD element extrusion width.
	 */
	public int getRodExtrusionWidth() {
		return rodExtrusion_;
	}

	/**
	 * Adds element type.
	 *
	 * @param type
	 *            Element type.
	 */
	public void addType(ElementType type) {
		types_.add(type);
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

	/**
	 * Sets BEAM element extrusion width.
	 *
	 * @param width
	 *            Width.
	 */
	public void setBeamExtrusionWidth(int width) {
		beamExtrusion_ = width;
	}

	/**
	 * Sets ROD element extrusion width.
	 *
	 * @param width
	 *            Width.
	 */
	public void setRodExtrusionWidth(int width) {
		rodExtrusion_ = width;
	}
}
