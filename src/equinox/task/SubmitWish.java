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
import equinox.dataServer.remote.data.Wish;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.SubmitWishRequest;
import equinox.dataServer.remote.message.SubmitWishResponse;
import equinox.network.DataServerManager;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for submit wish task.
 *
 * @author Murat Artim
 * @date May 16, 2014
 * @time 5:39:57 PM
 */
public class SubmitWish extends InternalEquinoxTask<Long> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Title. */
	private String title_;

	/** Description. */
	private String description_;

	/** Roadmap panel. */
	private final RoadmapPanel panel_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates submit wish task.
	 *
	 * @param title
	 *            Title of wish.
	 * @param description
	 *            Description of wish (can be null).
	 * @param panel
	 *            Roadmap panel.
	 */
	public SubmitWish(String title, String description, RoadmapPanel panel) {
		title_ = title;
		description_ = description;
		panel_ = panel;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Sumbit wish to database";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Long call() throws Exception {

		// check permission
		checkPermission(Permission.SUBMIT_WISH);

		// update progress info
		updateTitle("Sumbitting wish to database server");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		// trim report text (if necessary)
		if (title_.length() > Wish.MAX_TITLE_SIZE) {
			title_ = title_.substring(0, 85) + " and more...";
		}
		if (description_ != null && description_.length() > Wish.MAX_DESCRIPTION_SIZE) {
			description_ = description_.substring(0, 900) + " and more...(rest is truncated due to maximum character limit).";
		}

		try {

			// create request message
			SubmitWishRequest request = new SubmitWishRequest();
			request.setListenerHashCode(hashCode());
			request.setTitle(title_);
			request.setDescription(description_);

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
			else if (message instanceof SubmitWishResponse)
				return ((SubmitWishResponse) message).getWishId();

			// invalid response
			throw new Exception("Invalid response received from server.");
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

		// show info
		String title = "Wish saved";
		String message = "Thank you very much. Your wish has been successfully saved to roadmap database.";
		taskPanel_.getOwner().getOwner().getNotificationPane().showInfo(title, message);

		// notify panel
		try {
			panel_.wishSubmitted(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
