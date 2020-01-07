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

import equinox.dataServer.remote.data.Material;

/**
 * Class for equivalent stress analysis input.
 *
 * @author Murat Artim
 * @date Jan 30, 2015
 * @time 1:49:04 PM
 */
public class EquivalentStressInput implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Stress modification methods. */
	public static final String MULTIPLY = "Multiply", ADD = "Add", SET = "Set";

	/** True if omission should be applied to stress sequence. */
	private boolean removeNegativeStresses_, applyOmission_;

	/** Omission level. */
	private double omissionLevel_;

	/** Material. */
	private Material material_;

	/** Stress modification method. */
	private String stressModificationMethod_ = MULTIPLY;

	/** Stress modifier value. */
	private double stressModifier_ = 1.0;

	/**
	 * Creates equivalent stress analysis input.
	 */
	public EquivalentStressInput() {
	}

	/**
	 * Creates equivalent stress analysis input.
	 *
	 * @param removeNegativeStresses
	 *            True if negative stresses should be removed.
	 * @param applyOmission
	 *            True if omission should be applied to stress sequence.
	 * @param omissionLevel
	 *            Omission level.
	 * @param material
	 *            Material.
	 */
	public EquivalentStressInput(boolean removeNegativeStresses, boolean applyOmission, double omissionLevel, Material material) {
		removeNegativeStresses_ = removeNegativeStresses;
		applyOmission_ = applyOmission;
		omissionLevel_ = omissionLevel;
		material_ = material;
	}

	/**
	 * Sets whether omission should be applied to stress sequence.
	 *
	 * @param applyOmission
	 *            True if omission should be applied to stress sequence.
	 */
	public void setApplyOmission(boolean applyOmission) {
		applyOmission_ = applyOmission;
	}

	/**
	 * Sets whether negative stresses should be removed.
	 *
	 * @param removeNegativeStresses
	 *            True if negative stresses should be removed.
	 */
	public void setRemoveNegativeStresses(boolean removeNegativeStresses) {
		removeNegativeStresses_ = removeNegativeStresses;
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
	 * Sets material.
	 *
	 * @param material
	 *            Material to set.
	 */
	public void setMaterial(Material material) {
		material_ = material;
	}

	/**
	 * Sets stress modifier. Note that, this is only used for external equivalent stress analysis.
	 *
	 * @param stressModifier
	 *            Stress modification value.
	 * @param stressModificationMethod
	 *            Stress modification method.
	 */
	public void setStressModifier(double stressModifier, String stressModificationMethod) {
		stressModificationMethod_ = stressModificationMethod;
		stressModifier_ = stressModifier;
	}

	/**
	 * Returns true if omission should be applied to stress sequence.
	 *
	 * @return True if omission should be applied to stress sequence.
	 */
	public boolean applyOmission() {
		return applyOmission_;
	}

	/**
	 * Returns true if negative stresses should be removed.
	 *
	 * @return True if negative stresses should be removed.
	 */
	public boolean removeNegativeStresses() {
		return removeNegativeStresses_;
	}

	/**
	 * Returns omission level.
	 *
	 * @return Omission level.
	 */
	public double getOmissionLevel() {
		return omissionLevel_;
	}

	/**
	 * Returns material.
	 *
	 * @return Material.
	 */
	public Material getMaterial() {
		return material_;
	}

	/**
	 * Returns stress modification value.
	 *
	 * @return Stress modification value.
	 */
	public double getStressModificationValue() {
		return stressModifier_;
	}

	/**
	 * Returns stress modification method.
	 *
	 * @return Stress modification method.
	 */
	public String getStressModificationMethod() {
		return stressModificationMethod_;
	}
}
