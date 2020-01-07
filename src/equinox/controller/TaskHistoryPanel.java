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
import equinox.data.ui.HistoryItem;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

/**
 * Class for task history panel controller.
 *
 * @author Murat Artim
 * @date Oct 29, 2015
 * @time 11:31:43 AM
 */
public class TaskHistoryPanel implements Initializable {

	/** The main screen. */
	private MainScreen owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	@FXML
	private VBox root_;

	@FXML
	private ListView<HistoryItem> history_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set cell factory to history list
		history_.setCellFactory(new Callback<ListView<HistoryItem>, ListCell<HistoryItem>>() {

			@Override
			public ListCell<HistoryItem> call(ListView<HistoryItem> param) {
				return new HistoryCell();
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

	/**
	 * Returns task history.
	 *
	 * @return Task history.
	 */
	public ListView<HistoryItem> getTaskHistory() {
		return history_;
	}

	/**
	 * Loads and returns progress panel.
	 *
	 * @param owner
	 *            Main screen.
	 * @return The newly loaded progress panel.
	 */
	public static TaskHistoryPanel load(MainScreen owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("TaskHistoryPanel.fxml"));
			fxmlLoader.load();

			// get controller
			TaskHistoryPanel controller = (TaskHistoryPanel) fxmlLoader.getController();

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

	/**
	 * Inner class for customized history cell.
	 *
	 * @author Murat Artim
	 * @date Jan 20, 2014
	 * @time 1:32:11 PM
	 */
	private final class HistoryCell extends ListCell<HistoryItem> {

		@Override
		public void updateItem(HistoryItem item, boolean empty) {

			// update item
			super.updateItem(item, empty);

			// empty cell
			if (empty) {
				setText(null);
				setGraphic(null);
			}

			// valid cell
			else {
				setText(item.toString());
				setGraphic(item.getStatus());
			}
		}
	}
}
