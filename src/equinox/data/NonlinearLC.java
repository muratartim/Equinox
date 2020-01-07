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
 * Class for nonlinear load case.
 *
 * @author Murat Artim
 * @date Apr 8, 2014
 * @time 9:55:14 AM
 */
public class NonlinearLC {

	/** Factor value. */
	private double factorVal_;

	/** 1g code and direction number. */
	private final String directionNum_, onegCode_, factorNum_;

	/**
	 * Creates nonlinear load case.
	 *
	 * @param onegCode
	 *            1g code.
	 * @param directionNum
	 *            Direction number.
	 * @param factorVal
	 *            Factor value.
	 * @param factorNum
	 *            Factor number.
	 */
	public NonlinearLC(String onegCode, String directionNum, double factorVal, String factorNum) {
		onegCode_ = onegCode;
		directionNum_ = directionNum;
		factorVal_ = factorVal;
		factorNum_ = factorNum;
	}

	/**
	 * Sets factor value.
	 *
	 * @param factorVal
	 *            Factor value to set.
	 */
	public void setFactorValue(double factorVal) {
		factorVal_ = factorVal;
	}

	/**
	 * Returns the 1g code.
	 *
	 * @return The 1g code.
	 */
	public String get1gCode() {
		return onegCode_;
	}

	/**
	 * Returns the direction number.
	 *
	 * @return The direction number.
	 */
	public String getDirectionNumber() {
		return directionNum_;
	}

	/**
	 * Returns the factor value.
	 *
	 * @return The factor value.
	 */
	public double getFactorValue() {
		return factorVal_;
	}

	/**
	 * Returns the factor number.
	 *
	 * @return The factor number.
	 */
	public String getFactorNumber() {
		return factorNum_;
	}
}
