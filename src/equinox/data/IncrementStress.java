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
 * Class for incremental stress.
 *
 * @author Murat Artim
 * @date May 26, 2016
 * @time 10:24:47 AM
 */
public class IncrementStress {

	/** Total incremental stresses. */
	private final Double[] stresses_;

	/** Typical flight IDs and ANA peak numbers. */
	private final Integer[] flightIDs_, peakNumbers_;

	/**
	 * Creates incremental stress.
	 */
	public IncrementStress() {
		stresses_ = new Double[16];
		flightIDs_ = new Integer[16];
		peakNumbers_ = new Integer[16];
	}

	/**
	 * Sets increment info.
	 *
	 * @param factorNumber
	 *            Factor number.
	 * @param stress
	 *            Total incremental stress.
	 * @param flightID
	 *            Typical flight ID.
	 * @param peakNumber
	 *            Typical flight peak number.
	 */
	public void setInfo(int factorNumber, double stress, int flightID, int peakNumber) {

		// set factor index
		int index = stress >= 0 ? factorNumber : factorNumber + 8;

		// no stress exists
		if (stresses_[index] == null) {
			stresses_[index] = stress;
			flightIDs_[index] = flightID;
			peakNumbers_[index] = peakNumber;
		}

		// there is already a stress value
		else {

			// positive increment
			if (stress >= 0 && stress > stresses_[index]) {
				stresses_[index] = stress;
				flightIDs_[index] = flightID;
				peakNumbers_[index] = peakNumber;
			}

			// negative increment
			else if (stress < 0 && stress < stresses_[index]) {
				stresses_[index] = stress;
				flightIDs_[index] = flightID;
				peakNumbers_[index] = peakNumber;
			}
		}
	}

	/**
	 * Returns total incremental stresses.
	 *
	 * @return Total incremental stresses.
	 */
	public Double[] getStresses() {
		return stresses_;
	}

	/**
	 * Returns typical flight IDs.
	 *
	 * @return Typical flight IDs.
	 */
	public Integer[] getFlightIDs() {
		return flightIDs_;
	}

	/**
	 * Returns typical flight peak numbers.
	 *
	 * @return Typical flight peak numbers.
	 */
	public Integer[] getPeakNumbers() {
		return peakNumbers_;
	}
}
