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
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.Equinox;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.BasicSearchInput;
import equinox.dataServer.remote.data.SearchItem;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.dataServer.remote.data.SpectrumSearchInput;
import equinox.task.AdvancedSpectrumSearch;
import equinox.task.BasicSpectrumSearch;
import equinox.utility.Utility;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

/**
 * Class for search CDF panel.
 *
 * @author Murat Artim
 * @date May 2, 2014
 * @time 4:34:08 PM
 */
public class SearchSpectraPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextField cdfName_, acProgram_, acSection_, fatMission_, keywords_, fatMissionIssue_, flpIssue_, iflpIssue_, cdfIssue_, deliveryRef_, description_;

	@FXML
	private ToggleGroup nameCriteria_, acProgramCriteria_, acSectionCriteria_, missionCriteria_, missionIssueCriteria_, flpIssueCriteria_, iflpIssueCriteria_, cdfIssueCriteria_, descriptionCriteria_, deliveryRefCriteria_;

	@FXML
	private Button clear_, search_;

	@FXML
	private TitledPane basicSearchPane_, advancedSearchPane_;

	@FXML
	private Accordion accordion_;

	@FXML
	private ImageView goImage_;

	/** Search task status binding. */
	private SimpleBooleanProperty isSearchRunning_;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup search field
		keywords_.textProperty().addListener((ChangeListener<String>) (ov, old_Val, new_val) -> clear_.setVisible(!new_val.isEmpty()));

		// set hand cursor to clear button
		Utility.setHandCursor(clear_);

		// bind components
		isSearchRunning_ = new SimpleBooleanProperty(false);
		search_.disableProperty().bind(isSearchRunning_);
		isSearchRunning_.addListener((ChangeListener) (observable, oldValue, newValue) -> goImage_.setImage((boolean) newValue ? Utility.getImage("taskManager.gif") : Utility.getImage("runNowWhite.png")));

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	/**
	 * Returns the selected criteria for the given toggle group.
	 *
	 * @param tg
	 *            Toggle group.
	 * @return The selected criteria.
	 */
	private static int getSelectedCriteria(ToggleGroup tg) {
		int index = 0;
		for (final Toggle t : tg.getToggles()) {
			if (t.isSelected()) {
				break;
			}
			index++;
		}
		return index;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static SearchSpectraPanel load(InputPanel owner) {

		try {

			// load fxml file
			final FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("SearchSpectraPanel.fxml"));
			final Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			final SearchSpectraPanel controller = (SearchSpectraPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Shows missing input warning message.
	 *
	 * @param message
	 *            Message text to show.
	 * @param node
	 *            Node to show the warning message on.
	 */
	private static void showWarningMessage(String message, Node node) {
		final PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(node);
	}

	/**
	 * Performs advanced search.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void advancedSearch() throws Exception {

		// create search input
		SpectrumSearchInput input = new SpectrumSearchInput();

		// add search items
		String name = cdfName_.getText();
		if (name != null && !name.isEmpty()) {
			input.addInput(SpectrumInfoType.NAME, new SearchItem(name, getSelectedCriteria(nameCriteria_)));
		}
		String acProgram = acProgram_.getText();
		if (acProgram != null && !acProgram.isEmpty()) {
			input.addInput(SpectrumInfoType.AC_PROGRAM, new SearchItem(acProgram, getSelectedCriteria(acProgramCriteria_)));
		}
		String acSection = acSection_.getText();
		if (acSection != null && !acSection.isEmpty()) {
			input.addInput(SpectrumInfoType.AC_SECTION, new SearchItem(acSection, getSelectedCriteria(acSectionCriteria_)));
		}
		String fatMission = fatMission_.getText();
		if (fatMission != null && !fatMission.isEmpty()) {
			input.addInput(SpectrumInfoType.FAT_MISSION, new SearchItem(fatMission, getSelectedCriteria(missionCriteria_)));
		}
		String fatMissionIssue = fatMissionIssue_.getText();
		if (fatMissionIssue != null && !fatMissionIssue.isEmpty()) {
			input.addInput(SpectrumInfoType.FAT_MISSION_ISSUE, new SearchItem(fatMissionIssue, getSelectedCriteria(missionIssueCriteria_)));
		}
		String flpIssue = flpIssue_.getText();
		if (flpIssue != null && !flpIssue.isEmpty()) {
			input.addInput(SpectrumInfoType.FLP_ISSUE, new SearchItem(flpIssue, getSelectedCriteria(flpIssueCriteria_)));
		}
		String iflpIssue = iflpIssue_.getText();
		if (iflpIssue != null && !iflpIssue.isEmpty()) {
			input.addInput(SpectrumInfoType.IFLP_ISSUE, new SearchItem(iflpIssue, getSelectedCriteria(iflpIssueCriteria_)));
		}
		String cdfIssue = cdfIssue_.getText();
		if (cdfIssue != null && !cdfIssue.isEmpty()) {
			input.addInput(SpectrumInfoType.CDF_ISSUE, new SearchItem(cdfIssue, getSelectedCriteria(cdfIssueCriteria_)));
		}
		String deliveryRef = deliveryRef_.getText();
		if (deliveryRef != null && !deliveryRef.isEmpty()) {
			input.addInput(SpectrumInfoType.DELIVERY_REF, new SearchItem(deliveryRef, getSelectedCriteria(deliveryRefCriteria_)));
		}
		String description = description_.getText();
		if (description != null && !description.isEmpty()) {
			input.addInput(SpectrumInfoType.DESCRIPTION, new SearchItem(description, getSelectedCriteria(descriptionCriteria_)));
		}

		// no search items entered
		if (input.isEmpty()) {
			showWarningMessage("No search criteria entered. Please enter at least 1 search item to proceed.", search_);
			return;
		}

		// set engine settings
		SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) owner_.getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
		panel.setEngineSettings(input);

		// search
		AdvancedSpectrumSearch task = new AdvancedSpectrumSearch(input);
		isSearchRunning_.unbind();
		isSearchRunning_.bind(task.runningProperty());
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(task);
	}

	/**
	 * Performs basic search.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void basicSeach() throws Exception {

		// get keywords
		String keywords = keywords_.getText().trim();

		// no keyword entered
		if (keywords == null || keywords.isEmpty()) {
			showWarningMessage("No search keyword entered. Please enter keywords to proceed.", keywords_);
			return;
		}

		// create input list
		ArrayList<String> inputs = new ArrayList<>();

		// multiple keywords
		if (keywords.contains(",")) {

			// split
			String[] split = keywords.split(",");

			// no keywords
			if (split.length == 0) {
				showWarningMessage("No search keyword entered. Please enter keywords to proceed.", keywords_);
				return;
			}

			// add words to inputs
			for (String word : split) {
				word = word.trim();
				if (!word.isEmpty()) {
					inputs.add(word);
				}
			}

			// no keywords
			if (inputs.isEmpty()) {
				showWarningMessage("No search keyword entered. Please enter keywords to proceed.", keywords_);
				return;
			}
		}
		else {
			inputs.add(keywords);
		}

		// create input
		BasicSearchInput input = new BasicSearchInput();
		input.setKeywords(inputs);

		// set engine settings
		SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) owner_.getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
		panel.setEngineSettings(input);

		// search
		BasicSpectrumSearch task = new BasicSpectrumSearch(input);
		isSearchRunning_.unbind();
		isSearchRunning_.bind(task.runningProperty());
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(task);
	}

	@Override
	public String getHeader() {
		return "Search Spectra";
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onClearSearchClicked() {
		keywords_.clear();
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to search and download spectra from AFM database", null);
	}

	@FXML
	private void onResetClicked() {

		// reset search fields
		TextField[] fields = { cdfName_, acProgram_, acSection_, fatMission_, fatMissionIssue_, flpIssue_, iflpIssue_, cdfIssue_, deliveryRef_, description_ };
		for (TextField field : fields) {
			field.clear();
		}

		// reset keywords
		keywords_.clear();

		// reset criterion
		ToggleGroup[] toggles = { nameCriteria_, acProgramCriteria_, acSectionCriteria_, missionCriteria_, missionIssueCriteria_, flpIssueCriteria_, iflpIssueCriteria_, cdfIssueCriteria_, descriptionCriteria_, deliveryRefCriteria_ };
		for (ToggleGroup tg : toggles) {
			tg.getToggles().get(0).setSelected(true);
		}
	}

	@FXML
	private void onSearchClicked() {

		// already a search is running
		if (isSearchRunning_.get())
			return;

		try {

			// get search type
			boolean advanced = advancedSearchPane_.isExpanded();

			// advanced search
			if (advanced) {
				advancedSearch();
			}
			else {
				basicSeach();
			}
		}

		// exception occurred
		catch (Exception e) {

			// log exception
			Equinox.LOGGER.log(Level.WARNING, "Exception occured during setting database search inputs.", e);

			// show warning
			String message = "Exception occured during setting database search inputs: " + e.getLocalizedMessage();
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(search_);
		}
	}

	@Override
	public void showing() {
		// no implementation
	}

	@Override
	public void start() {
		// no implementation
	}
}
