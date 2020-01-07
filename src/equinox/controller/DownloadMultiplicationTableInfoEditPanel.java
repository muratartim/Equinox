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
import equinox.dataServer.remote.data.MultiplicationTableInfo;
import equinox.dataServer.remote.data.MultiplicationTableInfo.MultiplicationTableInfoType;
import equinox.font.IconicFont;
import equinox.task.UpdateMultiplicationTableInfoInGlobalDB;
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
 * Class for download multiplication table info edit panel controller.
 *
 * @author Murat Artim
 * @date Jul 1, 2016
 * @time 12:37:55 PM
 */
public class DownloadMultiplicationTableInfoEditPanel implements Initializable {

	/** Owner panel. */
	private DownloadMultiplicationTablePanel owner_;

	/** The owner popup window. */
	private PopOver popOver_;

	@FXML
	private VBox root_;

	@FXML
	private StackPane headerPane_;

	@FXML
	private Label name_;

	@FXML
	private TextField spectrumName_, ppName_, program_, section_, mission_, issue_, deliveryRef_, description_;

	@FXML
	private Button save_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onSaveClicked() {

		// get existing info
		MultiplicationTableInfo info = (MultiplicationTableInfo) owner_.getInfo();

		// create new multiplication table info
		MultiplicationTableInfo newInfo = new MultiplicationTableInfo();
		newInfo.setInfo(MultiplicationTableInfoType.SPECTRUM_NAME, spectrumName_.getText());
		newInfo.setInfo(MultiplicationTableInfoType.PILOT_POINT_NAME, ppName_.getText());
		newInfo.setInfo(MultiplicationTableInfoType.AC_PROGRAM, program_.getText());
		newInfo.setInfo(MultiplicationTableInfoType.AC_SECTION, section_.getText());
		newInfo.setInfo(MultiplicationTableInfoType.FAT_MISSION, mission_.getText());
		newInfo.setInfo(MultiplicationTableInfoType.ISSUE, issue_.getText());
		newInfo.setInfo(MultiplicationTableInfoType.DELIVERY_REF, deliveryRef_.getText());
		newInfo.setInfo(MultiplicationTableInfoType.DESCRIPTION, description_.getText());
		newInfo.setInfo(MultiplicationTableInfoType.ID, info.getID());
		newInfo.setInfo(MultiplicationTableInfoType.NAME, info.getInfo(MultiplicationTableInfoType.NAME));

		// check inputs
		if (!checkInputs(newInfo))
			return;

		// update info
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new UpdateMultiplicationTableInfoInGlobalDB(newInfo, owner_.getOwner()));

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
	private boolean checkInputs(MultiplicationTableInfo info) {

		// check required fields
		String spectrumName = (String) info.getInfo(MultiplicationTableInfoType.SPECTRUM_NAME);
		if (spectrumName == null || spectrumName.trim().isEmpty()) {
			String message = "Please supply spectrum name to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(spectrumName_);
			return false;
		}
		String ppName = (String) info.getInfo(MultiplicationTableInfoType.PILOT_POINT_NAME);
		if (ppName == null || ppName.trim().isEmpty()) {
			String message = "Please supply pilot point name to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(ppName_);
			return false;
		}
		String program = (String) info.getInfo(MultiplicationTableInfoType.AC_PROGRAM);
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
		String section = (String) info.getInfo(MultiplicationTableInfoType.AC_SECTION);
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
		String mission = (String) info.getInfo(MultiplicationTableInfoType.FAT_MISSION);
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

		// check spectrum name for Windows OS
		String message = Utility.isValidFileName(spectrumName);
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(spectrumName_);
			return false;
		}

