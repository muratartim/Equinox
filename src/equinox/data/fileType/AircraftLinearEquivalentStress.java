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

import equinox.data.ActionHandler;
import equinox.font.IconicFont;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;

/**
 * Class for A/C model linear equivalent stress.
 *
 * @author Murat Artim
 * @date May 6, 2016
 * @time 11:08:49 PM
 */
public class AircraftLinearEquivalentStress extends SpectrumItem {

	/**
	 * Creates A/C model linear equivalent stress item.
	 *
	 * @param id
	 *            A/C model linear equivalent stress ID.
	 * @param name
	 *            A/C model linear equivalent stress name.
	 */
	public AircraftLinearEquivalentStress(int id, String name) {

		// create spectrum item
		super(name, id);

		// create icon label
		Label iconLabel = new Label("\uf111");
		iconLabel.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		setGraphic(iconLabel);
	}

	@Override
	public AircraftEquivalentStresses getParentItem() {
		return (AircraftEquivalentStresses) getParent();
	}

	@Override
	public IconicFont getIconFont() {
		return IconicFont.FONTAWESOME;
	}

	/**
	 * Creates and returns context menu for load case.
	 *
	 * @param multipleSelection
	 *            True if multiple folders are selected.
	 * @param handler
	 *            Action handler.
	 * @param is3dEnabled
	 *            True if 3D viewer is enabled.
	 * @return Context menu.
	 */
	public static ContextMenu createContextMenu(boolean multipleSelection, ActionHandler handler, boolean is3dEnabled) {

		// create menu
		ContextMenu contextMenu = new ContextMenu();

		// LATER

		// return menu
		return contextMenu;
	}
}
