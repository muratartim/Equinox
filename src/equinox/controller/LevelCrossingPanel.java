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
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleAnchor;

import control.validationField.IntegerValidationField;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.LevelCrossingInput;
import equinox.task.PlotLevelCrossing;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Class for level crossing panel controller.
 *
 * @author Murat Artim
 * @date Jul 21, 2014
 * @time 9:46:03 AM
 */
public class LevelCrossingPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Plot input. */
	private LevelCrossingInput input_ = null;

	@FXML
	private VBox root_, dsgContainer_, seriesContainer_;

	@FXML
	private ToggleSwitch normalize_, showInfo_, showMarkers_, showCrosshair_, includeSpectrumName_, includeSTFName_, includeEID_, includeSequenceName_, includeMaterialName_, includeOmissionLevel_, includeProgram_, includeSection_, includeMission_;

	@FXML
	private Button refresh_;

	@FXML
	private Accordion accordion_;

	@FXML
	private TitledPane namingPane_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set listeners
		normalize_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onNormalizeSelected(newValue));
		showInfo_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onShowInfoSelected(newValue));
		showMarkers_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onShowMarkersSelected(newValue));
		showCrosshair_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onShowCrosshairSelected(newValue));
		ToggleSwitch[] namingOptions = { includeSequenceName_, includeEID_, includeMaterialName_, includeOmissionLevel_, includeProgram_, includeSection_, includeMission_, includeSTFName_ };
		for (ToggleSwitch ts : namingOptions) {
			ts.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> setupRefreshButton());
		}

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
	public void showing() {

		// clear grid
		dsgContainer_.getChildren().clear();

		// get selected files
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();

		// loop over selected files
		for (TreeItem<String> item : selected) {
			if (item instanceof FatigueEquivalentStress) {
				addFatigueEquivalentStress((FatigueEquivalentStress) item);
			}
			else if (item instanceof PreffasEquivalentStress) {
				addPreffasEquivalentStress((PreffasEquivalentStress) item);
			}
			else if (item instanceof LinearEquivalentStress) {
				addLinearEquivalentStress((LinearEquivalentStress) item);
			}
		}

		// select normalize
		normalize_.setSelected(true);

		// plot
		onRefreshClicked();
	}

	@Override
	public String getHeader() {
		return "Plot Level Crossing";
	}

	/**
	 * Called when plotting has been completed.
	 *
	 */
	public void plottingCompleted() {

		// setup show/hide series components
		seriesContainer_.getChildren().clear();
		XYPlot plot = getChart().getXYPlot();
		XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
		for (int i = 0; i < dataset.getSeriesCount(); i++) {

			// create horizontal box
			HBox hBox = new HBox();
			hBox.setAlignment(Pos.CENTER_LEFT);
			hBox.setSpacing(5.0);
			hBox.setMaxWidth(Double.MAX_VALUE);

			// create toggle switch
			ToggleSwitch tSwitch = new ToggleSwitch();
			HBox.setHgrow(tSwitch, Priority.NEVER);
			tSwitch.setPrefWidth(35.0);
			tSwitch.setMinWidth(ToggleSwitch.USE_PREF_SIZE);
			tSwitch.setMaxWidth(ToggleSwitch.USE_PREF_SIZE);
			tSwitch.setUserData(i);
			tSwitch.setSelected(true);

			// create label
			Label label = new Label(dataset.getSeries(i).getKey().toString());
			HBox.setHgrow(label, Priority.ALWAYS);
			label.setMaxWidth(Double.MAX_VALUE);

			// add components to horizontal box
			hBox.getChildren().add(tSwitch);
			hBox.getChildren().add(label);

			// set listener to toggle switch
			tSwitch.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onShowSeriesSelected(newValue, (int) tSwitch.getUserData()));

			// add to container
			seriesContainer_.getChildren().add(hBox);
			onShowSeriesSelected(true, i);
		}

		// reset refresh button
		refresh_.setDisable(true);
	}

	/**
	 * Called when show series selected.
	 *
	 * @param visible
	 *            True if visible.
	 * @param seriesID
	 *            Series ID.
	 */
	private void onShowSeriesSelected(boolean visible, int seriesID) {
		XYPlot plot = getChart().getXYPlot();
		if (plot.getRenderer().isSeriesVisible(seriesID) != visible) {
			plot.getRenderer().setSeriesVisible(seriesID, visible);
		}
	}

	/**
	 * Called when show markers option is selected.
	 *
	 * @param isSelected
	 *            True if option is selected.
	 */
	private void onShowMarkersSelected(boolean isSelected) {
		JFreeChart chart = getChart();
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
		renderer.setBaseShapesVisible(isSelected);
	}

	/**
	 * Called when show crosshair option is selected.
	 *
	 * @param isSelected
	 *            True if option is selected.
	 */
	private void onShowCrosshairSelected(boolean isSelected) {
		XYPlot plot = getChart().getXYPlot();
		plot.setDomainCrosshairVisible(isSelected);
		plot.setRangeCrosshairVisible(isSelected);
	}

	/**
	 * Called when show info option is selected.
	 *
	 * @param isSelected
	 *            True if option is selected.
	 */
	private void onShowInfoSelected(boolean isSelected) {
		XYPlot plot = getChart().getXYPlot();
		if (!isSelected) {
			plot.clearAnnotations();
		}
		else {
			if (!plot.getAnnotations().isEmpty())
				return;
			LevelCrossingViewPanel panel = (LevelCrossingViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.LEVEL_CROSSING_VIEW);
			TextTitle info = panel.getInfo();
			XYTitleAnnotation ta = new XYTitleAnnotation(0.98, 0.02, info, RectangleAnchor.BOTTOM_RIGHT);
			ta.setMaxWidth(0.48);
			plot.addAnnotation(ta);
		}
	}

	@FXML
	private void onResetClicked() {

		// reset DSG panel
		normalize_.setSelected(true);
		for (Node node : dsgContainer_.getChildren()) {
			node.setDisable(true);
			((IntegerValidationField) node).reset();
		}

		// reset naming panel
		includeSpectrumName_.setSelected(false);
		includeSTFName_.setSelected(true);
		includeEID_.setSelected(false);
		includeSequenceName_.setSelected(false);
		includeMaterialName_.setSelected(true);
		includeOmissionLevel_.setSelected(false);
		includeProgram_.setSelected(false);
		includeSection_.setSelected(false);
		includeMission_.setSelected(false);

		// reset series panel
		for (Node node : seriesContainer_.getChildren()) {
			HBox hBox = (HBox) node;
			ToggleSwitch ts = (ToggleSwitch) hBox.getChildren().get(0);
			ts.setSelected(true);
		}

		// reset options
		showInfo_.setSelected(true);
		showMarkers_.setSelected(false);
		showCrosshair_.setSelected(true);
		setupRefreshButton();
	}

	@FXML
	private void onRefreshClicked() {

		// check inputs
		if (!checkInputs())
			return;

		// get normalize
		boolean isNormalize = normalize_.isSelected();

		// get DSGs
		ArrayList<Integer> dsgs = new ArrayList<>();
		for (Node node : dsgContainer_.getChildren()) {
			IntegerValidationField tf = (IntegerValidationField) node;
			if (!isNormalize) {
				dsgs.add(Integer.parseInt(tf.getText()));
			}
		}

		// create input
		input_ = new LevelCrossingInput(isNormalize, dsgs);

		// set naming parameters
		input_.setIncludeSpectrumName(includeSpectrumName_.isSelected());
		input_.setIncludeSTFName(includeSTFName_.isSelected());
		input_.setIncludeEID(includeEID_.isSelected());
		input_.setIncludeSequenceName(includeSequenceName_.isSelected());
		input_.setIncludeMaterialName(includeMaterialName_.isSelected());
		input_.setIncludeOmissionLevel(includeOmissionLevel_.isSelected());
		input_.setIncludeProgram(includeProgram_.isSelected());
		input_.setIncludeSection(includeSection_.isSelected());
		input_.setIncludeMission(includeMission_.isSelected());

		// create task
		PlotLevelCrossing task = new PlotLevelCrossing(input_);

		// get equivalent stresses
		for (Node node : dsgContainer_.getChildren()) {
			IntegerValidationField tf = (IntegerValidationField) node;
			task.addEquivalentStress((SpectrumItem) tf.getUserData());
		}

		// start comparison task
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(task);
	}

	/**
	 * Checks inputs.
	 *
	 * @return True if inputs are valid.
	 */
	private boolean checkInputs() {

		// no rainflow available
		if (dsgContainer_.getChildren().isEmpty())
			return false;

		// check DSGs
		if (!normalize_.isSelected()) {
			for (Node node : dsgContainer_.getChildren()) {
				IntegerValidationField dsg = (IntegerValidationField) node;
				String message = dsg.validate();
				if (message != null) {
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(dsg);
					return false;
				}
			}
		}

		// get naming parameters
		boolean includeSpectrumName = includeSpectrumName_.isSelected();
		boolean includeSTFName = includeSTFName_.isSelected();
		boolean includeEID = includeEID_.isSelected();
		boolean includeSequenceName = includeSequenceName_.isSelected();
		boolean includeMaterialName = includeMaterialName_.isSelected();
		boolean includeOmissionLevel = includeOmissionLevel_.isSelected();
		boolean includeProgram = includeProgram_.isSelected();
		boolean includeSection = includeSection_.isSelected();
		boolean includeMission = includeMission_.isSelected();

		// no naming selected
		if (!includeSpectrumName && !includeSTFName && !includeEID && !includeSequenceName && !includeMaterialName && !includeOmissionLevel && !includeProgram && !includeSection && !includeMission) {
			String message = "Please select at least 1 naming for plot series.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(namingPane_);
			return false;
		}

		// valid inputs
		return true;
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to plot level crossing", null);
	}

	/**
	 * Called when normalize option is selected.
	 *
	 * @param isSelected
	 *            True if the option is selected.
	 */
	private void onNormalizeSelected(boolean isSelected) {
		for (Node node : dsgContainer_.getChildren()) {
			node.setDisable(isSelected);
			if (isSelected) {
				((IntegerValidationField) node).reset();
			}
		}
		setupRefreshButton();
	}

	@FXML
	private void setupRefreshButton() {

		// no input
		if (input_ == null)
			return;

		// normalize
		if (normalize_.isSelected() != input_.isNormalize()) {
			refresh_.setDisable(false);
			return;
		}

		// DSG
		for (int i = 0; i < dsgContainer_.getChildren().size(); i++) {
			IntegerValidationField tf = (IntegerValidationField) dsgContainer_.getChildren().get(i);
			String dsg = tf.getText();
			if (dsg == null || dsg.isEmpty()) {
				if (input_.getDsgs().get(i) != 0) {
					refresh_.setDisable(false);
					return;
				}
			}
			else if (Integer.parseInt(dsg) != input_.getDsgs().get(i)) {
				refresh_.setDisable(false);
				return;
			}
		}

		// get naming parameters
		boolean includeSpectrumName = includeSpectrumName_.isSelected();
		boolean includeSTFName = includeSTFName_.isSelected();
		boolean includeEID = includeEID_.isSelected();
		boolean includeSequenceName = includeSequenceName_.isSelected();
		boolean includeMaterialName = includeMaterialName_.isSelected();
		boolean includeOmissionLevel = includeOmissionLevel_.isSelected();
		boolean includeProgram = includeProgram_.isSelected();
		boolean includeSection = includeSection_.isSelected();
		boolean includeMission = includeMission_.isSelected();

		// series naming
		if (includeSpectrumName != input_.getIncludeSpectrumName() || includeSTFName != input_.getIncludeSTFName() || includeEID != input_.getIncludeEID() || includeSequenceName != input_.getIncludeSequenceName() || includeMaterialName != input_.getIncludeMaterialName()
				|| includeOmissionLevel != input_.getIncludeOmissionLevel() || includeProgram != input_.getIncludeProgram() || includeSection != input_.getIncludeSection() || includeMission != input_.getIncludeMission()) {
			refresh_.setDisable(false);
			return;
		}

		// no change
		refresh_.setDisable(true);
	}

	/**
	 * Adds given fatigue equivalent stress to inputs.
	 *
	 * @param eqStress
	 *            Fatigue equivalent stress to add.
	 */
	private void addFatigueEquivalentStress(FatigueEquivalentStress eqStress) {
		IntegerValidationField tf = new IntegerValidationField();
		tf.setPromptText("DSG for " + getFatigueEquivalentStressName(eqStress));
		tf.setUserData(eqStress);
		VBox.setVgrow(tf, Priority.NEVER);
		tf.setMaxWidth(Double.MAX_VALUE);
		tf.setDefaultValue(null);
		tf.setMinimumValue(1, true);
		tf.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> setupRefreshButton());
		tf.setDisable(true);
		dsgContainer_.getChildren().add(tf);
	}

	/**
	 * Adds given preffas equivalent stress to inputs.
	 *
	 * @param eqStress
	 *            Preffas equivalent stress to add.
	 */
	private void addPreffasEquivalentStress(PreffasEquivalentStress eqStress) {
		IntegerValidationField tf = new IntegerValidationField();
		tf.setPromptText("DSG for " + getPreffasEquivalentStressName(eqStress));
		tf.setUserData(eqStress);
		VBox.setVgrow(tf, Priority.NEVER);
		tf.setMaxWidth(Double.MAX_VALUE);
		tf.setDefaultValue(null);
		tf.setMinimumValue(1, true);
		tf.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> setupRefreshButton());
		tf.setDisable(true);
		dsgContainer_.getChildren().add(tf);
	}

	/**
	 * Adds given linear equivalent stress to inputs.
	 *
	 * @param eqStress
	 *            Linear equivalent stress to add.
	 */
	private void addLinearEquivalentStress(LinearEquivalentStress eqStress) {
		IntegerValidationField tf = new IntegerValidationField();
		tf.setPromptText("DSG for " + getLinearEquivalentStressName(eqStress));
		tf.setUserData(eqStress);
		VBox.setVgrow(tf, Priority.NEVER);
		tf.setMaxWidth(Double.MAX_VALUE);
		tf.setDefaultValue(null);
		tf.setMinimumValue(1, true);
		tf.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> setupRefreshButton());
		tf.setDisable(true);
		dsgContainer_.getChildren().add(tf);
	}

	/**
	 * Returns the level crossing chart.
	 *
	 * @return The level crossing chart.
	 */
	private JFreeChart getChart() {
		LevelCrossingViewPanel panel = (LevelCrossingViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.LEVEL_CROSSING_VIEW);
		return panel.getChart();
	}

	/**
	 * Returns fatigue equivalent stress name.
	 *
	 * @param eqStress
	 *            Fatigue equivalent stress.
	 * @return The name of fatigue equivalent stress.
	 */
	private static String getFatigueEquivalentStressName(FatigueEquivalentStress eqStress) {
		Spectrum cdfSet = eqStress.getParentItem().getParentItem().getParentItem();
		String name = cdfSet.getProgram();
		name += ", " + cdfSet.getSection();
		name += ", " + eqStress.getParentItem().getParentItem().getMission();
		name += ", " + eqStress.getParentItem().getName();
		name += ", OL: " + eqStress.getOmissionLevel();
		return name;
	}

	/**
	 * Returns preffas equivalent stress name.
	 *
	 * @param eqStress
	 *            Preffas equivalent stress.
	 * @return The name of preffas equivalent stress.
	 */
	private static String getPreffasEquivalentStressName(PreffasEquivalentStress eqStress) {
		Spectrum cdfSet = eqStress.getParentItem().getParentItem().getParentItem();
		String name = cdfSet.getProgram();
		name += ", " + cdfSet.getSection();
		name += ", " + eqStress.getParentItem().getParentItem().getMission();
		name += ", " + eqStress.getParentItem().getName();
		name += ", OL: " + eqStress.getOmissionLevel();
		return name;
	}

	/**
	 * Returns linear equivalent stress name.
	 *
	 * @param eqStress
	 *            Linear equivalent stress.
	 * @return The name of linear equivalent stress.
	 */
	private static String getLinearEquivalentStressName(LinearEquivalentStress eqStress) {
		Spectrum cdfSet = eqStress.getParentItem().getParentItem().getParentItem();
		String name = cdfSet.getProgram();
		name += ", " + cdfSet.getSection();
		name += ", " + eqStress.getParentItem().getParentItem().getMission();
		name += ", " + eqStress.getParentItem().getName();
		name += ", OL: " + eqStress.getOmissionLevel();
		return name;
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static LevelCrossingPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("LevelCrossingPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			LevelCrossingPanel controller = (LevelCrossingPanel) fxmlLoader.getController();

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
