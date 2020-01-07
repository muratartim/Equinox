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

import org.apache.commons.lang3.builder.HashCodeBuilder;

import equinox.data.ActionHandler;
import equinox.font.IconicFont;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;

/**
 * Class for load case item.
 *
 * @author Murat Artim
 * @date Aug 5, 2015
 * @time 12:04:34 PM
 */
public class AircraftLoadCase extends SpectrumItem {

	/**
	 * Creates load case item.
	 *
	 * @param id
	 *            Load case ID.
	 * @param name
	 *            Load case name.
	 * @param number
	 *            Load case number.
	 */
	public AircraftLoadCase(int id, String name, int number) {

		// create spectrum item
		super(name + " (" + number + ")", id);

		// create icon label
		Label iconLabel = new Label("\uebec");
		iconLabel.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		setGraphic(iconLabel);
	}

	@Override
	public AircraftLoadCases getParentItem() {
		return (AircraftLoadCases) getParent();
	}

	@Override
	public IconicFont getIconFont() {
		return IconicFont.ICOMOON;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(33, 51).append(getID()).append(getName()).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof AircraftLoadCase) {
			AircraftLoadCase lc = (AircraftLoadCase) o;
			if ((lc.getID() == getID()) && lc.getName().equals(getName()))
				return true;
		}
		return false;
	}

	/**
	 * Creates and returns context menu for load case.
	 *
	 * @param multipleSelection
	 *            True if multiple folders are selected.
	 * @param handler
	 *            Action handler.
	 * @param selectedFiles
	 *            Selected load cases.
	 * @param is3dEnabled
	 *            True if 3D viewer is enabled.
	 * @return Context menu.
	 */
	public static ContextMenu createContextMenu(boolean multipleSelection, ActionHandler handler, ObservableList<TreeItem<String>> selectedFiles, boolean is3dEnabled) {

		// create menu
		ContextMenu contextMenu = new ContextMenu();

		// check if load cases are from same model
		boolean fromSameModel = true;
		int modelID = -1;
		for (TreeItem<String> item : selectedFiles) {
			AircraftLoadCase loadCase = (AircraftLoadCase) item;
			if ((modelID != -1) && (loadCase.getParentItem().getParentItem().getID() != modelID)) {
				fromSameModel = false;
				break;
			}
			modelID = loadCase.getParentItem().getParentItem().getID();
		}

		// plot element stresses
		if (fromSameModel) {
			Label plotIcon = new Label("\ue930");
			plotIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			MenuItem plotElementStresses = new MenuItem("Plot Element Stresses", plotIcon);
			plotElementStresses.setId("plotElementStresses");
			plotElementStresses.setOnAction(handler);
			plotElementStresses.setDisable(!is3dEnabled);
			contextMenu.getItems().add(plotElementStresses);
		}

		// multiple selection
		if (multipleSelection) {

			// compare load cases
			if (fromSameModel) {
				Label statisticsIcon = new Label("\uf24e");
				statisticsIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
				MenuItem compare = new MenuItem("Compare Load Cases", statisticsIcon);
				compare.setId("compareLoadCases");
				compare.setOnAction(handler);
				contextMenu.getItems().add(compare);
				contextMenu.getItems().add(new SeparatorMenuItem());
			}
		}

		// single selection
		if (!multipleSelection) {

			// compare element stresses
			Label statisticsIcon = new Label("\uf24e");
			statisticsIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem compareElementStresses = new MenuItem("Compare Element Stresses", statisticsIcon);
			compareElementStresses.setId("compareElementStresses");
			compareElementStresses.setOnAction(handler);
			contextMenu.getItems().add(compareElementStresses);

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());

			// create element group from load case
			Label groupIcon = new Label("\uf247");
			groupIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem createGroup = new MenuItem("Create Element Group", groupIcon);
			createGroup.setId("createElementGroupsFromLoadCase");
			createGroup.setOnAction(handler);
			contextMenu.getItems().add(createGroup);

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());
		}

		// find
		Label findIcon = new Label("\uf002");
		findIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem find = new MenuItem("Find Similar Files", findIcon);
		find.setId("find");
		find.setOnAction(handler);
		contextMenu.getItems().add(find);

		// edit load case comment
		if (!multipleSelection) {
			Label editIcon = new Label("\uf044");
			editIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem comment = new MenuItem("Edit Load Case Comment", editIcon);
			comment.setId("commentLoadCase");
			comment.setOnAction(handler);
			contextMenu.getItems().add(comment);
		}

		// add separator
		contextMenu.getItems().add(new SeparatorMenuItem());

		// share load case
		if (!multipleSelection) {
			Label shareIcon = new Label("\uf064");
			shareIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem share = new MenuItem("Share", shareIcon);
			share.setId("share");
			share.setOnAction(handler);
			contextMenu.getItems().add(share);
		}

		// save
		Label saveIcon = new Label("\uf0c7");
		saveIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem save = new MenuItem("Save As...", saveIcon);
		save.setId("saveLoadCase");
		save.setOnAction(handler);
		contextMenu.getItems().add(save);

		// delete
		contextMenu.getItems().add(new SeparatorMenuItem());
		Label deleteIcon = new Label("\uf014");
		deleteIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem delete = new MenuItem("Delete", deleteIcon);
		delete.setId("delete");
		delete.setOnAction(handler);
		contextMenu.getItems().add(delete);

		// return menu
		return contextMenu;
	}
}
