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

import equinox.controller.DownloadPilotPointInfoPanel.InfoPage;
import equinox.controller.FatigueMaterialsPopup.FatigueMaterialAddingPanel;
import equinox.controller.LinearMaterialsPopup.LinearMaterialAddingPanel;
import equinox.controller.PreffasMaterialsPopup.PreffasMaterialAddingPanel;
import equinox.data.EquinoxTheme;
import equinox.data.material.FatigueMaterialItem;
import equinox.data.material.LinearMaterialItem;
import equinox.data.material.PreffasMaterialItem;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.font.IconicFont;
import equinox.task.UpdatePilotPointInfoInGlobalDB;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Class for download pilot point info edit page controller.
 *
 * @author Murat Artim
 * @date Jun 28, 2016
 * @time 11:43:45 PM
 */
public class DownloadPilotPointInfoEditPage implements InfoPage, FatigueMaterialAddingPanel, PreffasMaterialAddingPanel, LinearMaterialAddingPanel {

	/** The owner panel. */
	private DownloadPilotPointInfoPanel owner_;

	/** The owner popup. */
	private PopOver popOver_;

	@FXML
	private VBox root_;

	@FXML
	private TextField spectrumName_, program_, section_, mission_, description_, elementType_, framePos_, stringerPos_, dataSource_, genSource_, deliveryRef_, issue_, fatigueMaterial_, preffasMaterial_, linearMaterial_, eid_;

	@FXML
	private Label spectrumNameLabel_, programLabel_, sectionLabel_, missionLabel_, descriptionLabel_, elementTypeLabel_, framePosLabel_, stringerPosLabel_, dataSourceLabel_, genSourceLabel_, deliveryRefLabel_, issueLabel_, fatigueMaterialLabel_, preffasMaterialLabel_, linearMaterialLabel_,
			eidLabel_;

