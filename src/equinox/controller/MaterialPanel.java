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

import equinox.controller.FatigueMaterialsPopup.FatigueMaterialAddingPanel;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.LinearMaterialsPopup.LinearMaterialAddingPanel;
import equinox.controller.PreffasMaterialsPopup.PreffasMaterialAddingPanel;
import equinox.data.EquinoxTheme;
import equinox.data.IsamiVersion;
import equinox.data.Settings;
import equinox.data.material.FatigueMaterialItem;
import equinox.data.material.LinearMaterialItem;
import equinox.data.material.PreffasMaterialItem;
import equinox.dataServer.remote.data.Material;
import equinox.task.GetMaterials;
import equinox.task.GetMaterials.MaterialRequestingPanel;
import equinox.task.SaveMaterialInfo;
import equinox.utility.Utility;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Class for STF material panel controller.
 *
 * @author Murat Artim
 * @date 30 Aug 2017
 * @time 10:21:44
 *
 */
public class MaterialPanel implements InternalInputSubPanel, FatigueMaterialAddingPanel, PreffasMaterialAddingPanel, LinearMaterialAddingPanel, MaterialRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextField fatigueMaterial_, preffasMaterial_, linearMaterial_;

	@FXML
	private Button fatigueLibrary_, preffasLibrary_, linearLibrary_;

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
		return "Set Materials";
	}

	@Override
	public void showing() {
		onResetClicked();
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

	@Override
	public void setMaterials(Material[] materials) {
		fatigueMaterial_.setText(materials[GetMaterials.FATIGUE_MATERIAL] == null ? null : materials[GetMaterials.FATIGUE_MATERIAL].toString());
		preffasMaterial_.setText(materials[GetMaterials.PREFFAS_MATERIAL] == null ? null : materials[GetMaterials.PREFFAS_MATERIAL].toString());
		linearMaterial_.setText(materials[GetMaterials.LINEAR_MATERIAL] == null ? null : materials[GetMaterials.LINEAR_MATERIAL].toString());
	}

	@FXML
	private void onResetClicked() {

		// request material info
		IsamiVersion isamiVersion = (IsamiVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_VERSION);
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetMaterials(owner_.getSelectedFiles(), this, isamiVersion));
	}

	@FXML
	private void onOkClicked() {

		// get inputs
		String[] materials = new String[3];
		materials[GetMaterials.FATIGUE_MATERIAL] = fatigueMaterial_.getText();
		materials[GetMaterials.PREFFAS_MATERIAL] = preffasMaterial_.getText();
		materials[GetMaterials.LINEAR_MATERIAL] = linearMaterial_.getText();

		// check inputs
		if (!checkInputs(materials))
			return;

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// get selected files
		tm.runTaskInParallel(new SaveMaterialInfo(owner_.getSelectedFiles(), materials));

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Checks inputs.
	 *
	 * @param materials
	 *            Material info array.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(String[] materials) {

		// check input lengths
		if (materials[GetMaterials.FATIGUE_MATERIAL] != null && materials[GetMaterials.FATIGUE_MATERIAL].trim().length() > 500)
			return showInputLengthWarning(fatigueMaterial_, 500);
		if (materials[GetMaterials.PREFFAS_MATERIAL] != null && materials[GetMaterials.PREFFAS_MATERIAL].trim().length() > 500)
			return showInputLengthWarning(preffasMaterial_, 500);
		if (materials[GetMaterials.LINEAR_MATERIAL] != null && materials[GetMaterials.LINEAR_MATERIAL].trim().length() > 500)
			return showInputLengthWarning(linearMaterial_, 500);

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
		// LATER on help clicked
		owner_.getOwner().showHelp("How to set materials to pilot points", null);
	}

	@FXML
	private void onFatigueMaterialLibraryClicked() {
		((FatigueMaterialsPopup) owner_.getPopup(InputPanel.FATIGUE_MATERIALS_POPUP)).show(this, ArrowLocation.LEFT_TOP, false);
	}

	@FXML
	private void onPreffasMaterialLibraryClicked() {
		((PreffasMaterialsPopup) owner_.getPopup(InputPanel.PREFFAS_MATERIALS_POPUP)).show(this, ArrowLocation.LEFT_TOP, false);
	}

	@FXML
	private void onLinearMaterialLibraryClicked() {
		((LinearMaterialsPopup) owner_.getPopup(InputPanel.LINEAR_MATERIALS_POPUP)).show(this, ArrowLocation.LEFT_TOP, false);
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static MaterialPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("MaterialPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			MaterialPanel controller = (MaterialPanel) fxmlLoader.getController();

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
