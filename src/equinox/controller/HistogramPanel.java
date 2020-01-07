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
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import control.validationField.IntegerValidationField;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.HistogramInput;
import equinox.data.input.HistogramInput.HistogramDataType;
import equinox.task.PlotHistogram;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.VBox;

/**
 * Class for histogram panel.
 *
 * @author Murat Artim
 * @date Jul 4, 2014
 * @time 2:13:16 PM
 */
public class HistogramPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ComboBox<HistogramDataType> dataType_;

	@FXML
	private ChoiceBox<String> order_;

	@FXML
	private ToggleSwitch dataLabels_;

	@FXML
	private IntegerValidationField limit_;

	@FXML
	private Spinner<Integer> digits_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set results order
		order_.setItems(FXCollections.observableArrayList("Descending", "Ascending"));
		order_.getSelectionModel().select(0);

		// set digits
		digits_.setValueFactory(new IntegerSpinnerValueFactory(0, 5, 2));

		// add listener to end flight number field
		limit_.setDefaultValue(10);
		limit_.setMinimumValue(1, true);

		// set data types
		dataType_.getItems().clear();
		dataType_.setButtonCell(new HistogramDataTypeListCell());
		dataType_.setCellFactory(p -> new HistogramDataTypeListCell());
		dataType_.setItems(FXCollections.observableArrayList(HistogramDataType.values()));
		dataType_.getSelectionModel().select(0);

		// set listeners
		dataLabels_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
			StatisticsViewPanel panel = (StatisticsViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);
			panel.setLabelsVisible(newValue);
		});

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
		plotHistogram();
	}

	@Override
	public String getHeader() {
		return "Plot Histogram";
	}

	@FXML
	private void onResetClicked() {
		dataType_.getSelectionModel().select(0);
		order_.getSelectionModel().select(0);
		limit_.reset();
		if (!dataLabels_.isSelected()) {
			dataLabels_.setSelected(true);
		}
		digits_.getValueFactory().setValue(2);
	}

	@FXML
	private void onOkClicked() {
		plotHistogram();
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to plot rainflow histogram", null);
	}

	/**
	 * Plots histogram
	 *
	 */
	private void plotHistogram() {

		// check limit
		String message = limit_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(limit_);
			return;
		}

		// get selected equivalent stresses
		SpectrumItem eqStress = (SpectrumItem) owner_.getSelectedFiles().get(0);

		// create input
		HistogramInput input = new HistogramInput();

		// set inputs
		input.setDataType(dataType_.getSelectionModel().getSelectedItem());
		input.setLimit(Integer.parseInt(limit_.getText()));
		input.setLabelsVisible(dataLabels_.isSelected());
		input.setOrder(order_.getSelectionModel().getSelectedItem().equals("Descending"));
		input.setDigits(digits_.getValue());

		// create and start statistics task
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new PlotHistogram(input, eqStress));
	}

	/**
	 * Loads and returns spectrum statistics panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded spectrum statistics panel.
	 */
	public static HistogramPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("HistogramPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			HistogramPanel controller = (HistogramPanel) fxmlLoader.getController();

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
	 * Inner class for histogram data type list cell.
	 *
	 * @author Murat Artim
	 * @date 2 Sep 2018
	 * @time 16:38:10
	 */
	private class HistogramDataTypeListCell extends ListCell<HistogramDataType> {

		@Override
		protected void updateItem(HistogramDataType item, boolean empty) {
			super.updateItem(item, empty);
			if (!empty && item != null) {
				setText(item.getName());
			}
			else {
				setText(null);
			}
		}
	}
}