	@FXML
	private Button fatigueLibrary_, preffasLibrary_, linearLibrary_;

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
		owner_.getSaveButton().setVisible(checkForInfoChange());
		popOver_.setAutoHide(true);
		for (Label label : labels_) {
			label.setTextFill(Color.SLATEGRAY);
		}
	}

	/**
	 * Saves pilot point info to global database.
	 *
	 * @param info
	 *            Current pilot point info.
	 */
	public void onSaveClicked(PilotPointInfo info) {

		// create pilot point info
		PilotPointInfo newInfo = new PilotPointInfo();
		newInfo.setInfo(PilotPointInfoType.SPECTRUM_NAME, spectrumName_.getText());
		newInfo.setInfo(PilotPointInfoType.AC_PROGRAM, program_.getText());
		newInfo.setInfo(PilotPointInfoType.AC_SECTION, section_.getText());
		newInfo.setInfo(PilotPointInfoType.FAT_MISSION, mission_.getText());
		newInfo.setInfo(PilotPointInfoType.DESCRIPTION, description_.getText());
		newInfo.setInfo(PilotPointInfoType.ELEMENT_TYPE, elementType_.getText());
		newInfo.setInfo(PilotPointInfoType.FRAME_RIB_POSITION, framePos_.getText());
		newInfo.setInfo(PilotPointInfoType.STRINGER_POSITION, stringerPos_.getText());
		newInfo.setInfo(PilotPointInfoType.DATA_SOURCE, dataSource_.getText());
		newInfo.setInfo(PilotPointInfoType.GENERATION_SOURCE, genSource_.getText());
		newInfo.setInfo(PilotPointInfoType.DELIVERY_REF_NUM, deliveryRef_.getText());
		newInfo.setInfo(PilotPointInfoType.ISSUE, issue_.getText());
		newInfo.setInfo(PilotPointInfoType.FATIGUE_MATERIAL, fatigueMaterial_.getText());
		newInfo.setInfo(PilotPointInfoType.PREFFAS_MATERIAL, preffasMaterial_.getText());
		newInfo.setInfo(PilotPointInfoType.LINEAR_MATERIAL, linearMaterial_.getText());
		newInfo.setInfo(PilotPointInfoType.ID, info.getInfo(PilotPointInfoType.ID));
		newInfo.setInfo(PilotPointInfoType.NAME, info.getInfo(PilotPointInfoType.NAME));
		newInfo.setInfo(PilotPointInfoType.EID, eid_.getText());

		// check inputs
		if (!checkInputs(newInfo))
			return;

		// update info
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new UpdatePilotPointInfoInGlobalDB(newInfo, owner_.getOwner().getOwner()));

		// hide
		popOver_.hide();
	}

	@Override
	public void addLinearMaterials(ObservableList<LinearMaterialItem> materials) {
		for (LinearMaterialItem item : materials) {
			linearMaterial_.setText(item.getMaterial().toString());
			break;
		}
	}

	@Override
	public Node getLinearMaterialPopupNode() {
		return linearLibrary_;
	}

	@Override
	public void addPreffasMaterials(ObservableList<PreffasMaterialItem> materials) {
		for (PreffasMaterialItem item : materials) {
			preffasMaterial_.setText(item.getMaterial().toString());
			break;
		}
	}

	@Override
	public Node getPreffasMaterialPopupNode() {
		return preffasLibrary_;
	}

	@Override
	public void addFatigueMaterials(ObservableList<FatigueMaterialItem> materials) {
		for (FatigueMaterialItem item : materials) {
			fatigueMaterial_.setText(item.getMaterial().toString());
			break;
		}
	}

	@Override
	public Node getFatigueMaterialPopupNode() {
		return fatigueLibrary_;
	}

	@FXML
	private void onFatigueMaterialLibraryClicked() {
		((FatigueMaterialsPopup) owner_.getOwner().getOwner().getOwner().getOwner().getInputPanel().getPopup(InputPanel.FATIGUE_MATERIALS_POPUP)).show(this, ArrowLocation.RIGHT_BOTTOM, true);
	}

	@FXML
	private void onPreffasMaterialLibraryClicked() {
		((PreffasMaterialsPopup) owner_.getOwner().getOwner().getOwner().getOwner().getInputPanel().getPopup(InputPanel.PREFFAS_MATERIALS_POPUP)).show(this, ArrowLocation.RIGHT_BOTTOM, true);
	}

	@FXML
	private void onLinearMaterialLibraryClicked() {
		((LinearMaterialsPopup) owner_.getOwner().getOwner().getOwner().getOwner().getInputPanel().getPopup(InputPanel.LINEAR_MATERIALS_POPUP)).show(this, ArrowLocation.RIGHT_BOTTOM, true);
	}

	/**
	 * Checks for changes in spectrum info.
	 *
	 * @return True if there is change.
	 */
	private boolean checkForInfoChange() {

		// get info
		PilotPointInfo info = (PilotPointInfo) owner_.getOwner().getInfo();

		// compare required fields
		if (!info.getInfo(PilotPointInfoType.SPECTRUM_NAME).equals(spectrumName_.getText()))
			return true;
		if (!info.getInfo(PilotPointInfoType.AC_PROGRAM).equals(program_.getText()))
			return true;
		if (!info.getInfo(PilotPointInfoType.AC_SECTION).equals(section_.getText()))
			return true;
		if (!info.getInfo(PilotPointInfoType.FAT_MISSION).equals(mission_.getText()))
			return true;
		if (!info.getInfo(PilotPointInfoType.DESCRIPTION).equals(description_.getText()))
			return true;
		if (!info.getInfo(PilotPointInfoType.DATA_SOURCE).equals(dataSource_.getText()))
			return true;
		if (!info.getInfo(PilotPointInfoType.GENERATION_SOURCE).equals(genSource_.getText()))
			return true;
		if (!info.getInfo(PilotPointInfoType.DELIVERY_REF_NUM).equals(deliveryRef_.getText()))
			return true;
		if (!info.getInfo(PilotPointInfoType.ISSUE).equals(issue_.getText()))
			return true;

		// compare optional fields
		if (checkForChangeInOptionalInfo(info, PilotPointInfoType.ELEMENT_TYPE, elementType_))
			return true;
		if (checkForChangeInOptionalInfo(info, PilotPointInfoType.FRAME_RIB_POSITION, framePos_))
			return true;
		if (checkForChangeInOptionalInfo(info, PilotPointInfoType.STRINGER_POSITION, stringerPos_))
			return true;
		if (checkForChangeInOptionalInfo(info, PilotPointInfoType.FATIGUE_MATERIAL, fatigueMaterial_))
			return true;
		if (checkForChangeInOptionalInfo(info, PilotPointInfoType.PREFFAS_MATERIAL, preffasMaterial_))
			return true;
		if (checkForChangeInOptionalInfo(info, PilotPointInfoType.LINEAR_MATERIAL, linearMaterial_))
			return true;
		if (checkForChangeInOptionalInfo(info, PilotPointInfoType.EID, eid_))
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
	private static boolean checkForChangeInOptionalInfo(PilotPointInfo info, PilotPointInfoType infoType, TextField textField) {
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
	 * Checks inputs.
	 *
	 * @param info
	 *            Info array.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(PilotPointInfo info) {

		// check required fields
		String spectrumName = (String) info.getInfo(PilotPointInfoType.SPECTRUM_NAME);
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
		String program = (String) info.getInfo(PilotPointInfoType.AC_PROGRAM);
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
		String section = (String) info.getInfo(PilotPointInfoType.AC_SECTION);
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
		String mission = (String) info.getInfo(PilotPointInfoType.FAT_MISSION);
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
		String description = (String) info.getInfo(PilotPointInfoType.DESCRIPTION);
		if (description == null || description.trim().isEmpty()) {
			String message = "Please supply pilot point description to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(description_);
			return false;
		}
		String dataSource = (String) info.getInfo(PilotPointInfoType.DATA_SOURCE);
		if (dataSource == null || dataSource.trim().isEmpty()) {
			String message = "Please supply data source to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(dataSource_);
			return false;
		}
		String genSource = (String) info.getInfo(PilotPointInfoType.GENERATION_SOURCE);
		if (genSource == null || genSource.trim().isEmpty()) {
			String message = "Please supply generation source to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(genSource_);
			return false;
		}
		String delRef = (String) info.getInfo(PilotPointInfoType.DELIVERY_REF_NUM);
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
		String issue = (String) info.getInfo(PilotPointInfoType.ISSUE);
		if (issue == null || issue.trim().isEmpty()) {
			String message = "Please supply pilot point issue to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(issue_);
			return false;
		}

		// check input lengths
		if (spectrumName.trim().length() > 100)
			return showInputLengthWarning(spectrumName_, 100);
		if (program.trim().length() > 100)
			return showInputLengthWarning(program_, 100);
		if (section.trim().length() > 100)
			return showInputLengthWarning(section_, 100);
		if (mission.trim().length() > 50)
			return showInputLengthWarning(mission_, 50);
		if (description.trim().length() > 200)
			return showInputLengthWarning(description_, 200);
		if (dataSource.trim().length() > 50)
			return showInputLengthWarning(dataSource_, 50);
		if (genSource.trim().length() > 50)
			return showInputLengthWarning(genSource_, 50);
		if (delRef.trim().length() > 50)
			return showInputLengthWarning(deliveryRef_, 50);
		if (issue.trim().length() > 50)
			return showInputLengthWarning(issue_, 50);
		String elType = (String) info.getInfo(PilotPointInfoType.ELEMENT_TYPE);
		if (elType != null && elType.trim().length() > 50)
			return showInputLengthWarning(elementType_, 50);
		String framePos = (String) info.getInfo(PilotPointInfoType.FRAME_RIB_POSITION);
		if (framePos != null && framePos.trim().length() > 50)
			return showInputLengthWarning(framePos_, 50);
		String stringerPos = (String) info.getInfo(PilotPointInfoType.STRINGER_POSITION);
		if (stringerPos != null && stringerPos.trim().length() > 50)
			return showInputLengthWarning(stringerPos_, 50);
		String fatigueMaterial = (String) info.getInfo(PilotPointInfoType.FATIGUE_MATERIAL);
		if (fatigueMaterial != null && fatigueMaterial.trim().length() > 500)
			return showInputLengthWarning(fatigueMaterial_, 500);
		String preffasMaterial = (String) info.getInfo(PilotPointInfoType.PREFFAS_MATERIAL);
		if (preffasMaterial != null && preffasMaterial.trim().length() > 500)
			return showInputLengthWarning(preffasMaterial_, 500);
		String linearMaterial = (String) info.getInfo(PilotPointInfoType.LINEAR_MATERIAL);
		if (linearMaterial != null && linearMaterial.trim().length() > 500)
			return showInputLengthWarning(linearMaterial_, 500);
		String eid = (String) info.getInfo(PilotPointInfoType.EID);
		if (eid != null && eid.trim().length() > 50)
			return showInputLengthWarning(eid_, 50);

		// valid inputs
		return true;
	}

	/**
	 * Sets up spectrum info listeners.
	 */
	private void setupInfoListeners() {
		PilotPointInfoListener listener = new PilotPointInfoListener();
		spectrumName_.textProperty().addListener(listener);
		program_.textProperty().addListener(listener);
		section_.textProperty().addListener(listener);
		mission_.textProperty().addListener(listener);
		description_.textProperty().addListener(listener);
		elementType_.textProperty().addListener(listener);
		framePos_.textProperty().addListener(listener);
		stringerPos_.textProperty().addListener(listener);
		dataSource_.textProperty().addListener(listener);
		genSource_.textProperty().addListener(listener);
		deliveryRef_.textProperty().addListener(listener);
		issue_.textProperty().addListener(listener);
		fatigueMaterial_.textProperty().addListener(listener);
		preffasMaterial_.textProperty().addListener(listener);
		linearMaterial_.textProperty().addListener(listener);
		eid_.textProperty().addListener(listener);
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
	 *            Pilot point info.
	 * @param owner
	 *            The owner panel.
	 * @param popOver
	 *            The owner popup.
	 * @return The newly loaded file CDF set panel.
	 */
	public static DownloadPilotPointInfoEditPage load(PilotPointInfo info, DownloadPilotPointInfoPanel owner, PopOver popOver) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadPilotPointInfoEditPage.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			DownloadPilotPointInfoEditPage controller = (DownloadPilotPointInfoEditPage) fxmlLoader.getController();

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

			// set owner
			controller.owner_ = owner;
			controller.popOver_ = popOver;

			// listen for changes in info
			controller.setupInfoListeners();

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Inner class for pilot point info change listener.
	 *
	 * @author Murat Artim
	 * @date Jun 28, 2016
	 * @time 8:33:38 PM
	 */
	public class PilotPointInfoListener implements ChangeListener<String> {

		@Override
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
			owner_.getSaveButton().setVisible(checkForInfoChange());
		}
	}
}
