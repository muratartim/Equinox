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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.DownloadViewPanel.DownloadItemPanel;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.utility.Utility;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;

/**
 * Class for download view controls panel.
 *
 * @author Murat Artim
 * @date May 6, 2014
 * @time 3:59:22 PM
 */
public class DownloadViewControls implements Initializable {

	/** The owner panel. */
	private DownloadViewPanel owner_;

	@FXML
	private HBox root_;

	@FXML
	private Button download_, add_, settings_, selectAll_, unselectAll_, delete_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set root to grow always
		HBox.setHgrow(root_, Priority.ALWAYS);
	}

	/**
	 * Returns the owner panel of this sub panel.
	 *
	 * @return The owner panel of this sub panel.
	 */
	public DownloadViewPanel getOwner() {
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
	 * Returns download button.
	 *
	 * @return Download button.
	 */
	public Button getDownloadButton() {
		return download_;
	}

	/**
	 * Returns add button.
	 *
	 * @return Add button.
	 */
	public Button getAddButton() {
		return add_;
	}

	/**
	 * Returns settings button.
	 *
	 * @return Settings button.
	 */
	public Button getSettingsButton() {
		return settings_;
	}

	/**
	 * Returns select all button.
	 *
	 * @return Select all button.
	 */
	public Button getSelectAllButton() {
		return selectAll_;
	}

	/**
	 * Returns unselect all button.
	 *
	 * @return Unselect all button.
	 */
	public Button getUnselectAllButton() {
		return unselectAll_;
	}

	/**
	 * Returns delete button.
	 *
	 * @return Delete button.
	 */
	public Button getDeleteButton() {
		return delete_;
	}

	@FXML
	private void onSelectAllClicked() {
		owner_.getDownloadItems().forEach(item -> {
			if (!item.getSelectSwitch().isSelected()) {
				item.getSelectSwitch().setSelected(true);
			}
		});
	}

	@FXML
	private void onUnselectAllClicked() {
		owner_.getDownloadItems().forEach(item -> {
			if (item.getSelectSwitch().isSelected()) {
				item.getSelectSwitch().setSelected(false);
			}
		});
	}

	@FXML
	private void onDownloadClicked() {

		// get directory chooser
		DirectoryChooser dirChooser = owner_.getOwner().getOwner().getDirectoryChooser();

		// show save dialog
		File selectedDir = dirChooser.showDialog(owner_.getOwner().getOwner().getOwner().getStage());

		// no directory selected
		if (selectedDir == null)
			return;

		// set initial directory
		owner_.getOwner().getOwner().setInitialDirectory(selectedDir);

		// get task manager
		Path downloadDir = selectedDir.toPath();

		// loop over items
		ArrayList<DownloadItemPanel> items = owner_.getDownloadItems();
		for (int i = 0; i < items.size(); i++) {

			// get item
			DownloadItemPanel item = items.get(i);

			// not selected
			if (!item.getSelectSwitch().isSelected()) {
				continue;
			}

			// download
			item.download(downloadDir, i);
		}
	}

	@FXML
	private void onAddClicked() {

		// get download items
		ArrayList<DownloadItemPanel> items = owner_.getDownloadItems();

		// no item
		if (items.isEmpty())
			return;

		// pilot points
		if (items.get(0) instanceof DownloadPilotPointPanel) {

			// get pilot point info
			ArrayList<PilotPointInfo> info = new ArrayList<>();
			items.forEach(item -> {
				if (item.getSelectSwitch().isSelected()) {
					info.add((PilotPointInfo) item.getInfo());
				}
			});

			// show add STF popup
			final PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
			popOver.setDetachable(false);
			popOver.setContentNode(AddSTFPanel.load(info, getOwner().getOwner().getOwner(), popOver));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(add_);
		}

		// other items
		else {
			items.forEach(item -> {
				if (item.getSelectSwitch().isSelected()) {
					item.add();
				}
			});
		}
	}

	@FXML
	private void onSettingsClicked() {
		owner_.getOwner().getOwner().getInputPanel().showSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
	}

	@FXML
	private void onDeleteClicked() {

		// create confirmation popover
		PopOver popOver = new PopOver();

		// create confirmation action
		EventHandler<ActionEvent> handler = event -> {

			// delete
			owner_.getDownloadItems().forEach(item -> {
				if (item.getSelectSwitch().isSelected()) {
					item.delete();
				}
			});

			// hide pop-over
			popOver.hide();
		};

		// show message
		String message = "Are you sure you want to delete selected items from AF-Twin central database?";
		popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel2.load(popOver, message, 50, "Delete", handler, NotificationPanel2.QUESTION));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);

		// show popup
		popOver.show(delete_);
	}

	/**
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot text view panel.
	 */
	public static DownloadViewControls load(DownloadViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadViewControls.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			DownloadViewControls controller = (DownloadViewControls) fxmlLoader.getController();

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
