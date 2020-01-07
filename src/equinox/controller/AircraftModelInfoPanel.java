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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftModel;
import equinox.plugin.FileType;
import equinox.task.ExportAircraftModel;
import equinox.task.GetAircraftModelEditInfo;
import equinox.task.GetAircraftModelEditInfo.AircraftModelInfoRequestingPanel;
import equinox.task.SaveAircraftModelInfo;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for A/C model info panel controller.
 *
 * @author Murat Artim
 * @date 15 Aug 2016
 * @time 14:37:36
 */
public class AircraftModelInfoPanel implements InternalInputSubPanel, AircraftModelInfoRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Panel mode. */
	private boolean isExport_ = false;

	@FXML
	private VBox root_;

	@FXML
	private TextField program_, modelName_, deliveryRef_, description_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
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
	public String getHeader() {
		return isExport_ ? "Export A/C Model" : "Edit A/C Model Info";
	}

	@Override
	public void showing() {

		// set prompt texts
		deliveryRef_.setPromptText(isExport_ ? "DRAFT" : "Optional");

		// reset panel
		onResetClicked();
	}

	/**
	 * Sets panel mode.
	 *
	 * @param isExport
	 *            True if the panel is in export A/C model mode.
	 */
	public void setPanelMode(boolean isExport) {
		isExport_ = isExport;
	}

	@Override
	public void setAircraftModelInfo(String[] info) {
		program_.setText(info[GetAircraftModelEditInfo.PROGRAM]);
		modelName_.setText(info[GetAircraftModelEditInfo.MODEL_NAME]);
		deliveryRef_.setText(info[GetAircraftModelEditInfo.DELIVERY_REF]);
		description_.setText(info[GetAircraftModelEditInfo.DESCRIPTION]);
	}

	@FXML
	private void onResetClicked() {

		// get selected A/C model
		AircraftModel model = (AircraftModel) owner_.getSelectedFiles().get(0);

		// get A/C model info
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetAircraftModelEditInfo(model, this));
	}

	@FXML
	private void onOkClicked() {

		// get selected A/C model
		AircraftModel model = (AircraftModel) owner_.getSelectedFiles().get(0);

		// get inputs
		String[] info = new String[4];
		info[GetAircraftModelEditInfo.PROGRAM] = program_.getText();
		info[GetAircraftModelEditInfo.MODEL_NAME] = modelName_.getText();
		info[GetAircraftModelEditInfo.DELIVERY_REF] = deliveryRef_.getText();
		info[GetAircraftModelEditInfo.DESCRIPTION] = description_.getText();

		// check inputs
		if (!checkInputs(info))
			return;

		// export mode
		if (isExport_) {

			// get file chooser
			FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.ZIP.getExtensionFilter());

			// show save dialog
			fileChooser.setInitialFileName(model.getProgram() + "_" + model.getModelName() + ".zip");
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.getOwner().setInitialDirectory(selectedFile);

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, FileType.ZIP);

			// export A/C model
			ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
			tm.runTaskInParallel(new ExportAircraftModel(model, info, output));
		}

		// edit info mode
		else {
			ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
			tm.runTaskInParallel(new SaveAircraftModelInfo(model, info));
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Checks inputs.
	 *
	 * @param info
	 *            Info array.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(String[] info) {

		// check required fields
		if ((info[GetAircraftModelEditInfo.PROGRAM] == null) || info[GetAircraftModelEditInfo.PROGRAM].trim().isEmpty()) {
			String message = "Please supply A/C program to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(program_);
			return false;
		}
		if ((info[GetAircraftModelEditInfo.MODEL_NAME] == null) || info[GetAircraftModelEditInfo.MODEL_NAME].trim().isEmpty()) {
			String message = "Please supply A/C model name to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(modelName_);
			return false;
		}

		// check input lengths
		if ((info[GetAircraftModelEditInfo.PROGRAM] != null) && (info[GetAircraftModelEditInfo.PROGRAM].trim().length() > 100))
			return showInputLengthWarning(program_, 100);
		if ((info[GetAircraftModelEditInfo.MODEL_NAME] != null) && (info[GetAircraftModelEditInfo.MODEL_NAME].trim().length() > 100))
			return showInputLengthWarning(modelName_, 100);
		if ((info[GetAircraftModelEditInfo.DELIVERY_REF] != null) && (info[GetAircraftModelEditInfo.DELIVERY_REF].trim().length() > 50))
			return showInputLengthWarning(deliveryRef_, 50);
		if ((info[GetAircraftModelEditInfo.DESCRIPTION] != null) && (info[GetAircraftModelEditInfo.DESCRIPTION].trim().length() > 200))
			return showInputLengthWarning(description_, 200);

		// valid inputs
		return true;
	}

	/**
	 * Shows input length warning message.
	 *
	 * @param node
	 *            Node to display the message.
	 * @param maxLength
	 *            Maximum length.
	 * @return False.
	 */
	private static boolean showInputLengthWarning(Node node, int maxLength) {
		String message = "Character limit exceeded. Please use maximum " + maxLength + " caharacters.";
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(node);
		return false;
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp(isExport_ ? "How to export A/C model" : "How to edit A/C model info", null);
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static AircraftModelInfoPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("AircraftModelInfoPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			AircraftModelInfoPanel controller = (AircraftModelInfoPanel) fxmlLoader.getController();

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
