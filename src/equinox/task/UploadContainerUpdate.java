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

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.data.Settings;
import equinox.dataServer.remote.data.EquinoxUpdate;
import equinox.dataServer.remote.data.EquinoxUpdate.EquinoxUpdateInfoType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.UploadContainerUpdateRequest;
import equinox.dataServer.remote.message.UploadContainerUpdateResponse;
import equinox.network.DataServerManager;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.ServerUtility;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableUploadContainerUpdate;
import equinox.utility.Utility;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for upload container update task.
 *
 * @author Murat Artim
 * @date Sep 15, 2014
 * @time 3:25:58 PM
 */
public class UploadContainerUpdate extends InternalEquinoxTask<Boolean> implements LongRunningTask, SavableTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Version number. */
	private final double versionNumber_;

	/** Input files. */
	private final Path installMacFile_, installWinFile_, installWin64File_, installLinFile_, installLin64File_, verDescFile_;

	/** True to push to server. */
	private final boolean pushToDatabase_, pushToFileServer_, pushToWebServer_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates upload container update task.
	 *
	 * @param versionNumber
	 *            Version number.
	 * @param installMacFile
	 *            Mac installation package.
	 * @param installWinFile
	 *            Windows installation package.
	 * @param installWin64File
	 *            64 bit Windows installation package.
	 * @param installLinFile
	 *            Linux installation package.
	 * @param installLin64File
	 *            64 bit Linux installation package.
	 * @param verDescFile
	 *            Version description file.
	 * @param pushToDatabase
	 *            True to push to central database.
	 * @param pushToFileServer
	 *            True to push to file server.
	 * @param pushToWebServer
	 *            True to push to web server.
	 */
	public UploadContainerUpdate(double versionNumber, Path installMacFile, Path installWinFile, Path installWin64File, Path installLinFile, Path installLin64File, Path verDescFile, boolean pushToDatabase, boolean pushToFileServer, boolean pushToWebServer) {
		versionNumber_ = versionNumber;
		installMacFile_ = installMacFile;
		installWinFile_ = installWinFile;
		installWin64File_ = installWin64File;
		installLinFile_ = installLinFile;
		installLin64File_ = installLin64File;
		verDescFile_ = verDescFile;
		pushToDatabase_ = pushToDatabase;
		pushToFileServer_ = pushToFileServer;
		pushToWebServer_ = pushToWebServer;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Upload container update";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableUploadContainerUpdate(versionNumber_, installMacFile_.toFile(), installWinFile_.toFile(), installWin64File_.toFile(), installLinFile_.toFile(), installLin64File_.toFile(), verDescFile_.toFile(), pushToDatabase_, pushToFileServer_, pushToWebServer_);
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.UPLOAD_EQUINOX_UPDATE);

		// update progress info
		updateTitle("Uploading container update to server...");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		boolean isUploaded = false;

		try {

			// create request message
			UploadContainerUpdateRequest request = new UploadContainerUpdateRequest();
			request.setListenerHashCode(hashCode());

			// get connection to filer
			try (FilerConnection filer = getFilerConnection()) {

				// read version description string
				String versionDescription = "";
				try (BufferedReader reader = Files.newBufferedReader(verDescFile_, Charset.defaultCharset())) {
					String line;
					while ((line = reader.readLine()) != null) {
						versionDescription += line + "\n";
					}
				}
				request.setVersionDescription(versionDescription);

				// upload packages to filer and webs server
				request.addUpdate(uploadPackage(installMacFile_, ServerUtility.MACOS, ServerUtility.X64, filer));
				request.addUpdate(uploadPackage(installWinFile_, ServerUtility.WINDOWS, ServerUtility.X86, filer));
				request.addUpdate(uploadPackage(installWin64File_, ServerUtility.WINDOWS, ServerUtility.X64, filer));
				request.addUpdate(uploadPackage(installLinFile_, ServerUtility.LINUX, ServerUtility.X86, filer));
				request.addUpdate(uploadPackage(installLin64File_, ServerUtility.LINUX, ServerUtility.X64, filer));

				// upload version description to web server
				if (pushToWebServer_) {
					uploadVersionDescription(verDescFile_, filer);
				}
			}

			// task cancelled
			if (isCancelled())
				return null;

			// no push to central database
			if (!pushToDatabase_)
				return true;

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getDataServerManager();
			watcher.addMessageListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForDataServer(this, isQueryCompleted);

			// remove from network watcher
			watcher.removeMessageListener(this);
			removeListener = false;

			// enable task canceling
			taskPanel_.updateCancelState(true);

			// task cancelled
			if (isCancelled())
				return null;

			// get query message
			DataMessage message = serverMessageRef.get();

			// permission denied
			if (message instanceof DatabaseQueryPermissionDenied)
				throw new PermissionDeniedException(((DatabaseQueryPermissionDenied) message).getPermission());

			// query failed
			else if (message instanceof DatabaseQueryFailed)
				throw new ServerDatabaseQueryFailedException((DatabaseQueryFailed) message);

			// query succeeded
			else if (message instanceof UploadContainerUpdateResponse) {
				isUploaded = ((UploadContainerUpdateResponse) message).isUploaded();
			}

			// return result
			return isUploaded;
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}

	/**
	 * Uploads version description to web server.
	 *
	 * @param versionDescriptionFile
	 *            Version description file.
	 * @param filer
	 *            Filer connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void uploadVersionDescription(Path versionDescriptionFile, FilerConnection filer) throws Exception {

		// set data URL
		String fileName = Utility.getContainerVersionDescriptionFileName();
		String webUrl = (String) taskPanel_.getOwner().getOwner().getSettings().getValue(Settings.WEB_PATH) + fileName;

		// update info
		updateMessage("Uploading version description '" + fileName + "'...");

		// upload to web server
		if (filer.fileExists(webUrl)) {
			filer.getSftpChannel().rm(webUrl);
		}
		filer.getSftpChannel().put(versionDescriptionFile.toString(), webUrl);
	}

	/**
	 * Uploads container package to filer and web server.
	 *
	 * @param installFile
	 *            Container installation package.
	 * @param osType
	 *            Operating system.
	 * @param osArch
	 *            Operating system architecture.
	 * @param filer
	 *            Filer connection.
	 * @return Update info.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private EquinoxUpdate uploadPackage(Path installFile, String osType, String osArch, FilerConnection filer) throws Exception {

		// set data URL
		String fileName = Utility.getContainerFileName(osType, osArch);
		String dataUrl = filer.getDirectoryPath(FilerConnection.CONTAINER) + "/" + fileName;
		String webUrl = (String) taskPanel_.getOwner().getOwner().getSettings().getValue(Settings.WEB_PATH) + fileName;

		// update info
		updateMessage("Uploading container update '" + fileName + "'...");

		// upload to filer
		if (pushToFileServer_) {
			if (filer.fileExists(dataUrl)) {
				filer.getSftpChannel().rm(dataUrl);
			}
			filer.getSftpChannel().put(installFile.toString(), dataUrl);
		}

		// upload to web server
		if (pushToWebServer_) {
			if (filer.fileExists(webUrl)) {
				filer.getSftpChannel().rm(webUrl);
			}
			filer.getSftpChannel().put(installFile.toString(), webUrl);
		}

		// create and return update info
		EquinoxUpdate updateInfo = new EquinoxUpdate();
		updateInfo.setInfo(EquinoxUpdateInfoType.VERSION_NUMBER, versionNumber_);
		updateInfo.setInfo(EquinoxUpdateInfoType.OS_TYPE, osType);
		updateInfo.setInfo(EquinoxUpdateInfoType.OS_ARCH, osArch);
		updateInfo.setInfo(EquinoxUpdateInfoType.DATA_URL, dataUrl);
		updateInfo.setInfo(EquinoxUpdateInfoType.DATA_SIZE, installFile.toFile().length());
		return updateInfo;
	}
}
