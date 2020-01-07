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
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftEquivalentStresses;
import equinox.data.input.AircraftEquivalentStressType;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.AddAircraftEquivalentStresses;
import equinox.task.SaveTask;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for add equivalent stresses panel controller.
 *
 * @author Murat Artim
 * @date Apr 29, 2016
 * @time 1:21:37 PM
 */
public class AddAircraftEquivalentStressesPanel implements InternalInputSubPanel, SchedulingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private BorderPane dropZone_;

	@FXML
	private ImageView dropImage_, eqs_;

	@FXML
	private Hyperlink browse_;

	@FXML
	private ComboBox<AircraftEquivalentStressType> stressType_;

	@FXML
	private SplitMenuButton ok_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set stress types
		stressType_.setItems(FXCollections.observableArrayList(AircraftEquivalentStressType.values()));

		// add change listener to EQS file check image
		eqs_.imageProperty().addListener(new ChangeListener<Image>() {

			@Override
			public void changed(ObservableValue<? extends Image> observable, Image oldValue, Image newValue) {
				if (newValue == null)
					return;
				stressType_.getSelectionModel().clearSelection();
				stressType_.setDisable(false);
			}
		});
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

		// reset panel
		onResetClicked();
	}

	@Override
	public String getHeader() {
		return "Add Equivalent Stresses";
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// get inputs
		Path file = (Path) eqs_.getUserData();
		AircraftEquivalentStressType stressType = stressType_.getSelectionModel().getSelectedItem();

		// check inputs
		if (!checkInputs(file, stressType))
			return;

		// get selected item
		AircraftEquivalentStresses selected = (AircraftEquivalentStresses) owner_.getSelectedFiles().get(0);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new AddAircraftEquivalentStresses(file, stressType, selected));
		}
		else {
			tm.runTaskInParallel(new SaveTask(new AddAircraftEquivalentStresses(file, stressType, selected), scheduleDate));
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Processes given input file.
	 *
	 * @param file
	 *            Input file.
	 * @return True if process completed successfully.
	 */
	public boolean processFile(File file) {

		// check file types
		ArrayList<ImageView> toBeAnimated = new ArrayList<>();
		boolean success = false;

		// get file type
		FileType fileType = FileType.getFileType(file);

		// not recognized
		if (fileType == null)
			return false;

		// EQS or XLS
		if (fileType.equals(FileType.EQS) || fileType.equals(FileType.XLS)) {
			owner_.getOwner().setInitialDirectory(file);
			eqs_.setUserData(file.toPath());
			if (!toBeAnimated.contains(eqs_)) {
				toBeAnimated.add(eqs_);
			}
			success = true;
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
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.EQS.getExtensionFilter(), FileType.XLS.getExtensionFilter());

		// show open dialog
		File file = fileChooser.showOpenDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if ((file == null) || !file.exists())
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(file);

		// process file
		processFile(file);
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
				if (fileType.equals(FileType.EQS) || fileType.equals(FileType.XLS)) {
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
				if (fileType.equals(FileType.EQS) || fileType.equals(FileType.XLS)) {
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

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.EQS) || fileType.equals(FileType.XLS)) {
					success = processFile(file);
					break;
				}
			}
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to add aircraft equivalent stresses", null);
	}

	@FXML
	private void onDownloadSampleInputClicked() {
		owner_.getOwner().downloadSampleInput("AddEquivalentStresses");
	}

	@FXML
	private void onResetClicked() {

		// clear all files
		eqs_.setUserData(null);
		eqs_.setImage(AddSpectrumPanel.EMPTY);

		// reset stress type
		stressType_.getSelectionModel().clearSelection();
		stressType_.setValue(null);
		stressType_.setDisable(true);

		// reset hyperlink
		browse_.setVisited(false);
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

	/**
	 * Checks inputs.
	 *
	 * @param file
	 *            Input file.
	 * @param stressType
	 *            Selected stress type.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(Path file, AircraftEquivalentStressType stressType) {

		// check inputs
		String message = null;
		Node node = null;

		// no files given
		if ((file == null) || !Files.exists(file)) {
			message = "Please supply equivalent stress input file to proceed.";
			node = eqs_;
		}

		// no stress type selected
		else if (stressType == null) {
			message = "Please select equivalent stress type to proceed.";
			node = stressType_;
		}

		// all valid inputs
		if (message == null)
			return true;

		// show warning
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(node);
		return false;
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static AddAircraftEquivalentStressesPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("AddAircraftEquivalentStressesPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			AddAircraftEquivalentStressesPanel controller = (AddAircraftEquivalentStressesPanel) fxmlLoader.getController();

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
