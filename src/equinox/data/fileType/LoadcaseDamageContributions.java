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
 * Class for loadcase damage contributions spectrum item.
 *
 * @author Murat Artim
 * @date Apr 2, 2015
 * @time 12:00:10 PM
 */
public class LoadcaseDamageContributions extends SpectrumItem {

	/** Material name. */
	private final String materialName_;

	/**
	 * Creates loadcase damage angle item.
	 *
	 * @param name
	 *            Loadcase damage contribution name.
	 * @param id
	 *            ID of item.
	 * @param materialName
	 *            Material name.
	 */
	public LoadcaseDamageContributions(String name, int id, String materialName) {

		// create spectrum item
		super(name, id);
		materialName_ = materialName;

		// create icon label
		Label iconLabel = new Label("\uf200");
		iconLabel.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		setGraphic(iconLabel);
	}

	@Override
	public STFFile getParentItem() {
		return (STFFile) getParent();
	}

	@Override
	public IconicFont getIconFont() {
		return IconicFont.FONTAWESOME;
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
			Label dameContIcon = new Label("\uf200");
			dameContIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem plot = new MenuItem("Plot Damage Contributions", dameContIcon);
			plot.setId("plotDamageContributions");
			plot.setOnAction(handler);
			contextMenu.getItems().add(plot);
		}

		// compare damage contributions
		if (multipleSelection) {
			Label statisticsIcon = new Label("\uf24e");
			statisticsIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem compare = new MenuItem("Compare Damage Contributions", statisticsIcon);
			compare.setId("compareDamageContributions");
			compare.setOnAction(handler);
			contextMenu.getItems().add(compare);
		}

		// find
		contextMenu.getItems().add(new SeparatorMenuItem());
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
		share.setId("shareDamageContributions");
		share.setOnAction(handler);
		contextMenu.getItems().add(share);

		// save
		Label saveIcon = new Label("\uf0c7");
		saveIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem save = new MenuItem("Save As...", saveIcon);
		save.setId("saveDamageContributions");
		save.setOnAction(handler);
		contextMenu.getItems().add(save);
		contextMenu.getItems().add(new SeparatorMenuItem());

		// export
		Label exportIcon = new Label("\uea98");
		exportIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem export = new MenuItem("Export...", exportIcon);
		export.setId("exportDamageContributions");
		export.setOnAction(handler);
		contextMenu.getItems().add(export);
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
