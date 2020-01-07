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
import java.util.List;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.CompareDamageContributionsInput;
import equinox.dataServer.remote.data.ContributionType;
import equinox.task.GetContributionNames;
import equinox.task.GetContributionNames.DamageContributionRequester;
import equinox.task.PlotDamageComparison;
import equinox.utility.Utility;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

/**
 * Class for compare damage contributions panel controller.
 *
 * @author Murat Artim
 * @date Apr 15, 2015
 * @time 5:27:57 PM
 */
public class CompareDamageContributionsPanel implements InternalInputSubPanel, DamageContributionRequester {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_, incPane_;

	@FXML
	private RadioButton incCont_, onegCont_, gagCont_, dpCont_, dtCont_;

	@FXML
	private ListView<String> contributionList_;

	@FXML
	private ToggleSwitch includeSpectrumName_, includeSTFName_, includeEID_, includeMaterialName_, includeProgram_, includeSection_, includeMission_;

	@FXML
	private TitledPane namingPane_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		contributionList_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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

		// expand first panel
		accordion_.setExpandedPane(accordion_.getPanes().get(0));

		// add selected contributions
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();
		ArrayList<SpectrumItem> contributions = new ArrayList<>();
		for (TreeItem<String> item : selected) {
			contributions.add((SpectrumItem) item);
		}

		// get contribution list
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetContributionNames(this, contributions));
	}

	@Override
	public String getHeader() {
		return "Compare Contributions";
	}

	@Override
	public void setContributions(List<String> contributions) {

		// reset incremental contributions list
		contributionList_.getSelectionModel().clearSelection();
		contributionList_.getItems().clear();

		// add contributions
		boolean hasGAG = false, has1G = false, hasDP = false, hasDT = false;
		for (String cont : contributions) {
			if (cont.equals(ContributionType.ONEG.getName())) {
				has1G = true;
			}
			else if (cont.equals(ContributionType.GAG.getName())) {
				hasGAG = true;
			}
			else if (cont.equals(ContributionType.DELTA_P.getName())) {
				hasDP = true;
			}
			else if (cont.equals(ContributionType.DELTA_T.getName())) {
				hasDT = true;
			}
			else {
				contributionList_.getItems().add(cont);
			}
		}

		// setup contribution types
		incCont_.setDisable(contributionList_.getItems().isEmpty());
		onegCont_.setDisable(!has1G);
		gagCont_.setDisable(!hasGAG);
		dpCont_.setDisable(!hasDP);
		dtCont_.setDisable(!hasDT);

		// reset panel
		onResetClicked();
	}

	@FXML
	private void onContributionTypeSelected() {
		incPane_.setDisable(!incCont_.isSelected());
	}

	@FXML
	private void onResetClicked() {

		// reset contribution types
		if (!incCont_.isDisabled()) {
			incCont_.setSelected(true);
		}
		else if (incCont_.isDisabled() && !onegCont_.isDisabled()) {
			onegCont_.setSelected(true);
		}
		else if (incCont_.isDisabled() && onegCont_.isDisabled() && !gagCont_.isDisabled()) {
			gagCont_.setSelected(true);
		}
		else if (incCont_.isDisabled() && onegCont_.isDisabled() && gagCont_.isDisabled() && !dpCont_.isDisabled()) {
			dpCont_.setSelected(true);
		}
		else if (incCont_.isDisabled() && onegCont_.isDisabled() && gagCont_.isDisabled() && dpCont_.isDisabled() && !dtCont_.isDisabled()) {
			dtCont_.setSelected(true);
		}

		// reset incremental contributions pane
		incPane_.setDisable(!incCont_.isSelected());

		// reset naming options
		includeSpectrumName_.setSelected(false);
		if (!includeSTFName_.isSelected()) {
			includeSTFName_.setSelected(true);
		}
		includeEID_.setSelected(false);
		includeMaterialName_.setSelected(false);
		includeProgram_.setSelected(false);
		includeSection_.setSelected(false);
		includeMission_.setSelected(false);
	}

	@FXML
	private void onOkClicked() {

		// get contributions names
		ArrayList<String> contributionNames = new ArrayList<>();
		ContributionType contributionType = null;

		// incremental
		if (incCont_.isSelected()) {
			for (String name : contributionList_.getSelectionModel().getSelectedItems()) {
				contributionNames.add(name);
			}
			contributionType = ContributionType.INCREMENT;
		}

		// 1g
		else if (onegCont_.isSelected()) {
			contributionNames.add(ContributionType.ONEG.getName());
			contributionType = ContributionType.ONEG;
		}

		// GAG
		else if (gagCont_.isSelected()) {
			contributionNames.add(ContributionType.GAG.getName());
			contributionType = ContributionType.GAG;
		}

		// delta-p
		else if (dpCont_.isSelected()) {
			contributionNames.add(ContributionType.DELTA_P.getName());
			contributionType = ContributionType.DELTA_P;
		}

		// delta-t
		else if (dtCont_.isSelected()) {
			contributionNames.add(ContributionType.DELTA_T.getName());
			contributionType = ContributionType.DELTA_T;
		}

		// no contribution name given
		if (contributionNames.isEmpty()) {
			String message = "Please select at least 1 contribution to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(contributionList_);
			return;
		}

		// get naming parameters
		boolean[] namingOptions = new boolean[7];
		namingOptions[CompareDamageContributionsInput.SPECTRUM_NAME] = includeSpectrumName_.isSelected();
		namingOptions[CompareDamageContributionsInput.STF_NAME] = includeSTFName_.isSelected();
		namingOptions[CompareDamageContributionsInput.EID] = includeEID_.isSelected();
		namingOptions[CompareDamageContributionsInput.MATERIAL_NAME] = includeMaterialName_.isSelected();
		namingOptions[CompareDamageContributionsInput.PROGRAM] = includeProgram_.isSelected();
		namingOptions[CompareDamageContributionsInput.SECTION] = includeSection_.isSelected();
		namingOptions[CompareDamageContributionsInput.MISSION] = includeMission_.isSelected();

		// no naming selected
		boolean anySelected = false;
		for (boolean option : namingOptions) {
			if (option) {
				anySelected = true;
				break;
			}
		}
		if (!anySelected) {
			String message = "Please select at least 1 naming for plot series.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(namingPane_);
			return;
		}

		// create input
		CompareDamageContributionsInput input = new CompareDamageContributionsInput(contributionNames, namingOptions);

		// create task
		PlotDamageComparison task = new PlotDamageComparison(input, contributionType);

		// get selected contributions
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();
		for (TreeItem<String> item : selected) {
			task.addContribution((LoadcaseDamageContributions) item);
		}

		// plot
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(task);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to compare damage contributions", null);
	}

	/**
	 * Loads and returns spectrum statistics panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded spectrum statistics panel.
	 */
	public static CompareDamageContributionsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CompareDamageContributionsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CompareDamageContributionsPanel controller = (CompareDamageContributionsPanel) fxmlLoader.getController();

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
