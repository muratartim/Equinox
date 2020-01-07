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
 * Class for delta-t 1 point interpolator.
 *
 * @author Murat Artim
 * @date Oct 20, 2014
 * @time 3:47:29 PM
 */
public class DT1PointInterpolator extends DTInterpolator {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Event name and ISSY code. */
	private final String event_, issyCode_;

	/** Delta-t ratio. */
	private final double ratio_, refTemperature_;

	/**
	 * Sets parameters to interpolator.
	 *
	 * @param event
	 *            Event name.
	 * @param issyCode
	 *            ISSY code.
	 * @param stress
	 *            Delta-t stress.
	 * @param refTemperature
	 *            Reference temperature.
	 */
	public DT1PointInterpolator(String event, String issyCode, double stress, double refTemperature) {
		event_ = event;
		issyCode_ = issyCode;
		refTemperature_ = refTemperature;
		ratio_ = refTemperature == 0.0 ? 0.0 : stress / refTemperature;
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
	 * Returns reference temperature.
	 *
	 * @return Reference temperature.
	 */
	public double getReferenceTemperature() {
		return refTemperature_;
	}

	@Override
	public double getStress(double deltaTemp) {
		return ratio_ * deltaTemp;
	}
}
