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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import equinox.controller.ServerAnalysisFailedPanel;
import equinox.serverUtilities.FilerConnection;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.Utility;

/**
 * Class for download failed analysis outputs task.
 *
 * @author Murat Artim
 * @date 7 Apr 2017
 * @time 16:47:18
 *
 */
public class DownloadFailedAnalysisOutputs extends TemporaryFileCreatingTask<ArrayList<Path>> implements ShortRunningTask {

	/** Download URL. */
	private final String downloadUrl_;

	/** Analysis failed panel. */
	private final ServerAnalysisFailedPanel panel_;

	/**
	 * Creates download failed analysis outputs task.
	 *
	 * @param downloadUrl
	 *            Download URL.
	 * @param panel
	 *            Analysis failed panel.
	 */
	public DownloadFailedAnalysisOutputs(String downloadUrl, ServerAnalysisFailedPanel panel) {
		downloadUrl_ = downloadUrl;
		panel_ = panel;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Download failed analysis outputs";
	}

	@Override
	protected ArrayList<Path> call() throws Exception {

		// update progress info
		updateTitle("Downloading failed analysis outputs...");
		updateMessage("Please wait...");

		// create download path
		Path downloadPath = getWorkingDirectory().resolve("output.zip");

		// download file
		try (FilerConnection filer = getFilerConnection()) {
			if (filer.fileExists(downloadUrl_)) {
				filer.getSftpChannel().get(downloadUrl_, downloadPath.toString());
			}
		}

		// extract outputs
		updateMessage("Extracting output files...");
		ArrayList<Path> outputs = Utility.extractAllFilesFromZIP(downloadPath, this, getWorkingDirectory());

		// mark output files as permanent
		for (Path file : outputs) {
			setFileAsPermanent(file);
		}

		// return outputs
		return outputs;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set outputs to panel
		try {
			panel_.setOutputs(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// set null outputs
		panel_.setOutputs(null);
	}
}
