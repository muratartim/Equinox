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

import equinox.controller.MainScreen;
import equinox.data.fileType.AircraftEquivalentStresses;
import equinox.data.fileType.AircraftModel;
import equinox.data.fileType.AircraftLoadCases;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.ui.DragListener.DragCell;
import javafx.scene.control.TreeCell;

/**
 * Class for file tree cell.
 *
 * @author Murat Artim
 * @date Feb 13, 2015
 * @time 2:04:46 PM
 */
public class FileTreeCell extends TreeCell<String>implements DragCell {

	/** The owner main screen. */
	private final MainScreen mainScreen_;

	/** Drag listeners. */
	private final DragListener spectrumDragListener_, stfDragListener_, acModelDragListener_, loadCasesDragListener_,
			acModelEquivalentStressesDragListener_;

	/**
	 * Creates file tree cell.
	 *
	 * @param mainScreen
	 *            The owner main screen.
	 */
	public FileTreeCell(MainScreen mainScreen) {
		super();
		mainScreen_ = mainScreen;
		spectrumDragListener_ = new SpectrumDragListener(this);
		stfDragListener_ = new STFDragListener(this);
		acModelDragListener_ = new AircraftModelDragListener(this);
		loadCasesDragListener_ = new LoadCasesDragListener(this);
		acModelEquivalentStressesDragListener_ = new AircraftEquivalentStressesDragListener(this);
	}

	@Override
	public void updateItem(String item, boolean empty) {

		// update item
		super.updateItem(item, empty);

		// empty cell
		if (empty || item == null) {
			setText(null);
			setGraphic(null);
			setOnDragOver(null);
			setOnDragEntered(null);
			setOnDragExited(null);
			setOnDragDropped(null);
		}

		// valid cell
		else {

			// set text and graphic
			SpectrumItem file = (SpectrumItem) getTreeItem();
			setText(file.getValue());
			setGraphic(file.getGraphic());

			// spectrum
			if (file instanceof Spectrum) {
				setOnDragOver(spectrumDragListener_);
				setOnDragEntered(spectrumDragListener_);
				setOnDragExited(spectrumDragListener_);
				setOnDragDropped(spectrumDragListener_);
			}

			// STF file
			else if (file instanceof STFFile) {
				setOnDragOver(stfDragListener_);
				setOnDragEntered(stfDragListener_);
				setOnDragExited(stfDragListener_);
				setOnDragDropped(stfDragListener_);
			}

			// A/C model
			else if (file instanceof AircraftModel) {
				setOnDragOver(acModelDragListener_);
				setOnDragEntered(acModelDragListener_);
				setOnDragExited(acModelDragListener_);
				setOnDragDropped(acModelDragListener_);
			}

			// load cases folder
			else if (file instanceof AircraftLoadCases) {
				setOnDragOver(loadCasesDragListener_);
				setOnDragEntered(loadCasesDragListener_);
				setOnDragExited(loadCasesDragListener_);
				setOnDragDropped(loadCasesDragListener_);
			}

			// A/C model equivalent stresses folder
			else if (file instanceof AircraftEquivalentStresses) {
				setOnDragOver(acModelEquivalentStressesDragListener_);
				setOnDragEntered(acModelEquivalentStressesDragListener_);
				setOnDragExited(acModelEquivalentStressesDragListener_);
				setOnDragDropped(acModelEquivalentStressesDragListener_);
			}
		}
	}

	@Override
	public SpectrumItem getSpectrumItem() {
		return (SpectrumItem) getTreeItem();
	}

	@Override
	public MainScreen getMainScreen() {
		return mainScreen_;
	}
}
