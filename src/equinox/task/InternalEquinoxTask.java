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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.jcraft.jsch.JSchException;

import equinox.Equinox;
import equinox.controller.TaskPanel;
import equinox.data.ExecutionMode;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.exchangeServer.remote.message.InstructionSetRunRequest;
import equinox.exchangeServer.remote.message.ShareFile;
import equinox.plugin.EquinoxTask;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.SharedFileInfo;
import equinox.serverUtilities.SharedFileInfo.SharedFileInfoType;
import equinox.utility.Utility;

/**
 * Abstract class for internal Equinox tasks.
 *
 * @author Murat Artim
 * @param <V>
 *            Task output class.
 * @date Dec 12, 2013
 * @time 4:09:17 PM
 */
public abstract class InternalEquinoxTask<V> extends EquinoxTask<V> {

	/** Progress panel of this task. */
	protected TaskPanel taskPanel_;

	/** Warning messages of the task. */
	protected String warnings_ = "";

	/** Logger of the task. */
	protected Logger logger_;

	/** Follower tasks. */
	private HashMap<InternalEquinoxTask<?>, ExecutionMode> followerTasks_ = null;

	/**
	 * Sets owner panel to this task.
	 *
	 * @param taskPanel
	 *            Task panel.
	 */
	public void setOwner(TaskPanel taskPanel) {
		taskPanel_ = taskPanel;
		updateTitle(getTaskTitle());
		updateMessage("Queued for execution...");
	}

	/**
	 * Adds follower task.
	 *
	 * @param task
	 *            Task to add.
	 * @param mode
	 *            Execution mode.
	 */
	public void addFollowerTask(InternalEquinoxTask<?> task, ExecutionMode mode) {
		if (followerTasks_ == null) {
			followerTasks_ = new HashMap<>();
		}
		followerTasks_.put(task, mode);
	}

	/**
	 * Returns a list containing the follower tasks or null if no follower tasks are defined.
	 *
	 * @return List containing the follower tasks or null if no follower tasks are defined.
	 */
	public HashMap<InternalEquinoxTask<?>, ExecutionMode> getFollowerTasks() {
		return followerTasks_;
	}

	/**
	 * Returns the task panel of this task.
	 *
	 * @return The task panel of this task.
	 */
	public TaskPanel getTaskPanel() {
		return taskPanel_;
	}

	/**
	 * Returns warning messages of the task, or empty string if there is no warning.
	 *
	 * @return Warning messages of the task, or empty string if there is no warning.
	 */
	public String getWarnings() {
		return warnings_;
	}

	/**
	 * Returns true if this task can be cancelled.
	 *
	 * @return True if this task can be cancelled.
	 */
	public abstract boolean canBeCancelled();

	/**
	 * Returns the task title. This is used to set to task panel before the task is executed.
	 *
	 * @return The task title.
	 */
	public abstract String getTaskTitle();

	/**
	 * Creates and returns connection to SFTP filer server. Note that, the supplied session, channel and sftpChannel objects must be disconnected after usage.
	 *
	 * @return Newly created filer connection.
	 * @throws JSchException
	 *             If exception occurs during process.
	 */
	public FilerConnection getFilerConnection() throws JSchException {
		return Utility.createFilerConnection(taskPanel_.getOwner().getOwner().getSettings());
	}

	/**
	 * Adds given warning to warnings.
	 *
	 * @param warning
	 *            Warning message to add.
	 */
	public void addWarning(String warning) {

		// append warning
		warnings_ += warning + "\n";

		// log to task logger (if any)
		if (logger_ != null) {
			logger_.log(Level.WARNING, warning);
		}
	}

	/**
	 * Adds given warning message with the related exception message. The exception message will be logged as well.
	 *
	 * @param warning
	 *            Warning message to add.
	 * @param e
	 *            Exception to add.
	 */
	public void addWarning(String warning, Throwable e) {
		warnings_ += warning + "\n";
		warnings_ += e.getMessage() + "\n";
		for (StackTraceElement ste : e.getStackTrace()) {
			warnings_ += ste.toString() + "\n";
		}
		Equinox.LOGGER.log(Level.WARNING, warning, e);

		// log to task logger (if any)
		if (logger_ != null) {
			logger_.log(Level.WARNING, warning, e);
		}
	}

	@Override
	public String toString() {
		return getTaskTitle();
	}

