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
package equinox.task;

import java.nio.file.Path;

import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.SharedFileInfo;
import equinox.serverUtilities.SharedFileInfo.SharedFileInfoType;
import equinox.task.InternalEquinoxTask.LongRunningTask;

/**
 * Class for download shared file task.
 *
 * @author Murat Artim
 * @date Sep 24, 2014
 * @time 9:36:16 AM
 */
public class DownloadSharedFile extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** Shared file info. */
	private final SharedFileInfo info;

	/** Output file. */
	private final Path output_;

	/**
	 * Creates download shared file task.
	 *
	 * @param info
	 *            Shared file info.
	 * @param output
	 *            Output file.
	 */
	public DownloadSharedFile(SharedFileInfo info, Path output) {
		this.info = info;
		output_ = output;
	}

	@Override
	public String getTaskTitle() {
		return "Download shared file";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Downloading shared file");
		updateMessage("Please wait...");

		// get download URL
		String url = (String) info.getInfo(SharedFileInfoType.DATA_URL);

		// download file
		if (url != null) {
			try (FilerConnection filer = getFilerConnection()) {
				if (filer.fileExists(url)) {
					filer.getSftpChannel().get(url, output_.toString());
				}
			}
		}
		return null;
	}
}
