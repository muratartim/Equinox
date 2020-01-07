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
package equinox.data.ui;

import java.io.File;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.ActiveTasksPanel;
import equinox.controller.NotificationPanel1;
import equinox.controller.STFInfoViewPanel;
import equinox.data.fileType.STFFile;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.plugin.FileType;
import equinox.task.SavePilotPointImage;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

/**
 * Class for STF drag listener.
 *
 * @author Murat Artim
 * @date Feb 24, 2015
 * @time 1:49:21 PM
 */
public class STFDragListener extends DragListener {

	/**
	 * Creates STF drag listener.
	 *
	 * @param cell
	 *            The owner cell implementation.
	 */
	public STFDragListener(DragCell cell) {
		super(cell);
	}

	@Override
	protected void onDragOver(DragEvent event) {

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

	@Override
	protected void onDragEntered(DragEvent event) {

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
					Label label = (Label) cell_.getGraphic();
					label.setText("\uf16b");
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@Override
	protected void onDragExited(DragEvent event) {
		Label label = (Label) cell_.getGraphic();
		label.setText("\uf1c4");
		event.consume();
	}

	@Override
	protected void onDragDropped(DragEvent event) {

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

		// get STF file
		STFFile stfFile = (STFFile) cell_.getSpectrumItem();

		// set initial directory
		cell_.getMainScreen().setInitialDirectory(file);

		// maximum image size exceeded
		if (file.length() >= STFInfoViewPanel.MAX_IMAGE_SIZE) {
			String message = "Maximum image size exceeded. Please reduce the image size to maximum 3MB to set it.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(stfFile.getGraphic());
			return;
		}

		// save image
		ActiveTasksPanel tm = cell_.getMainScreen().getActiveTasksPanel();
		tm.runTaskInParallel(new SavePilotPointImage(stfFile, PilotPointImageType.IMAGE, file.toPath()));
	}
}
