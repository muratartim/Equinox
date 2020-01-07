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
import equinox.data.ReferenceLoadCase;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.ui.PlotContour;

/**
 * Class for plot A/C equivalent stresses input.
 *
 * @author Murat Artim
 * @date Sep 10, 2015
 * @time 4:31:58 PM
 */
public class PlotAircraftEquivalentStressesInput {

	/** Interpolation data source option. */
	public static final int CLOSEST_PP = 0, WEIGHTED_NUM = 1, WEIGHTED_DIST = 2;

	/** A/C equivalent stress. */
	private final AircraftFatigueEquivalentStress stress_;

	/** Element groups. */
	private final ArrayList<String> groups_;

	/** 1D element extrusion widths. */
	private final int beamExtrusion_, rodExtrusion_, dataSource_;

	/** Element opacity. */
	private final double opacity_;

	/** True if element outlines should be drawn. */
	private final boolean outlines_, interpolate_;

	/** Element type and stress component. */
	private final ElementTypeForStress elementType_;

	/** Equivalent stress type. */
	private final AircraftEquivalentStressType stressComponent_;

	/** Reference load cases. */
	private final ReferenceLoadCase[] refLCs_;

	/** Reference stress component. */
	private final ElementStress refStressComp_;

	/** Fatigue mission. */
	private final String mission_;

	/** Maximum pilot point distance for weighted distance interpolation. */
	private double weightedDist_;

	/** Maximum number of pilot points for weighted number interpolation. */
	private int weightedNum_;

	/** Value range. */
	private Double lowerBound_ = 0.0, upperBound_ = null;

	/** Plot contour option. */
	private final PlotContour contour_;

	/**
	 * Creates plot A/C equivalent stresses input.
	 *
	 * @param stress
	 *            A/C equivalent stress.
	 * @param elementType
	 *            Element type.
	 * @param stressComponent
	 *            Stress component.
	 * @param mission
	 *            Fatigue mission.
	 * @param interpolate
	 *            True to interpolate elements without stresses.
	 * @param onegLC
	 *            1G load case.
	 * @param dpLC
	 *            Delta-p load case.
	 * @param vgLC
	 *            Vertical gust load case.
	 * @param refStressComp
	 *            Reference stress component.
	 * @param dataSource
	 *            Interpolation data source option.
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
	public PlotAircraftEquivalentStressesInput(AircraftFatigueEquivalentStress stress, ElementTypeForStress elementType,
			AircraftEquivalentStressType stressComponent, String mission, boolean interpolate, ReferenceLoadCase onegLC, ReferenceLoadCase dpLC,
			ReferenceLoadCase vgLC, ElementStress refStressComp, int dataSource, int beamExtrusion, int rodExtrusion, int opacity, boolean outlines,
			PlotContour contour) {
		stress_ = stress;
		elementType_ = elementType;
		stressComponent_ = stressComponent;
		groups_ = new ArrayList<>();
		mission_ = mission;
		interpolate_ = interpolate;
		refLCs_ = new ReferenceLoadCase[] { onegLC, dpLC, vgLC };
		refStressComp_ = refStressComp;
		dataSource_ = dataSource;
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
	 * Sets the maximum pilot point distance for weighted distance interpolation.
	 *
	 * @param distance
	 *            Maximum pilot point distance for weighted distance interpolation.
	 */
	public void setMaximumPilotPointDistance(double distance) {
		weightedDist_ = distance;
	}

	/**
	 * Sets the maximum number of pilot points for weighted number interpolation.
	 *
	 * @param maxPPs
	 *            Maximum number of pilot points for weighted number interpolation.
	 */
	public void setMaximumNumberOfPilotPoints(int maxPPs) {
		weightedNum_ = maxPPs;
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
	 * Returns stress component.
	 *
	 * @return Stress component.
	 */
	public AircraftEquivalentStressType getStressComponent() {
		return stressComponent_;
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
	 * Returns true to interpolate elements without stresses.
	 *
	 * @return True to interpolate elements without stresses.
	 */
	public boolean getInterpolate() {
		return interpolate_;
	}

	/**
	 * Returns the interpolation data source.
	 *
	 * @return Interpolation data source.
	 */
	public int getInterpolationDataSource() {
		return dataSource_;
	}

	/**
	 * Returns the maximum distance for weighted distance interpolation.
	 *
	 * @return Maximum distance for weighted distance interpolation.
	 */
	public double getMaximumPilotPointDistance() {
		return weightedDist_;
	}

	/**
	 * Returns the maximum number of pilot points for weighted number interpolation.
	 *
	 * @return Maximum number of pilot points for weighted number interpolation.
	 */
	public int getMaximumNumberOfPilotPoints() {
		return weightedNum_;
	}

	/**
	 * Returns reference load cases.
	 *
	 * @return Reference load cases.
	 */
	public ReferenceLoadCase[] getReferenceLoadCases() {
		return refLCs_;
	}

	/**
	 * Returns the reference load case factor for given load case ID.
	 *
	 * @param loadCaseID
	 *            Load case ID.
	 * @return Reference load case factor for given load case ID.
	 */
	public double getReferenceLoadCaseFactor(int loadCaseID) {
		for (ReferenceLoadCase lc : refLCs_) {
			if (lc.getID() == loadCaseID)
				return lc.getFactor();
		}
		return 0.0;
	}

	/**
	 * Returns reference stress component.
	 *
	 * @return Reference stress component.
	 */
	public ElementStress getReferenceStressComponent() {
		return refStressComp_;
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
