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
import equinox.data.EquinoxTheme;
import equinox.data.fileType.Rfort;
import equinox.task.GetRfortOmissions;
import equinox.task.GetRfortOmissions.RfortOmissionsRequestingPanel;
import equinox.task.GetRfortPilotPoints;
import equinox.task.GetRfortPilotPoints.RfortPilotPointsRequestingPanel;
import equinox.task.PlotRfortResults;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Class for RFORT results panel controller.
 *
 * @author Murat Artim
 * @date Apr 19, 2016
 * @time 2:48:38 PM
 */
public class RfortResultsPanel implements InternalInputSubPanel, RfortPilotPointsRequestingPanel, RfortOmissionsRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Stress type. */
	private String stressType_;

	@FXML
	private VBox root_, pilotPoints_, omissions_;

	@FXML
	private Accordion accordion_;

	@FXML
	private ToggleSwitch absoluteDeviations_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set listeners
		absoluteDeviations_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onOkClicked();
			}
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

		// get selected item
		Rfort rfort = (Rfort) owner_.getSelectedFiles().get(0);

		// get pilot points and omissions
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetRfortPilotPoints(rfort, this, false));
		tm.runTaskInParallel(new GetRfortOmissions(rfort, this));

		// get absolute deviations option
		boolean plotAbsoluteDeviations = absoluteDeviations_.isSelected();

		// plot
		tm.runTaskInParallel(new PlotRfortResults(rfort, stressType_, null, null, plotAbsoluteDeviations));
	}

	@Override
	public String getHeader() {
		return "RFORT Results";
	}

	@Override
	public void setPilotPoints(ArrayList<String> ppNames) {

		// reset pilot points
		pilotPoints_.getChildren().clear();
		for (String ppName : ppNames) {

			// create horizontal box
			HBox hBox = new HBox();
			hBox.setAlignment(Pos.CENTER_LEFT);
			hBox.setSpacing(5.0);
			hBox.setMaxWidth(Double.MAX_VALUE);

			// create toggle switch
			ToggleSwitch tSwitch = new ToggleSwitch();
			HBox.setHgrow(tSwitch, Priority.NEVER);
			tSwitch.setPrefWidth(35.0);
			tSwitch.setMinWidth(ToggleSwitch.USE_PREF_SIZE);
			tSwitch.setMaxWidth(ToggleSwitch.USE_PREF_SIZE);
			tSwitch.setUserData(ppName);
			tSwitch.setSelected(true);

			// create label
			Label label = new Label(ppName);
			HBox.setHgrow(label, Priority.ALWAYS);
			label.setMaxWidth(Double.MAX_VALUE);

			// add components to horizontal box
			hBox.getChildren().add(tSwitch);
			hBox.getChildren().add(label);

			// set listener to toggle switch
			tSwitch.selectedProperty().addListener(new ChangeListener<Boolean>() {

				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					onOkClicked();
				}
			});

			// add to container
			pilotPoints_.getChildren().add(hBox);
		}
	}

	@Override
	public void setOmissions(ArrayList<String> omissions) {

		// reset omissions
		omissions_.getChildren().clear();
		for (String omissionName : omissions) {

			// create horizontal box
			HBox hBox = new HBox();
			hBox.setAlignment(Pos.CENTER_LEFT);
			hBox.setSpacing(5.0);
			hBox.setMaxWidth(Double.MAX_VALUE);

			// create toggle switch
			ToggleSwitch tSwitch = new ToggleSwitch();
			HBox.setHgrow(tSwitch, Priority.NEVER);
			tSwitch.setPrefWidth(35.0);
			tSwitch.setMinWidth(ToggleSwitch.USE_PREF_SIZE);
			tSwitch.setMaxWidth(ToggleSwitch.USE_PREF_SIZE);
			tSwitch.setUserData(omissionName);
			tSwitch.setSelected(true);

			// create label
			Label label = new Label(omissionName);
			HBox.setHgrow(label, Priority.ALWAYS);
			label.setMaxWidth(Double.MAX_VALUE);

			// add components to horizontal box
			hBox.getChildren().add(tSwitch);
			hBox.getChildren().add(label);

			// set listener to toggle switch
			tSwitch.selectedProperty().addListener(new ChangeListener<Boolean>() {

				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					onOkClicked();
				}
			});

			// add to container
			omissions_.getChildren().add(hBox);
		}
	}

	/**
	 * Sets stress type to this panel.
	 *
	 * @param stressType
	 *            Stress type.
	 */
	public void setStressType(String stressType) {
		stressType_ = stressType;
	}

	@FXML
	private void onResetClicked() {

		// reset show/hide pilot points
		for (Node node : pilotPoints_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			ts.setSelected(true);
		}

		// reset show/hide omissions
		for (Node node : omissions_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			ts.setSelected(true);
		}

		// reset options
		absoluteDeviations_.setSelected(true);
	}

	@FXML
	private void onOkClicked() {

		// check inputs
		if (!checkInputs())
			return;

		// get visible pilot points
		ArrayList<String> visiblePPs = new ArrayList<>();
		for (Node node : pilotPoints_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			if (ts.isSelected()) {
				visiblePPs.add((String) ts.getUserData());
			}
		}

		// get visible omissions
		ArrayList<String> visibleOmissions = new ArrayList<>();
		for (Node node : omissions_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			if (ts.isSelected()) {
				visibleOmissions.add((String) ts.getUserData());
			}
		}

		// get absolute deviations option
		boolean plotAbsoluteDeviations = absoluteDeviations_.isSelected();

		// get selected item
		Rfort rfort = (Rfort) owner_.getSelectedFiles().get(0);

		// plot
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new PlotRfortResults(rfort, stressType_, visiblePPs, visibleOmissions, plotAbsoluteDeviations));
	}

	/**
	 * Checks inputs.
	 *
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs() {

		// no pilot points visible
		boolean allHidden = true;
		for (Node node : pilotPoints_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			if (ts.isSelected()) {
				allHidden = false;
				break;
			}
		}
		if (allHidden) {
			String message = "Please select at least 1 pilot point to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(pilotPoints_);
			return false;
		}

		// no pilot points visible
		allHidden = true;
		for (Node node : omissions_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			if (ts.isSelected()) {
				allHidden = false;
				break;
			}
		}
		if (allHidden) {
			String message = "Please select at least 1 omission to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(omissions_);
			return false;
		}

		// acceptable
		return true;
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to plot RFORT results", null);
	}

	/**
	 * Loads and returns spectrum statistics panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded spectrum statistics panel.
	 */
	public static RfortResultsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("RfortResultsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			RfortResultsPanel controller = (RfortResultsPanel) fxmlLoader.getController();

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
