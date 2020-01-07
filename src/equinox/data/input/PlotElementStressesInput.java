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
import equinox.data.ui.LoadCaseFactorTableItem;
import equinox.data.ui.PlotContour;

/**
 * Class for plot element stresses input.
 *
 * @author Murat Artim
 * @date Aug 7, 2015
 * @time 2:25:41 PM
 */
public class PlotElementStressesInput {

	/** Element groups. */
	private final ArrayList<String> groups_;

	/** 1D element extrusion widths. */
	private final int beamExtrusion_, rodExtrusion_;

	/** Element opacity. */
	private final double opacity_;

	/** True if element outlines should be drawn. */
	private final boolean outlines_;

	/** Element type and stress component. */
	private final ElementTypeForStress elementType_;

	/** Element stress component. */
	private final ElementStress stressComponent_;

	/** Value range. */
	private Double lowerBound_ = null, upperBound_ = null;

	/** Load cases. */
	private final ArrayList<LoadCaseFactorTableItem> loadCases_;

	/** Plot contour option. */
	private final PlotContour contour_;

	/**
	 * Creates plot element stresses input.
	 *
	 * @param loadCases
	 *            Load cases.
	 * @param elementType
	 *            Element type.
	 * @param stressComponent
	 *            Stress component.
	 * @param beamExtrusion
	 *            BEAM element extrusion width.
	 * @param rodExtrusion
	 *            ROD element extrusion width.
	 * @param opacity
	 *            Element opacity.
	 * @param outlines
	 *            True if element outlines should be drawn.
	 * @param contour
	 *            Plot contour option.
	 */
	public PlotElementStressesInput(ArrayList<LoadCaseFactorTableItem> loadCases, ElementTypeForStress elementType, ElementStress stressComponent,
			int beamExtrusion, int rodExtrusion, int opacity, boolean outlines, PlotContour contour) {
		loadCases_ = loadCases;
		elementType_ = elementType;
		stressComponent_ = stressComponent;
		groups_ = new ArrayList<>();
		beamExtrusion_ = beamExtrusion;
		rodExtrusion_ = rodExtrusion;
		opacity_ = opacity / 100.0;
		outlines_ = outlines;
		contour_ = contour;
	}

	/**
	 * Sets value range.
	 *
	 * @param lowerBound
	 *            Lower bound.
	 * @param upperBound
	 *            Upper bound.
	 */
	public void setValueRange(Double lowerBound, Double upperBound) {
		lowerBound_ = lowerBound;
		upperBound_ = upperBound;
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
	 * Returns load cases.
	 *
	 * @return Load cases.
	 */
	public ArrayList<LoadCaseFactorTableItem> getLoadCases() {
		return loadCases_;
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
	 * Returns opacity.
	 *
	 * @return Opacity.
	 */
	public double getOpacity() {
		return opacity_;
	}

	/**
	 * Returns true if outlines should be drawn.
	 *
	 * @return True if outlines should be drawn.
	 */
	public boolean getOutlines() {
		return outlines_;
	}

	/**
	 * Returns plot contour option.
	 *
	 * @return Plot contour option.
	 */
	public PlotContour getPlotContour() {
		return contour_;
	}

	/**
	 * Returns lower bound value, or null if there is no lower bound.
	 *
	 * @return Lower bound value, or null if there is no lower bound.
	 */
	public Double getLowerBound() {
		return lowerBound_;
	}

	/**
	 * Returns upper bound value, or null if there is no upper bound.
	 *
	 * @return Upper bound value, or null if there is no upper bound.
	 */
	public Double getUpperBound() {
		return upperBound_;
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
