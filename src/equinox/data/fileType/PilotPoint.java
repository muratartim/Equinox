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
 * Class for pilot point.
 *
 * @author Murat Artim
 * @date Aug 28, 2015
 * @time 11:17:43 AM
 */
public class PilotPoint extends SpectrumItem {

	/** STF file ID. */
	private final int stfID_;

	/**
	 * Creates pilot point item.
	 *
	 * @param id
	 *            Pilot point ID.
	 * @param stfFileName
	 *            STF file name.
	 * @param stfID
	 *            STF file ID.
	 */
	public PilotPoint(int id, String stfFileName, int stfID) {

		// create spectrum item
		super(stfFileName, id);
		stfID_ = stfID;

		// create icon label
		Label iconLabel = new Label("\uec96");
		iconLabel.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		setGraphic(iconLabel);
	}

	/**
	 * Returns STF file ID.
	 *
	 * @return STF file ID.
	 */
	public int getSTFFileID() {
		return stfID_;
	}

	@Override
	public PilotPoints getParentItem() {
		return (PilotPoints) getParent();
	}

	@Override
	public IconicFont getIconFont() {
		return IconicFont.ICOMOON;
	}

	/**
	 * Creates and returns context menu for load case.
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

		// find
		Label findIcon = new Label("\uf002");
		findIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem find = new MenuItem("Find Similar Files", findIcon);
		find.setId("find");
		find.setOnAction(handler);
		contextMenu.getItems().add(find);

		// delete link
		contextMenu.getItems().add(new SeparatorMenuItem());
		Label deleteIcon = new Label("\uf014");
		deleteIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem delete = new MenuItem("Delete Link", deleteIcon);
		delete.setId("delete");
		delete.setOnAction(handler);
		contextMenu.getItems().add(delete);

		// return menu
		return contextMenu;
	}
}
