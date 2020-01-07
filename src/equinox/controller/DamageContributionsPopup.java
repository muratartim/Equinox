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

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import com.jfoenix.controls.JFXTabPane;

import equinox.controller.InputPanel.InputPopup;
import equinox.data.DamageContribution;
import equinox.data.EquinoxTheme;
import equinox.data.LoadcaseFactor;
import equinox.data.LoadcaseItem;
import equinox.data.fileType.Spectrum;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.dataServer.remote.data.ContributionType;
import equinox.task.GetLoadcases;
import equinox.task.GetLoadcases.LoadcaseRequestingPanel;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

/**
 * Class for damage contributions popup controller.
 *
 * @author Murat Artim
 * @date Dec 17, 2015
 * @time 10:56:55 AM
 */
public class DamageContributionsPopup implements InputPopup, LoadcaseRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	/** Requesting panel. */
	private DamageContributionAddingPanel panel_;

	/** Incremental loadcase list. */
	private final ObservableList<LoadcaseItem> incLoadcases_ = FXCollections.observableArrayList();

	/** Steady damage contribution list. */
	private final ObservableList<DamageContribution> steadyCases_ = FXCollections.observableArrayList();

	@FXML
	private VBox root_;

	@FXML
	private TableView<LoadcaseItem> incTable_;

	@FXML
	private ListView<DamageContribution> steadyList_;

	@FXML
	private TextField search_, contributionName_;

	@FXML
	private Button cancelSearch_, ok_;

	@FXML
	private JFXTabPane loadcaseTypeTab_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// bind components
		loadcaseTypeTab_.getSelectionModel().selectedIndexProperty().addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
			boolean isSteady = loadcaseTypeTab_.getSelectionModel().getSelectedIndex() == 0;
			if (isSteady) {
				contributionName_.clear();
				contributionName_.setDisable(true);
			}
			else {
				contributionName_.setDisable(false);
			}
		});

		// create loadcase table columns
		TableColumn<LoadcaseItem, String> loadcaseNumberCol = new TableColumn<>("Loadcase");
		loadcaseNumberCol.setCellValueFactory(new PropertyValueFactory<LoadcaseItem, String>("loadcaseNumber"));
		TableColumn<LoadcaseItem, String> eventNameCol = new TableColumn<>("Event Name");
		eventNameCol.setCellValueFactory(new PropertyValueFactory<LoadcaseItem, String>("eventName"));
		TableColumn<LoadcaseItem, String> commentsCol = new TableColumn<>("Comments");
		commentsCol.setCellValueFactory(new PropertyValueFactory<LoadcaseItem, String>("comments"));
		incTable_.getColumns().add(loadcaseNumberCol);
		incTable_.getColumns().add(eventNameCol);
		incTable_.getColumns().add(commentsCol);

		// create table placeholders
		incTable_.setPlaceholder(NoResultsPanel.load("Your search did not match any loadcase.", null));

		// set automatic column sizing for tables
		incTable_.setColumnResizePolicy(param -> true);

		// setup tables
		incTable_.setItems(incLoadcases_);
		incTable_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		incTable_.setTableMenuButtonVisible(true);

		// setup steady case list
		steadyCases_.add(new DamageContribution(ContributionType.ONEG.getName(), null, ContributionType.ONEG));
		steadyCases_.add(new DamageContribution(ContributionType.DELTA_P.getName(), null, ContributionType.DELTA_P));
		steadyCases_.add(new DamageContribution(ContributionType.DELTA_T.getName(), null, ContributionType.DELTA_T));
		steadyCases_.add(new DamageContribution(ContributionType.GAG.getName(), null, ContributionType.GAG));
		steadyList_.setItems(steadyCases_);
		steadyList_.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		steadyList_.setPlaceholder(NoResultsPanel.load("Your search did not match any steady case.", null));

		// setup search field
		search_.textProperty().addListener((ChangeListener<String>) (ov, old_Val, new_val) -> {
			boolean isSteady = loadcaseTypeTab_.getSelectionModel().getSelectedIndex() == 0;
			search(old_Val, new_val, isSteady);
			if (!isSteady) {
				contributionName_.setText(new_val);
			}
		});
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public void setLoadcases(ArrayList<LoadcaseItem> loadcases) {

		// set table items
		incLoadcases_.clear();
		for (LoadcaseItem loadcase : loadcases) {
			incLoadcases_.add(loadcase);
		}
		incTable_.setItems(incLoadcases_);

		// reset
		search_.clear();
		incTable_.getSelectionModel().clearSelection();
		steadyList_.getSelectionModel().clearSelection();
		loadcaseTypeTab_.getSelectionModel().select(0);
		contributionName_.clear();

		// show popup
		popOver_.show(panel_.getDamageContributionPopupNode());

		// focus on search
		search_.requestFocus();
	}

	/**
	 * Shows this panel.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param spectrum
	 *            Selected spectrum.
	 */
	public void show(DamageContributionAddingPanel panel, Spectrum spectrum) {

		// already shown
		if (isShown_)
			return;

		// set panel
		panel_ = panel;

		// create pop-over
		popOver_ = new PopOver();
		popOver_.setArrowLocation(ArrowLocation.LEFT_TOP);
		popOver_.setDetached(false);
		popOver_.setHideOnEscape(false);
		popOver_.setAutoHide(false);
		popOver_.setContentNode(root_);

		// set showing handler
		popOver_.setOnShowing(event -> {
			owner_.getOwner().getRoot().setMouseTransparent(true);
			isShown_ = true;
		});

		// set hidden handler
		popOver_.setOnHidden(event -> {
			owner_.getOwner().getRoot().setMouseTransparent(false);
			isShown_ = false;
		});

		// request loadcases
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetLoadcases(this, spectrum, GetLoadcases.INCREMENT));
	}

	@FXML
	private void onCancelSearchClicked() {
		search_.clear();
	}

	@FXML
	private void onOkClicked() {

		// get loadcase type
		boolean isSteady = loadcaseTypeTab_.getSelectionModel().getSelectedIndex() == 0;

		// steady
		if (isSteady) {

			// get selected contribution
			DamageContribution selected = steadyList_.getSelectionModel().getSelectedItem();

			// check inputs
			if (!checkSteadyInputs(selected))
				return;

			// add damage contribution to requesting panel
			panel_.addDamageContribution(selected);
		}

		// increment
		else {

			// get selected loadcases
			ObservableList<LoadcaseItem> selected = incTable_.getSelectionModel().getSelectedItems();

			// check inputs
			if (!checkIncrementInputs(selected))
				return;

			// create loadcase factors
			ArrayList<LoadcaseFactor> loadcaseFactors = new ArrayList<>();
			for (LoadcaseItem loadcase : selected) {
				LoadcaseFactor lf = new LoadcaseFactor();
				lf.setIsOneg(loadcase.isOneg());
				lf.setLoadcaseNumber(loadcase.getLoadcaseNumber());
				lf.setEventName(loadcase.getEventName());
				lf.setComments(loadcase.getComments());
				lf.setModifier(GenerateStressSequenceInput.MULTIPLY, 0.0);
				loadcaseFactors.add(lf);
			}

			// get contribution name
			String name = contributionName_.getText();

			// add damage contribution to requesting panel
			panel_.addDamageContribution(new DamageContribution(name, loadcaseFactors, ContributionType.INCREMENT));
		}

		// hide
		popOver_.hide();
	}

	/**
	 * Checks steady loadcase inputs.
	 *
	 * @param selectedContribution
	 *            Selected contribution.
	 * @return True if the inputs are valid.
	 */
	private boolean checkSteadyInputs(DamageContribution selectedContribution) {

		// no contribution selected
		if (selectedContribution == null) {
			String message = "No steady case selected. Please select 1 steady case to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(steadyList_);
			return false;
		}

		// check if contribution name already exists
		String name = selectedContribution.getName();
		ObservableList<DamageContribution> contributions = panel_.getDamageContributions();
		for (DamageContribution c : contributions) {
			if (c.getName().equals(name)) {
				String message = "Selected steady case '" + name + "' is already added. Please select another case.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(ok_);
				return false;
			}
		}

		// valid inputs
		return true;
	}

	/**
	 * Checks increment loadcase inputs.
	 *
	 * @param selectedLoadcases
	 *            Selected loadcases.
	 * @return True if the inputs are valid.
	 */
	private boolean checkIncrementInputs(ObservableList<LoadcaseItem> selectedLoadcases) {

		// no loadcase selected
		if (selectedLoadcases.isEmpty()) {
			String message = "No loadcase selected. Please select at least 1 loadcase to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(incTable_);
			return false;
		}

		// no contribution name given
		String name = contributionName_.getText();
		if (name == null || name.trim().isEmpty()) {
			String message = "No name for contribution given. Please enter a name for loadcase contribution to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(contributionName_);
			return false;
		}

		// check if contribution name already exists
		ObservableList<DamageContribution> contributions = panel_.getDamageContributions();
		for (DamageContribution c : contributions) {
			if (c.getName().equals(name)) {
				String message = "A contribution with the same name already defined. Please enter a unique name to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(contributionName_);
				return false;
			}
		}

		// check if any of the selected loadcases already exist in the defined contributions
		for (LoadcaseItem loadcase : selectedLoadcases) {

			// get loadcase specs
			String eventName1 = loadcase.getEventName();
			String comments1 = loadcase.getComments();
			String loadcaseNumber1 = loadcase.getLoadcaseNumber();

			// loop over damage contributions
			for (DamageContribution c : contributions) {

				// not increment
				if (!c.getType().equals(ContributionType.INCREMENT)) {
					continue;
				}

				// loop over loadcase factors of contribution
				for (LoadcaseFactor loadcaseFactor : c.getLoadcaseFactors()) {

					// get loadcase specs
					String eventName2 = loadcaseFactor.getEventName();
					String comments2 = loadcaseFactor.getComments();
					String loadcaseNumber2 = loadcaseFactor.getLoadcaseNumber();

					// loadcase already exists in contribution
					if (eventName1.equals(eventName2) && comments1.equals(comments2) && loadcaseNumber1.equals(loadcaseNumber2)) {
						String message = "Selected loadcase '" + eventName1 + "' is already contained in contribution '" + c.getName() + "'. Please select unique loadceses to proceed.";
						PopOver popOver = new PopOver();
						popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
						popOver.setDetachable(false);
						popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
						popOver.setHideOnEscape(true);
						popOver.setAutoHide(true);
						popOver.show(ok_);
						return false;
					}
				}
			}
		}

		// valid inputs
		return true;
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	/**
	 * Searches material list for given new value.
	 *
	 * @param old_Val
	 *            Old text value.
	 * @param new_Val
	 *            New text value.
	 * @param isSteady
	 *            True if steady cases are selected.
	 */
	private void search(String old_Val, String new_Val, boolean isSteady) {

		// steady cases
		if (isSteady) {
			steadyList_.getSelectionModel().clearSelection();
			if (old_Val != null && new_Val.length() < old_Val.length()) {
				steadyList_.setItems(steadyCases_);
			}
			String value = new_Val.toUpperCase();
			ObservableList<DamageContribution> subentries = FXCollections.observableArrayList();
			for (DamageContribution item : steadyList_.getItems()) {
				if (item.toString().toUpperCase().contains(value)) {
					subentries.add(item);
				}
			}
			steadyList_.setItems(subentries);
			cancelSearch_.setVisible(!new_Val.isEmpty());
			if (new_Val.isEmpty()) {
				onCancelSearchClicked();
				return;
			}
		}

		// increment cases
		else {
			incTable_.getSelectionModel().clearSelection();
			if (old_Val != null && new_Val.length() < old_Val.length()) {
				incTable_.setItems(incLoadcases_);
			}
			String value = new_Val.toUpperCase();
			ObservableList<LoadcaseItem> subentries = FXCollections.observableArrayList();
			for (LoadcaseItem item : incTable_.getItems()) {
				if (item.getSearchString().toUpperCase().contains(value)) {
					subentries.add(item);
				}
			}
			incTable_.setItems(subentries);
			cancelSearch_.setVisible(!new_Val.isEmpty());
			if (new_Val.isEmpty()) {
				onCancelSearchClicked();
				return;
			}
		}
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static DamageContributionsPopup load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DamageContributionsPopup.fxml"));
			fxmlLoader.load();

			// get controller
			DamageContributionsPopup controller = (DamageContributionsPopup) fxmlLoader.getController();

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

	/**
	 * Interface for damage contribution adding panels.
	 *
	 * @author Murat Artim
	 * @date Dec 12, 2015
	 * @time 2:56:37 PM
	 */
	public interface DamageContributionAddingPanel {

		/**
		 * Adds damage contribution.
		 *
		 * @param damageContribution
		 *            Damage contribution.
		 */
		void addDamageContribution(DamageContribution damageContribution);

		/**
		 * Returns damage contributions.
		 *
		 * @return Damage contributions.
		 */
		ObservableList<DamageContribution> getDamageContributions();

		/**
		 * Returns damage contribution popup node.
		 *
		 * @return Damage contribution popup node.
		 */
		Node getDamageContributionPopupNode();
	}
}
