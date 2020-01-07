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
import java.util.ResourceBundle;

import org.controlsfx.control.ToggleSwitch;

import equinox.data.EquinoxTheme;
import equinox.task.InternalEquinoxTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.PluginTask;
import equinox.task.automation.AutomaticTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.RunInstructionSet;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;

/**
 * Class for task panel controller.
 *
 * @author Murat Artim
 * @date Jul 22, 2014
 * @time 1:23:25 PM
 */
public class TaskPanel implements Initializable, ChangeListener<State> {

	/** The owner panel. */
	private ActiveTasksPanel owner_;

	/** The task of this panel. */
	private InternalEquinoxTask<?> task_;

	@FXML
	private HBox root_;

	@FXML
	private ProgressIndicator progressIndicator_;

	@FXML
	private Label title_, message_;

	@FXML
	private ToggleSwitch notify_;

	@FXML
	private Button cancel_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Returns owner panel.
	 *
	 * @return Owner panel.
	 */
	public ActiveTasksPanel getOwner() {
		return owner_;
	}

	/**
	 * Returns root node.
	 *
	 * @return Root node.
	 */
	public Parent getRoot() {
		return root_;
	}

	/**
	 * Returns task.
	 *
	 * @return Task.
	 */
	public InternalEquinoxTask<?> getTask() {
		return task_;
	}

	/**
	 * Updates cancel state.
	 *
	 * @param canBeCanceled
	 *            True if the task can be canceled.
	 */
	public void updateCancelState(boolean canBeCanceled) {

		// set button state
		Platform.runLater(() -> cancel_.setDisable(!canBeCanceled));
	}

	/**
	 * Returns true for task completion notification.
	 *
	 * @return True for task completion notification.
	 */
	public boolean shouldNotify() {
		return notify_.isSelected();
	}

	@Override
	public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
		owner_.taskStateChanged(task_, newValue);
	}

	@FXML
	private void onCancelClicked() {
		task_.cancel();
	}

	/**
	 * Loads and returns task panel.
	 *
	 * @param owner
	 *            The owner progress panel.
	 * @param task
	 *            Task of this panel.
	 * @return The newly loaded task panel.
	 */
	public static TaskPanel load(ActiveTasksPanel owner, InternalEquinoxTask<?> task) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("TaskPanel.fxml"));
			fxmlLoader.load();

			// get controller
			TaskPanel controller = (TaskPanel) fxmlLoader.getController();

			// set root and parent
			controller.owner_ = owner;

			// set controller to task
			task.setOwner(controller);

			// set task
			controller.task_ = task;

			// setup cancel button state
			controller.cancel_.setDisable(!controller.task_.canBeCancelled());

			// setup notification
			if (task instanceof AutomaticTask || task instanceof AutomaticTaskOwner || task instanceof RunInstructionSet) {
				controller.notify_.setSelected(false);
			}
			else {
				controller.notify_.setSelected(task instanceof LongRunningTask || task instanceof PluginTask && ((PluginTask) task).isLongRunning());
			}

			// bind UI to task
			controller.progressIndicator_.progressProperty().bind(controller.task_.progressProperty());
			controller.title_.textProperty().bind(controller.task_.titleProperty());
			controller.message_.textProperty().bind(controller.task_.messageProperty());
			controller.task_.stateProperty().addListener(controller);

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
