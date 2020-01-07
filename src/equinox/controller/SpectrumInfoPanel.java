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
import equinox.data.fileType.Spectrum;
import equinox.plugin.FileType;
import equinox.task.ExportSpectrum;
import equinox.task.GetSpectrumEditInfo;
import equinox.task.GetSpectrumEditInfo.SpectrumInfoRequestingPanel;
import equinox.task.SaveSpectrumInfo;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for spectrum info panel controller.
 *
 * @author Murat Artim
 * @date Feb 3, 2016
 * @time 11:48:29 AM
 */
public class SpectrumInfoPanel implements InternalInputSubPanel, SpectrumInfoRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Panel mode. */
	private boolean isExport_ = false;

	@FXML
	private VBox root_;

	@FXML
	private TextField program_, section_, mission_, missionIssue_, flpIssue_, iflpIssue_, cdfIssue_, deliveryRef_, description_;

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
		return isExport_ ? "Export Spectrum" : "Edit Spectrum Info";
	}

	@Override
	public void showing() {

		// set prompt texts
		missionIssue_.setPromptText(isExport_ ? "Required" : "Optional");
		flpIssue_.setPromptText(isExport_ ? "Required" : "Optional");
		iflpIssue_.setPromptText(isExport_ ? "Required" : "Optional");
		cdfIssue_.setPromptText(isExport_ ? "Required" : "Optional");
		deliveryRef_.setPromptText(isExport_ ? "DRAFT" : "Optional");

		// reset panel
		onResetClicked();
	}

	@Override
	public void setSpectrumInfo(String[] info) {
		program_.setText(info[GetSpectrumEditInfo.PROGRAM]);
		section_.setText(info[GetSpectrumEditInfo.SECTION]);
		mission_.setText(info[GetSpectrumEditInfo.MISSION]);
		missionIssue_.setText(info[GetSpectrumEditInfo.MISSION_ISSUE]);
		flpIssue_.setText(info[GetSpectrumEditInfo.FLP_ISSUE]);
		iflpIssue_.setText(info[GetSpectrumEditInfo.IFLP_ISSUE]);
		cdfIssue_.setText(info[GetSpectrumEditInfo.CDF_ISSUE]);
		deliveryRef_.setText(info[GetSpectrumEditInfo.DELIVERY_REF]);
		description_.setText(info[GetSpectrumEditInfo.DESCRIPTION]);
	}

	/**
	 * Sets panel mode.
	 *
	 * @param isExport
	 *            True if the panel is in export spectrum mode.
	 */
	public void setPanelMode(boolean isExport) {
		isExport_ = isExport;
	}

	@FXML
	private void onResetClicked() {

		// get selected spectrum
		Spectrum spectrum = (Spectrum) owner_.getSelectedFiles().get(0);

		// get spectrum info
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetSpectrumEditInfo(spectrum, this));
	}

	@FXML
	private void onOkClicked() {

		// get selected spectrum
		Spectrum spectrum = (Spectrum) owner_.getSelectedFiles().get(0);

		// get inputs
		String[] info = new String[9];
		info[GetSpectrumEditInfo.PROGRAM] = program_.getText();
		info[GetSpectrumEditInfo.SECTION] = section_.getText();
		info[GetSpectrumEditInfo.MISSION] = mission_.getText();
		info[GetSpectrumEditInfo.MISSION_ISSUE] = missionIssue_.getText();
		info[GetSpectrumEditInfo.FLP_ISSUE] = flpIssue_.getText();
		info[GetSpectrumEditInfo.IFLP_ISSUE] = iflpIssue_.getText();
		info[GetSpectrumEditInfo.CDF_ISSUE] = cdfIssue_.getText();
		info[GetSpectrumEditInfo.DELIVERY_REF] = deliveryRef_.getText();
		info[GetSpectrumEditInfo.DESCRIPTION] = description_.getText();

		// check inputs
		if (!checkInputs(info))
			return;

		// export mode
		if (isExport_) {

			// get file chooser
			FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.ZIP.getExtensionFilter());

			// show save dialog
			fileChooser.setInitialFileName(spectrum.getName() + ".zip");
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.getOwner().setInitialDirectory(selectedFile);

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, FileType.ZIP);

			// export spectrum
			ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
			tm.runTaskInParallel(new ExportSpectrum(spectrum, info, output));
		}

		// edit info mode
		else {
			ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
			tm.runTaskInParallel(new SaveSpectrumInfo(spectrum, info));
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
		if ((info[GetSpectrumEditInfo.PROGRAM] == null) || info[GetSpectrumEditInfo.PROGRAM].trim().isEmpty()) {
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
		if ((info[GetSpectrumEditInfo.SECTION] == null) || info[GetSpectrumEditInfo.SECTION].trim().isEmpty()) {
			String message = "Please supply A/C section to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(section_);
			return false;
		}
		if ((info[GetSpectrumEditInfo.MISSION] == null) || info[GetSpectrumEditInfo.MISSION].trim().isEmpty()) {
			String message = "Please supply fatigue mission to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(mission_);
			return false;
		}
		if (isExport_) {
			if ((info[GetSpectrumEditInfo.MISSION_ISSUE] == null) || info[GetSpectrumEditInfo.MISSION_ISSUE].trim().isEmpty()) {
				String message = "Please supply fatigue mission issue to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(missionIssue_);
				return false;
			}
			if ((info[GetSpectrumEditInfo.FLP_ISSUE] == null) || info[GetSpectrumEditInfo.FLP_ISSUE].trim().isEmpty()) {
				String message = "Please supply FLP issue to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(flpIssue_);
				return false;
			}
			if ((info[GetSpectrumEditInfo.IFLP_ISSUE] == null) || info[GetSpectrumEditInfo.IFLP_ISSUE].trim().isEmpty()) {
				String message = "Please supply IFLP issue to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(iflpIssue_);
				return false;
			}
			if ((info[GetSpectrumEditInfo.CDF_ISSUE] == null) || info[GetSpectrumEditInfo.CDF_ISSUE].trim().isEmpty()) {
				String message = "Please supply CDF issue to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(cdfIssue_);
				return false;
			}
		}

		// check input lengths
		if ((info[GetSpectrumEditInfo.PROGRAM] != null) && (info[GetSpectrumEditInfo.PROGRAM].trim().length() > 100))
			return showInputLengthWarning(program_, 100);
		if ((info[GetSpectrumEditInfo.SECTION] != null) && (info[GetSpectrumEditInfo.SECTION].trim().length() > 100))
			return showInputLengthWarning(section_, 100);
		if ((info[GetSpectrumEditInfo.MISSION] != null) && (info[GetSpectrumEditInfo.MISSION].trim().length() > 50))
			return showInputLengthWarning(mission_, 50);
		if ((info[GetSpectrumEditInfo.MISSION_ISSUE] != null) && (info[GetSpectrumEditInfo.MISSION_ISSUE].trim().length() > 10))
			return showInputLengthWarning(missionIssue_, 10);
		if ((info[GetSpectrumEditInfo.FLP_ISSUE] != null) && (info[GetSpectrumEditInfo.FLP_ISSUE].trim().length() > 10))
			return showInputLengthWarning(flpIssue_, 10);
		if ((info[GetSpectrumEditInfo.IFLP_ISSUE] != null) && (info[GetSpectrumEditInfo.IFLP_ISSUE].trim().length() > 10))
			return showInputLengthWarning(iflpIssue_, 10);
		if ((info[GetSpectrumEditInfo.CDF_ISSUE] != null) && (info[GetSpectrumEditInfo.CDF_ISSUE].trim().length() > 10))
			return showInputLengthWarning(cdfIssue_, 10);
		if ((info[GetSpectrumEditInfo.DELIVERY_REF] != null) && (info[GetSpectrumEditInfo.DELIVERY_REF].trim().length() > 50))
			return showInputLengthWarning(deliveryRef_, 50);
		if ((info[GetSpectrumEditInfo.DESCRIPTION] != null) && (info[GetSpectrumEditInfo.DESCRIPTION].trim().length() > 200))
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
		owner_.getOwner().showHelp(isExport_ ? "How to export spectrum" : "How to edit spectrum info", null);
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static SpectrumInfoPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("SpectrumInfoPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			SpectrumInfoPanel controller = (SpectrumInfoPanel) fxmlLoader.getController();

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
