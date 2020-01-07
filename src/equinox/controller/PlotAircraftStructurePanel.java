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
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.ElementType;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftModel;
import equinox.data.input.PlotStructureInput;
import equinox.task.GetElementGroups;
import equinox.task.GetElementGroups.ElementGroupsRequestingPanel;
import equinox.task.PlotAircraftStructure;
import equinox.utility.SpinnerListener;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Class for plot A/C structure panel controller.
 *
 * @author Murat Artim
 * @date Jul 10, 2015
 * @time 10:49:20 AM
 */
public class PlotAircraftStructurePanel implements InternalInputSubPanel, ElementGroupsRequestingPanel {

	/** Section colors. */
	private static final Color[] TYPE_COLORS = { new Color(70 / 255.0, 130 / 255.0, 180 / 255.0, 1), new Color(230 / 255.0, 230 / 255.0, 77 / 255.0, 1), new Color(255 / 255.0, 69 / 255.0, 0, 1), new Color(0, 128 / 255.0, 0, 1) };

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ToggleSwitch quad_, tria_, beam_, rod_, shear_, quadOutlines_, triaOutlines_, beamOutlines_, rodOutlines_, shearOutlines_;

	@FXML
	private ColorPicker quadColor_, triaColor_, beamColor_, rodColor_, shearColor_;

	@FXML
	private Spinner<Integer> quadOpacity_, triaOpacity_, beamOpacity_, rodOpacity_, shearOpacity_, beamExtrusion_, rodExtrusion_;

	@FXML
	private TitledPane typesPane_;

