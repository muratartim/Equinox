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
import equinox.data.fileType.STFFile;
import equinox.task.GetSTFInfo2;
import equinox.task.GetSTFInfo2.STFInfoRequestingPanel;
import equinox.task.SavePilotPointInfo;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Class for STF info panel.
 *
 * @author Murat Artim
 * @date Feb 2, 2016
 * @time 3:21:26 PM
 */
public class STFInfoPanel implements InternalInputSubPanel, STFInfoRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextField description_, dataSource_, genSource_, deliveryRef_, issue_, elementType_, framePos_, stringerPos_, eid_;

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
		return "Edit Pilot Point Info";
	}

	@Override
	public void showing() {
		onResetClicked();
	}

	@Override
	public void setSTFInfo(String[] info) {
		description_.setText(info[GetSTFInfo2.DESCRIPTION]);
		dataSource_.setText(info[GetSTFInfo2.DATA_SOURCE]);
		genSource_.setText(info[GetSTFInfo2.GEN_SOURCE]);
		deliveryRef_.setText(info[GetSTFInfo2.DELIVERY_REF]);
		issue_.setText(info[GetSTFInfo2.ISSUE]);
		elementType_.setText(info[GetSTFInfo2.ELEMENT_TYPE]);
		framePos_.setText(info[GetSTFInfo2.FRAME_RIB_POS]);
		stringerPos_.setText(info[GetSTFInfo2.STRINGER_POS]);
		eid_.setText(info[GetSTFInfo2.EID]);
	}

	@FXML
	private void onResetClicked() {

		// get selected STF file
		STFFile stfFile = (STFFile) owner_.getSelectedFiles().get(0);

		// get pilot point info
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetSTFInfo2(stfFile, this));
	}

	@FXML
	private void onOkClicked() {

		// get selected STF file
		STFFile stfFile = (STFFile) owner_.getSelectedFiles().get(0);

		// get inputs
		String[] info = new String[12];
		info[GetSTFInfo2.DESCRIPTION] = description_.getText();
		info[GetSTFInfo2.DATA_SOURCE] = dataSource_.getText();
		info[GetSTFInfo2.GEN_SOURCE] = genSource_.getText();
		info[GetSTFInfo2.DELIVERY_REF] = deliveryRef_.getText();
		info[GetSTFInfo2.ISSUE] = issue_.getText();
		info[GetSTFInfo2.ELEMENT_TYPE] = elementType_.getText();
		info[GetSTFInfo2.FRAME_RIB_POS] = framePos_.getText();
		info[GetSTFInfo2.STRINGER_POS] = stringerPos_.getText();
		info[GetSTFInfo2.EID] = eid_.getText();

		// check inputs
		if (!checkInputs(info))
			return;

		// save pilot point info
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new SavePilotPointInfo(stfFile, info));

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

		// check input lengths
		if ((info[GetSTFInfo2.DESCRIPTION] != null) && (info[GetSTFInfo2.DESCRIPTION].trim().length() > 200))
			return showInputLengthWarning(description_, 200);
		if ((info[GetSTFInfo2.DATA_SOURCE] != null) && (info[GetSTFInfo2.DATA_SOURCE].trim().length() > 50))
			return showInputLengthWarning(dataSource_, 50);
		if ((info[GetSTFInfo2.GEN_SOURCE] != null) && (info[GetSTFInfo2.GEN_SOURCE].trim().length() > 50))
			return showInputLengthWarning(genSource_, 50);
		if ((info[GetSTFInfo2.DELIVERY_REF] != null) && (info[GetSTFInfo2.DELIVERY_REF].trim().length() > 50))
			return showInputLengthWarning(deliveryRef_, 50);
		if ((info[GetSTFInfo2.ISSUE] != null) && (info[GetSTFInfo2.ISSUE].trim().length() > 50))
			return showInputLengthWarning(issue_, 50);
		if ((info[GetSTFInfo2.ELEMENT_TYPE] != null) && (info[GetSTFInfo2.ELEMENT_TYPE].trim().length() > 50))
			return showInputLengthWarning(elementType_, 50);
		if ((info[GetSTFInfo2.FRAME_RIB_POS] != null) && (info[GetSTFInfo2.FRAME_RIB_POS].trim().length() > 50))
			return showInputLengthWarning(framePos_, 50);
		if ((info[GetSTFInfo2.STRINGER_POS] != null) && (info[GetSTFInfo2.STRINGER_POS].trim().length() > 50))
			return showInputLengthWarning(stringerPos_, 50);
		if ((info[GetSTFInfo2.EID] != null) && (info[GetSTFInfo2.EID].trim().length() > 50))
			return showInputLengthWarning(eid_, 50);

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
		owner_.getOwner().showHelp("How to edit pilot point info", null);
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static STFInfoPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("STFInfoPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			STFInfoPanel controller = (STFInfoPanel) fxmlLoader.getController();

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
