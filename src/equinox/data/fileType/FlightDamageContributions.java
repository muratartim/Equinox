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
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Class for typical flight damage contributions spectrum item.
 *
 * @author Murat Artim
 * @date 14 Oct 2016
 * @time 10:25:11
 */
public class FlightDamageContributions extends SpectrumItem {

	/** Material name. */
	private final String materialName_;

	/**
	 * Creates typical flight damage contributions spectrum item.
	 *
	 * @param name
	 *            Name of damage contributions.
	 * @param id
	 *            ID of item.
	 * @param materialName
	 *            Material name.
	 */
	public FlightDamageContributions(String name, int id, String materialName) {

		// create spectrum item
		super(name, id);
		materialName_ = materialName;

		// create icon label
		Label iconLabel = new Label("\ueb7d");
		iconLabel.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		setGraphic(iconLabel);
	}

	@Override
	public STFFile getParentItem() {
		return (STFFile) getParent();
	}

	@Override
	public IconicFont getIconFont() {
		return IconicFont.ICOMOON;
	}

	/**
	 * Returns the material name.
	 *
	 * @return The material name.
	 */
	public String getMaterialName() {
		return materialName_;
	}

	/**
	 * Creates and returns context menu for ANA files.
	 *
	 * @param multipleSelection
	 *            True if multiple ANA files are selected.
	 * @param handler
	 *            Action handler.
	 * @return Context menu.
	 */
	public static ContextMenu createContextMenu(boolean multipleSelection, ActionHandler handler) {

		// create menu
		ContextMenu contextMenu = new ContextMenu();

		// plot damage contributions
		if (!multipleSelection) {
			Label dameContIcon = new Label("\ueb7d");
			dameContIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			MenuItem plot = new MenuItem("Plot Damage Contributions", dameContIcon);
			plot.setId("plotFlightDamageContributions");
			plot.setOnAction(handler);
			contextMenu.getItems().add(plot);
			contextMenu.getItems().add(new SeparatorMenuItem());
		}

		// find
		Label findIcon = new Label("\uf002");
		findIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem find = new MenuItem("Find Similar Files", findIcon);
		find.setId("find");
		find.setOnAction(handler);
		contextMenu.getItems().add(find);

		// rename
		Label renameIcon = new Label("\uf246");
		renameIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem rename = new MenuItem("Rename...", renameIcon);
		rename.setId("rename");
		rename.setOnAction(handler);
		contextMenu.getItems().add(rename);

		// add separator
		contextMenu.getItems().add(new SeparatorMenuItem());

		// share
		Label shareIcon = new Label("\uf064");
		shareIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem share = new MenuItem("Share", shareIcon);
		share.setId("shareFlightDamageContributions");
		share.setOnAction(handler);
		contextMenu.getItems().add(share);

		// save
		Label saveIcon = new Label("\uf0c7");
		saveIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem save = new MenuItem("Save As...", saveIcon);
		save.setId("saveFlightDamageContributions");
		save.setOnAction(handler);
		contextMenu.getItems().add(save);
		contextMenu.getItems().add(new SeparatorMenuItem());

		// delete
		Label deleteIcon = new Label("\uf014");
		deleteIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem remove = new MenuItem("Delete", deleteIcon);
		remove.setId("delete");
		remove.setOnAction(handler);
		contextMenu.getItems().add(remove);

		// return menu
		return contextMenu;
	}
}
