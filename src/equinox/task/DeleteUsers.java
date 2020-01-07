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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.DeleteUsersRequest;
import equinox.dataServer.remote.message.DeleteUsersResponse;
import equinox.network.DataServerManager;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableDeleteUsers;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for delete users task.
 *
 * @author Murat Artim
 * @date 4 Apr 2018
 * @time 18:07:00
 */
public class DeleteUsers extends InternalEquinoxTask<Boolean> implements LongRunningTask, SavableTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** SQL statement. */
	private final String aliases_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates execute SQL statement task.
	 *
	 * @param aliases
	 *            User aliases.
	 */
	public DeleteUsers(String aliases) {
		aliases_ = aliases;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Delete user accounts";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableDeleteUsers(aliases_);
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.DELETE_USER);

		// update progress info
		updateTitle("Delete user accounts...");
		updateMessage("Please wait...");

		// split aliases
		String[] split = null;
		if (aliases_.contains(",")) {
			split = aliases_.split(",");
			for (int i = 0; i < split.length; i++) {
				split[i] = split[i].trim();
			}
		}
		if (split == null) {
			split = new String[1];
			split[0] = aliases_.trim();
		}

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		boolean isDeleted = false;

		try {

			// create request message
			DeleteUsersRequest request = new DeleteUsersRequest();
			request.setListenerHashCode(hashCode());
			request.setAliases(split);

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
			else if (message instanceof DeleteUsersResponse) {
				isDeleted = ((DeleteUsersResponse) message).isDeleted();
			}

			// return result
			return isDeleted;
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}
}
