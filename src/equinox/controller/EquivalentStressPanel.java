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
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import control.validationField.DoubleValidationField;
import equinox.controller.FatigueMaterialsPopup.FatigueMaterialAddingPanel;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.LinearMaterialsPopup.LinearMaterialAddingPanel;
import equinox.controller.PreffasMaterialsPopup.PreffasMaterialAddingPanel;
import equinox.controller.ScheduleTaskPanel.SchedulingPanel;
import equinox.data.AnalysisEngine;
import equinox.data.EquinoxTheme;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.Settings;
import equinox.data.fileType.SpectrumItem;
import equinox.data.fileType.StressSequence;
import equinox.data.input.EquivalentStressInput;
import equinox.data.material.FatigueMaterialItem;
import equinox.data.material.LinearMaterialItem;
import equinox.data.material.PreffasMaterialItem;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.LinearMaterial;
import equinox.dataServer.remote.data.Material;
import equinox.dataServer.remote.data.PreffasMaterial;
import equinox.font.IconicFont;
import equinox.task.EquivalentStressAnalysis;
import equinox.task.GetMaterials;
import equinox.task.GetMaterials.MaterialRequestingPanel;
import equinox.task.SaveTask;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

/**
 * Class for equivalent stress panel controller.
 *
 * @author Murat Artim
 * @date Jul 7, 2014
 * @time 10:59:35 AM
 */
