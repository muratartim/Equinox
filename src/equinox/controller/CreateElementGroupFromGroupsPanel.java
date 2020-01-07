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

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftModel;
import equinox.task.CreateElementGroupFromGroups;
import equinox.task.GetElementGroups;
import equinox.task.GetElementGroups.ElementGroupsRequestingPanel;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;

/**
 * Class for create element group from existing groups panel controller.
 *
 * @author Murat Artim
 * @date Aug 3, 2015
 * @time 12:44:45 PM
 */
public class CreateElementGroupFromGroupsPanel implements InternalInputSubPanel, ElementGroupsRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextField name_;

	@FXML
	private ComboBox<String> groupA_, groupB_;

	@FXML
	private ToggleButton union_, intersection_, complement_, difference_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public void showing() {

		// get selected A/C model
		AircraftModel selected = (AircraftModel) owner_.getSelectedFiles().get(0);

		// get element groups and positions
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetElementGroups(this, selected));
	}

	@Override
	public String getHeader() {
		return "Create Element Group";
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void setElementGroups(ArrayList<String> groups) {
		groupA_.getSelectionModel().clearSelection();
		groupB_.getSelectionModel().clearSelection();
		groupA_.getItems().setAll(groups);
		groupB_.getItems().setAll(groups);
		union_.setSelected(true);
		name_.clear();
	}

	@FXML
	private void onOkClicked() {

		// get inputs
		String name = name_.getText();
		String groupA = groupA_.getSelectionModel().getSelectedItem();
		String groupB = groupB_.getSelectionModel().getSelectedItem();
		int operation = -1;
		if (union_.isSelected()) {
			operation = CreateElementGroupFromGroups.UNION;
		}
		else if (intersection_.isSelected()) {
			operation = CreateElementGroupFromGroups.INTERSECTION;
		}
		else if (complement_.isSelected()) {
			operation = CreateElementGroupFromGroups.COMPLEMENT;
		}
		else if (difference_.isSelected()) {
			operation = CreateElementGroupFromGroups.DIFFERENCE;
		}

		// check inputs
		if (!checkInputs(name, groupA, groupB, operation))
			return;

		// get selected model
		AircraftModel model = (AircraftModel) owner_.getSelectedFiles().get(0);

		// create group
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new CreateElementGroupFromGroups(model, name, groupA, groupB, operation));

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {
		name_.clear();
		groupA_.getSelectionModel().clearSelection();
		groupB_.getSelectionModel().clearSelection();
		union_.setSelected(true);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to create element group from existing groups", null);
	}

	/**
	 * Checks inputs.
	 *
	 * @param name
	 *            Group name.
	 * @param groupA
	 *            Group A.
	 * @param groupB
	 *            Group B.
	 * @param operation
	 *            Operation.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(String name, String groupA, String groupB, int operation) {

		// invalid name
		if ((name == null) || name.trim().isEmpty()) {
			String message = "Please enter a group name to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(name_);
			return false;
		}

		// no groups A
		if (groupA == null) {
			String message = "Please select group A to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(groupA_);
			return false;
		}

		// no groups B
		if (groupB == null) {
			String message = "Please select group B to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(groupB_);
			return false;
		}

		// same groups
		if (groupA.equals(groupB)) {
			String message = "Please select different groups to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(groupB_);
			return false;
		}

		// no operation selected
		if (operation == -1) {
			String message = "Please select an operation to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(union_);
			return false;
		}

		// acceptable
		return true;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static CreateElementGroupFromGroupsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CreateElementGroupFromGroupsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CreateElementGroupFromGroupsPanel controller = (CreateElementGroupFromGroupsPanel) fxmlLoader.getController();

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
