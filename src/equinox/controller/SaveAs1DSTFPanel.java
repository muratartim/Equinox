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
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import control.validationField.DoubleValidationField;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.StressComponent;
import equinox.data.fileType.STFFile;
import equinox.plugin.FileType;
import equinox.task.SaveAs1DSTF;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for transform panel controller.
 *
 * @author Murat Artim
 * @date Nov 29, 2014
 * @time 12:27:26 PM
 */
public class SaveAs1DSTFPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private DoubleValidationField rotation_;

	@FXML
	private ChoiceBox<StressComponent> stressComponent_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// add stress components
		stressComponent_.setItems(FXCollections.observableArrayList(StressComponent.values()));
		stressComponent_.getSelectionModel().select(0);

		// add listener
		stressComponent_.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<StressComponent>() {

			@Override
			public void changed(ObservableValue<? extends StressComponent> observable, StressComponent oldValue, StressComponent newValue) {
				rotation_.setDisable(!newValue.equals(StressComponent.ROTATED));
			}
		});

		// add listener to rotation angle
		rotation_.setDefaultValue(0.0);
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
		// no implementation
	}

	@Override
	public String getHeader() {
		return "Save As 1D STF";
	}

	@FXML
	private void onResetClicked() {
		stressComponent_.getSelectionModel().select(StressComponent.NORMAL_X);
		rotation_.reset();
	}

	@FXML
	private void onOKClicked() {

		// check rotation
		String message = rotation_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(rotation_);
			return;
		}

		// get stress component and angle
		StressComponent component = stressComponent_.getSelectionModel().getSelectedItem();
		double angle = Double.parseDouble(rotation_.getText());

		// get selected file
		STFFile selected = (STFFile) owner_.getSelectedFiles().get(0);

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.STF.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName(Utility.correctFileName(selected.getName()));
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File output = FileType.appendExtension(selectedFile, FileType.STF);

		// save file
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SaveAs1DSTF(selected, output, component, angle));

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to save 2D STF files as 1D STF files", null);
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static SaveAs1DSTFPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("SaveAs1DSTFPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			SaveAs1DSTFPanel controller = (SaveAs1DSTFPanel) fxmlLoader.getController();

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
