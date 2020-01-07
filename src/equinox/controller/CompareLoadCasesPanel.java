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

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.ElementStress;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.input.CompareLoadCasesInput;
import equinox.task.CompareLoadCases;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

/**
 * Class for compare load cases panel controller.
 *
 * @author Murat Artim
 * @date Sep 10, 2015
 * @time 10:44:01 AM
 */
public class CompareLoadCasesPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextArea eids_;

	@FXML
	private ComboBox<ElementStress> stressComponent_;

	@FXML
	private ToggleSwitch dataLabels_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup element types and stress components
		stressComponent_.setItems(FXCollections.observableArrayList(ElementStress.values()));

		// set listeners
		dataLabels_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				StatisticsViewPanel panel = (StatisticsViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);
				panel.setLabelsVisible(newValue);
			}
		});
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
	public String getHeader() {
		return "Compare Load Cases";
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {
		// no implementation
	}

	@FXML
	private void onOkClicked() {

		// get inputs
		String eidString = eids_.getText();
		ElementStress stressComponent = stressComponent_.getSelectionModel().getSelectedItem();

		// check inputs
		int[] eids = checkInputs(eidString, stressComponent);
		if ((eids == null) || (eids.length == 0))
			return;

		// get selected load cases
		ArrayList<AircraftLoadCase> loadCases = new ArrayList<>();
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();
		for (TreeItem<String> item : selected) {
			loadCases.add((AircraftLoadCase) item);
		}

		// no load case selected
		if (loadCases.isEmpty())
			return;

		// create and submit task
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new CompareLoadCases(new CompareLoadCasesInput(loadCases, eids, stressComponent, dataLabels_.isSelected())));
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {
		eids_.clear();
		stressComponent_.getSelectionModel().clearSelection();
		if (!dataLabels_.isSelected()) {
			dataLabels_.setSelected(true);
		}
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to compare load cases", null);
	}

	/**
	 * Checks user inputs.
	 *
	 * @param eidString
	 *            Element IDs.
	 * @param stressComponent
	 *            Selected stress component.
	 * @return List containing the element IDs, or null if EIDs cannot be extracted.
	 */
	private int[] checkInputs(String eidString, ElementStress stressComponent) {

		// no stress component selected
		if (stressComponent == null) {
			String message = "Please select a stress component to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(stressComponent_);
			return null;
		}

		// invalid eids
		if ((eidString == null) || eidString.trim().isEmpty()) {
			String message = "Please enter element IDs to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(eids_);
			return null;
		}

		// extract element IDs
		try {
			String[] eidStrings = eidString.split(",");
			int[] eids = new int[eidStrings.length];
			for (int i = 0; i < eidStrings.length; i++) {
				eids[i] = Integer.parseInt(eidStrings[i].trim());
			}
			return eids;
		}

		// invalid EIDs
		catch (Exception e) {
			String message = "Please enter valid element IDs to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(eids_);
			return null;
		}
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static CompareLoadCasesPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CompareLoadCasesPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CompareLoadCasesPanel controller = (CompareLoadCasesPanel) fxmlLoader.getController();

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
