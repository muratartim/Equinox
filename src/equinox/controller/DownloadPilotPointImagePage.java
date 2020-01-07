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

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.DownloadPilotPointInfoPanel.InfoPage;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.plugin.FileType;
import equinox.task.GetPilotPointImage;
import equinox.task.SavePilotPointImageToGlobalDB;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for download pilot point image page controller.
 *
 * @author Murat Artim
 * @date Feb 15, 2016
 * @time 3:50:16 PM
 */
public class DownloadPilotPointImagePage implements InfoPage {

	/** The owner panel. */
	private DownloadPilotPointInfoPanel owner_;

	/** The owner popup. */
	private PopOver popOver_;

	/** Pilot point image type. */
	private PilotPointImageType imageType_;

	/** Image request indicator. */
	private boolean imageRequested_ = false, canEdit_ = false;

	@FXML
	private VBox root_, setImagePane_;

	@FXML
	private ImageView ppImage_;

	@FXML
	private Label noImage_, downloading_, infoLabel_;

	@FXML
	private Button reset_, change_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@Override
	public VBox getRoot() {
		return root_;
	}

	@Override
	public String getPageName() {
		return imageType_.getPageName();
	}

	@Override
	public void showing(PilotPointInfo info) {

		// remove auto-hide
		if (canEdit_) {
			popOver_.setAutoHide(false);
		}

		// image already requested
		if (imageRequested_)
			return;

		// request pilot point image
		imageRequested_ = true;
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetPilotPointImage((long) info.getInfo(PilotPointInfoType.ID), imageType_, this));
	}

	/**
	 * Sets pilot point image to this panel.
	 *
	 * @param imageBytes
	 *            Image bytes.
	 */
	public void setPilotPointImage(byte[] imageBytes) {

		// STF file has image
		if (imageBytes != null) {

			// set image to view
			ppImage_.setImage(new Image(new ByteArrayInputStream(imageBytes)));

			// setup components
			setImagePane_.setVisible(false);
			noImage_.setVisible(false);
			downloading_.setVisible(false);
			ppImage_.setVisible(true);
			reset_.setVisible(false);
			change_.setVisible(false);
		}

		// no image
		else {

			// set image to view
			ppImage_.setImage(null);

			// setup components
			if (canEdit_) {
				setImagePane_.setVisible(true);
			}
			else {
				noImage_.setVisible(true);
			}
			downloading_.setVisible(false);
			ppImage_.setVisible(false);
			reset_.setVisible(false);
			change_.setVisible(false);
		}
	}

	@FXML
	private void onDragOver(DragEvent event) {

		// not administrator
		if (!canEdit_)
			return;

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

		// not administrator
		if (!canEdit_)
			return;

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

	@FXML
	private void showButtons() {
		if (canEdit_) {
			if (!setImagePane_.isVisible()) {
				reset_.setVisible(true);
				change_.setVisible(true);
			}
		}
	}

	@FXML
	private void hideButtons() {
		if (canEdit_) {
			if (!setImagePane_.isVisible()) {
				reset_.setVisible(false);
				change_.setVisible(false);
			}
		}
	}

	@FXML
	private void onImageClicked() {

		// get pilot point image
		Image image = ppImage_.getImage();

		// no image
		if (image == null)
			return;

		// show image
		PopOver popOver = new PopOver();
		popOver.setDetached(true);
		popOver.setTitle(owner_.getPilotPointName() + " - " + getPageName());
		popOver.setContentNode(ImagePanel.load(image));
		popOver.setHideOnEscape(true);
		popOver.show(owner_.getOwner().getOwner().getOwner().getOwner().getOwner().getStage());
	}

	@FXML
	private void onSetImageClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getOwner().getOwner().getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show open dialog
		File file = fileChooser.showOpenDialog(owner_.getOwner().getOwner().getOwner().getOwner().getOwner().getStage());

		// no file selected
		if (file == null || !file.exists())
			return;

		// process image
		processImage(file);
	}

	@FXML
	private void onResetClicked() {

		// get pilot point info
		PilotPointInfo info = (PilotPointInfo) owner_.getOwner().getInfo();

		// save
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new SavePilotPointImageToGlobalDB(info, imageType_, null, this));
	}

	@FXML
	private void onChangeClicked() {
		onSetImageClicked();
	}

	/**
	 * Processes given image file.
	 *
	 * @param file
	 *            Image file.
	 */
	private void processImage(File file) {

		// set initial directory
		owner_.getOwner().getOwner().getOwner().getOwner().setInitialDirectory(file);

		// maximum image size exceeded
		if (file.length() >= PilotPointImageType.MAX_IMAGE_SIZE) {
			String message = "Maximum image size exceeded. Please reduce the image size to maximum 2MB to set it.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(ppImage_);
			return;
		}

		// get pilot point info
		PilotPointInfo info = (PilotPointInfo) owner_.getOwner().getInfo();

		// save
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getOwner().getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new SavePilotPointImageToGlobalDB(info, imageType_, file.toPath(), this));
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @param imageType
	 *            Pilot point image type.
	 * @param canEdit
	 *            True if the current user edit pilot point image.
	 * @param popOver
	 *            The owner popup.
	 * @return The newly loaded file CDF set panel.
	 */
	public static DownloadPilotPointImagePage load(DownloadPilotPointInfoPanel owner, PilotPointImageType imageType, boolean canEdit, PopOver popOver) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DownloadPilotPointImagePage.fxml"));
			fxmlLoader.load();

			// get controller
			DownloadPilotPointImagePage controller = (DownloadPilotPointImagePage) fxmlLoader.getController();

			// set attributes
			controller.owner_ = owner;
			controller.popOver_ = popOver;
			controller.canEdit_ = canEdit;
			controller.imageType_ = imageType;
			controller.downloading_.setText("Downloading " + imageType.getPageName() + "...");
			controller.noImage_.setText(imageType.getPageName() + " not available.");
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