	@FXML
	private ListView<String> groups_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup opacities
		quadOpacity_.setValueFactory(new IntegerSpinnerValueFactory(0, 100, 100));
		quadOpacity_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(quadOpacity_));
		triaOpacity_.setValueFactory(new IntegerSpinnerValueFactory(0, 100, 100));
		triaOpacity_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(triaOpacity_));
		beamOpacity_.setValueFactory(new IntegerSpinnerValueFactory(0, 100, 100));
		beamOpacity_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(beamOpacity_));
		rodOpacity_.setValueFactory(new IntegerSpinnerValueFactory(0, 100, 100));
		rodOpacity_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(rodOpacity_));
		shearOpacity_.setValueFactory(new IntegerSpinnerValueFactory(0, 100, 100));
		shearOpacity_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(shearOpacity_));

		// bind element type inputs
		quadOpacity_.disableProperty().bind(quad_.selectedProperty().not());
		triaOpacity_.disableProperty().bind(tria_.selectedProperty().not());
		beamOpacity_.disableProperty().bind(beam_.selectedProperty().not());
		rodOpacity_.disableProperty().bind(rod_.selectedProperty().not());
		shearOpacity_.disableProperty().bind(shear_.selectedProperty().not());
		quadOutlines_.disableProperty().bind(quad_.selectedProperty().not());
		triaOutlines_.disableProperty().bind(tria_.selectedProperty().not());
		beamOutlines_.disableProperty().bind(beam_.selectedProperty().not());
		rodOutlines_.disableProperty().bind(rod_.selectedProperty().not());
		shearOutlines_.disableProperty().bind(shear_.selectedProperty().not());
		quadColor_.disableProperty().bind(quad_.selectedProperty().not());
		triaColor_.disableProperty().bind(tria_.selectedProperty().not());
		beamColor_.disableProperty().bind(beam_.selectedProperty().not());
		rodColor_.disableProperty().bind(rod_.selectedProperty().not());
		shearColor_.disableProperty().bind(shear_.selectedProperty().not());

		// set default colors to element types
		quadColor_.setValue(TYPE_COLORS[0]);
		triaColor_.setValue(TYPE_COLORS[0]);
		beamColor_.setValue(TYPE_COLORS[1]);
		rodColor_.setValue(TYPE_COLORS[2]);
		shearColor_.setValue(TYPE_COLORS[3]);

		// set multiple selection for lists
		groups_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// setup settings
		beamExtrusion_.setValueFactory(new IntegerSpinnerValueFactory(10, 100, 50));
		beamExtrusion_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(beamExtrusion_));
		rodExtrusion_.setValueFactory(new IntegerSpinnerValueFactory(10, 100, 20));
		rodExtrusion_.editorProperty().getValue().textProperty().addListener(new SpinnerListener(rodExtrusion_));

		// expand first panel
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
	public String getHeader() {
		return "Plot A/C Structure";
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {

		// get selected A/C model
		AircraftModel selected = (AircraftModel) owner_.getSelectedFiles().get(0);

		// get element groups and positions
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetElementGroups(this, selected));
	}

	@Override
	public void setElementGroups(ArrayList<String> groups) {
		groups_.getItems().setAll(groups);
	}

	@FXML
	private void onOkClicked() {

		// get selected A/C model
		AircraftModel selected = (AircraftModel) owner_.getSelectedFiles().get(0);

		// nothing selected
		if (selected == null)
			return;

		// no element type selected
		if (!quad_.isSelected() && !tria_.isSelected() && !beam_.isSelected() && !rod_.isSelected() && !shear_.isSelected()) {
			String message = "Please select at least 1 element type.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(typesPane_);
			return;
		}

		// create plot input
		PlotStructureInput input = new PlotStructureInput(selected);

		// set element types
		ElementType quad = new ElementType(ElementType.QUAD);
		quad.setColor(quadColor_.getValue());
		quad.setOpacity(quadOpacity_.getValue());
		quad.setOutlines(quadOutlines_.isSelected());
		quad.setSelected(quad_.isSelected());
		input.addType(quad);
		ElementType tria = new ElementType(ElementType.TRIA);
		tria.setColor(triaColor_.getValue());
		tria.setOpacity(triaOpacity_.getValue());
		tria.setOutlines(triaOutlines_.isSelected());
		tria.setSelected(tria_.isSelected());
		input.addType(tria);
		ElementType beam = new ElementType(ElementType.BEAM);
		beam.setColor(beamColor_.getValue());
		beam.setOpacity(beamOpacity_.getValue());
		beam.setOutlines(beamOutlines_.isSelected());
		beam.setSelected(beam_.isSelected());
		input.addType(beam);
		ElementType rod = new ElementType(ElementType.ROD);
		rod.setColor(rodColor_.getValue());
		rod.setOpacity(rodOpacity_.getValue());
		rod.setOutlines(rodOutlines_.isSelected());
		rod.setSelected(rod_.isSelected());
		input.addType(rod);
		ElementType shear = new ElementType(ElementType.SHEAR);
		shear.setColor(shearColor_.getValue());
		shear.setOpacity(shearOpacity_.getValue());
		shear.setOutlines(shearOutlines_.isSelected());
		shear.setSelected(shear_.isSelected());
		input.addType(shear);

		// set element group inputs
		for (String group : groups_.getSelectionModel().getSelectedItems()) {
			input.addGroup(group);
		}

		// set 1D element extrusion widths
		input.setBeamExtrusionWidth(beamExtrusion_.getValue());
		input.setRodExtrusionWidth(rodExtrusion_.getValue());

		// create and start statistics task
		owner_.getOwner().getActiveTasksPanel().runTaskSequentially(new PlotAircraftStructure(input));
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {

		// reset element group and position selections
		groups_.getSelectionModel().clearSelection();

		// reset element type options
		quad_.setSelected(true);
		tria_.setSelected(true);
		beam_.setSelected(false);
		rod_.setSelected(false);
		shear_.setSelected(false);
		quadOpacity_.getValueFactory().setValue(100);
		triaOpacity_.getValueFactory().setValue(100);
		beamOpacity_.getValueFactory().setValue(100);
		rodOpacity_.getValueFactory().setValue(100);
		shearOpacity_.getValueFactory().setValue(100);
		quadOutlines_.setSelected(false);
		triaOutlines_.setSelected(false);
		beamOutlines_.setSelected(false);
		rodOutlines_.setSelected(false);
		shearOutlines_.setSelected(false);
		quadColor_.setValue(TYPE_COLORS[0]);
		triaColor_.setValue(TYPE_COLORS[0]);
		beamColor_.setValue(TYPE_COLORS[1]);
		rodColor_.setValue(TYPE_COLORS[2]);
		shearColor_.setValue(TYPE_COLORS[3]);

		// reset settings
		beamExtrusion_.getValueFactory().setValue(50);
		rodExtrusion_.getValueFactory().setValue(20);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to plot aircraft structure", null);
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static PlotAircraftStructurePanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("PlotAircraftStructurePanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			PlotAircraftStructurePanel controller = (PlotAircraftStructurePanel) fxmlLoader.getController();

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
