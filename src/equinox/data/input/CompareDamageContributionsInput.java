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

import java.util.List;

/**
 * Class for compare damage contributions input.
 *
 * @author Murat Artim
 * @date Apr 16, 2015
 * @time 12:05:04 PM
 */
public class CompareDamageContributionsInput {

	/** Naming option index. */
	public static final int SPECTRUM_NAME = 0, STF_NAME = 1, EID = 2, MATERIAL_NAME = 3, PROGRAM = 4, SECTION = 5, MISSION = 6;

	/** Contribution names. */
	private final List<String> contributionNames_;

	/** Naming options. */
	private final boolean[] namingOptions_;

	/**
	 * Creates compare damage contributions input.
	 *
	 * @param contributionNames
	 *            Contribution names.
	 * @param namingOptions
	 *            Naming options.
	 */
	public CompareDamageContributionsInput(List<String> contributionNames, boolean[] namingOptions) {
		contributionNames_ = contributionNames;
		namingOptions_ = namingOptions;
	}

	/**
	 * Returns contribution names.
	 *
	 * @return Contribution names.
	 */
	public List<String> getContributionNames() {
		return contributionNames_;
	}

	/**
	 * Returns true if the given naming should be included in the series naming.
	 *
	 * @param index
	 *            Index of naming option.
	 * @return True if the given naming should be included in the series naming.
	 */
	public boolean includeName(int index) {
		return namingOptions_[index];
	}
}
