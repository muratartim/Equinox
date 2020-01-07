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

import equinox.data.EquinoxTheme;
import equinox.data.fileType.STFFile;
import equinox.task.SetSTFMission;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Class for override STF mission panel controller.
 *
 * @author Murat Artim
 * @date Nov 12, 2015
 * @time 4:37:38 PM
 */
public class OverrideSTFMissionPanel implements Initializable {

	/** The owner panel. */
	private InputPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** STF file. */
	private STFFile stfFile_;

	@FXML
	private VBox root_;

	@FXML
	private TextField mission_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Sets STF file mission to this panel.
	 *
	 * @param mission
	 *            Mission.
	 */
	public void setMission(String mission) {
		mission_.setText(mission);
	}

	@FXML
	private void onOkClicked() {

		// get mission
		String mission = mission_.getText();

		// hide pop-over
		popOver_.hide();

		// get spectrum mission
		String spectrumMission = stfFile_.getParentItem().getMission();

		// same as spectrum mission
		if (mission.equals(spectrumMission)) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SetSTFMission(stfFile_, ""));
		}
		else {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SetSTFMission(stfFile_, mission));
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
	 * @param stfFile
	 *            STF file.
	 * @return The newly loaded file CDF set panel.
	 */
	public static VBox load(PopOver popOver, InputPanel owner, STFFile stfFile) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("OverrideSTFMissionPanel.fxml"));
			fxmlLoader.load();

			// get controller
			OverrideSTFMissionPanel controller = (OverrideSTFMissionPanel) fxmlLoader.getController();

			// set attributes
			controller.popOver_ = popOver;
			controller.owner_ = owner;
			controller.stfFile_ = stfFile;
			controller.setMission(controller.stfFile_.getMission());

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
