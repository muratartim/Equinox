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
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.SpectrumItem;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.exchangeServer.remote.message.StatusChange;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.DeleteTemporaryFiles;
import equinox.task.InternalEquinoxTask;
import equinox.task.SaveBucketEquivalentStresses;
import equinox.task.SaveEquivalentStresses;
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
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for save equivalent stress info panel controller.
 *
 * @author Murat Artim
 * @date May 6, 2015
 * @time 12:03:56 PM
 */
public class SaveEquivalentStressInfoPanel implements InternalInputSubPanel, ListChangeListener<ExchangeUser>, SchedulingPanel {

	/** Bucket equivalent stress type. */
	public static final int FATIGUE = 0, PREFFAS = 1, LINEAR = 2;

	/** The owner panel. */
	private InputPanel owner_;

	/** Options. */
	private BooleanProperty[] options_;

	/** Panel mode. */
	private boolean isSave_ = true;

	/** Selected bucket equivalent stress type. */
	private int bucketStressType_ = FATIGUE;

	@FXML
	private VBox root_;

	@FXML
	private ToggleSwitch eqStress_, materialName_, materialData_, ppName_, eid_, sequenceName_, spectrumName_, program_, section_, mission_, validity_, maxStress_, minStress_, rRatio_, omission_;

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
		options_ = new BooleanProperty[] { eqStress_.selectedProperty(), materialName_.selectedProperty(), materialData_.selectedProperty(), ppName_.selectedProperty(), eid_.selectedProperty(), sequenceName_.selectedProperty(), spectrumName_.selectedProperty(), program_.selectedProperty(),
				section_.selectedProperty(), mission_.selectedProperty(), validity_.selectedProperty(), maxStress_.selectedProperty(), minStress_.selectedProperty(), rRatio_.selectedProperty(), omission_.selectedProperty() };

		// reset options
		for (BooleanProperty option : options_) {
			option.set(false);
		}
		options_[SaveEquivalentStresses.EQUIVALENT_STRESS].set(true);
		options_[SaveEquivalentStresses.MAT_NAME].set(true);
		options_[SaveEquivalentStresses.PP_NAME].set(true);
		options_[SaveEquivalentStresses.MISSION].set(true);
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

		// get first selected file
		SpectrumItem selected = (SpectrumItem) owner_.getSelectedFiles().get(0);

		// equivalent stress
		if (selected instanceof FatigueEquivalentStress || selected instanceof PreffasEquivalentStress || selected instanceof LinearEquivalentStress) {
			ppName_.setDisable(false);
			spectrumName_.setDisable(false);
			sequenceName_.setDisable(false);
			maxStress_.setDisable(false);
			minStress_.setDisable(false);
			rRatio_.setDisable(false);
		}

		// external equivalent stress
		else if (selected instanceof ExternalFatigueEquivalentStress || selected instanceof ExternalPreffasEquivalentStress || selected instanceof ExternalLinearEquivalentStress) {
			ppName_.setDisable(true);
			spectrumName_.setDisable(true);
			ppName_.setSelected(false);
			spectrumName_.setSelected(false);
			sequenceName_.setDisable(false);
			maxStress_.setDisable(false);
			minStress_.setDisable(false);
			rRatio_.setDisable(false);
		}

		// fast equivalent stress
		else if (selected instanceof FastFatigueEquivalentStress || selected instanceof FastPreffasEquivalentStress || selected instanceof FastLinearEquivalentStress) {
			ppName_.setDisable(false);
			spectrumName_.setDisable(false);
			sequenceName_.setDisable(true);
			maxStress_.setDisable(true);
			minStress_.setDisable(true);
			rRatio_.setDisable(true);
			sequenceName_.setSelected(false);
			maxStress_.setSelected(false);
			minStress_.setSelected(false);
			rRatio_.setSelected(false);
		}

