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
package equinox.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.Equinox;
import equinox.data.EquinoxTheme;
import equinox.data.ui.HistoryItem;
import equinox.task.InternalEquinoxTask;
import equinox.task.InternalEquinoxTask.DirectoryOutputtingTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.PluginTask;
import equinox.task.SaveTask;
import equinox.utility.exception.IgnoredFailureException;
import equinox.utility.exception.PermissionDeniedException;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Class for active tasks panel controller.
 *
 * @author Murat Artim
 * @date Oct 29, 2015
 * @time 9:56:32 AM
 */
public class ActiveTasksPanel implements Initializable {

	/** The main screen. */
	private MainScreen owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	/** Task panels. */
	private List<InternalEquinoxTask<?>> tasks_;

	/** Task result returned from a sequential task. */
	private Object taskResult_ = null;

	/** Number of running tasks. */
	private volatile int runningTasks_ = 0;

	@FXML
	private StackPane root_;

	@FXML
	private VBox taskContainer_;

	@FXML
	private Label noTaskLabel_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create synchronized list of tasks
		tasks_ = Collections.synchronizedList(new ArrayList<InternalEquinoxTask<?>>());

		// bind no tasks label
		taskContainer_.getChildren().addListener((ListChangeListener<Node>) c -> noTaskLabel_.setVisible(taskContainer_.getChildren().isEmpty()));
	}

	/**
	 * Returns the root of this controller.
	 *
	 * @return The root of this controller.
	 */
	public StackPane getRoot() {
		return root_;
	}

	/**
	 * Returns the main screen of the application.
	 *
	 * @return The main screen of the application.
	 */
	public MainScreen getOwner() {
		return owner_;
	}

	/**
	 * Cancels all running tasks.
	 *
	 */
	public void cancelAllTasks() {

		// synchronize on list
		synchronized (tasks_) {

			// get list iterator
			Iterator<InternalEquinoxTask<?>> iterator = tasks_.iterator();

			// loop over tasks
			while (iterator.hasNext()) {
				iterator.next().cancel();
			}
		}
	}

	/**
	 * Returns true if there is any running task.
	 *
	 * @return True if there is any running task.
	 */
	public boolean hasRunningTasks() {
		ThreadPoolExecutor tPool = (ThreadPoolExecutor) Equinox.FIXED_THREADPOOL;
		return tPool.getActiveCount() > 0;
	}

	/**
	 * Runs given task in parallel execution mode.
	 *
	 * @param task
	 *            Task to be executed in parallel mode.
	 * @return Returns a Future representing that task. The Future's get method will return null upon successful completion.
	 */
	public Future<?> runTaskInParallel(InternalEquinoxTask<?> task) {
		return submitTask(task, false, true);
	}

	/**
	 * Runs given task sequentially.
	 *
	 * @param task
	 *            Task to be executed.
	 * @return Returns a Future representing that task. The Future's get method will return null upon successful completion.
	 */
	public Future<?> runTaskSequentially(InternalEquinoxTask<?> task) {
		return submitTask(task, true, true);
	}

	/**
	 * Runs given tasks in sequential mode. Tasks are executed in the given order.
	 *
	 * @param tasks
	 *            Tasks to be executed in sequential mode.
	 */
	public void runTasksSequentially(InternalEquinoxTask<?>... tasks) {
		for (InternalEquinoxTask<?> task : tasks) {
			submitTask(task, true, true);
		}
	}

	/**
	 * Runs given task silently (i.e. no task submission notification will be shown). This method is useful to execute tasks directly from within other tasks (i.e. not from FX application thread).
	 *
	 * @param task
	 *            Task to be executed.
	 * @param isSequential
	 *            True if task should be executed within the sequential task queue.
	 * @return Returns a Future representing that task. The Future's get method will return null upon successful completion.
	 */
	public Future<?> runTaskSilently(InternalEquinoxTask<?> task, boolean isSequential) {
		return submitTask(task, isSequential, false);
	}

	/**
	 * Submits given task for execution.
	 *
	 * @param task
	 *            Task to be submitted.
	 * @param isSequential
	 *            True if task should be executed within the sequential task queue.
	 * @param showNotification
	 *            True if task submission notification should be shown (if applicable).
	 * @return Returns a Future representing that task. The Future's get method will return null upon successful completion.
	 */
	private Future<?> submitTask(InternalEquinoxTask<?> task, boolean isSequential, boolean showNotification) {

		// create task panel
		TaskPanel.load(this, task);

		// add to tasks list
		tasks_.add(task);

		// add task to queued list
		owner_.getQueuedTasksPanel().getQueuedTasks().getItems().add(task);

		// show notification
		if (showNotification) {

			// notify if task is sequentially run and queued
			boolean notificationShown = false;
			if (isSequential && runningTasks_ != 0) {
				String message = task.getTaskTitle() + " is queued for execution. You can see currently running tasks from Task Manager.";
				owner_.getNotificationPane().showQueued(message);
				notificationShown = true;
			}

			// notify long running task submission
			if (!notificationShown && (task instanceof LongRunningTask || task instanceof PluginTask && ((PluginTask) task).isLongRunning())) {

				// get number of active tasks
				ThreadPoolExecutor tPool = (ThreadPoolExecutor) Equinox.FIXED_THREADPOOL;
				boolean showSubmitted = tPool.getActiveCount() < tPool.getMaximumPoolSize();

				// show submitted
				if (showSubmitted) {
					String message = task.getTaskTitle() + " is running on background. You can see currently running tasks from Task Manager.";
					owner_.getNotificationPane().showSumbitted(message);
				}

				// show queued
				else {
					String message = task.getTaskTitle() + " is queued for execution. You can see currently running tasks from Task Manager.";
					owner_.getNotificationPane().showQueued(message);
				}

				// notification shown
				notificationShown = true;
			}

			// notify saved task notification
			else if (!notificationShown && task instanceof SaveTask) {

				// saved task
				if (!((SaveTask) task).isScheduled()) {
					String message = "Task is saved to execute later. You can access and execute saved tasks from Task Manager.";
					owner_.getNotificationPane().showSaved(message);
				}

				// scheduled task
				else {
					String message = "Task is scheduled for execution. You can access scheduled tasks from Task Manager.";
					owner_.getNotificationPane().showScheduled(message);
				}
			}
		}

		// sequential
		if (isSequential)
			return Equinox.SINGLE_THREADPOOL.submit(task);

		// short running task
		else if (task instanceof ShortRunningTask)
			return Equinox.CACHED_THREADPOOL.submit(task);

		// other
		else
			return Equinox.FIXED_THREADPOOL.submit(task);
	}

	/**
	 * Called from task panel when ever a task's state has changed.
	 *
	 * @param task
	 *            Task whose state has changed.
	 * @param state
	 *            New task state.
	 */
	public void taskStateChanged(InternalEquinoxTask<?> task, State state) {

		// update task summary
		updateTaskSummary();

		// update UI
		updateUI(task, state);
	}

	/**
	 * Sets the result of a task.
	 *
	 * @param result
	 *            Result to set.
	 */
	public void setResult(Object result) {
		taskResult_ = result;
	}

	/**
	 * Returns the result of last completed task.
	 *
	 * @return the result of last completed task.
	 */
	public Object getResult() {
		return taskResult_;
	}

	/**
	 * Returns true if task manager is shown.
	 *
	 * @return True if task manager is shown.
	 */
	public boolean isShown() {
		return isShown_;
	}

	/**
	 * Shows this panel on modal mode. Note that, this panel should be explicitly closed otherwise it will block indefinitely.
	 */
	public void showModal() {

		// already shown
		if (isShown_)
			return;

		// create pop-over
		popOver_ = new PopOver();
		popOver_.setHideOnEscape(false);
		popOver_.setAutoHide(false);
		popOver_.setContentNode(root_);
		popOver_.setStyle("-fx-base: #ececec;");
		popOver_.centerOnScreen();
		popOver_.setArrowSize(0.0);
		popOver_.setId("modal");

		// set showing handler
		popOver_.setOnShowing(event -> {
			owner_.addModalLayer("modalTask");
			isShown_ = true;
		});

		// set hidden handler
		popOver_.setOnHidden(event -> {
			owner_.removeModalLayer("modalTask");
			isShown_ = false;
		});

		// show
		popOver_.show(owner_.getOwner().getStage());
	}

	/**
	 * Shows this panel.
	 *
	 * @param node
	 *            Node to show the popup window.
	 */
	public void show(Node node) {

		// already shown
		if (isShown_)
			return;

		// create pop-over
		popOver_ = new PopOver();
		popOver_.setArrowLocation(ArrowLocation.TOP_RIGHT);
		popOver_.setDetachable(false);
		popOver_.setHideOnEscape(true);
		popOver_.setAutoHide(true);
		popOver_.setContentNode(root_);
		popOver_.setStyle("-fx-base: #ececec;");
		popOver_.setId("non-modal");

		// set showing handler
		popOver_.setOnShowing(event -> isShown_ = true);

		// set hidden handler
		popOver_.setOnHidden(event -> isShown_ = false);

		// show
		popOver_.show(node);
	}

	/**
	 * Hides this panel.
	 */
	public void hide() {
		popOver_.hide();
	}

	/**
	 * Updates task summary.
	 *
	 */
	private void updateTaskSummary() {

		// synchronize on list
		synchronized (tasks_) {

			// get list iterator
			Iterator<InternalEquinoxTask<?>> iterator = tasks_.iterator();

			// loop over tasks
			int running = 0;
			while (iterator.hasNext()) {

				// get task state
				State state = iterator.next().getState();

				// set counts
				if (state.equals(State.RUNNING)) {
					running++;
				}
			}

			// update summary properties
			runningTasks_ = running;
		}
	}

	/**
	 * Updates UI according to current task state.
	 *
	 * @param task
	 *            Task whose state has changed.
	 * @param state
	 *            New task state.
	 */
	private void updateUI(InternalEquinoxTask<?> task, State state) {

		// add to javafx event queue
		Platform.runLater(() -> {

			// task started running
			if (state.equals(State.RUNNING)) {

				// root element of task panel
				Parent root = task.getTaskPanel().getRoot();

				// not added yet
				if (!taskContainer_.getChildren().contains(root)) {

					// remove task from queued list
					owner_.getQueuedTasksPanel().getQueuedTasks().getItems().remove(task);

					// add root node to task container
					taskContainer_.getChildren().add(root);
				}
			}

			// update info image
			Image image = runningTasks_ == 0 ? MenuBarPanel.QUIET : MenuBarPanel.RUNNING;
			owner_.getMenuBarPanel().getNotificationNode().setImage(image);

			// succeeded, failed or canceled
			if (state.equals(State.SUCCEEDED) || state.equals(State.CANCELLED) || state.equals(State.FAILED)) {

				// remove task from queued list
				owner_.getQueuedTasksPanel().getQueuedTasks().getItems().remove(task);

				// remove task
				tasks_.remove(task);
				taskContainer_.getChildren().remove(task.getTaskPanel().getRoot());

				// ON hide popup if there are no active and queued tasks
				if (taskContainer_.getChildren().isEmpty() && owner_.getQueuedTasksPanel().getQueuedTasks().getItems().isEmpty() && popOver_ != null && popOver_.getId().equals("non-modal")) {
					popOver_.hide();
				}

				// add to history
				owner_.getTaskHistoryPanel().getTaskHistory().getItems().add(0, new HistoryItem(task.getTaskTitle(), task.getDuration(), state));
			}

			// task failed
			if (state.equals(State.FAILED)) {

				// get exception
				Throwable exception = task.getException();

				// ignored failure exception
				if (exception instanceof IgnoredFailureException)
					return;

				// permission denied exception
				else if (exception instanceof PermissionDeniedException) {
					owner_.getNotificationPane().showPermissionDenied(((PermissionDeniedException) exception).getPermission());
				}

				// other exception
				else {
					String title1 = "Task failed (in " + task.getDuration() + ")";
					String message1 = task.getTaskTitle() + " has failed due to an exception.";
					message1 += " Click 'Details' for more information.";
					owner_.getNotificationPane().showError(title1, message1, exception);
				}
			}

			// task succeeded
			else if (state.equals(State.SUCCEEDED)) {

				// has warnings
				if (!task.getWarnings().isEmpty()) {
					String title2 = "Task completed with warnings (in " + task.getDuration() + ")";
					String message2 = task.getTaskTitle() + " is completed with warnings.";
					message2 += " Click 'Details' for more information.";
					owner_.getNotificationPane().showCompletedWithWarnings(title2, message2, task.getWarnings());
				}

				// no warnings and should be notified
				else if (task.getTaskPanel().shouldNotify()) {

					// directory outputting task
					if (task instanceof DirectoryOutputtingTask) {
						DirectoryOutputtingTask dirOutputtingTask = (DirectoryOutputtingTask) task;
						String title3 = "Task completed (in " + task.getDuration() + ")";
						String message3 = dirOutputtingTask.getOutputMessage();
						String buttonText1 = dirOutputtingTask.getOutputButtonText();
						owner_.getNotificationPane().showDirectoryOutputtingOk(title3, message3, buttonText1, dirOutputtingTask.getOutputDirectory());
					}

					// directory outputting plugin task
					else if (task instanceof PluginTask && ((PluginTask) task).isDirectoryOutputting()) {
						PluginTask pluginTask = (PluginTask) task;
						String title4 = "Task completed (in " + task.getDuration() + ")";
						String message4 = task.getTaskTitle() + " is successfully completed. ";
						message4 += "Click 'Outputs' to see outputs of the task.";
						String buttonText2 = "Outputs";
						owner_.getNotificationPane().showDirectoryOutputtingOk(title4, message4, buttonText2, pluginTask.getOutputDirectory());
					}

					// standard task
					else {
						String title5 = "Task completed (in " + task.getDuration() + ")";
						String message5 = task.getTaskTitle() + " is successfully completed.";
						owner_.getNotificationPane().showOk(title5, message5);
					}
				}
			}
		});
	}

	/**
	 * Loads and returns progress panel.
	 *
	 * @param owner
	 *            Main screen.
	 * @return The newly loaded progress panel.
	 */
	public static ActiveTasksPanel load(MainScreen owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ActiveTasksPanel.fxml"));
			fxmlLoader.load();

			// get controller
			ActiveTasksPanel controller = (ActiveTasksPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
