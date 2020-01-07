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
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import equinox.Equinox;
import equinox.controller.DownloadViewPanel.DownloadItemPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.Spectrum;
import equinox.dataServer.remote.data.DownloadInfo;
import equinox.dataServer.remote.data.MultiplicationTableInfo.MultiplicationTableInfoType;
import equinox.dataServer.remote.data.MultiplicationTableSearchInput;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.data.SearchItem;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.dataServer.remote.data.SpectrumSearchInput;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.AdvancedMultiplicationTableSearch;
import equinox.task.AdvancedSpectrumSearch;
import equinox.task.DeletePilotPointFromGlobalDB;
import equinox.task.DownloadPilotPoint;
import equinox.task.DownloadPilotPointAttributes;
import equinox.task.DownloadPilotPointImages;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;

/**
 * Class for download pilot point panel controller.
 *
 * @author Murat Artim
 * @date Feb 12, 2016
 * @time 4:00:03 PM
 */
public class DownloadPilotPointPanel implements Initializable, DownloadItemPanel {

	/** The owner panel. */
	private DownloadViewPanel owner_;

	/** Pilot point info. */
	private PilotPointInfo info_;

	@FXML
	private HBox root_;

	@FXML
	private Button infoButton_, addButton_, delete_;

	@FXML
	private ToggleSwitch select_;

