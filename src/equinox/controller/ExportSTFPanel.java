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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.FatigueMaterialsPopup.FatigueMaterialAddingPanel;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.LinearMaterialsPopup.LinearMaterialAddingPanel;
import equinox.controller.PreffasMaterialsPopup.PreffasMaterialAddingPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.STFFile;
import equinox.data.material.FatigueMaterialItem;
import equinox.data.material.LinearMaterialItem;
import equinox.data.material.PreffasMaterialItem;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.ExportSTF;
import equinox.task.GetSTFInfo2;
import equinox.task.GetSTFInfo2.STFInfoRequestingPanel;
import equinox.utility.Utility;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Pagination;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for export STF panel controller.
 *
 * @author Murat Artim
 * @date Feb 5, 2016
 * @time 11:10:55 AM
 */
public class ExportSTFPanel implements InternalInputSubPanel, STFInfoRequestingPanel, FatigueMaterialAddingPanel, PreffasMaterialAddingPanel, LinearMaterialAddingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Image panels. */
	private ExportSTFImagePanel[] imagePanels_;

	@FXML
	private VBox root_;

	@FXML
	private TextField ppName_, spectrumName_, description_, dataSource_, genSource_, deliveryRef_, issue_, elementType_, framePos_, stringerPos_, eid_, mission_, fatigueMaterial_, preffasMaterial_, linearMaterial_;

	@FXML
	private Button ok_, fatigueLibrary_, preffasLibrary_, linearLibrary_;

	@FXML
	private Accordion accordion_;

	@FXML
	private Pagination pagination_;

	@FXML
	private ComboBox<PilotPointImageType> imageType_;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		// set image types
		imageType_.getItems().setAll(PilotPointImageType.values());
		imageType_.getSelectionModel().select(PilotPointImageType.IMAGE);

		// setup pagination page factory
		pagination_.setPageFactory(pageIndex -> imagePanels_[pageIndex].getRoot());

		// expand first pane
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
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
		return "Export Pilot Point";
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
		fatigueMaterial_.setText(info[GetSTFInfo2.FATIGUE_MATERIAL]);
		preffasMaterial_.setText(info[GetSTFInfo2.PREFFAS_MATERIAL]);
		linearMaterial_.setText(info[GetSTFInfo2.LINEAR_MATERIAL]);
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
	private void onImageTypeSelected() {

		// get selected image type
		PilotPointImageType selected = imageType_.getSelectionModel().getSelectedItem();

		// nothing selected
		if (selected == null)
			return;

		// set selected image panel
		for (int i = 0; i < imagePanels_.length; i++) {
			if (imagePanels_[i].getImageType().equals(selected)) {
				pagination_.setCurrentPageIndex(i);
				break;
			}
		}
	}

	@FXML
	private void onResetClicked() {

		// get selected STF file
		STFFile stfFile = (STFFile) owner_.getSelectedFiles().get(0);

		// set pilot point and spectrum names
		ppName_.setText(FileType.getNameWithoutExtension(stfFile.getName()));
		spectrumName_.setText(stfFile.getParentItem().getName());

		// set fatigue mission
		mission_.setText(stfFile.getMission());

		// get pilot point info and image
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetSTFInfo2(stfFile, this));

		// reset image panels
		for (ExportSTFImagePanel panel : imagePanels_) {
			panel.onResetClicked();
		}

		// reset image type
		imageType_.getSelectionModel().select(PilotPointImageType.IMAGE);
	}

	@FXML
	private void onOkClicked() {

		// get selected STF file
		STFFile stfFile = (STFFile) owner_.getSelectedFiles().get(0);

		// get pilot point and spectrum names
		String ppName = ppName_.getText();
		String spectrumName = spectrumName_.getText();
		String mission = mission_.getText();

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
		info[GetSTFInfo2.FATIGUE_MATERIAL] = fatigueMaterial_.getText();
		info[GetSTFInfo2.PREFFAS_MATERIAL] = preffasMaterial_.getText();
		info[GetSTFInfo2.LINEAR_MATERIAL] = linearMaterial_.getText();

		// check inputs
		if (!checkInputs(ppName, spectrumName, mission, info))
			return;

		// get pilot point images
		HashMap<PilotPointImageType, Image> images = new HashMap<>();
		for (ExportSTFImagePanel panel : imagePanels_) {
			Image image = panel.getImage();
			if (image != null) {
				images.put(panel.getImageType(), image);
			}
		}

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.ZIP.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName(ppName + ".zip");
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File output = FileType.appendExtension(selectedFile, FileType.ZIP);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new ExportSTF(stfFile, ppName, spectrumName, mission, info, images, output));

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Checks inputs.
	 *
	 * @param ppName
	 *            Pilot point name.
	 * @param spectrumName
	 *            Spectrum name.
	 * @param mission
	 *            Fatigue mission.
	 * @param info
	 *            Info array.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(String ppName, String spectrumName, String mission, String[] info) {

		// check missing inputs
		if (ppName == null || ppName.trim().isEmpty())
			return showMissingInputWarning(ppName_, "Please supply pilot point name to proceed.");
		if (spectrumName == null || spectrumName.trim().isEmpty())
			return showMissingInputWarning(spectrumName_, "Please supply spectrum name to proceed.");
		if (mission == null || mission.trim().isEmpty())
			return showMissingInputWarning(mission_, "Please supply fatigue mission to proceed.");
		if (info[GetSTFInfo2.DESCRIPTION] == null || info[GetSTFInfo2.DESCRIPTION].trim().isEmpty())
			return showMissingInputWarning(description_, "Please supply pilot point description to proceed.");
		if (info[GetSTFInfo2.DATA_SOURCE] == null || info[GetSTFInfo2.DATA_SOURCE].trim().isEmpty())
			return showMissingInputWarning(dataSource_, "Please supply data source to proceed.");
		if (info[GetSTFInfo2.GEN_SOURCE] == null || info[GetSTFInfo2.GEN_SOURCE].trim().isEmpty())
			return showMissingInputWarning(genSource_, "Please supply generation source to proceed.");
		if (info[GetSTFInfo2.ISSUE] == null || info[GetSTFInfo2.ISSUE].trim().isEmpty())
			return showMissingInputWarning(issue_, "Please supply pilot point issue to proceed.");

		// check pilot point name for Windows OS
		String message = Utility.isValidFileName(ppName);
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

		// check spectrum name for Windows OS
		message = Utility.isValidFileName(spectrumName);
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

		// check input lengths
		if (ppName.trim().length() > 100)
			return showInputLengthWarning(ppName_, 100);
		if (spectrumName.trim().length() > 100)
			return showInputLengthWarning(spectrumName_, 100);
		if (mission.trim().length() > 50)
			return showInputLengthWarning(mission_, 50);
		if (info[GetSTFInfo2.DESCRIPTION] != null && info[GetSTFInfo2.DESCRIPTION].trim().length() > 200)
			return showInputLengthWarning(description_, 200);
		if (info[GetSTFInfo2.DATA_SOURCE] != null && info[GetSTFInfo2.DATA_SOURCE].trim().length() > 50)
			return showInputLengthWarning(dataSource_, 50);
		if (info[GetSTFInfo2.GEN_SOURCE] != null && info[GetSTFInfo2.GEN_SOURCE].trim().length() > 50)
			return showInputLengthWarning(genSource_, 50);
		if (info[GetSTFInfo2.DELIVERY_REF] != null && info[GetSTFInfo2.DELIVERY_REF].trim().length() > 50)
			return showInputLengthWarning(deliveryRef_, 50);
		if (info[GetSTFInfo2.ISSUE] != null && info[GetSTFInfo2.ISSUE].trim().length() > 50)
			return showInputLengthWarning(issue_, 50);
		if (info[GetSTFInfo2.ELEMENT_TYPE] != null && info[GetSTFInfo2.ELEMENT_TYPE].trim().length() > 50)
			return showInputLengthWarning(elementType_, 50);
		if (info[GetSTFInfo2.FRAME_RIB_POS] != null && info[GetSTFInfo2.FRAME_RIB_POS].trim().length() > 50)
			return showInputLengthWarning(framePos_, 50);
		if (info[GetSTFInfo2.STRINGER_POS] != null && info[GetSTFInfo2.STRINGER_POS].trim().length() > 50)
			return showInputLengthWarning(stringerPos_, 50);
		if (info[GetSTFInfo2.EID] != null && info[GetSTFInfo2.EID].trim().length() > 50)
			return showInputLengthWarning(eid_, 50);
		if (info[GetSTFInfo2.FATIGUE_MATERIAL] != null && info[GetSTFInfo2.FATIGUE_MATERIAL].trim().length() > 500)
			return showInputLengthWarning(fatigueMaterial_, 500);
		if (info[GetSTFInfo2.PREFFAS_MATERIAL] != null && info[GetSTFInfo2.PREFFAS_MATERIAL].trim().length() > 500)
			return showInputLengthWarning(preffasMaterial_, 500);
		if (info[GetSTFInfo2.LINEAR_MATERIAL] != null && info[GetSTFInfo2.LINEAR_MATERIAL].trim().length() > 500)
			return showInputLengthWarning(linearMaterial_, 500);

		// valid inputs
		return true;
	}

	/**
	 * Shows missing input warning message.
	 *
	 * @param node
	 *            Node to display the message.
	 * @param message
	 *            Warning message.
	 * @return False.
	 */
	private static boolean showMissingInputWarning(Node node, String message) {
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
		owner_.getOwner().showHelp("How to export pilot points", null);
	}

	@FXML
	private void onFatigueMaterialLibraryClicked() {
		((FatigueMaterialsPopup) owner_.getPopup(InputPanel.FATIGUE_MATERIALS_POPUP)).show(this, ArrowLocation.LEFT_BOTTOM, false);
	}

	@FXML
	private void onPreffasMaterialLibraryClicked() {
		((PreffasMaterialsPopup) owner_.getPopup(InputPanel.PREFFAS_MATERIALS_POPUP)).show(this, ArrowLocation.LEFT_BOTTOM, false);
	}

	@FXML
	private void onLinearMaterialLibraryClicked() {
		((LinearMaterialsPopup) owner_.getPopup(InputPanel.LINEAR_MATERIALS_POPUP)).show(this, ArrowLocation.LEFT_BOTTOM, false);
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static ExportSTFPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ExportSTFPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			ExportSTFPanel controller = (ExportSTFPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;
			controller.imagePanels_ = new ExportSTFImagePanel[12];
			controller.imagePanels_[0] = ExportSTFImagePanel.load(controller, PilotPointImageType.IMAGE);
			controller.imagePanels_[1] = ExportSTFImagePanel.load(controller, PilotPointImageType.MISSION_PROFILE);
			controller.imagePanels_[2] = ExportSTFImagePanel.load(controller, PilotPointImageType.LONGEST_FLIGHT);
			controller.imagePanels_[3] = ExportSTFImagePanel.load(controller, PilotPointImageType.FLIGHT_WITH_HIGHEST_OCCURRENCE);
			controller.imagePanels_[4] = ExportSTFImagePanel.load(controller, PilotPointImageType.FLIGHT_WITH_MAX_TOTAL_STRESS);
			controller.imagePanels_[5] = ExportSTFImagePanel.load(controller, PilotPointImageType.LEVEL_CROSSING);
			controller.imagePanels_[6] = ExportSTFImagePanel.load(controller, PilotPointImageType.DAMAGE_ANGLE);
			controller.imagePanels_[7] = ExportSTFImagePanel.load(controller, PilotPointImageType.NUMBER_OF_PEAKS);
			controller.imagePanels_[8] = ExportSTFImagePanel.load(controller, PilotPointImageType.FLIGHT_OCCURRENCE);
			controller.imagePanels_[9] = ExportSTFImagePanel.load(controller, PilotPointImageType.RAINFLOW_HISTOGRAM);
			controller.imagePanels_[10] = ExportSTFImagePanel.load(controller, PilotPointImageType.LOADCASE_DAMAGE_CONTRIBUTION);
			controller.imagePanels_[11] = ExportSTFImagePanel.load(controller, PilotPointImageType.FLIGHT_DAMAGE_CONTRIBUTION);

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
