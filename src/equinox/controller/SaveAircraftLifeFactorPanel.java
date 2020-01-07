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
import java.util.ResourceBundle;
import java.util.logging.Level;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import equinox.Equinox;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.exchangeServer.remote.message.StatusChange;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.DeleteTemporaryFiles;
import equinox.task.GetMissions;
import equinox.task.GetMissions.MissionRequestingPanel;
import equinox.task.SaveAircraftLifeFactors;
import equinox.task.SaveTask;
import equinox.task.ShareGeneratedItem;
import equinox.utility.Utility;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for save A/C model life factor panel controller.
 *
 * @author Murat Artim
 * @date Oct 1, 2015
 * @time 4:25:21 PM
 */
public class SaveAircraftLifeFactorPanel implements InternalInputSubPanel, ListChangeListener<ExchangeUser>, MissionRequestingPanel, SchedulingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Options. */
	private BooleanProperty[] options_;

	/** Panel mode. */
	private boolean isSave_ = true;

	@FXML
	private VBox root_;

	@FXML
	private ComboBox<String> basisMission_;

	@FXML
	private ToggleSwitch fatStress_, propStress_, fatP_, elberM_, eid_, mission_;

	@FXML
	private SplitMenuButton ok_;

	@FXML
	private ListView<ExchangeUser> recipients_;

	@FXML
	private TitledPane recipientsPane_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup options
		options_ = new BooleanProperty[] { fatStress_.selectedProperty(), propStress_.selectedProperty(), fatP_.selectedProperty(), elberM_.selectedProperty(), eid_.selectedProperty(), mission_.selectedProperty() };

		// reset options
		for (BooleanProperty option : options_) {
			if (option.equals(options_[SaveAircraftLifeFactors.FAT_FACTOR])) {
				option.set(true);
			}
			else if (option.equals(options_[SaveAircraftLifeFactors.PROP_FACTOR])) {
				option.set(true);
			}
			else if (option.equals(options_[SaveAircraftLifeFactors.EID])) {
				option.set(true);
			}
			else if (option.equals(options_[SaveAircraftLifeFactors.MISSION])) {
				option.set(true);
			}
			else {
				option.set(false);
			}
		}
		recipients_.getSelectionModel().clearSelection();

		// set multiple selection
		recipients_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@Override
	public void onChanged(javafx.collections.ListChangeListener.Change<? extends ExchangeUser> c) {

		// get currently selected recipients
		ObservableList<ExchangeUser> selected = recipients_.getSelectionModel().getSelectedItems();

		// add new recipients
		recipients_.getItems().setAll(c.getList());

		// make previous selections
		recipients_.getSelectionModel().clearSelection();
		for (ExchangeUser recipient : selected) {
			recipients_.getSelectionModel().select(recipient);
		}
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public void showing() {

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(0));

		// enable/disable recipients pane
		recipientsPane_.setDisable(isSave_);

		// get selected equivalent stress
		AircraftFatigueEquivalentStress selected = (AircraftFatigueEquivalentStress) owner_.getSelectedFiles().get(0);

		// get element groups and missions
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetMissions(this, selected));
	}

	@Override
	public void setMissions(ArrayList<String> missions) {
		basisMission_.getItems().setAll(missions);
		basisMission_.getSelectionModel().clearSelection();
	}

	@Override
	public String getHeader() {
		return isSave_ ? "Save Life Factor" : "Share Life Factor";
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// save mode
		if (isSave_) {
			save(runNow, scheduleDate);
		}
		else {
			share(runNow, scheduleDate);
		}
	}

	/**
	 * Sets panel mode.
	 *
	 * @param isSave
	 *            True to save, false to share.
	 */
	public void setMode(boolean isSave) {
		isSave_ = isSave;
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
	 * Saves A/C life factors.
	 *
	 * @param runNow
	 *            True if task(s) should be run right now.
	 * @param scheduleDate
	 *            Schedule date (can be null).
	 */
	private void save(boolean runNow, Date scheduleDate) {

		// no basis mission selected
		String basisMission = basisMission_.getSelectionModel().getSelectedItem();
		if (basisMission == null) {
			String message = "Please select basis fatigue mission for comparison.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(basisMission_);
			return;
		}

		// no option selected
		boolean noSelection = true;
		for (BooleanProperty option : options_) {
			if (option.get()) {
				noSelection = false;
				break;
			}
		}
		if (noSelection) {
			String message = "Please select at least 1 option to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(ok_);
			return;
		}

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.XLS.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName("Life factors.xls");
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File output = FileType.appendExtension(selectedFile, FileType.XLS);

		// get selected equivalent stress
		AircraftFatigueEquivalentStress eqStress = (AircraftFatigueEquivalentStress) owner_.getSelectedFiles().get(0);

		// nothing selected
		if (eqStress == null)
			return;

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// run now
		if (runNow) {
			tm.runTaskInParallel(new SaveAircraftLifeFactors(eqStress, options_, output, basisMission));
		}
		else {
			tm.runTaskInParallel(new SaveTask(new SaveAircraftLifeFactors(eqStress, options_, output, basisMission), scheduleDate));
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Shares A/C life factors.
	 *
	 * @param runNow
	 *            True if task(s) should be run right now.
	 * @param scheduleDate
	 *            Schedule date (can be null).
	 */
	private void share(boolean runNow, Date scheduleDate) {

		// has no permission
		if (!Equinox.USER.hasPermission(Permission.SHARE_FILE, true, owner_.getOwner()))
			return;

		// no basis mission selected
		String basisMission = basisMission_.getSelectionModel().getSelectedItem();
		if (basisMission == null) {
			String message = "Please select basis fatigue mission for comparison.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(basisMission_);
			return;
		}

		// no option selected
		boolean noSelection = true;
		for (BooleanProperty option : options_) {
			if (option.get()) {
				noSelection = false;
				break;
			}
		}
		if (noSelection) {
			String message = "Please select at least 1 option to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(ok_);
			return;
		}

		// get selected recipients
		ObservableList<ExchangeUser> recipients = recipients_.getSelectionModel().getSelectedItems();

		// check inputs
		if (!checkInputs(recipients))
			return;

		// create working directory
		Path workingDirectory = createWorkingDirectory("ShareAircraftLifeFactor");
		if (workingDirectory == null)
			return;

		// create output file
		Path output = workingDirectory.resolve("Life Factors.xls");

		// get selected equivalent stress
		AircraftFatigueEquivalentStress eqStress = (AircraftFatigueEquivalentStress) owner_.getSelectedFiles().get(0);

		// nothing selected
		if (eqStress == null)
			return;

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// create tasks
		SaveAircraftLifeFactors saveTask = new SaveAircraftLifeFactors(eqStress, options_, output.toFile(), basisMission);
		ShareGeneratedItem share = new ShareGeneratedItem(output, new ArrayList<>(recipients));
		DeleteTemporaryFiles delete = new DeleteTemporaryFiles(workingDirectory, null);

		// run now
		if (runNow) {
			tm.runTasksSequentially(saveTask, share, delete);
		}
		else {
			tm.runTasksSequentially(saveTask, new SaveTask(share, scheduleDate));
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Creates working directory.
	 *
	 * @param name
	 *            Name of directory.
	 * @return Path to working directory, or null if directory could not be created.
	 */
	private Path createWorkingDirectory(String name) {

		// create directory
		try {
			return Utility.createWorkingDirectory(name);
		}

		// exception occurred during process
		catch (IOException e) {

			// create error message
			String message = "Exception occurred during creating working directory for the process. ";

			// log exception
			Equinox.LOGGER.log(Level.WARNING, message, e);

			// show error message
			message += e.getLocalizedMessage();
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(ok_);
			return null;
		}
	}

	/**
	 * Checks message inputs and displays warning message if needed.
	 *
	 * @param selected
	 *            Selected recipients to share.
	 * @return True if message is acceptable.
	 */
	private boolean checkInputs(ObservableList<ExchangeUser> selected) {

		// this user is not available
		if (!owner_.getOwner().isAvailable()) {

			// create confirmation action
			PopOver popOver = new PopOver();
			EventHandler<ActionEvent> handler = event -> {
				owner_.getOwner().getExchangeServerManager().sendMessage(new StatusChange(Equinox.USER.createExchangeUser(), true));
				popOver.hide();
			};

			// show question
			String warning = "Your status is currently set to 'Busy'. Would you like to set it to 'Available' to share file?";
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel2.load(popOver, warning, 50, "Yes", handler, NotificationPanel2.QUESTION));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(ok_);
			return false;
		}

		// no recipients
		else if (selected.isEmpty()) {
			String warning = "Please select at least 1 recipient to share file.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(warning, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(ok_);
			return false;
		}

		// acceptable inputs
		return true;
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {

		// reset basis mission
		basisMission_.setValue(null);
		basisMission_.getSelectionModel().clearSelection();

		// reset options
		for (BooleanProperty option : options_) {
			if (option.equals(options_[SaveAircraftLifeFactors.FAT_FACTOR])) {
				option.set(true);
			}
			else if (option.equals(options_[SaveAircraftLifeFactors.PROP_FACTOR])) {
				option.set(true);
			}
			else if (option.equals(options_[SaveAircraftLifeFactors.EID])) {
				option.set(true);
			}
			else if (option.equals(options_[SaveAircraftLifeFactors.MISSION])) {
				option.set(true);
			}
			else {
				option.set(false);
			}
		}
		recipients_.getSelectionModel().clearSelection();
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to save aircraft life factors", null);
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static SaveAircraftLifeFactorPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("SaveAircraftLifeFactorPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			SaveAircraftLifeFactorPanel controller = (SaveAircraftLifeFactorPanel) fxmlLoader.getController();

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
