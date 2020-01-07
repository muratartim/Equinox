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
import equinox.dataServer.remote.data.SpectrumInfo;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.font.IconicFont;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Class for download spectrum info edit panel controller.
 *
 * @author Murat Artim
 * @date Nov 5, 2015
 * @time 3:50:14 PM
 */
public class DownloadSpectrumInfoEditPanel implements Initializable {

	/** The owner panel. */
	private DownloadSpectrumPanel owner_;

	/** Popup window. */
	private PopOver popOver_;

	@FXML
	private VBox root_;

	@FXML
	private StackPane headerPane_;

	@FXML
	private Label name_;

	@FXML
	private TextField size_, pilotPoints_, multiplicationTables_, program_, section_, mission_, missionIssue_, flpIssue_, iflpIssue_, cdfIssue_, deliveryRef_, description_;

	@FXML
	private Button save_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onSaveClicked() {

		// create spectrum info
		SpectrumInfo info = new SpectrumInfo();
		info.setInfo(SpectrumInfoType.AC_PROGRAM, program_.getText());
		info.setInfo(SpectrumInfoType.AC_SECTION, section_.getText());
		info.setInfo(SpectrumInfoType.FAT_MISSION, mission_.getText());
		info.setInfo(SpectrumInfoType.FAT_MISSION_ISSUE, missionIssue_.getText());
		info.setInfo(SpectrumInfoType.FLP_ISSUE, flpIssue_.getText());
		info.setInfo(SpectrumInfoType.IFLP_ISSUE, iflpIssue_.getText());
		info.setInfo(SpectrumInfoType.CDF_ISSUE, cdfIssue_.getText());
		info.setInfo(SpectrumInfoType.DELIVERY_REF, deliveryRef_.getText());
		info.setInfo(SpectrumInfoType.DESCRIPTION, description_.getText());

		// check inputs
		if (!checkInputs(info))
			return;

		// save update
		owner_.updateInfo(info);

		// hide
		popOver_.hide();
	}

	@FXML
	private void onCloseClicked() {
		popOver_.hide();
	}

	/**
	 * Checks inputs.
	 *
	 * @param info
	 *            Info array.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(SpectrumInfo info) {

		// check required fields
		String program = (String) info.getInfo(SpectrumInfoType.AC_PROGRAM);
		if (program == null || program.trim().isEmpty()) {
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
		String section = (String) info.getInfo(SpectrumInfoType.AC_SECTION);
		if (section == null || section.trim().isEmpty()) {
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
		String mission = (String) info.getInfo(SpectrumInfoType.FAT_MISSION);
		if (mission == null || mission.trim().isEmpty()) {
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
		String missionIssue = (String) info.getInfo(SpectrumInfoType.FAT_MISSION_ISSUE);
		if (missionIssue == null || missionIssue.trim().isEmpty()) {
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
		String flpIssue = (String) info.getInfo(SpectrumInfoType.FLP_ISSUE);
		if (flpIssue == null || flpIssue.trim().isEmpty()) {
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
		String iflpIssue = (String) info.getInfo(SpectrumInfoType.IFLP_ISSUE);
		if (iflpIssue == null || iflpIssue.trim().isEmpty()) {
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
		String cdfIssue = (String) info.getInfo(SpectrumInfoType.CDF_ISSUE);
		if (cdfIssue == null || cdfIssue.trim().isEmpty()) {
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
		String delRef = (String) info.getInfo(SpectrumInfoType.DELIVERY_REF);
		if (delRef == null || delRef.trim().isEmpty()) {
			String message = "Please supply delivery reference to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(deliveryRef_);
			return false;
		}
		String description = (String) info.getInfo(SpectrumInfoType.DESCRIPTION);
		if (description == null || description.trim().isEmpty()) {
			String message = "Please supply description to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(description_);
			return false;
		}

		// check input lengths
		if (program.trim().length() > 100)
			return showInputLengthWarning(program_, 100);
		if (section.trim().length() > 100)
			return showInputLengthWarning(section_, 100);
		if (mission.trim().length() > 50)
			return showInputLengthWarning(mission_, 50);
		if (missionIssue.trim().length() > 10)
			return showInputLengthWarning(missionIssue_, 10);
		if (flpIssue.trim().length() > 10)
			return showInputLengthWarning(flpIssue_, 10);
		if (iflpIssue.trim().length() > 10)
			return showInputLengthWarning(iflpIssue_, 10);
		if (cdfIssue.trim().length() > 10)
			return showInputLengthWarning(cdfIssue_, 10);
		if (delRef.trim().length() > 50)
			return showInputLengthWarning(deliveryRef_, 50);
		if (description.trim().length() > 200)
			return showInputLengthWarning(description_, 200);

		// valid inputs
		return true;
	}

	/**
	 * Checks for changes in spectrum info.
	 *
	 * @return True if there is change.
	 */
	private boolean checkForInfoChange() {

		// get info
		SpectrumInfo info = (SpectrumInfo) owner_.getInfo();

		// compare
		if (!info.getInfo(SpectrumInfoType.AC_PROGRAM).equals(program_.getText()))
			return true;
		if (!info.getInfo(SpectrumInfoType.AC_SECTION).equals(section_.getText()))
			return true;
		if (!info.getInfo(SpectrumInfoType.FAT_MISSION).equals(mission_.getText()))
			return true;
		if (!info.getInfo(SpectrumInfoType.FAT_MISSION_ISSUE).equals(missionIssue_.getText()))
			return true;
		if (!info.getInfo(SpectrumInfoType.FLP_ISSUE).equals(flpIssue_.getText()))
			return true;
		if (!info.getInfo(SpectrumInfoType.IFLP_ISSUE).equals(iflpIssue_.getText()))
			return true;
		if (!info.getInfo(SpectrumInfoType.CDF_ISSUE).equals(cdfIssue_.getText()))
			return true;
		if (!info.getInfo(SpectrumInfoType.DELIVERY_REF).equals(deliveryRef_.getText()))
			return true;
		if (!info.getInfo(SpectrumInfoType.DESCRIPTION).equals(description_.getText()))
			return true;

		// no change
		return false;
	}

