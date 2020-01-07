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

import equinox.dataServer.remote.data.MultiplicationTableInfo;
import equinox.dataServer.remote.data.MultiplicationTableInfo.MultiplicationTableInfoType;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;

/**
 * Class for download multiplication table task.
 *
 * @author Murat Artim
 * @date Feb 29, 2016
 * @time 2:32:48 PM
 */
public class DownloadMultiplicationTable extends TemporaryFileCreatingTask<Void> implements LongRunningTask {

	/** Multiplication table info. */
	private final MultiplicationTableInfo info_;

	/** Output file. */
	private Path output_;

	/**
	 * Creates download multiplication table task.
	 *
	 * @param info
	 *            CDF set info.
	 * @param output
	 *            Output file.
	 */
	public DownloadMultiplicationTable(MultiplicationTableInfo info, Path output) {
		info_ = info;
		output_ = output;
	}

	@Override
	public String getTaskTitle() {
		return "Download multiplication table";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.DOWNLOAD_MULTIPLICATION_TABLE);

		// update progress info
		updateTitle("Downloading multiplication table");

		// get URL to file
		String url = (String) info_.getInfo(MultiplicationTableInfoType.DATA_URL);
		String name = (String) info_.getInfo(MultiplicationTableInfoType.NAME);

		// no URL found
		if (url == null || url.trim().isEmpty())
			throw new Exception("No URL found for downloading multiplication table '" + name + "'.");

		// check output file
		if (output_ == null) {
			output_ = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
		}

		// download archive
		try (FilerConnection filer = getFilerConnection()) {
			if (filer.fileExists(url)) {
				filer.getSftpChannel().get(url, output_.toString());
			}
		}

		// return
		return null;
	}
}
