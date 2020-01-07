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
import org.controlsfx.control.ToggleSwitch;

import equinox.Equinox;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.EquinoxTheme;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.serverUtilities.ServerUtility;
import equinox.task.SaveTask;
import equinox.task.UploadContainerUpdate;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for upload container update panel controller.
 *
 * @author Murat Artim
 * @date Sep 15, 2014
 * @time 1:41:44 PM
 */
public class UploadContainerUpdatePanel implements InternalInputSubPanel, SchedulingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_, fileContainer_;

	@FXML
	private BorderPane fileDropZone_;

	@FXML
	private Hyperlink browseFile_;

	@FXML
	private ImageView fileDropImage_, macInstall_, winInstall_, win64Install_, linInstall_, lin64Install_, versionDesc_;

	@FXML
	private SplitMenuButton ok_;

	@FXML
	private Spinner<Double> versionNumber_;

	@FXML
	private ToggleSwitch pushToDatabase_, pushToFileServer_, pushToWebServer_;

	@FXML
	private Accordion accordion_;

	@FXML
	private TitledPane serverPushPane_;

	@FXML
	private Label macFileName_, winFileName_, win64FileName_, linFileName_, lin64FileName_, versionDescFileName_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup version number spinner
		double min = 0.1;
		double max = 1000.0;
		double step = 0.1;
		double initial = Equinox.getContainerVersion();
		versionNumber_.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, initial, step));

		// setup file names
		macFileName_.setText(Utility.getContainerFileName(ServerUtility.MACOS, ServerUtility.X64));
		winFileName_.setText(Utility.getContainerFileName(ServerUtility.WINDOWS, ServerUtility.X86));
		win64FileName_.setText(Utility.getContainerFileName(ServerUtility.WINDOWS, ServerUtility.X64));
		linFileName_.setText(Utility.getContainerFileName(ServerUtility.LINUX, ServerUtility.X86));
		lin64FileName_.setText(Utility.getContainerFileName(ServerUtility.LINUX, ServerUtility.X64));
		versionDescFileName_.setText(Utility.getContainerVersionDescriptionFileName());

		// show first panel
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
		return "Upload Container Update";
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// get inputs
		double versionNumber = versionNumber_.getValue();
		Path installMacFile = (Path) macInstall_.getUserData();
		Path installWinFile = (Path) winInstall_.getUserData();
		Path installWin64File = (Path) win64Install_.getUserData();
		Path installLinFile = (Path) linInstall_.getUserData();
		Path installLin64File = (Path) lin64Install_.getUserData();
		Path verDescFile = (Path) versionDesc_.getUserData();
		boolean pushToDatabase = pushToDatabase_.isSelected();
		boolean pushToFileServer = pushToFileServer_.isSelected();
		boolean pushToWebServer = pushToWebServer_.isSelected();

		// check inputs
		if (!checkInputs(installMacFile, installWinFile, installWin64File, installLinFile, installLin64File, verDescFile, pushToDatabase, pushToFileServer, pushToWebServer))
			return;

		// create and start task
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new UploadContainerUpdate(versionNumber, installMacFile, installWinFile, installWin64File, installLinFile, installLin64File, verDescFile, pushToDatabase, pushToFileServer, pushToWebServer));
		}

		// save task
		else {
			tm.runTaskInParallel(new SaveTask(new UploadContainerUpdate(versionNumber, installMacFile, installWinFile, installWin64File, installLinFile, installLin64File, verDescFile, pushToDatabase, pushToFileServer, pushToWebServer), scheduleDate));
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

			// mac install file
			else if (file.getName().equals(Utility.getContainerFileName(ServerUtility.MACOS, ServerUtility.X64))) {
				owner_.getOwner().setInitialDirectory(file);
				macInstall_.setUserData(file.toPath());
				if (!toBeAnimated.contains(macInstall_)) {
					toBeAnimated.add(macInstall_);
				}
				success = true;
			}

			// windows install file
			else if (file.getName().equals(Utility.getContainerFileName(ServerUtility.WINDOWS, ServerUtility.X86))) {
				owner_.getOwner().setInitialDirectory(file);
				winInstall_.setUserData(file.toPath());
				if (!toBeAnimated.contains(winInstall_)) {
					toBeAnimated.add(winInstall_);
				}
				success = true;
			}

			// 64 bit windows install file
			else if (file.getName().equals(Utility.getContainerFileName(ServerUtility.WINDOWS, ServerUtility.X64))) {
				owner_.getOwner().setInitialDirectory(file);
				win64Install_.setUserData(file.toPath());
				if (!toBeAnimated.contains(win64Install_)) {
					toBeAnimated.add(win64Install_);
				}
				success = true;
			}

			// linux install file
			else if (file.getName().equals(Utility.getContainerFileName(ServerUtility.LINUX, ServerUtility.X86))) {
				owner_.getOwner().setInitialDirectory(file);
				linInstall_.setUserData(file.toPath());
				if (!toBeAnimated.contains(linInstall_)) {
					toBeAnimated.add(linInstall_);
				}
				success = true;
			}

			// 64 bit linux install file
			else if (file.getName().equals(Utility.getContainerFileName(ServerUtility.LINUX, ServerUtility.X64))) {
				owner_.getOwner().setInitialDirectory(file);
				lin64Install_.setUserData(file.toPath());
				if (!toBeAnimated.contains(lin64Install_)) {
					toBeAnimated.add(lin64Install_);
				}
				success = true;
			}

			// version description file
			else if (file.getName().equals(Utility.getContainerVersionDescriptionFileName())) {
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
				if (fileType.equals(FileType.ZIP) || fileType.equals(FileType.GZ) || fileType.equals(FileType.HTML)) {
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
				if (fileType.equals(FileType.ZIP) || fileType.equals(FileType.GZ) || fileType.equals(FileType.HTML)) {
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
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.getCustomFileFilter("Input files", FileType.ZIP, FileType.GZ, FileType.HTML));

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
		owner_.getOwner().showHelp("How to upload container update", null);
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

		// reset version number spinner
		versionNumber_.getValueFactory().setValue(Equinox.getContainerVersion());

		// clear all files
		ImageView[] items = { macInstall_, winInstall_, win64Install_, linInstall_, lin64Install_, versionDesc_ };
		for (ImageView item : items) {
			item.setUserData(null);
			item.setImage(AddSpectrumPanel.EMPTY);
		}

		// reset hyperlinks
		browseFile_.setVisited(false);

		// reset server push options
		pushToDatabase_.setSelected(true);
		pushToFileServer_.setSelected(true);
		pushToWebServer_.setSelected(true);

		// show first panel
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	/**
	 * Checks inputs and displays warning message if necessary.
	 *
	 * @param installMacFile
	 *            Mac installation package.
	 * @param installWinFile
	 *            Windows installation package.
	 * @param installWin64File
	 *            64 bit Windows installation package.
	 * @param installLinFile
	 *            Linux installation package.
	 * @param installLin64File
	 *            64 bit Linux installation package.
	 * @param versionDescFile
	 *            Version description file.
	 * @param pushToDatabase
	 *            True to push update to database.
	 * @param pushToFileServer
	 *            True to push update to file server.
	 * @param pushToWebServer
	 *            True to push update to web server.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(Path installMacFile, Path installWinFile, Path installWin64File, Path installLinFile, Path installLin64File, Path versionDescFile, boolean pushToDatabase, boolean pushToFileServer, boolean pushToWebServer) {

		// check inputs
		String message = null;
		Node node = null;

		// check files
		if (installMacFile == null || !Files.exists(installMacFile)) {
			message = "Please select a valid Mac installation package to proceed.";
			node = macInstall_;
		}
		else if (installWinFile == null || !Files.exists(installWinFile)) {
			message = "Please select a valid Windows installation package to proceed.";
			node = winInstall_;
		}
		else if (installWin64File == null || !Files.exists(installWin64File)) {
			message = "Please select a valid 64 bit Windows installation package to proceed.";
			node = win64Install_;
		}
		else if (installLinFile == null || !Files.exists(installLinFile)) {
			message = "Please select a valid Linux installation package to proceed.";
			node = linInstall_;
		}
		else if (installLin64File == null || !Files.exists(installLin64File)) {
			message = "Please select a valid 64 bit Linux installation package to proceed.";
			node = lin64Install_;
		}
		else if (versionDescFile == null || !Files.exists(versionDescFile)) {
			message = "Please select a valid version description file to proceed.";
			node = versionDesc_;
		}
		if (!pushToDatabase && !pushToFileServer && !pushToWebServer) {
			message = "Please select at least 1 server push to proceed.";
			node = serverPushPane_;
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
	public static UploadContainerUpdatePanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("UploadContainerUpdatePanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			UploadContainerUpdatePanel controller = (UploadContainerUpdatePanel) fxmlLoader.getController();

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
