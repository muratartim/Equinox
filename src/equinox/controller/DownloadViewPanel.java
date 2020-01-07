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
import java.util.logging.Level;

import org.controlsfx.control.ToggleSwitch;

import equinox.Equinox;
import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.DownloadInfo;
import equinox.dataServer.remote.data.HelpVideoInfo;
import equinox.dataServer.remote.data.MultiplicationTableInfo;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.data.SearchInput;
import equinox.dataServer.remote.data.SpectrumInfo;
import equinox.plugin.FileType;
import equinox.task.SaveImage;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for download view panel.
 *
 * @author Murat Artim
 * @date May 5, 2014
 * @time 11:46:10 AM
 */
public class DownloadViewPanel implements InternalViewSubPanel {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Controls. */
	private DownloadViewControls controls_;

	/** Download items. */
	private final ArrayList<DownloadItemPanel> items_ = new ArrayList<>();

	/** Panel header. */
	private String header_ = "Downloads View";

	/** No results panel. */
	private VBox noResultsPanel_;

	@FXML
	private VBox root_, container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create controls
		controls_ = DownloadViewControls.load(this);

		// create no results panel
		noResultsPanel_ = NoResultsPanel.load("Your search did not match any files.", null);
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {
		// no implementation
	}

	@Override
	public void hiding() {
		// no implementation
	}

	@Override
	public ViewPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public HBox getControls() {
		return controls_.getRoot();
	}

	@Override
	public String getHeader() {
		return header_;
	}

	@Override
	public boolean canSaveView() {
		return true;
	}

