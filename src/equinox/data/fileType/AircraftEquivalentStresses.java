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
 * Class for aircraft model equivalent stresses folder.
 *
 * @author Murat Artim
 * @date Sep 4, 2015
 * @time 1:24:54 PM
 */
public class AircraftEquivalentStresses extends SpectrumItem {

	/**
	 * Creates aircraft model equivalent stresses folder.
	 *
	 * @param id
	 *            A/C model ID.
	 */
	public AircraftEquivalentStresses(int id) {

		// create spectrum item
		super("Equivalent Stresses", id);

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
	 * Returns the aircraft model fatigue equivalent stresses.
	 *
	 * @return The aircraft model fatigue equivalent stresses.
	 */
	public ArrayList<AircraftFatigueEquivalentStress> getFatigueEquivalentStresses() {
		ArrayList<AircraftFatigueEquivalentStress> equivalentStresses = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof AircraftFatigueEquivalentStress) {
				if (equivalentStresses == null)
					equivalentStresses = new ArrayList<>();
				equivalentStresses.add((AircraftFatigueEquivalentStress) item);
			}
		}
		return equivalentStresses;
	}

	/**
	 * Returns the aircraft model preffas equivalent stresses.
	 *
	 * @return The aircraft model preffas equivalent stresses.
	 */
	public ArrayList<AircraftPreffasEquivalentStress> getPreffasEquivalentStresses() {
		ArrayList<AircraftPreffasEquivalentStress> equivalentStresses = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof AircraftPreffasEquivalentStress) {
				if (equivalentStresses == null)
					equivalentStresses = new ArrayList<>();
				equivalentStresses.add((AircraftPreffasEquivalentStress) item);
			}
		}
		return equivalentStresses;
	}

	/**
	 * Returns the aircraft model linear equivalent stresses.
	 *
	 * @return The aircraft model linear equivalent stresses.
	 */
	public ArrayList<AircraftLinearEquivalentStress> getLinearEquivalentStresses() {
		ArrayList<AircraftLinearEquivalentStress> equivalentStresses = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof AircraftLinearEquivalentStress) {
				if (equivalentStresses == null)
					equivalentStresses = new ArrayList<>();
				equivalentStresses.add((AircraftLinearEquivalentStress) item);
			}
		}
		return equivalentStresses;
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
			MenuItem addLoadCases = new MenuItem("Add Equivalent Stresses", addIcon);
			addLoadCases.setId("addEquivalentStresses");
			addLoadCases.setOnAction(handler);
			contextMenu.getItems().add(addLoadCases);
		}

		// return menu
		return contextMenu;
	}
}
