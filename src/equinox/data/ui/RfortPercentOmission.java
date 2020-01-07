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
package equinox.data.ui;

/**
 * Class for RFORT percentage omission.
 *
 * @author Murat Artim
 * @date Mar 2, 2016
 * @time 4:56:16 PM
 */
public class RfortPercentOmission extends RfortOmission {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Omission level. This is the percentage value of maximum stress amplitude. */
	private final int percentOmission_;

	/**
	 * Creates RFORT percentage omission.
	 *
	 * @param percentOmission
	 *            Percentage omission. This is the percentage value of maximum stress amplitude.
	 */
	public RfortPercentOmission(int percentOmission) {
		percentOmission_ = percentOmission;
	}

	@Override
	public String getOmissionType() {
		return PERCENT;
	}

	/**
	 * Returns the percentage omission.
	 *
	 * @return The percentage omission.
	 */
	public int getPercentOmission() {
		return percentOmission_;
	}

	@Override
	public String toString() {
		return percentOmission_ + "% on max. stress amplitudes";
	}
}
