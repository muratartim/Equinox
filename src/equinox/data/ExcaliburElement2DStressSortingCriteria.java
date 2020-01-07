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
 * Enumeration for 2D element stress sorting criteria.
 *
 * @author Murat Artim
 * @date Jun 25, 2014
 * @time 11:42:27 AM
 */
public enum ExcaliburElement2DStressSortingCriteria implements ExcaliburStressSortingCriteria {

	/** Criteria. */
	MAX_ABS_PRINCIPAL("Maximum absolute principal stress"), MAX_ABS_SX("Maximum absolute normal stress - SX"), MAX_ABS_SY("Maximum absolute normal stress - SY"), MAX_ABS_SXY("Maximum absolute shear stress - SXY"), MAX_PRINCIPAL("Maximum principal stress"), MAX_SX("Maximum normal stress - SX"),
	MAX_SY("Maximum normal stress - SY"), MAX_SXY("Maximum shear stress - SXY"), MAX_ROTATED_STRESS("Maximum rotated stress");

	/** Criteria name. */
	private final String name_;

	/**
	 * Creates 2D element stress sorting criteria.
	 *
	 * @param name
	 *            Name of criteria.
	 */
	ExcaliburElement2DStressSortingCriteria(String name) {
		name_ = name;
	}

	@Override
	public String toString() {
		return name_;
	}
}
