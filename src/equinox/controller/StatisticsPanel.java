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
import org.controlsfx.control.ToggleSwitch;

import com.jfoenix.controls.JFXTabPane;

import control.validationField.IntegerValidationField;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.LoadcaseItem;
import equinox.data.fileType.Flight;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.StressSequence;
import equinox.data.input.StatisticsInput;
import equinox.data.input.StatisticsInput.Statistic;
import equinox.task.GenerateStatistics;
import equinox.task.GetLoadcases;
import equinox.task.GetLoadcases.LoadcaseRequestingPanel;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

/**
 * Class for spectrum statistics panel controller.
 *
 * @author Murat Artim
 * @date Apr 21, 2014
 * @time 10:36:26 PM
 */
public class StatisticsPanel implements InternalInputSubPanel, LoadcaseRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Event lists. */
	private final ObservableList<LoadcaseItem> onegLoadcases_ = FXCollections.observableArrayList(), incLoadcases_ = FXCollections.observableArrayList();

	@FXML
	private VBox root_;

	@FXML
	private ChoiceBox<Statistic> statistics_;

	@FXML
	private ChoiceBox<String> order_;

	@FXML
	private ToggleSwitch multiplyWithValidity_, dataLabels_;

	@FXML
	private TitledPane loadcasePane_;

	@FXML
	private ListView<LoadcaseItem> onegLoadcaseList_, incLoadcaseList_;

	@FXML
	private TextField onegSearch_, incSearch_;

	@FXML
	private IntegerValidationField limit_;

	@FXML
	private Button onegCancel_, incCancel_, ok_;

	@FXML
	private Label limitLabel_;

	@FXML
	private JFXTabPane loadcaseTab_;

	@FXML
	private Accordion accordion_;

	/** Panel mode. */
	private boolean isSingleFlight_ = true;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// add selection listener to flight list
		statistics_.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Statistic>() {

			@Override
			public void changed(ObservableValue<? extends Statistic> observable, Statistic oldValue, Statistic newValue) {

				// no selection
				if (newValue == null)
					return;

				// validity checkbox
				multiplyWithValidity_.setDisable(!newValue.equals(Statistic.LOADCASE_OCCURRENCE));
				if (multiplyWithValidity_.isDisabled()) {
					multiplyWithValidity_.setSelected(false);
				}

				// no event selection
				if (newValue.equals(Statistic.NUM_PEAKS) || newValue.equals(Statistic.FLIGHT_OCCURRENCE)) {
					loadcasePane_.setExpanded(false);
					loadcasePane_.setDisable(true);
					loadcasePane_.setText("Select Loadcase");
				}

				// event selection
				else {
					loadcasePane_.setExpanded(true);
					loadcasePane_.setDisable(false);
					if (!isSingleFlight_) {
						loadcasePane_.setText(newValue.equals(Statistic.LOADCASE_OCCURRENCE) ? "Select Loadcase" : "Select Loadcase (Optional)");
					}
					else {
						loadcasePane_.setText("Select Loadcases (Optional)");
					}
				}
			}
		});

		// set results order
		order_.setItems(FXCollections.observableArrayList("Descending", "Ascending"));
		order_.getSelectionModel().select(0);

		// add listener to end flight number field
		limit_.setDefaultValue(10);
		limit_.setMinimumValue(1, true);

		// setup event lists
		onegLoadcaseList_.setItems(onegLoadcases_);
		incLoadcaseList_.setItems(incLoadcases_);

		// setup search fields
		onegSearch_.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> ov, String old_Val, String new_val) {
				onegSearch(old_Val, new_val);
			}
		});
		incSearch_.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> ov, String old_Val, String new_val) {
				incSearch(old_Val, new_val);
			}
		});

		// set listeners
		dataLabels_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				StatisticsViewPanel panel = (StatisticsViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);
				panel.setLabelsVisible(newValue);
			}
		});

		// set hand cursor to cancel buttons
		Utility.setHandCursor(onegCancel_, incCancel_);

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
		// no implementation
	}

	@Override
	public void showing() {

		// get selected files
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();

		// get spectrum
		Spectrum spectrum = null;
		for (TreeItem<String> item : selected) {

			// stress sequence
			if (item instanceof StressSequence) {
				isSingleFlight_ = false;
				spectrum = ((StressSequence) item).getParentItem().getParentItem();
				break;
			}

			// flight
			else if (item instanceof Flight) {
				isSingleFlight_ = selected.size() == 1;
				spectrum = ((Flight) item).getParentItem().getParentItem().getParentItem().getParentItem();
				break;
			}
		}

		// single flight
		if (isSingleFlight_) {

			// set statistics list
			statistics_.getItems().clear();
			for (Statistic statistic : Statistic.values()) {
				if (statistic.equals(Statistic.NUM_PEAKS) || statistic.equals(Statistic.FLIGHT_OCCURRENCE)) {
					continue;
				}
				statistics_.getItems().add(statistic);
			}
			statistics_.getSelectionModel().select(0);

			// multiple loadcase selection
			onegLoadcaseList_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			incLoadcaseList_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

			// set limit label
			limitLabel_.setText("Max. loadcases:");
		}

		// multiple flights
		else {

			// set statistics list
			statistics_.getItems().clear();
			statistics_.setItems(FXCollections.observableArrayList(Statistic.values()));
			statistics_.getSelectionModel().select(0);

			// single loadcase selection
			onegLoadcaseList_.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
			incLoadcaseList_.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

			// set limit label
			limitLabel_.setText("Max. flights:");
		}

		// create task and start
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetLoadcases(this, spectrum, GetLoadcases.ALL));
	}

	@Override
	public String getHeader() {
		return "Plot Statistics";
	}

	@Override
	public void setLoadcases(ArrayList<LoadcaseItem> loadcases) {
		onegLoadcases_.clear();
		incLoadcases_.clear();
		for (LoadcaseItem loadcase : loadcases) {
			if (loadcase.isOneg()) {
				onegLoadcases_.add(loadcase);
			}
			else {
				incLoadcases_.add(loadcase);
			}
		}
		onegLoadcaseList_.setItems(onegLoadcases_);
		onegSearch_.clear();
		incLoadcaseList_.setItems(incLoadcases_);
		incSearch_.clear();
	}

	/**
	 * Searches 1g loadcase list for given new value.
	 *
	 * @param old_Val
	 *            Old text value.
	 * @param new_Val
	 *            New text value.
	 */
	private void onegSearch(String old_Val, String new_Val) {
		onegLoadcaseList_.getSelectionModel().clearSelection();
		if ((old_Val != null) && (new_Val.length() < old_Val.length())) {
			onegLoadcaseList_.setItems(onegLoadcases_);
		}
		String value = new_Val.toUpperCase();
		ObservableList<LoadcaseItem> subentries = FXCollections.observableArrayList();
		for (LoadcaseItem item : onegLoadcaseList_.getItems()) {
			if (item.toString().toUpperCase().contains(value)) {
				subentries.add(item);
			}
		}
		onegLoadcaseList_.setItems(subentries);
		onegCancel_.setVisible(!new_Val.isEmpty());
	}

	/**
	 * Searches incremental loadcase list for given new value.
	 *
	 * @param old_Val
	 *            Old text value.
	 * @param new_Val
	 *            New text value.
	 */
	private void incSearch(String old_Val, String new_Val) {
		incLoadcaseList_.getSelectionModel().clearSelection();
		if ((old_Val != null) && (new_Val.length() < old_Val.length())) {
			incLoadcaseList_.setItems(incLoadcases_);
		}
		String value = new_Val.toUpperCase();
		ObservableList<LoadcaseItem> subentries = FXCollections.observableArrayList();
		for (LoadcaseItem item : incLoadcaseList_.getItems()) {
			if (item.toString().toUpperCase().contains(value)) {
				subentries.add(item);
			}
		}
		incLoadcaseList_.setItems(subentries);
		incCancel_.setVisible(!new_Val.isEmpty());
	}

	@FXML
	private void onResetClicked() {
		statistics_.getSelectionModel().select(0);
		multiplyWithValidity_.setSelected(false);
		multiplyWithValidity_.setDisable(!isSingleFlight_);
		loadcasePane_.setExpanded(isSingleFlight_);
		loadcasePane_.setDisable(!isSingleFlight_);
		onegLoadcaseList_.getSelectionModel().clearSelection();
		incLoadcaseList_.getSelectionModel().clearSelection();
		loadcaseTab_.getSelectionModel().select(0);
		order_.getSelectionModel().select(0);
		limit_.reset();
		dataLabels_.setSelected(true);
		onegSearch_.clear();
		incSearch_.clear();
	}

	@FXML
	private void onOkClicked() {

		// create input
		StatisticsInput input = new StatisticsInput();

		// set inputs
		input.setStatistic(statistics_.getSelectionModel().getSelectedItem());
		input.setLabelDisplay(dataLabels_.isSelected());
		input.setOrder(order_.getSelectionModel().getSelectedItem().equals("Descending"));
		input.setValidityMultiplier(multiplyWithValidity_.isSelected());
		input.setLoadcaseType(loadcaseTab_.getSelectionModel().getSelectedIndex() == 0);

		// add 1g loadcases
		if (loadcaseTab_.getSelectionModel().getSelectedIndex() == 0) {
			for (LoadcaseItem event : onegLoadcaseList_.getSelectionModel().getSelectedItems()) {
				input.addLoadcase(event);
			}
		}

		// add incremental loadcases
		else {
			for (LoadcaseItem event : incLoadcaseList_.getSelectionModel().getSelectedItems()) {
				input.addLoadcase(event);
			}
		}

		// add flights
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();
		for (TreeItem<String> item : selected) {

			// flight
			if (item instanceof Flight) {
				input.addFlight((Flight) item);
			}
			else if (item instanceof StressSequence) {
				ArrayList<Flight> flights = ((StressSequence) item).getFlights().getFlights();
				for (Flight flight : flights) {
					input.addFlight(flight);
				}
			}
		}

		// check input
		if (!checkInput(input))
			return;

		// set max limit
		input.setLimit(Integer.parseInt(limit_.getText()));

		// create and start statistics task
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GenerateStatistics(input));
	}

	/**
	 * Checks inputs and shows warning if necessary.
	 *
	 * @param input
	 *            Input to check.
	 * @return True if the inputs are correct.
	 */
	private boolean checkInput(StatisticsInput input) {

		// check max limit
		String message = limit_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(limit_);
			return false;
		}

		// get statistic
		Statistic stat = input.getStatistic();

		// multiple flights
		if (input.getFlights().size() > 1) {

			// not loadcase occurrence
			if (!stat.equals(Statistic.LOADCASE_OCCURRENCE))
				return true;

			// loadcases selected
			if (!input.getLoadcases().isEmpty())
				return true;

			// show warning
			boolean isOneg = loadcaseTab_.getSelectionModel().getSelectedIndex() == 0;
			message = "Please select a loadcase to show statistics.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(isOneg ? onegLoadcaseList_ : incLoadcaseList_);
			return false;
		}

		// return
		return true;
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onCancelSearchClicked() {
		if (loadcaseTab_.getSelectionModel().getSelectedIndex() == 0) {
			onegSearch_.clear();
		}
		else {
			incSearch_.clear();
		}
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to plot statistics", null);
	}

	/**
	 * Loads and returns spectrum statistics panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded spectrum statistics panel.
	 */
	public static StatisticsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("StatisticsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			StatisticsPanel controller = (StatisticsPanel) fxmlLoader.getController();

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
