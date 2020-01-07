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
 * Class for GAG peak.
 *
 * @author Murat Artim
 * @date Jun 1, 2015
 * @time 2:50:18 PM
 */
public class GAGPeak {

	/** Total stress. */
	private double stress_;

	/** Events and load cases. */
	private String events_, issyCodes_;

	/** Segment. */
	private Segment segment_;

	/**
	 * Sets stress.
	 *
	 * @param stress
	 *            Stress.
	 */
	public void setStress(double stress) {
		stress_ = stress;
	}

	/**
	 * Sets events.
	 *
	 * @param events
	 *            Events.
	 */
	public void setEvents(String events) {
		events_ = events;
	}

	/**
	 * Sets load cases.
	 *
	 * @param issyCodes
	 *            Load cases.
	 */
	public void setIssyCodes(String issyCodes) {
		issyCodes_ = issyCodes;
	}

	/**
	 * Sets segment.
	 *
	 * @param segment
	 *            Segment.
	 */
	public void setSegment(Segment segment) {
		segment_ = segment;
	}

	/**
	 * Returns stress.
	 *
	 * @return Stress.
	 */
	public double getStress() {
		return stress_;
	}

	/**
	 * Returns events.
	 *
	 * @return Events.
	 */
	public String getEvents() {
		return events_;
	}

	/**
	 * Returns load cases.
	 *
	 * @return Load cases.
	 */
	public String getIssyCodes() {
		return issyCodes_;
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
