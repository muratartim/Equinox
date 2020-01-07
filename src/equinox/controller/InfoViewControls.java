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
import equinox.data.fileType.SpectrumItem;
import equinox.data.ui.TableItem;
import equinox.task.ShowOutputFile;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Class for info view controls panel controller.
 *
 * @author Murat Artim
 * @date Feb 24, 2015
 * @time 9:30:51 AM
 */
public class InfoViewControls implements Initializable {

	/** The owner panel. */
	private InfoViewPanel owner_;

	@FXML
	private HBox root_;

	@FXML
	private ToggleSwitch expandAll_;

	@FXML
	private Button outputFile_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set root to grow always
		HBox.setHgrow(root_, Priority.ALWAYS);

		// add listeners to expand
		expandAll_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

				// get current tree table view
				TreeTableView<TableItem> view = owner_.getViewIndex() == InfoViewPanel.INFO_VIEW ? owner_.getInfoTable() : owner_.getSTFView().getInfoTable();

				// expand nodes
				for (TreeItem<TableItem> item : view.getRoot().getChildren()) {
					expandNodes(newValue, item);
				}
			}
		});
	}

	/**
	 * Returns the owner panel of this sub panel.
	 *
	 * @return The owner panel of this sub panel.
	 */
	public InfoViewPanel getOwner() {
		return owner_;
	}

	/**
	 * Returns the root of this controller.
	 *
	 * @return The root of this controller.
	 */
	public HBox getRoot() {
		return root_;
	}

	/**
	 * Shows or hides the output file button.
	 *
	 * @param show
	 *            True to show the output file button.
	 */
	public void showOutputFileButton(boolean show) {
		if (!show && root_.getChildren().contains(outputFile_)) {
			root_.getChildren().remove(outputFile_);
		}
		else if (show && !root_.getChildren().contains(outputFile_)) {
			root_.getChildren().add(outputFile_);
		}
	}

	@FXML
	private void onOutputFileClicked() {
		SpectrumItem item = (SpectrumItem) owner_.getOwner().getOwner().getInputPanel().getSelectedFiles().get(0);
		owner_.getOwner().getOwner().getActiveTasksPanel().runTaskSilently(new ShowOutputFile(item), false);
	}

	/**
	 * Expands all nodes of given node.
	 *
	 * @param expand
	 *            True to expand.
	 * @param item
	 *            Root node.
	 */
	private static void expandNodes(boolean expand, TreeItem<TableItem> item) {
		item.setExpanded(expand);
		if (!item.isLeaf()) {
			for (TreeItem<TableItem> child : item.getChildren()) {
				expandNodes(expand, child);
			}
		}
	}

	/**
	 * Sets up listener for info view table.
	 *
	 * @param controller
	 *            Controller.
	 */
	private static void setupInfoTableListener(InfoViewControls controller) {

		// set listeners
		controller.owner_.getInfoTable().getRoot().getChildren().addListener(new ListChangeListener<TreeItem<TableItem>>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends TreeItem<TableItem>> c) {
				for (TreeItem<TableItem> item : controller.owner_.getInfoTable().getRoot().getChildren()) {
					expandNodes(controller.expandAll_.isSelected(), item);
				}
			}
		});
		controller.owner_.getSTFView().getInfoTable().getRoot().getChildren().addListener(new ListChangeListener<TreeItem<TableItem>>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends TreeItem<TableItem>> c) {
				for (TreeItem<TableItem> item : controller.owner_.getSTFView().getInfoTable().getRoot().getChildren()) {
					expandNodes(controller.expandAll_.isSelected(), item);
				}
			}
		});
	}

	/**
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot text view panel.
	 */
	public static InfoViewControls load(InfoViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("InfoViewControls.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			InfoViewControls controller = (InfoViewControls) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;

			// setup listener for info table
			setupInfoTableListener(controller);

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
