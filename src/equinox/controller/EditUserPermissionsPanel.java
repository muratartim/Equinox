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
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;

import org.controlsfx.control.CheckListView;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.EquinoxTheme;
import equinox.data.input.UserProfile;
import equinox.font.IconicFont;
import equinox.serverUtilities.Permission;
import equinox.task.EditUserPermissions;
import equinox.task.GetUserPermissions;
import equinox.task.GetUserPermissions.UserPermissionRequestingPanel;
import equinox.task.SaveTask;
import equinox.utility.Utility;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Class for edit user permissions panel controller.
 *
 * @author Murat Artim
 * @date 6 Apr 2018
 * @time 01:36:28
 */
public class EditUserPermissionsPanel implements InternalInputSubPanel, SchedulingPanel, UserPermissionRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextField alias_;

	@FXML
	private ComboBox<UserProfile> profiles_;

	@FXML
	private CheckListView<Permission> permissions_;

	@FXML
	private Accordion accordion_;

	@FXML
	private SplitMenuButton ok_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set user profiles
		profiles_.setItems(FXCollections.observableArrayList(UserProfile.values()));

		// set non-admin permissions
		ArrayList<Permission> nonAdminPerms = new ArrayList<>();
		for (Permission p : Permission.values()) {
			if (!p.isAdminPermission()) {
				nonAdminPerms.add(p);
			}
		}
		permissions_.setItems(FXCollections.observableArrayList(nonAdminPerms));

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
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
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {
		onResetClicked();
	}

	@Override
	public String getHeader() {
		return "Edit User Permissions";
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// get user info
		String alias = alias_.getText();

		// get permissions
		ObservableList<Permission> permissions = permissions_.getCheckModel().getCheckedItems();

		// check inputs
		if (!checkInputs(alias, permissions))
			return;

		// create and start task
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new EditUserPermissions(alias, permissions.toArray(new Permission[permissions.size()])));
		}
		else {
			tm.runTaskInParallel(new SaveTask(new EditUserPermissions(alias, permissions.toArray(new Permission[permissions.size()])), scheduleDate));
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@Override
	public void setUserPermissions(String alias, Permission[] permissions) {

		// reset user info
		alias_.setText(alias);

		// reset permissions
		permissions_.getCheckModel().clearChecks();
		profiles_.getSelectionModel().clearSelection();

		// set permissions
		for (Permission p : permissions) {
			permissions_.getCheckModel().check(p);
		}

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(1));
	}

	@FXML
	private void onOKClicked() {
		setTaskScheduleDate(true, null);
	}

	@FXML
	private void onSaveTaskClicked() {
		setTaskScheduleDate(false, null);
	}

	@FXML
	private void onScheduleTaskClicked() {
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
		popOver.setDetachable(false);
		popOver.setContentNode(ScheduleTaskPanel.load(popOver, this, null));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(ok_);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {

		// reset user info
		alias_.clear();

		// reset permissions
		permissions_.getCheckModel().clearChecks();
		profiles_.getSelectionModel().clearSelection();

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@FXML
	private void onProfileSelected() {

		// clear permissions
		permissions_.getCheckModel().clearChecks();

		// no profile selected
		if (profiles_.getSelectionModel().isEmpty())
			return;

		// get selected profile
		UserProfile selected = profiles_.getSelectionModel().getSelectedItem();

		// set permissions
		for (Permission p : selected.getPermissions()) {
			permissions_.getCheckModel().check(p);
		}
	}

	@FXML
	private void onGoClicked() {

		// alias
		String alias = alias_.getText();
		if (alias == null || alias.trim().isEmpty()) {
			String message = "Please enter valid user alias to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(alias_);
			return;
		}

		// request user permissions
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskSilently(new GetUserPermissions(alias, this), false);
	}

	/**
	 * Checks user inputs.
	 *
	 * @param alias
	 *            User alias.
	 * @param permissions
	 *            User permissions.
	 * @return True if inputs are valid.
	 */
	private boolean checkInputs(String alias, ObservableList<Permission> permissions) {

		// alias
		if (alias == null || alias.trim().isEmpty()) {
			String message = "Please enter valid user alias to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(alias_);
			return false;
		}

		// permissions
		if (permissions == null || permissions.isEmpty()) {
			String message = "Please select at least one user permission to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(permissions_);
			return false;
		}

		// valid inputs
		return true;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static EditUserPermissionsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("EditUserPermissionsPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			EditUserPermissionsPanel controller = (EditUserPermissionsPanel) fxmlLoader.getController();

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
