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
 * Enumeration for stress component.
 *
 * @author Murat Artim
 * @date Apr 8, 2014
 * @time 3:18:14 PM
 */
public enum StressComponent {

	/** Stress component. */
	NORMAL_X("Normal X"), NORMAL_Y("Normal Y"), SHEAR_XY("Shear XY"), ROTATED("Rotated");

	/** Name of component. */
	private final String name_;

	/**
	 * Creates stress component constant.
	 *
	 * @param name
	 *            Name of component.
	 */
	StressComponent(String name) {
		name_ = name;
	}

	@Override
	public String toString() {
		return name_;
	}

	/**
	 * Returns the stress component with the given name.
	 * 
	 * @param name
	 *            Name of demanded stress component.
	 * @return The stress component with the given name.
	 */
	public static StressComponent getStressComponent(String name) {
		for (StressComponent comp : values()) {
			if (comp.name_.equals(name))
				return comp;
		}
		return null;
	}
}
