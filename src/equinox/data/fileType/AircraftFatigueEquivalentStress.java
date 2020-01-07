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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Class for A/C model fatigue equivalent stress.
 *
 * @author Murat Artim
 * @date Sep 4, 2015
 * @time 1:49:30 PM
 */
public class AircraftFatigueEquivalentStress extends SpectrumItem {

	/**
	 * Creates A/C model fatigue equivalent stress item.
	 *
	 * @param id
	 *            A/C model fatigue equivalent stress ID.
	 * @param name
	 *            A/C model fatigue equivalent stress name.
	 */
	public AircraftFatigueEquivalentStress(int id, String name) {

		// create spectrum item
		super(name, id);

		// create icon label
		Label iconLabel = new Label("\uf10c");
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

		// single selection
		if (!multipleSelection) {

			// create plot menu
			Label plotIcon = new Label("\ue930");
			plotIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			Menu plot = new Menu("Plot...", plotIcon);
			contextMenu.getItems().add(plot);

			// plot equivalent stresses
			MenuItem plotStresses = new MenuItem("Equivalent Stresses");
			plotStresses.setId("plotAircraftModelEquivalentStresses");
			plotStresses.setOnAction(handler);
			plot.getItems().add(plotStresses);

			// plot life factors
			MenuItem plotLifeFactors = new MenuItem("Life/Adjustment Factors");
			plotLifeFactors.setId("plotAircraftModelLifeFactors");
			plotLifeFactors.setOnAction(handler);
			plot.getItems().add(plotLifeFactors);

			// plot equivalent stresses
			MenuItem plotRatios = new MenuItem("Equivalent Stress Ratios");
			plotRatios.setId("plotAircraftModelEquivalentStressRatios");
			plotRatios.setOnAction(handler);
			plot.getItems().add(plotRatios);

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());

			// compare equivalent stresses
			Label statisticsIcon = new Label("\uf24e");
			statisticsIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem compareStresses = new MenuItem("Compare Equivalent Stresses", statisticsIcon);
			compareStresses.setId("compareAircraftModelEquivalentStresses");
			compareStresses.setOnAction(handler);
			contextMenu.getItems().add(compareStresses);

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());

			// generate life factors
			Label generateLifeFactorsIcon = new Label("\ueb84");
			generateLifeFactorsIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			MenuItem generateLifeFactors = new MenuItem("Generate Life/Adjustment Factors", generateLifeFactorsIcon);
			generateLifeFactors.setId("generateAircraftModelLifeFactor");
			generateLifeFactors.setOnAction(handler);
			contextMenu.getItems().add(generateLifeFactors);

			// generate equivalent stress ratios
			Label generateStressRatiosIcon = new Label("\ue902");
			generateStressRatiosIcon.getStylesheets().add(IconicFont.CUSTOM.getStyleSheet());
			MenuItem generateStressRatios = new MenuItem("Generate Equivalent Stress Ratios", generateStressRatiosIcon);
			generateStressRatios.setId("generateAircraftModelStressRatio");
			generateStressRatios.setOnAction(handler);
			contextMenu.getItems().add(generateStressRatios);

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());

			// create element group from equivalent stress
			Label groupIcon = new Label("\uf247");
			groupIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem createGroup = new MenuItem("Create Element Group", groupIcon);
			createGroup.setId("createElementGroupsFromEquivalentStress");
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

		// rename
		Label renameIcon = new Label("\uf246");
		renameIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem rename = new MenuItem("Rename...", renameIcon);
		rename.setId("rename");
		rename.setOnAction(handler);
		contextMenu.getItems().add(rename);

		// add separator
		contextMenu.getItems().add(new SeparatorMenuItem());

		// single selection
		if (!multipleSelection) {

			// create share menu
			Label shareIcon = new Label("\uf064");
			shareIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			Menu share = new Menu("Share...", shareIcon);
			contextMenu.getItems().add(share);

			// share equivalent stress
			MenuItem shareStress = new MenuItem("Equivalent Stresses");
			shareStress.setId("share");
			shareStress.setOnAction(handler);
			share.getItems().add(shareStress);

			// share life factors
			MenuItem shareLifeFactors = new MenuItem("Life/Adjustment Factors");
			shareLifeFactors.setId("shareAircraftLifeFactors");
			shareLifeFactors.setOnAction(handler);
			share.getItems().add(shareLifeFactors);

			// share equivalent stress ratios
			MenuItem shareStressRatios = new MenuItem("Equivalent Stress Ratios");
			shareStressRatios.setId("shareAircraftStressRatios");
			shareStressRatios.setOnAction(handler);
			share.getItems().add(shareStressRatios);
		}

		// create save menu
		Label saveIcon = new Label("\uf0c7");
		saveIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		Menu save = new Menu("Save...", saveIcon);
		contextMenu.getItems().add(save);

		// save equivalent stress
		MenuItem saveStress = new MenuItem("Equivalent Stresses");
		saveStress.setId("saveAircraftEquivalentStress");
		saveStress.setOnAction(handler);
		save.getItems().add(saveStress);

		// single selection
		if (!multipleSelection) {

			// save life factors
			MenuItem saveLifeFactors = new MenuItem("Life/Adjustment Factors");
			saveLifeFactors.setId("saveAircraftLifeFactors");
			saveLifeFactors.setOnAction(handler);
			save.getItems().add(saveLifeFactors);

			// save equivalent stress ratios
			MenuItem saveStressRatios = new MenuItem("Equivalent Stress Ratios");
			saveStressRatios.setId("saveAircraftStressRatios");
			saveStressRatios.setOnAction(handler);
			save.getItems().add(saveStressRatios);
		}

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
