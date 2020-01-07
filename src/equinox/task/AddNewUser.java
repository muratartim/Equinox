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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.dataServer.remote.message.AddNewUserRequest;
import equinox.dataServer.remote.message.AddNewUserResponse;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.network.DataServerManager;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableAddNewUser;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for add new user task.
 *
 * @author Murat Artim
 * @date 4 Apr 2018
 * @time 16:15:07
 */
public class AddNewUser extends InternalEquinoxTask<Boolean> implements LongRunningTask, SavableTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** User info. */
	private final String alias, name, organization, email;

	/** User permissions. */
	private final Permission[] permissions;

	/** Profile image. */
	private final Path image;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates add new user task.
	 *
	 * @param alias
	 *            User alias.
	 * @param name
	 *            User name.
	 * @param organization
	 *            User organization.
	 * @param email
	 *            User email.
	 * @param permissions
	 *            User permissions.
	 * @param image
	 *            Profile image (can be null).
	 */
	public AddNewUser(String alias, String name, String organization, String email, Permission[] permissions, Path image) {
		this.alias = alias;
		this.name = name;
		this.organization = organization;
		this.email = email;
		this.permissions = permissions;
		this.image = image;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Create user account";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableAddNewUser(alias, name, organization, email, permissions, image == null ? null : image.toFile());
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.ADD_NEW_USER);

		// update progress info
		updateTitle("Creating user account...");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		boolean isAdded = false;

		try {

			// create request message
			AddNewUserRequest request = new AddNewUserRequest();
			request.setListenerHashCode(hashCode());
			request.setAlias(alias);
			request.setName(name);
			request.setOrganization(organization);
			request.setEmail(email);
			request.setPermissions(permissions);

			// set image URL
			String imageUrl = null;
			if (image != null && Files.exists(image)) {

				// get connection to filer
				try (FilerConnection filer = getFilerConnection()) {

					// create image URL
					imageUrl = filer.getDirectoryPath(FilerConnection.USERS) + "/" + alias + ".png";

					// delete image from filer (if exists)
					if (filer.fileExists(imageUrl)) {
						filer.getSftpChannel().rm(imageUrl);
					}

					// upload image to filer
					filer.getSftpChannel().put(image.toString(), imageUrl);
				}
			}
			request.setImageUrl(imageUrl);

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
			else if (message instanceof AddNewUserResponse) {
				isAdded = ((AddNewUserResponse) message).isAdded();
			}

			// return result
			return isAdded;
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}
}
