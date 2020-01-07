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
 * Class for reference load case.
 *
 * @author Murat Artim
 * @date Sep 10, 2015
 * @time 3:26:30 PM
 */
public class ReferenceLoadCase {

	/** Load case name. */
	private final String name_;

	/** Load case ID. */
	private final int id_;

	/** Load case factor. */
	private double factor_ = 1.0;

	/**
	 * Creates reference load case.
	 *
	 * @param name
	 *            Load case name.
	 * @param number
	 *            Load case number.
	 * @param id
	 *            Load case ID.
	 */
	public ReferenceLoadCase(String name, int number, int id) {
		name_ = name + " (" + number + ")";
		id_ = id;
	}

	/**
	 * Sets load case factor.
	 *
	 * @param factor
	 *            Load case factor.
	 */
	public void setFactor(double factor) {
		factor_ = factor;
	}

	/**
	 * Returns load case ID.
	 *
	 * @return Load case ID.
	 */
	public int getID() {
		return id_;
	}

	/**
	 * Returns load case factor.
	 *
	 * @return Load case factor.
	 */
	public double getFactor() {
		return factor_;
	}

	@Override
	public String toString() {
		return name_;
	}
}
