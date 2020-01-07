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
package equinox.task.serializableTask;

import java.io.File;

import equinox.task.SerializableTask;
import equinox.task.UploadContainerUpdate;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of upload container update task.
 *
 * @author Murat Artim
 * @date Oct 13, 2015
 * @time 2:06:18 PM
 */
public class SerializableUploadContainerUpdate implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Version number. */
	private final double versionNumber_;

	/** Input files. */
	private final File installMacFile_, installWinFile_, installWin64File_, installLinFile_, installLin64File_, verDescFile_;

	/** True to push to server. */
	private final boolean pushToDatabase_, pushToFileServer_, pushToWebServer_;

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
	public SerializableUploadContainerUpdate(double versionNumber, File installMacFile, File installWinFile, File installWin64File, File installLinFile, File installLin64File, File verDescFile, boolean pushToDatabase, boolean pushToFileServer, boolean pushToWebServer) {
		versionNumber_ = versionNumber;
		installMacFile_ = installMacFile;
		installWinFile_ = installWinFile;
		installWin64File_ = installWin64File;
		installLinFile_ = installLinFile;
		installLin64File_ = installLinFile;
		verDescFile_ = verDescFile;
		pushToDatabase_ = pushToDatabase;
		pushToFileServer_ = pushToFileServer;
		pushToWebServer_ = pushToWebServer;
	}

	@Override
	public UploadContainerUpdate getTask(TreeItem<String> fileTreeRoot) {
		return new UploadContainerUpdate(versionNumber_, installMacFile_.toPath(), installWinFile_.toPath(), installWin64File_.toPath(), installLinFile_.toPath(), installLin64File_.toPath(), verDescFile_.toPath(), pushToDatabase_, pushToFileServer_, pushToWebServer_);
	}
}
