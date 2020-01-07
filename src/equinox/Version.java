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
package equinox;

/**
 * Enumeration for application versions.
 *
 * @author Murat Artim
 * @date 1 Aug 2017
 * @time 12:33:09
 *
 */
public enum Version {

	/** Application version. */
	NEPTUNE("Neptune", 3.4), URANUS("Uranus", 3.5), CASSINI("Cassini", 3.6), JUPITER("Jupiter", 3.7), MARS("Mars", 3.8), EARTH("Earth", 3.9), VENUS("Venus", 4.0), MERCURY("Mercury", 4.1), SUN("Sun", 4.2);

	/** Name of version. */
	private final String name_;

	/** Number of version. */
	private final double number_;

	/**
	 * Creates application version constant.
	 *
	 * @param name
	 *            Name of version.
	 * @param number
	 *            Number of version.
	 */
	Version(String name, double number) {
		name_ = name;
		number_ = number;
	}

	/**
	 * Returns name of version.
	 *
	 * @return Name of version.
	 */
	public String getName() {
		return name_;
	}

	/**
	 * Returns number of version.
	 *
	 * @return Number of version.
	 */
	public double getNumber() {
		return number_;
	}

	@Override
	public String toString() {
		return name_ + " (" + number_ + ")";
	}
}
