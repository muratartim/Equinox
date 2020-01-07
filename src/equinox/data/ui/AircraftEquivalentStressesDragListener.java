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
import java.util.List;

import equinox.controller.AddAircraftEquivalentStressesPanel;
import equinox.controller.InputPanel;
import equinox.plugin.FileType;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

/**
 * Class for A/C model equivalent stresses drag listener.
 *
 * @author Murat Artim
 * @date Sep 5, 2015
 * @time 12:49:33 PM
 */
public class AircraftEquivalentStressesDragListener extends DragListener {

	/**
	 * Creates A/C model equivalent stresses drag listener.
	 *
	 * @param cell
	 *            The owner cell implementation.
	 */
	public AircraftEquivalentStressesDragListener(DragCell cell) {
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

				// EQS or XLS
				if (fileType.equals(FileType.EQS) || fileType.equals(FileType.XLS)) {
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

				// EQS or XLS
				if (fileType.equals(FileType.EQS) || fileType.equals(FileType.XLS)) {
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
		label.setText("\ue9db");
		event.consume();
	}

	@Override
	protected void onDragDropped(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		boolean success = false;
		if (db.hasFiles()) {

			// get files
			List<File> files = db.getFiles();

			// check file types
			for (File file : files) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null)
					continue;

				// EQS or XLS
				if (fileType.equals(FileType.EQS) || fileType.equals(FileType.XLS)) {

					// get input panel
					InputPanel inputPanel = cell_.getMainScreen().getInputPanel();

					// show add equivalent stresses panel
					inputPanel.showSubPanel(InputPanel.ADD_EQUIVALENT_STRESSES_PANEL);

					// process files
					AddAircraftEquivalentStressesPanel panel = (AddAircraftEquivalentStressesPanel) inputPanel.getSubPanel(InputPanel.ADD_EQUIVALENT_STRESSES_PANEL);
					success = panel.processFile(file);
					break;
				}
			}
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}
}
