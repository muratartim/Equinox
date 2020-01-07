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
import java.util.logging.Level;

import org.controlsfx.control.ToggleSwitch;

import equinox.Equinox;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.DamageAngle;
import equinox.task.PlotDamageAngles;
import equinox.task.PlotDamageAngles.ResultOrdering;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

/**
 * Class for plot damage angle panel.
 *
 * @author Murat Artim
 * @date Aug 11, 2014
 * @time 3:57:01 PM
 */
public class PlotDamageAnglePanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ComboBox<ResultOrdering> order_;

	@FXML
	private ToggleSwitch dataLabels_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set results order
		order_.setButtonCell(new ResultOrderingListCell());
		order_.setCellFactory(p -> new ResultOrderingListCell());
		order_.getItems().setAll(ResultOrdering.values());
		order_.getSelectionModel().select(0);

		// set listeners
		dataLabels_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
			StatisticsViewPanel panel = (StatisticsViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);
			panel.setLabelsVisible(newValue);
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
		plot();
	}

	@Override
	public String getHeader() {
		return "Plot Damage Angles";
	}

	@FXML
	private void onResetClicked() {
		order_.getSelectionModel().select(0);
		dataLabels_.setSelected(true);
	}

	@FXML
	private void onOkClicked() {
		plot();
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to plot damage angles", null);
	}

	/**
	 * Plots damage angles.
	 *
	 */
	private void plot() {

		try {

			// get selected damage angles
			ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();
			DamageAngle[] angles = new DamageAngle[selected.size()];
			for (int i = 0; i < selected.size(); i++) {
				angles[i] = (DamageAngle) selected.get(i);
			}

			// set inputs
			boolean showLabels = dataLabels_.isSelected();
			ResultOrdering order = order_.getSelectionModel().getSelectedItem();

			// create and start statistics task
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new PlotDamageAngles(angles, order, showLabels));
		}

		// exception occurred
		catch (Exception e) {

			// log exception
			Equinox.LOGGER.log(Level.WARNING, "Exception occured during setting damage angle plot inputs.", e);

			// create and show notification
			String message = "Exception occurred during setting damage angle plot inputs: " + e.getLocalizedMessage();
			message += " Click 'Details' for more information.";
			owner_.getOwner().getNotificationPane().showError("Problem encountered", message, e);
		}
	}

	/**
	 * Loads and returns spectrum statistics panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded spectrum statistics panel.
	 */
	public static PlotDamageAnglePanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("PlotDamageAnglePanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			PlotDamageAnglePanel controller = (PlotDamageAnglePanel) fxmlLoader.getController();

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

	/**
	 * Inner class for comparison criteria list cell.
	 *
	 * @author Murat Artim
	 * @date 2 Sep 2018
	 * @time 16:38:10
	 */
	private class ResultOrderingListCell extends ListCell<ResultOrdering> {

		@Override
		protected void updateItem(ResultOrdering item, boolean empty) {
			super.updateItem(item, empty);
			if (!empty && item != null) {
				setText(item.getName());
			}
			else {
				setText(null);
			}
		}
	}
}
