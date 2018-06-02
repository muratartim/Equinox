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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.controller.RoadmapPanel;
import equinox.network.NetworkWatcher;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;
import equinoxServer.remote.data.Permission;
import equinoxServer.remote.data.Wish;
import equinoxServer.remote.data.Wish.WishInfo;
import equinoxServer.remote.message.CloseWishRequest;
import equinoxServer.remote.message.CloseWishResponse;
import equinoxServer.remote.message.DatabaseQueryFailed;
import equinoxServer.remote.message.DatabaseQueryMessage;
import equinoxServer.remote.message.DatabaseQueryPermissionDenied;

/**
 * Class for close wish task.
 *
 * @author Murat Artim
 * @date Sep 11, 2014
 * @time 3:17:45 PM
 */
public class CloseWish extends InternalEquinoxTask<Boolean> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial Id. */
	private static final long serialVersionUID = 1L;

	/** Wish. */
	private final Wish wish_;

	/** Roadmap panel. */
	private final RoadmapPanel panel_;

	/** Closure text. */
	private final String closure_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DatabaseQueryMessage> serverMessageRef;

	/**
	 * Creates like wish task.
	 *
	 * @param wish
	 *            Wish.
	 * @param closure
	 *            Closure text.
	 * @param panel
	 *            Roadmap panel.
	 */
	public CloseWish(Wish wish, String closure, RoadmapPanel panel) {
		wish_ = wish;
		panel_ = panel;
		closure_ = closure;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Close wish";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public void respondToDatabaseQueryMessage(DatabaseQueryMessage message) throws Exception {
		processServerDatabaseQueryMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.CLOSE_WISH);

		// update progress info
		updateTitle("Closing wish in the databse...");
		updateMessage("Please wait...");

		// initialize variables
		NetworkWatcher watcher = null;
		boolean removeListener = false;
		boolean isClosed = false;

		try {

			// create request message
			CloseWishRequest request = new CloseWishRequest();
			request.setDatabaseQueryID(hashCode());
			request.setWishId((long) wish_.getInfo(WishInfo.ID));
			request.setClosure(closure_);

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getNetworkWatcher();
			watcher.addDatabaseQueryListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForQuery(this, isQueryCompleted);

			// remove from network watcher
			watcher.removeDatabaseQueryListener(this);
			removeListener = false;

			// enable task canceling
			taskPanel_.updateCancelState(true);

			// task cancelled
			if (isCancelled())
				return null;

			// get query message
			DatabaseQueryMessage message = serverMessageRef.get();

			// permission denied
			if (message instanceof DatabaseQueryPermissionDenied)
				throw new PermissionDeniedException(((DatabaseQueryPermissionDenied) message).getPermission());

			// query failed
			else if (message instanceof DatabaseQueryFailed)
				throw new ServerDatabaseQueryFailedException((DatabaseQueryFailed) message);

			// query succeeded
			else if (message instanceof CloseWishResponse) {
				isClosed = ((CloseWishResponse) message).isWishClosed();
			}

			// return result
			return isClosed;
		}

		// remove from network watcher
		finally {
			if ((watcher != null) && removeListener) {
				watcher.removeDatabaseQueryListener(this);
			}
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set results to plugins panel
		try {

			// notify panel
			if (get()) {
				panel_.wishClosed();
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
