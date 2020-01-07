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
 * Enumeration for element type for stresses.
 *
 * @author Murat Artim
 * @date Aug 25, 2015
 * @time 1:45:56 PM
 */
public enum ElementTypeForStress {

	/** Element type for stress. */
	SKIN("QUAD & TRIA elements"), BEAM("BEAM elements"), ROD("ROD elements");

	/** Name of element stress. */
	private final String name_;

	/**
	 * Creates element stress.
	 *
	 * @param name
	 *            Name of element stress.
	 */
	ElementTypeForStress(String name) {
		name_ = name;
	}

	/**
	 * Returns the name of element stress.
	 *
	 * @return The name of element stress.
	 */
	public String getName() {
		return name_;
	}

	@Override
	public String toString() {
		return name_;
	}
}
