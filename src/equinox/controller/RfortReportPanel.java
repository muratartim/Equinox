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
import java.util.logging.Level;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import equinox.Equinox;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.Rfort;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.exchangeServer.remote.message.StatusChange;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.DeleteTemporaryFiles;
import equinox.task.GenerateRfortReport;
import equinox.task.GetRfortOmissions;
import equinox.task.GetRfortOmissions.RfortOmissionsRequestingPanel;
import equinox.task.GetRfortPilotPoints;
import equinox.task.GetRfortPilotPoints.RfortPilotPointsRequestingPanel;
import equinox.task.GetRfortTypicalFlights;
import equinox.task.ShareGeneratedItem;
import equinox.utility.Utility;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for RFORT report panel controller.
 *
 * @author Murat Artim
 * @date Apr 21, 2016
 * @time 11:07:24 AM
 */
public class RfortReportPanel implements InternalInputSubPanel, RfortPilotPointsRequestingPanel, RfortOmissionsRequestingPanel, ListChangeListener<ExchangeUser> {

	/** The owner panel. */
	private InputPanel owner_;

	/** Panel mode. */
	private boolean isSave_ = true;

	@FXML
	private VBox root_, pilotPoints_, omissions_;

	@FXML
	private Accordion accordion_;

	@FXML
	private ToggleSwitch absoluteDeviations_, dataLabels_;

	@FXML
	private ComboBox<String> flight_;

	@FXML
	private ListView<ExchangeUser> recipients_;

	@FXML
	private TitledPane recipientsPane_;

