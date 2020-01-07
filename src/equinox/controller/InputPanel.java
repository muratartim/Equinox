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
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.Equinox;
import equinox.data.EquinoxTheme;
import equinox.data.Settings;
import equinox.data.UserAuthentication;
import equinox.data.fileType.SpectrumItem;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.plugin.InputSubPanel;
import equinox.task.AddStressSequence;
import equinox.task.automation.CheckInstructionSet;
import equinox.utility.Utility;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Pagination;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for files panel controller.
 *
 * @author Murat Artim
 * @date Dec 27, 2013
 * @time 7:54:11 PM
 */
public class InputPanel implements Initializable {

	/** Sub-panel index. */
	public static final int FILE_VIEW_PANEL = 0, ADD_SPECTRUM_PANEL = 1, GENERATE_STRESS_SEQUENCE_PANEL = 2, SPECTRUM_STATS_PANEL = 3, SETTINGS_PANEL = 4, SEARCH_SPECTRA_PANEL = 5, HELP_PANEL = 6, BUG_REPORT_PANEL = 7, LOGIN_PANEL = 8, ROADMAP_PANEL = 9, DUMMY_STF_PANEL = 10, HISTOGRAM_PANEL = 11,
			EQUIVALENT_STRESS_PANEL = 12, COMPARE_STRESS_SEQUENCE_PANEL = 13, LEVEL_CROSSING_PANEL = 14, DAMAGE_ANGLE_PANEL = 15, PLOT_DAMAGE_ANGLE_PANEL = 16, PLOT_FLIGHTS_PANEL = 17, UPLOAD_CONTAINER_UPDATE_PANEL = 18, COMPARE_FLIGHTS_PANEL = 19, COMPARE_EQUIVALENT_STRESS_PANEL = 20,
			COMPARE_DAMAGE_ANGLE_LIFE_FACTORS_PANEL = 21, MISSION_PARAMETERS_PANEL = 22, SAVE_AS_1D_STF_PANEL = 23, STF_EQUIVALENT_STRESS_PANEL = 24, RFORT_EXTENDED_PANEL = 25, MISSION_PROFILE_PANEL = 26, ADD_STH_PANEL = 27, EXTERNAL_STATS_PANEL = 28, COMPARE_EXTERNAL_STRESS_SEQUENCE_PANEL = 29,
			EXTERNAL_LEVEL_CROSSING_PANEL = 30, PLOT_EXTERNAL_FLIGHTS_PANEL = 31, COMPARE_EXTERNAL_FLIGHTS_PANEL = 32, MYCHECK_PANEL = 33, UPLOAD_PLUGIN_PANEL = 34, DAMAGE_CONTRIBUTION_PANEL = 35, COMPARE_DAMAGE_CONTRIBUTIONS_PANEL = 36, SAVE_EQUIVALENT_STRESS_PANEL = 37, HISTOGRAM_3D_PANEL = 38,
			ADD_AC_MODEL_PANEL = 39, PLOT_AC_STRUCTURE_PANEL = 40, RENAME_ELEMENT_GROUPS_PANEL = 41, DELETE_ELEMENT_GROUPS_PANEL = 42, CREATE_ELEMENT_GROUP_FROM_EIDS_PANEL = 43, CREATE_ELEMENT_GROUP_FROM_COORDS_PANEL = 44, CREATE_ELEMENT_GROUP_FROM_GROUPS_PANEL = 45,
			CREATE_ELEMENT_GROUP_FROM_QV_LV_POSITIONS_PANEL = 46, PLOT_ELEMENT_STRESSES_PANEL = 47, COMPARE_ELEMENT_STRESSES_PANEL = 48, SAVE_DAMAGE_CONTRIBUTIONS_PANEL = 49, SAVE_DAMAGE_ANGLE_PANEL = 50, COMPARE_LOAD_CASES_PANEL = 51, PLOT_AC_EQUIVALENT_STRESSES_PANEL = 52,
			COMPARE_AC_EQUIVALENT_STRESSES_PANEL = 53, GENERATE_LIFE_FACTOR_PANEL = 54, GENERATE_EQUIVALENT_STRESS_RATIO_PANEL = 55, SAVE_LIFE_FACTOR_PANEL = 56, SAVE_EQUIVALENT_STRESS_RATIO_PANEL = 57, GENERATE_AC_LIFE_FACTORS_PANEL = 58, GENERATE_AC_EQUIVALENT_STRESS_RATIOS_PANEL = 59,
			SAVE_AC_LIFE_FACTOR_PANEL = 60, SAVE_AC_EQUIVALENT_STRESS_RATIO_PANEL = 61, PLOT_AC_LIFE_FACTORS_PANEL = 62, PLOT_AC_EQUIVALENT_STRESS_RATIOS_PANEL = 63, STF_INFO_PANEL = 64, SPECTRUM_INFO_PANEL = 65, EXPORT_STF_PANEL = 66, SEARCH_PILOT_POINTS_PANEL = 67,
			EXPORT_MULTIPLICATION_TABLES_PANEL = 68, SEARCH_MULTIPLICATION_TABLES_PANEL = 69, ADD_RFORT_OMISSIONS_PANEL = 70, UPLOAD_HELP_VIDEO_PANEL = 71, PLOT_RFORT_STRESSES_PANEL = 72, PLOT_RFORT_RESULTS_PANEL = 73, RFORT_REPORT_PANEL = 74, ADD_EQUIVALENT_STRESSES_PANEL = 75,
			STRESS_SEQEUNCE_INFO_PANEL = 76, AC_MODEL_INFO_PANEL = 77, SAVE_FLIGHT_DAMAGE_CONTRIBUTIONS_PANEL = 78, SEARCH_ENGINE_SETTINGS_PANEL = 79, DATA_INSIGHTS_PANEL = 80, EXECUTE_SQL_STATEMENT_PANEL = 81, ADAPT_DRF_PANEL = 82, MATERIAL_PANEL = 83, EXCALIBUR_PANEL = 84, ADD_NEW_USER_PANEL = 85,
			DELETE_USERS_PANEL = 86, EDIT_USER_PERMISSIONS_PANEL = 87, UPLOAD_APP_UPDATE_PANEL = 88, MANAGE_SERVICE_PANEL = 89;

