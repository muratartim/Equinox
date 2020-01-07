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
import equinox.task.AddSpectrum;
import equinox.task.GetConvTableSheetNames;
import equinox.task.GetConvTableSheetNames.ConversionTableSheetsRequestingPanel;
import equinox.task.SaveTask;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
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
 * Class for spectrum panel controller.
 *
 * @author Murat Artim
 * @date Jan 17, 2014
 * @time 11:24:23 AM
 */
public class AddSpectrumPanel implements InternalInputSubPanel, ConversionTableSheetsRequestingPanel, SchedulingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Drop zone images. */
	public static final Image DROP_ZONE = Utility.getImage("dropZone.png"), DROP_ZONE_ACTIVE = Utility.getImage("dropActive.gif"), EMPTY = Utility.getImage("empty.png");

	@FXML
	private VBox root_, container_;

	@FXML
	private BorderPane dropZone_;

	@FXML
	private ComboBox<String> sheet_;

	@FXML
	private ImageView dropImage_, ana_, cvt_, fls_, xls_, txt_, spec_;

	@FXML
	private Hyperlink browse_;

	@FXML
	private SplitMenuButton ok_;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		// add change listener to conversion table check image
		xls_.imageProperty().addListener((ChangeListener<Image>) (observable, oldValue, newValue) -> {

			// no value given
			if (newValue == null)
				return;

			// get data
			Path conversionTable = (Path) xls_.getUserData();

			// no data
			if (conversionTable == null) {
				sheet_.getSelectionModel().clearSelection();
				sheet_.setValue(null);
				sheet_.getItems().clear();
				sheet_.setDisable(true);
				return;
			}

			// get worksheet names
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetConvTableSheetNames(AddSpectrumPanel.this, conversionTable));
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
		onResetClicked();
	}

	@Override
	public String getHeader() {
		return "Add New Spectrum";
	}

	@Override
	public void setConversionTableSheetNames(String[] sheetNames) {

		// clear all sheets
		sheet_.getSelectionModel().clearSelection();
		sheet_.setValue(null);
		sheet_.getItems().clear();

		// set sheet names
		sheet_.getItems().setAll(sheetNames);

		// enable menu button
		sheet_.setDisable(false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// get SPEC files
		List<File> specFiles = (List<File>) spec_.getUserData();

		// no SPEC file
		if (specFiles == null || specFiles.isEmpty()) {

			// get files
			Path anaFile = (Path) ana_.getUserData();
			Path cvtFile = (Path) cvt_.getUserData();
			Path flsFile = (Path) fls_.getUserData();
			Path conversionTable = (Path) xls_.getUserData();
			Path txtFile = (Path) txt_.getUserData();

			// get selected conversion table sheet
			String sheet = sheet_.getSelectionModel().getSelectedItem();

			// check inputs
			if (!checkInputs(anaFile, cvtFile, flsFile, conversionTable, txtFile, sheet))
				return;

			// get task manager
			ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

			// run now
			if (runNow) {
				tm.runTaskInParallel(new AddSpectrum(anaFile, txtFile, cvtFile, flsFile, conversionTable, sheet, null));
			}
			else {
				tm.runTaskInParallel(new SaveTask(new AddSpectrum(anaFile, txtFile, cvtFile, flsFile, conversionTable, sheet, null), scheduleDate));
			}
		}

		// there are SPEC files
		else {

			// check inputs
			if (!checkInputs(specFiles))
				return;

			// get task manager
			ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

			// loop over SPEC files
			for (File specFile : specFiles) {

				// run now
				if (runNow) {
					tm.runTaskInParallel(new AddSpectrum(specFile.toPath()));
				}
				else {
					tm.runTaskInParallel(new SaveTask(new AddSpectrum(specFile.toPath()), scheduleDate));
				}
			}
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Processes selected files.
	 *
	 * @param files
	 *            Selected files.
	 * @return True if process completed successfully.
	 */
	@SuppressWarnings("unchecked")
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

			// SPEC
			if (fileType.equals(FileType.SPEC)) {
				owner_.getOwner().setInitialDirectory(file);
				List<File> specFiles = (List<File>) spec_.getUserData();
				if (specFiles == null) {
					specFiles = new ArrayList<>();
					spec_.setUserData(specFiles);
				}
				specFiles.add(file);
				if (!toBeAnimated.contains(spec_)) {
					toBeAnimated.add(spec_);
				}
				success = true;
			}

			// ANA, GZ or ZIP
			else if (fileType.equals(FileType.ANA) || fileType.equals(FileType.GZ) || fileType.equals(FileType.ZIP)) {
				owner_.getOwner().setInitialDirectory(file);
				ana_.setUserData(file.toPath());
				if (!toBeAnimated.contains(ana_)) {
					toBeAnimated.add(ana_);
				}
				success = true;
			}

			// CVT
			else if (fileType.equals(FileType.CVT)) {
				owner_.getOwner().setInitialDirectory(file);
				cvt_.setUserData(file.toPath());
				if (!toBeAnimated.contains(cvt_)) {
					toBeAnimated.add(cvt_);
				}
				success = true;
			}

			// FLS
			else if (fileType.equals(FileType.FLS)) {
				owner_.getOwner().setInitialDirectory(file);
				fls_.setUserData(file.toPath());
				if (!toBeAnimated.contains(fls_)) {
					toBeAnimated.add(fls_);
				}
				success = true;
			}

			// XLS
			else if (fileType.equals(FileType.XLS)) {
				owner_.getOwner().setInitialDirectory(file);
				xls_.setUserData(file.toPath());
				if (!toBeAnimated.contains(xls_)) {
					toBeAnimated.add(xls_);
				}
				success = true;
			}

			// TXT
			else if (fileType.equals(FileType.TXT)) {
				owner_.getOwner().setInitialDirectory(file);
				txt_.setUserData(file.toPath());
				if (!toBeAnimated.contains(txt_)) {
					toBeAnimated.add(txt_);
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
				if (fileType.equals(FileType.ANA) || fileType.equals(FileType.GZ) || fileType.equals(FileType.ZIP) || fileType.equals(FileType.CVT) || fileType.equals(FileType.FLS) || fileType.equals(FileType.XLS) || fileType.equals(FileType.TXT) || fileType.equals(FileType.SPEC)) {
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
				if (fileType.equals(FileType.ANA) || fileType.equals(FileType.GZ) || fileType.equals(FileType.ZIP) || fileType.equals(FileType.CVT) || fileType.equals(FileType.FLS) || fileType.equals(FileType.XLS) || fileType.equals(FileType.TXT) || fileType.equals(FileType.SPEC)) {
					dropImage_.setImage(DROP_ZONE_ACTIVE);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragExited(DragEvent event) {
		dropImage_.setImage(DROP_ZONE);
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
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.getSpectrumFileFilter(false));

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

		// clear all files
		ImageView[] items = { spec_, ana_, txt_, cvt_, fls_, xls_ };
		for (ImageView item : items) {
			item.setUserData(null);
			item.setImage(EMPTY);
		}

		// reset worksheet
		sheet_.getSelectionModel().clearSelection();
		sheet_.setValue(null);
		sheet_.getItems().clear();
		sheet_.setDisable(true);

		// reset hyperlink
		browse_.setVisited(false);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to add new spectrum", null);
	}

	@FXML
	private void onDownloadSampleInputClicked() {
		owner_.getOwner().downloadSampleInput("AddNewSpectrum");
	}

	/**
	 * Checks spectrum bundle inputs.
	 *
	 * @param specFiles
	 *            Spectrum bundles.
	 * @return True if the inputs are acceptable.
	 */
	private boolean checkInputs(List<File> specFiles) {

		// loop over bundles
		for (File file : specFiles) {

			// invalid bundle file
			if (file == null || !file.exists()) {
				String message = "Please select a valid spectrum bundle file to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(spec_);
				return false;
			}
		}

		// valid inputs
		return true;
	}

	/**
	 * Check inputs and shows warning if necessary.
	 *
	 * @param anaFile
	 *            ANA file.
	 * @param cvtFile
	 *            CVT file.
	 * @param flsFile
	 *            FLS file.
	 * @param conversionTable
	 *            Conversion table.
	 * @param txtFile
	 *            TXT file.
	 * @param sheet
	 *            Selected conversion table sheet.
	 * @return True if the inputs are correct.
	 */
	private boolean checkInputs(Path anaFile, Path cvtFile, Path flsFile, Path conversionTable, Path txtFile, String sheet) {

		// check inputs
		String message = null;
		Node node = null;

		// check files
		if (anaFile == null || !Files.exists(anaFile)) {
			message = "Please select a valid ANA file to proceed.";
			node = ana_;
		}
		else if (cvtFile == null || !Files.exists(cvtFile)) {
			message = "Please select a valid CVT file to proceed.";
			node = cvt_;
		}
		else if (flsFile == null || !Files.exists(flsFile)) {
			message = "Please select a valid FLS file to proceed.";
			node = fls_;
		}
		else if (conversionTable == null || !Files.exists(conversionTable)) {
			message = "Please select a valid Conversion table file to proceed.";
			node = xls_;
		}
		else if (sheet == null || sheet.isEmpty()) {
			message = "Please select a valid worksheet from the conversion table to proceed.";
			node = sheet_;
		}
		else if (txtFile != null && !Files.exists(txtFile)) {
			message = "Please select a valid TXT file to proceed.";
			node = txt_;
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
	public static AddSpectrumPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("AddSpectrumPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			AddSpectrumPanel controller = (AddSpectrumPanel) fxmlLoader.getController();

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
