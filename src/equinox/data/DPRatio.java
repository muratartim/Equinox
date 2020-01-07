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
package equinox.data;

import java.io.Serializable;

/**
 * Class for delta-P ratio.
 *
 * @author Murat Artim
 * @date Oct 21, 2014
 * @time 10:40:49 AM
 */
public class DPRatio implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Event name and ISSY code. */
	private final String event_, issyCode_;

	/** Stress value. */
	private final double ratio_, refPressure_;

	/**
	 * Creates delta-P ratio data.
	 *
	 * @param refPressure
	 *            Reference pressure value.
	 * @param dpStress
	 *            Delta-p stress value.
	 * @param event
	 *            Event name.
	 * @param issyCode
	 *            ISSY code.
	 */
	public DPRatio(double refPressure, double dpStress, String event, String issyCode) {
		refPressure_ = refPressure;
		ratio_ = refPressure_ == 0.0 ? 0.0 : dpStress / refPressure_;
		event_ = event;
		issyCode_ = issyCode;
	}

	/**
	 * Returns the event name.
	 *
	 * @return The event name.
	 */
	public String getEvent() {
		return event_;
	}

	/**
	 * Returns ISSY code.
	 *
	 * @return ISSY code.
	 */
	public String getIssyCode() {
		return issyCode_;
	}

	/**
	 * Returns reference pressure.
	 *
	 * @return Reference pressure.
	 */
	public double getReferencePressure() {
		return refPressure_;
	}

	/**
	 * Returns the stress value for the given delta-p pressure.
	 *
	 * @param deltaP
	 *            Delta-p pressure.
	 * @return The stress value.
	 */
	public double getStress(double deltaP) {
		return ratio_ * deltaP;
	}
}