	/** Popup index. */
	public static final int FATIGUE_MATERIALS_POPUP = 0, PREFFAS_MATERIALS_POPUP = 1, LINEAR_MATERIALS_POPUP = 2, LINK_PILOT_POINTS_POPUP = 3, SHARE_FILE_POPUP = 4, CHAT_POPUP = 5, SEGMENT_FACTORS_POPUP = 6, LOADCASE_FACTORS_POPUP = 7, DAMAGE_CONTRIBUTIONS_POPUP = 8;

	/** The main screen of the application. */
	private MainScreen owner_;

	/** Pagination control. */
	private Pagination pagination_;

	/** Index of previous panel. */
	private int previousPanelIndex_ = FILE_VIEW_PANEL;

	/** Sub panels. */
	private HashMap<Integer, InputSubPanel> subPanels_;

	/** Popups. */
	private HashMap<Integer, InputPopup> popups_;

	@FXML
	private VBox root_;

	@FXML
	private Label header_, statusLabel_;

	@FXML
	private MenuButton addButton_, automateButton_;

	@FXML
	private Region region_, region2_;

	@FXML
	private ToolBar toolbar_, statusbar_;

	@FXML
	private Button authentication_, dataService_, analysisService_, exchangeService_, automationService_;

	@FXML
	private Tooltip authenticationTooltip_, dataServiceTooltip_, exchangeServiceTooltip_, analysisServiceTooltip_, automationServiceTooltip_;

