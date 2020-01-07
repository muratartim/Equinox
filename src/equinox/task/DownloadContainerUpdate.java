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

import equinox.Equinox;
import equinox.dataServer.remote.data.EquinoxUpdate;
import equinox.dataServer.remote.data.EquinoxUpdate.EquinoxUpdateInfoType;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.ServerUtility;
import equinox.task.InternalEquinoxTask.DirectoryOutputtingTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;
import javafx.application.Platform;

/**
 * Class for download container update task.
 *
 * @author Murat Artim
 * @date May 27, 2014
 * @time 1:48:41 PM
 */
public class DownloadContainerUpdate extends InternalEquinoxTask<Void> implements LongRunningTask, DirectoryOutputtingTask {

	/** Update info. */
	private final EquinoxUpdate update_;

	/**
	 * Creates download update task.
	 *
	 * @param update
	 *            Update info.
	 */
	public DownloadContainerUpdate(EquinoxUpdate update) {
		update_ = update;
	}

	@Override
	public String getTaskTitle() {
		return "Download container update";
	}

	@Override
	public Path getOutputDirectory() {
		return Equinox.UPDATE_DIR;
	}

	@Override
	public String getOutputMessage() {
		String message = "Container installation package successfully downloaded. ";
		message += "Click 'Open' to see the installation package.";
		return message;
	}

	@Override
	public String getOutputButtonText() {
		return "Open";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// update directory doesn't exist
		if (!Files.exists(Equinox.UPDATE_DIR)) {
			updateMessage("Creating update directory...");
			Files.createDirectories(Equinox.UPDATE_DIR);
		}

		// clean update directory
		updateMessage("Cleaning update directory...");
		Utility.deleteTemporaryFiles(Equinox.UPDATE_DIR, Equinox.UPDATE_DIR);

		// download file
		updateProgress(-1, 100);
		updateMessage("Downloading container installation package...");
		String osType = (String) update_.getInfo(EquinoxUpdateInfoType.OS_TYPE);
		String osArch = (String) update_.getInfo(EquinoxUpdateInfoType.OS_ARCH);
		String dataUrl = (String) update_.getInfo(EquinoxUpdateInfoType.DATA_URL);
		Path destination = Equinox.UPDATE_DIR.resolve(Utility.getContainerFileName(osType, osArch));
		try (FilerConnection filer = getFilerConnection()) {
			filer.getSftpChannel().get(dataUrl, destination.toString());
		}

		// mac or linux (return update directory)
		if (osType.equals(ServerUtility.MACOS) || osType.equals(ServerUtility.LINUX))
			return null;

		// extract installation executable
		updateMessage("Extracting container installation package...");
		Path installer = Utility.extractFileFromZIP(destination, this, FileType.EXE, Equinox.UPDATE_DIR);

		// execute installer
		updateMessage("Executing container installer...");
		executeWindowsInstaller(installer);

		// exit Equinox
		Platform.exit();

		// return
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// hide task panel
		taskPanel_.getOwner().hide();
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// hide task panel
		taskPanel_.getOwner().hide();
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// hide task panel
		taskPanel_.getOwner().hide();
	}

	/**
	 * Executes Windows installer executable.
	 *
	 * @param installer
	 *            Path to installer executable.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void executeWindowsInstaller(Path installer) throws Exception {

		// no parent installer directory found
		Path parentDir = installer.getParent();
		if (parentDir == null)
			throw new Exception("No parent installer directory found.");

		// set it executable
		installer.toFile().setExecutable(true);

		// create and setup process builder
		ProcessBuilder pb = new ProcessBuilder(installer.toAbsolutePath().toString(), "/SILENT");
		pb.directory(parentDir.toFile());

		// start process
		pb.start();
	}
}
