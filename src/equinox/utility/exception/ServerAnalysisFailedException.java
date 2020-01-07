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

import equinox.analysisServer.remote.message.AnalysisFailed;

/**
 * Class for server analysis failed exception.
 *
 * @author Murat Artim
 * @date 7 Apr 2017
 * @time 16:04:44
 *
 */
public class ServerAnalysisFailedException extends Exception {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Server message. */
	private final AnalysisFailed serverMessage_;

	/**
	 * Creates server analysis failed exception.
	 *
	 * @param serverMessage
	 *            Server message.
	 */
	public ServerAnalysisFailedException(AnalysisFailed serverMessage) {
		serverMessage_ = serverMessage;
	}

	/**
	 * Returns the thrown exception message of the analysis from the server.
	 *
	 * @return The thrown exception message of the analysis from the server.
	 */
	public String getServerExceptionMessage() {
		return serverMessage_.getExceptionMessage();
	}

	/**
	 * Returns analysis output file download URL or <code>null</code> if no output file was uploaded or produced.
	 *
	 * @return Analysis output file download URL or <code>null</code> if no output file was uploaded or produced.
	 */
	public String getDownloadUrl() {
		return serverMessage_.getDownloadUrl();
	}

	/**
	 * Returns the listener hash code.
	 *
	 * @return Listener hash code.
	 */
	public int getListenerHashCode() {
		return serverMessage_.getListenerHashCode();
	}
}
