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
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;

/**
 * Class for pilot points folder.
 *
 * @author Murat Artim
 * @date Aug 27, 2015
 * @time 2:23:16 PM
 */
public class PilotPoints extends SpectrumItem {

	/**
	 * Creates pilot points folder.
	 *
	 * @param id
	 *            A/C model ID.
	 */
	public PilotPoints(int id) {

		// create spectrum item
		super("Linked Pilot Points", id);

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
	 * Returns the pilot points.
	 *
	 * @return The pilot points.
	 */
	public ArrayList<PilotPoint> getPilotPoints() {
		ArrayList<PilotPoint> pilotPoints = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof PilotPoint) {
				if (pilotPoints == null)
					pilotPoints = new ArrayList<>();
				pilotPoints.add((PilotPoint) item);
			}
		}
		return pilotPoints;
	}

	/**
	 * Creates and returns context menu for element stresses folders.
	 *
	 * @param multipleSelection
	 *            True if multiple folders are selected.
	 * @param handler
	 *            Action handler.
	 * @param selected
	 *            Selected pilot points folders.
	 * @return Context menu.
	 */
	public static ContextMenu createContextMenu(boolean multipleSelection, ActionHandler handler, ObservableList<TreeItem<String>> selected) {

		// create menu
		ContextMenu contextMenu = new ContextMenu();

		// single selection
		if (!multipleSelection) {

			// add load cases
			Label linkIcon = new Label("\uec96");
			linkIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			MenuItem addLoadCases = new MenuItem("Link Pilot Points", linkIcon);
			addLoadCases.setId("linkPilotPoints");
			addLoadCases.setOnAction(handler);
			contextMenu.getItems().add(addLoadCases);

			// plot linked pilot point data
			if (((PilotPoints) selected.get(0)).getPilotPoints() != null) {

				// add separator
				contextMenu.getItems().add(new SeparatorMenuItem());

				// create plot menu
				Label plotIcon = new Label("\ue930");
				plotIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
				Menu plot = new Menu("Plot...", plotIcon);
				contextMenu.getItems().add(plot);

				// plot pilot point equivalent stresses
				Label eqStressIcon = new Label("\uedaf");
				eqStressIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
				MenuItem plotEquivalentStresses = new MenuItem("Equivalent Stresses", eqStressIcon);
				plotEquivalentStresses.setId("plotPilotPointEquivalentStresses");
				plotEquivalentStresses.setOnAction(handler);
				plot.getItems().add(plotEquivalentStresses);

				// plot pilot point life factors
				Label lifeFactorsIcon = new Label("\ueb84");
				lifeFactorsIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
				MenuItem plotLifeFactors = new MenuItem("Life/Adjustment Factors", lifeFactorsIcon);
				plotLifeFactors.setId("plotPilotPointLifeFactors");
				plotLifeFactors.setOnAction(handler);
				plot.getItems().add(plotLifeFactors);

				// plot pilot point equivalent stress ratios
				Label stressRatiosIcon = new Label("\ue902");
				stressRatiosIcon.getStylesheets().add(IconicFont.CUSTOM.getStyleSheet());
				MenuItem plotStressRatios = new MenuItem("Equivalent Stress Ratios", stressRatiosIcon);
				plotStressRatios.setId("plotPilotPointEquivalentStressRatios");
				plotStressRatios.setOnAction(handler);
				plot.getItems().add(plotStressRatios);

				// plot pilot point damage contributions
				Label dameContIcon = new Label("\uf200");
				dameContIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
				MenuItem plotDamageContributions = new MenuItem("Damage Contributions", dameContIcon);
				plotDamageContributions.setId("plotPilotPointDamageContributions");
				plotDamageContributions.setOnAction(handler);
				plot.getItems().add(plotDamageContributions);

				// plot pilot point maximum damage angles
				Label damageAngleIcon = new Label("\ueab0");
				damageAngleIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
				MenuItem plotMaximumDamageAngles = new MenuItem("Maximum Damage Angles", damageAngleIcon);
				plotMaximumDamageAngles.setId("plotPilotPointMaximumDamageAngles");
				plotMaximumDamageAngles.setOnAction(handler);
				plot.getItems().add(plotMaximumDamageAngles);

				// add separator
				contextMenu.getItems().add(new SeparatorMenuItem());

				// create element group from linked pilot points
				Label groupIcon = new Label("\uf247");
				groupIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
				MenuItem createGroup = new MenuItem("Create Element Group", groupIcon);
				createGroup.setId("createElementGroupFromLinkedPilotPoints");
				createGroup.setOnAction(handler);
				contextMenu.getItems().add(createGroup);
			}
		}

		// return menu
		return contextMenu;
	}
}
