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

import java.util.HashMap;

/**
 * Class for RFORT direct omission.
 *
 * @author Murat Artim
 * @date Apr 15, 2016
 * @time 2:59:59 PM
 */
public class RfortDirectOmission extends RfortOmission {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Name of omission. */
	private final String name_;

	/** Omission mapping. */
	private final HashMap<String, Double> omissions_;

	/**
	 * Creates RFORT direct omission.
	 *
	 * @param name
	 *            Name of omission.
	 */
	public RfortDirectOmission(String name) {
		name_ = name;
		omissions_ = new HashMap<>();
	}

	@Override
	public String getOmissionType() {
		return DIRECT;
	}

	/**
	 * Returns the name of omission.
	 *
	 * @return The name of omission.
	 */
	public String getName() {
		return name_;
	}

	/**
	 * Returns the omissions.
	 *
	 * @return The omissions.
	 */
	public HashMap<String, Double> getOmissions() {
		return omissions_;
	}

	/**
	 * Adds omission.
	 *
	 * @param pilotPoint
	 *            Pilot point name.
	 * @param omission
	 *            Omission value.
	 */
	public void addOmission(String pilotPoint, double omission) {
		omissions_.put(pilotPoint, omission);
	}

	@Override
	public String toString() {
		return name_;
	}
}
