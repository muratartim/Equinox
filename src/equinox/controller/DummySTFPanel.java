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
import org.controlsfx.control.ToggleSwitch;

import control.validationField.DoubleValidationField;
import control.validationField.IntegerValidationField;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.Spectrum;
import equinox.task.CreateDummySTFFile;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

/**
 * Class for dummy STF file panel.
 *
 * @author Murat Artim
 * @date May 20, 2014
 * @time 5:52:00 PM
 */
public class DummySTFPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextField fileName_;

	@FXML
	private ChoiceBox<String> stressState_;

	@FXML
	private Accordion accordion_;

	@FXML
	private Button ok_;

	@FXML
	private ToggleSwitch dtInf_, dtSup_;

	@FXML
	private IntegerValidationField dpLC_, dtInfLC_, dtSupLC_;

	@FXML
	private DoubleValidationField oneGX_, oneGY_, oneGXY_, incrementX_, incrementY_, incrementXY_, deltaPX_, deltaPY_, deltaPXY_, dtInfX_, dtInfY_, dtInfXY_, dtSupX_, dtSupY_, dtSupXY_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// add stress states
		stressState_.getItems().clear();
		stressState_.getItems().add("1D stress state");
		stressState_.getItems().add("2D stress state");
		stressState_.getSelectionModel().select(0);

		// set listener to DT sup
		dtSup_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onDTSupClicked());

		// set listener to DT sup
		dtInf_.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> onDTInfClicked());

		// add listener
		stressState_.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
			boolean is1D = newValue.equals("1D stress state");
			oneGY_.setDisable(is1D);
			oneGXY_.setDisable(is1D);
			incrementY_.setDisable(is1D);
			incrementXY_.setDisable(is1D);
			deltaPY_.setDisable(is1D);
			deltaPXY_.setDisable(is1D);
			if (dtSup_.isSelected()) {
				dtSupY_.setDisable(is1D);
				dtSupXY_.setDisable(is1D);
			}
			if (dtInf_.isSelected()) {
				dtInfY_.setDisable(is1D);
				dtInfXY_.setDisable(is1D);
			}
		});

		// add listeners to stress factor fields
		DoubleValidationField[] fields = { oneGX_, oneGY_, oneGXY_, incrementX_, incrementY_, incrementXY_, deltaPX_, deltaPY_, deltaPXY_, dtInfX_, dtInfY_, dtInfXY_, dtSupX_, dtSupY_, dtSupXY_ };
		for (DoubleValidationField tf : fields) {
			tf.setDefaultValue(10.0);
		}
		dpLC_.setAsOptionalInput(true);
		dpLC_.setDefaultValue(null);
		dtInfLC_.setDefaultValue(null);
		dtSupLC_.setDefaultValue(null);

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
		onResetClicked();
	}

	@Override
	public String getHeader() {
		return "Dummy Stress Input File";
	}

	/**
	 * Called when DT sup clicked.
	 */
	private void onDTSupClicked() {
		boolean selected = dtSup_.isSelected();
		dtSupLC_.setDisable(!selected);
		dtSupX_.setDisable(!selected);
		boolean is1D = stressState_.getSelectionModel().getSelectedItem().equals("1D stress state");
		if (!is1D) {
			dtSupY_.setDisable(!selected);
			dtSupXY_.setDisable(!selected);
		}
		dtInf_.setDisable(!selected);
		if (!selected) {
			dtSupLC_.reset();
			dtSupX_.reset();
			if (!is1D) {
				dtSupY_.reset();
				dtSupXY_.reset();
			}
			dtInf_.setSelected(false);
		}
	}

	/**
	 * Called when DT inf clicked.
	 */
	private void onDTInfClicked() {
		boolean selected = dtInf_.isSelected();
		dtInfLC_.setDisable(!selected);
		dtInfX_.setDisable(!selected);
		boolean is1D = stressState_.getSelectionModel().getSelectedItem().equals("1D stress state");
		if (!is1D) {
			dtInfY_.setDisable(!selected);
			dtInfXY_.setDisable(!selected);
		}
		if (!selected) {
			dtInfLC_.reset();
			dtInfX_.reset();
			if (!is1D) {
				dtInfY_.reset();
				dtInfXY_.reset();
			}
		}
	}

	@FXML
	private void onResetClicked() {
		fileName_.clear();
		dpLC_.reset();
		DoubleValidationField[] fields = { oneGX_, oneGY_, oneGXY_, incrementX_, incrementY_, incrementXY_, deltaPX_, deltaPY_, deltaPXY_ };
		for (DoubleValidationField tf : fields) {
			tf.reset();
		}
		stressState_.getSelectionModel().select(0);
		dtSup_.setSelected(false);
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@FXML
	private void onOKClicked() {

		// get selected CDF sets
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();

		// check inputs
		if (!checkInputs(selected))
			return;

		// get inputs
		String fileName = fileName_.getText();
		boolean is1D = stressState_.getSelectionModel().getSelectedIndex() == 0;
		double oneGX = Double.parseDouble(oneGX_.getText());
		double oneGY = Double.parseDouble(oneGY_.getText());
		double oneGXY = Double.parseDouble(oneGXY_.getText());
		double incrementX = Double.parseDouble(incrementX_.getText());
		double incrementY = Double.parseDouble(incrementY_.getText());
		double incrementXY = Double.parseDouble(incrementXY_.getText());
		String dpLC = dpLC_.getText().isEmpty() ? null : dpLC_.getText();
		double deltaPX = Double.parseDouble(deltaPX_.getText());
		double deltaPY = Double.parseDouble(deltaPY_.getText());
		double deltaPXY = Double.parseDouble(deltaPXY_.getText());
		String dtSupLC = dtSup_.isSelected() ? dtSupLC_.getText() : null;
		double dtSupX = dtSup_.isSelected() ? Double.parseDouble(dtSupX_.getText()) : 0.0;
		double dtSupY = dtSup_.isSelected() ? Double.parseDouble(dtSupY_.getText()) : 0.0;
		double dtSupXY = dtSup_.isSelected() ? Double.parseDouble(dtSupXY_.getText()) : 0.0;
		String dtInfLC = dtSup_.isSelected() && dtInf_.isSelected() ? dtInfLC_.getText() : null;
		double dtInfX = dtSup_.isSelected() && dtInf_.isSelected() ? Double.parseDouble(dtInfX_.getText()) : 0.0;
		double dtInfY = dtSup_.isSelected() && dtInf_.isSelected() ? Double.parseDouble(dtInfY_.getText()) : 0.0;
		double dtInfXY = dtSup_.isSelected() && dtInf_.isSelected() ? Double.parseDouble(dtInfXY_.getText()) : 0.0;

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// loop over CDF sets
		for (TreeItem<String> item : selected) {

			// create task
			CreateDummySTFFile task = new CreateDummySTFFile((Spectrum) item, fileName, is1D);

			// set 1g and increment stresses
			task.setOneGStresses(oneGX, oneGY, oneGXY);
			task.setIncrementStresses(incrementX, incrementY, incrementXY);

			// set delta-p stresses
			task.setDeltaPStresses(dpLC, deltaPX, deltaPY, deltaPXY);

			// set delta-t stresses
			if (dtSup_.isSelected()) {
				task.setDeltaTSupStresses(dtSupLC, dtSupX, dtSupY, dtSupXY);
				if (dtInf_.isSelected()) {
					task.setDeltaTInfStresses(dtInfLC, dtInfX, dtInfY, dtInfXY);
				}
			}

			// add task
			tm.runTaskInParallel(task);
		}

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	/**
	 * Checks inputs.
	 *
	 * @param selected
	 *            Selected files.
	 * @return True if inputs are valid.
	 */
	private boolean checkInputs(ObservableList<TreeItem<String>> selected) {

		// no file name given
		String fileName = fileName_.getText();
		if (fileName == null || fileName.isEmpty()) {
			String message = "No file name entered. Please enter name for the dummy STF file.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(fileName_);
			return false;
		}

		// get selected stress state
		boolean is1D = stressState_.getSelectionModel().getSelectedIndex() == 0;

		// check 1G x
		String message = oneGX_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(oneGX_);
			return false;
		}

		// check 1G y
		message = oneGY_.validate();
		if (!is1D & message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(oneGY_);
			return false;
		}

		// check 1G xy
		message = oneGXY_.validate();
		if (!is1D & message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(oneGXY_);
			return false;
		}

		// check increment x
		message = incrementX_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(incrementX_);
			return false;
		}

		// check increment y
		message = incrementY_.validate();
		if (!is1D & message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(incrementY_);
			return false;
		}

		// check increment xy
		message = incrementXY_.validate();
		if (!is1D & message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(incrementXY_);
			return false;
		}

		// check delta-p load case
		message = dpLC_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(dpLC_);
			return false;
		}

		// check delta-p x
		message = deltaPX_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(deltaPX_);
			return false;
		}

		// check delta-p y
		message = deltaPY_.validate();
		if (!is1D & message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(deltaPY_);
			return false;
		}

		// check delta-p xy
		message = deltaPXY_.validate();
		if (!is1D & message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(deltaPXY_);
			return false;
		}

		// delta-t superior selected
		if (dtSup_.isSelected()) {

			// check delta-t superior load case
			message = dtSupLC_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(dtSupLC_);
				return false;
			}

			// check delta-t superior x
			message = dtSupX_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(dtSupX_);
				return false;
			}

			// check delta-t superior y
			message = dtSupY_.validate();
			if (!is1D & message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(dtSupY_);
				return false;
			}

			// check delta-t superior xy
			message = dtSupXY_.validate();
			if (!is1D & message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(dtSupXY_);
				return false;
			}
		}

		// delta-t inferior selected
		if (dtInf_.isSelected()) {

			// check delta-t inferior load case
			message = dtInfLC_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(dtInfLC_);
				return false;
			}

			// check delta-t inferior x
			message = dtInfX_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(dtInfX_);
				return false;
			}

			// check delta-t inferior y
			message = dtInfY_.validate();
			if (!is1D & message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(dtInfY_);
				return false;
			}

			// check delta-t inferior xy
			message = dtInfXY_.validate();
			if (!is1D & message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(dtInfXY_);
				return false;
			}
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
		owner_.getOwner().showHelp("How to create dummy STF files", null);
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static DummySTFPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DummySTFPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			DummySTFPanel controller = (DummySTFPanel) fxmlLoader.getController();

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
