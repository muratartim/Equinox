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
import equinox.dataServer.remote.data.MultiplicationTableInfo;
import equinox.dataServer.remote.data.MultiplicationTableInfo.MultiplicationTableInfoType;
import equinox.font.IconicFont;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Class for download multiplication table info panel controller.
 *
 * @author Murat Artim
 * @date Feb 29, 2016
 * @time 2:45:14 PM
 */
public class DownloadMultiplicationTableInfoPanel implements Initializable {

	/** The owner popup window. */
	private PopOver popOver_;

	@FXML
	private VBox root_;

	@FXML
	private StackPane headerPane_;

	@FXML
	private Label name_, spectrumName_, ppName_, program_, section_, mission_, issue_, deliveryRef_, description_;

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
	public static VBox load(MultiplicationTableInfo info, PopOver popOver) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadMultiplicationTableInfoPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			DownloadMultiplicationTableInfoPanel controller = (DownloadMultiplicationTableInfoPanel) fxmlLoader.getController();

			// set attributes
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
