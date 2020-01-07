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

import org.controlsfx.control.ToggleSwitch;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.Histogram3DInput;
import equinox.data.input.HistogramInput.HistogramDataType;
import equinox.task.Plot3DHistogram;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;

/**
 * Class for histogram 3D panel controller.
 *
 * @author Murat Artim
 * @date Jul 1, 2015
 * @time 12:22:52 PM
 */
public class Histogram3DPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ComboBox<HistogramDataType> dataTypeX_, dataTypeY_;

	@FXML
	private ToggleSwitch xLabels_, yLabels_, zLabels_;

	@FXML
	private Slider resolution_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set data types
		dataTypeX_.getItems().clear();
		dataTypeX_.setButtonCell(new HistogramDataTypeListCell());
		dataTypeX_.setCellFactory(p -> new HistogramDataTypeListCell());
		dataTypeX_.setItems(FXCollections.observableArrayList(HistogramDataType.values()));
		dataTypeX_.getSelectionModel().select(HistogramDataType.MEAN_STRESS);
		dataTypeY_.getItems().clear();
		dataTypeY_.setButtonCell(new HistogramDataTypeListCell());
		dataTypeY_.setCellFactory(p -> new HistogramDataTypeListCell());
		dataTypeY_.setItems(FXCollections.observableArrayList(HistogramDataType.values()));
		dataTypeY_.getSelectionModel().select(HistogramDataType.STRESS_AMPLITUDE);

		// add listeners
		dataTypeX_.getSelectionModel().selectedItemProperty().addListener((ChangeListener<HistogramDataType>) (observable, oldValue, newValue) -> {

			// null
			if (newValue == null)
				return;

			// setup components
			onDataTypeSelected();
		});
		dataTypeY_.getSelectionModel().selectedItemProperty().addListener((ChangeListener<HistogramDataType>) (observable, oldValue, newValue) -> {

			// null
			if (newValue == null)
				return;

			// setup components
			onDataTypeSelected();
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
		return "Plot 3D Histogram";
	}

	@FXML
	private void onResetClicked() {
		dataTypeX_.getSelectionModel().select(HistogramDataType.MEAN_STRESS);
		dataTypeY_.getSelectionModel().select(HistogramDataType.STRESS_AMPLITUDE);
		xLabels_.setSelected(false);
		yLabels_.setSelected(false);
		zLabels_.setSelected(false);
		resolution_.setValue(60.0);
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
		owner_.getOwner().showHelp("How to plot 3D rainflow histogram", null);
	}

	/**
	 * Called when data type is selected.
	 *
	 */
	private void onDataTypeSelected() {
		HistogramDataType x = dataTypeX_.getSelectionModel().getSelectedItem();
		HistogramDataType y = dataTypeY_.getSelectionModel().getSelectedItem();
		if (x.equals(y)) {
			for (HistogramDataType type : HistogramDataType.values()) {
				if (!type.equals(y)) {
					dataTypeX_.getSelectionModel().select(type);
					break;
				}
			}
		}
	}

	/**
	 * Plots histogram
	 */
	private void plotHistogram() {

		// get selected equivalent stresses
		SpectrumItem eqStress = (SpectrumItem) owner_.getSelectedFiles().get(0);

		// create input
		Histogram3DInput input = new Histogram3DInput(eqStress);

		// set inputs
		input.setDataType(dataTypeX_.getSelectionModel().getSelectedItem(), dataTypeY_.getSelectionModel().getSelectedItem());
		input.setLabelDisplay(xLabels_.isSelected(), yLabels_.isSelected(), zLabels_.isSelected());
		input.setResolution((int) resolution_.getValue());

		// create and start statistics task
		owner_.getOwner().getActiveTasksPanel().runTaskSequentially(new Plot3DHistogram(input));
	}

	/**
	 * Loads and returns spectrum statistics panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded spectrum statistics panel.
	 */
	public static Histogram3DPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("Histogram3DPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			Histogram3DPanel controller = (Histogram3DPanel) fxmlLoader.getController();

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
