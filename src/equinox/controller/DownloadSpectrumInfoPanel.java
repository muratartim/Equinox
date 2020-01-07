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
import equinox.dataServer.remote.data.SpectrumInfo;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.font.IconicFont;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Class for download spectrum info panel controller.
 *
 * @author Murat Artim
 * @date Jun 26, 2016
 * @time 8:56:53 AM
 */
public class DownloadSpectrumInfoPanel implements Initializable {

	/** The owner popup window. */
	private PopOver popOver_;

	@FXML
	private VBox root_;

	@FXML
	private StackPane headerPane_;

	@FXML
	private Label name_, size_, pilotPoints_, multiplicationTables_, program_, section_, mission_, missionIssue_, flpIssue_, iflpIssue_, cdfIssue_, deliveryRef_, description_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onCloseClicked() {
		popOver_.hide();
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param info
	 *            Spectrum info.
	 * @param popOver
	 *            The owner popup window.
	 * @return The newly loaded file CDF set panel.
	 */
	public static VBox load(SpectrumInfo info, PopOver popOver) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadSpectrumInfoPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			DownloadSpectrumInfoPanel controller = (DownloadSpectrumInfoPanel) fxmlLoader.getController();

			// set attributes
			controller.popOver_ = popOver;
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

			// listen for detach events
			controller.popOver_.detachedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
				if (newValue) {
					controller.root_.getChildren().remove(controller.headerPane_);
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
}
