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

import equinox.data.EquinoxTheme;
import equinox.task.InternalEquinoxTask;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;

/**
 * Class for queued tasks panel controller.
 *
 * @author Murat Artim
 * @date Oct 29, 2015
 * @time 10:23:52 AM
 */
public class QueuedTasksPanel implements Initializable {

	/** The main screen. */
	private MainScreen owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	@FXML
	private VBox root_;

	@FXML
	private Label noQueuedTaskLabel_;

	@FXML
	private Button cancelQueuedTask_;

	@FXML
	private ListView<InternalEquinoxTask<?>> queuedTasksList_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set single selection
		queuedTasksList_.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

		// bind components
		cancelQueuedTask_.disableProperty().bind(queuedTasksList_.getSelectionModel().selectedItemProperty().isNull());

		// bind no tasks label
		queuedTasksList_.getItems().addListener(new ListChangeListener<InternalEquinoxTask<?>>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends InternalEquinoxTask<?>> c) {
				noQueuedTaskLabel_.setVisible(queuedTasksList_.getItems().isEmpty());
			}
		});
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
	 * Returns queued tasks.
	 *
	 * @return Queued tasks.
	 */
	public ListView<InternalEquinoxTask<?>> getQueuedTasks() {
		return queuedTasksList_;
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
		popOver_.setOnShowing(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				isShown_ = true;
			}
		});

		// set hidden handler
		popOver_.setOnHidden(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				isShown_ = false;
			}
		});

		// show
		popOver_.show(node);
	}

	@FXML
	private void onCancelQueuedTaskClicked() {
		InternalEquinoxTask<?> selected = queuedTasksList_.getSelectionModel().getSelectedItem();
		if (selected == null)
			return;
		selected.cancel();
	}

	/**
	 * Loads and returns progress panel.
	 *
	 * @param owner
	 *            Main screen.
	 * @return The newly loaded progress panel.
	 */
	public static QueuedTasksPanel load(MainScreen owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("QueuedTasksPanel.fxml"));
			fxmlLoader.load();

			// get controller
			QueuedTasksPanel controller = (QueuedTasksPanel) fxmlLoader.getController();

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
