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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;

import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.SharedFileInfo;
import equinox.serverUtilities.SharedFileInfo.SharedFileInfoType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for download shared spectrum task.
 *
 * @author Murat Artim
 * @date Jun 14, 2016
 * @time 4:05:08 PM
 */
public class DownloadSharedSpectrum extends TemporaryFileCreatingTask<AddSpectrum> implements ShortRunningTask {

	/** Shared file info. */
	private final SharedFileInfo info;

	/**
	 * Creates download shared spectrum task.
	 *
	 * @param info
	 *            Shared file info.
	 */
	public DownloadSharedSpectrum(SharedFileInfo info) {
		this.info = info;
	}

	@Override
	public String getTaskTitle() {
		return "Download shared spectrum";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected AddSpectrum call() throws Exception {

		// update progress info
		updateTitle("Downloading shared spectrum");
		updateMessage("Downloading shared spectrum from file server...");

		// create path to output file
		Path output = getWorkingDirectory().resolve((String) info.getInfo(SharedFileInfoType.FILE_NAME));

		// download file from filer
		String url = (String) info.getInfo(SharedFileInfoType.DATA_URL);
		if (url != null) {
			try (FilerConnection filer = getFilerConnection()) {
				if (filer.fileExists(url)) {
					filer.getSftpChannel().get(url, output.toString());
				}
			}
		}

		// null file
		if (output == null)
			throw new Exception("Could not retrieve shared file from server.");

		// null file name path
		Path outputFileNamePath = output.getFileName();
		if (outputFileNamePath == null)
			throw new Exception("Could not retrieve shared file from server.");

		// create add spectrum task
		AddSpectrum task = new AddSpectrum(null);

		// copy and set spectrum bundle file
		Path outputCopy = task.getWorkingDirectory().resolve(outputFileNamePath.toString());
		task.setSpectrumBundle(Files.copy(output, outputCopy, StandardCopyOption.REPLACE_EXISTING));

		// return task
		return task;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// load spectrum
		try {
			taskPanel_.getOwner().runTaskInParallel(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
