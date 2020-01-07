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
 * Class for damage angle analysis 1g stresses.
 *
 * @author Murat Artim
 * @date Feb 28, 2015
 * @time 11:26:25 AM
 */
public class OneGStresses {

	/** Segment. */
	private final Segment segment_;

	/** 1g stresses for incremental angles. */
	private final double[] stresses_;

	/**
	 * Creates damage angle analysis 1g stresses.
	 *
	 * @param segment
	 *            Segment.
	 * @param stresses
	 *            1g stresses for incremental angles.
	 */
	public OneGStresses(Segment segment, double[] stresses) {
		segment_ = segment;
		stresses_ = stresses;
	}

	/**
	 * Returns the segment.
	 *
	 * @return The segment.
	 */
	public Segment getSegment() {
		return segment_;
	}

	/**
	 * Returns the 1g stresses for incremental angles.
	 *
	 * @return 1g stresses for incremental angles.
	 */
	public double[] getStresses() {
		return stresses_;
	}
}
