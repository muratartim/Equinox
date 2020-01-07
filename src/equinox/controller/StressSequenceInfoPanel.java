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
import equinox.data.fileType.ExternalStressSequence;
import equinox.task.SaveStressSequenceInfo;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Class for external stress sequence info panel controller.
 *
 * @author Murat Artim
 * @date Jun 17, 2016
 * @time 1:14:06 PM
 */
public class StressSequenceInfoPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextField program_, section_, mission_;

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
		return "Edit Sequence Info";
	}

	@Override
	public void showing() {
		onResetClicked();
	}

	@FXML
	private void onResetClicked() {

		// set program, section and mission
		ExternalStressSequence sequence = (ExternalStressSequence) owner_.getSelectedFiles().get(0);
		program_.setText(sequence.getProgram());
		section_.setText(sequence.getSection());
		mission_.setText(sequence.getMission());
	}

	@FXML
	private void onOkClicked() {

		// get selected sequence
		ExternalStressSequence sequence = (ExternalStressSequence) owner_.getSelectedFiles().get(0);

		// get inputs
		String[] info = new String[3];
		info[SaveStressSequenceInfo.PROGRAM] = program_.getText();
		info[SaveStressSequenceInfo.SECTION] = section_.getText();
		info[SaveStressSequenceInfo.MISSION] = mission_.getText();

		// check inputs
		if (!checkInputs(info))
			return;

		// update info
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new SaveStressSequenceInfo(sequence, info));

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
		if ((info[SaveStressSequenceInfo.PROGRAM] != null) && (info[SaveStressSequenceInfo.PROGRAM].trim().length() > 100))
			return showInputLengthWarning(program_, 100);
		if ((info[SaveStressSequenceInfo.SECTION] != null) && (info[SaveStressSequenceInfo.SECTION].trim().length() > 100))
			return showInputLengthWarning(section_, 100);
		if ((info[SaveStressSequenceInfo.MISSION] != null) && (info[SaveStressSequenceInfo.MISSION].trim().length() > 50))
			return showInputLengthWarning(mission_, 50);

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
		owner_.getOwner().showHelp("How to edit stress seqeunce info", null);
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static StressSequenceInfoPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("StressSequenceInfoPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			StressSequenceInfoPanel controller = (StressSequenceInfoPanel) fxmlLoader.getController();

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
