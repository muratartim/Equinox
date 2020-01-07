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

import equinox.Equinox;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.ui.HelpItem;
import equinox.data.ui.HelpListCell;
import equinox.data.ui.HelpTreeCell;
import equinox.task.AddHelpFiles;
import equinox.task.ReadHelpPages;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/**
 * Class for help panel.
 *
 * @author Murat Artim
 * @date May 9, 2014
 * @time 6:31:20 PM
 */
public class HelpPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Help document list. */
	private final ObservableList<TreeItem<String>> files_ = FXCollections.observableArrayList();

	@FXML
	private VBox root_;

	@FXML
	private TreeView<String> helpTree_;

	@FXML
	private ListView<TreeItem<String>> helpList_;

	@FXML
	private TextField search_;

	@FXML
	private Button cancel_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup help tree
		helpTree_.setRoot(new TreeItem<String>());
		helpTree_.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

		// add selection listener to help tree
		helpTree_.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<TreeItem<String>>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends TreeItem<String>> arg0) {
				helpTreeSelectionChanged();
			}
		});

		// add change listener to help tree
		helpTree_.getRoot().addEventHandler(TreeItem.childrenModificationEvent(), new EventHandler<TreeModificationEvent<Object>>() {

			@Override
			public void handle(TreeModificationEvent<Object> event) {
				helpTreeFilesChanged();
			}

		});

		// set cell factory to help tree
		helpTree_.setCellFactory(new Callback<TreeView<String>, TreeCell<String>>() {

			@Override
			public TreeCell<String> call(TreeView<String> p) {
				return new HelpTreeCell();
			}
		});

		// setup help list
		helpList_.setItems(files_);
		helpList_.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

		// add selection listener to help list
		helpList_.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<TreeItem<String>>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends TreeItem<String>> arg0) {
				helpListSelectionChanged();
			}
		});

		// set cell factory to help list
		helpList_.setCellFactory(new Callback<ListView<TreeItem<String>>, ListCell<TreeItem<String>>>() {

			@Override
			public ListCell<TreeItem<String>> call(ListView<TreeItem<String>> param) {
				return new HelpListCell();
			}
		});

		// set hand cursor to cancel button
		Utility.setHandCursor(cancel_);

		// setup search field
		search_.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> ov, String old_Val, String new_val) {

				// show file list
				if (!helpList_.isVisible()) {
					helpList_.setVisible(true);
					helpTree_.setVisible(false);
					helpTree_.setMouseTransparent(true);
				}

				// search
				search(old_Val, new_val);
			}
		});
	}

	@Override
	public void start() {
		TextField searchField = owner_.getOwner().getMenuBarPanel().getSearchField();
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new ReadHelpPages(helpTree_, searchField));
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public void showing() {
		// no implementation
	}

	@Override
	public String getHeader() {
		return "Help";
	}

	/**
	 * Returns help tree.
	 *
	 * @return Help tree.
	 */
	public TreeView<String> getHelpTree() {
		return helpTree_;
	}

	@FXML
	private void onCancelSearchClicked() {
		search_.clear();
		helpList_.setVisible(false);
		helpTree_.setVisible(true);
		helpTree_.setMouseTransparent(false);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Searches file list for given new value.
	 *
	 * @param old_Val
	 *            Old text value.
	 * @param new_Val
	 *            New text value.
	 */
	private void search(String old_Val, String new_Val) {
		helpList_.getSelectionModel().clearSelection();
		if ((old_Val != null) && (new_Val.length() < old_Val.length())) {
			helpList_.setItems(files_);
		}
		String value = new_Val.toUpperCase();
		ObservableList<TreeItem<String>> subentries = FXCollections.observableArrayList();
		for (TreeItem<String> item : helpList_.getItems()) {
			if (item.toString().toUpperCase().contains(value)) {
				subentries.add(item);
			}
		}
		helpList_.setItems(subentries);
		cancel_.setVisible(!new_Val.isEmpty());
	}

	/**
	 * Called when help list selection has changed.
	 */
	private void helpListSelectionChanged() {

		// no selection
		if (helpList_.getSelectionModel().isEmpty()) {
			helpList_.setContextMenu(null);
		}
		else {

			// set selected file
			HelpItem selectedFile = (HelpItem) helpList_.getSelectionModel().getSelectedItem();

			// show help
			owner_.getOwner().showHelp(selectedFile.getPage(), selectedFile.getLocation());
		}
	}

	/**
	 * Called when help tree selection has changed.
	 */
	private void helpTreeSelectionChanged() {

		// no selection
		if (helpTree_.getSelectionModel().isEmpty()) {
			helpTree_.setContextMenu(null);
		}
		else {

			// set selected file
			HelpItem selectedFile = (HelpItem) helpTree_.getSelectionModel().getSelectedItem();

			// show help
			owner_.getOwner().showHelp(selectedFile.getPage(), selectedFile.getLocation());
		}
	}

	/**
	 * Called when files in the help tree have changed.
	 *
	 */
	private void helpTreeFilesChanged() {

		// remove context menu if no files
		if (helpTree_.getRoot().getChildren().isEmpty()) {
			helpTree_.setContextMenu(null);
			helpList_.setContextMenu(null);
		}

		// clear file list
		helpList_.getSelectionModel().clearSelection();
		files_.clear();

		// add files to file list
		Equinox.SINGLE_THREADPOOL.submit(new AddHelpFiles(owner_.getOwner(), helpTree_.getRoot(), files_, helpList_, search_));
	}

	/**
	 * Loads and returns help panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded files panel.
	 */
	public static HelpPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("HelpPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			HelpPanel controller = (HelpPanel) fxmlLoader.getController();

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
