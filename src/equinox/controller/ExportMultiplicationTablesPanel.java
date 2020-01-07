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
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.ExportMultiplicationTables;
import equinox.task.SaveTask;
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
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for export multiplication tables panel controller.
 *
 * @author Murat Artim
 * @date Feb 18, 2016
 * @time 5:51:04 PM
 */
public class ExportMultiplicationTablesPanel implements InternalInputSubPanel, SchedulingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private BorderPane dropZone_;

	@FXML
	private ImageView dropImage_, mut_;

	@FXML
	private Hyperlink browse_;

	@FXML
	private TextField spectrumName_, pilotPointName_, description_, deliveryRef_, issue_, mission_, program_, section_;

	@FXML
	private SplitMenuButton ok_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
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
	public String getHeader() {
		return "Export Loadcase Factor Files";
	}

	@Override
	public void showing() {
		onResetClicked();
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// get files
		@SuppressWarnings("unchecked")
		List<File> mutFiles = (List<File>) mut_.getUserData();

		// get info
		String spectrumName = spectrumName_.getText();
		String ppName = pilotPointName_.getText();
		String program = program_.getText();
		String section = section_.getText();
		String mission = mission_.getText();
		String issue = issue_.getText();
		String delRef = deliveryRef_.getText();
		String description = description_.getText();

		// check inputs
		if (!checkInputs(mutFiles, spectrumName, ppName, program, section, mission, issue, delRef, description))
			return;

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.ZIP.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName("LoadcaseFactors.zip");
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File output = FileType.appendExtension(selectedFile, FileType.ZIP);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new ExportMultiplicationTables(mutFiles, spectrumName, ppName, program, section, mission, issue, delRef, description, output));
		}
		else {
			tm.runTaskInParallel(new SaveTask(new ExportMultiplicationTables(mutFiles, spectrumName, ppName, program, section, mission, issue, delRef, description, output), scheduleDate));
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Checks inputs.
	 *
	 * @param mutFiles
	 *            Multiplication table files.
	 * @param spectrumName
	 *            Spectrum name.
	 * @param ppName
	 *            Pilot point name.
	 * @param program
	 *            A/C program.
	 * @param section
	 *            A/C section.
	 * @param mission
	 *            Fatigue mission.
	 * @param issue
	 *            Multiplication table file issue.
	 * @param delRef
	 *            Delivery reference.
	 * @param description
	 *            Description.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(List<File> mutFiles, String spectrumName, String ppName, String program, String section, String mission, String issue, String delRef, String description) {

		// check inputs
		if ((mutFiles == null) || mutFiles.isEmpty())
			return showMissingInputWarning(mut_, "Please supply loadcase factor files to proceed.");
		if ((spectrumName == null) || spectrumName.trim().isEmpty())
			return showMissingInputWarning(spectrumName_, "Please supply spectrum name to proceed.");
		if ((program == null) || program.trim().isEmpty())
			return showMissingInputWarning(program_, "Please supply A/C program to proceed.");
		if ((section == null) || section.trim().isEmpty())
			return showMissingInputWarning(section_, "Please supply A/C section to proceed.");
		if ((mission == null) || mission.trim().isEmpty())
			return showMissingInputWarning(mission_, "Please supply fatigue mission to proceed.");

		// check spectrum name for Windows OS
		String message = Utility.isValidFileName(spectrumName);
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(spectrumName_);
			return false;
		}

		// check pilot point name for Windows OS
		if ((ppName != null) && !ppName.trim().isEmpty()) {
			message = Utility.isValidFileName(ppName);
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(pilotPointName_);
				return false;
			}
		}

		// check input lengths
		if (spectrumName.trim().length() > 100)
			return showInputLengthWarning(spectrumName_, 100);
		if ((ppName != null) && (ppName.trim().length() > 100))
			return showInputLengthWarning(pilotPointName_, 100);
		if (program.trim().length() > 100)
			return showInputLengthWarning(program_, 100);
		if (section.trim().length() > 100)
			return showInputLengthWarning(section_, 100);
		if (mission.trim().length() > 50)
			return showInputLengthWarning(mission_, 50);
		if (issue.trim().length() > 50)
			return showInputLengthWarning(issue_, 50);
		if (delRef.trim().length() > 50)
			return showInputLengthWarning(deliveryRef_, 50);
		if (description.trim().length() > 200)
			return showInputLengthWarning(description_, 200);

		// inputs are acceptable
		return true;
	}

	/**
	 * Shows input length warning message.
	 *
	 * @param node
	 *            Node to display the message.
	 * @param maxLength
	 *            Maximum length.
	 * @return False.
	 */
	private static boolean showInputLengthWarning(Node node, int maxLength) {
		String message = "Character limit exceeded. Please use maximum " + maxLength + " caharacters.";
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
	 * Shows missing input warning message.
	 *
	 * @param node
	 *            Node to display the message.
	 * @param message
	 *            Warning message.
	 * @return False.
	 */
	private static boolean showMissingInputWarning(Node node, String message) {
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(node);
		return false;
	}

	@FXML
	private void onDragOver(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if ((event.getGestureSource() != dropZone_) && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.MUT)) {
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
		if ((event.getGestureSource() != dropZone_) && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.MUT)) {
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
		if ((event.getGestureSource() != dropZone_) && db.hasFiles()) {

			// process files
			success = processFiles(db.getFiles());
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
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

			// MUT
			if (fileType.equals(FileType.MUT)) {
				owner_.getOwner().setInitialDirectory(file);
				@SuppressWarnings("unchecked")
				List<File> mutFiles = (List<File>) mut_.getUserData();
				if (mutFiles == null) {
					mutFiles = new ArrayList<>();
					mutFiles.add(file);
					mut_.setUserData(mutFiles);
					if (!toBeAnimated.contains(mut_)) {
						toBeAnimated.add(mut_);
					}
				}
				else {
					mutFiles.add(file);
				}
				success = true;
			}
		}

		// animate file types
		if (success && !toBeAnimated.isEmpty()) {
			Animator.bouncingScale(0.0, 100.0, 1.0, 1.5, 1.0, new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					for (ImageView item : toBeAnimated) {
						item.setImage(Utility.getImage("full.png"));
					}
				}
			}, toBeAnimated).play();
		}

		// return
		return success;
	}

	@FXML
	private void onBrowseClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.MUT.getExtensionFilter());

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if ((files == null) || files.isEmpty())
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(files.get(0));

		// process files
		processFiles(files);
	}

	@FXML
	private void onResetClicked() {

		// clear all files
		mut_.setUserData(null);
		mut_.setImage(AddSpectrumPanel.EMPTY);

		// reset hyperlink
		browse_.setVisited(false);

		// get selected item
		TreeItem<String> item = owner_.getSelectedFiles().get(0);

		// spectrum
		if (item instanceof Spectrum) {
			Spectrum spectrum = (Spectrum) item;
			spectrumName_.setText(spectrum.getName());
			pilotPointName_.clear();
			program_.setText(spectrum.getProgram());
			section_.setText(spectrum.getSection());
			mission_.setText(spectrum.getMission());
		}

		// pilot point
		else if (item instanceof STFFile) {
			STFFile stfFile = (STFFile) item;
			spectrumName_.setText(stfFile.getParentItem().getName());
			pilotPointName_.setText(FileType.getNameWithoutExtension(stfFile.getName()));
			program_.setText(stfFile.getParentItem().getProgram());
			section_.setText(stfFile.getParentItem().getSection());
			mission_.setText(stfFile.getMission());
		}

		// set other fields
		issue_.clear();
		deliveryRef_.clear();
		description_.clear();
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
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to export loadcase factor files", null);
	}

	@FXML
	private void onDownloadSampleInputClicked() {
		owner_.getOwner().downloadSampleInput("ExportMultiplicationTables");
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static ExportMultiplicationTablesPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ExportMultiplicationTablesPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			ExportMultiplicationTablesPanel controller = (ExportMultiplicationTablesPanel) fxmlLoader.getController();

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
