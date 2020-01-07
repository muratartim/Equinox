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
package equinox.utility.exception;

import java.io.File;

/**
 * Class for internal engine analysis failed exception.
 *
 * @author Murat Artim
 * @date 10 Apr 2017
 * @time 12:32:44
 *
 */
public class InternalEngineAnalysisFailedException extends Exception {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Output files. */
	private final File[] outputFiles_;

	/**
	 * Creates internal engine analysis failed exception.
	 */
	public InternalEngineAnalysisFailedException() {
		super();
		outputFiles_ = null;
	}

	/**
	 * Creates internal engine analysis failed exception.
	 *
	 * @param message
	 *            Exception message.
	 * @param outputFiles
	 *            Output files.
	 */
	public InternalEngineAnalysisFailedException(String message, File... outputFiles) {
		super(message);
		outputFiles_ = outputFiles;
	}

	/**
	 * Creates internal engine analysis failed exception.
	 * 
	 * @param e
	 *            Originating exception.
	 * @param outputFiles
	 *            Output files.
	 */
	public InternalEngineAnalysisFailedException(Exception e, File... outputFiles) {
		super(e.getMessage());
		setStackTrace(e.getStackTrace());
		outputFiles_ = outputFiles;
	}

	/**
	 * Returns output files array.
	 *
	 * @return Output files array or null if there is no output file produced.
	 */
	public File[] getOuputFiles() {
		return outputFiles_;
	}
}
