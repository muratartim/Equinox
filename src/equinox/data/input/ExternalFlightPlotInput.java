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
 * Class for external flight plot input.
 *
 * @author Murat Artim
 * @date Mar 15, 2015
 * @time 4:30:33 PM
 */
public class ExternalFlightPlotInput {

	/** Plot component options. */
	private boolean[] namingOptions_ = { false, false, true, false, false, false };

	/**
	 * Sets series naming options.
	 *
	 * @param options
	 *            Series naming options.
	 */
	public void setNamingOptions(boolean[] options) {
		namingOptions_ = options;
	}

	/**
	 * Returns series naming options.
	 *
	 * @return Series naming options.
	 */
	public boolean[] getNamingOptions() {
		return namingOptions_;
	}
}
