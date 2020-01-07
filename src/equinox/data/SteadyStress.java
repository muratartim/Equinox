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
 * Class for steady stresses.
 *
 * @author Murat Artim
 * @date May 27, 2016
 * @time 9:49:57 AM
 */
public class SteadyStress {

	/** Steady stresses. */
	private final double onegStress_, dpStress_, dtStress_;

	/** Typical flight ID and ANA peak number. */
	private final int flightID_, peakNum_;

	/**
	 * Creates steady stress.
	 *
	 * @param oneg
	 *            1G stress.
	 * @param dp
	 *            Delta-p stress.
	 * @param dt
	 *            Delta-t stress.
	 * @param flightID
	 *            Typical flight ID.
	 * @param peakNum
	 *            ANA peak number.
	 */
	public SteadyStress(double oneg, double dp, double dt, int flightID, int peakNum) {
		onegStress_ = oneg;
		dpStress_ = dp;
		dtStress_ = dt;
		flightID_ = flightID;
		peakNum_ = peakNum;
	}

	/**
	 * Returns 1G stress.
	 *
	 * @return 1G stress.
	 */
	public double getOnegStress() {
		return onegStress_;
	}

	/**
	 * Returns delta-p stress.
	 *
	 * @return Delta-p stress.
	 */
	public double getDPStress() {
		return dpStress_;
	}

	/**
	 * Returns delta-t stress.
	 *
	 * @return Delta-t stress.
	 */
	public double getDTStress() {
		return dtStress_;
	}

	/**
	 * Returns typical flight ID.
	 *
	 * @return Typical flight ID.
	 */
	public int getFlightID() {
		return flightID_;
	}

	/**
	 * Returns ANA peak number.
	 *
	 * @return ANA peak number.
	 */
	public int getPeakNum() {
		return peakNum_;
	}
}
