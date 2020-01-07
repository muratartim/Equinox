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
 * Enumeration for plot contour.
 *
 * @author Murat Artim
 * @date Nov 3, 2015
 * @time 1:22:45 PM
 */
public enum PlotContour {

	/** Plot contour option. */
	DISCRETE("Discrete"), SMOOTHED("Smoothed");

	/** Name of contour. */
	private final String name_;

	/**
	 * Creates plot contour.
	 *
	 * @param name
	 *            Name of contour.
	 */
	PlotContour(String name) {
		name_ = name;
	}

	@Override
	public String toString() {
		return name_;
	}
}
