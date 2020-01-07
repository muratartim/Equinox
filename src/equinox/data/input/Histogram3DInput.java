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

import equinox.data.fileType.SpectrumItem;
import equinox.data.input.HistogramInput.HistogramDataType;

/**
 * Class for histogram 3D input.
 *
 * @author Murat Artim
 * @date Jul 4, 2014
 * @time 2:15:39 PM
 */
public class Histogram3DInput {

	/** Histogram data types. */
	private HistogramDataType dataTypeX_ = HistogramDataType.MEAN_STRESS, dataTypeY_ = HistogramDataType.STRESS_AMPLITUDE;

	/** Label display. */
	private boolean showlabelsX_ = false, showlabelsY_ = false, showlabelsZ_ = false;

	/** Resolution. */
	private int resolution_ = 60;

	/** Equivalent stress. */
	private final SpectrumItem equivalentStress_;

	/**
	 * Creates histogram input.
	 *
	 * @param equivalentStress
	 *            Equivalent stress.
	 */
	public Histogram3DInput(SpectrumItem equivalentStress) {
		equivalentStress_ = equivalentStress;
	}

	/**
	 * Sets label display option.
	 *
	 * @param x
	 *            True to show X labels.
	 * @param y
	 *            True to show Y labels.
	 * @param z
	 *            True to show Z labels.
	 */
	public void setLabelDisplay(boolean x, boolean y, boolean z) {
		showlabelsX_ = x;
		showlabelsY_ = y;
		showlabelsZ_ = z;
	}

	/**
	 * Sets histogram data type.
	 *
	 * @param x
	 *            Data type for X axis to set.
	 * @param y
	 *            Data type for Y axis to set.
	 */
	public void setDataType(HistogramDataType x, HistogramDataType y) {
		dataTypeX_ = x;
		dataTypeY_ = y;
	}

	/**
	 * Sets resolution.
	 * 
	 * @param resolution
	 *            Resolution to set.
	 */
	public void setResolution(int resolution) {
		resolution_ = resolution;
	}

	/**
	 * Returns equivalent stress.
	 *
	 * @return Equivalent stress.
	 */
	public SpectrumItem getEquivalentStress() {
		return equivalentStress_;
	}

	/**
	 * Returns X axis data type.
	 *
	 * @return X axis data type.
	 */
	public HistogramDataType getDataTypeX() {
		return dataTypeX_;
	}

	/**
	 * Returns Y axis data type.
	 *
	 * @return Y axis data type.
	 */
	public HistogramDataType getDataTypeY() {
		return dataTypeY_;
	}

	/**
	 * Returns true if X axis data labels are shown.
	 *
	 * @return True if X axis data labels are shown.
	 */
	public boolean getLabelDisplayX() {
		return showlabelsX_;
	}

	/**
	 * Returns true if Y axis data labels are shown.
	 *
	 * @return True if Y axis data labels are shown.
	 */
	public boolean getLabelDisplayY() {
		return showlabelsY_;
	}

	/**
	 * Returns true if Z axis data labels are shown.
	 *
	 * @return True if Z axis data labels are shown.
	 */
	public boolean getLabelDisplayZ() {
		return showlabelsZ_;
	}

	/**
	 * Returns resolution.
	 * 
	 * @return Resolution.
	 */
	public int getResolution() {
		return resolution_;
	}
}