	/**
	 * Sets up spectrum info listeners.
	 */
	private void setupInfoListeners() {
		SpectrumInfoListener listener = new SpectrumInfoListener();
		program_.textProperty().addListener(listener);
		section_.textProperty().addListener(listener);
		mission_.textProperty().addListener(listener);
		missionIssue_.textProperty().addListener(listener);
		flpIssue_.textProperty().addListener(listener);
		iflpIssue_.textProperty().addListener(listener);
		cdfIssue_.textProperty().addListener(listener);
		deliveryRef_.textProperty().addListener(listener);
		description_.textProperty().addListener(listener);
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

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param info
	 *            Spectrum info.
	 * @param owner
	 *            The owner panel.
	 * @param popOver
	 *            Popup window.
	 * @return The newly loaded file CDF set panel.
	 */
	public static VBox load(SpectrumInfo info, DownloadSpectrumPanel owner, PopOver popOver) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadSpectrumInfoEditPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			DownloadSpectrumInfoEditPanel controller = (DownloadSpectrumInfoEditPanel) fxmlLoader.getController();

			// set attributes
			controller.name_.setText((String) info.getInfo(SpectrumInfoType.NAME));
			controller.size_.setText(Utility.readableFileSize((long) info.getInfo(SpectrumInfoType.DATA_SIZE)));
			controller.program_.setText((String) info.getInfo(SpectrumInfoType.AC_PROGRAM));
			controller.section_.setText((String) info.getInfo(SpectrumInfoType.AC_SECTION));
			controller.mission_.setText((String) info.getInfo(SpectrumInfoType.FAT_MISSION));
			controller.missionIssue_.setText((String) info.getInfo(SpectrumInfoType.FAT_MISSION_ISSUE));
			controller.flpIssue_.setText((String) info.getInfo(SpectrumInfoType.FLP_ISSUE));
			controller.iflpIssue_.setText((String) info.getInfo(SpectrumInfoType.IFLP_ISSUE));
			controller.cdfIssue_.setText((String) info.getInfo(SpectrumInfoType.CDF_ISSUE));
			controller.deliveryRef_.setText((String) info.getInfo(SpectrumInfoType.DELIVERY_REF));
			String description = (String) info.getInfo(SpectrumInfoType.DESCRIPTION);
			controller.description_.setText(description == null || description.trim().isEmpty() ? "-" : description);
			controller.pilotPoints_.setText(Integer.toString((int) info.getInfo(SpectrumInfoType.PILOT_POINTS)));
			controller.multiplicationTables_.setText(Integer.toString((int) info.getInfo(SpectrumInfoType.MULT_TABLES)));

			// set owner panel
			controller.owner_ = owner;
			controller.popOver_ = popOver;

			// listen for changes in info
			controller.setupInfoListeners();

			// listen for detach events
			controller.popOver_.detachedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
				if (newValue) {
					controller.headerPane_.getChildren().remove(controller.save_);
					controller.root_.getChildren().remove(controller.headerPane_);
					StackPane.setAlignment(controller.save_, Pos.TOP_RIGHT);
					StackPane.setMargin(controller.save_, new Insets(4.0, 10.0, 0.0, 0.0));
					controller.popOver_.getRoot().getChildren().add(controller.save_);
				}
			});

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Inner class for spectrum info change listener.
	 *
	 * @author Murat Artim
	 * @date Jun 28, 2016
	 * @time 8:33:38 PM
	 */
	public class SpectrumInfoListener implements ChangeListener<String> {

		@Override
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
			save_.setVisible(checkForInfoChange());
		}
	}
}
