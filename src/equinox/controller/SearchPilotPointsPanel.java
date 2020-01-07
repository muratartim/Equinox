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
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.data.PilotPointSearchInput;
import equinox.dataServer.remote.data.SearchItem;
import equinox.task.AdvancedPilotPointSearch;
import equinox.task.BasicPilotPointSearch;
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
 * Class for search pilot points panel controller.
 *
 * @author Murat Artim
 * @date Feb 15, 2016
 * @time 11:14:37 AM
 */
public class SearchPilotPointsPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextField keywords_, pilotPointName_, spectrumName_, acProgram_, acSection_, fatMission_, framePos_, stringerPos_, dataSource_, genSource_, deliveryRef_, description_, issue_, fatigueMaterial_, preffasMaterial_, linearMaterial_, eid_;

	@FXML
	private ToggleGroup ppNameCriteria_, spectrumNameCriteria_, acProgramCriteria_, acSectionCriteria_, missionCriteria_, framePosCriteria_, stringerPosCriteria_, dataSourceCriteria_, genSourceCriteria_, deliveryRefCriteria_, descriptionCriteria_, issueCriteria_, fatigueMaterialCriteria_,
			preffasMaterialCriteria_, linearMaterialCriteria_, eidCriteria_;

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
		// no implementation
	}

	@Override
	public String getHeader() {
		return "Search Pilot Points";
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {

		// reset search fields
		TextField[] fields = { pilotPointName_, spectrumName_, acProgram_, acSection_, fatMission_, framePos_, stringerPos_, dataSource_, genSource_, deliveryRef_, description_, issue_, fatigueMaterial_, preffasMaterial_, linearMaterial_, eid_ };
		for (TextField field : fields) {
			field.clear();
		}

		// reset keywords
		keywords_.clear();

		// reset criterion
		ToggleGroup[] toggles = { ppNameCriteria_, spectrumNameCriteria_, acProgramCriteria_, acSectionCriteria_, missionCriteria_, framePosCriteria_, stringerPosCriteria_, dataSourceCriteria_, genSourceCriteria_, deliveryRefCriteria_, descriptionCriteria_, issueCriteria_, fatigueMaterialCriteria_,
				preffasMaterialCriteria_, linearMaterialCriteria_, eidCriteria_ };
		for (ToggleGroup tg : toggles) {
			tg.getToggles().get(0).setSelected(true);
		}
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
		BasicPilotPointSearch task = new BasicPilotPointSearch(input);
		isSearchRunning_.unbind();
		isSearchRunning_.bind(task.runningProperty());
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(task);
	}

	/**
	 * Performs advanced search.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void advancedSearch() throws Exception {

		// create search input
		PilotPointSearchInput input = new PilotPointSearchInput();

		// add search items
		String pilotPointName = pilotPointName_.getText();
		if (pilotPointName != null && !pilotPointName.isEmpty()) {
			input.addInput(PilotPointInfoType.NAME, new SearchItem(pilotPointName, getSelectedCriteria(ppNameCriteria_)));
		}
		String spectrumName = spectrumName_.getText();
		if (spectrumName != null && !spectrumName.isEmpty()) {
			input.addInput(PilotPointInfoType.SPECTRUM_NAME, new SearchItem(spectrumName, getSelectedCriteria(spectrumNameCriteria_)));
		}
		String program = acProgram_.getText();
		if (program != null && !program.isEmpty()) {
			input.addInput(PilotPointInfoType.AC_PROGRAM, new SearchItem(program, getSelectedCriteria(acProgramCriteria_)));
		}
		String section = acSection_.getText();
		if (section != null && !section.isEmpty()) {
			input.addInput(PilotPointInfoType.AC_SECTION, new SearchItem(section, getSelectedCriteria(acSectionCriteria_)));
		}
		String mission = fatMission_.getText();
		if (mission != null && !mission.isEmpty()) {
			input.addInput(PilotPointInfoType.FAT_MISSION, new SearchItem(mission, getSelectedCriteria(missionCriteria_)));
		}
		String framePos = framePos_.getText();
		if (framePos != null && !framePos.isEmpty()) {
			input.addInput(PilotPointInfoType.FRAME_RIB_POSITION, new SearchItem(framePos, getSelectedCriteria(framePosCriteria_)));
		}
		String stringerPos = stringerPos_.getText();
		if (stringerPos != null && !stringerPos.isEmpty()) {
			input.addInput(PilotPointInfoType.STRINGER_POSITION, new SearchItem(stringerPos, getSelectedCriteria(stringerPosCriteria_)));
		}
		String dataSource = dataSource_.getText();
		if (dataSource != null && !dataSource.isEmpty()) {
			input.addInput(PilotPointInfoType.DATA_SOURCE, new SearchItem(dataSource, getSelectedCriteria(dataSourceCriteria_)));
		}
		String genSource = genSource_.getText();
		if (genSource != null && !genSource.isEmpty()) {
			input.addInput(PilotPointInfoType.GENERATION_SOURCE, new SearchItem(genSource, getSelectedCriteria(genSourceCriteria_)));
		}
		String deliveryRef = deliveryRef_.getText();
		if (deliveryRef != null && !deliveryRef.isEmpty()) {
			input.addInput(PilotPointInfoType.DELIVERY_REF_NUM, new SearchItem(deliveryRef, getSelectedCriteria(deliveryRefCriteria_)));
		}
		String description = description_.getText();
		if (description != null && !description.isEmpty()) {
			input.addInput(PilotPointInfoType.DESCRIPTION, new SearchItem(description, getSelectedCriteria(descriptionCriteria_)));
		}
		String issue = issue_.getText();
		if (issue != null && !issue.isEmpty()) {
			input.addInput(PilotPointInfoType.ISSUE, new SearchItem(issue, getSelectedCriteria(issueCriteria_)));
		}
		String fatigueMaterial = fatigueMaterial_.getText();
		if (fatigueMaterial != null && !fatigueMaterial.isEmpty()) {
			input.addInput(PilotPointInfoType.FATIGUE_MATERIAL, new SearchItem(fatigueMaterial, getSelectedCriteria(fatigueMaterialCriteria_)));
		}
		String preffasMaterial = preffasMaterial_.getText();
		if (preffasMaterial != null && !preffasMaterial.isEmpty()) {
			input.addInput(PilotPointInfoType.PREFFAS_MATERIAL, new SearchItem(preffasMaterial, getSelectedCriteria(preffasMaterialCriteria_)));
		}
		String linearMaterial = linearMaterial_.getText();
		if (linearMaterial != null && !linearMaterial.isEmpty()) {
			input.addInput(PilotPointInfoType.LINEAR_MATERIAL, new SearchItem(linearMaterial, getSelectedCriteria(linearMaterialCriteria_)));
		}
		String eid = eid_.getText();
		if (eid != null && !eid.isEmpty()) {
			input.addInput(PilotPointInfoType.EID, new SearchItem(eid, getSelectedCriteria(eidCriteria_)));
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
		AdvancedPilotPointSearch task = new AdvancedPilotPointSearch(input);
		isSearchRunning_.unbind();
		isSearchRunning_.bind(task.runningProperty());
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(task);
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
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(node);
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
		for (Toggle t : tg.getToggles()) {
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
	public static SearchPilotPointsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("SearchPilotPointsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			SearchPilotPointsPanel controller = (SearchPilotPointsPanel) fxmlLoader.getController();

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
