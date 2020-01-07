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
 * Enumeration for delta-T interpolation.
 *
 * @author Murat Artim
 * @date Apr 8, 2014
 * @time 3:18:14 PM
 */
public enum DTInterpolation {

	/** Interpolation. */
	NONE("None"), ONE_POINT("1 point"), TWO_POINTS("2 points");

	/** Name of component. */
	private final String name_;

	/**
	 * Creates delta-T interpolation constant.
	 *
	 * @param name
	 *            Name of interpolation.
	 */
	DTInterpolation(String name) {
		name_ = name;
	}

	@Override
	public String toString() {
		return name_;
	}

	/**
	 * Returns the interpolation for the given name.
	 * 
	 * @param name
	 *            Name of demanded interpolation.
	 * @return The interpolation for the given name.
	 */
	public static DTInterpolation getDTInterpolation(String name) {
		for (DTInterpolation dtInt : values()) {
			if (dtInt.name_.equals(name))
				return dtInt;
		}
		return null;
	}
}
