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
import java.nio.file.Path;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import equinox.Equinox;
import equinox.controller.DownloadViewPanel.DownloadItemPanel;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.DownloadInfo;
import equinox.dataServer.remote.data.HelpVideoInfo;
import equinox.dataServer.remote.data.HelpVideoInfo.HelpVideoInfoType;
import equinox.font.IconicFont;
import equinox.serverUtilities.Permission;
import equinox.task.DeleteHelpVideo;
import equinox.task.DownloadHelpVideo;
import equinox.utility.Utility;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Class for download help video panel controller.
 *
 * @author Murat Artim
 * @date Mar 28, 2016
 * @time 1:00:46 PM
 */
public class DownloadHelpVideoPanel implements Initializable, DownloadItemPanel {

	/** The owner panel. */
	private DownloadViewPanel owner_;

	/** Help video info. */
	private HelpVideoInfo info_;

	@FXML
	private VBox root_, buttonPane_;

	@FXML
	private Label name_, description_, duration_, size_;

	@FXML
	private Button delete_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@Override
	public void start() {

		// remove delete button if not administrator or logged in
		if (!Equinox.USER.hasPermission(Permission.DELETE_HELP_VIDEO, false, null) || !Equinox.USER.isLoggedAsAdministrator()) {
			buttonPane_.getChildren().remove(delete_);
		}
	}

	@Override
	public String getHeader() {
		return "Help Videos";
	}

	/**
	 * Returns the owner panel of this sub panel.
	 *
	 * @return The owner panel of this sub panel.
	 */
	public DownloadViewPanel getOwner() {
		return owner_;
	}

	@Override
	public VBox getRoot() {
		return root_;
	}

	@Override
	public ToggleSwitch getSelectSwitch() {
		return null;
	}

	@Override
	public DownloadInfo getInfo() {
		return info_;
	}

	@Override
	public void download(Path directory, int index) {
		// no implementation
	}

	@Override
	public void add() {
		// no implementation
	}

	@Override
	public void delete() {
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new DeleteHelpVideo(info_, owner_));
	}

	@Override
	public void setInfo(DownloadInfo info) {

		// set info
		info_ = (HelpVideoInfo) info;

		// set title
		name_.setText((String) info_.getInfo(HelpVideoInfoType.NAME));
		description_.setText((String) info_.getInfo(HelpVideoInfoType.DESCRIPTION));
		duration_.setText("Duration: " + (String) info_.getInfo(HelpVideoInfoType.DURATION));
		size_.setText("Download size: " + Utility.readableFileSize((long) info_.getInfo(HelpVideoInfoType.DATA_SIZE)));
	}

	@Override
	public boolean canBeDownloaded() {
		return false;
	}

	@Override
	public boolean canBeAdded() {
		return false;
	}

	@Override
	public boolean canBeDeleted() {
		return buttonPane_.getChildren().contains(delete_);
	}

	@FXML
	private void onWatchClicked() {
		long id = (long) info_.getInfo(HelpVideoInfoType.ID);
		String name = (String) info_.getInfo(HelpVideoInfoType.NAME);
		owner_.getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(new DownloadHelpVideo(id, name));
	}

	@FXML
	private void onDeleteClicked() {

		// create confirmation action
		PopOver popOver = new PopOver();
		EventHandler<ActionEvent> handler = event -> {

			// get task panel
			ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();

			// delete
			tm.runTaskInParallel(new DeleteHelpVideo(info_, owner_));

			// hide pop-over
			popOver.hide();
		};

		// show message
		String message = "Are you sure you want to delete the help video from ESCSAS global database?";
		popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel2.load(popOver, message, 50, "Delete", handler, NotificationPanel2.QUESTION));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);

		// show popup
		popOver.show(delete_);
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static DownloadHelpVideoPanel load(DownloadViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadHelpVideoPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			DownloadHelpVideoPanel controller = (DownloadHelpVideoPanel) fxmlLoader.getController();

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
