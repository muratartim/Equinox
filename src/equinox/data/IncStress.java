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

/**
 * Class for increment stresses.
 *
 * @author Murat Artim
 * @date Jun 1, 2015
 * @time 10:00:54 AM
 */
public class IncStress {

	/** Event name and ISSY code. */
	private String event_ = null, issyCode_ = null;

	/** Stress value. */
	private double[] stress_ = null;

	/**
	 * Sets stresses.
	 *
	 * @param stress
	 *            Stresses.
	 */
	public void setStress(double[] stress) {
		stress_ = stress;
	}

	/**
	 * Sets event.
	 *
	 * @param event
	 *            Event to set.
	 */
	public void addEvent(String event) {
		event_ = event_ == null ? event : event_ + ", " + event;
	}

	/**
	 * Sets load case.
	 *
	 * @param issyCode
	 *            Load case.
	 */
	public void addIssyCode(String issyCode) {
		issyCode_ = issyCode_ == null ? issyCode : issyCode_ + ", " + issyCode;
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
	 * Returns the stress value.
	 *
	 * @return The stress value.
	 */
	public double[] getStress() {
		return stress_;
	}
}
