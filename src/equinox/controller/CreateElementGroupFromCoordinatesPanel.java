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
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftModel;
import equinox.task.CreateElementGroupFromCoordinates;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Class for create element group from coordinates panel controller.
 *
 * @author Murat Artim
 * @date Jul 31, 2015
 * @time 11:13:36 AM
 */
public class CreateElementGroupFromCoordinatesPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextField name_;

	@FXML
	private DoubleValidationField xMin_, xMax_, yMin_, yMax_, zMin_, zMax_;

	@FXML
	private ToggleSwitch x_, y_, z_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// bind checkboxes
		xMin_.disableProperty().bind(x_.selectedProperty().not());
		xMax_.disableProperty().bind(x_.selectedProperty().not());
		yMin_.disableProperty().bind(y_.selectedProperty().not());
		yMax_.disableProperty().bind(y_.selectedProperty().not());
		zMin_.disableProperty().bind(z_.selectedProperty().not());
		zMax_.disableProperty().bind(z_.selectedProperty().not());

		// add text field listeners
		xMin_.setDefaultValue(null);
		xMax_.setDefaultValue(null);
		yMin_.setDefaultValue(null);
		yMax_.setDefaultValue(null);
		zMin_.setDefaultValue(null);
		zMax_.setDefaultValue(null);
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public void showing() {
		onResetClicked();
	}

	@Override
	public String getHeader() {
		return "Create Element Group";
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public void start() {
		// no implementation
	}

	@FXML
	private void onOkClicked() {

		// get inputs
		String name = name_.getText();
		boolean x = x_.isSelected();
		String[] xRange = { xMin_.getText(), xMax_.getText() };
		boolean y = y_.isSelected();
		String[] yRange = { yMin_.getText(), yMax_.getText() };
		boolean z = z_.isSelected();
		String[] zRange = { zMin_.getText(), zMax_.getText() };

		// check inputs
		if (!checkInputs(name, x, y, z, xRange, yRange, zRange))
			return;

		// get selected model
		AircraftModel model = (AircraftModel) owner_.getSelectedFiles().get(0);

		// create group
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new CreateElementGroupFromCoordinates(model, name, x, y, z, xRange, yRange, zRange));

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {
		name_.clear();
		x_.setSelected(true);
		y_.setSelected(false);
		z_.setSelected(false);
		xMin_.reset();
		xMax_.reset();
		yMin_.reset();
		yMax_.reset();
		zMin_.reset();
		zMax_.reset();
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to create element group from X/Y/Z coordinates", null);
	}

	/**
	 * Checks inputs.
	 *
	 * @param name
	 *            Group name.
	 * @param x
	 *            True if x range given.
	 * @param y
	 *            True if y range given.
	 * @param z
	 *            True if z range given.
	 * @param xRange
	 *            X range.
	 * @param yRange
	 *            Y range.
	 * @param zRange
	 *            Z range.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(String name, boolean x, boolean y, boolean z, String[] xRange, String[] yRange, String[] zRange) {

		// invalid name
		if ((name == null) || name.trim().isEmpty()) {
			String message = "Please enter a group name to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(name_);
			return false;
		}

		// no range selected
		if (!x && !y && !z) {
			String message = "Please select at least 1 coordinate range to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(x_);
			return false;
		}

		// x range
		if (x) {
			try {

				// parse values
				double min = Double.parseDouble(xRange[0].trim());
				double max = Double.parseDouble(xRange[1].trim());

				// compare
				if (min >= max) {
					String message = "Minimum value should be smaller than maximum value.";
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(xMin_);
					return false;
				}
			}

			// invalid range value
			catch (NumberFormatException e) {
				String message = "Please enter valid range values to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(x_);
				return false;
			}
		}

		// y range
		if (y) {
			try {

				// parse values
				double min = Double.parseDouble(yRange[0].trim());
				double max = Double.parseDouble(yRange[1].trim());

				// compare
				if (min >= max) {
					String message = "Minimum value should be smaller than maximum value.";
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(yMin_);
					return false;
				}
			}

			// invalid range value
			catch (NumberFormatException e) {
				String message = "Please enter valid range values to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(y_);
				return false;
			}
		}

		// z range
		if (z) {
			try {

				// parse values
				double min = Double.parseDouble(zRange[0].trim());
				double max = Double.parseDouble(zRange[1].trim());

				// compare
				if (min >= max) {
					String message = "Minimum value should be smaller than maximum value.";
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(zMin_);
					return false;
				}
			}

			// invalid range value
			catch (NumberFormatException e) {
				String message = "Please enter valid range values to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(z_);
				return false;
			}
		}

		// acceptable
		return true;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static CreateElementGroupFromCoordinatesPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CreateElementGroupFromCoordinatesPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CreateElementGroupFromCoordinatesPanel controller = (CreateElementGroupFromCoordinatesPanel) fxmlLoader.getController();

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
