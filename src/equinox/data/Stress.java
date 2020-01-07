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
 * Class for stress data.
 *
 * @author Murat Artim
 * @date Apr 17, 2014
 * @time 11:55:32 AM
 */
public class Stress {

	/** Event name and ISSY code. */
	private final String event_, issyCode_;

	/** Stress value. */
	private final double stress_;

	/** Segment. */
	private final Segment segment_;

	/**
	 * Creates stress data.
	 *
	 * @param stress
	 *            Stress value.
	 * @param event
	 *            Event name.
	 * @param issyCode
	 *            ISSY code.
	 * @param segment
	 *            Segment.
	 */
	public Stress(double stress, String event, String issyCode, Segment segment) {
		stress_ = stress;
		event_ = event;
		issyCode_ = issyCode;
		segment_ = segment;
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
	public double getStress() {
		return stress_;
	}

	/**
	 * Returns segment.
	 *
	 * @return Segment.
	 */
	public Segment getSegment() {
		return segment_;
	}
}
