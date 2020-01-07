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
package equinox.utility;

/**
 * Interface for download listener. The download listener receives messages from the read loop. It is passed the progress as a percentage if known, or
 * -1.0 to indicate indeterminate progress.
 *
 * @author Murat Artim
 * @date May 15, 2016
 * @time 3:48:14 PM
 */
public interface DownloadListener {

	/**
	 * Sets download progress to this listener.
	 *
	 * @param rbc
	 *            Data reader.
	 * @param progress
	 *            Progress as a percentage, or -1.0 to indicate indeterminate progress.
	 */
	public void setDownloadProgress(RBCWrapper rbc, double progress);
}