	@Override
	public void updateMessage(String message) {
		super.updateMessage(message);

		// log to task logger (if any)
		if (logger_ != null) {
			logger_.log(Level.FINE, message);
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// update info
		updateProgress(100, 100);
		updateMessage("Complete.");

		// close task logger (if any)
		if (logger_ != null) {
			logger_.log(Level.INFO, getClass().getSimpleName() + " is successfully completed.");
			Arrays.stream(logger_.getHandlers()).forEach(h -> h.close());
		}

		// execute follower tasks
		if (followerTasks_ != null) {
			executeFollowerTasks();
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// log exception
		Equinox.LOGGER.log(Level.WARNING, getClass().getSimpleName() + " has failed.", getException());

		// update info
		updateProgress(0, 100);
		updateMessage("Failed.");

		// close task logger (if any)
		if (logger_ != null) {
			logger_.log(Level.SEVERE, getClass().getSimpleName() + " has failed.", getException());
			Arrays.stream(logger_.getHandlers()).forEach(h -> h.close());
		}
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// update info
		updateProgress(0, 100);
		updateMessage("Canceled.");

		// close task logger (if any)
		if (logger_ != null) {
			logger_.log(Level.WARNING, getClass().getSimpleName() + " has been cancelled.");
			Arrays.stream(logger_.getHandlers()).forEach(h -> h.close());
		}
	}

	/**
	 * Handles result retrieval exceptions. This method should be called within the catch block of <code>succeeded</code> method.
	 *
	 * @param e
	 *            Exception to handle.
	 */
	protected void handleResultRetrievalException(Exception e) {

		// create error message
		String message = "Exception occurred during retrieving results of '" + getTaskTitle() + "': ";

		// log exception
		Equinox.LOGGER.log(Level.WARNING, message, e);

		// show error message
		message += e.getLocalizedMessage();
		message += " Click 'Details' for more information.";
		taskPanel_.getOwner().getOwner().getNotificationPane().showError("Problem encountered", message, e);
	}

	/**
	 * Creates a new task logger.
	 *
	 * @param name
	 *            Name of the newly created logger. Note that the name must unique within the JVM execution.
	 * @param logFile
	 *            Path to log file.
	 * @param level
	 *            Log level. Note that, if the log level is off, no logger will be created.
	 * @throws Exception
	 *             If exception occurs during creating task logger.
	 */
	public void createLogger(String name, Path logFile, Level level) throws Exception {

		// already has logger
		if (logger_ != null)
			return;

		// log level is off
		if (level.equals(Level.OFF))
			return;

		// create logger
		logger_ = Logger.getLogger(name);

		// create file handler
		FileHandler fileHandler = new FileHandler(logFile.toString());

		// set simple formatter to file handler
		fileHandler.setFormatter(new SimpleFormatter());

		// add handler to logger
		logger_.addHandler(fileHandler);

		// set log level
		logger_.setLevel(level);
	}

	/**
	 * Executes follower tasks (if any). This method is called from <code>succeeded</code> method of this task.
	 */
	private void executeFollowerTasks() {

		// there are no follower tasks
		if (followerTasks_ == null)
			return;

		// loop over follower tasks
		Iterator<Entry<InternalEquinoxTask<?>, ExecutionMode>> iterator = followerTasks_.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<InternalEquinoxTask<?>, ExecutionMode> entry = iterator.next();
			ExecutionMode mode = entry.getValue();
			if (mode.equals(ExecutionMode.NO_RUN)) {
				continue;
			}
			taskPanel_.getOwner().runTaskSilently(entry.getKey(), mode.equals(ExecutionMode.SEQUENTIAL));
		}
	}

	/**
	 * Interface for long running tasks.
	 *
	 * @author Murat Artim
	 * @date Nov 10, 2014
	 * @time 5:11:13 PM
	 */
	public interface LongRunningTask {
		// no implementation
	}

	/**
	 * Interface for short running tasks.
	 *
	 * @author Murat Artim
	 * @date Nov 5, 2015
	 * @time 9:18:19 PM
	 */
	public interface ShortRunningTask {
		// no implementation
	}

	/**
	 * Interface for directory outputting tasks.
	 *
	 * @author Murat Artim
	 * @date Mar 16, 2015
	 * @time 10:58:59 AM
	 */
	public interface DirectoryOutputtingTask {

		/**
		 * Returns output directory of the task.
		 *
		 * @return The output directory of the task.
		 */
		Path getOutputDirectory();

		/**
		 * Returns the output message of the task.
		 *
		 * @return The output message of the task.
		 */
		String getOutputMessage();

		/**
		 * Returns output button text.
		 *
		 * @return Output button text.
		 */
		String getOutputButtonText();
	}

	/**
	 * Interface for file sharing tasks.
	 *
	 * @author Murat Artim
	 * @date 14 Feb 2018
	 * @time 16:56:04
	 */
	public interface FileSharingTask {

		/**
		 * Uploads file with the given path to filer. Note that the destination file name is generated using the following convention:
		 * <p>
		 * userAlias_simpleTaskClassName_currentTimeMillis.zip
		 *
		 * @param path
		 *            Path to file to upload.
		 * @param recipients
		 *            List of recipient usernames.
		 * @param fileType
		 *            File type.
		 *
		 * @throws Exception
		 *             If exception occurs during process.
		 */
		default void shareFile(Path path, Collection<ExchangeUser> recipients, int fileType) throws Exception {

			// get task
			InternalEquinoxTask<?> task = (InternalEquinoxTask<?>) this;

			// update info
			task.updateMessage("Uploading file to filer...");
			String url = null;

			// get filer connection
			try (FilerConnection filer = task.getFilerConnection()) {

				// set path to destination file
				url = filer.getDirectoryPath(FilerConnection.EXCHANGE) + "/" + Equinox.USER.getAlias() + "_" + task.getClass().getSimpleName() + "_" + System.currentTimeMillis() + ".zip";

				// upload file to filer
				filer.getSftpChannel().put(path.toString(), url);
			}

			// update info
			task.updateMessage("Notifying recipients of shared file...");

			// create message
			ShareFile message = new ShareFile();
			SharedFileInfo info = new SharedFileInfo();
			info.setInfo(SharedFileInfoType.OWNER, Equinox.USER.getUsername());
			info.setInfo(SharedFileInfoType.FILE_TYPE, fileType);
			info.setInfo(SharedFileInfoType.FILE_NAME, path.getFileName().toString());
			info.setInfo(SharedFileInfoType.DATA_SIZE, path.toFile().length());
			info.setInfo(SharedFileInfoType.DATA_URL, url);
			message.setSharedFileInfo(info);

			// add recipients
			for (ExchangeUser recipient : recipients) {
				message.addRecipient(recipient);
			}

			// send message
			task.taskPanel_.getOwner().getOwner().getExchangeServerManager().sendMessage(message);
		}

		/**
		 * Uploads file with the given path to filer and send run instruction set request to given recipient. Note that the destination file name is generated using the following convention:
		 * <p>
		 * userAlias_simpleTaskClassName_currentTimeMillis.zip
		 *
		 * @param path
		 *            Path to file to upload.
		 * @param recipient
		 *            Recipient username.
		 * @throws Exception
		 *             If exception occurs during process.
		 */
		default void sendInstructionSetRunRequest(Path path, ExchangeUser recipient) throws Exception {

			// get task
			InternalEquinoxTask<?> task = (InternalEquinoxTask<?>) this;

			// update info
			task.updateMessage("Uploading instruction set file to filer...");
			String url = null;

			// get filer connection
			try (FilerConnection filer = task.getFilerConnection()) {

				// set path to destination file
				url = filer.getDirectoryPath(FilerConnection.EXCHANGE) + "/" + Equinox.USER.getAlias() + "_" + task.getClass().getSimpleName() + "_" + System.currentTimeMillis() + ".zip";

				// upload file to filer
				filer.getSftpChannel().put(path.toString(), url);
			}

			// update info
			task.updateMessage("Sending instruction set run request...");

			// create message
			InstructionSetRunRequest message = new InstructionSetRunRequest();
			SharedFileInfo info = new SharedFileInfo();
			info.setInfo(SharedFileInfoType.OWNER, Equinox.USER.getUsername());
			info.setInfo(SharedFileInfoType.FILE_TYPE, SharedFileInfo.INSTRUCTION_SET);
			info.setInfo(SharedFileInfoType.FILE_NAME, path.getFileName().toString());
			info.setInfo(SharedFileInfoType.DATA_SIZE, path.toFile().length());
			info.setInfo(SharedFileInfoType.DATA_URL, url);
			message.setSharedFileInfo(info);
			message.setSender(Equinox.USER.createExchangeUser());
			message.setRecipient(recipient);

			// send message
			task.taskPanel_.getOwner().getOwner().getExchangeServerManager().sendMessage(message);
		}
	}
}
