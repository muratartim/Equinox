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

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftModel;
import equinox.task.CreateElementGroupFromEIDs;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Class for create element group from EIDs panel controller.
 *
 * @author Murat Artim
 * @date Jul 30, 2015
 * @time 12:45:29 PM
 */
public class CreateElementGroupFromEIDPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextField name_;

	@FXML
	private TextArea eids_;

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
		onResetClicked();
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

	@FXML
	private void onOkClicked() {

		// get inputs
		String name = name_.getText();
		String eidString = eids_.getText();

		// check inputs
		int[] eids = checkInputs(name, eidString);
		if ((eids == null) || (eids.length == 0))
			return;

		// get selected model
		AircraftModel model = (AircraftModel) owner_.getSelectedFiles().get(0);

		// create group
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new CreateElementGroupFromEIDs(model, name, eids));

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
		eids_.clear();
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to create element group from element IDs", null);
	}

	/**
	 * Checks user inputs.
	 *
	 * @param name
	 *            Group name.
	 * @param eidString
	 *            Element IDs.
	 * @return List containing the element IDs, or null if EIDs cannot be extracted.
	 */
	private int[] checkInputs(String name, String eidString) {

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
	public static CreateElementGroupFromEIDPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CreateElementGroupFromEIDPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CreateElementGroupFromEIDPanel controller = (CreateElementGroupFromEIDPanel) fxmlLoader.getController();

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
