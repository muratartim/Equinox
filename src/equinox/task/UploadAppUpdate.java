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

import java.io.File;
import java.nio.file.Path;

import equinox.data.Settings;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableUploadAppUpdate;

/**
 * Class for upload application update task.
 *
 * @author Murat Artim
 * @date 26 May 2018
 * @time 20:19:09
 */
public class UploadAppUpdate extends InternalEquinoxTask<Void> implements LongRunningTask, SavableTask {

	/** Input files. */
	private final Path manifest_, jar_, libs_, resources_, dlls_, verDesc_;

	/**
	 * Creates upload application update task.
	 *
	 * @param manifest
	 *            Path to manifest file.
	 * @param jar
	 *            Path to jar package.
	 * @param libs
	 *            Path to libs package.
	 * @param resources
	 *            Path to resources package.
	 * @param dlls
	 *            Path to dlls package.
	 * @param verDesc
	 *            Path to version description file.
	 */
	public UploadAppUpdate(Path manifest, Path jar, Path libs, Path resources, Path dlls, Path verDesc) {
		manifest_ = manifest;
		jar_ = jar;
		libs_ = libs;
		resources_ = resources;
		dlls_ = dlls;
		verDesc_ = verDesc;
	}

	@Override
	public String getTaskTitle() {
		return "Upload application update";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public SerializableTask getSerializableTask() {
		File jar = jar_ == null ? null : jar_.toFile();
		File libs = libs_ == null ? null : libs_.toFile();
		File resources = resources_ == null ? null : resources_.toFile();
		File dlls = dlls_ == null ? null : dlls_.toFile();
		File verDesc = verDesc_ == null ? null : verDesc_.toFile();
		return new SerializableUploadAppUpdate(manifest_.toFile(), jar, libs, resources, dlls, verDesc);
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.UPLOAD_EQUINOX_UPDATE);

		// update progress info
		updateTitle("Uploading application update to server...");
		updateMessage("Please wait...");

		// get connection to filer
		try (FilerConnection filer = getFilerConnection()) {

			// upload packages to filer
			uploadFile(manifest_, filer);
			uploadFile(jar_, filer);
			uploadFile(libs_, filer);
			uploadFile(resources_, filer);
			uploadFile(dlls_, filer);

			// upload version description to web server
			uploadVersionDescription(verDesc_, filer);
		}

		// return
		return null;
	}

	/**
	 * Uploads given file to SMTP file server under update directory.
	 *
	 * @param file
	 *            File to upload.
	 * @param filer
	 *            File server connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void uploadFile(Path file, FilerConnection filer) throws Exception {

		// null file
		if (file == null)
			return;

		// set data URL
		String fileName = file.getFileName().toString();
		String dataUrl = filer.getDirectoryPath(FilerConnection.UPDATE) + "/" + fileName;

		// update info
		updateMessage("Uploading application resource '" + fileName + "'...");

		// remove file if already exists
		if (filer.fileExists(dataUrl)) {
			filer.getSftpChannel().rm(dataUrl);
		}

		// upload
		filer.getSftpChannel().put(file.toString(), dataUrl);
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

		// null file
		if (versionDescriptionFile == null)
			return;

		// set data URL
		String fileName = versionDescriptionFile.getFileName().toString();
		String webUrl = (String) taskPanel_.getOwner().getOwner().getSettings().getValue(Settings.WEB_PATH) + fileName;

		// update info
		updateMessage("Uploading version description '" + fileName + "'...");

		// remove file if already exists
		if (filer.fileExists(webUrl)) {
			filer.getSftpChannel().rm(webUrl);
		}

		// upload
		filer.getSftpChannel().put(versionDescriptionFile.toString(), webUrl);
	}
}
