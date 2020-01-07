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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.Equinox;
import equinox.controller.InputPanel.InputPopup;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.fileType.AircraftModel;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.fileType.StressSequence;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.exchangeServer.remote.message.StatusChange;
import equinox.plugin.FileType;
import equinox.process.SaveBucketOutputFilesProcess;
import equinox.task.DeleteTemporaryFiles;
import equinox.task.InternalEquinoxTask;
import equinox.task.SaveAircraftEquivalentStress;
import equinox.task.SaveAndZipBucketOutputFiles;
import equinox.task.SaveExternalFLS;
import equinox.task.SaveExternalStressSequenceAsSTH;
import equinox.task.SaveLoadCase;
import equinox.task.SaveOutputFile;
import equinox.task.SaveRainflow;
import equinox.task.SaveStressSequenceAsSTH;
import equinox.task.ShareAircraftModel;
import equinox.task.ShareAircraftModelFile;
import equinox.task.ShareGeneratedItem;
import equinox.task.ShareSTF;
import equinox.task.ShareSTFBucket;
import equinox.task.ShareSpectrum;
import equinox.task.ShareSpectrumFile;
import equinox.utility.Utility;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;

/**
 * Class for share file panel controller.
 *
 * @author Murat Artim
 * @date Sep 23, 2014
 * @time 10:30:59 AM
 */
public class ShareFilePopup implements InputPopup, ListChangeListener<ExchangeUser> {

	/** The owner panel. */
	private InputPanel owner_;

	/** Item to share. */
	private SpectrumItem item_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	/** File type of the core spectrum file to be shared. */
	private FileType fileType_;

	/** Additional parameters. */
	private String[] params_;

	@FXML
	private VBox root_, container_;

