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
 * Enumeration for ISAMI sub-versions.
 *
 * @author Murat Artim
 * @date 11 Jun 2017
 * @time 03:29:54
 *
 */
public enum IsamiSubVersion {

	/** Isami sub-version. */
	DERIVATIVES("Derivatives", "isami_derivatives"), A350("A350", "isami_A350"), DIVERSIFICATION("Diversification", "isami_diversification");

	/** Display and input nmes of version. */
	private final String displayName_, inputName_;

	/**
	 * Creates ISAMI sub-version constant.
	 *
	 * @param displayName
	 *            Display name.
	 * @param inputName
	 *            Input name.
	 */
	IsamiSubVersion(String displayName, String inputName) {
		displayName_ = displayName;
		inputName_ = inputName;
	}

	/**
	 * Returns display name.
	 *
	 * @return Display name.
	 */
	public String getDisplayName() {
		return displayName_;
	}

	/**
	 * Returns input name.
	 *
	 * @return Input name.
	 */
	public String getInputName() {
		return inputName_;
	}

	@Override
	public String toString() {
		return displayName_;
	}
}
