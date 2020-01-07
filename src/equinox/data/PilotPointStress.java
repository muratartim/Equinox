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
 * Class for pilot point stress data used for interpolation.
 *
 * @author Murat Artim
 * @date Sep 17, 2015
 * @time 11:54:44 AM
 */
public class PilotPointStress implements Comparable<PilotPointStress> {

	/** Pilot point distance and stress. */
	private final Double distance_, stress_;

	/**
	 * Creates pilot point stress.
	 *
	 * @param distance
	 *            Pilot point distance to interpolated element.
	 * @param stress
	 *            Pilot point stress.
	 */
	public PilotPointStress(Double distance, Double stress) {
		distance_ = distance;
		stress_ = stress;
	}

	/**
	 * Returns pilot point distance to interpolated element.
	 *
	 * @return Pilot point distance to interpolated element.
	 */
	public Double getDistance() {
		return distance_;
	}

	/**
	 * Returns pilot point stress.
	 *
	 * @return Pilot point stress.
	 */
	public Double getStress() {
		return stress_;
	}

	@Override
	public int compareTo(PilotPointStress o) {
		return distance_.compareTo(o.getDistance());
	}
}
