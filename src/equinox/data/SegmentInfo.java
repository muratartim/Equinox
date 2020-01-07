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
 * Class for segment info.
 *
 * @author Murat Artim
 * @date Mar 10, 2015
 * @time 9:10:45 AM
 */
public class SegmentInfo {

	/** Stress index. */
	public static final int ONEG = 0, DP = 1, DT = 2, MAX_POS_INC = 3, MIN_POS_INC = 4, MAX_NEG_INC = 5, MIN_NEG_INC = 6;

	/** Stresses. */
	private final double[] stresses_;

	/** Stress info. */
	private final int[] flightID_, peakNum_;

	/** Segment. */
	private final Segment segment_;

	/**
	 * Creates segment info.
	 *
	 * @param segment
	 *            Segment.
	 */
	public SegmentInfo(Segment segment) {
		segment_ = segment;
		stresses_ = new double[7];
		flightID_ = new int[7];
		peakNum_ = new int[7];
	}

	/**
	 * Returns segment.
	 *
	 * @return Segment.
	 */
	public Segment getSegment() {
		return segment_;
	}

	/**
	 * Sets stress at given index.
	 *
	 * @param index
	 *            Index of stress.
	 * @param stress
	 *            Stress value.
	 */
	public void setStress(int index, double stress) {
		stresses_[index] = stress;
	}

	/**
	 * Sets flight ID at given index.
	 *
	 * @param index
	 *            Index of flight ID.
	 * @param flightID
	 *            Flight ID.
	 */
	public void setFlightID(int index, int flightID) {
		flightID_[index] = flightID;
	}

	/**
	 * Sets peak number at given index.
	 *
	 * @param index
	 *            Index of peak number.
	 * @param peakNum
	 *            Peak number.
	 */
	public void setPeakNumber(int index, int peakNum) {
		peakNum_[index] = peakNum;
	}

	/**
	 * Returns stress at given index.
	 *
	 * @param index
	 *            Index of stress.
	 * @return Stress value.
	 */
	public double getStress(int index) {
		return stresses_[index];
	}

	/**
	 * Returns flight ID at given index.
	 *
	 * @param index
	 *            Index of flight ID.
	 * @return Flight ID.
	 */
	public int getFlightID(int index) {
		return flightID_[index];
	}

	/**
	 * Returns peak number at given index.
	 *
	 * @param index
	 *            Index of peak number.
	 * @return Peak number.
	 */
	public int getPeakNumber(int index) {
		return peakNum_[index];
	}

	@Override
	public String toString() {
		return segment_.toString();
	}
}
