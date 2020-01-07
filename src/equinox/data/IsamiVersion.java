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
 * Enumeration for ISAMI version.
 *
 * @author Murat Artim
 * @date 17 Apr 2018
 * @time 15:28:54
 */
public enum IsamiVersion {

	/** ISAMI version constant. */
	// @formatter:off
	V9_0_0_NEO("v9.0.0_NEO", "900.2.15"), V9_1_0_NEO("v9.1.0_NEO", "910.3.15"), V9_2_0("v9.2.0", "920.2.15"), V9_3_0("v9.3.0", "930.5.15"),
	V9_4_0("v9.4.0", "940.4.15"), V9_5_0("v9.5.0", "950.2.15"), V9_6_0("v9.6.0", "950.2.15"), V9_7_0("v9.7.0", "970.4.15");
	// @formatter:on

	/** Default ISAMI version. */
	public static final IsamiVersion DEFAULT_VERSION = V9_7_0;

	/** ISAMI and material library version. */
	private final String isamiVersion, materialLibraryVersion;

	/**
	 * Creates ISAMI version constant.
	 *
	 * @param isamiVersion
	 *            ISAMI version.
	 * @param materialLibraryVersion
	 *            Connected material library version.
	 */
	IsamiVersion(String isamiVersion, String materialLibraryVersion) {
		this.isamiVersion = isamiVersion;
		this.materialLibraryVersion = materialLibraryVersion;
	}

	/**
	 * Returns ISAMI version.
	 *
	 * @return ISAMI version.
	 */
	public String getIsamiVersion() {
		return isamiVersion;
	}

	/**
	 * Returns material library version.
	 *
	 * @return Material library version.
	 */
	public String getMaterialLibraryVersion() {
		return materialLibraryVersion;
	}

	@Override
	public String toString() {
		return isamiVersion;
	}
}
