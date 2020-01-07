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
import equinox.data.EquinoxTheme;
import equinox.data.fileType.DamageAngle;
import equinox.task.CompareDamageAngleLifeFactors;
import equinox.task.CompareDamageAngleLifeFactors.ResultOrdering;
import equinox.task.GetDamageAngles;
import equinox.task.GetDamageAngles.DamageAngleRequestingPanel;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

/**
 * Class for compare damage angle life factors panel controller.
 *
 * @author Murat Artim
 * @date Oct 14, 2014
 * @time 10:07:44 AM
 */
public class CompareDamageAngleLifeFactorsPanel implements InternalInputSubPanel, DamageAngleRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ComboBox<String> basisAngle_;

	@FXML
	private ChoiceBox<ResultOrdering> order_;

	@FXML
	private ToggleSwitch dataLabels_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set results order
		order_.setItems(FXCollections.observableArrayList(ResultOrdering.values()));
		order_.getSelectionModel().select(0);

		// set listeners
		dataLabels_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				StatisticsViewPanel panel = (StatisticsViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);
				panel.setLabelsVisible(newValue);
			}
		});
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

		// reset basis list
		basisAngle_.setValue(null);
		basisAngle_.getSelectionModel().clearSelection();
		basisAngle_.getItems().clear();

		// get angles
		DamageAngle angle = (DamageAngle) owner_.getSelectedFiles().get(0);
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetDamageAngles(angle, this));
	}

	@Override
	public String getHeader() {
		return "Compare Damage Angles";
	}

	@Override
	public void setDamageAngles(ArrayList<String> angles) {
		basisAngle_.getItems().setAll(angles);
	}

	@FXML
	private void onResetClicked() {
		basisAngle_.setValue(null);
		basisAngle_.getSelectionModel().clearSelection();
		order_.getSelectionModel().select(0);
		if (!dataLabels_.isSelected()) {
			dataLabels_.setSelected(true);
		}
	}

	@FXML
	private void onOkClicked() {

		// get basis damage angle
		String basis = basisAngle_.getSelectionModel().getSelectedItem();
		if (basis == null) {
			String message = "Please select basis damage angle for comparison.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(basisAngle_);
			return;
		}

		// get selected rainflow cycles
		DamageAngle angle = (DamageAngle) owner_.getSelectedFiles().get(0);

		// set inputs
		boolean showLabels = dataLabels_.isSelected();
		ResultOrdering order = order_.getSelectionModel().getSelectedItem();

		// create and start statistics task
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new CompareDamageAngleLifeFactors(angle, Double.parseDouble(basis), order, showLabels));
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to compare damage angles", null);
	}

	/**
	 * Loads and returns spectrum statistics panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded spectrum statistics panel.
	 */
	public static CompareDamageAngleLifeFactorsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CompareDamageAngleLifeFactorsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CompareDamageAngleLifeFactorsPanel controller = (CompareDamageAngleLifeFactorsPanel) fxmlLoader.getController();

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
