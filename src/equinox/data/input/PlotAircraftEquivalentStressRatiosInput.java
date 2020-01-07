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
import equinox.data.ui.PlotContour;

/**
 * Class for plot A/C equivalent stress ratios input.
 *
 * @author Murat Artim
 * @date Oct 4, 2015
 * @time 7:55:17 PM
 */
public class PlotAircraftEquivalentStressRatiosInput {

	/** A/C equivalent stress. */
	private final AircraftFatigueEquivalentStress stress_;

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

	/** Ratio type. */
	private final EquivalentStressRatioType ratioType_;

	/** Fatigue mission. */
	private final String mission_, basisMission_;

	/** Value range. */
	private Double lowerBound_ = 0.0, upperBound_ = null;

	/** Plot contour option. */
	private final PlotContour contour_;

	/**
	 * Creates plot A/C equivalent stress ratios input.
	 *
	 * @param stress
	 *            A/C equivalent stress.
	 * @param elementType
	 *            Element type.
	 * @param ratioType
	 *            Equivalent stress ratio type.
	 * @param mission
	 *            Fatigue mission.
	 * @param basisMission
	 *            Basis fatigue mission.
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
	public PlotAircraftEquivalentStressRatiosInput(AircraftFatigueEquivalentStress stress, ElementTypeForStress elementType,
			EquivalentStressRatioType ratioType, String mission, String basisMission, int beamExtrusion, int rodExtrusion, int opacity,
			boolean outlines, PlotContour contour) {
		stress_ = stress;
		elementType_ = elementType;
		ratioType_ = ratioType;
		groups_ = new ArrayList<>();
		mission_ = mission;
		basisMission_ = basisMission;
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
	 * Returns A/C equivalent stress.
	 *
	 * @return A/C equivalent stress.
	 */
	public AircraftFatigueEquivalentStress getEquivalentStress() {
		return stress_;
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
	 * Returns equivalent stress ratio type.
	 *
	 * @return Equivalent stress ratio type.
	 */
	public EquivalentStressRatioType getRatioType() {
		return ratioType_;
	}

	/**
	 * Returns fatigue mission.
	 *
	 * @return Fatigue mission.
	 */
	public String getMission() {
		return mission_;
	}

	/**
	 * Returns basis fatigue mission.
	 *
	 * @return Basis fatigue mission.
	 */
	public String getBasisMission() {
		return basisMission_;
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
