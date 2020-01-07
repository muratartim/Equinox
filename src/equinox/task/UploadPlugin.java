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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.data.ClientPluginInfo;
import equinox.dataServer.remote.data.ServerPluginInfo;
import equinox.dataServer.remote.data.ServerPluginInfo.PluginInfoType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.UploadPluginRequest;
import equinox.dataServer.remote.message.UploadPluginResponse;
import equinox.network.DataServerManager;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableUploadPlugin;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for upload plugin task.
 *
 * @author Murat Artim
 * @date Mar 31, 2015
 * @time 12:59:50 PM
 */
public class UploadPlugin extends InternalEquinoxTask<Boolean> implements LongRunningTask, SavableTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Plugin info. */
	private final ClientPluginInfo pluginInfo_;

	/** Plugin jar file. */
	private final Path jarFile_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates upload plugin task.
	 *
	 * @param pluginInfo
	 *            Plugin info.
	 * @param jarFile
	 *            Plugin jar file.
	 */
	public UploadPlugin(ClientPluginInfo pluginInfo, Path jarFile) {
		pluginInfo_ = pluginInfo;
		jarFile_ = jarFile;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Upload plugin";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableUploadPlugin(pluginInfo_, jarFile_);
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.UPLOAD_EQUINOX_PLUGIN);

		// update progress info
		updateTitle("Uploading Equinox plugin");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		boolean isUploaded = false;

		try {

			// create request message
			UploadPluginRequest request = new UploadPluginRequest();
			request.setListenerHashCode(hashCode());

			// get connection to filer
			try (FilerConnection filer = getFilerConnection()) {

				// create and set plugin info
				String imageURL = filer.getDirectoryPath(FilerConnection.PLUGINS) + "/" + ((String) pluginInfo_.getInfo(PluginInfoType.JAR_NAME)).replaceFirst(".jar", ".png");
				String jarURL = filer.getDirectoryPath(FilerConnection.PLUGINS) + "/" + (String) pluginInfo_.getInfo(PluginInfoType.JAR_NAME);
				ServerPluginInfo info = new ServerPluginInfo();
				info.setInfo(PluginInfoType.NAME, pluginInfo_.getInfo(PluginInfoType.NAME));
				info.setInfo(PluginInfoType.JAR_NAME, pluginInfo_.getInfo(PluginInfoType.JAR_NAME));
				info.setInfo(PluginInfoType.DESCRIPTION, pluginInfo_.getInfo(PluginInfoType.DESCRIPTION));
				info.setInfo(PluginInfoType.VERSION_NUMBER, pluginInfo_.getInfo(PluginInfoType.VERSION_NUMBER));
				info.setInfo(PluginInfoType.IMAGE_URL, imageURL);
				info.setInfo(PluginInfoType.DATA_SIZE, pluginInfo_.getInfo(PluginInfoType.DATA_SIZE));
				info.setInfo(PluginInfoType.DEVELOPER_NAME, pluginInfo_.getInfo(PluginInfoType.DEVELOPER_NAME));
				info.setInfo(PluginInfoType.DEVELOPER_EMAIL, pluginInfo_.getInfo(PluginInfoType.DEVELOPER_EMAIL));
				info.setInfo(PluginInfoType.DATA_URL, jarURL);
				request.setPluginInfo(info);

				// delete plugin data from filer
				updateMessage("Deleting plugin '" + (String) pluginInfo_.getInfo(PluginInfoType.NAME) + "' from filer...");
				if (filer.fileExists(jarURL)) {
					filer.getSftpChannel().rm(jarURL);
				}
				if (filer.fileExists(imageURL)) {
					filer.getSftpChannel().rm(imageURL);
				}

				// upload plugin image and jar to filer
				updateMessage("Uploading plugin '" + (String) pluginInfo_.getInfo(PluginInfoType.NAME) + "' to filer...");
				try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pluginInfo_.getImageBytes())) {
					filer.getSftpChannel().put(inputStream, imageURL);
				}
				filer.getSftpChannel().put(jarFile_.toString(), jarURL);
			}

			// task cancelled
			if (isCancelled())
				return null;

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
			else if (message instanceof UploadPluginResponse) {
				isUploaded = ((UploadPluginResponse) message).isUploaded();
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

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set results to plugins panel
		try {

			// upload sample inputs (if any)
			if (get()) {
				if (pluginInfo_.getSampleInputs() != null) {
					ArrayList<File> files = new ArrayList<>();
					files.add(pluginInfo_.getSampleInputs());
					taskPanel_.getOwner().runTaskInParallel(new UploadSampleInputs(files));
				}
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