	@FXML
	private HBox services_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// load sub panels
		subPanels_ = new HashMap<>();
		subPanels_.put(FILE_VIEW_PANEL, FileViewPanel.load(this));
		subPanels_.put(ADD_SPECTRUM_PANEL, AddSpectrumPanel.load(this));
		subPanels_.put(GENERATE_STRESS_SEQUENCE_PANEL, GenerateStressSequencePanel.load(this));
		subPanels_.put(SPECTRUM_STATS_PANEL, StatisticsPanel.load(this));
		subPanels_.put(SETTINGS_PANEL, SettingsPanel.load(this));
		subPanels_.put(SEARCH_SPECTRA_PANEL, SearchSpectraPanel.load(this));
		subPanels_.put(HELP_PANEL, HelpPanel.load(this));
		subPanels_.put(BUG_REPORT_PANEL, BugReportPanel.load(this));
		subPanels_.put(LOGIN_PANEL, LoginPanel.load(this));
		subPanels_.put(ROADMAP_PANEL, RoadmapPanel.load(this));
		subPanels_.put(DUMMY_STF_PANEL, DummySTFPanel.load(this));
		subPanels_.put(HISTOGRAM_PANEL, HistogramPanel.load(this));
		subPanels_.put(EQUIVALENT_STRESS_PANEL, EquivalentStressPanel.load(this));
		subPanels_.put(COMPARE_STRESS_SEQUENCE_PANEL, CompareStressSequencePanel.load(this));
		subPanels_.put(LEVEL_CROSSING_PANEL, LevelCrossingPanel.load(this));
		subPanels_.put(DAMAGE_ANGLE_PANEL, DamageAnglePanel.load(this));
		subPanels_.put(PLOT_DAMAGE_ANGLE_PANEL, PlotDamageAnglePanel.load(this));
		subPanels_.put(PLOT_FLIGHTS_PANEL, PlotFlightsPanel.load(this));
		subPanels_.put(UPLOAD_CONTAINER_UPDATE_PANEL, UploadContainerUpdatePanel.load(this));
		subPanels_.put(COMPARE_FLIGHTS_PANEL, CompareFlightsPanel.load(this));
		subPanels_.put(COMPARE_EQUIVALENT_STRESS_PANEL, CompareEquivalentStressPanel.load(this));
		subPanels_.put(COMPARE_DAMAGE_ANGLE_LIFE_FACTORS_PANEL, CompareDamageAngleLifeFactorsPanel.load(this));
		subPanels_.put(MISSION_PARAMETERS_PANEL, MissionParametersPanel.load(this));
		subPanels_.put(SAVE_AS_1D_STF_PANEL, SaveAs1DSTFPanel.load(this));
		subPanels_.put(STF_EQUIVALENT_STRESS_PANEL, STFEquivalentStressPanel.load(this));
		subPanels_.put(RFORT_EXTENDED_PANEL, RfortExtendedPanel.load(this));
		subPanels_.put(MISSION_PROFILE_PANEL, MissionProfilePanel.load(this));
		subPanels_.put(ADD_STH_PANEL, AddSTHPanel.load(this));
		subPanels_.put(EXTERNAL_STATS_PANEL, ExternalStatisticsPanel.load(this));
		subPanels_.put(COMPARE_EXTERNAL_STRESS_SEQUENCE_PANEL, CompareExternalStressSequencePanel.load(this));
		subPanels_.put(EXTERNAL_LEVEL_CROSSING_PANEL, ExternalLevelCrossingPanel.load(this));
		subPanels_.put(PLOT_EXTERNAL_FLIGHTS_PANEL, PlotExternalFlightsPanel.load(this));
		subPanels_.put(COMPARE_EXTERNAL_FLIGHTS_PANEL, CompareExternalFlightsPanel.load(this));
		subPanels_.put(MYCHECK_PANEL, MyCheckPanel.load(this));
		subPanels_.put(UPLOAD_PLUGIN_PANEL, UploadPluginPanel.load(this));
		subPanels_.put(DAMAGE_CONTRIBUTION_PANEL, DamageContributionPanel.load(this));
		subPanels_.put(COMPARE_DAMAGE_CONTRIBUTIONS_PANEL, CompareDamageContributionsPanel.load(this));
		subPanels_.put(SAVE_EQUIVALENT_STRESS_PANEL, SaveEquivalentStressInfoPanel.load(this));
		subPanels_.put(HISTOGRAM_3D_PANEL, Histogram3DPanel.load(this));
		subPanels_.put(ADD_AC_MODEL_PANEL, AddAircraftModelPanel.load(this));
		subPanels_.put(PLOT_AC_STRUCTURE_PANEL, PlotAircraftStructurePanel.load(this));
		subPanels_.put(RENAME_ELEMENT_GROUPS_PANEL, RenameElementGroupPanel.load(this));
		subPanels_.put(DELETE_ELEMENT_GROUPS_PANEL, DeleteElementGroupsPanel.load(this));
		subPanels_.put(CREATE_ELEMENT_GROUP_FROM_EIDS_PANEL, CreateElementGroupFromEIDPanel.load(this));
		subPanels_.put(CREATE_ELEMENT_GROUP_FROM_COORDS_PANEL, CreateElementGroupFromCoordinatesPanel.load(this));
		subPanels_.put(CREATE_ELEMENT_GROUP_FROM_GROUPS_PANEL, CreateElementGroupFromGroupsPanel.load(this));
		subPanels_.put(CREATE_ELEMENT_GROUP_FROM_QV_LV_POSITIONS_PANEL, CreateElementGroupFromQVLVPanel.load(this));
		subPanels_.put(PLOT_ELEMENT_STRESSES_PANEL, PlotElementStressesPanel.load(this));
		subPanels_.put(COMPARE_ELEMENT_STRESSES_PANEL, CompareElementStressesPanel.load(this));
		subPanels_.put(SAVE_DAMAGE_CONTRIBUTIONS_PANEL, SaveDamageContributionsPanel.load(this));
		subPanels_.put(SAVE_DAMAGE_ANGLE_PANEL, SaveDamageAngleInfoPanel.load(this));
		subPanels_.put(COMPARE_LOAD_CASES_PANEL, CompareLoadCasesPanel.load(this));
		subPanels_.put(PLOT_AC_EQUIVALENT_STRESSES_PANEL, PlotAircraftEquivalentStressesPanel.load(this));
		subPanels_.put(COMPARE_AC_EQUIVALENT_STRESSES_PANEL, CompareAircraftEquivalentStressesPanel.load(this));
		subPanels_.put(GENERATE_LIFE_FACTOR_PANEL, GenerateLifeFactorPanel.load(this));
		subPanels_.put(GENERATE_EQUIVALENT_STRESS_RATIO_PANEL, GenerateEquivalentStressRatioPanel.load(this));
		subPanels_.put(SAVE_LIFE_FACTOR_PANEL, SaveLifeFactorPanel.load(this));
		subPanels_.put(SAVE_EQUIVALENT_STRESS_RATIO_PANEL, SaveEquivalentStressRatioPanel.load(this));
		subPanels_.put(GENERATE_AC_LIFE_FACTORS_PANEL, GenerateAircraftLifeFactorsPanel.load(this));
		subPanels_.put(GENERATE_AC_EQUIVALENT_STRESS_RATIOS_PANEL, GenerateAircraftEquivalentStressRatiosPanel.load(this));
		subPanels_.put(SAVE_AC_LIFE_FACTOR_PANEL, SaveAircraftLifeFactorPanel.load(this));
		subPanels_.put(SAVE_AC_EQUIVALENT_STRESS_RATIO_PANEL, SaveAircraftEquivalentStressRatioPanel.load(this));
		subPanels_.put(PLOT_AC_LIFE_FACTORS_PANEL, PlotAircraftLifeFactorsPanel.load(this));
		subPanels_.put(PLOT_AC_EQUIVALENT_STRESS_RATIOS_PANEL, PlotAircraftEquivalentStressRatiosPanel.load(this));
		subPanels_.put(STF_INFO_PANEL, STFInfoPanel.load(this));
		subPanels_.put(SPECTRUM_INFO_PANEL, SpectrumInfoPanel.load(this));
		subPanels_.put(EXPORT_STF_PANEL, ExportSTFPanel.load(this));
		subPanels_.put(SEARCH_PILOT_POINTS_PANEL, SearchPilotPointsPanel.load(this));
		subPanels_.put(EXPORT_MULTIPLICATION_TABLES_PANEL, ExportMultiplicationTablesPanel.load(this));
		subPanels_.put(SEARCH_MULTIPLICATION_TABLES_PANEL, SearchMultiplicationTablesPanel.load(this));
		subPanels_.put(ADD_RFORT_OMISSIONS_PANEL, AddRfortOmissionsPanel.load(this));
		subPanels_.put(UPLOAD_HELP_VIDEO_PANEL, UploadHelpVideoPanel.load(this));
		subPanels_.put(PLOT_RFORT_STRESSES_PANEL, RfortEquivalentStressPanel.load(this));
		subPanels_.put(PLOT_RFORT_RESULTS_PANEL, RfortResultsPanel.load(this));
		subPanels_.put(RFORT_REPORT_PANEL, RfortReportPanel.load(this));
		subPanels_.put(ADD_EQUIVALENT_STRESSES_PANEL, AddAircraftEquivalentStressesPanel.load(this));
		subPanels_.put(STRESS_SEQEUNCE_INFO_PANEL, StressSequenceInfoPanel.load(this));
		subPanels_.put(AC_MODEL_INFO_PANEL, AircraftModelInfoPanel.load(this));
		subPanels_.put(SAVE_FLIGHT_DAMAGE_CONTRIBUTIONS_PANEL, SaveFlightDamageContributionsPanel.load(this));
		subPanels_.put(SEARCH_ENGINE_SETTINGS_PANEL, SearchEngineSettingsPanel.load(this));
		subPanels_.put(DATA_INSIGHTS_PANEL, DataInsightsPanel.load(this));
		subPanels_.put(EXECUTE_SQL_STATEMENT_PANEL, ExecuteSQLStatementPanel.load(this));
		subPanels_.put(ADAPT_DRF_PANEL, AdaptDRFPanel.load(this));
		subPanels_.put(MATERIAL_PANEL, MaterialPanel.load(this));
		subPanels_.put(EXCALIBUR_PANEL, ExcaliburPanel.load(this));
		subPanels_.put(ADD_NEW_USER_PANEL, AddUserPanel.load(this));
		subPanels_.put(DELETE_USERS_PANEL, DeleteUsersPanel.load(this));
		subPanels_.put(EDIT_USER_PERMISSIONS_PANEL, EditUserPermissionsPanel.load(this));
		subPanels_.put(UPLOAD_APP_UPDATE_PANEL, UploadAppUpdatePanel.load(this));
		subPanels_.put(MANAGE_SERVICE_PANEL, ManageServicePanel.load(this));

