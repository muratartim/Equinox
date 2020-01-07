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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;

import org.controlsfx.control.PopOver;

import equinox.Equinox;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.ActionHandler;
import equinox.data.EquinoxTheme;
import equinox.data.Settings;
import equinox.data.ui.NotificationPanel;
import equinox.dataServer.remote.message.GetAccessRequestsRequest;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.exchangeServer.remote.message.StatusChange;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.serverUtilities.ServerUtility;
import equinox.task.AdaptDRF;
import equinox.task.AddStressSequence;
import equinox.task.BackupWorkspace;
import equinox.task.CheckForEquinoxUpdates;
import equinox.task.CreateWorkspace;
import equinox.task.DeleteTemporaryFiles;
import equinox.task.Excalibur;
import equinox.task.GetAccessRequests;
import equinox.task.GetHelpVideos;
import equinox.task.GetPlugins;
import equinox.task.MyCheck;
import equinox.task.OpenWorkspace;
import equinox.task.PreparePilotPointUpload;
import equinox.task.ResetExchangeTable;
import equinox.task.ResetWorkspace;
import equinox.task.RfortAnalysis;
import equinox.task.RunInstructionSetOn;
import equinox.task.SaveTask;
import equinox.task.SaveWorkspace;
import equinox.task.ShareInstructionSet;
import equinox.task.ShareWorkspace;
import equinox.task.ShowNewsFeed;
import equinox.task.UploadDamageContributions;
import equinox.task.UploadMaterials;
import equinox.task.UploadMultiplicationTables;
import equinox.task.UploadSampleInputs;
import equinox.task.UploadSpectra;
import equinox.task.automation.CheckInstructionSet;
import equinox.task.automation.ConvertJSONFiletoXMLFile;
import equinox.task.automation.ConvertXMLFiletoJSONFile;
import equinox.utility.Utility;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

/**
 * Class for menu bar panel controller.
 *
 * @author Murat Artim
 * @date Oct 23, 2014
 * @time 2:00:03 PM
 */
public class MenuBarPanel implements Initializable, ListChangeListener<ExchangeUser>, SchedulingPanel {

	/** Task manager notification images. */
	public static final Image QUIET = Utility.getImage("tm.png"), RUNNING = Utility.getImage("taskManager.gif");

	/** The owner of this panel. */
	private MainScreen owner_;

	@FXML
	private TextField searchField_;

	@FXML
	private Button cancelSearch_;

	@FXML
	private StackPane root_;

	@FXML
	private MenuBar menuBar_;

	@FXML
	private Menu openRecentMenu_, selectedMenu_, administratorMenu_, shareWorkspaceFileMenu_, shareWorkspaceCollaborationMenu_, pluginMenu_, shareInstructionSetMenu_, shareInstructionSetCollaborationMenu_, runInstructionSetMenu_, runInstructionSetCollaborationMenu_;

	@FXML
	private MenuItem login_, saveFile_, shareFile_, myCheckMenuItem_, adaptDRFMenuItem_, excaliburMenuItem_, objectView_;

	@FXML
	private ImageView notificationImage_;

	@FXML
	private Label rfortExtendedLabel_, myCheckLabel_, adaptDRFLabel_, excaliburLabel_;

	@FXML
	private RadioMenuItem available_;

	/** True if the modal task scheduling popup is shown. */
	private boolean isModalTaskSchedulePopupShown_ = false;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set listener to cancel button
		cancelSearch_.setOnAction(event -> searchField_.clear());

		// set listener to search text
		searchField_.textProperty().addListener((ChangeListener<String>) (ov, old_Val, new_val) -> cancelSearch_.setVisible(!new_val.isEmpty()));

		// set internal plugin versions
		rfortExtendedLabel_.setText(rfortExtendedLabel_.getText() + " " + RfortAnalysis.VERSION);
		myCheckLabel_.setText(myCheckLabel_.getText() + " " + MyCheck.VERSION);
		adaptDRFLabel_.setText(adaptDRFLabel_.getText() + " " + AdaptDRF.VERSION);
		excaliburLabel_.setText(excaliburLabel_.getText() + " " + Excalibur.VERSION);

