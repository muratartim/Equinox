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

import equinox.plugin.FileType;

/**
 * Enumeration for analysis engine.
 *
 * @author Murat Artim
 * @date Feb 7, 2015
 * @time 2:00:28 PM
 */
public enum AnalysisEngine {

	/** Analysis engine type. */
	ISAMI("ISAMI Engine", FileType.HTML), SAFE("SAFE Engine", FileType.DOSSIER), INBUILT("Inbuilt Engine", FileType.DOSSIER);

	/** Name of analysis engine. */
	private final String name_;

	/** Output file type of analysis engine. */
	private final FileType outputFileType_;

	/**
	 * Creates analysis engine constant.
	 *
	 * @param name
	 *            Name of engine.
	 * @param outputFileType
	 *            Output file type of analysis engine.
	 */
	AnalysisEngine(String name, FileType outputFileType) {
		name_ = name;
		outputFileType_ = outputFileType;
	}

	/**
	 * Returns name of engine.
	 *
	 * @return Name of engine.
	 */
	public String getName() {
		return name_;
	}

	/**
	 * Returns output file type of the analysis engine.
	 *
	 * @return Output file type of the analysis engine.
	 */
	public FileType getOutputFileType() {
		return outputFileType_;
	}

	@Override
	public String toString() {
		return name_;
	}
}
