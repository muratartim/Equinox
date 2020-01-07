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
 * Class for fast equivalent stress output.
 *
 * @author Murat Artim
 * @date 10 Apr 2017
 * @time 17:31:30
 *
 */
public class FastESAOutput {

	/** Equivalent stress value. */
	private final Double stress_;

	/** Output file ID. */
	private final Integer outputFileID_;

	/**
	 * Creates fast equivalent stress output.
	 *
	 * @param stress
	 *            Equivalent stress value.
	 * @param outputFileID
	 *            Output file ID.
	 */
	public FastESAOutput(Double stress, Integer outputFileID) {
		stress_ = stress;
		outputFileID_ = outputFileID;
	}

	/**
	 * Returns equivalent stress value.
	 *
	 * @return Equivalent stress value.
	 */
	public Double getStress() {
		return stress_;
	}

	/**
	 * Returns output file ID or null output file was not kept.
	 *
	 * @return Output file or null output file was not kept.
	 */
	public Integer getOutputFileID() {
		return outputFileID_;
	}
}
