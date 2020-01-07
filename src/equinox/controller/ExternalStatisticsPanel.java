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

import control.validationField.IntegerValidationField;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.ExternalFlight;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.input.ExternalStatisticsInput;
import equinox.data.input.ExternalStatisticsInput.ExternalStatistic;
import equinox.task.GenerateExternalStatistics;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

/**
 * Class for external statistics panel controller.
 *
 * @author Murat Artim
 * @date Mar 13, 2015
 * @time 4:34:00 PM
 */
public class ExternalStatisticsPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ChoiceBox<ExternalStatistic> statistics_;

	@FXML
	private ChoiceBox<String> order_;

	@FXML
	private ToggleSwitch dataLabels_;

	@FXML
	private IntegerValidationField limit_;

	@FXML
	private Button ok_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set statistics list
		statistics_.setItems(FXCollections.observableArrayList(ExternalStatistic.values()));
		statistics_.getSelectionModel().select(0);

		// set results order
		order_.setItems(FXCollections.observableArrayList("Descending", "Ascending"));
		order_.getSelectionModel().select(0);

		// setup flight number field
		limit_.setDefaultValue(10);
		limit_.setMinimumValue(1, true);

		// set listeners
		dataLabels_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
			StatisticsViewPanel panel = (StatisticsViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);
			panel.setLabelsVisible(newValue);
		});

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
		// no implementation
	}

	@Override
	public String getHeader() {
		return "Plot Statistics";
	}

	@FXML
	private void onResetClicked() {
		statistics_.getSelectionModel().select(0);
		order_.getSelectionModel().select(0);
		limit_.reset();
		dataLabels_.setSelected(true);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to plot statistics", null);
	}

	@FXML
	private void onOkClicked() {

		// check limit
		String message = limit_.validate();
		if (message != null) {
			accordion_.setExpandedPane(accordion_.getPanes().get(1));
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(limit_);
			return;
		}

		// create input
		ExternalStatisticsInput input = new ExternalStatisticsInput();

		// set inputs
		input.setStatistic(statistics_.getSelectionModel().getSelectedItem());
		input.setLabelDisplay(dataLabels_.isSelected());
		input.setLimit(Integer.parseInt(limit_.getText()));
		input.setOrder(order_.getSelectionModel().getSelectedItem().equals("Descending"));

		// create task
		GenerateExternalStatistics task = new GenerateExternalStatistics(input);

		// add flights
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();
		for (TreeItem<String> item : selected) {

			// flight
			if (item instanceof ExternalFlight) {
				task.addTypicalFlight((ExternalFlight) item);
			}
			else if (item instanceof ExternalStressSequence) {
				ArrayList<ExternalFlight> flights = ((ExternalStressSequence) item).getFlights().getFlights();
				for (ExternalFlight flight : flights) {
					task.addTypicalFlight(flight);
				}
			}
		}

		// create and start statistics task
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(task);
	}

	/**
	 * Loads and returns spectrum statistics panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded spectrum statistics panel.
	 */
	public static ExternalStatisticsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ExternalStatisticsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			ExternalStatisticsPanel controller = (ExternalStatisticsPanel) fxmlLoader.getController();

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
