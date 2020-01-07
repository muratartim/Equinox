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
import java.util.EnumMap;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import equinox.Equinox;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.AnalysisEngine;
import equinox.data.EquinoxTheme;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.ProgramArguments;
import equinox.data.ProgramArguments.ArgumentType;
import equinox.data.Settings;
import equinox.font.IconicFont;
import equinox.serverUtilities.ServerUtility;
import equinox.task.LoadSettings;
import equinox.task.RewriteCFGFile;
import equinox.task.SaveSettings;
import equinox.utility.Utility;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

/**
 * Class for settings panel.
 *
 * @author Murat Artim
 * @date Apr 30, 2014
 * @time 1:09:20 PM
 */
public class SettingsPanel implements InternalInputSubPanel {

	/** Sub panel index. */
	public static final int ANALYSIS_ENGINE = 0, HUB_SERVER = 1, FILE_SERVER = 2, WEB_SERVER = 3, UPDATER = 4, NOTIFICATIONS = 5, MEMORY = 6, UI_THEME = 7;

	/** The owner panel. */
	private InputPanel owner_;

	/** Settings. */
	private Settings settings_;

	@FXML
	private VBox root_;

	@FXML
	private TextField dataHostname_, dataPort_, analysisHostname_, analysisPort_, exchangeHostname_, exchangePort_, webHostname_, webPort_, webPath_, filerRootPath_, filerHostname_, filerPort_, filerUsername_;

	@FXML
	private PasswordField filerPassword_;

	@FXML
	private ToggleSwitch showNewsfeed_, notifyEquinoxUpdates_, notifyPluginUpdates_, notifyMaterialUpdates_, notifyMessages_, notifyFiles_, notifyErrors_, notifyWarnings_, notifyInfo_, notifyQueued_, notifySubmitted_, notifySucceeded_, notifySaved_, notifyScheduled_, useSystemTray_, fallback_,
			showFromBottom_, keepOutputs_, detailedAnalysis_, compression_;

	@FXML
	private ComboBox<AnalysisEngine> analysisEngine_;

	@FXML
	private ComboBox<String> pageSize_;

	@FXML
	private ComboBox<IsamiVersion> analysisEngineVersion_;

	@FXML
	private ComboBox<IsamiSubVersion> analysisEngineSubVersion_;

	@FXML
	private Accordion accordion_;

	@FXML
	private Spinner<Integer> minHeap_, maxHeap_, maxTasks_, maxSubTasks_, cacheSize_, visibleSTFs_;

	@FXML
	private ToggleGroup theme_;

	@FXML
	private ToggleButton steelblue_, slategray_, midnightblue_;

	@FXML
	private Label steelblueLabel_, slategrayLabel_, midnightblueLabel_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set analysis engines and versions
		analysisEngine_.getItems().setAll(AnalysisEngine.values());
		analysisEngineSubVersion_.getItems().setAll(IsamiSubVersion.values());
		analysisEngineVersion_.getItems().setAll(IsamiVersion.values());

		// setup memory and performance settings
		int maxHeap = Equinox.OS_ARCH.equals(ServerUtility.X86) ? 1024 : 6144;
		int initialHeap = Equinox.OS_ARCH.equals(ServerUtility.X86) ? 1024 : 3072;
		minHeap_.setValueFactory(new IntegerSpinnerValueFactory(128, 1024, 256, 128));
		maxHeap_.setValueFactory(new IntegerSpinnerValueFactory(128, maxHeap, initialHeap, 128));
		int maxTasks = 2 * Runtime.getRuntime().availableProcessors();
		maxTasks_.setValueFactory(new IntegerSpinnerValueFactory(2, maxTasks, maxTasks / 2 + 1, 1));
		maxSubTasks_.setValueFactory(new IntegerSpinnerValueFactory(2, maxTasks, 2, 1));
		pageSize_.getItems().setAll("4096", "8192", "16384", "32768");
		cacheSize_.setValueFactory(new IntegerSpinnerValueFactory(1000, 8000, 4000, 500));
		visibleSTFs_.setValueFactory(new IntegerSpinnerValueFactory(2, 100, 15, 1));

