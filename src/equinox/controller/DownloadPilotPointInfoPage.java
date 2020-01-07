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

import equinox.controller.DownloadPilotPointInfoPanel.InfoPage;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.font.IconicFont;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Class for download pilot point info page controller.
 *
 * @author Murat Artim
 * @date Feb 15, 2016
 * @time 2:52:53 PM
 */
public class DownloadPilotPointInfoPage implements InfoPage {

	@FXML
	private VBox root_;

	@FXML
	private Label spectrumName_, program_, section_, mission_, description_, elementType_, framePos_, stringerPos_, dataSource_, genSource_, deliveryRef_, issue_, fatigueMaterial_, preffasMaterial_, linearMaterial_, spectrumNameLabel_, programLabel_, sectionLabel_, missionLabel_, descriptionLabel_,
			elementTypeLabel_, framePosLabel_, stringerPosLabel_, dataSourceLabel_, genSourceLabel_, deliveryRefLabel_, issueLabel_, fatigueMaterialLabel_, preffasMaterialLabel_, linearMaterialLabel_, eidLabel_, eid_;

	/** Labels. */
	private Label[] labels_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		labels_ = new Label[] { spectrumNameLabel_, programLabel_, sectionLabel_, missionLabel_, descriptionLabel_, elementTypeLabel_, framePosLabel_, stringerPosLabel_, dataSourceLabel_, genSourceLabel_, deliveryRefLabel_, issueLabel_, fatigueMaterialLabel_, preffasMaterialLabel_,
				linearMaterialLabel_, eidLabel_ };
	}

	@Override
	public VBox getRoot() {
		return root_;
	}

	@Override
	public String getPageName() {
		return "Pilot Point Info";
	}

	@Override
	public void showing(PilotPointInfo info) {
		for (Label label : labels_) {
			label.setTextFill(Color.SLATEGRAY);
		}
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param info
	 *            Pilot point info.
	 * @return The newly loaded file CDF set panel.
	 */
	public static DownloadPilotPointInfoPage load(PilotPointInfo info) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadPilotPointInfoPage.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			DownloadPilotPointInfoPage controller = (DownloadPilotPointInfoPage) fxmlLoader.getController();

			// set attributes
			controller.spectrumName_.setText((String) info.getInfo(PilotPointInfoType.SPECTRUM_NAME));
			controller.program_.setText((String) info.getInfo(PilotPointInfoType.AC_PROGRAM));
			controller.section_.setText((String) info.getInfo(PilotPointInfoType.AC_SECTION));
			controller.mission_.setText((String) info.getInfo(PilotPointInfoType.FAT_MISSION));
			controller.description_.setText((String) info.getInfo(PilotPointInfoType.DESCRIPTION));
			controller.dataSource_.setText((String) info.getInfo(PilotPointInfoType.DATA_SOURCE));
			controller.genSource_.setText((String) info.getInfo(PilotPointInfoType.GENERATION_SOURCE));
			controller.deliveryRef_.setText((String) info.getInfo(PilotPointInfoType.DELIVERY_REF_NUM));
			controller.issue_.setText((String) info.getInfo(PilotPointInfoType.ISSUE));
			String item = (String) info.getInfo(PilotPointInfoType.ELEMENT_TYPE);
			controller.elementType_.setText(item == null ? "-" : item);
			item = (String) info.getInfo(PilotPointInfoType.FRAME_RIB_POSITION);
			controller.framePos_.setText(item == null ? "-" : item);
			item = (String) info.getInfo(PilotPointInfoType.STRINGER_POSITION);
			controller.stringerPos_.setText(item == null ? "-" : item);
			item = (String) info.getInfo(PilotPointInfoType.FATIGUE_MATERIAL);
			controller.fatigueMaterial_.setText(item == null ? "-" : item);
			item = (String) info.getInfo(PilotPointInfoType.PREFFAS_MATERIAL);
			controller.preffasMaterial_.setText(item == null ? "-" : item);
			item = (String) info.getInfo(PilotPointInfoType.LINEAR_MATERIAL);
			controller.linearMaterial_.setText(item == null ? "-" : item);
			item = (String) info.getInfo(PilotPointInfoType.EID);
			controller.eid_.setText(item == null ? "-" : item);

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