		// check pilot point name for Windows OS
		if (!ppName.trim().isEmpty()) {
			message = Utility.isValidFileName(ppName);
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(ppName_);
				return false;
			}
		}

		// check input lengths
		if (spectrumName.trim().length() > 100)
			return showInputLengthWarning(spectrumName_, 100);
		if (ppName.trim().length() > 100)
			return showInputLengthWarning(ppName_, 100);
		if (program.trim().length() > 100)
			return showInputLengthWarning(program_, 100);
		if (section.trim().length() > 100)
			return showInputLengthWarning(section_, 100);
		if (mission.trim().length() > 50)
			return showInputLengthWarning(mission_, 50);
		String issue = (String) info.getInfo(MultiplicationTableInfoType.ISSUE);
		if (issue != null && issue.trim().length() > 50)
			return showInputLengthWarning(issue_, 50);
		String delRef = (String) info.getInfo(MultiplicationTableInfoType.DELIVERY_REF);
		if (delRef != null && delRef.trim().length() > 50)
			return showInputLengthWarning(deliveryRef_, 50);
		String description = (String) info.getInfo(MultiplicationTableInfoType.DESCRIPTION);
		if (description != null && description.trim().length() > 200)
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

	/**
	 * Sets up spectrum info listeners.
	 */
	private void setupInfoListeners() {
		MultiplicationTableInfoListener listener = new MultiplicationTableInfoListener();
		spectrumName_.textProperty().addListener(listener);
		ppName_.textProperty().addListener(listener);
		program_.textProperty().addListener(listener);
		section_.textProperty().addListener(listener);
		mission_.textProperty().addListener(listener);
		issue_.textProperty().addListener(listener);
		deliveryRef_.textProperty().addListener(listener);
		description_.textProperty().addListener(listener);
	}

	/**
	 * Checks for changes in spectrum info.
	 *
	 * @return True if there is change.
	 */
	private boolean checkForInfoChange() {

		// get info
		MultiplicationTableInfo info = (MultiplicationTableInfo) owner_.getInfo();

		// compare required fields
		if (!info.getInfo(MultiplicationTableInfoType.SPECTRUM_NAME).equals(spectrumName_.getText()))
			return true;
		if (!info.getInfo(MultiplicationTableInfoType.PILOT_POINT_NAME).equals(ppName_.getText()))
			return true;
		if (!info.getInfo(MultiplicationTableInfoType.AC_PROGRAM).equals(program_.getText()))
			return true;
		if (!info.getInfo(MultiplicationTableInfoType.AC_SECTION).equals(section_.getText()))
			return true;
		if (!info.getInfo(MultiplicationTableInfoType.FAT_MISSION).equals(mission_.getText()))
			return true;

		// compare optional fields
		if (checkForChangeInOptionalInfo(info, MultiplicationTableInfoType.ISSUE, issue_))
			return true;
		if (checkForChangeInOptionalInfo(info, MultiplicationTableInfoType.DELIVERY_REF, deliveryRef_))
			return true;
		if (checkForChangeInOptionalInfo(info, MultiplicationTableInfoType.DESCRIPTION, description_))
			return true;

		// no change
		return false;
	}

	/**
	 * Checks for changes in optional pilot point info.
	 *
	 * @param info
	 *            Pilot point info.
	 * @param infoType
	 *            Info value to check.
	 * @param textField
	 *            Text field entry.
	 * @return True if there is change.
	 */
	private static boolean checkForChangeInOptionalInfo(MultiplicationTableInfo info, MultiplicationTableInfoType infoType, TextField textField) {
		String value = (String) info.getInfo(infoType);
		String entry = textField.getText().trim();
		if (value == null) {
			if (!entry.equals("-") && !entry.isEmpty())
				return true;
		}
		else {
			if (!value.equals(entry))
				return true;
		}
		return false;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param info
	 *            Spectrum info.
	 * @param popOver
	 *            The owner popup window.
	 * @param owner
	 *            Owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static VBox load(MultiplicationTableInfo info, PopOver popOver, DownloadMultiplicationTablePanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadMultiplicationTableInfoEditPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			DownloadMultiplicationTableInfoEditPanel controller = (DownloadMultiplicationTableInfoEditPanel) fxmlLoader.getController();

			// set attributes
			controller.owner_ = owner;
			controller.popOver_ = popOver;
			controller.name_.setText((String) info.getInfo(MultiplicationTableInfoType.NAME));
			controller.spectrumName_.setText((String) info.getInfo(MultiplicationTableInfoType.SPECTRUM_NAME));
			String ppName = (String) info.getInfo(MultiplicationTableInfoType.PILOT_POINT_NAME);
			controller.ppName_.setText(ppName == null || ppName.trim().isEmpty() ? "-" : ppName);
			controller.program_.setText((String) info.getInfo(MultiplicationTableInfoType.AC_PROGRAM));
			controller.section_.setText((String) info.getInfo(MultiplicationTableInfoType.AC_SECTION));
			controller.mission_.setText((String) info.getInfo(MultiplicationTableInfoType.FAT_MISSION));
			String issue = (String) info.getInfo(MultiplicationTableInfoType.ISSUE);
			controller.issue_.setText(issue == null || issue.trim().isEmpty() ? "-" : issue);
			String deliveryRef = (String) info.getInfo(MultiplicationTableInfoType.DELIVERY_REF);
			controller.deliveryRef_.setText(deliveryRef == null || deliveryRef.trim().isEmpty() ? "-" : deliveryRef);
			String description = (String) info.getInfo(MultiplicationTableInfoType.DESCRIPTION);
			controller.description_.setText(description == null || description.trim().isEmpty() ? "-" : description);

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
	 * Inner class for multiplication table info change listener.
	 *
	 * @author Murat Artim
	 * @date Jun 28, 2016
	 * @time 8:33:38 PM
	 */
	public class MultiplicationTableInfoListener implements ChangeListener<String> {

		@Override
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
			save_.setVisible(checkForInfoChange());
		}
	}
}