		// create pagination control
		pagination_ = new Pagination(subPanels_.size(), FILE_VIEW_PANEL);
		pagination_.getStylesheets().add(Equinox.class.getResource("css/HiddenPagination.css").toString());
		VBox.setVgrow(pagination_, Priority.ALWAYS);

		// setup pagination page factory
		pagination_.setPageFactory(pageIndex -> {
			header_.setText(subPanels_.get(pageIndex).getHeader());
			subPanels_.get(pageIndex).showing();
			owner_.getMenuBarPanel().disableSelectedItems(pageIndex != FILE_VIEW_PANEL);
			statusLabel_.setVisible(pageIndex == FILE_VIEW_PANEL);
			return subPanels_.get(pageIndex).getRoot();
		});

		// add pagination to root container
		root_.getChildren().add(1, pagination_);

		// load popups
		popups_ = new HashMap<>();
		popups_.put(CHAT_POPUP, ChatPopup.load(this));
		popups_.put(LINK_PILOT_POINTS_POPUP, LinkPilotPointsPopup.load(this));
		popups_.put(FATIGUE_MATERIALS_POPUP, FatigueMaterialsPopup.load(this));
		popups_.put(PREFFAS_MATERIALS_POPUP, PreffasMaterialsPopup.load(this));
		popups_.put(LINEAR_MATERIALS_POPUP, LinearMaterialsPopup.load(this));
		popups_.put(SHARE_FILE_POPUP, ShareFilePopup.load(this));
		popups_.put(SEGMENT_FACTORS_POPUP, SegmentFactorsPopup.load(this));
		popups_.put(LOADCASE_FACTORS_POPUP, LoadcaseFactorsPopup.load(this));
		popups_.put(DAMAGE_CONTRIBUTIONS_POPUP, DamageContributionsPopup.load(this));
	}

	/**
	 * Starts this panel.
	 */
	public void start() {

		// set minimum toolbar width
		toolbar_.minWidthProperty().set(header_.getWidth() + addButton_.getWidth() + automateButton_.getWidth() + 2 * 11.0 + 3 * 4.0);
		statusbar_.minWidthProperty().set(services_.getWidth() + statusLabel_.getWidth() + 2 * 11.0 + 2 * 4.0);

		// bind region width to tool bar width
		region_.prefWidthProperty().bind(toolbar_.widthProperty().subtract(header_.widthProperty()).subtract(addButton_.widthProperty()).subtract(automateButton_.widthProperty()).subtract(2 * 11.0 + 3 * 4.0));
		region2_.prefWidthProperty().bind(statusbar_.widthProperty().subtract(services_.widthProperty()).subtract(statusLabel_.widthProperty()).subtract(2 * 11.0 + 2 * 4.0));

		// start internal sub panels
		for (InputSubPanel panel : subPanels_.values()) {
			if (panel instanceof InternalInputSubPanel) {
				((InternalInputSubPanel) panel).start();
			}
		}

		// set initial position
		root_.setTranslateX(-root_.getWidth());
	}

	/**
	 * Stops this panel.
	 */
	public void stop() {
		FileViewPanel panel = (FileViewPanel) subPanels_.get(FILE_VIEW_PANEL);
		panel.stop();
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
	 * Returns the root of this controller.
	 *
	 * @return The root of this controller.
	 */
	public VBox getRoot() {
		return root_;
	}

	/**
	 * Returns the demanded sub panel of this panel.
	 *
	 * @param index
	 *            Index of the demanded sub panel.
	 * @return The demanded sub panel of this panel.
	 */
	public InputSubPanel getSubPanel(int index) {
		return subPanels_.get(index);
	}

	/**
	 * Returns the demanded popup of this panel.
	 *
	 * @param index
	 *            Index of the demanded popup.
	 * @return The demanded popup of this panel.
	 */
	public InputPopup getPopup(int index) {
		return popups_.get(index);
	}

	/**
	 * Clears file selection.
	 */
	public void clearFileSelection() {
		((FileViewPanel) subPanels_.get(FILE_VIEW_PANEL)).clearFileSelection();
	}

	/**
	 * Returns file tree root.
	 *
	 * @return File tree root.
	 */
	public TreeItem<String> getFileTreeRoot() {
		return ((FileViewPanel) subPanels_.get(FILE_VIEW_PANEL)).getFileTreeRoot();
	}

	/**
	 * Returns selected files.
	 *
	 * @return Selected files.
	 */
	public ObservableList<TreeItem<String>> getSelectedFiles() {
		return ((FileViewPanel) subPanels_.get(FILE_VIEW_PANEL)).getSelectedFiles();
	}

	/**
	 * Finds similar items.
	 *
	 * @param items
	 *            Items to search for.
	 */
	public void search(SpectrumItem... items) {
		((FileViewPanel) subPanels_.get(FILE_VIEW_PANEL)).search(items);
	}

	/**
	 * Returns the settings.
	 *
	 * @return The settings.
	 */
	public Settings getSettings() {
		return ((SettingsPanel) subPanels_.get(SETTINGS_PANEL)).getSettings();
	}

	/**
	 * Shows demanded sub panel.
	 *
	 * @param index
	 *            Index of demanded sub panel.
	 */
	public void showSubPanel(int index) {
		previousPanelIndex_ = pagination_.getCurrentPageIndex();
		pagination_.setCurrentPageIndex(index);
	}

	/**
	 * Adds given input panel.
	 *
	 * @param inputPanel
	 *            Input panel to add.
	 * @return Index of added input panel.
	 */
	public int addInputPanel(InputSubPanel inputPanel) {
		int index = pagination_.getPageCount();
		subPanels_.put(index, inputPanel);
		pagination_.setPageCount(index + 1);
		return index;
	}

	/**
	 * Removes the input panel with the given index.
	 *
	 * @param index
	 *            Index of the input panel to remove.
	 */
	public void removeInputPanel(int index) {
		subPanels_.remove(index);
	}

	/**
	 * Returns current sub panel index.
	 *
	 * @return Current sub panel index.
	 */
	public int getCurrentSubPanelIndex() {
		return pagination_.getCurrentPageIndex();
	}

	/**
	 * Returns the previous sub panel index.
	 *
	 * @return The previous sub panel index.
	 */
	public int getPreviousSubPanelIndex() {
		return previousPanelIndex_;
	}

	/**
	 * Returns the status label of this panel.
	 *
	 * @return The status label of this panel.
	 */
	public Label getStatusLabel() {
		return statusLabel_;
	}

	/**
	 * Returns user authentication button.
	 *
	 * @return User authentication button.
	 */
	public Button getAuthenticationButton() {
		return authentication_;
	}

	/**
	 * Returns data service button.
	 *
	 * @return Data service button.
	 */
	public Button getDataServiceButton() {
		return dataService_;
	}

	/**
	 * Returns analysis service button.
	 *
	 * @return Analysis service button.
	 */
	public Button getAnalysisServiceButton() {
		return analysisService_;
	}

	/**
	 * Returns exchange service button.
	 *
	 * @return Exchange service button.
	 */
	public Button getExchangeServiceButton() {
		return exchangeService_;
	}

	/**
	 * Called when user authentication status changes.
	 *
	 * @param isExpired
	 *            True if user authentication is expired.
	 */
	public void authenticationStatusChanged(boolean isExpired) {
		authentication_.setStyle(isExpired ? "-fx-base:red" : "-fx-base:green");
		authenticationTooltip_.setText(isExpired ? "User Authentication: Expired" : "User Authentication: Valid");
	}

	/**
	 * Called when data service connection status changes.
	 *
	 * @param isConnected
	 *            True if connected to service.
	 */
	public void dataServiceConnectionStatusChanged(boolean isConnected) {
		dataService_.setStyle(isConnected ? "-fx-base:green" : "-fx-base:red");
		dataServiceTooltip_.setText(isConnected ? "Data Service: Available" : "Data Service: Not available");
	}

	/**
	 * Called when analysis service connection status changes.
	 *
	 * @param isConnected
	 *            True if connected to service.
	 */
	public void analysisServiceConnectionStatusChanged(boolean isConnected) {
		analysisService_.setStyle(isConnected ? "-fx-base:green" : "-fx-base:red");
		analysisServiceTooltip_.setText(isConnected ? "Analysis Service: Available" : "Analysis Service: Not available");
	}

	/**
	 * Called when exchange service connection status changes.
	 *
	 * @param isConnected
	 *            True if connected to service.
	 */
	public void exchangeServiceConnectionStatusChanged(boolean isConnected) {
		exchangeService_.setStyle(isConnected ? "-fx-base:green" : "-fx-base:red");
		exchangeServiceTooltip_.setText(isConnected ? "Collaboration Service: Available" : "Collaboration Service: Not available");
	}

	/**
	 * Called when automation service status changes.
	 *
	 * @param isAvailable
	 *            True if service is available.
	 */
	public void automationServiceStatusChanged(boolean isAvailable) {
		Platform.runLater(() -> {
			automationService_.setStyle(isAvailable ? "-fx-base:green" : "-fx-base:red");
			automationServiceTooltip_.setText(isAvailable ? "Automation Service: Available" : "Automation Service: Not available");
		});
	}

	@FXML
	private void onAuthenticationClicked() {

		// get user authentication
		UserAuthentication userAuth = (UserAuthentication) authentication_.getUserData();

		// expired
		String msg = null;
		if (userAuth == null || userAuth.isExpired()) {
			msg = "Your user authentication is expired. Please connect to data service to obtain new authentication.";
		}

		// valid
		else {
			msg = "Your user authentication will expire in " + userAuth.getAuthenticationExpiryDuration() + ".";
		}

		// show pop-up
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.BOTTOM_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel1.load(msg, 50, NotificationPanel1.INFO));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(authentication_);
	}

	@FXML
	private void onConnectToAnalysisServiceClicked() {

		// already connected
		if (owner_.getAnalysisServerManager().isConnected()) {
			String title = "No operation";
			String msg = "Already connected to Analysis Service.";
			owner_.getNotificationPane().showInfo(title, msg);
			return;
		}

		// connect
		owner_.getAnalysisServerManager().connect(null, true);
	}

	@FXML
	private void onConnectToDataServiceClicked() {

		// already connected
		if (owner_.getDataServerManager().isConnected()) {
			String title = "No operation";
			String msg = "Already connected to Data Service.";
			owner_.getNotificationPane().showInfo(title, msg);
			return;
		}

		// connect
		owner_.getDataServerManager().connect(null, true);
	}

	@FXML
	private void onConnectToExchangeServiceClicked() {

		// already connected
		if (owner_.getExchangeServerManager().isConnected()) {
			String title = "No operation";
			String msg = "Already connected to Collaboration Service.";
			owner_.getNotificationPane().showInfo(title, msg);
			return;
		}

		// connect
		owner_.getExchangeServerManager().connect(null, true);
	}

	@FXML
	private void onAutomationServiceClicked() {

		// alive
		if (owner_.getAutomationServer().isAlive()) {
			String title = "No operation";
			String msg = "Automation service is alive and running.";
			owner_.getNotificationPane().showInfo(title, msg);
			return;
		}

		// create and start new automation server
		owner_.createAndStartAutomationServer();
	}

	@FXML
	private void onHealthMonitoringClicked() {
		((HealthMonitorViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.HEALTH_MONITOR_VIEW)).requestServerStatistics();
		owner_.getViewPanel().showSubPanel(ViewPanel.HEALTH_MONITOR_VIEW);
	}

	@FXML
	public void onMessageClicked() {
		((ChatPopup) getPopup(CHAT_POPUP)).show(null);
	}

	@FXML
	public void onAddCDFSetClicked() {
		showSubPanel(ADD_SPECTRUM_PANEL);
	}

	@FXML
	public void onAddSTHClicked() {
		showSubPanel(ADD_STH_PANEL);
	}

	@FXML
	public void onAddACModelClicked() {
		showSubPanel(ADD_AC_MODEL_PANEL);
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
	private void onDownloadSampleSIGMAClicked() {
		owner_.downloadSampleInput("AddNewStressSequenceFromSIGMA");
	}

	@FXML
	public void onRunInstructionSetClicked() {

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
		owner_.getActiveTasksPanel().runTaskInParallel(new CheckInstructionSet(file.toPath(), CheckInstructionSet.RUN));
	}

	@FXML
	public void onCheckInstructionSetClicked() {

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
		owner_.getActiveTasksPanel().runTaskInParallel(new CheckInstructionSet(file.toPath(), CheckInstructionSet.CHECK));
	}

	/**
	 * Loads and returns files panel.
	 *
	 * @param owner
	 *            Main screen.
	 * @return The newly loaded files panel.
	 */
	public static InputPanel load(MainScreen owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("InputPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			InputPanel controller = (InputPanel) fxmlLoader.getController();

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

	/**
	 * Interface for internal input sub panel.
	 *
	 * @author Murat Artim
	 * @date Dec 27, 2013
	 * @time 11:22:20 PM
	 */
	public interface InternalInputSubPanel extends InputSubPanel, Initializable {

		/**
		 * Returns the owner panel of this sub panel.
		 *
		 * @return The owner panel of this sub panel.
		 */
		InputPanel getOwner();

		/**
		 * Called only once, after the object structure is initialized.
		 */
		void start();
	}

	/**
	 * Interface for input popup.
	 *
	 * @author Murat Artim
	 * @date Dec 15, 2015
	 * @time 3:59:17 PM
	 */
	public interface InputPopup extends Initializable {

		/**
		 * Returns the owner panel of this sub panel.
		 *
		 * @return The owner panel of this sub panel.
		 */
		InputPanel getOwner();
	}
}
