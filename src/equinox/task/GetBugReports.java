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

import equinox.controller.BugReportViewPanel;
import equinox.controller.ViewPanel;
import equinox.network.NetworkWatcher;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;
import equinoxServer.remote.data.BugReport;
import equinoxServer.remote.data.Permission;
import equinoxServer.remote.message.DatabaseQueryFailed;
import equinoxServer.remote.message.DatabaseQueryMessage;
import equinoxServer.remote.message.DatabaseQueryPermissionDenied;
import equinoxServer.remote.message.GetBugReportsRequest;
import equinoxServer.remote.message.GetBugReportsResponse;

/**
 * Class for get bug reports task.
 *
 * @author Murat Artim
 * @date Sep 12, 2014
 * @time 5:07:38 PM
 */
public class GetBugReports extends InternalEquinoxTask<ArrayList<BugReport>> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Status of required bug reports. */
	private final int status_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DatabaseQueryMessage> serverMessageRef;

	/**
	 * Creates get bug reports task.
	 *
	 * @param status
	 *            Status of required bug reports.
	 */
	public GetBugReports(int status) {
		status_ = status;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Get bug reports";
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
	protected ArrayList<BugReport> call() throws Exception {

		// check permission
		checkPermission(Permission.GET_BUG_REPORTS);

		// update progress info
		updateTitle("Retreiving bug reports from database");
		updateMessage("Please wait...");

		// initialize variables
		NetworkWatcher watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			GetBugReportsRequest request = new GetBugReportsRequest();
			request.setDatabaseQueryID(hashCode());
			request.setStatus(status_);

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
			else if (message instanceof GetBugReportsResponse)
				return ((GetBugReportsResponse) message).getReports();

			// no aircraft program found
			throw new Exception("No bug report found.");
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

		// set results to bug report panel
		try {
			BugReportViewPanel panel = (BugReportViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.BUG_REPORT_VIEW);
			panel.setReports(get(), status_);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.BUG_REPORT_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
