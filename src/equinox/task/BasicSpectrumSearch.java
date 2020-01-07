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
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.controller.DownloadViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.EmbeddedTask;
import equinox.dataServer.remote.data.BasicSearchInput;
import equinox.dataServer.remote.data.DownloadInfo;
import equinox.dataServer.remote.data.SpectrumInfo;
import equinox.dataServer.remote.message.BasicSpectrumSearchRequest;
import equinox.dataServer.remote.message.BasicSpectrumSearchResponse;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.network.DataServerManager;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for basic spectrum search.
 *
 * @author Murat Artim
 * @date May 7, 2014
 * @time 11:06:12 AM
 */
public class BasicSpectrumSearch extends InternalEquinoxTask<ArrayList<DownloadInfo>> implements ShortRunningTask, DatabaseQueryListenerTask, AutomaticTaskOwner<SpectrumInfo> {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Search input. */
	private final BasicSearchInput input;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<SpectrumInfo>> automaticTasks_ = null;

	/**
	 * Creates basic spectrum search task.
	 *
	 * @param input
	 *            Search input.
	 */
	public BasicSpectrumSearch(BasicSearchInput input) {
		this.input = input;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Search spectra";
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
	public void addAutomaticTask(String taskID, EmbeddedTask<SpectrumInfo> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<SpectrumInfo>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	protected ArrayList<DownloadInfo> call() throws Exception {

		// check permission
		checkPermission(Permission.SEARCH_SPECTRUM);

		// update progress info
		updateTitle("Searching spectra");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		ArrayList<DownloadInfo> results = null;

		try {

			// create request message
			BasicSpectrumSearchRequest request = new BasicSpectrumSearchRequest();
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
			else if (message instanceof BasicSpectrumSearchResponse) {
				results = ((BasicSpectrumSearchResponse) message).getSearchResults();
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

			// get results
			ArrayList<DownloadInfo> results = get();

			// executed by user
			if (automaticTasks_ == null) {
				DownloadViewPanel panel = (DownloadViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.DOWNLOAD_VIEW);
				panel.setDownloadItems(results, input);
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.DOWNLOAD_VIEW);
			}

			// executed for automatic tasks
			else {

				// no results found
				if (results == null || results.isEmpty()) {
					addWarning("No spectrum found with given search criteria. Cannot execute furter connected tasks.");
					return;
				}

				// get only first result
				SpectrumInfo firstResult = (SpectrumInfo) results.get(0);

				// manage automatic tasks
				automaticTaskOwnerSucceeded(firstResult, automaticTasks_, taskPanel_);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}
}
