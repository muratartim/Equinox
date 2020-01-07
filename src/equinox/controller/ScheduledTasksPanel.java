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
import java.util.Date;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.Equinox;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.EquinoxTheme;
import equinox.data.ui.SavedTaskItem;
import equinox.font.IconicFont;
import equinox.serverUtilities.ServerUtility;
import equinox.task.DeleteSavedTasks;
import equinox.task.RescheduleSavedTasks;
import equinox.task.RunSavedTasks;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.layout.VBox;

/**
 * Class for scheduled tasks panel controller.
 *
 * @author Murat Artim
 * @date Oct 29, 2015
 * @time 11:21:01 AM
 */
public class ScheduledTasksPanel implements Initializable, SchedulingPanel {

	/** The main screen. */
	private MainScreen owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	@FXML
	private VBox root_;

	@FXML
	private Label noScheduledTaskLabel_;

	@FXML
	private Button deleteScheduledTasks_, rescheduleTasks_;

	@FXML
	private SplitMenuButton runScheduledTasks_;

	@FXML
	private ListView<SavedTaskItem> scheduledTasksList_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set multiple selection
		scheduledTasksList_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// bind components
		deleteScheduledTasks_.disableProperty().bind(scheduledTasksList_.getSelectionModel().selectedItemProperty().isNull());
		rescheduleTasks_.disableProperty().bind(scheduledTasksList_.getSelectionModel().selectedItemProperty().isNull());
		runScheduledTasks_.disableProperty().bind(scheduledTasksList_.getSelectionModel().selectedItemProperty().isNull());

		// bind no tasks label
		scheduledTasksList_.getItems().addListener((ListChangeListener<SavedTaskItem>) c -> noScheduledTaskLabel_.setVisible(scheduledTasksList_.getItems().isEmpty()));

		// windows OS
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			runScheduledTasks_.setPrefWidth(50);
		}
	}

	/**
	 * Returns the root of this controller.
	 *
	 * @return The root of this controller.
	 */
	public VBox getRoot() {
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
	 * Returns true if task manager is shown.
	 *
	 * @return True if task manager is shown.
	 */
	public boolean isShown() {
		return isShown_;
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

		// set showing handler
		popOver_.setOnShowing(event -> isShown_ = true);

		// set hidden handler
		popOver_.setOnHidden(event -> isShown_ = false);

		// show
		popOver_.show(node);
	}

	/**
	 * Returns scheduled tasks list.
	 *
	 * @return Scheduled tasks list.
	 */
	public ListView<SavedTaskItem> getScheduledTasks() {
		return scheduledTasksList_;
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// no tasks selected
		if (scheduledTasksList_.getSelectionModel().isEmpty())
			return;

		// re-schedule selected tasks
		owner_.getActiveTasksPanel().runTaskInParallel(new RescheduleSavedTasks(scheduledTasksList_.getSelectionModel().getSelectedItems(), scheduleDate));
	}

	@FXML
	private void onDeleteScheduledTasksClicked() {

		// no tasks selected
		if (scheduledTasksList_.getSelectionModel().isEmpty())
			return;

		// delete selected tasks
		owner_.getActiveTasksPanel().runTaskInParallel(new DeleteSavedTasks(scheduledTasksList_.getSelectionModel().getSelectedItems(), true));
	}

	@FXML
	private void onRescheduleTasksClicked() {

		// no tasks selected
		if (scheduledTasksList_.getSelectionModel().isEmpty())
			return;

		// get selected items
		ObservableList<SavedTaskItem> selected = scheduledTasksList_.getSelectionModel().getSelectedItems();

		// set initial date
		Date initialDate = null;
		if (selected.size() == 1) {
			SavedTaskItem item = selected.get(0);
			initialDate = item.getDate();
		}

		// show schedule panel
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
		popOver.setDetachable(false);
		popOver.setContentNode(ScheduleTaskPanel.load(popOver, this, initialDate));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(rescheduleTasks_);
	}

	@FXML
	private void onRunScheduledTasksClicked() {

		// no tasks selected
		if (scheduledTasksList_.getSelectionModel().isEmpty())
			return;

		// get selected tasks
		ObservableList<SavedTaskItem> selected = scheduledTasksList_.getSelectionModel().getSelectedItems();

		// create and run tasks
		RunSavedTasks run = new RunSavedTasks(selected, true, false);
		DeleteSavedTasks delete = new DeleteSavedTasks(selected, true);
		owner_.getActiveTasksPanel().runTasksSequentially(run, delete);
	}

	@FXML
	private void onRunScheduledTasksSequentiallyClicked() {

		// no tasks selected
		if (scheduledTasksList_.getSelectionModel().isEmpty())
			return;

		// get selected tasks
		ObservableList<SavedTaskItem> selected = scheduledTasksList_.getSelectionModel().getSelectedItems();

		// create and run tasks
		RunSavedTasks run = new RunSavedTasks(selected, true, true);
		DeleteSavedTasks delete = new DeleteSavedTasks(selected, true);
		owner_.getActiveTasksPanel().runTasksSequentially(run, delete);
	}

	/**
	 * Loads and returns progress panel.
	 *
	 * @param owner
	 *            Main screen.
	 * @return The newly loaded progress panel.
	 */
	public static ScheduledTasksPanel load(MainScreen owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ScheduledTasksPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			ScheduledTasksPanel controller = (ScheduledTasksPanel) fxmlLoader.getController();

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
