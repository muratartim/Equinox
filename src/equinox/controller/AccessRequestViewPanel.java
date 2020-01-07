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
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;

import equinox.Equinox;
import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.AccessRequest;
import equinox.dataServer.remote.message.GetAccessRequestsRequest;
import equinox.plugin.FileType;
import equinox.task.GetAccessRequests;
import equinox.task.SaveImage;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for access request view panel controller.
 *
 * @author Murat Artim
 * @date 15 Apr 2018
 * @time 14:32:24
 */
public class AccessRequestViewPanel implements InternalViewSubPanel {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Control panel. */
	private AccessRequestViewControls controls_;

	@FXML
	private VBox root_, container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create controls
		controls_ = AccessRequestViewControls.load(this);
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
		return "User Access Requests";
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
		fileChooser.setInitialFileName("User Access Requests" + FileType.PNG.getExtension());
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
		return "User Access Requests";
	}

	@Override
	public WritableImage getViewImage() {
		return container_.snapshot(null, null);
	}

	/**
	 * Called when status changed.
	 *
	 * @param status
	 *            Request status.
	 */
	public void onStatusSelected(int status) {
		if (status == GetAccessRequestsRequest.ALL) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetAccessRequests(GetAccessRequestsRequest.ALL));
		}
		else if (status == GetAccessRequestsRequest.PENDING) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetAccessRequests(GetAccessRequestsRequest.PENDING));
		}
		else if (status == GetAccessRequestsRequest.GRANTED) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetAccessRequests(GetAccessRequestsRequest.GRANTED));
		}
		else if (status == GetAccessRequestsRequest.REJECTED) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetAccessRequests(GetAccessRequestsRequest.REJECTED));
		}
	}

	/**
	 * Sets wishes to this panel.
	 *
	 * @param items
	 *            Wishes to set.
	 * @param status
	 *            Status of wishes.
	 */
	public void setRequests(ArrayList<AccessRequest> items, int status) {

		try {

			// clear container
			container_.getChildren().clear();

			// create list for request panels
			ArrayList<VBox> panels = new ArrayList<>();

			// add results to container
			for (AccessRequest item : items) {
				AccessRequestPanel panel = AccessRequestPanel.load(this);
				panel.setRequest(item);
				panels.add(panel.getRoot());
			}
			container_.getChildren().addAll(panels);

			// reset controls
			controls_.reset(status);
		}

		// exception occurred
		catch (Exception e) {

			// log exception
			Equinox.LOGGER.log(Level.WARNING, "Exception occured during setting user access requests.", e);

			// create and show notification
			String message = "Exception occurred during setting user access requests: " + e.getLocalizedMessage();
			message += " Click 'Details' for more information.";
			owner_.getOwner().getNotificationPane().showError("Problem encountered", message, e);
		}
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static AccessRequestViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("AccessRequestViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			AccessRequestViewPanel controller = (AccessRequestViewPanel) fxmlLoader.getController();

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