	@Override
	public void saveView() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName("Search Results" + FileType.PNG.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File file = FileType.appendExtension(selectedFile, FileType.PNG);

		// take snapshot of the scene
		WritableImage snapshot = container_.snapshot(null, null);

		// create and start task in progress panel
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SaveImage(file, snapshot));
	}

	@Override
	public String getViewName() {
		return header_;
	}

	@Override
	public WritableImage getViewImage() {
		return container_.snapshot(null, null);
	}

	/**
	 * Returns download items.
	 *
	 * @return Download items.
	 */
	public ArrayList<DownloadItemPanel> getDownloadItems() {
		return items_;
	}

	/**
	 * Updates download item with given download info from this panel.
	 *
	 * @param item
	 *            Download info.
	 */
	public void updateDownloadItem(DownloadInfo item) {

		try {

			// loop over download items
			int index1 = -1, index2 = -1;
			for (DownloadItemPanel itemPanel : items_) {

				// get existing info
				DownloadInfo existing = itemPanel.getInfo();

				// same class
				if (existing.getClass().getName().equals(item.getClass().getName())) {

					// same ID
					if (existing.getID() == item.getID()) {

						// get indices
						index1 = items_.indexOf(itemPanel);
						index2 = container_.getChildren().indexOf(itemPanel.getRoot());

						// remove from items and container
						items_.remove(itemPanel);
						container_.getChildren().remove(itemPanel.getRoot());

						// end
						break;
					}
				}
			}

			// create download panel
			DownloadItemPanel panel = null;
			if (item instanceof SpectrumInfo) {
				panel = DownloadSpectrumPanel.load(this);
			}
			else if (item instanceof PilotPointInfo) {
				panel = DownloadPilotPointPanel.load(this);
			}
			else if (item instanceof MultiplicationTableInfo) {
				panel = DownloadMultiplicationTablePanel.load(this);
			}
			else if (item instanceof HelpVideoInfo) {
				panel = DownloadHelpVideoPanel.load(this);
			}

			// add panel to view
			if (panel != null) {
				header_ = panel.getHeader();
				items_.add(index1, panel);
				container_.getChildren().add(index2, panel.getRoot());
				panel.setInfo(item);
				panel.start();
			}

			// setup controls
			setupControls();

			// no items
			if (items_.isEmpty()) {
				container_.getChildren().add(noResultsPanel_);
			}
		}

		// exception occurred
		catch (Exception e) {

			// log exception
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during setting download items.", e);

			// create and show notification
			String message = "Exception occurred during setting download items: " + e.getLocalizedMessage();
			message += " Click 'Details' for more information.";
			owner_.getOwner().getNotificationPane().showError("Problem encountered", message, e);
		}
	}

	/**
	 * Removes download item with given download info from this panel.
	 *
	 * @param item
	 *            Download info.
	 */
	public void removeDownloadItem(DownloadInfo item) {

		// loop over download items
		for (DownloadItemPanel itemPanel : items_) {

			// item found
			if (itemPanel.getInfo().equals(item)) {

				// remove from items and container
				items_.remove(itemPanel);
				container_.getChildren().remove(itemPanel.getRoot());

				// setup controls
				setupControls();

				// no items
				if (items_.isEmpty()) {
					container_.getChildren().add(noResultsPanel_);
				}

				// end
				break;
			}
		}
	}

	/**
	 * Sets search results to this panel.
	 *
	 * @param items
	 *            Download items to set.
	 * @param input
	 *            Search input.
	 */
	public void setDownloadItems(ArrayList<DownloadInfo> items, SearchInput input) {

		try {

			// clear container and items
			items_.clear();
			container_.getChildren().clear();

			// add results to container
			for (DownloadInfo item : items) {

				// create download panel
				DownloadItemPanel panel = null;
				if (item instanceof SpectrumInfo) {
					panel = DownloadSpectrumPanel.load(this);
				}
				else if (item instanceof PilotPointInfo) {
					panel = DownloadPilotPointPanel.load(this);
				}
				else if (item instanceof MultiplicationTableInfo) {
					panel = DownloadMultiplicationTablePanel.load(this);
				}
				else if (item instanceof HelpVideoInfo) {
					panel = DownloadHelpVideoPanel.load(this);
				}

				// add panel to view
				if (panel != null) {
					if (input == null) {
						header_ = panel.getHeader();
					}
					else {
						header_ = "Top " + input.getMaxHits() + " " + panel.getHeader() + " ordered by " + input.getOrderByCriteria() + " in " + (input.getOrder() ? "ascending" : "descending") + " order";
					}
					items_.add(panel);
					container_.getChildren().add(panel.getRoot());
					panel.setInfo(item);
					panel.start();
				}
			}

			// setup controls
			setupControls();

			// no items
			if (items.isEmpty()) {
				container_.getChildren().add(noResultsPanel_);
			}
		}

		// exception occurred
		catch (Exception e) {

			// log exception
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during setting download items.", e);

			// create and show notification
			String message = "Exception occurred during setting download items: " + e.getLocalizedMessage();
			message += " Click 'Details' for more information.";
			owner_.getOwner().getNotificationPane().showError("Problem encountered", message, e);
		}
	}

	/**
	 * Sets up control panel components.
	 *
	 */
	public void setupControls() {

		// get buttons
		Button download = controls_.getDownloadButton();
		Button add = controls_.getAddButton();
		controls_.getSelectAllButton().setDisable(items_.isEmpty());
		Button unselect = controls_.getUnselectAllButton();
		Button delete = controls_.getDeleteButton();

		// count selected items
		for (DownloadItemPanel item : items_) {
			ToggleSwitch selectSwitch = item.getSelectSwitch();
			if (selectSwitch != null) {
				if (selectSwitch.isSelected()) {
					download.setDisable(!item.canBeDownloaded());
					add.setDisable(!item.canBeAdded());
					unselect.setDisable(false);
					delete.setDisable(!item.canBeDeleted());
					return;
				}
			}
		}
		download.setDisable(true);
		add.setDisable(true);
		unselect.setDisable(true);
		delete.setDisable(true);
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static DownloadViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			DownloadViewPanel controller = (DownloadViewPanel) fxmlLoader.getController();

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
	 * Interface for download item panels.
	 *
	 * @author Murat Artim
	 * @date Feb 12, 2016
	 * @time 4:07:54 PM
	 */
	public interface DownloadItemPanel {

		/**
		 * Returns header of this panel.
		 *
		 * @return Header of this panel.
		 */
		String getHeader();

		/**
		 * Returns the root element of this panel.
		 *
		 * @return The root element of this panel.
		 */
		Parent getRoot();

		/**
		 * Returns download info.
		 *
		 * @return Download info.
		 */
		DownloadInfo getInfo();

		/**
		 * Returns select switch of this panel.
		 *
		 * @return Select switch of this panel.
		 */
		ToggleSwitch getSelectSwitch();

		/**
		 * Returns true if this item can be downloaded.
		 *
		 * @return True if this item can be downloaded.
		 */
		boolean canBeDownloaded();

		/**
		 * Returns true if this item can be added to local database.
		 *
		 * @return True if this item can be added to local database.
		 */
		boolean canBeAdded();

		/**
		 * Returns true if this item can be deleted from global database.
		 *
		 * @return True if this item can be deleted from global database.
		 */
		boolean canBeDeleted();

		/**
		 * Sets download info to this panel.
		 *
		 * @param info
		 *            Download info.
		 */
		void setInfo(DownloadInfo info);

		/**
		 * Starts this panel.
		 */
		void start();

		/**
		 * Downloads the item.
		 *
		 * @param directory
		 *            Download directory.
		 * @param index
		 *            Index of item.
		 */
		void download(Path directory, int index);

		/**
		 * Adds the item to database.
		 */
		void add();

		/**
		 * Deletes this item from the global database.
		 */
		void delete();
	}
}
