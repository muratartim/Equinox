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

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.Equinox;
import equinox.data.EquinoxTheme;
import equinox.data.ui.SavedTaskItem;
import equinox.font.IconicFont;
import equinox.serverUtilities.ServerUtility;
import equinox.task.DeleteSavedTasks;
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
 * Class for saved tasks panel controller.
 *
 * @author Murat Artim
 * @date Oct 29, 2015
 * @time 10:45:34 AM
 */
public class SavedTasksPanel implements Initializable {

	/** The main screen. */
	private MainScreen owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	@FXML
	private VBox root_;

	@FXML
	private Label noSavedTaskLabel_;

	@FXML
	private Button deleteSavedTasks_;

	@FXML
	private SplitMenuButton runSavedTasks_;

	@FXML
	private ListView<SavedTaskItem> savedTasksList_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set multiple selection
		savedTasksList_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// bind components
		deleteSavedTasks_.disableProperty().bind(savedTasksList_.getSelectionModel().selectedItemProperty().isNull());
		runSavedTasks_.disableProperty().bind(savedTasksList_.getSelectionModel().selectedItemProperty().isNull());

		// bind no tasks label
		savedTasksList_.getItems().addListener((ListChangeListener<SavedTaskItem>) c -> noSavedTaskLabel_.setVisible(savedTasksList_.getItems().isEmpty()));

		// windows OS
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			runSavedTasks_.setPrefWidth(50);
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
	 * Returns saved tasks list.
	 *
	 * @return Saved tasks list.
	 */
	public ListView<SavedTaskItem> getSavedTasks() {
		return savedTasksList_;
	}

	@FXML
	private void onDeleteSavedTasksClicked() {

		// no tasks selected
		if (savedTasksList_.getSelectionModel().isEmpty())
			return;

		// delete selected tasks
		owner_.getActiveTasksPanel().runTaskInParallel(new DeleteSavedTasks(savedTasksList_.getSelectionModel().getSelectedItems(), false));
	}

	@FXML
	private void onRunClicked() {

		// no tasks selected
		if (savedTasksList_.getSelectionModel().isEmpty())
			return;

		// get selected tasks
		ObservableList<SavedTaskItem> selected = savedTasksList_.getSelectionModel().getSelectedItems();

		// run in parallel
		RunSavedTasks run = new RunSavedTasks(selected, false, false);
		DeleteSavedTasks delete = new DeleteSavedTasks(selected, false);
		owner_.getActiveTasksPanel().runTasksSequentially(run, delete);
	}

	@FXML
	private void onRunParallelClicked() {

		// no tasks selected
		if (savedTasksList_.getSelectionModel().isEmpty())
			return;

		// get selected tasks
		ObservableList<SavedTaskItem> selected = savedTasksList_.getSelectionModel().getSelectedItems();

		// create and run tasks
		RunSavedTasks run = new RunSavedTasks(selected, false, false);
		DeleteSavedTasks delete = new DeleteSavedTasks(selected, false);
		owner_.getActiveTasksPanel().runTasksSequentially(run, delete);
	}

	@FXML
	private void onRunSequentiallyClicked() {

		// no tasks selected
		if (savedTasksList_.getSelectionModel().isEmpty())
			return;

		// get selected tasks
		ObservableList<SavedTaskItem> selected = savedTasksList_.getSelectionModel().getSelectedItems();

		// create and run tasks
		RunSavedTasks run = new RunSavedTasks(selected, false, true);
		DeleteSavedTasks delete = new DeleteSavedTasks(selected, false);
		owner_.getActiveTasksPanel().runTasksSequentially(run, delete);
	}

	/**
	 * Loads and returns progress panel.
	 *
	 * @param owner
	 *            Main screen.
	 * @return The newly loaded progress panel.
	 */
	public static SavedTasksPanel load(MainScreen owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("SavedTasksPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			SavedTasksPanel controller = (SavedTasksPanel) fxmlLoader.getController();

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
