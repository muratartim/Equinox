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
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import org.controlsfx.control.CheckListView;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.EquinoxTheme;
import equinox.data.input.UserProfile;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.AddNewUser;
import equinox.task.SaveTask;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for add user panel controller.
 *
 * @author Murat Artim
 * @date 4 Apr 2018
 * @time 14:42:01
 */
public class AddUserPanel implements InternalInputSubPanel, SchedulingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_, container_;

	@FXML
	private BorderPane dropZone_;

	@FXML
	private ImageView dropImage_, png_;

	@FXML
	private Hyperlink browse_;

	@FXML
	private TextField alias_, name_, organization_, email_;

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
		return "Create User Account";
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// get user info
		String alias = alias_.getText();
		String name = name_.getText();
		String organization = organization_.getText();
		String email = email_.getText();

		// get permissions
		ObservableList<Permission> permissions = permissions_.getCheckModel().getCheckedItems();

		// get profile image
		Path image = (Path) png_.getUserData();

		// check inputs
		if (!checkInputs(alias, name, organization, email, permissions, image))
			return;

		// create and start task
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new AddNewUser(alias, name, organization, email, permissions.toArray(new Permission[0]), image));
		}
		else {
			tm.runTaskInParallel(new SaveTask(new AddNewUser(alias, name, organization, email, permissions.toArray(new Permission[0]), image), scheduleDate));
		}

		// reset panel
		onResetClicked();
	}

	/**
	 * Processes selected files.
	 *
	 * @param files
	 *            Selected files.
	 * @return True if process completed successfully.
	 */
	public boolean processFiles(List<File> files) {

		// check file types
		ArrayList<ImageView> toBeAnimated = new ArrayList<>();
		boolean success = false;
		for (File file : files) {

			// get file type
			FileType fileType = FileType.getFileType(file);

			// not recognized
			if (fileType == null) {
				continue;
			}

			// PNG
			if (fileType.equals(FileType.PNG)) {
				owner_.getOwner().setInitialDirectory(file);
				png_.setUserData(file.toPath());
				if (!toBeAnimated.contains(png_)) {
					toBeAnimated.add(png_);
				}
				success = true;
			}
		}

		// animate file types
		if (success && !toBeAnimated.isEmpty()) {
			Animator.bouncingScale(0.0, 100.0, 1.0, 1.5, 1.0, (EventHandler<ActionEvent>) event -> {
				for (ImageView item : toBeAnimated) {
					item.setImage(Utility.getImage("full.png"));
				}
			}, toBeAnimated).play();
		}

		// return
		return success;
	}

	@FXML
	private void onDragOver(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if (event.getGestureSource() != dropZone_ && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.PNG)) {
					event.acceptTransferModes(TransferMode.ANY);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragEntered(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if (event.getGestureSource() != dropZone_ && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.PNG)) {
					dropImage_.setImage(AddSpectrumPanel.DROP_ZONE_ACTIVE);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragExited(DragEvent event) {
		dropImage_.setImage(AddSpectrumPanel.DROP_ZONE);
		event.consume();
	}

	@FXML
	private void onDragDropped(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		boolean success = false;
		if (event.getGestureSource() != dropZone_ && db.hasFiles()) {

			// process files
			success = processFiles(db.getFiles());
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}

	@FXML
	private void onBrowseClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(files.get(0));

		// process files
		processFiles(files);
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
		name_.clear();
		organization_.clear();
		email_.clear();

		// reset permissions
		permissions_.getCheckModel().clearChecks();
		profiles_.getSelectionModel().clearSelection();

		// reset data
		browse_.setVisited(false);
		png_.setUserData(null);
		png_.setImage(AddSpectrumPanel.EMPTY);

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@FXML
	private void onDownloadSampleInputClicked() {
		owner_.getOwner().downloadSampleInput("AddNewUser");
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

	/**
	 * Checks user inputs.
	 *
	 * @param alias
	 *            User alias.
	 * @param name
	 *            User name.
	 * @param organization
	 *            Organization siglum.
	 * @param email
	 *            User email address.
	 * @param permissions
	 *            User permissions.
	 * @param image
	 *            User profile image.
	 * @return True if inputs are valid.
	 */
	private boolean checkInputs(String alias, String name, String organization, String email, ObservableList<Permission> permissions, Path image) {

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

		// name
		if (name == null || name.trim().isEmpty()) {
			String message = "Please enter valid user name to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(name_);
			return false;
		}

		// organization
		if (organization == null || organization.trim().isEmpty()) {
			String message = "Please enter valid user organization to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(organization_);
			return false;
		}

		// email
		if (!Utility.isValidEmailAddress(email)) {
			String message = "Please enter a valid user email address to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(email_);
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
	public static AddUserPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("AddUserPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			AddUserPanel controller = (AddUserPanel) fxmlLoader.getController();

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
