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

import java.io.Serializable;

/**
 * Class for delta-t interpolator.
 *
 * @author Murat Artim
 * @date Oct 20, 2014
 * @time 3:36:18 PM
 */
public abstract class DTInterpolator implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Returns the interpolated stress value.
	 *
	 * @param deltaT
	 *            Delta temperature.
	 * @return The interpolated stress value.
	 */
	public abstract double getStress(double deltaT);
}
