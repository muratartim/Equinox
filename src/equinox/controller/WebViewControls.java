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

import equinox.data.EquinoxTheme;
import equinox.data.fileType.SpectrumItem;
import equinox.plugin.FileType;
import equinox.task.SaveOutputFile;
import equinox.utility.Utility;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;

/**
 * Class for web view controls controller.
 *
 * @author Murat Artim
 * @date 26 Apr 2017
 * @time 13:34:01
 *
 */
public class WebViewControls implements Initializable {

	/** The owner panel. */
	private WebViewPanel owner_;

	@FXML
	private HBox root_;

	@FXML
	private Button save_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set root to grow always
		HBox.setHgrow(root_, Priority.ALWAYS);
	}

	/**
	 * Returns the owner panel of this sub panel.
	 *
	 * @return The owner panel of this sub panel.
	 */
	public WebViewPanel getOwner() {
		return owner_;
	}

	/**
	 * Returns the root of this controller.
	 *
	 * @return The root of this controller.
	 */
	public HBox getRoot() {
		return root_;
	}

	/**
	 * Returns save button.
	 *
	 * @return Save button.
	 */
	public Button getSaveButton() {
		return save_;
	}

	@FXML
	private void onSaveClicked() {

		// get output file name
		String fileName = owner_.getHeader();
		if (fileName == null)
			return;
		FileType fileType = FileType.getFileType(fileName);
		if (fileType == null)
			return;

		// get selected files
		ObservableList<TreeItem<String>> selected = owner_.getOwner().getOwner().getInputPanel().getSelectedFiles();

		// no file selected
		if (selected.isEmpty())
			return;

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getOwner().getFileChooser(fileType.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName(fileName);
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File output = FileType.appendExtension(selectedFile, fileType);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new SaveOutputFile((SpectrumItem) selected.get(0), output.toPath()));
	}

	/**
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot text view panel.
	 */
	public static WebViewControls load(WebViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("WebViewControls.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			WebViewControls controller = (WebViewControls) fxmlLoader.getController();

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
