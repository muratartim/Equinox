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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import equinox.data.DTInterpolation;
import equinox.data.LoadcaseFactor;
import equinox.data.SegmentFactor;
import equinox.data.StressComponent;

/**
 * Class for fast equivalent stress analysis input.
 *
 * @author Murat Artim
 * @date Jun 14, 2016
 * @time 9:27:05 PM
 */
public class FastEquivalentStressInput implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Stress modification values. */
	private final double[] modificationValues_ = { 1.0, 1.0, 1.0, 1.0 };

	/** Stress modification methods. */
	private final String[] modificationMethods_ = { GenerateStressSequenceInput.MULTIPLY, GenerateStressSequenceInput.MULTIPLY, GenerateStressSequenceInput.MULTIPLY, GenerateStressSequenceInput.MULTIPLY };

	/** Reference delta-p. */
	private Double refDP_ = null, refDTInf_ = null, refDTSup_ = null;

	/** Delta-P load case. */
	private String dpLoadcase_ = null, dtLoadcaseInf_ = null, dtLoadcaseSup_ = null;

	/** Delta-T interpolation. */
	private DTInterpolation dtInterpolation_ = DTInterpolation.NONE;

	/** Stress component. */
	private StressComponent component_ = StressComponent.NORMAL_X;

	/** Stress rotation angle. */
	private double angle_ = 0.0;

	/** List containing the loadcase factors. */
	private ArrayList<LoadcaseFactor> loadcaseFactors_ = null;

	/** List containing the segment factors. */
	private ArrayList<SegmentFactor> segmentFactors_ = null;

	/** True if the negative stresses should be removed. */
	private boolean removeNegativeStresses_ = false, applyOmission_ = true;

	/** Omission level. */
	private double omissionLevel_;

	/**
	 * Sets whether the negative stresses should removed.
	 *
	 * @param removeNegativeStresses
	 *            True if the negative stresses should be removed.
	 */
	public void setRemoveNegativeStresses(boolean removeNegativeStresses) {
		removeNegativeStresses_ = removeNegativeStresses;
	}

	/**
	 * Sets apply omission parameter.
	 *
	 * @param applyOmission
	 *            True to apply omission.
	 */
	public void setApplyOmission(boolean applyOmission) {
		applyOmission_ = applyOmission;
	}

	/**
	 * Sets omission level.
	 *
	 * @param omissionLevel
	 *            Omission level.
	 */
	public void setOmissionLevel(double omissionLevel) {
		omissionLevel_ = omissionLevel;
	}

	/**
	 * Sets stress modifiers.
	 *
	 * @param index
	 *            Index of the modifier.
	 * @param value
	 *            Value of the modifier.
	 * @param method
	 *            Method of the modification.
	 */
	public void setStressModifier(int index, double value, String method) {
		modificationValues_[index] = value;
		modificationMethods_[index] = method;
	}

	/**
	 * Sets loadcase factors.
	 *
	 * @param loadcaseFactors
	 *            Loadcase factors.
	 */
	public void setLoadcaseFactors(Collection<LoadcaseFactor> loadcaseFactors) {
		if (loadcaseFactors == null)
			return;
		loadcaseFactors_ = new ArrayList<>();
		for (LoadcaseFactor factor : loadcaseFactors)
			loadcaseFactors_.add(factor);
	}

	/**
	 * Sets segment factors.
	 *
	 * @param segmentFactors
	 *            Segment factors.
	 */
	public void setSegmentFactors(Collection<SegmentFactor> segmentFactors) {
		if (segmentFactors == null)
			return;
		segmentFactors_ = new ArrayList<>();
		for (SegmentFactor factor : segmentFactors)
			segmentFactors_.add(factor);
	}

	/**
	 * Sets stress component.
	 *
	 * @param component
	 *            Stress component.
	 */
	public void setStressComponent(StressComponent component) {
		component_ = component;
	}

	/**
	 * Sets rotation angle. Note that the angle is converted to radians.
	 *
	 * @param angle
	 *            Rotation angle in degrees.
	 */
	public void setRotationAngle(double angle) {
		angle_ = Math.toRadians(angle);
	}

	/**
	 * Sets reference delta-p.
	 *
	 * @param refDP
	 *            Reference delta-p (null can also be given for automatic determination).
	 */
	public void setReferenceDP(Double refDP) {
		refDP_ = refDP;
	}

	/**
	 * Sets delta-p load case.
	 *
	 * @param dpLoadcase
	 *            Delta-P load case (null can also be given for automatic determination).
	 */
	public void setDPLoadcase(String dpLoadcase) {
		dpLoadcase_ = dpLoadcase;
	}

	/**
	 * Sets delta-t interpolation.
	 *
	 * @param dtInterpolation
	 *            Delta-t interpolation.
	 */
	public void setDTInterpolation(DTInterpolation dtInterpolation) {
		dtInterpolation_ = dtInterpolation;
	}

	/**
	 * Sets reference delta-t inferior.
	 *
	 * @param refDTInf
	 *            Reference delta-t inferior.
	 */
	public void setReferenceDTInf(Double refDTInf) {
		refDTInf_ = refDTInf;
	}

	/**
	 * Sets inferior delta-t load case.
	 *
	 * @param dtLoadcaseInf
	 *            Inferior delta-t load case.
	 */
	public void setDTLoadcaseInf(String dtLoadcaseInf) {
		dtLoadcaseInf_ = dtLoadcaseInf;
	}

	/**
	 * Sets reference delta-t superior.
	 *
	 * @param refDTSup
	 *            Reference delta-t superior.
	 */
	public void setReferenceDTSup(Double refDTSup) {
		refDTSup_ = refDTSup;
	}

	/**
	 * Sets superior delta-t load case.
	 *
	 * @param dtLoadcaseSup
	 *            Superior delta-t load case.
	 */
	public void setDTLoadcaseSup(String dtLoadcaseSup) {
		dtLoadcaseSup_ = dtLoadcaseSup;
	}

	/**
	 * Returns stress component.
	 *
	 * @return Stress component.
	 */
	public StressComponent getStressComponent() {
		return component_;
	}

	/**
	 * Returns rotation angle in radians.
	 *
	 * @return Rotation angle in radians.
	 */
	public double getRotationAngle() {
		return angle_;
	}

	/**
	 * Returns the stress modification value.
	 *
	 * @param index
	 *            The index of the stress modifier.
	 * @return Stress modification value.
	 */
	public double getStressModificationValue(int index) {
		return modificationValues_[index];
	}

	/**
	 * Returns the stress modification method.
	 *
	 * @param index
	 *            The index of the stress modifier.
	 * @return Stress modification method.
	 */
	public String getStressModificationMethod(int index) {
		return modificationMethods_[index];
	}

	/**
	 * Returns the loadcase factors or null if no factors defined.
	 *
	 * @return The loadcase factors or null if no factors defined.
	 */
	public ArrayList<LoadcaseFactor> getLoadcaseFactors() {
		return loadcaseFactors_;
	}

	/**
	 * Returns the segment factors or null if no factors defined.
	 *
	 * @return The segment factors or null if no factors defined.
	 */
	public ArrayList<SegmentFactor> getSegmentFactors() {
		return segmentFactors_;
	}

	/**
	 * Returns the reference delta-p, or null if the reference value will be determined automatically.
	 *
	 * @return The reference delta-p.
	 */
	public Double getReferenceDP() {
		return refDP_;
	}

	/**
	 * Returns delta-p load case, or null if the delta-p load case will be determined automatically.
	 *
	 * @return Delta-p load case.
	 */
	public String getDPLoadcase() {
		return dpLoadcase_;
	}

	/**
	 * Returns delta-t interpolation.
	 *
	 * @return Delta-t interpolation.
	 */
	public DTInterpolation getDTInterpolation() {
		return dtInterpolation_;
	}

	/**
	 * Returns the reference inferior delta-t, or null if no reference value specified.
	 *
	 * @return The reference inferior delta-t.
	 */
	public Double getReferenceDTInf() {
		return refDTInf_;
	}

	/**
	 * Returns inferior delta-t load case, or null if no inferior delta-t load case specified.
	 *
	 * @return Inferior delta-t load case.
	 */
	public String getDTLoadcaseInf() {
		return dtLoadcaseInf_;
	}

	/**
	 * Returns the reference superior delta-t, or null if no reference value specified.
	 *
	 * @return The reference superior delta-t.
	 */
	public Double getReferenceDTSup() {
		return refDTSup_;
	}

	/**
	 * Returns superior delta-t load case, or null if no superior delta-t load case specified.
	 *
	 * @return Superior delta-t load case.
	 */
	public String getDTLoadcaseSup() {
		return dtLoadcaseSup_;
	}

	/**
	 * Returns true if the negative stresses should be removed.
	 *
	 * @return True if the negative stresses should be removed.
	 */
	public boolean isRemoveNegativeStresses() {
		return removeNegativeStresses_;
	}

	/**
	 * Returns true omission should be applied.
	 *
	 * @return True omission should be applied.
	 */
	public boolean isApplyOmission() {
		return applyOmission_;
	}

	/**
	 * Returns omission level.
	 *
	 * @return Omission level.
	 */
	public double getOmissionLevel() {
		return omissionLevel_;
	}
}
