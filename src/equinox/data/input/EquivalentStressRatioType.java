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
package equinox.data.input;

/**
 * Enumeration for equivalent stress ratio type.
 *
 * @author Murat Artim
 * @date Sep 23, 2015
 * @time 11:48:08 AM
 */
public enum EquivalentStressRatioType {

	/** Equivalent stress ratio type. */
	FATIGUE_RATIO("Fatigue equivalent stress ratio"), PROPAGATION_RATIO("Propagation equivalent stress ratio");

	/** Name of life factor. */
	private final String name_;

	/**
	 * Creates equivalent stress ratio type.
	 *
	 * @param name
	 *            Name of equivalent stress ratio.
	 */
	EquivalentStressRatioType(String name) {
		name_ = name;
	}

	@Override
	public String toString() {
		return name_;
	}
}
