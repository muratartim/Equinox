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
 * Class for delta-t 2 points interpolator.
 *
 * @author Murat Artim
 * @date Oct 20, 2014
 * @time 3:53:06 PM
 */
public class DT2PointsInterpolator extends DTInterpolator {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Event names and ISSY codes. */
	private String eventInf_, issyCodeInf_, eventSup_, issyCodeSup_;

	/** Delta-t stresses and reference temperatures. */
	private double stressInf_, refTempInf_, stressSup_, refTempSup_;

	/**
	 * Sets superior parameters to interpolator.
	 *
	 * @param eventSup
	 *            Superior event name.
	 * @param issyCodeSup
	 *            Superior ISSY code.
	 * @param stressSup
	 *            Superior delta-t stress.
	 * @param refTempSup
	 *            Superior reference temperature.
	 */
	public void setSupParameters(String eventSup, String issyCodeSup, double stressSup, double refTempSup) {
		eventSup_ = eventSup;
		issyCodeSup_ = issyCodeSup;
		stressSup_ = stressSup;
		refTempSup_ = refTempSup;
	}

	/**
	 * Sets inferior parameters to interpolator.
	 *
	 * @param eventInf
	 *            Inferior event name.
	 * @param issyCodeInf
	 *            Inferior ISSY code.
	 * @param stressInf
	 *            Inferior delta-t stress.
	 * @param refTempInf
	 *            Inferior reference temperature.
	 */
	public void setInfParameters(String eventInf, String issyCodeInf, double stressInf, double refTempInf) {
		eventInf_ = eventInf;
		issyCodeInf_ = issyCodeInf;
		stressInf_ = stressInf;
		refTempInf_ = refTempInf;
	}

	@Override
	public double getStress(double deltaTemp) {
		double param = refTempInf_ - refTempSup_ == 0 ? 0.0 : (stressInf_ - stressSup_) / (refTempInf_ - refTempSup_);
		return param * (deltaTemp - refTempSup_) + stressSup_;
	}

	/**
	 * Returns the event name.
	 *
	 * @return The event name.
	 */
	public String getEventInf() {
		return eventInf_;
	}

	/**
	 * Returns ISSY code.
	 *
	 * @return ISSY code.
	 */
	public String getIssyCodeInf() {
		return issyCodeInf_;
	}

	/**
	 * Returns the event name.
	 *
	 * @return The event name.
	 */
	public String getEventSup() {
		return eventSup_;
	}

	/**
	 * Returns ISSY code.
	 *
	 * @return ISSY code.
	 */
	public String getIssyCodeSup() {
		return issyCodeSup_;
	}

	/**
	 * Returns superior reference temperature.
	 *
	 * @return Superior reference temperature.
	 */
	public double getReferenceTemperatureSup() {
		return refTempSup_;
	}

	/**
	 * Returns inferior reference temperature.
	 *
	 * @return Inferior reference temperature.
	 */
	public double getReferenceTemperatureInf() {
		return refTempInf_;
	}
}
