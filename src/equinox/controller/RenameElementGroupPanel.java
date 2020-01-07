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
import equinox.task.GetElementGroups;
import equinox.task.GetElementGroups.ElementGroupsRequestingPanel;
import equinox.task.RenameElementGroup;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Class for rename element group panel controller.
 *
 * @author Murat Artim
 * @date Jul 27, 2015
 * @time 12:24:22 PM
 */
public class RenameElementGroupPanel implements InternalInputSubPanel, ElementGroupsRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ListView<String> groups_;

	@FXML
	private TextField name_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// add change listener to list selections
		groups_.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (newValue == null) {
					name_.clear();
					name_.setDisable(true);
					return;
				}
				name_.setDisable(false);
				name_.setText(newValue);
				name_.selectAll();
			}
		});
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
		return "Rename Element Groups";
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
		groups_.getItems().setAll(groups);
		name_.clear();
		name_.setDisable(true);
	}

	@FXML
	private void onNameEntered() {
		onOkClicked();
	}

	@FXML
	private void onOkClicked() {

		// no group selected
		if (groups_.getSelectionModel().isEmpty()) {
			String message = "Please select a group to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(groups_);
			return;
		}

		// get name
		String name = name_.getText().trim();

		// invalid name
		if ((name == null) || name.isEmpty()) {
			String message = "Please enter a group name to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(name_);
			return;
		}

		// get name of selected group
		String group = groups_.getSelectionModel().getSelectedItem();

		// no change
		if (name.equals(group)) {
			onResetClicked();
			return;
		}

		// name already exists
		for (String g : groups_.getItems()) {
			if (g.equals(name)) {
				String message = "Entered name is already assigned to a group. Please enter a unique group name to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 40, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(name_);
				return;
			}
		}

		// get selected model
		AircraftModel model = (AircraftModel) owner_.getSelectedFiles().get(0);

		// rename
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new RenameElementGroup(this, model, group, name));
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {
		groups_.getSelectionModel().clearSelection();
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to rename element groups", null);
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static RenameElementGroupPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("RenameElementGroupPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			RenameElementGroupPanel controller = (RenameElementGroupPanel) fxmlLoader.getController();

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
