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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import control.validationField.DoubleValidationField;
import equinox.Equinox;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.ClientPluginInfo;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.ServerPluginInfo.PluginInfoType;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.SaveTask;
import equinox.task.UploadPlugin;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for upload plugin panel controller.
 *
 * @author Murat Artim
 * @date Mar 31, 2015
 * @time 12:18:58 PM
 */
public class UploadPluginPanel implements InternalInputSubPanel, SchedulingPanel {

	/** Maximum description length. */
	private static final int MAX_DESCRIPTION_LENGTH = 1000;

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_, container_;

	@FXML
	private BorderPane dropZone_;

	@FXML
	private ImageView dropImage_, png_, jar_, zip_;

	@FXML
	private Hyperlink browse_;

	@FXML
	private TextField name_, developerName_, developerEmail_;

	@FXML
	private DoubleValidationField version_;

	@FXML
	private TextArea description_;

	@FXML
	private Accordion accordion_;

	@FXML
	private SplitMenuButton ok_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// add listener to version number field
		version_.setDefaultValue(null);
		version_.setMinimumValue(1.0, true);

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
		return "Upload Plugin";
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// create info
		ClientPluginInfo info = new ClientPluginInfo();

		// check developer name
		String developerName = developerName_.getText();
		if (developerName == null || developerName.isEmpty()) {
			String message = "Please enter developer name to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(developerName_);
			return;
		}
		info.setInfo(PluginInfoType.DEVELOPER_NAME, developerName);

		// check developer email
		String developerEmail = developerEmail_.getText();
		if (!Utility.isValidEmailAddress(developerEmail)) {
			String message = "Please enter a valid developer email address to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(developerEmail_);
			return;
		}
		info.setInfo(PluginInfoType.DEVELOPER_EMAIL, developerEmail);

		// check name
		String name = name_.getText();
		if (name == null || name.isEmpty()) {
			String message = "Please enter plugin name to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(name_);
			return;
		}
		info.setInfo(PluginInfoType.NAME, name);

		// check version
		String message = version_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(version_);
			return;
		}
		info.setInfo(PluginInfoType.VERSION_NUMBER, Double.parseDouble(version_.getText()));

		// check description
		String description = description_.getText();
		if (description == null || description.isEmpty()) {
			message = "Please enter plugin description to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(description_);
			return;
		}

		// check description length
		if (description.length() > MAX_DESCRIPTION_LENGTH) {
			message = "Plugin description exceeds maximum allowed number of characters (" + MAX_DESCRIPTION_LENGTH + ").";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(description_);
			return;
		}
		info.setInfo(PluginInfoType.DESCRIPTION, description);

		// check image
		Path image = (Path) png_.getUserData();
		if (image == null || !Files.exists(image)) {
			message = "Please supply plugin image to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(png_);
			return;
		}

		// read image
		try {
			info.setImage(image.toFile());
		}

		// exception occurred
		catch (Exception e) {
			message = "Exception occured during reading plugin image.";
			Equinox.LOGGER.log(Level.WARNING, message, e);
			owner_.getOwner().getNotificationPane().showError("Problem encountered", message, e);
			return;
		}

		// check jar file
		Path jar = (Path) jar_.getUserData();
		if (jar == null || !Files.exists(jar)) {
			message = "Please supply plugin jar file to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(jar_);
			return;
		}
		Path jarFileName = jar.getFileName();
		if (jarFileName == null) {
			message = "Please supply plugin jar file to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(jar_);
			return;
		}
		info.setInfo(PluginInfoType.JAR_NAME, jarFileName.toString());
		info.setInfo(PluginInfoType.DATA_SIZE, jar.toFile().length());

		// set sample inputs (if any)
		Path sampleInputs = (Path) zip_.getUserData();
		if (sampleInputs != null && Files.exists(sampleInputs)) {
			info.setSampleInputs(sampleInputs.toFile());
		}
		else {
			info.setSampleInputs(null);
		}

		// create and submit task
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new UploadPlugin(info, jar));
		}
		else {
			tm.runTaskInParallel(new SaveTask(new UploadPlugin(info, jar), scheduleDate));
		}

		// return file view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
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

			// JAR
			else if (fileType.equals(FileType.JAR)) {
				owner_.getOwner().setInitialDirectory(file);
				jar_.setUserData(file.toPath());
				if (!toBeAnimated.contains(jar_)) {
					toBeAnimated.add(jar_);
				}
				success = true;
			}

			// ZIP
			else if (fileType.equals(FileType.ZIP)) {
				owner_.getOwner().setInitialDirectory(file);
				zip_.setUserData(file.toPath());
				if (!toBeAnimated.contains(zip_)) {
					toBeAnimated.add(zip_);
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
				if (fileType.equals(FileType.PNG) || fileType.equals(FileType.JAR)) {
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
				if (fileType.equals(FileType.PNG) || fileType.equals(FileType.JAR)) {
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
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.PNG.getExtensionFilter(), FileType.JAR.getExtensionFilter(), FileType.ZIP.getExtensionFilter());

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

		// reset info
		developerName_.clear();
		developerEmail_.clear();
		name_.clear();
		version_.reset();
		description_.clear();

		// reset data
		browse_.setVisited(false);
		ImageView[] items = { png_, jar_, zip_ };
		for (ImageView item : items) {
			item.setUserData(null);
			item.setImage(AddSpectrumPanel.EMPTY);
		}

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@FXML
	private void onDownloadSampleInputClicked() {
		owner_.getOwner().downloadSampleInput("UploadPlugin");
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static UploadPluginPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("UploadPluginPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			UploadPluginPanel controller = (UploadPluginPanel) fxmlLoader.getController();

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
