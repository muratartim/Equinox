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

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.EquinoxTheme;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.SaveTask;
import equinox.task.UploadAppUpdate;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for upload application update panel controller.
 *
 * @author Murat Artim
 * @date 26 May 2018
 * @time 19:42:24
 */
public class UploadAppUpdatePanel implements InternalInputSubPanel, SchedulingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_, fileContainer_;

	@FXML
	private BorderPane fileDropZone_;

	@FXML
	private Hyperlink browseFile_;

	@FXML
	private ImageView fileDropImage_, manifest_, jar_, libs_, resources_, dlls_, versionDesc_;

	@FXML
	private SplitMenuButton ok_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
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
		return "Upload Application Update";
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// get inputs
		Path manifest = (Path) manifest_.getUserData();
		Path jar = (Path) jar_.getUserData();
		Path libs = (Path) libs_.getUserData();
		Path resources = (Path) resources_.getUserData();
		Path dlls = (Path) dlls_.getUserData();
		Path verDescFile = (Path) versionDesc_.getUserData();

		// check inputs
		if (!checkInputs(manifest, jar, libs, resources, dlls, verDescFile))
			return;

		// create and start task
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new UploadAppUpdate(manifest, jar, libs, resources, dlls, verDescFile));
		}

		// save task
		else {
			tm.runTaskInParallel(new SaveTask(new UploadAppUpdate(manifest, jar, libs, resources, dlls, verDescFile), scheduleDate));
		}

		// return to file view
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

			// not recognized type
			if (FileType.getFileType(file) == null) {
				continue;
			}

			// manifest file
			else if (file.getName().equals("MANIFEST.MF")) {
				owner_.getOwner().setInitialDirectory(file);
				manifest_.setUserData(file.toPath());
				if (!toBeAnimated.contains(manifest_)) {
					toBeAnimated.add(manifest_);
				}
				success = true;
			}

			// jar file
			else if (file.getName().equals("jar.zip")) {
				owner_.getOwner().setInitialDirectory(file);
				jar_.setUserData(file.toPath());
				if (!toBeAnimated.contains(jar_)) {
					toBeAnimated.add(jar_);
				}
				success = true;
			}

			// libs file
			else if (file.getName().equals("libs.zip")) {
				owner_.getOwner().setInitialDirectory(file);
				libs_.setUserData(file.toPath());
				if (!toBeAnimated.contains(libs_)) {
					toBeAnimated.add(libs_);
				}
				success = true;
			}

			// resources file
			else if (file.getName().equals("resources.zip")) {
				owner_.getOwner().setInitialDirectory(file);
				resources_.setUserData(file.toPath());
				if (!toBeAnimated.contains(resources_)) {
					toBeAnimated.add(resources_);
				}
				success = true;
			}

			// dlls file
			else if (file.getName().equals("dlls.zip")) {
				owner_.getOwner().setInitialDirectory(file);
				dlls_.setUserData(file.toPath());
				if (!toBeAnimated.contains(dlls_)) {
					toBeAnimated.add(dlls_);
				}
				success = true;
			}

			// version description file
			else if (file.getName().equals("versionDescription.html")) {
				owner_.getOwner().setInitialDirectory(file);
				versionDesc_.setUserData(file.toPath());
				if (!toBeAnimated.contains(versionDesc_)) {
					toBeAnimated.add(versionDesc_);
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
	private void onFileDragOver(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if (event.getGestureSource() != fileDropZone_ && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.ZIP) || fileType.equals(FileType.MF) || fileType.equals(FileType.HTML)) {
					event.acceptTransferModes(TransferMode.ANY);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onFileDragEntered(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if (event.getGestureSource() != fileDropZone_ && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.ZIP) || fileType.equals(FileType.MF) || fileType.equals(FileType.HTML)) {
					fileDropImage_.setImage(AddSpectrumPanel.DROP_ZONE_ACTIVE);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onFileDragExited(DragEvent event) {
		fileDropImage_.setImage(AddSpectrumPanel.DROP_ZONE);
		event.consume();
	}

	@FXML
	private void onFileDragDropped(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		boolean success = false;
		if (event.getGestureSource() != fileDropZone_ && db.hasFiles()) {

			// process files
			success = processFiles(db.getFiles());
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}

	@FXML
	private void onBrowseFileClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.getCustomFileFilter("Input files", FileType.ZIP, FileType.MF, FileType.HTML));

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
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to upload application update", null);
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

		// clear all files
		ImageView[] items = { manifest_, jar_, libs_, resources_, dlls_, versionDesc_ };
		for (ImageView item : items) {
			item.setUserData(null);
			item.setImage(AddSpectrumPanel.EMPTY);
		}

		// reset hyperlinks
		browseFile_.setVisited(false);
	}

	/**
	 * Checks inputs and displays warning message if necessary.
	 *
	 * @param manifest
	 *            Path to manifest file.
	 * @param jar
	 *            Path to jar package.
	 * @param libs
	 *            Path to libs package.
	 * @param resources
	 *            Path to resources package.
	 * @param dlls
	 *            Path to dlls package.
	 * @param verDescFile
	 *            Path version description file.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(Path manifest, Path jar, Path libs, Path resources, Path dlls, Path verDescFile) {

		// check inputs
		String message = null;
		Node node = null;

		// check if any application resource supplied
		boolean jarNotSupplied = jar == null || !Files.exists(jar);
		boolean libsNotSupplied = libs == null || !Files.exists(libs);
		boolean resourcesNotSupplied = resources == null || !Files.exists(resources);
		boolean dllsNotSupplied = dlls == null || !Files.exists(dlls);

		// check files
		if (manifest == null || !Files.exists(manifest)) {
			message = "Please supply a valid manifest file to proceed.";
			node = manifest_;
		}

		// no application resource supplied
		else if (jarNotSupplied && libsNotSupplied && resourcesNotSupplied && dllsNotSupplied) {
			message = "Please supply at least 1 application resource file (jar, libs, resources or dlls) to proceed.";
			node = ok_;
		}

		// all valid inputs
		if (message == null)
			return true;

		// show warning
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(node);
		return false;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static UploadAppUpdatePanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("UploadAppUpdatePanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			UploadAppUpdatePanel controller = (UploadAppUpdatePanel) fxmlLoader.getController();

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
