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

import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.fileType.AircraftModel;
import equinox.data.fileType.PilotPoints;
import equinox.data.fileType.SpectrumItem;
import equinox.task.CreateElementGroupFromEquivalentStress;
import equinox.task.CreateElementGroupFromLoadCase;
import equinox.task.CreateElementGroupFromPilotPoints;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Class for create element group from spectrum item panel controller.
 *
 * @author Murat Artim
 * @date Sep 7, 2015
 * @time 3:44:20 PM
 */
public class CreateElementGroupFromItemPanel implements Initializable {

	/** The owner panel. */
	private InputPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	@FXML
	private VBox root_;

	@FXML
	private TextField rename_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onOkClicked() {

		// get name
		String name = rename_.getText().trim();

		// no name
		if ((name == null) || name.isEmpty()) {
			String message = "Please enter a name to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(rename_);
			return;
		}

		// invalid name
		String warning = Utility.isValidFileName(name);
		if (warning != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(warning, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(rename_);
			return;
		}

		// get selected items
		SpectrumItem selected = (SpectrumItem) owner_.getSelectedFiles().get(0);

		// hide pop-over
		popOver_.hide();

		// load case
		if (selected instanceof AircraftLoadCase) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new CreateElementGroupFromLoadCase((AircraftLoadCase) selected, name));
		}
		else if (selected instanceof AircraftFatigueEquivalentStress) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new CreateElementGroupFromEquivalentStress((AircraftFatigueEquivalentStress) selected, name));
		}
		else if (selected instanceof AircraftModel) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new CreateElementGroupFromPilotPoints((AircraftModel) selected, name));
		}
		else if (selected instanceof PilotPoints) {
			AircraftModel model = ((PilotPoints) selected).getParentItem();
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new CreateElementGroupFromPilotPoints(model, name));
		}
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param popOver
	 *            The owner pop-over.
	 * @param owner
	 *            The owner panel.
	 * @param name
	 *            Name of item.
	 * @return The newly loaded file CDF set panel.
	 */
	public static VBox load(PopOver popOver, InputPanel owner, String name) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CreateElementGroupFromItemPanel.fxml"));
			fxmlLoader.load();

			// get controller
			CreateElementGroupFromItemPanel controller = (CreateElementGroupFromItemPanel) fxmlLoader.getController();

			// set attributes
			controller.popOver_ = popOver;
			controller.owner_ = owner;
			controller.rename_.setText(name);
			controller.rename_.selectAll();

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
