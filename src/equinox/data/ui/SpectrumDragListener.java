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
import java.util.ArrayList;
import java.util.List;

import equinox.data.fileType.Spectrum;
import equinox.plugin.FileType;
import equinox.task.AddSTFFiles;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

/**
 * Class for spectrum drag listener.
 *
 * @author Murat Artim
 * @date Feb 24, 2015
 * @time 1:27:56 PM
 */
public class SpectrumDragListener extends DragListener {

	/**
	 * Creates spectrum drag listener.
	 *
	 * @param cell
	 *            The owner cell implementation.
	 */
	public SpectrumDragListener(DragCell cell) {
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
				if (fileType == null)
					continue;

				// accepted type
				if (fileType.equals(FileType.STF)) {
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
				if (fileType == null)
					continue;

				// accepted type
				if (fileType.equals(FileType.STF)) {
					Label label = (Label) cell_.getGraphic();
					label.setText("\uef0b");
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
		label.setText("\ueb8c");
		event.consume();
	}

	@Override
	protected void onDragDropped(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		boolean success = false;
		if (db.hasFiles()) {

			// get spectrum
			Spectrum spectrum = (Spectrum) cell_.getSpectrumItem();

			// get files
			List<File> files = db.getFiles();

			// create list to store filtered files
			ArrayList<File> filteredFiles = null;

			// check file types
			for (File file : files) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null)
					continue;

				// STF
				if (fileType.equals(FileType.STF)) {
					cell_.getMainScreen().setInitialDirectory(file);
					if (filteredFiles == null)
						filteredFiles = new ArrayList<>();
					filteredFiles.add(file);
				}
			}

			// run task
			if (filteredFiles != null && !filteredFiles.isEmpty()) {
				cell_.getMainScreen().getActiveTasksPanel().runTaskSequentially(new AddSTFFiles(filteredFiles, spectrum, null));
				success = true;
			}
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}
}