		// bind theme selections
		steelblue_.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> steelblueLabel_.setText(newValue ? "\uf058" : "\uf111"));
		slategray_.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> slategrayLabel_.setText(newValue ? "\uf058" : "\uf111"));
		midnightblue_.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> midnightblueLabel_.setText(newValue ? "\uf058" : "\uf111"));

		// expand first panel
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
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new LoadSettings(this));
	}

	@Override
	public void showing() {
		setFromSettings();
		setFromArguments();
	}

	@Override
	public String getHeader() {
		return "Settings";
	}

	/**
	 * Returns the settings.
	 *
	 * @return The settings.
	 */
	public Settings getSettings() {
		return settings_;
	}

	/**
	 * Sets settings object.
	 *
	 * @param settings
	 *            Settings to set.
	 */
	public void setSettings(Settings settings) {

		// set settings
		settings_ = settings;
		setFromSettings();
	}

	/**
	 * Expands the sub panel with the given index.
	 *
	 * @param index
	 *            Index of sub panel to expand.
	 */
	public void expandPanel(int index) {
		accordion_.setExpandedPane(accordion_.getPanes().get(index));
	}

	@FXML
	private void onOkClicked() {

		// get entered values
		AnalysisEngine analysisEngine = analysisEngine_.getSelectionModel().getSelectedItem();
		IsamiSubVersion isamiSubVersion = analysisEngineSubVersion_.getSelectionModel().getSelectedItem();
		IsamiVersion isamiVersion = analysisEngineVersion_.getSelectionModel().getSelectedItem();
		String filerHostname = filerHostname_.getText();
		String filerPort = filerPort_.getText();
		String filerRootPath = filerRootPath_.getText();
		String filerUsername = filerUsername_.getText();
		String filerPassword = filerPassword_.getText();
		boolean notifyEquinoxUpdates = notifyEquinoxUpdates_.isSelected();
		boolean notifyPluginUpdates = notifyPluginUpdates_.isSelected();
		boolean notifyMaterialUpdates = notifyMaterialUpdates_.isSelected();
		String dataHostname = dataHostname_.getText();
		String dataPort = dataPort_.getText();
		String analysisHostname = analysisHostname_.getText();
		String analysisPort = analysisPort_.getText();
		String exchangeHostname = exchangeHostname_.getText();
		String exchangePort = exchangePort_.getText();
		String webHostname = webHostname_.getText();
		String webPort = webPort_.getText();
		String webPath = webPath_.getText();
		boolean notifyMessages = notifyMessages_.isSelected();
		boolean notifyFiles = notifyFiles_.isSelected();
		boolean notifyErrors = notifyErrors_.isSelected();
		boolean notifyWarnings = notifyWarnings_.isSelected();
		boolean notifyInfo = notifyInfo_.isSelected();
		boolean notifyQueued = notifyQueued_.isSelected();
		boolean notifySubmitted = notifySubmitted_.isSelected();
		boolean notifySucceeded = notifySucceeded_.isSelected();
		boolean notifySaved = notifySaved_.isSelected();
		boolean notifyScheduled = notifyScheduled_.isSelected();
		boolean useSystemTray = useSystemTray_.isSelected();
		boolean showNewsfeed = showNewsfeed_.isSelected();
		boolean showFromBottom = showFromBottom_.isSelected();
		boolean fallbackToInbuilt = fallback_.isSelected();
		boolean applyCompression = compression_.isSelected();
		boolean keepOutputs = keepOutputs_.isSelected();
		boolean detailedAnalysis = detailedAnalysis_.isSelected();
		String heapMessage = ProgramArguments.checkHeapSize(minHeap_.getValue(), maxHeap_.getValue());
		String tasksMessage = ProgramArguments.checkParallelTasks(maxTasks_.getValue(), maxSubTasks_.getValue());

		// check values
		String message = null;
		Node node = null;
		if (analysisEngine == null) {
			message = "Please select an analysis engine.";
			node = analysisEngine_;
		}
		else if (isamiVersion == null) {
			message = "Please select an ISAMI version.";
			node = analysisEngineVersion_;
		}
		else if (isamiSubVersion == null) {
			message = "Please select an ISAMI sub-version.";
			node = analysisEngineSubVersion_;
		}
		else if (filerHostname == null || filerHostname.isEmpty()) {
			message = "Invalid filer hostname given. Please supply a valid hostname.";
			node = filerHostname_;
		}
		else if (filerPort == null || filerPort.isEmpty()) {
			message = "Invalid filer port given. Please supply a valid port.";
			node = filerPort_;
		}
		else if (filerRootPath == null) {
			message = "Invalid filer root path given. Please supply a valid path.";
			node = filerRootPath_;
		}
		else if (filerUsername == null || filerUsername.isEmpty()) {
			message = "Invalid filer username given. Please supply a valid username.";
			node = filerUsername_;
		}
		else if (filerPassword == null || filerPassword.isEmpty()) {
			message = "Invalid filer password given. Please supply a valid password.";
			node = filerPassword_;
		}
		else if (dataHostname == null || dataHostname.isEmpty()) {
			message = "Invalid data service hostname given. Please supply a valid hostname.";
			node = dataHostname_;
		}
		else if (dataPort == null || dataPort.isEmpty()) {
			message = "Invalid data service port given. Please supply a valid port.";
			node = dataPort_;
		}
		else if (analysisHostname == null || analysisHostname.isEmpty()) {
			message = "Invalid analysis service hostname given. Please supply a valid hostname.";
			node = analysisHostname_;
		}
		else if (analysisPort == null || analysisPort.isEmpty()) {
			message = "Invalid analysis service port given. Please supply a valid port.";
			node = analysisPort_;
		}
		else if (exchangeHostname == null || exchangeHostname.isEmpty()) {
			message = "Invalid collaboration service hostname given. Please supply a valid hostname.";
			node = exchangeHostname_;
		}
		else if (exchangePort == null || exchangePort.isEmpty()) {
			message = "Invalid collaboration service port given. Please supply a valid port.";
			node = exchangePort_;
		}
		else if (webHostname == null || webHostname.isEmpty()) {
			message = "Invalid web server hostname given. Please supply a valid hostname.";
			node = webHostname_;
		}
		else if (webPort == null || webPort.isEmpty()) {
			message = "Invalid web server port given. Please supply a valid port.";
			node = webPort_;
		}
		else if (webPath == null || webPath.isEmpty()) {
			message = "Invalid web server upload path given. Please supply a valid path.";
			node = webPath_;
		}
		else if (heapMessage != null) {
			message = heapMessage;
			node = message.contains("Minimum") ? minHeap_ : maxHeap_;
		}
		else if (tasksMessage != null) {
			message = tasksMessage;
			node = message.contains("sub-tasks") ? maxSubTasks_ : maxTasks_;
		}

		// show warning
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(node);
			return;
		}

		// set settings
		boolean restart = false;
		if (settings_.setValue(Settings.FILER_HOSTNAME, filerHostname)) {
			restart = true;
		}
		if (settings_.setValue(Settings.FILER_PORT, filerPort)) {
			restart = true;
		}
		if (settings_.setValue(Settings.FILER_ROOT_PATH, filerRootPath)) {
			restart = true;
		}
		if (settings_.setValue(Settings.FILER_USERNAME, filerUsername)) {
			restart = true;
		}
		if (settings_.setValue(Settings.FILER_PASSWORD, filerPassword)) {
			restart = true;
		}
		if (settings_.setValue(Settings.NOTIFY_EQUINOX_UPDATES, notifyEquinoxUpdates)) {
			restart = true;
		}
		if (settings_.setValue(Settings.NOTIFY_PLUGIN_UPDATES, notifyPluginUpdates)) {
			restart = true;
		}
		if (settings_.setValue(Settings.NOTIFY_MATERIAL_UPDATES, notifyMaterialUpdates)) {
			restart = true;
		}
		if (settings_.setValue(Settings.DATA_SERVER_HOSTNAME, dataHostname)) {
			restart = true;
		}
		if (settings_.setValue(Settings.DATA_SERVER_PORT, dataPort)) {
			restart = true;
		}
		if (settings_.setValue(Settings.ANALYSIS_SERVER_HOSTNAME, analysisHostname)) {
			restart = true;
		}
		if (settings_.setValue(Settings.ANALYSIS_SERVER_PORT, analysisPort)) {
			restart = true;
		}
		if (settings_.setValue(Settings.EXCHANGE_SERVER_HOSTNAME, exchangeHostname)) {
			restart = true;
		}
		if (settings_.setValue(Settings.EXCHANGE_SERVER_PORT, exchangePort)) {
			restart = true;
		}
		if (settings_.setValue(Settings.WEB_HOSTNAME, webHostname)) {
			restart = true;
		}
		if (settings_.setValue(Settings.WEB_PORT, webPort)) {
			restart = true;
		}
		if (settings_.setValue(Settings.WEB_PATH, webPath)) {
			restart = true;
		}
		if (settings_.setValue(Settings.NOTIFY_MESSAGES, notifyMessages)) {
			restart = true;
		}
		if (settings_.setValue(Settings.NOTIFY_FILES, notifyFiles)) {
			restart = true;
		}
		if (settings_.setValue(Settings.NOTIFY_ERRORS, notifyErrors)) {
			restart = true;
		}
		if (settings_.setValue(Settings.NOTIFY_WARNINGS, notifyWarnings)) {
			restart = true;
		}
		if (settings_.setValue(Settings.NOTIFY_INFO, notifyInfo)) {
			restart = true;
		}
		if (settings_.setValue(Settings.NOTIFY_QUEUED, notifyQueued)) {
			restart = true;
		}
		if (settings_.setValue(Settings.NOTIFY_SUBMITTED, notifySubmitted)) {
			restart = true;
		}
		if (settings_.setValue(Settings.NOTIFY_SUCCEEDED, notifySucceeded)) {
			restart = true;
		}
		if (settings_.setValue(Settings.NOTIFY_SAVED, notifySaved)) {
			restart = true;
		}
		if (settings_.setValue(Settings.NOTIFY_SCHEDULED, notifyScheduled)) {
			restart = true;
		}
		if (settings_.setValue(Settings.USE_SYSTEMTRAY, useSystemTray)) {
			restart = true;
		}
		if (settings_.setValue(Settings.SHOW_NOTIFY_FROM_BOTTOM, showFromBottom)) {
			restart = true;
		}
		if (settings_.setValue(Settings.SHOW_NEWSFEED, showNewsfeed)) {
			restart = true;
		}
		if (settings_.setValue(Settings.ANALYSIS_ENGINE, analysisEngine)) {
			restart = true;
		}
		if (settings_.setValue(Settings.ISAMI_SUB_VERSION, isamiSubVersion)) {
			restart = true;
		}
		if (settings_.setValue(Settings.FALLBACK_TO_INBUILT, fallbackToInbuilt)) {
			restart = true;
		}
		if (settings_.setValue(Settings.APPLY_COMPRESSION, applyCompression)) {
			restart = true;
		}
		if (settings_.setValue(Settings.KEEP_ANALYSIS_OUTPUTS, keepOutputs)) {
			restart = true;
		}
		if (settings_.setValue(Settings.DETAILED_ANALYSIS, detailedAnalysis)) {
			restart = true;
		}
		if (settings_.setValue(Settings.ISAMI_VERSION, isamiVersion)) {
			restart = true;
		}

		// get program arguments
		EnumMap<ArgumentType, String> arguments = getArguments();

		// arguments did not change (save settings)
		if (arguments == null) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SaveSettings(settings_, restart));
		}

		// rewrite CFG file
		else {
			owner_.getOwner().getActiveTasksPanel().runTasksSequentially(new RewriteCFGFile(arguments), new SaveSettings(settings_, true));
		}

		// return
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {
		setFromSettings();
		setFromArguments();
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("Settings", null);
	}

	@FXML
	private void onAnalysisEngineSelected() {
		boolean isInbuilt = analysisEngine_.getSelectionModel().getSelectedItem().equals(AnalysisEngine.INBUILT);
		fallback_.setDisable(isInbuilt);
		boolean isIsami = analysisEngine_.getSelectionModel().getSelectedItem().equals(AnalysisEngine.ISAMI);
		analysisEngineVersion_.setDisable(!isIsami);
		analysisEngineSubVersion_.setDisable(!isIsami);
		compression_.setDisable(!isIsami);
	}

	@FXML
	private void onSuppressNotificationsClicked() {
		owner_.getOwner().getNotificationPane().clearAllNotifications();
	}

	@SuppressWarnings("static-method")
	@FXML
	private void onThemeClicked(ActionEvent e) {
		((ToggleButton) e.getSource()).setSelected(true);
	}

	/**
	 * Sets the UI components from the settings object.
	 */
	private void setFromSettings() {

		// analysis engine settings
		analysisEngine_.getSelectionModel().select((AnalysisEngine) settings_.getValue(Settings.ANALYSIS_ENGINE));
		analysisEngineVersion_.getSelectionModel().select((IsamiVersion) settings_.getValue(Settings.ISAMI_VERSION));
		analysisEngineSubVersion_.getSelectionModel().select((IsamiSubVersion) settings_.getValue(Settings.ISAMI_SUB_VERSION));
		fallback_.setSelected((boolean) settings_.getValue(Settings.FALLBACK_TO_INBUILT));
		compression_.setSelected((boolean) settings_.getValue(Settings.APPLY_COMPRESSION));
		keepOutputs_.setSelected((boolean) settings_.getValue(Settings.KEEP_ANALYSIS_OUTPUTS));
		detailedAnalysis_.setSelected((boolean) settings_.getValue(Settings.DETAILED_ANALYSIS));
		onAnalysisEngineSelected();

		// filer settings
		filerHostname_.setText((String) settings_.getValue(Settings.FILER_HOSTNAME));
		filerPort_.setText((String) settings_.getValue(Settings.FILER_PORT));
		filerRootPath_.setText((String) settings_.getValue(Settings.FILER_ROOT_PATH));
		filerUsername_.setText((String) settings_.getValue(Settings.FILER_USERNAME));
		filerPassword_.setText((String) settings_.getValue(Settings.FILER_PASSWORD));

		// microservices settings
		dataHostname_.setText((String) settings_.getValue(Settings.DATA_SERVER_HOSTNAME));
		dataPort_.setText((String) settings_.getValue(Settings.DATA_SERVER_PORT));
		exchangeHostname_.setText((String) settings_.getValue(Settings.EXCHANGE_SERVER_HOSTNAME));
		exchangePort_.setText((String) settings_.getValue(Settings.EXCHANGE_SERVER_PORT));
		analysisHostname_.setText((String) settings_.getValue(Settings.ANALYSIS_SERVER_HOSTNAME));
		analysisPort_.setText((String) settings_.getValue(Settings.ANALYSIS_SERVER_PORT));

		// web server settings
		webHostname_.setText((String) settings_.getValue(Settings.WEB_HOSTNAME));
		webPort_.setText((String) settings_.getValue(Settings.WEB_PORT));
		webPath_.setText((String) settings_.getValue(Settings.WEB_PATH));

		// software update settings
		notifyEquinoxUpdates_.setSelected((boolean) settings_.getValue(Settings.NOTIFY_EQUINOX_UPDATES));
		notifyPluginUpdates_.setSelected((boolean) settings_.getValue(Settings.NOTIFY_PLUGIN_UPDATES));
		notifyMaterialUpdates_.setSelected((boolean) settings_.getValue(Settings.NOTIFY_MATERIAL_UPDATES));
		notifyMessages_.setSelected((boolean) settings_.getValue(Settings.NOTIFY_MESSAGES));
		notifyFiles_.setSelected((boolean) settings_.getValue(Settings.NOTIFY_FILES));
		notifyErrors_.setSelected((boolean) settings_.getValue(Settings.NOTIFY_ERRORS));
		notifyWarnings_.setSelected((boolean) settings_.getValue(Settings.NOTIFY_WARNINGS));
		notifyInfo_.setSelected((boolean) settings_.getValue(Settings.NOTIFY_INFO));
		notifyQueued_.setSelected((boolean) settings_.getValue(Settings.NOTIFY_QUEUED));
		notifySubmitted_.setSelected((boolean) settings_.getValue(Settings.NOTIFY_SUBMITTED));
		notifySucceeded_.setSelected((boolean) settings_.getValue(Settings.NOTIFY_SUCCEEDED));
		notifySaved_.setSelected((boolean) settings_.getValue(Settings.NOTIFY_SAVED));
		notifyScheduled_.setSelected((boolean) settings_.getValue(Settings.NOTIFY_SCHEDULED));
		useSystemTray_.setSelected((boolean) settings_.getValue(Settings.USE_SYSTEMTRAY));
		showFromBottom_.setSelected((boolean) settings_.getValue(Settings.SHOW_NOTIFY_FROM_BOTTOM));
		showNewsfeed_.setSelected((boolean) settings_.getValue(Settings.SHOW_NEWSFEED));
	}

	/**
	 * Sets the UI components from the program arguments object.
	 */
	private void setFromArguments() {

		// set memory space
		minHeap_.getValueFactory().setValue(Integer.parseInt(Equinox.ARGUMENTS.getArgument(ArgumentType.JVM_MIN_HEAP_SIZE)));
		maxHeap_.getValueFactory().setValue(Integer.parseInt(Equinox.ARGUMENTS.getArgument(ArgumentType.JVM_MAX_HEAP_SIZE)));

		// set parallelism
		maxTasks_.getValueFactory().setValue(Integer.parseInt(Equinox.ARGUMENTS.getArgument(ArgumentType.MAX_PARALLEL_TASKS)));
		maxSubTasks_.getValueFactory().setValue(Integer.parseInt(Equinox.ARGUMENTS.getArgument(ArgumentType.MAX_PARALLEL_SUBTASKS)));

		// set database arguments
		pageSize_.getSelectionModel().select(Equinox.ARGUMENTS.getArgument(ArgumentType.DATABASE_PAGE_SIZE));
		cacheSize_.getValueFactory().setValue(Integer.parseInt(Equinox.ARGUMENTS.getArgument(ArgumentType.DATABASE_PAGE_CACHE_SIZE)));

		// set UI arguments
		visibleSTFs_.getValueFactory().setValue(Integer.parseInt(Equinox.ARGUMENTS.getArgument(ArgumentType.MAX_VISIBLE_STFS_PER_SPECTRUM)));

		// set color theme
		String colorTheme = Equinox.ARGUMENTS.getArgument(ArgumentType.COLOR_THEME);
		for (Toggle toggle : theme_.getToggles()) {
			ToggleButton tb = (ToggleButton) toggle;
			if (colorTheme.equals(tb.getText())) {
				theme_.selectToggle(toggle);
				break;
			}
		}
	}

	/**
	 * Returns entered program arguments, or null if there is no change in program arguments.
	 *
	 * @return Entered program arguments, or null if there is no change in program arguments.
	 */
	private EnumMap<ArgumentType, String> getArguments() {

		// get inputs
		EnumMap<ArgumentType, String> arguments = new EnumMap<>(ArgumentType.class);
		arguments.put(ArgumentType.JVM_MIN_HEAP_SIZE, minHeap_.getValue().toString());
		arguments.put(ArgumentType.JVM_MAX_HEAP_SIZE, maxHeap_.getValue().toString());
		arguments.put(ArgumentType.MAX_PARALLEL_TASKS, maxTasks_.getValue().toString());
		arguments.put(ArgumentType.MAX_PARALLEL_SUBTASKS, maxSubTasks_.getValue().toString());
		arguments.put(ArgumentType.DATABASE_PAGE_SIZE, pageSize_.getSelectionModel().getSelectedItem());
		arguments.put(ArgumentType.DATABASE_PAGE_CACHE_SIZE, cacheSize_.getValue().toString());
		arguments.put(ArgumentType.MAX_VISIBLE_STFS_PER_SPECTRUM, visibleSTFs_.getValue().toString());
		arguments.put(ArgumentType.COLOR_THEME, ((ToggleButton) theme_.getSelectedToggle()).getText());

		// check for change
		boolean changed = false;
		for (ArgumentType argumentType : ArgumentType.values()) {
			if (!arguments.get(argumentType).equals(Equinox.ARGUMENTS.getArgument(argumentType))) {
				changed = true;
				break;
			}
		}

		// return arguments (or null if there is no change)
		return changed ? arguments : null;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static SettingsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("SettingsPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			SettingsPanel controller = (SettingsPanel) fxmlLoader.getController();

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
