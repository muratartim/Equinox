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
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.font.IconicFont;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Class for download pilot point info panel controller.
 *
 * @author Murat Artim
 * @date Feb 15, 2016
 * @time 2:04:05 PM
 */
public class DownloadPilotPointInfoPanel implements Initializable {

	/** Owner panel. */
	private DownloadPilotPointPanel owner_;

	/** Pages. */
	private InfoPage[] infoPages_;

	/** Pilot point info. */
	private PilotPointInfo info_;

	/** The owner popup window. */
	private PopOver popOver_;

	@FXML
	private VBox root_;

	@FXML
	private StackPane headerPane_;

	@FXML
	private Label name_;

	@FXML
	private Pagination pagination_;

	@FXML
	private Button save_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup pagination page factory
		pagination_.setPageFactory(pageIndex -> {
			infoPages_[pageIndex].showing(info_);
			if (infoPages_[pageIndex] instanceof DownloadPilotPointInfoEditPage == false) {
				save_.setVisible(false);
			}
			return infoPages_[pageIndex].getRoot();
		});
	}

	/**
	 * Returns the owner panel.
	 *
	 * @return Owner panel.
	 */
	public DownloadPilotPointPanel getOwner() {
		return owner_;
	}

	/**
	 * Returns pilot point name.
	 *
	 * @return Pilot point name.
	 */
	public String getPilotPointName() {
		return name_.getText();
	}

	/**
	 * Returns the save button of this panel.
	 *
	 * @return The save button of this panel.
	 */
	public Button getSaveButton() {
		return save_;
	}

	@FXML
	private void onCloseClicked() {
		popOver_.hide();
	}

	@FXML
	private void onSaveClicked() {
		DownloadPilotPointInfoEditPage page = (DownloadPilotPointInfoEditPage) infoPages_[0];
		page.onSaveClicked(info_);
	}

	@FXML
	private void onInfoClicked() {
		pagination_.setCurrentPageIndex(0);
	}

	@FXML
	private void onImageClicked() {
		pagination_.setCurrentPageIndex(1);
	}

	@FXML
	private void onMissionProfileClicked() {
		pagination_.setCurrentPageIndex(2);
	}

	@FXML
	private void onLogestFlightClicked() {
		pagination_.setCurrentPageIndex(3);
	}

	@FXML
	private void onHighestOccurrenceFlightClicked() {
		pagination_.setCurrentPageIndex(4);
	}

	@FXML
	private void onHighestStressFlightClicked() {
		pagination_.setCurrentPageIndex(5);
	}

	@FXML
	private void onLevelCrossingsClicked() {
		pagination_.setCurrentPageIndex(6);
	}

	@FXML
	private void onDamageAngleClicked() {
		pagination_.setCurrentPageIndex(7);
	}

	@FXML
	private void onNumberOfPeaksClicked() {
		pagination_.setCurrentPageIndex(8);
	}

	@FXML
	private void onFlightOccurrencesClicked() {
		pagination_.setCurrentPageIndex(9);
	}

	@FXML
	private void onRainflowHistogramClicked() {
		pagination_.setCurrentPageIndex(10);
	}

	@FXML
	private void onLoadcaseDamageContributionClicked() {
		pagination_.setCurrentPageIndex(11);
	}

	@FXML
	private void onFlightDamageContributionClicked() {
		pagination_.setCurrentPageIndex(12);
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param info
	 *            Pilot point info.
	 * @param owner
	 *            The owner panel.
	 * @param canEdit
	 *            True if the current user can edit pilot point info in global database.
	 * @param popOver
	 *            The owner popup.
	 * @return The newly loaded file CDF set panel.
	 */
	public static VBox load(PilotPointInfo info, DownloadPilotPointPanel owner, boolean canEdit, PopOver popOver) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadPilotPointInfoPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			DownloadPilotPointInfoPanel controller = (DownloadPilotPointInfoPanel) fxmlLoader.getController();

			// set attributes
			controller.owner_ = owner;
			controller.popOver_ = popOver;
			controller.info_ = info;
			controller.name_.setText((String) info.getInfo(PilotPointInfoType.NAME));
			controller.infoPages_ = new InfoPage[13];
			controller.infoPages_[0] = canEdit ? DownloadPilotPointInfoEditPage.load(info, controller, popOver) : DownloadPilotPointInfoPage.load(info);
			controller.infoPages_[1] = DownloadPilotPointImagePage.load(controller, PilotPointImageType.IMAGE, canEdit, popOver);
			controller.infoPages_[2] = DownloadPilotPointImagePage.load(controller, PilotPointImageType.MISSION_PROFILE, canEdit, popOver);
			controller.infoPages_[3] = DownloadPilotPointImagePage.load(controller, PilotPointImageType.LONGEST_FLIGHT, canEdit, popOver);
			controller.infoPages_[4] = DownloadPilotPointImagePage.load(controller, PilotPointImageType.FLIGHT_WITH_HIGHEST_OCCURRENCE, canEdit, popOver);
			controller.infoPages_[5] = DownloadPilotPointImagePage.load(controller, PilotPointImageType.FLIGHT_WITH_MAX_TOTAL_STRESS, canEdit, popOver);
			controller.infoPages_[6] = DownloadPilotPointImagePage.load(controller, PilotPointImageType.LEVEL_CROSSING, canEdit, popOver);
			controller.infoPages_[7] = DownloadPilotPointImagePage.load(controller, PilotPointImageType.DAMAGE_ANGLE, canEdit, popOver);
			controller.infoPages_[8] = DownloadPilotPointImagePage.load(controller, PilotPointImageType.NUMBER_OF_PEAKS, canEdit, popOver);
			controller.infoPages_[9] = DownloadPilotPointImagePage.load(controller, PilotPointImageType.FLIGHT_OCCURRENCE, canEdit, popOver);
			controller.infoPages_[10] = DownloadPilotPointImagePage.load(controller, PilotPointImageType.RAINFLOW_HISTOGRAM, canEdit, popOver);
			controller.infoPages_[11] = DownloadPilotPointImagePage.load(controller, PilotPointImageType.LOADCASE_DAMAGE_CONTRIBUTION, canEdit, popOver);
			controller.infoPages_[12] = DownloadPilotPointImagePage.load(controller, PilotPointImageType.FLIGHT_DAMAGE_CONTRIBUTION, canEdit, popOver);

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
	 * Interface for info pages.
	 *
	 * @author Murat Artim
	 * @date Feb 15, 2016
	 * @time 3:06:31 PM
	 */
	public interface InfoPage extends Initializable {

		/**
		 * Returns the root element of the page.
		 *
		 * @return The root element of the page.
		 */
		VBox getRoot();

		/**
		 * Returns page name.
		 *
		 * @return Page name.
		 */
		String getPageName();

		/**
		 * Called just before the page is shown.
		 *
		 * @param info
		 *            Pilot point info.
		 */
		void showing(PilotPointInfo info);
	}
}
