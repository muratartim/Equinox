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

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.controller.DownloadViewPanel;
import equinox.controller.ViewPanel;
import equinox.dataServer.remote.data.BasicSearchInput;
import equinox.dataServer.remote.data.DownloadInfo;
import equinox.dataServer.remote.message.BasicMultiplicationTableSearchRequest;
import equinox.dataServer.remote.message.BasicMultiplicationTableSearchResponse;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.network.DataServerManager;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for basic multiplication table search task.
 *
 * @author Murat Artim
 * @date Feb 29, 2016
 * @time 11:46:11 AM
 */
public class BasicMultiplicationTableSearch extends InternalEquinoxTask<ArrayList<DownloadInfo>> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Search input. */
	private final BasicSearchInput input;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates basic multiplication table search task.
	 *
	 * @param input
	 *            Search input.
	 */
	public BasicMultiplicationTableSearch(BasicSearchInput input) {
		this.input = input;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Search multiplication tables";
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
	protected ArrayList<DownloadInfo> call() throws Exception {

		// check permission
		checkPermission(Permission.SEARCH_MULTIPLICATION_TABLE);

		// update progress info
		updateTitle("Searching multiplication tables");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		ArrayList<DownloadInfo> results = null;

		try {

			// create request message
			BasicMultiplicationTableSearchRequest request = new BasicMultiplicationTableSearchRequest();
			request.setListenerHashCode(hashCode());
			request.setInput(input);

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
			else if (message instanceof BasicMultiplicationTableSearchResponse) {
				results = ((BasicMultiplicationTableSearchResponse) message).getSearchResults();
			}

			// return results
			return results;
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

		// set results to download panel
		try {
			DownloadViewPanel panel = (DownloadViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.DOWNLOAD_VIEW);
			panel.setDownloadItems(get(), input);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.DOWNLOAD_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
