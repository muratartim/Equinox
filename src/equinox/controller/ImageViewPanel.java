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

import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.plugin.FileType;
import equinox.task.SaveImage;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for image view panel controller.
 *
 * @author Murat Artim
 * @date Sep 22, 2014
 * @time 2:29:10 PM
 */
public class ImageViewPanel implements InternalViewSubPanel {

	/** The owner panel. */
	private ViewPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ImageView image_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
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
	public void hiding() {
		// no implementation
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public ViewPanel getOwner() {
		return owner_;
	}

	@Override
	public String getHeader() {
		return "Image View";
	}

	@Override
	public HBox getControls() {
		return null;
	}

	@Override
	public boolean canSaveView() {
		return true;
	}

	@Override
	public void saveView() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName("Image View" + FileType.PNG.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File file = FileType.appendExtension(selectedFile, FileType.PNG);

		// take snapshot of the scene
		WritableImage snapshot = image_.snapshot(null, null);

		// create and start task in progress panel
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SaveImage(file, snapshot));
	}

	@Override
	public String getViewName() {
		return "Image View";
	}

	@Override
	public WritableImage getViewImage() {
		return image_.snapshot(null, null);
	}

	/**
	 * Sets shared view image to this panel.
	 *
	 * @param image
	 *            View image.
	 */
	public void setView(Image image) {
		image_.setImage(image);
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static ImageViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ImageViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			ImageViewPanel controller = (ImageViewPanel) fxmlLoader.getController();

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
