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
 * Class for 1g stress used for fast equivalent stress analysis.
 *
 * @author Murat Artim
 * @date Jun 14, 2016
 * @time 11:10:21 PM
 */
public class OnegStress {

	/** Segment. */
	private final Segment segment_;

	/** 1g stress. */
	private final double stress_;

	/**
	 * Creates 1g stress.
	 *
	 * @param segment
	 *            Segment.
	 * @param stress
	 *            1g stress.
	 */
	public OnegStress(Segment segment, double stress) {
		segment_ = segment;
		stress_ = stress;
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
	 * Returns the 1g stress.
	 *
	 * @return 1g stress.
	 */
	public double getStress() {
		return stress_;
	}
}
