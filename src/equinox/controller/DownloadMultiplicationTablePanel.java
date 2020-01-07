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
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import equinox.Equinox;
import equinox.controller.DownloadViewPanel.DownloadItemPanel;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.DownloadInfo;
import equinox.dataServer.remote.data.MultiplicationTableInfo;
import equinox.dataServer.remote.data.MultiplicationTableInfo.MultiplicationTableInfoType;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.data.PilotPointSearchInput;
import equinox.dataServer.remote.data.SearchItem;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.dataServer.remote.data.SpectrumSearchInput;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.AdvancedPilotPointSearch;
import equinox.task.AdvancedSpectrumSearch;
import equinox.task.DeleteMultiplicationTableFromGlobalDB;
import equinox.task.DownloadMultiplicationTable;
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
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;

/**
 * Class for download multiplication table panel controller.
 *
 * @author Murat Artim
 * @date Feb 29, 2016
 * @time 2:18:39 PM
 */
public class DownloadMultiplicationTablePanel implements Initializable, DownloadItemPanel {

	/** The owner panel. */
	private DownloadViewPanel owner_;

	/** Multiplication table info. */
	private MultiplicationTableInfo info_;

	@FXML
	private HBox root_;

	@FXML
	private Button infoButton_, delete_;

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
		if (!Equinox.USER.hasPermission(Permission.DELETE_MULTIPLICATION_TABLE, false, null) || !Equinox.USER.isLoggedAsAdministrator()) {
			root_.getChildren().remove(delete_);
		}
	}

	@Override
	public String getHeader() {
		return "Loadcase Factors Files";
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
		String name = (String) info_.getInfo(MultiplicationTableInfoType.NAME);
		Path output = directory.resolve(name + "_" + index + FileType.ZIP.getExtension());

		// add task
		tm.runTaskInParallel(new DownloadMultiplicationTable(info_, output));
	}

	@Override
	public void add() {
		// no implementation
	}

	@Override
	public void delete() {
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new DeleteMultiplicationTableFromGlobalDB(info_, owner_));
	}

	@Override
	public void setInfo(DownloadInfo info) {

		// set info
		info_ = (MultiplicationTableInfo) info;

		// set title
		title_.setText((String) info_.getInfo(MultiplicationTableInfoType.NAME));

		// set info label
		String label = (String) info_.getInfo(MultiplicationTableInfoType.SPECTRUM_NAME);
		label += ", " + (String) info_.getInfo(MultiplicationTableInfoType.AC_PROGRAM);
		label += ", " + (String) info_.getInfo(MultiplicationTableInfoType.AC_SECTION);
		label += ", " + (String) info_.getInfo(MultiplicationTableInfoType.FAT_MISSION);

		infoLabel_.setText(label);
	}

	@Override
	public boolean canBeDownloaded() {
		return true;
	}

	@Override
	public boolean canBeAdded() {
		return false;
	}

	@Override
	public boolean canBeDeleted() {
		return root_.getChildren().contains(delete_);
	}

	@FXML
	private void onInfoClicked() {

		// check if the current user is administrator
		boolean canEdit = Equinox.USER.hasPermission(Permission.UPDATE_MULTIPLICATION_TABLE_INFO, false, null);

		// show info
		PopOver popOver = new PopOver();
		double buttonY = infoButton_.localToScreen(infoButton_.getBoundsInLocal()).getMinY();
		double height = Screen.getPrimary().getBounds().getHeight();
		popOver.setArrowLocation(buttonY <= height * 0.5 ? ArrowLocation.TOP_RIGHT : ArrowLocation.BOTTOM_RIGHT);
		popOver.setDetachable(true);
		popOver.setContentNode(canEdit ? DownloadMultiplicationTableInfoEditPanel.load(info_, popOver, this) : DownloadMultiplicationTableInfoPanel.load(info_, popOver));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.setAutoFix(false);
		popOver.setTitle((String) info_.getInfo(MultiplicationTableInfoType.NAME));
		popOver.show(infoButton_);
	}

	@FXML
	private void onDownloadClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getOwner().getFileChooser(FileType.ZIP.getExtensionFilter());

		// show save dialog
		String name = (String) info_.getInfo(MultiplicationTableInfoType.NAME);
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
		tm.runTaskInParallel(new DownloadMultiplicationTable(info_, output.toPath()));
	}

	@FXML
	private void onSearchPilotPointsClicked() {

		// create search input
		PilotPointSearchInput input = new PilotPointSearchInput();

		// get search info
		String spectrumName = (String) info_.getInfo(MultiplicationTableInfoType.SPECTRUM_NAME);
		String ppName = (String) info_.getInfo(MultiplicationTableInfoType.PILOT_POINT_NAME);
		String program = (String) info_.getInfo(MultiplicationTableInfoType.AC_PROGRAM);
		String section = (String) info_.getInfo(MultiplicationTableInfoType.AC_SECTION);

		// add inputs
		input.addInput(PilotPointInfoType.SPECTRUM_NAME, new SearchItem(spectrumName, 1));
		input.addInput(PilotPointInfoType.NAME, new SearchItem(ppName, 1));
		input.addInput(PilotPointInfoType.AC_PROGRAM, new SearchItem(program, 1));
		input.addInput(PilotPointInfoType.AC_SECTION, new SearchItem(section, 1));

		// set engine settings
		SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) owner_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
		panel.setEngineSettings(input);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new AdvancedPilotPointSearch(input));
	}

	@FXML
	private void onSearchSpectrumClicked() {

		// create search input
		SpectrumSearchInput input = new SpectrumSearchInput();

		// get search info
		String spectrumName = (String) info_.getInfo(MultiplicationTableInfoType.SPECTRUM_NAME);
		String program = (String) info_.getInfo(MultiplicationTableInfoType.AC_PROGRAM);
		String section = (String) info_.getInfo(MultiplicationTableInfoType.AC_SECTION);

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
	private void onDeleteClicked() {

		// create confirmation action
		PopOver popOver = new PopOver();
		EventHandler<ActionEvent> handler = event -> {

			// get task panel
			ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();

			// delete
			tm.runTaskInParallel(new DeleteMultiplicationTableFromGlobalDB(info_, owner_));

			// hide pop-over
			popOver.hide();
		};

		// show message
		String message = "Are you sure you want to delete the loadcase factors file from ESCSAS global database?";
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
	public static DownloadMultiplicationTablePanel load(DownloadViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadMultiplicationTablePanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			DownloadMultiplicationTablePanel controller = (DownloadMultiplicationTablePanel) fxmlLoader.getController();

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