		// remove unsupported plugins
		removeUnsupportedPlugins();
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.XML.getExtensionFilter(), FileType.JSON.getExtensionFilter());

		// show open dialog
		File file = fileChooser.showOpenDialog(owner_.getOwner().getStage());

		// no file selected
		if (file == null || !file.exists())
			return;

		// set initial directory
		owner_.setInitialDirectory(file);

		// create batch analysis
		owner_.getActiveTasksPanel().runTaskInParallel(new SaveTask(new CheckInstructionSet(file.toPath(), CheckInstructionSet.RUN), scheduleDate));
	}

	/**
	 * Adds plugin menu item.
	 *
	 * @param menuItem
	 *            Plugin menu item.
	 * @return The index of menu item.
	 */
	public int addPluginMenuItem(MenuItem menuItem) {
		int index = pluginMenu_.getItems().size() - 2;
		pluginMenu_.getItems().add(index, menuItem);
		return index;
	}

	/**
	 * Disables or enables selected item related menus and menu items.
	 *
	 * @param disable
	 *            True to disable.
	 */
	public void disableSelectedItems(boolean disable) {
		if (!disable && owner_.getInputPanel().getSelectedFiles().isEmpty()) {
			disable = true;
		}
		saveFile_.setDisable(disable);
		shareFile_.setDisable(disable);
		selectedMenu_.setDisable(disable);
	}

	/**
	 * Returns available network status item.
	 *
	 * @return Available network status item.
	 */
	public RadioMenuItem getNetworkStatusItem() {
		return available_;
	}

	/**
	 * Returns the notification node for displaying notifications.
	 *
	 * @return The notification node.
	 */
	public ImageView getNotificationNode() {
		return notificationImage_;
	}

	/**
	 * Returns open recent menu.
	 *
	 * @return The open recent menu.
	 */
	public Menu getOpenRecentMenu() {
		return openRecentMenu_;
	}

	/**
	 * Returns the main screen of the application.
	 *
	 * @return The main screen of the application.
	 */
	public MainScreen getOwner() {
		return owner_;
	}

	/**
	 * Returns plugins menu.
	 *
	 * @return Plugins menu.
	 */
	public Menu getPluginsMenu() {
		return pluginMenu_;
	}

	/**
	 * Returns the root of this controller.
	 *
	 * @return The root of this controller.
	 */
	public StackPane getRoot() {
		return root_;
	}

	/**
	 * Returns search text field.
	 *
	 * @return Search text field.
	 */
	public TextField getSearchField() {
		return searchField_;
	}

	@FXML
	private void on3DViewClicked() {
		owner_.getViewPanel().showSubPanel(ViewPanel.OBJECT_VIEW);
	}

	@FXML
	private void onAboutClicked() {
		IntroPanel introPanel = IntroPanel.load(owner_);
		introPanel.showAbout();
	}

	@FXML
	private void onAddACModelClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.ADD_AC_MODEL_PANEL);
	}

	@FXML
	private void onAddCDFSetClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.ADD_SPECTRUM_PANEL);
	}

	@FXML
	public void onAddSIGMAClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.SIGMA.getExtensionFilter());

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
			return;

		// set initial directory
		owner_.setInitialDirectory(files.get(0));

		// get progress panel
		ActiveTasksPanel tm = owner_.getActiveTasksPanel();

		// add tasks
		for (File file : files) {
			tm.runTaskInParallel(new AddStressSequence(file.toPath()));
		}
	}

	@FXML
	private void onAddSTHClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.ADD_STH_PANEL);
	}

	@FXML
	private void onRunInstructionSetClicked() {
		owner_.getInputPanel().onRunInstructionSetClicked();
	}

	/**
	 * Recipient to send the request.
	 *
	 * @param recipient
	 *            Recipient to send the request.
	 */
	private void onRunInstructionSetOnClicked(ExchangeUser recipient) {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.XML.getExtensionFilter(), FileType.JSON.getExtensionFilter());

		// show open dialog
		File file = fileChooser.showOpenDialog(owner_.getOwner().getStage());

		// no file selected
		if (file == null || !file.exists())
			return;

		// set initial directory
		owner_.setInitialDirectory(file);

		// execute tasks
		owner_.getActiveTasksPanel().runTaskInParallel(new RunInstructionSetOn(file.toPath(), recipient));
	}

	@FXML
	private void onCheckInstructionSetClicked() {
		owner_.getInputPanel().onCheckInstructionSetClicked();
	}

	@FXML
	private void onSaveInstructionSetClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.XML.getExtensionFilter(), FileType.JSON.getExtensionFilter());

		// show open dialog
		File file = fileChooser.showOpenDialog(owner_.getOwner().getStage());

		// no file selected
		if (file == null || !file.exists())
			return;

		// set initial directory
		owner_.setInitialDirectory(file);

		// create batch analysis
		owner_.getActiveTasksPanel().runTaskInParallel(new SaveTask(new CheckInstructionSet(file.toPath(), CheckInstructionSet.RUN), null));
	}

	@FXML
	private void onScheduleInstructionSetClicked() {

		// popup already shown
		if (isModalTaskSchedulePopupShown_)
			return;

		// create pop-over
		PopOver popOver = new PopOver();
		popOver.setHideOnEscape(false);
		popOver.setAutoHide(false);
		popOver.setContentNode(ScheduleTaskPanel.load(popOver, this, null));
		popOver.setStyle("-fx-base: #ececec;");
		popOver.centerOnScreen();
		popOver.setArrowSize(0.0);
		popOver.setId("modal");

		// set showing handler
		popOver.setOnShowing(event -> {
			owner_.addModalLayer("modalScheduleTask");
			isModalTaskSchedulePopupShown_ = true;
		});

		// set hidden handler
		popOver.setOnHidden(event -> {
			owner_.removeModalLayer("modalScheduleTask");
			isModalTaskSchedulePopupShown_ = false;
		});

		// show
		popOver.show(owner_.getOwner().getStage());
	}

	@FXML
	private void onGenerateExecutionPlanClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.XML.getExtensionFilter(), FileType.JSON.getExtensionFilter());

		// show open dialog
		File file = fileChooser.showOpenDialog(owner_.getOwner().getStage());

		// no file selected
		if (file == null || !file.exists())
			return;

		// set initial directory
		owner_.setInitialDirectory(file);

		// create batch analysis
		owner_.getActiveTasksPanel().runTaskInParallel(new CheckInstructionSet(file.toPath(), CheckInstructionSet.GENERATE_EXECUTION_PLAN));
	}

	@FXML
	private void onConvertToJsonClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.XML.getExtensionFilter());

		// show open dialog
		File file = fileChooser.showOpenDialog(owner_.getOwner().getStage());

		// no file selected
		if (file == null || !file.exists())
			return;

		// set initial directory
		owner_.setInitialDirectory(file);

		// run task
		Path outputJsonFile = file.toPath().resolveSibling(FileType.getNameWithoutExtension(file.toPath()) + ".json");
		owner_.getActiveTasksPanel().runTaskInParallel(new ConvertXMLFiletoJSONFile(file.toPath(), outputJsonFile));
	}

	@FXML
	private void onConvertToXmlClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.JSON.getExtensionFilter());

		// show open dialog
		File file = fileChooser.showOpenDialog(owner_.getOwner().getStage());

		// no file selected
		if (file == null || !file.exists())
			return;

		// set initial directory
		owner_.setInitialDirectory(file);

		// run task
		Path outputXmlFile = file.toPath().resolveSibling(FileType.getNameWithoutExtension(file.toPath()) + ".xml");
		owner_.getActiveTasksPanel().runTaskInParallel(new ConvertJSONFiletoXMLFile(file.toPath(), outputXmlFile));
	}

	@FXML
	private void onDownloadRootTemplateClicked() {
		owner_.downloadSampleInput("equinoxInput.xml");
	}

	@FXML
	private void onDownloadGenerateStressSequenceTemplateClicked() {
		owner_.downloadSampleInput("generateStressSequenceInput.xml");
	}

	@FXML
	private void onDownloadEquivalentStressAnalysisTemplateClicked() {
		owner_.downloadSampleInput("equivalentStressAnalysisInput.xml");
	}

	@FXML
	private void onDownloadDamageContributionAnalysisTemplateClicked() {
		owner_.downloadSampleInput("loadcaseDamageContributionAnalysisInput.xml");
	}

	@FXML
	private void onDownloadSampleInstructionSetsClicked() {
		owner_.downloadSampleInput("instructionSetExamples.zip");
	}

	@FXML
	private void onAvailableSelected() {
		owner_.getExchangeServerManager().sendMessage(new StatusChange(Equinox.USER.createExchangeUser(), true));
	}

	@FXML
	private void onBackupWorkspaceClicked() {

		// get notification pane
		NotificationPanel np = owner_.getNotificationPane();

		// setup notification title and message
		String title = "Backup workspace";
		String message = "Backup operation will temporarily lock the workspace. Do you want to continue?";

		// show notification
		np.showQuestion(title, message, "Yes", "No", event -> {

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(FileType.ZIP.getExtensionFilter());

			// show save dialog
			String fileName = Utility.correctFileName(owner_.getOwner().getStage().getTitle().split(" - ")[1]);
			fileChooser.setInitialFileName(fileName + FileType.EQX.getExtension() + FileType.ZIP.getExtension());
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// append extension if necessary
			File file = FileType.appendExtension(selectedFile, FileType.ZIP);

			// backup
			owner_.getActiveTasksPanel().runTaskSequentially(new BackupWorkspace(file.toPath()));
			np.hide();
		}, event -> np.hide());
	}

	@FXML
	private void onBugReportClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.BUG_REPORT_PANEL);
	}

	@FXML
	private void onBusySelected() {
		owner_.getExchangeServerManager().sendMessage(new StatusChange(Equinox.USER.createExchangeUser(), false));
	}

	@Override
	public void onChanged(javafx.collections.ListChangeListener.Change<? extends ExchangeUser> c) {

		// remove all current recipients
		shareWorkspaceFileMenu_.getItems().clear();
		shareWorkspaceCollaborationMenu_.getItems().clear();
		shareInstructionSetMenu_.getItems().clear();
		shareInstructionSetCollaborationMenu_.getItems().clear();
		runInstructionSetMenu_.getItems().clear();
		runInstructionSetCollaborationMenu_.getItems().clear();

		// add new recipients
		ObservableList<? extends ExchangeUser> list = c.getList();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			ExchangeUser recipient = list.get(i);
			MenuItem item1 = new MenuItem(recipient.toString());
			item1.setOnAction(event -> onShareWorkspaceClicked(recipient));
			shareWorkspaceFileMenu_.getItems().add(item1);
			MenuItem item2 = new MenuItem(recipient.toString());
			item2.setOnAction(event -> onShareWorkspaceClicked(recipient));
			shareWorkspaceCollaborationMenu_.getItems().add(item2);
			MenuItem item3 = new MenuItem(recipient.toString());
			item3.setOnAction(event -> onShareInstructionSetClicked(recipient));
			shareInstructionSetMenu_.getItems().add(item3);
			MenuItem item4 = new MenuItem(recipient.toString());
			item4.setOnAction(event -> onShareInstructionSetClicked(recipient));
			shareInstructionSetCollaborationMenu_.getItems().add(item4);
			MenuItem item5 = new MenuItem(recipient.toString());
			item5.setOnAction(event -> onRunInstructionSetOnClicked(recipient));
			runInstructionSetMenu_.getItems().add(item5);
			MenuItem item6 = new MenuItem(recipient.toString());
			item6.setOnAction(event -> onRunInstructionSetOnClicked(recipient));
			runInstructionSetCollaborationMenu_.getItems().add(item6);
		}
	}

	@FXML
	private void onCheckForUpdatesClicked() {
		owner_.getActiveTasksPanel().runTaskInParallel(new CheckForEquinoxUpdates(true));
	}

	@FXML
	private void onCodeDocumentClicked() {

		// open default mail application
		try {

			// desktop is not supported
			if (!Desktop.isDesktopSupported()) {
				String message = "Cannot open default browser. Desktop class is not supported.";
				owner_.getNotificationPane().showWarning(message, null);
				return;
			}

			// get desktop
			Desktop desktop = Desktop.getDesktop();

			// open action is not supported
			if (!desktop.isSupported(Desktop.Action.BROWSE)) {
				String message = "Cannot open default browser. Browse action is not supported.";
				owner_.getNotificationPane().showWarning(message, null);
				return;
			}

			// open browser
			String hostname = (String) owner_.getSettings().getValue(Settings.WEB_HOSTNAME);
			// String port = (String) owner_.getSettings().getValue(Settings.WEB_PORT);
			// URI uri = new URI("http://" + hostname + ":" + port + "/2B03/EquinoxWeb/sourceDoc/index.html");
			URI uri = new URI(hostname);
			desktop.browse(uri);
		}

		// exception occurred
		catch (Exception e) {
			String msg = "Exception occurred during opening default browser: ";
			Equinox.LOGGER.log(Level.WARNING, msg, e);
			msg += e.getLocalizedMessage();
			msg += " Click 'Details' for more information.";
			owner_.getNotificationPane().showError("Problem encountered", msg, e);
		}
	}

	@FXML
	private void onComparisonViewClicked() {
		owner_.getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
	}

	@FXML
	private void onDamageContributionViewClicked() {
		owner_.getViewPanel().showSubPanel(ViewPanel.DAMAGE_CONTRIBUTION_VIEW);
	}

	@FXML
	private void onDownloadSampleSIGMAClicked() {
		owner_.downloadSampleInput("AddNewStressSequenceFromSIGMA");
	}

	@FXML
	private void onDownloadsViewClicked() {
		owner_.getViewPanel().showSubPanel(ViewPanel.DOWNLOAD_VIEW);
	}

	@FXML
	private void onExecuteSQLStatementClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.EXECUTE_SQL_STATEMENT_PANEL);
	}

	@FXML
	private void onEquinoxWebClicked() {

		// open default mail application
		try {

			// desktop is not supported
			if (!Desktop.isDesktopSupported()) {
				String message = "Cannot open default browser. Desktop class is not supported.";
				owner_.getNotificationPane().showWarning(message, null);
				return;
			}

			// get desktop
			Desktop desktop = Desktop.getDesktop();

			// open action is not supported
			if (!desktop.isSupported(Desktop.Action.BROWSE)) {
				String message = "Cannot open default browser. Browse action is not supported.";
				owner_.getNotificationPane().showWarning(message, null);
				return;
			}

			// open browser
			String hostname = (String) owner_.getSettings().getValue(Settings.WEB_HOSTNAME);
			// String port = (String) owner_.getSettings().getValue(Settings.WEB_PORT);
			// URI uri = new URI("http://" + hostname + ":" + port + "/2B03/EquinoxWeb/demo.html");
			URI uri = new URI(hostname);
			desktop.browse(uri);
		}

		// exception occurred
		catch (Exception e) {
			String msg = "Exception occurred during opening default browser: ";
			Equinox.LOGGER.log(Level.WARNING, msg, e);
			msg += e.getLocalizedMessage();
			msg += " Click 'Details' for more information.";
			owner_.getNotificationPane().showError("Problem encountered", msg, e);
		}
	}

	@FXML
	private void onFlightComparisonViewClicked() {
		owner_.getViewPanel().showSubPanel(ViewPanel.COMPARE_FLIGHTS_VIEW);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.HELP_PANEL);
	}

	@FXML
	private void onHelpVideosClicked() {
		owner_.getActiveTasksPanel().runTaskInParallel(new GetHelpVideos());
	}

	@FXML
	private void onImageViewClicked() {
		owner_.getViewPanel().showSubPanel(ViewPanel.IMAGE_VIEW);
	}

	@FXML
	private void onInfoViewClicked() {
		owner_.getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);
	}

	@FXML
	private void onNewsfeedViewClicked() {
		owner_.getActiveTasksPanel().runTasksSequentially(new ShowNewsFeed());
	}

	@FXML
	private void onInstallMorePluginsClicked() {
		owner_.getActiveTasksPanel().runTaskInParallel(new GetPlugins());
	}

	@FXML
	private void onLevelCrossingViewClicked() {
		owner_.getViewPanel().showSubPanel(ViewPanel.LEVEL_CROSSING_VIEW);
	}

	@FXML
	private void onLoginClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.LOGIN_PANEL);
	}

	@FXML
	private void onLogoutClicked() {

		// logout
		Equinox.USER.logoutAsAdministrator();

		// disable all administrator menu items
		for (MenuItem item : administratorMenu_.getItems()) {
			item.setDisable(true);
		}

		// add login item
		administratorMenu_.getItems().add(0, login_);
		administratorMenu_.getItems().add(1, new SeparatorMenuItem());
	}

	@FXML
	private void onMessageClicked() {
		owner_.getInputPanel().onMessageClicked();
	}

	@FXML
	private void onMissionParametersViewClicked() {
		owner_.getViewPanel().showSubPanel(ViewPanel.MISSION_PARAMETERS_VIEW);
	}

	@FXML
	private void onMissionProfileViewClicked() {
		owner_.getViewPanel().showSubPanel(ViewPanel.MISSION_PROFILE_VIEW);
	}

	@FXML
	private void onMyCheckClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.MYCHECK_PANEL);
	}

	@FXML
	private void onAdaptDRFClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.ADAPT_DRF_PANEL);
	}

	@FXML
	private void onExcaliburClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.EXCALIBUR_PANEL);
	}

	@FXML
	private void onNewWorkspaceClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.EQX.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName("newWorkspace" + FileType.EQX.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.setInitialDirectory(selectedFile);

		// append extension if necessary
		File file = FileType.appendExtension(selectedFile, FileType.EQX);

		// execute tasks
		owner_.getActiveTasksPanel().runTasksSequentially(new CreateWorkspace(file.toPath(), null));
	}

	@FXML
	private void onOpenWorkspaceClicked() {

		// get directory chooser
		DirectoryChooser dirChooser = owner_.getDirectoryChooser();

		// show dialog
		File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

		// no directory selected
		if (selectedDir == null || !selectedDir.exists() || !selectedDir.getName().endsWith(FileType.EQX.getExtension()))
			return;

		// workspace is already open
		if (selectedDir.toPath().equals(Equinox.WORKSPACE_PATHS.getCurrentPath())) {
			String title = "No operation";
			String msg = "Workspace '" + selectedDir.getName() + "' is already open.";
			owner_.getNotificationPane().showInfo(title, msg);
			return;
		}

		// set initial directory
		owner_.setInitialDirectory(selectedDir);

		// open workspace
		owner_.getActiveTasksPanel().runTasksSequentially(new OpenWorkspace(selectedDir.toPath(), null));
	}

	@FXML
	private void onPlotViewClicked() {
		owner_.getViewPanel().showSubPanel(ViewPanel.PLOT_VIEW);
	}

	@FXML
	private void onPluginsViewClicked() {
		owner_.getViewPanel().showSubPanel(ViewPanel.PLUGIN_VIEW);
	}

	@FXML
	private void onRestartClicked() {

		// there are running tasks
		if (owner_.getActiveTasksPanel().hasRunningTasks()) {
			owner_.getOwner().askForClosure(true);
		}

		// there is no running task
		else {

			try {

				// restart
				owner_.getOwner().restartContainer();

				// exit
				Platform.exit();
			}

			// exception occurred
			catch (Exception e) {

				// log exception
				Equinox.LOGGER.log(Level.WARNING, "Exception occured during restarting Equinox.", e);

				// create and show notification
				String message1 = "Exception occured during restarting Equinox: " + e.getLocalizedMessage();
				message1 += " Click 'Details' for more information.";
				NotificationPanel np = owner_.getNotificationPane();
				np.showError("Problem encountered", message1, e);
			}
		}
	}

	@FXML
	private void onQuitClicked() {

		// there are running tasks
		if (owner_.getActiveTasksPanel().hasRunningTasks()) {
			owner_.getOwner().askForClosure(false);
		}

		// there is no running task
		else {
			Platform.exit();
		}
	}

	@FXML
	private void onResetWorkspaceClicked() {

		// get notification pane
		NotificationPanel np = owner_.getNotificationPane();

		// setup notification title and message
		String title = "Reset workspace";
		String message = "Reseting will delete all files in the workspace. Do you want to continue?";

		// show notification
		np.showQuestion(title, message, "Yes", "No", event -> {

			// reset
			owner_.getActiveTasksPanel().runTaskSequentially(new ResetWorkspace());

			// hide notification
			np.hide();
		}, event -> np.hide());
	}

	@FXML
	private void onResetExchangeDatabaseClicked() {

		// there is no available user
		if (!owner_.areThereAvailableMembers()) {
			owner_.getActiveTasksPanel().runTaskInParallel(new ResetExchangeTable());
			return;
		}

		// get notification pane
		NotificationPanel np = owner_.getNotificationPane();

		// setup notification title and message
		String title = "Reset exchange database";
		String message = "There are online and available users. Are you sure you want to reset exchange database?";

		// show notification
		np.showQuestion(title, message, "Yes", "No", event -> {
			owner_.getActiveTasksPanel().runTaskSequentially(new ResetExchangeTable());
			np.hide();
		}, event -> np.hide());
	}

	@FXML
	private void onRFORTExtendedClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.RFORT_EXTENDED_PANEL);
	}

	@FXML
	private void onRoadmapClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.ROADMAP_PANEL);
	}

	@FXML
	private void onSaveWorkspaceClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.EQX.getExtensionFilter());

		// show save dialog
		String fileName = Utility.correctFileName(owner_.getOwner().getStage().getTitle().split(" - ")[1]);
		fileChooser.setInitialFileName(fileName + FileType.EQX.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.setInitialDirectory(selectedFile);

		// append extension if necessary
		File file = FileType.appendExtension(selectedFile, FileType.EQX);

		// execute tasks
		owner_.getActiveTasksPanel().runTasksSequentially(new SaveWorkspace(file.toPath()), new OpenWorkspace(file.toPath(), null));
	}

	@FXML
	private void onSaveViewClicked() {
		owner_.getViewPanel().onSaveAsClicked();
	}

	@FXML
	private void onSearchACModelClicked() {
		// TODO on search A/C model clicked
	}

	@FXML
	private void onSearchMultiplicationTablesClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.SEARCH_MULTIPLICATION_TABLES_PANEL);
	}

	@FXML
	private void onSearchPilotPointsClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.SEARCH_PILOT_POINTS_PANEL);
	}

	@FXML
	private void onSearchSpectrumClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.SEARCH_SPECTRA_PANEL);
	}

	@FXML
	private void onSearchSettingsClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
	}

	@FXML
	private void onDatabaseConnectionSettingsClicked() {
		SettingsPanel panel = (SettingsPanel) owner_.getInputPanel().getSubPanel(InputPanel.SETTINGS_PANEL);
		panel.expandPanel(SettingsPanel.FILE_SERVER);
		owner_.getInputPanel().showSubPanel(InputPanel.SETTINGS_PANEL);
	}

	@FXML
	private void onDataInsightsClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.DATA_INSIGHTS_PANEL);
	}

	@FXML
	private void onSettingsClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.SETTINGS_PANEL);
	}

	/**
	 * Recipient to share with.
	 *
	 * @param recipient
	 *            Recipient to share with.
	 */
	private void onShareInstructionSetClicked(ExchangeUser recipient) {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.XML.getExtensionFilter(), FileType.JSON.getExtensionFilter());

		// show open dialog
		File file = fileChooser.showOpenDialog(owner_.getOwner().getStage());

		// no file selected
		if (file == null || !file.exists())
			return;

		// set initial directory
		owner_.setInitialDirectory(file);

		// execute tasks
		owner_.getActiveTasksPanel().runTaskInParallel(new ShareInstructionSet(file.toPath(), recipient));
	}

	/**
	 * Recipient to share with.
	 *
	 * @param recipient
	 *            Recipient to share with.
	 */
	private void onShareWorkspaceClicked(ExchangeUser recipient) {

		// create working directory
		Path workingDirectory = null;
		try {
			workingDirectory = Utility.createWorkingDirectory("ShareWorkspace");
		}

		// exception occurred during process
		catch (IOException e) {
			String message = "Exception occurred during creating working directory for the process.";
			Equinox.LOGGER.log(Level.WARNING, message, e);
			message += " Click 'Details' for more information.";
			owner_.getNotificationPane().showError("Problem encountered", message, e);
			return;
		}

		// get last workspace path
		Path lastPath = Equinox.WORKSPACE_PATHS.getCurrentPath().getFileName();
		if (lastPath == null) {
			String message = "Cannot create output directory for the process.";
			Equinox.LOGGER.log(Level.WARNING, message);
			owner_.getNotificationPane().showWarning(message, null);
			return;
		}

		// create output directory
		String name = lastPath.toString();
		Path output = workingDirectory.resolve(FileType.appendExtension(name, FileType.EQX));

		// execute tasks
		owner_.getActiveTasksPanel().runTasksSequentially(new SaveWorkspace(output), new ShareWorkspace(recipient, output), new DeleteTemporaryFiles(workingDirectory, null));
	}

	@FXML
	private void onShareViewClicked() {
		owner_.getViewPanel().onShareClicked();
	}

	@FXML
	private void onShortcutsClicked() {
		PopOver popOver = new PopOver();
		popOver.setDetached(true);
		popOver.setTitle("Shortcuts");
		popOver.setContentNode(ShortcutsPanel.load(menuBar_));
		popOver.setHideOnEscape(true);
		popOver.show(owner_.getOwner().getStage());
	}

	@FXML
	private void onUploadAircraftModelsClicked() {
		// TODO on upload A/C model clicked
	}

	@FXML
	private void onUploadContainerUpdateClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.UPLOAD_CONTAINER_UPDATE_PANEL);
	}

	@FXML
	private void onUploadApplicationUpdateClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.UPLOAD_APP_UPDATE_PANEL);
	}

	@FXML
	private void onUploadMaterialsClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.XLS.getExtensionFilter());

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
			return;

		// set initial directory
		owner_.setInitialDirectory(files.get(0));

		// get task manager
		ActiveTasksPanel taskManager = owner_.getActiveTasksPanel();

		// submit tasks sequentially
		for (File file : files) {
			taskManager.runTaskSequentially(new UploadMaterials(file.toPath()));
		}
	}

	@FXML
	private void onUploadDamageContributionsClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.XLS.getExtensionFilter());

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
			return;

		// set initial directory
		owner_.setInitialDirectory(files.get(0));

		// get task manager
		ActiveTasksPanel taskManager = owner_.getActiveTasksPanel();

		// submit tasks sequentially
		for (File file : files) {
			taskManager.runTaskSequentially(new UploadDamageContributions(file.toPath()));
		}
	}

	@FXML
	private void onUploadMultiplicationTablesClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.ZIP.getExtensionFilter());

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
			return;

		// set initial directory
		owner_.setInitialDirectory(files.get(0));

		// create and start upload task
		owner_.getActiveTasksPanel().runTaskInParallel(new UploadMultiplicationTables(files));
	}

	@FXML
	private void onUploadPilotPointsClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.ZIP.getExtensionFilter());

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
			return;

		// set initial directory
		owner_.setInitialDirectory(files.get(0));

		// create and start upload task
		owner_.getActiveTasksPanel().runTaskInParallel(new PreparePilotPointUpload(files));
	}

	@FXML
	private void onUploadPluginClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.UPLOAD_PLUGIN_PANEL);
	}

	@FXML
	private void onUploadSampleInputsClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.ZIP.getExtensionFilter());

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
			return;

		// set initial directory
		owner_.setInitialDirectory(files.get(0));

		// create and start upload task
		owner_.getActiveTasksPanel().runTaskInParallel(new UploadSampleInputs(files));
	}

	@FXML
	private void onUploadSpectraClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.ZIP.getExtensionFilter());

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
			return;

		// set initial directory
		owner_.setInitialDirectory(files.get(0));

		// create and start upload task
		owner_.getActiveTasksPanel().runTaskInParallel(new UploadSpectra(files));
	}

	@FXML
	private void onUploadVideoClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.UPLOAD_HELP_VIDEO_PANEL);
	}

	@FXML
	private void onAddNewUserClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.ADD_NEW_USER_PANEL);
	}

	@FXML
	private void onEditUserPermissionsClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.EDIT_USER_PERMISSIONS_PANEL);
	}

	@FXML
	private void onDeleteUsersClicked() {
		owner_.getInputPanel().showSubPanel(InputPanel.DELETE_USERS_PANEL);
	}

	@FXML
	private void onShowPermissionRequestsClicked() {
		owner_.getActiveTasksPanel().runTaskInParallel(new GetAccessRequests(GetAccessRequestsRequest.PENDING));
	}

	@FXML
	private void onWhatsNewClicked() {

		// open default mail application
		try {

			// desktop is not supported
			if (!Desktop.isDesktopSupported()) {
				String message = "Cannot open default browser. Desktop class is not supported.";
				owner_.getNotificationPane().showWarning(message, null);
				return;
			}

			// get desktop
			Desktop desktop = Desktop.getDesktop();

			// open action is not supported
			if (!desktop.isSupported(Desktop.Action.BROWSE)) {
				String message = "Cannot open default browser. Browse action is not supported.";
				owner_.getNotificationPane().showWarning(message, null);
				return;
			}

			// open browser
			// String hostname = (String) owner_.getSettings().getValue(Settings.WEB_HOSTNAME);
			// String port = (String) owner_.getSettings().getValue(Settings.WEB_PORT);
			// URI uri = new URI("http://" + hostname + ":" + port + "/2B03/EquinoxWeb/files/versionDescription.html");
			// desktop.browse(uri);

			// open browser
			String hostname = (String) owner_.getSettings().getValue(Settings.WEB_HOSTNAME);
			// String port = (String) owner_.getSettings().getValue(Settings.WEB_PORT);
			// URI uri = new URI("http://" + hostname + ":" + port + "/2B03/EquinoxWeb/demo.html");
			URI uri = new URI(hostname);
			desktop.browse(uri);
		}

		// exception occurred
		catch (Exception e) {
			String msg = "Exception occurred during opening default browser: ";
			Equinox.LOGGER.log(Level.WARNING, msg, e);
			msg += e.getLocalizedMessage();
			msg += " Click 'Details' for more information.";
			owner_.getNotificationPane().showError("Problem encountered", msg, e);
		}
	}

	@FXML
	private void onHealthMonitoringClicked() {
		((HealthMonitorViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.HEALTH_MONITOR_VIEW)).requestServerStatistics();
		owner_.getViewPanel().showSubPanel(ViewPanel.HEALTH_MONITOR_VIEW);
	}

	@FXML
	private void onManageDataServiceClicked() {
		((ManageServicePanel) owner_.getInputPanel().getSubPanel(InputPanel.MANAGE_SERVICE_PANEL)).setService(ManageServicePanel.DATA_SERVICE);
		owner_.getInputPanel().showSubPanel(InputPanel.MANAGE_SERVICE_PANEL);
	}

	@FXML
	private void onManageAnalysisServiceClicked() {
		((ManageServicePanel) owner_.getInputPanel().getSubPanel(InputPanel.MANAGE_SERVICE_PANEL)).setService(ManageServicePanel.ANALYSIS_SERVICE);
		owner_.getInputPanel().showSubPanel(InputPanel.MANAGE_SERVICE_PANEL);
	}

	@FXML
	private void onManageExchangeServiceClicked() {
		((ManageServicePanel) owner_.getInputPanel().getSubPanel(InputPanel.MANAGE_SERVICE_PANEL)).setService(ManageServicePanel.EXCHANGE_SERVICE);
		owner_.getInputPanel().showSubPanel(InputPanel.MANAGE_SERVICE_PANEL);
	}

	/**
	 * Removes login item from the menu button.
	 */
	public void removeLoginItem() {
		if (administratorMenu_.getItems().contains(login_)) {
			administratorMenu_.getItems().remove(login_);
			administratorMenu_.getItems().remove(0);
		}
		for (MenuItem item : administratorMenu_.getItems()) {
			item.setDisable(false);
		}
	}

	/**
	 * Removes plugin menu item.
	 *
	 * @param index
	 *            Plugin menu item index.
	 */
	public void removePluginMenuItem(int index) {
		pluginMenu_.getItems().remove(index);
	}

	/**
	 * Removes unsupported plugins according to current OS.
	 */
	private void removeUnsupportedPlugins() {
		if (!Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			pluginMenu_.getItems().remove(myCheckMenuItem_);
		}
	}

	/**
	 * Sets selected menu items.
	 *
	 * @param menu
	 *            Context menu containing the menu items.
	 * @param handler
	 *            Action handler.
	 */
	public void setSelectedMenu(ContextMenu menu, ActionHandler handler) {
		selectedMenu_.getItems().clear();
		saveFile_.setDisable(true);
		shareFile_.setDisable(true);
		if (menu == null || menu.getItems().isEmpty()) {
			selectedMenu_.setDisable(true);
			return;
		}
		for (MenuItem item : menu.getItems()) {
			String id = item.getId();
			if (id != null && id.startsWith("save")) {
				saveFile_.setDisable(false);
				saveFile_.setId(id);
				saveFile_.setOnAction(handler);
			}
			else if (id != null && id.equals("share")) {
				shareFile_.setDisable(false);
				shareFile_.setId(id);
				shareFile_.setOnAction(handler);
			}
			styleMenu(item);
			selectedMenu_.getItems().add(item);
		}
		selectedMenu_.setDisable(false);
	}

	/**
	 * Sets up administrator menu.
	 *
	 * @param isPrivileged
	 *            True if this user is a privileged user.
	 */
	public void setupAdministratorMenu(boolean isPrivileged) {
		administratorMenu_.setDisable(!isPrivileged);
	}

	@FXML
	public void showActiveTasks() {
		owner_.getActiveTasksPanel().show(notificationImage_);
	}

	@FXML
	public void showQueuedTasks() {
		owner_.getQueuedTasksPanel().show(notificationImage_);
	}

	@FXML
	public void showSavedTasks() {
		owner_.getSavedTasksPanel().show(notificationImage_);
	}

	@FXML
	public void showScheduledTasks() {
		owner_.getScheduledTasksPanel().show(notificationImage_);
	}

	@FXML
	public void showTaskHistory() {
		owner_.getTaskHistoryPanel().show(notificationImage_);
	}

	/**
	 * Starts this panel.
	 */
	public void start() {

		// set initial position
		root_.setTranslateY(-root_.getHeight());

		// disable/enable 3D object view
		objectView_.setDisable(!((ObjectViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.OBJECT_VIEW)).isEnabled());
	}

	/**
	 * Sets styling to menu.
	 *
	 * @param item
	 *            Menu item.
	 */
	private void styleMenu(MenuItem item) {
		if (item instanceof Menu) {
			item.getStyleClass().add("mymenu");
			Menu menu = (Menu) item;
			for (MenuItem item2 : menu.getItems()) {
				styleMenu(item2);
			}
		}
	}

	/**
	 * Loads and returns menu bar panel.
	 *
	 * @param owner
	 *            The owner panel of this menu bar.
	 * @return Menu bar panel.
	 */
	public static MenuBarPanel load(MainScreen owner) {

		try {

			// get file name
			String fileName = null;
			if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
				fileName = "MenuBarPanel.fxml";
			}
			else {
				fileName = "MenuBarPanelWindows.fxml";
			}

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("" + fileName));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			MenuBarPanel controller = (MenuBarPanel) fxmlLoader.getController();

			// set attributes
			controller.owner_ = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
