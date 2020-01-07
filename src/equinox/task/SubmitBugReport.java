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

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import equinox.Equinox;
import equinox.controller.BugReportPanel;
import equinox.dataServer.remote.data.BugReport;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.SubmitBugReportRequest;
import equinox.dataServer.remote.message.SubmitBugReportResponse;
import equinox.network.DataServerManager;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for submit bug report task.
 *
 * @author Murat Artim
 * @date May 11, 2014
 * @time 7:23:05 PM
 */
public class SubmitBugReport extends InternalEquinoxTask<Boolean> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Report text. */
	private String report_;

	/** True if system information and event log should be added to the report. */
	private final boolean sysInfo_, eventLog_;

	/** Bug report panel. */
	private final BugReportPanel panel_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates submit bug report task.
	 *
	 * @param report
	 *            Report text.
	 * @param sysInfo
	 *            True if system information should be added to the report.
	 * @param eventLog
	 *            True if event log should be added to the report.
	 * @param panel
	 *            Bug report panel.
	 */
	public SubmitBugReport(String report, boolean sysInfo, boolean eventLog, BugReportPanel panel) {
		report_ = report;
		sysInfo_ = sysInfo;
		eventLog_ = eventLog;
		panel_ = panel;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public String getTaskTitle() {
		return "Sumbit bug report to database";
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
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.SUBMIT_BUG_REPORT);

		// update progress info
		updateTitle("Sumbitting bug report to database server");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		// trim report text (if necessary)
		if (report_.length() > BugReport.MAX_REPORT_SIZE) {
			report_ = report_.substring(0, 900) + " and more...(rest is truncated due to maximum character limit).";
		}

		// get system information
		updateMessage("Collecting system information...");
		String sysInfo = sysInfo_ ? getSystemInformation() : null;
		if (sysInfo != null && sysInfo.length() > BugReport.MAX_SYS_INFO_SIZE) {
			sysInfo = sysInfo.substring(0, 400) + " and more...(rest is truncated due to maximum character limit).";
		}

		// get event log
		updateMessage("Reading event log...");
		String eventLog = eventLog_ ? getEventLog() : null;
		if (eventLog != null && eventLog.length() > BugReport.MAX_LOG_SIZE) {
			eventLog = eventLog.substring(0, 400) + " and more...(rest is truncated due to maximum character limit).";
		}

		try {

			// create request message
			SubmitBugReportRequest request = new SubmitBugReportRequest();
			request.setListenerHashCode(hashCode());
			request.setReport(report_);
			request.setEventLog(eventLog);
			request.setSystemInfo(sysInfo);

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
			else if (message instanceof SubmitBugReportResponse)
				return ((SubmitBugReportResponse) message).isReportSubmitted();

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

		// set chart data
		try {

			// submitted
			if (get()) {

				// notify panel
				panel_.reportSubmitted();

				// show info
				String title = "Report saved";
				String message = "Thank you very much. Your report has been successfully saved to bugs database.";
				taskPanel_.getOwner().getOwner().getNotificationPane().showOk(title, message);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Collects and returns system information.
	 *
	 * @return System information.
	 */
	private static String getSystemInformation() {

		// get system information
		try {
			String info = "os.name = " + System.getProperty("os.name") + "\n";
			info += "os.version = " + System.getProperty("os.version") + "\n";
			info += "os.arch = " + System.getProperty("os.arch") + "\n";
			info += "java.vm.name = " + System.getProperty("java.vm.name") + "\n";
			info += "java.vm.vendor = " + System.getProperty("java.vm.vendor") + "\n";
			info += "java.vm.version = " + System.getProperty("java.vm.version") + "\n";
			info += "java.runtime.name = " + System.getProperty("java.runtime.name") + "\n";
			info += "java.runtime.version = " + System.getProperty("java.runtime.version") + "\n";
			int mb = 1024 * 1024;
			Runtime runtime = Runtime.getRuntime();
			info += "used memory = " + (runtime.totalMemory() - runtime.freeMemory()) / mb + "\n";
			info += "free memory = " + runtime.freeMemory() / mb + "\n";
			info += "total memory = " + runtime.totalMemory() / mb + "\n";
			info += "max memory = " + runtime.maxMemory() / mb;
			return info;
		}

		// exception occurred while getting system information
		catch (SecurityException e) {
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during retreiving system information for bug report.", e);
			return null;
		}
	}

	/**
	 * Reads and returns the contents of the event log file.
	 *
	 * @return Contents of the event log file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String getEventLog() throws Exception {

		try {

			// initialize log string
			String log = "";

			// open log file to read
			try (BufferedReader reader = Files.newBufferedReader(Equinox.LOG_FILE, Charset.defaultCharset())) {

				// read log file
				String line;
				while ((line = reader.readLine()) != null) {

					// maximum character size reached
					if (log.length() + line.length() >= BugReport.MAX_LOG_SIZE - 70) {
						log += "and more...(rest is truncated due to maximum character limit).";
						break;
					}

					// append line
					log += line + "\n";
				}
			}

			// return log string
			return log;
		}

		// exception occurred while reading event log
		catch (SecurityException e) {
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during reading event log for bug report.", e);
			return null;
		}
	}
}