		// STF file bucket
		else if (selected instanceof STFFileBucket) {
			ppName_.setDisable(false);
			spectrumName_.setDisable(false);
			sequenceName_.setDisable(true);
			maxStress_.setDisable(true);
			minStress_.setDisable(true);
			rRatio_.setDisable(true);
			sequenceName_.setSelected(false);
			maxStress_.setSelected(false);
			minStress_.setSelected(false);
			rRatio_.setSelected(false);
		}
	}

	@Override
	public String getHeader() {
		return isSave_ ? "Save Eq. Stress" : "Share Eq. Stress";
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

	/**
	 * Sets bucket equivalent stress type.
	 *
	 * @param bucketStressType
	 *            Bucket equivalent stress type.
	 */
	public void setBucketStressType(int bucketStressType) {
		bucketStressType_ = bucketStressType;
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
	 * Shares equivalent stress.
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

		// get selected recipients
		ObservableList<ExchangeUser> recipients = recipients_.getSelectionModel().getSelectedItems();

		// check inputs
		if (!checkInputs(recipients))
			return;

		// create working directory
		Path workingDirectory = createWorkingDirectory("ShareEquivalentStress");
		if (workingDirectory == null)
			return;

		// create output file
		Path output = workingDirectory.resolve("Equivalent Stresses.xls");

		// get selected files
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();
		boolean isBucketSTF = false;
		for (TreeItem<String> file : selected) {
			if (file instanceof STFFileBucket) {
				isBucketSTF = true;
				break;
			}
		}

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		InternalEquinoxTask<?> saveTask = null;

		// bucket STF files
		if (isBucketSTF) {

			// get selected STF file buckets
			ArrayList<STFFileBucket> buckets = new ArrayList<>();
			for (TreeItem<String> item : selected) {
				buckets.add((STFFileBucket) item);
			}

			// create task
			saveTask = new SaveBucketEquivalentStresses(buckets, options_, output.toFile(), bucketStressType_);
		}

		// equivalent stresses
		else {

			// get selected equivalent stresses
			ArrayList<SpectrumItem> stresses = new ArrayList<>();
			for (TreeItem<String> item : selected) {
				stresses.add((SpectrumItem) item);
			}

			// create task
			saveTask = new SaveEquivalentStresses(stresses, options_, output.toFile());
		}

		// create share and delete tasks
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

		// check options
		boolean noSelection = true;
		for (BooleanProperty option : options_) {
			if (option.get()) {
				noSelection = false;
				break;
			}
		}

		// no option selected
		if (noSelection) {
			String message = "Please select at least 1 option to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(eqStress_);
			return false;
		}

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
			popOver.show(recipients_);
			return false;
		}

		// acceptable inputs
		return true;
	}

	/**
	 * Saves equivalent stress.
	 *
	 * @param runNow
	 *            True if task(s) should be run right now.
	 * @param scheduleDate
	 *            Schedule date (can be null).
	 */
	private void save(boolean runNow, Date scheduleDate) {

		// check inputs
		if (!checkInputs())
			return;

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.XLS.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName("Equivalent Stresses.xls");
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File output = FileType.appendExtension(selectedFile, FileType.XLS);

		// get selected files
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();
		boolean isBucketSTF = false;
		for (TreeItem<String> file : selected) {
			if (file instanceof STFFileBucket) {
				isBucketSTF = true;
				break;
			}
		}

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// bucket STF files
		if (isBucketSTF) {

			// get selected STF file buckets
			ArrayList<STFFileBucket> buckets = new ArrayList<>();
			for (TreeItem<String> item : selected) {
				buckets.add((STFFileBucket) item);
			}

			// run now
			if (runNow) {
				tm.runTaskInParallel(new SaveBucketEquivalentStresses(buckets, options_, output, bucketStressType_));
			}
			else {
				tm.runTaskInParallel(new SaveTask(new SaveBucketEquivalentStresses(buckets, options_, output, bucketStressType_), scheduleDate));
			}
		}

		// equivalent stresses
		else {

			// get selected equivalent stresses
			ArrayList<SpectrumItem> items = new ArrayList<>();
			for (TreeItem<String> item : selected) {
				items.add((SpectrumItem) item);
			}

			// run now
			if (runNow) {
				tm.runTaskInParallel(new SaveEquivalentStresses(items, options_, output));
			}
			else {
				tm.runTaskInParallel(new SaveTask(new SaveEquivalentStresses(items, options_, output), scheduleDate));
			}
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Checks inputs.
	 *
	 * @return True if inputs are valid.
	 */
	private boolean checkInputs() {

		// check options
		boolean noSelection = true;
		for (BooleanProperty option : options_) {
			if (option.get()) {
				noSelection = false;
				break;
			}
		}

		// no option selected
		if (noSelection) {
			String message = "Please select at least 1 option to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(eqStress_);
			return false;
		}

		// inputs are valid
		return true;
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {

		// get first selected file
		SpectrumItem selected = (SpectrumItem) owner_.getSelectedFiles().get(0);

		// loop over options
		for (int i = 0; i < options_.length; i++) {
			if (i == SaveEquivalentStresses.EQUIVALENT_STRESS) {
				if (!options_[i].get()) {
					options_[i].set(true);
				}
			}
			else if (i == SaveEquivalentStresses.MAT_NAME) {
				if (!options_[i].get()) {
					options_[i].set(true);
				}
			}
			else if (i == SaveEquivalentStresses.MISSION) {
				if (!options_[i].get()) {
					options_[i].set(true);
				}
			}
			else if (i == SaveEquivalentStresses.PP_NAME) {
				if (selected instanceof FatigueEquivalentStress || selected instanceof PreffasEquivalentStress || selected instanceof LinearEquivalentStress || selected instanceof FastFatigueEquivalentStress || selected instanceof FastPreffasEquivalentStress
						|| selected instanceof FastLinearEquivalentStress || selected instanceof STFFileBucket) {
					if (!options_[i].get()) {
						options_[i].set(true);
					}
				}
			}
			else if (i == SaveEquivalentStresses.SEQ_NAME) {
				if (selected instanceof ExternalFatigueEquivalentStress || selected instanceof ExternalPreffasEquivalentStress || selected instanceof ExternalLinearEquivalentStress) {
					if (!options_[i].get()) {
						options_[i].set(true);
					}
				}
			}
			else {
				options_[i].set(false);
			}
		}
		recipients_.getSelectionModel().clearSelection();
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to save equivalent stresses", null);
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static SaveEquivalentStressInfoPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("SaveEquivalentStressInfoPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			SaveEquivalentStressInfoPanel controller = (SaveEquivalentStressInfoPanel) fxmlLoader.getController();

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
