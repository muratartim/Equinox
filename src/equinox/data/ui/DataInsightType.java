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
package equinox.data.ui;

/**
 * Enumeration for data insight type.
 * 
 * @author Murat Artim
 * @date 26 Jul 2017
 * @time 11:16:15
 *
 */
public enum DataInsightType {

	/** Data insight type. */
	SPECTRUM_COUNT("Spectrum count"), SPECTRUM_SIZE("Spectrum size"), PILOT_POINT_COUNT("Pilot point count"), DAMAGE_CONTRIBUTIONS("Damage contributions");

	/** Type name. */
	private final String name_;

	/**
	 * Creates data insight type.
	 * 
	 * @param name
	 *            name of type.
	 */
	DataInsightType(String name) {
		name_ = name;
	}

	@Override
	public String toString() {
		return name_;
	}
}
