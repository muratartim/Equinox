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
 * Enumeration for excalibur stress type.
 *
 * @author Murat Artim
 * @date 29 Nov 2017
 * @time 12:14:01
 */
public enum ExcaliburStressType {

	/** Element type. */
	ELEMENT_1D("1D element stresses", "element_1d"), ELEMENT_2D("2D element stresses", "element_2d"), FRAME("Frame stresses", "frame_stresses");

	/** Name of element type. */
	private final String displayName_, dbTableName_;

	/**
	 * Creates stress type constant.
	 *
	 * @param displayName
	 *            Display name of stress type.
	 * @param dbTableName
	 *            Database table name.
	 */
	ExcaliburStressType(String displayName, String dbTableName) {
		displayName_ = displayName;
		dbTableName_ = dbTableName;
	}

	/**
	 * Returns database table name.
	 *
	 * @return Database table name.
	 */
	public String getDatabaseTableName() {
		return dbTableName_;
	}

	@Override
	public String toString() {
		return displayName_;
	}
}