	@FXML
	private Button ok_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup recipients
		recipients_.getSelectionModel().clearSelection();
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

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(0));

		// enable/disable recipients pane
		recipientsPane_.setDisable(isSave_);

		// reset options
		absoluteDeviations_.setSelected(true);
		dataLabels_.setSelected(true);

		// get selected item
		Rfort rfort = (Rfort) owner_.getSelectedFiles().get(0);

		// get pilot points and omissions
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetRfortPilotPoints(rfort, this, false));
		tm.runTaskInParallel(new GetRfortOmissions(rfort, this));
		tm.runTaskInParallel(new GetRfortTypicalFlights(rfort, this));
	}

	@Override
	public String getHeader() {
		return isSave_ ? "Generate RFORT Report" : "Share RFORT Report";
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

	@Override
	public void setPilotPoints(ArrayList<String> ppNames) {

		// reset pilot points
		pilotPoints_.getChildren().clear();
		for (String ppName : ppNames) {

			// create horizontal box
			HBox hBox = new HBox();
			hBox.setAlignment(Pos.CENTER_LEFT);
			hBox.setSpacing(5.0);
			hBox.setMaxWidth(Double.MAX_VALUE);

			// create toggle switch
			ToggleSwitch tSwitch = new ToggleSwitch();
			HBox.setHgrow(tSwitch, Priority.NEVER);
			tSwitch.setPrefWidth(35.0);
			tSwitch.setMinWidth(ToggleSwitch.USE_PREF_SIZE);
			tSwitch.setMaxWidth(ToggleSwitch.USE_PREF_SIZE);
			tSwitch.setUserData(ppName);
			tSwitch.setSelected(true);

			// create label
			Label label = new Label(ppName);
			HBox.setHgrow(label, Priority.ALWAYS);
			label.setMaxWidth(Double.MAX_VALUE);

			// add components to horizontal box
			hBox.getChildren().add(tSwitch);
			hBox.getChildren().add(label);

			// add to container
			pilotPoints_.getChildren().add(hBox);
		}
	}

	@Override
	public void setOmissions(ArrayList<String> omissions) {

		// reset omissions
		omissions_.getChildren().clear();
		for (String omissionName : omissions) {

			// create horizontal box
			HBox hBox = new HBox();
			hBox.setAlignment(Pos.CENTER_LEFT);
			hBox.setSpacing(5.0);
			hBox.setMaxWidth(Double.MAX_VALUE);

			// create toggle switch
			ToggleSwitch tSwitch = new ToggleSwitch();
			HBox.setHgrow(tSwitch, Priority.NEVER);
			tSwitch.setPrefWidth(35.0);
			tSwitch.setMinWidth(ToggleSwitch.USE_PREF_SIZE);
			tSwitch.setMaxWidth(ToggleSwitch.USE_PREF_SIZE);
			tSwitch.setUserData(omissionName);
			tSwitch.setSelected(true);

			// create label
			Label label = new Label(omissionName);
			HBox.setHgrow(label, Priority.ALWAYS);
			label.setMaxWidth(Double.MAX_VALUE);

			// add components to horizontal box
			hBox.getChildren().add(tSwitch);
			hBox.getChildren().add(label);

			// add to container
			omissions_.getChildren().add(hBox);
		}
	}

	/**
	 * Sets RFORT typical flights.
	 *
	 * @param flights
	 *            Typical flights.
	 */
	public void setTypicalFlights(ArrayList<String> flights) {
		flight_.getItems().clear();
		flight_.getItems().setAll(flights);
		flight_.getSelectionModel().select(0);
	}

	@FXML
	private void onResetClicked() {

		// reset show/hide pilot points
		for (Node node : pilotPoints_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			ts.setSelected(true);
		}

		// reset show/hide omissions
		for (Node node : omissions_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			ts.setSelected(true);
		}

		// reset options
		absoluteDeviations_.setSelected(true);
		dataLabels_.setSelected(true);
		flight_.getSelectionModel().select(0);

		// reset recipients
		recipients_.getSelectionModel().clearSelection();
	}

	@FXML
	private void onOkClicked() {

		// save mode
		if (isSave_) {
			save();
		}

		// share
		else {
			share();
		}
	}

	/**
	 * Shares RFORT report.
	 */
	private void share() {

		// has no permission
		if (!Equinox.USER.hasPermission(Permission.SHARE_FILE, true, owner_.getOwner()))
			return;

		// get selected item
		Rfort rfort = (Rfort) owner_.getSelectedFiles().get(0);

		// get selected pilot points
		ArrayList<String> pilotPoints = new ArrayList<>();
		for (Node node : pilotPoints_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			if (ts.isSelected()) {
				pilotPoints.add((String) ts.getUserData());
			}
		}

		// get selected omissions
		ArrayList<String> omissions = new ArrayList<>();
		for (Node node : omissions_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			if (ts.isSelected()) {
				omissions.add((String) ts.getUserData());
			}
		}

		// get selected recipients
		ObservableList<ExchangeUser> recipients = recipients_.getSelectionModel().getSelectedItems();

		// check inputs
		if (!checkInputs(pilotPoints, omissions, recipients))
			return;

		// get options
		boolean absoluteDeviations = absoluteDeviations_.isSelected();
		boolean dataLabels = dataLabels_.isSelected();
		String flight = flight_.getSelectionModel().getSelectedItem();

		// create working directory
		Path workingDirectory = createWorkingDirectory("ShareRfortReport");
		if (workingDirectory == null)
			return;

		// create output file
		Path output = workingDirectory.resolve("Rfort Report.pdf");

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// create tasks
		GenerateRfortReport saveTask = new GenerateRfortReport(rfort, output, pilotPoints, omissions, absoluteDeviations, dataLabels, flight);
		ShareGeneratedItem share = new ShareGeneratedItem(output, new ArrayList<>(recipients));
		DeleteTemporaryFiles delete = new DeleteTemporaryFiles(workingDirectory, null);

		// execute tasks
		tm.runTasksSequentially(saveTask, share, delete);

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
	 * Saves RFORT report.
	 */
	private void save() {

		// get selected item
		Rfort rfort = (Rfort) owner_.getSelectedFiles().get(0);

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.PDF.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName(Utility.correctFileName(rfort.getName()));
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// get selected pilot points
		ArrayList<String> pilotPoints = new ArrayList<>();
		for (Node node : pilotPoints_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			if (ts.isSelected()) {
				pilotPoints.add((String) ts.getUserData());
			}
		}

		// get selected omissions
		ArrayList<String> omissions = new ArrayList<>();
		for (Node node : omissions_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			if (ts.isSelected()) {
				omissions.add((String) ts.getUserData());
			}
		}

		// check inputs
		if (!checkInputs(pilotPoints, omissions, null))
			return;

		// get options
		boolean absoluteDeviations = absoluteDeviations_.isSelected();
		boolean dataLabels = dataLabels_.isSelected();
		String flight = flight_.getSelectionModel().getSelectedItem();

		// generate report
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GenerateRfortReport(rfort, selectedFile.toPath(), pilotPoints, omissions, absoluteDeviations, dataLabels, flight));

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Checks inputs.
	 *
	 * @param pilotPoints
	 *            Pilot points.
	 * @param omissions
	 *            Omissions.
	 * @param recipients
	 *            Recipients. Can be null for saving.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(ArrayList<String> pilotPoints, ArrayList<String> omissions, ObservableList<ExchangeUser> recipients) {

		// no pilot point selected
		if (pilotPoints.isEmpty()) {
			String message = "Please select at least 1 pilot point to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(pilotPoints_);
			return false;
		}

		// no omission selected
		if (omissions.isEmpty()) {
			String message = "Please select at least 1 omission to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(omissions_);
			return false;
		}

		// share
		if (!isSave_) {

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
			else if (recipients.isEmpty()) {
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
		}

		// acceptable
		return true;
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to generate RFORT report", null);
	}

	/**
	 * Loads and returns spectrum statistics panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded spectrum statistics panel.
	 */
	public static RfortReportPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("RfortReportPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			RfortReportPanel controller = (RfortReportPanel) fxmlLoader.getController();

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
