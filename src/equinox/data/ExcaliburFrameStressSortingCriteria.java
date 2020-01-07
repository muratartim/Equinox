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
 * Class for frame stress sorting criteria.
 *
 * @author Murat Artim
 * @date 29 Nov 2017
 * @time 12:36:16
 */
public enum ExcaliburFrameStressSortingCriteria implements ExcaliburStressSortingCriteria {

	/** Criteria. */
	MAX_ABS_STRESS("Maximum absolute stress"), MAX_STRESS("Maximum stress");

	/** Criteria name. */
	private final String name_;

	/**
	 * Creates frame stress sorting criteria.
	 *
	 * @param name
	 *            Name of criteria.
	 */
	ExcaliburFrameStressSortingCriteria(String name) {
		name_ = name;
	}

	@Override
	public String toString() {
		return name_;
	}
}
