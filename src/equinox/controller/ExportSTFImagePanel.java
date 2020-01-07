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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.Equinox;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.STFFile;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.GetSTFImage;
import equinox.task.GetSTFImage.STFImageRequestingPanel;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for export STF image panel controller.
 *
 * @author Murat Artim
 * @date May 10, 2016
 * @time 1:58:54 PM
 */
public class ExportSTFImagePanel implements Initializable, STFImageRequestingPanel {

	/** The owner panel. */
	private ExportSTFPanel owner_;

	/** Pilot point image type. */
	private PilotPointImageType imageType_;

	@FXML
	private StackPane root_;

	@FXML
	private VBox setImagePane_;

	@FXML
	private Button reset_, change_;

	@FXML
	private ImageView image_;

	@FXML
	private Label infoLabel_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Returns the root container.
	 *
	 * @return The root container.
	 */
	public StackPane getRoot() {
		return root_;
	}

	/**
	 * Returns image type of this panel.
	 *
	 * @return Image type of this panel.
	 */
	public PilotPointImageType getImageType() {
		return imageType_;
	}

	/**
	 * Returns the image of this panel.
	 *
	 * @return The image of this panel.
	 */
	public Image getImage() {
		return image_.getImage();
	}

	@Override
	public void setImage(Image image) {
		image_.setImage(image);
		image_.setVisible(image != null);
		setImagePane_.setVisible(image == null);
	}

	@FXML
	public void onSetImageClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show open dialog
		File file = fileChooser.showOpenDialog(owner_.getOwner().getOwner().getOwner().getStage());

		// no file selected
		if (file == null || !file.exists())
			return;

		// process image
		processImage(file);
	}

	@FXML
	private void onImageClicked() {

		// get selected STF file
		STFFile stfFile = (STFFile) owner_.getOwner().getSelectedFiles().get(0);

		// show popup
		PopOver popOver = new PopOver();
		popOver.setDetached(true);
		popOver.setTitle(stfFile.getName() + " - " + imageType_.getPageName());
		popOver.setContentNode(ImagePanel.load(image_.getImage()));
		popOver.setHideOnEscape(true);
		popOver.show(owner_.getOwner().getOwner().getOwner().getStage());
	}

	@SuppressWarnings("static-method")
	@FXML
	private void onDragOver(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if (db.hasFiles()) {
			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.PNG)) {
					event.acceptTransferModes(TransferMode.ANY);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragDropped(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		boolean success = false;
		if (db.hasFiles()) {
			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// PNG
				if (fileType.equals(FileType.PNG)) {
					processImage(file);
					success = true;
					break;
				}
			}
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}

	/**
	 * processes given image file.
	 *
	 * @param file
	 *            Image file.
	 */
	private void processImage(File file) {

		// set initial directory
		owner_.getOwner().getOwner().setInitialDirectory(file);

		// maximum image size exceeded
		if (file.length() >= PilotPointImageType.MAX_IMAGE_SIZE) {
			String message = "Maximum image size exceeded. Please reduce the image size to maximum 2MB to set it.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(image_);
			return;
		}

		try {

			// get image bytes
			byte[] imageBytes = new byte[(int) file.length()];
			try (ImageInputStream imgStream = ImageIO.createImageInputStream(file)) {
				imgStream.read(imageBytes);
				try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
					image_.setImage(new Image(inputStream));
				}
			}

			// setup components
			image_.setVisible(true);
			setImagePane_.setVisible(false);
		}

		// exception occurred while setting image
		catch (IOException e) {

			// log exception
			Equinox.LOGGER.log(Level.WARNING, "Exception occured during reading pilot point image.", e);

			// show warning
			String message = "Exception occured during reading pilot point image: " + e.getLocalizedMessage();
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(image_);
		}
	}

	@FXML
	public void onResetClicked() {

		// get selected STF file
		STFFile stfFile = (STFFile) owner_.getOwner().getSelectedFiles().get(0);

		// get image
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetSTFImage(stfFile, imageType_, this));
	}

	@FXML
	private void onChangeClicked() {
		onSetImageClicked();
	}

	@FXML
	private void showButtons() {
		if (!setImagePane_.isVisible()) {
			reset_.setVisible(true);
			change_.setVisible(true);
		}
	}

	@FXML
	private void hideButtons() {
		if (!setImagePane_.isVisible()) {
			reset_.setVisible(false);
			change_.setVisible(false);
		}
	}

	/**
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @param imageType
	 *            Pilot point image type.
	 * @return The newly loaded plot text view panel.
	 */
	public static ExportSTFImagePanel load(ExportSTFPanel owner, PilotPointImageType imageType) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ExportSTFImagePanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			ExportSTFImagePanel controller = (ExportSTFImagePanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;
			controller.imageType_ = imageType;

			// set info label
			controller.infoLabel_.setText(imageType.getPageName() + (!imageType.equals(PilotPointImageType.IMAGE) ? " plot." : "."));

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