public class EquivalentStressPanel implements InternalInputSubPanel, SchedulingPanel, FatigueMaterialAddingPanel, PreffasMaterialAddingPanel, LinearMaterialAddingPanel, MaterialRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ToggleSwitch removeNegative_, omission_, fatigueAnalysis_, preffasAnalysis_, linearAnalysis_;

	@FXML
	private DoubleValidationField omissionLevel_, modifier_;

	@FXML
	private MenuButton modifierMethod_;

	@FXML
	private SplitMenuButton ok_;

	@FXML
	private TitledPane modifierPane_, fatigueMaterialsPane_, preffasMaterialsPane_, linearMaterialsPane_;

	@FXML
	private Accordion accordion1_, accordion2_;

	@FXML
	private Button addFatigueMaterials_, addPreffasMaterials_, addLinearMaterials_, removeFatigueMaterials_, removePreffasMaterials_, removeLinearMaterials_, resetFatigueMaterials_, resetPreffasMaterials_, resetLinearMaterials_;

	@FXML
	private ListView<FatigueMaterial> fatigueMaterials_;

	@FXML
	private ListView<PreffasMaterial> preffasMaterials_;

	@FXML
	private ListView<LinearMaterial> linearMaterials_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// bind components
		omissionLevel_.disableProperty().bind(omission_.selectedProperty().not());
		fatigueAnalysis_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
			fatigueMaterialsPane_.setDisable(!newValue);
			if (fatigueMaterialsPane_.isDisabled()) {
				fatigueMaterialsPane_.setExpanded(false);
			}
		});
		preffasAnalysis_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
			preffasMaterialsPane_.setDisable(!newValue);
			if (preffasMaterialsPane_.isDisabled()) {
				preffasMaterialsPane_.setExpanded(false);
			}
		});
		linearAnalysis_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
			linearMaterialsPane_.setDisable(!newValue);
			if (linearMaterialsPane_.isDisabled()) {
				linearMaterialsPane_.setExpanded(false);
			}
		});
		fatigueMaterials_.getSelectionModel().getSelectedItems().addListener((ListChangeListener<FatigueMaterial>) c -> removeFatigueMaterials_.setDisable(fatigueMaterials_.getSelectionModel().getSelectedItems().isEmpty()));
		preffasMaterials_.getSelectionModel().getSelectedItems().addListener((ListChangeListener<PreffasMaterial>) c -> removePreffasMaterials_.setDisable(preffasMaterials_.getSelectionModel().getSelectedItems().isEmpty()));
		linearMaterials_.getSelectionModel().getSelectedItems().addListener((ListChangeListener<LinearMaterial>) c -> removeLinearMaterials_.setDisable(linearMaterials_.getSelectionModel().getSelectedItems().isEmpty()));
		fatigueMaterials_.getItems().addListener((ListChangeListener<FatigueMaterial>) c -> resetFatigueMaterials_.setDisable(fatigueMaterials_.getItems().isEmpty()));
		preffasMaterials_.getItems().addListener((ListChangeListener<PreffasMaterial>) c -> resetPreffasMaterials_.setDisable(preffasMaterials_.getItems().isEmpty()));
		linearMaterials_.getItems().addListener((ListChangeListener<LinearMaterial>) c -> resetLinearMaterials_.setDisable(linearMaterials_.getItems().isEmpty()));

		// enable multiple selection for lists
		fatigueMaterials_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		preffasMaterials_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		linearMaterials_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// set place holders for material lists
		fatigueMaterials_.setPlaceholder(new Label("No fatigue material added."));
		preffasMaterials_.setPlaceholder(new Label("No preffas prop. material added."));
		linearMaterials_.setPlaceholder(new Label("No linear prop. material added."));

		// setup validation fields
		omissionLevel_.setDefaultValue(0.0);
		omissionLevel_.setMinimumValue(0.0, true);
		modifier_.setDefaultValue(1.0);

		// expand first panes
		accordion1_.setExpandedPane(accordion1_.getPanes().get(0));
		accordion2_.setExpandedPane(accordion2_.getPanes().get(0));
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
	public void showing() {
		modifierPane_.setDisable(owner_.getSelectedFiles().get(0) instanceof StressSequence);
		onResetClicked();
	}

	@Override
	public String getHeader() {
		return "Equivalent Stress Analysis";
	}

	@Override
	public void addFatigueMaterials(ObservableList<FatigueMaterialItem> materials) {
		for (FatigueMaterialItem item : materials) {
			FatigueMaterial material = item.getMaterial();
			if (!fatigueMaterials_.getItems().contains(material)) {
				fatigueMaterials_.getItems().add(material);
			}
			else {
				int index = fatigueMaterials_.getItems().indexOf(material);
				fatigueMaterials_.getItems().set(index, material);
			}
		}
	}

	@Override
	public Node getFatigueMaterialPopupNode() {
		return addFatigueMaterials_;
	}

	@Override
	public void addPreffasMaterials(ObservableList<PreffasMaterialItem> materials) {
		for (PreffasMaterialItem item : materials) {
			PreffasMaterial material = item.getMaterial();
			if (!preffasMaterials_.getItems().contains(material)) {
				preffasMaterials_.getItems().add(material);
			}
			else {
				int index = preffasMaterials_.getItems().indexOf(material);
				preffasMaterials_.getItems().set(index, material);
			}
		}
	}

	@Override
	public Node getPreffasMaterialPopupNode() {
		return addPreffasMaterials_;
	}

	@Override
	public void addLinearMaterials(ObservableList<LinearMaterialItem> materials) {
		for (LinearMaterialItem item : materials) {
			LinearMaterial material = item.getMaterial();
			if (!linearMaterials_.getItems().contains(material)) {
				linearMaterials_.getItems().add(material);
			}
			else {
				int index = linearMaterials_.getItems().indexOf(material);
				linearMaterials_.getItems().set(index, material);
			}
		}
	}

	@Override
	public Node getLinearMaterialPopupNode() {
		return addLinearMaterials_;
	}

	@Override
	public void setTaskScheduleDate(boolean runNow, Date scheduleDate) {

		// check inputs
		if (!checkInputs())
			return;

		// get omission inputs
		boolean removeNegativeStresses = removeNegative_.isSelected();
		boolean applyOmission = omission_.isSelected();
		double omissionLevel = !applyOmission ? 0.0 : Double.parseDouble(omissionLevel_.getText());

		// stress modification inputs
		double modifierValue = modifierPane_.isDisabled() ? 1.0 : Double.parseDouble(modifier_.getText());
		String modifierMethod = null;
		for (MenuItem method : modifierMethod_.getItems()) {
			if (((RadioMenuItem) method).isSelected()) {
				modifierMethod = method.getText();
				break;
			}
		}

		// create equivalent stress input array
		ArrayList<EquivalentStressInput> inputs = new ArrayList<>();

		// create and add fatigue equivalent stress inputs
		ObservableList<FatigueMaterial> fatigueMaterials = fatigueAnalysis_.isSelected() ? fatigueMaterials_.getItems() : null;
		if (fatigueMaterials != null) {
			for (FatigueMaterial material : fatigueMaterials) {
				EquivalentStressInput input = new EquivalentStressInput(removeNegativeStresses, applyOmission, omissionLevel, material);
				input.setStressModifier(modifierValue, modifierMethod);
				inputs.add(input);
			}
		}

		// create and add preffas equivalent stress inputs
		ObservableList<PreffasMaterial> preffasMaterials = preffasAnalysis_.isSelected() ? preffasMaterials_.getItems() : null;
		if (preffasMaterials != null) {
			for (PreffasMaterial material : preffasMaterials) {
				EquivalentStressInput input = new EquivalentStressInput(removeNegativeStresses, applyOmission, omissionLevel, material);
				input.setStressModifier(modifierValue, modifierMethod);
				inputs.add(input);
			}
		}

		// create and add linear equivalent stress inputs
		ObservableList<LinearMaterial> linearMaterials = linearAnalysis_.isSelected() ? linearMaterials_.getItems() : null;
		if (linearMaterials != null) {
			for (LinearMaterial material : linearMaterials) {
				EquivalentStressInput input = new EquivalentStressInput(removeNegativeStresses, applyOmission, omissionLevel, material);
				input.setStressModifier(modifierValue, modifierMethod);
				inputs.add(input);
			}
		}

		// get analysis engine info
		AnalysisEngine engine = (AnalysisEngine) owner_.getOwner().getSettings().getValue(Settings.ANALYSIS_ENGINE);
		IsamiVersion isamiVersion = (IsamiVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_VERSION);
		IsamiSubVersion isamiSubVersion = (IsamiSubVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_SUB_VERSION);
		boolean applyCompression = (boolean) owner_.getOwner().getSettings().getValue(Settings.APPLY_COMPRESSION);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// loop over selected stress sequences
		for (TreeItem<String> item : owner_.getSelectedFiles()) {

			// loop over inputs
			for (EquivalentStressInput input : inputs) {

				// run now
				if (runNow) {
					tm.runTaskInParallel(new EquivalentStressAnalysis((SpectrumItem) item, input, engine).setIsamiEngineInputs(isamiVersion, isamiSubVersion, applyCompression));
				}

				// run on scheduled date
				else {
					tm.runTaskInParallel(new SaveTask(new EquivalentStressAnalysis((SpectrumItem) item, input, engine).setIsamiEngineInputs(isamiVersion, isamiSubVersion, applyCompression), scheduleDate));
				}
			}
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@Override
	public void setMaterials(Material[] materials) {
		if (materials[GetMaterials.FATIGUE_MATERIAL] != null) {
			fatigueMaterials_.getItems().add((FatigueMaterial) materials[GetMaterials.FATIGUE_MATERIAL]);
		}
		if (materials[GetMaterials.PREFFAS_MATERIAL] != null) {
			preffasMaterials_.getItems().add((PreffasMaterial) materials[GetMaterials.PREFFAS_MATERIAL]);
		}
		if (materials[GetMaterials.LINEAR_MATERIAL] != null) {
			linearMaterials_.getItems().add((LinearMaterial) materials[GetMaterials.LINEAR_MATERIAL]);
		}
	}

	/**
	 * Checks inputs.
	 *
	 * @return True if the inputs are valid.
	 */
	private boolean checkInputs() {

		// no analysis type selected
		if (!fatigueAnalysis_.isSelected() && !preffasAnalysis_.isSelected() && !linearAnalysis_.isSelected()) {
			String message = "No analysis type selected. Please select at least 1 analysis type to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(fatigueAnalysis_);
			return false;
		}

		// no fatigue material added
		if (fatigueAnalysis_.isSelected() && fatigueMaterials_.getItems().isEmpty()) {
			String message = "No fatigue material added. Please add at least 1 fatigue material to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(fatigueMaterials_);
			return false;
		}

		// no preffas material added
		if (preffasAnalysis_.isSelected() && preffasMaterials_.getItems().isEmpty()) {
			String message = "No Preffas material added. Please add at least 1 Preffas material to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(preffasMaterials_);
			return false;
		}

		// no linear material added
		if (linearAnalysis_.isSelected() && linearMaterials_.getItems().isEmpty()) {
			String message = "No linear propagation material added. Please add at least 1 linear propagation material to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(linearMaterials_);
			return false;
		}

		// check omission level
		if (omission_.isSelected()) {
			String message = omissionLevel_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(omissionLevel_);
				return false;
			}
		}

		// check stress modifier
		if (!modifierPane_.isDisabled()) {
			String message = modifier_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(modifier_);
				return false;
			}
		}

		// valid inputs
		return true;
	}

	@FXML
	private void onAddFatigueMaterialsClicked() {
		((FatigueMaterialsPopup) owner_.getPopup(InputPanel.FATIGUE_MATERIALS_POPUP)).show(this, ArrowLocation.LEFT_TOP, false);
	}

	@FXML
	private void onAddPreffasMaterialsClicked() {
		((PreffasMaterialsPopup) owner_.getPopup(InputPanel.PREFFAS_MATERIALS_POPUP)).show(this, ArrowLocation.LEFT_TOP, false);
	}

	@FXML
	private void onAddLinearMaterialsClicked() {
		((LinearMaterialsPopup) owner_.getPopup(InputPanel.LINEAR_MATERIALS_POPUP)).show(this, ArrowLocation.LEFT_TOP, false);
	}

	@FXML
	private void onRemoveFatigueMaterialsClicked() {
		fatigueMaterials_.getItems().removeAll(fatigueMaterials_.getSelectionModel().getSelectedItems());
	}

	@FXML
	private void onRemovePreffasMaterialsClicked() {
		preffasMaterials_.getItems().removeAll(preffasMaterials_.getSelectionModel().getSelectedItems());
	}

	@FXML
	private void onRemoveLinearMaterialsClicked() {
		linearMaterials_.getItems().removeAll(linearMaterials_.getSelectionModel().getSelectedItems());
	}

	@FXML
	private void onResetFatigueMaterialsClicked() {
		fatigueMaterials_.getItems().clear();
	}

	@FXML
	private void onResetPreffasMaterialsClicked() {
		preffasMaterials_.getItems().clear();
	}

	@FXML
	private void onResetLinearMaterialsClicked() {
		linearMaterials_.getItems().clear();
	}

	@FXML
	private void onResetClicked() {

		// reset analysis types and materials
		fatigueAnalysis_.setSelected(true);
		preffasAnalysis_.setSelected(false);
		linearAnalysis_.setSelected(false);

		// reset omission inputs
		removeNegative_.setSelected(false);
		omission_.setSelected(true);
		omissionLevel_.reset();

		// reset stress modifier
		modifier_.reset();
		modifierMethod_.setText(EquivalentStressInput.MULTIPLY);
		for (MenuItem item : modifierMethod_.getItems()) {
			RadioMenuItem radio = (RadioMenuItem) item;
			if (radio.getText().equals(EquivalentStressInput.MULTIPLY)) {
				radio.setSelected(true);
				break;
			}
		}

		// expand first pane
		accordion1_.setExpandedPane(accordion1_.getPanes().get(0));
		accordion2_.setExpandedPane(accordion2_.getPanes().get(0));

		// request material info
		fatigueMaterials_.getSelectionModel().clearSelection();
		preffasMaterials_.getSelectionModel().clearSelection();
		linearMaterials_.getSelectionModel().clearSelection();
		fatigueMaterials_.getItems().clear();
		preffasMaterials_.getItems().clear();
		linearMaterials_.getItems().clear();
		IsamiVersion isamiVersion = (IsamiVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_VERSION);
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetMaterials(owner_.getSelectedFiles(), this, isamiVersion));
	}

	@FXML
	private void onOKClicked() {
		setTaskScheduleDate(true, null);
	}

	@FXML
	private void onSaveTaskClicked() {
		setTaskScheduleDate(false, null);
	}

	@FXML
	private void onScheduleTaskClicked() {
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
		popOver.setDetachable(false);
		popOver.setContentNode(ScheduleTaskPanel.load(popOver, this, null));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(ok_);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to calculate equivalent stress", null);
	}

	@SuppressWarnings("static-method")
	@FXML
	private void onModifierMethodSelected(ActionEvent e) {
		RadioMenuItem item = (RadioMenuItem) e.getSource();
		MenuButton owner = (MenuButton) item.getParentPopup().getOwnerNode();
		owner.setText(item.getText());
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static EquivalentStressPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("EquivalentStressPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			EquivalentStressPanel controller = (EquivalentStressPanel) fxmlLoader.getController();

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
