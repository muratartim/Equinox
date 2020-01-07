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
package equinox.data.fileType;

import java.util.ArrayList;

import equinox.data.ActionHandler;
import equinox.font.IconicFont;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;

/**
 * Class for load cases folder.
 *
 * @author Murat Artim
 * @date Aug 5, 2015
 * @time 11:51:51 AM
 */
public class AircraftLoadCases extends SpectrumItem {

	/**
	 * Creates load cases folder.
	 *
	 * @param id
	 *            A/C model ID.
	 */
	public AircraftLoadCases(int id) {

		// create spectrum item
		super("Load Cases", id);

		// create icon label
		Label iconLabel = new Label("\ue9db");
		iconLabel.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		setGraphic(iconLabel);
	}

	@Override
	public AircraftModel getParentItem() {
		return (AircraftModel) getParent();
	}

	@Override
	public IconicFont getIconFont() {
		return IconicFont.ICOMOON;
	}

	/**
	 * Returns the load cases.
	 *
	 * @return The load cases.
	 */
	public ArrayList<AircraftLoadCase> getLoadCases() {
		ArrayList<AircraftLoadCase> loadCases = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof AircraftLoadCase) {
				if (loadCases == null)
					loadCases = new ArrayList<>();
				loadCases.add((AircraftLoadCase) item);
			}
		}
		return loadCases;
	}

	/**
	 * Creates and returns context menu for element stresses folders.
	 *
	 * @param multipleSelection
	 *            True if multiple folders are selected.
	 * @param handler
	 *            Action handler.
	 * @return Context menu.
	 */
	public static ContextMenu createContextMenu(boolean multipleSelection, ActionHandler handler) {

		// create menu
		ContextMenu contextMenu = new ContextMenu();

		// single selection
		if (!multipleSelection) {

			// add load cases
			Label addIcon = new Label("\uf055");
			addIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem addLoadCases = new MenuItem("Add Load Cases", addIcon);
			addLoadCases.setId("addLoadCases");
			addLoadCases.setOnAction(handler);
			contextMenu.getItems().add(addLoadCases);

			// download sample load case file
			Label downloadIcon = new Label("\uf0ed");
			downloadIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem sampleLoadCase = new MenuItem("Download Sample Load Case File", downloadIcon);
			sampleLoadCase.setId("downloadSampleLoadCase");
			sampleLoadCase.setOnAction(handler);
			contextMenu.getItems().add(sampleLoadCase);
		}

		// return menu
		return contextMenu;
	}
}