	@FXML
	private Label title_, infoLabel_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set listener
		select_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> owner_.setupControls());
	}

	@Override
	public void start() {

		// remove delete button if not administrator or logged in
		if (!Equinox.USER.hasPermission(Permission.DELETE_PILOT_POINT, false, null) || !Equinox.USER.isLoggedAsAdministrator()) {
			root_.getChildren().remove(delete_);
		}
	}

	@Override
	public String getHeader() {
		return "Pilot Points";
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
	public HBox getRoot() {
		return root_;
	}

	@Override
	public ToggleSwitch getSelectSwitch() {
		return select_;
	}

	@Override
	public DownloadInfo getInfo() {
		return info_;
	}

	@Override
	public void download(Path directory, int index) {

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();

		// create path to temporary file
		String name = (String) info_.getInfo(PilotPointInfoType.NAME);
		Path output = directory.resolve(name + "_" + index + FileType.ZIP.getExtension());

		// add task
		tm.runTaskInParallel(new DownloadPilotPoint(info_, output, null));
	}

	@Override
	public void add() {

		// get spectra
		String spectrumName = (String) info_.getInfo(PilotPointInfoType.SPECTRUM_NAME);
		Spectrum selected = null;
		TreeItem<String> root = owner_.getOwner().getOwner().getInputPanel().getFileTreeRoot();
		for (TreeItem<String> item : root.getChildren())
			if (item instanceof Spectrum) {
				Spectrum spectrum = (Spectrum) item;
				if (spectrum.getName().equals(spectrumName)) {
					selected = spectrum;
				}
			}

		// no spectrum found to add the pilot point
		if (selected == null) {
			String message = "No spectrum found in local database to add the pilot point '";
			message += (String) info_.getInfo(PilotPointInfoType.NAME) + "'. Pilot point not added.";
			owner_.getOwner().getOwner().getNotificationPane().showWarning(message, null);
		}

		// add pilot point
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskSequentially(new DownloadPilotPoint(info_, null, selected));
	}

	@Override
	public void delete() {
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new DeletePilotPointFromGlobalDB(info_, owner_));
	}

	@Override
	public void setInfo(DownloadInfo info) {

		// set info
		info_ = (PilotPointInfo) info;

		// set title
		title_.setText((String) info_.getInfo(PilotPointInfoType.NAME));

		// set info label
		String label = (String) info_.getInfo(PilotPointInfoType.AC_PROGRAM);
		label += ", " + (String) info_.getInfo(PilotPointInfoType.AC_SECTION);
		label += ", " + (String) info_.getInfo(PilotPointInfoType.FAT_MISSION);
		label += ", " + (String) info_.getInfo(PilotPointInfoType.DESCRIPTION);
		label += ", " + (String) info_.getInfo(PilotPointInfoType.DELIVERY_REF_NUM);
		infoLabel_.setText(label);
	}

	@Override
	public boolean canBeDownloaded() {
		return true;
	}

	@Override
	public boolean canBeAdded() {
		return true;
	}

	@Override
	public boolean canBeDeleted() {
		return root_.getChildren().contains(delete_);
	}

	@FXML
	private void onInfoClicked() {

		// check if the current user can edit pilot point info
		boolean canEdit = false;
		if (Equinox.USER.hasPermission(Permission.UPDATE_PILOT_POINT_INFO, false, null) && Equinox.USER.hasPermission(Permission.SAVE_PILOT_POINT_IMAGE, false, null)) {
			canEdit = true;
		}

		// show info
		PopOver popOver = new PopOver();
		double buttonY = infoButton_.localToScreen(infoButton_.getBoundsInLocal()).getMinY();
		double height = Screen.getPrimary().getBounds().getHeight();
		popOver.setArrowLocation(buttonY <= height * 0.5 ? ArrowLocation.TOP_RIGHT : ArrowLocation.BOTTOM_RIGHT);
		popOver.setDetachable(true);
		popOver.setContentNode(DownloadPilotPointInfoPanel.load(info_, this, canEdit, popOver));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.setAutoFix(false);
		popOver.setTitle((String) info_.getInfo(PilotPointInfoType.NAME));
		popOver.show(infoButton_);
	}

	@FXML
	private void onDownloadClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getOwner().getFileChooser(FileType.ZIP.getExtensionFilter());

		// show save dialog
		String name = (String) info_.getInfo(PilotPointInfoType.NAME);
		fileChooser.setInitialFileName(name + FileType.ZIP.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File output = FileType.appendExtension(selectedFile, FileType.ZIP);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new DownloadPilotPoint(info_, output.toPath(), null));
	}

	@FXML
	private void onDownloadImagesClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getOwner().getFileChooser(FileType.ZIP.getExtensionFilter());

		// show save dialog
		String name = (String) info_.getInfo(PilotPointInfoType.NAME);
		fileChooser.setInitialFileName(name + "_images" + FileType.ZIP.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File output = FileType.appendExtension(selectedFile, FileType.ZIP);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new DownloadPilotPointImages(info_, output.toPath()));
	}

	@FXML
	private void onDownloadAttributesClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getOwner().getFileChooser(FileType.ZIP.getExtensionFilter());

		// show save dialog
		String name = (String) info_.getInfo(PilotPointInfoType.NAME);
		fileChooser.setInitialFileName(name + "_attributes" + FileType.ZIP.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File output = FileType.appendExtension(selectedFile, FileType.ZIP);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new DownloadPilotPointAttributes(info_, output.toPath()));
	}

	@FXML
	private void onAddClicked() {
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
		popOver.setDetachable(false);
		ArrayList<PilotPointInfo> info = new ArrayList<>();
		info.add(info_);
		popOver.setContentNode(AddSTFPanel.load(info, getOwner().getOwner().getOwner(), popOver));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(addButton_);
	}

	@FXML
	private void onSearchSpectrumClicked() {

		// create search input
		SpectrumSearchInput input = new SpectrumSearchInput();

		// get search info
		String spectrumName = (String) info_.getInfo(PilotPointInfoType.SPECTRUM_NAME);
		String program = (String) info_.getInfo(PilotPointInfoType.AC_PROGRAM);
		String section = (String) info_.getInfo(PilotPointInfoType.AC_SECTION);

		// add inputs
		input.addInput(SpectrumInfoType.NAME, new SearchItem(spectrumName, 1));
		input.addInput(SpectrumInfoType.AC_PROGRAM, new SearchItem(program, 1));
		input.addInput(SpectrumInfoType.AC_SECTION, new SearchItem(section, 1));

		// set engine settings
		SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) owner_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
		panel.setEngineSettings(input);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new AdvancedSpectrumSearch(input));
	}

	@FXML
	private void onSearchMultiplicationTablesClicked() {

		// create search input
		MultiplicationTableSearchInput input = new MultiplicationTableSearchInput();

		// get search info
		String ppName = (String) info_.getInfo(PilotPointInfoType.NAME);
		String program = (String) info_.getInfo(PilotPointInfoType.AC_PROGRAM);
		String section = (String) info_.getInfo(PilotPointInfoType.AC_SECTION);

		// add inputs
		input.addInput(MultiplicationTableInfoType.PILOT_POINT_NAME, new SearchItem(ppName, 1));
		input.addInput(MultiplicationTableInfoType.AC_PROGRAM, new SearchItem(program, 1));
		input.addInput(MultiplicationTableInfoType.AC_SECTION, new SearchItem(section, 1));

		// set engine settings
		SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) owner_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
		panel.setEngineSettings(input);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new AdvancedMultiplicationTableSearch(input));
	}

	@FXML
	private void onDeleteClicked() {

		// create confirmation action
		PopOver popOver = new PopOver();
		EventHandler<ActionEvent> handler = event -> {

			// get task panel
			ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();

			// delete
			tm.runTaskInParallel(new DeletePilotPointFromGlobalDB(info_, owner_));

			// hide pop-over
			popOver.hide();
		};

		// show message
		String message = "Are you sure you want to delete the pilot point from ESCSAS global database?";
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
	public static DownloadPilotPointPanel load(DownloadViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadPilotPointPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			DownloadPilotPointPanel controller = (DownloadPilotPointPanel) fxmlLoader.getController();

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