	@FXML
	private Button share_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public void onChanged(javafx.collections.ListChangeListener.Change<? extends ExchangeUser> c) {

		// get currently selected recipients
		ArrayList<ExchangeUser> selected = getSelectedRecipients();

		// remove all current recipients
		container_.getChildren().clear();

		// add new recipients
		ObservableList<? extends ExchangeUser> list = c.getList();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			ExchangeUser recipient = list.get(i);
			ToggleButton button = new ToggleButton(recipient.toString());
			button.setUserData(recipient);
			button.setMaxWidth(Double.MAX_VALUE);
			if (size == 1) {
				button.getStylesheets().add(Equinox.class.getResource("css/ToggleButton2.css").toString());
			}
			else if (i == size - 1) {
				button.getStylesheets().add(Equinox.class.getResource("css/ToggleButton2.css").toString());
			}
			else {
				button.getStylesheets().add(Equinox.class.getResource("css/ToggleButton.css").toString());
			}
			if (selected.contains(recipient)) {
				button.setSelected(true);
			}
			container_.getChildren().add(button);
		}
	}

	/**
	 * Shows share file panel.
	 *
	 * @param item
	 *            Item to share.
	 * @param fileType
	 *            File type of file to share. Can be null if only spectrum item is sufficient.
	 * @param params
	 *            Additional parameters.
	 */
	public void show(SpectrumItem item, FileType fileType, String... params) {

		// not shown
		if (!isShown_) {

			// no recipient
			if (container_.getChildren().isEmpty()) {

				// create popup
				String message = "There is no available user to share file.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.INFO));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);

				// show popup
				FileViewPanel panel = (FileViewPanel) owner_.getSubPanel(InputPanel.FILE_VIEW_PANEL);
				panel.showPopOverOnSelectedItem(popOver, "Share File");
				return;
			}

			// set item
			item_ = item;
			fileType_ = fileType;
			params_ = params;

			// create pop-over
			popOver_ = new PopOver();
			popOver_.setArrowLocation(ArrowLocation.TOP_CENTER);
			popOver_.setDetachable(false);
			popOver_.setContentNode(root_);
			popOver_.setHideOnEscape(true);
			popOver_.setAutoHide(true);

			// set showing handler
			popOver_.setOnShowing(event -> isShown_ = true);

			// set hidden handler
			popOver_.setOnHidden(event -> isShown_ = false);

			// clear all selections
			for (Node recipient : container_.getChildren()) {
				((ToggleButton) recipient).setSelected(false);
			}

			// show
			FileViewPanel panel = (FileViewPanel) owner_.getSubPanel(InputPanel.FILE_VIEW_PANEL);
			panel.showPopOverOnSelectedItem(popOver_, "Share File");
		}
	}

	/**
	 * Returns true if this panel is shown.
	 *
	 * @return True if this panel is shown.
	 */
	public boolean isShown() {
		return isShown_;
	}

	@FXML
	private void onShareClicked() {

		// get inputs
		ArrayList<ExchangeUser> recipients = getSelectedRecipients();

		// check inputs
		if (!checkInputs(recipients))
			return;

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// spectrum
		if (item_ instanceof Spectrum && fileType_ == null) {
			tm.runTaskInParallel(new ShareSpectrum((Spectrum) item_, recipients));
		}

		// spectrum file
		else if (item_ instanceof Spectrum && fileType_ != null) {
			tm.runTaskInParallel(new ShareSpectrumFile((Spectrum) item_, fileType_, recipients));
		}

		// aircraft model
		else if (item_ instanceof AircraftModel && fileType_ == null) {
			tm.runTaskInParallel(new ShareAircraftModel((AircraftModel) item_, recipients));
		}

		// aircraft model file
		else if (item_ instanceof AircraftModel && fileType_ != null) {
			tm.runTaskInParallel(new ShareAircraftModelFile((AircraftModel) item_, fileType_, recipients));
		}

		// stress sequence
		else if (item_ instanceof StressSequence || item_ instanceof ExternalStressSequence) {

			// create working directory
			Path workingDirectory = createWorkingDirectory("ShareSequence");

			// create output file
			Path output = null;
			if (item_ instanceof ExternalStressSequence && fileType_ != null) {
				output = workingDirectory.resolve(FileType.appendExtension(Utility.correctFileName(item_.getName()), FileType.FLS));
			}
			else {
				output = workingDirectory.resolve(FileType.appendExtension(Utility.correctFileName(item_.getName()), FileType.STH));
			}

			// create tasks
			InternalEquinoxTask<?> saveTask = null;
			if (item_ instanceof StressSequence && fileType_ == null) {
				saveTask = new SaveStressSequenceAsSTH((StressSequence) item_, output.toFile());
			}
			else if (item_ instanceof ExternalStressSequence && fileType_ == null) {
				saveTask = new SaveExternalStressSequenceAsSTH((ExternalStressSequence) item_, output.toFile());
			}
			else if (item_ instanceof ExternalStressSequence && fileType_ != null) {
				saveTask = new SaveExternalFLS(item_.getID(), output.toFile(), FileType.FLS);
			}
			ShareGeneratedItem share = new ShareGeneratedItem(output, recipients);
			DeleteTemporaryFiles delete = new DeleteTemporaryFiles(workingDirectory, null);

			// execute tasks
			tm.runTasksSequentially(saveTask, share, delete);
		}

		// STF file
		else if (item_ instanceof STFFile) {
			tm.runTaskInParallel(new ShareSTF((STFFile) item_, recipients));
		}

		// STF file bucket
		else if (item_ instanceof STFFileBucket) {

			// STF files
			if (params_[0].equals("STF")) {
				tm.runTaskInParallel(new ShareSTFBucket((STFFileBucket) item_, recipients));
			}

			// fatigue output files
			else if (params_[0].equals("fatigueOutputs")) {

				// create working directory
				Path workingDirectory = createWorkingDirectory("ShareBucketFatigueOutputs");

				// create output file
				Path output = workingDirectory.resolve("outputs.zip");

				// create tasks
				SaveAndZipBucketOutputFiles saveTask = new SaveAndZipBucketOutputFiles((STFFileBucket) item_, output, SaveBucketOutputFilesProcess.FATIGUE);
				ShareGeneratedItem share = new ShareGeneratedItem(output, recipients);
				DeleteTemporaryFiles delete = new DeleteTemporaryFiles(workingDirectory, null);

				// execute tasks
				tm.runTasksSequentially(saveTask, share, delete);
			}

			// preffas output files
			else if (params_[0].equals("preffasOutputs")) {

				// create working directory
				Path workingDirectory = createWorkingDirectory("ShareBucketPreffasOutputs");

				// create output file
				Path output = workingDirectory.resolve("outputs.zip");

				// create tasks
				SaveAndZipBucketOutputFiles saveTask = new SaveAndZipBucketOutputFiles((STFFileBucket) item_, output, SaveBucketOutputFilesProcess.PREFFAS);
				ShareGeneratedItem share = new ShareGeneratedItem(output, recipients);
				DeleteTemporaryFiles delete = new DeleteTemporaryFiles(workingDirectory, null);

				// execute tasks
				tm.runTasksSequentially(saveTask, share, delete);
			}

			// linear output files
			else if (params_[0].equals("linearOutputs")) {

				// create working directory
				Path workingDirectory = createWorkingDirectory("ShareBucketLinearOutputs");

				// create output file
				Path output = workingDirectory.resolve("outputs.zip");

				// create tasks
				SaveAndZipBucketOutputFiles saveTask = new SaveAndZipBucketOutputFiles((STFFileBucket) item_, output, SaveBucketOutputFilesProcess.LINEAR);
				ShareGeneratedItem share = new ShareGeneratedItem(output, recipients);
				DeleteTemporaryFiles delete = new DeleteTemporaryFiles(workingDirectory, null);

				// execute tasks
				tm.runTasksSequentially(saveTask, share, delete);
			}
		}

		// aircraft equivalent stresses
		else if (item_ instanceof AircraftFatigueEquivalentStress) {

			// create working directory
			Path workingDirectory = createWorkingDirectory("ShareAircraftEquivalentStress");

			// create output file
			Path output = workingDirectory.resolve(FileType.appendExtension(Utility.correctFileName(item_.getName()), FileType.XLS));

			// create tasks
			SaveAircraftEquivalentStress saveTask = new SaveAircraftEquivalentStress((AircraftFatigueEquivalentStress) item_, output.toFile());
			ShareGeneratedItem share = new ShareGeneratedItem(output, recipients);
			DeleteTemporaryFiles delete = new DeleteTemporaryFiles(workingDirectory, null);

			// execute tasks
			tm.runTasksSequentially(saveTask, share, delete);
		}

		// load case
		else if (item_ instanceof AircraftLoadCase) {

			// create working directory
			Path workingDirectory = createWorkingDirectory("ShareLoadCase");

			// create output file
			Path output = workingDirectory.resolve(FileType.appendExtension(Utility.correctFileName(item_.getName()), FileType.LCS));

			// create tasks
			SaveLoadCase saveTask = new SaveLoadCase((AircraftLoadCase) item_, output.toFile());
			ShareGeneratedItem share = new ShareGeneratedItem(output, recipients);
			DeleteTemporaryFiles delete = new DeleteTemporaryFiles(workingDirectory, null);

			// execute tasks
			tm.runTasksSequentially(saveTask, share, delete);
		}

		// equivalent stress
		else if (item_ instanceof FatigueEquivalentStress || item_ instanceof PreffasEquivalentStress || item_ instanceof LinearEquivalentStress || item_ instanceof ExternalFatigueEquivalentStress || item_ instanceof ExternalPreffasEquivalentStress || item_ instanceof ExternalLinearEquivalentStress
				|| item_ instanceof FastFatigueEquivalentStress || item_ instanceof FastPreffasEquivalentStress || item_ instanceof FastLinearEquivalentStress) {

			// analysis output files
			if (fileType_ == null) {

				// create working directory
				Path workingDirectory = createWorkingDirectory("ShareAnalysisOutputFile");

				// create output file
				Path output = workingDirectory.resolve(Utility.correctFileName(item_.getName()));

				// create tasks
				SaveOutputFile saveTask = new SaveOutputFile(item_, output);
				ShareGeneratedItem share = new ShareGeneratedItem(output, recipients);
				DeleteTemporaryFiles delete = new DeleteTemporaryFiles(workingDirectory, null);

				// execute tasks
				tm.runTasksSequentially(saveTask, share, delete);
			}

			// rainflow cycles
			else if (fileType_.equals(FileType.RFLOW)) {

				// create working directory
				Path workingDirectory = createWorkingDirectory("ShareRainflow");

				// create output file
				Path output = workingDirectory.resolve(FileType.appendExtension(Utility.correctFileName(item_.getName()), fileType_));

				// create tasks
				SaveRainflow saveTask = new SaveRainflow(item_, output.toFile());
				ShareGeneratedItem share = new ShareGeneratedItem(output, recipients);
				DeleteTemporaryFiles delete = new DeleteTemporaryFiles(workingDirectory, null);

				// execute tasks
				tm.runTasksSequentially(saveTask, share, delete);
			}
		}

		// hide pop-over
		popOver_.hide();
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
			popOver.show(share_);
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
	private boolean checkInputs(ArrayList<ExchangeUser> selected) {

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
			popOver.show(share_);
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
			popOver.show(share_);
			return false;
		}

		// acceptable inputs
		return true;
	}

	/**
	 * Returns selected recipients.
	 *
	 * @return Selected recipients.
	 */
	private ArrayList<ExchangeUser> getSelectedRecipients() {
		ArrayList<ExchangeUser> selected = new ArrayList<>();
		for (Node node : container_.getChildren()) {
			ToggleButton recipient = (ToggleButton) node;
			if (recipient.isSelected()) {
				selected.add((ExchangeUser) recipient.getUserData());
			}
		}
		return selected;
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static ShareFilePopup load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ShareFilePopup.fxml"));
			fxmlLoader.load();

			// get controller
			ShareFilePopup controller = (ShareFilePopup) fxmlLoader.getController();

			// set attributes
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
