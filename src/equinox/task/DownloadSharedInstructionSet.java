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
import java.util.concurrent.ExecutionException;

import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.SharedFileInfo;
import equinox.serverUtilities.SharedFileInfo.SharedFileInfoType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.CheckInstructionSet;
import equinox.utility.Utility;

/**
 * Class for download shared instruction set task.
 *
 * @author Murat Artim
 * @date 22 Sep 2018
 * @time 18:01:14
 */
public class DownloadSharedInstructionSet extends TemporaryFileCreatingTask<CheckInstructionSet> implements ShortRunningTask {

	/** Shared file info. */
	private final SharedFileInfo info;

	/** Path to output file. */
	private final Path output;

	/** True to run the instruction set once it is downloaded. */
	private final boolean run;

	/**
	 * Creates download shared instruction set task.
	 *
	 * @param info
	 *            Shared file info.
	 * @param output
	 *            Path to output file. Will be used only if the run parameter is false.
	 * @param run
	 *            True to run the instruction set once it is downloaded.
	 */
	public DownloadSharedInstructionSet(SharedFileInfo info, Path output, boolean run) {
		this.info = info;
		this.output = output;
		this.run = run;
	}

	@Override
	public String getTaskTitle() {
		return "Download shared instruction set";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected CheckInstructionSet call() throws Exception {

		// update progress info
		updateTitle("Downloading shared instruction set");
		updateMessage("Downloading shared instruction set from file server...");

		// create path to output file
		Path outputPath = run ? getWorkingDirectory().resolve((String) info.getInfo(SharedFileInfoType.FILE_NAME)) : output;

		// download file from filer
		String url = (String) info.getInfo(SharedFileInfoType.DATA_URL);
		if (url != null) {
			try (FilerConnection filer = getFilerConnection()) {
				if (filer.fileExists(url)) {
					filer.getSftpChannel().get(url, outputPath.toString());
				}
			}
		}

		// no run requested
		if (!run)
			return null;

		// extract instruction set
		Path instructionSet = Utility.extractFileFromZIP(outputPath, this, FileType.XML, null);
		if (instructionSet == null) {
			instructionSet = Utility.extractFileFromZIP(outputPath, this, FileType.JSON, null);
		}

		// mark output path as permanent
		setFileAsPermanent(instructionSet);

		// create and return task
		return new CheckInstructionSet(instructionSet, CheckInstructionSet.RUN);
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// load spectrum
		try {

			// get task
			CheckInstructionSet task = get();

			// run task
			if (task != null) {
				taskPanel_.getOwner().runTaskInParallel(get());
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}