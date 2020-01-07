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
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;

/**
 * Class for RFORT spectrum file.
 *
 * @author Murat Artim
 * @date Mar 10, 2016
 * @time 10:20:49 AM
 */
public class Rfort extends SpectrumItem {

	/** Analysis types. */
	private final boolean fatigue_, preffas_, linear_;

	/**
	 * Creates RFORT spectrum file.
	 *
	 * @param fileName
	 *            File name.
	 * @param fileID
	 *            File ID.
	 * @param fatigue
	 *            True if fatigue analysis was run for pilot points.
	 * @param preffas
	 *            True if preffas analysis was run for pilot points.
	 * @param linear
	 *            True if linear prop. analysis was run for pilot points.
	 */
	public Rfort(String fileName, int fileID, boolean fatigue, boolean preffas, boolean linear) {

		// create spectrum item
		super(fileName, fileID);

		// create icon label
		Label iconLabel = new Label("\uf0b0");
		iconLabel.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		setGraphic(iconLabel);

		// set analysis types
		fatigue_ = fatigue;
		preffas_ = preffas;
		linear_ = linear;
	}

	/**
	 * Returns true if fatigue analysis was run for pilot points.
	 *
	 * @return True if fatigue analysis was run for pilot points.
	 */
	public boolean isFatigue() {
		return fatigue_;
	}

	/**
	 * Returns true if preffas analysis was run for pilot points.
	 *
	 * @return True if preffas analysis was run for pilot points.
	 */
	public boolean isPreffas() {
		return preffas_;
	}

	/**
	 * Returns true if linear prop. analysis was run for pilot points.
	 *
	 * @return True if linear prop. analysis was run for pilot points.
	 */
	public boolean isLinear() {
		return linear_;
	}

	@Override
	public SpectrumItem getParentItem() {
		return null;
	}

	@Override
	public IconicFont getIconFont() {
		return IconicFont.FONTAWESOME;
	}

	/**
	 * Creates and returns context menu for ANA files.
	 *
	 * @param multipleSelection
	 *            True if multiple ANA files are selected.
	 * @param handler
	 *            Action handler.
	 * @param selected
	 *            Selected files.
	 * @return Context menu.
	 */
	public static ContextMenu createContextMenu(boolean multipleSelection, ActionHandler handler, ObservableList<TreeItem<String>> selected) {

		// create menu
		ContextMenu contextMenu = new ContextMenu();

		// single selection
		if (!multipleSelection) {

			// get selected RFORT item
			Rfort rfort = (Rfort) selected.get(0);

			// plot RFORT results
			Label resultIcon = new Label("\ue901");
			resultIcon.getStylesheets().add(IconicFont.CUSTOM.getStyleSheet());
			Menu resultsMenu = new Menu("Plot RFORT Results", resultIcon);
			contextMenu.getItems().add(resultsMenu);

			// plot fatigue equivalent stresses
			Label fatigueIcon1 = new Label("\uf10c");
			fatigueIcon1.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem fatResults = new MenuItem("Fatigue Results", fatigueIcon1);
			fatResults.setId("plotRfortFatigueResults");
			fatResults.setOnAction(handler);
			fatResults.setDisable(!rfort.isFatigue());
			resultsMenu.getItems().add(fatResults);

			// plot preffas equivalent stresses
			Label preffasIcon1 = new Label("\uf192");
			preffasIcon1.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem prefResults = new MenuItem("Preffas Results", preffasIcon1);
			prefResults.setId("plotRfortPreffasResults");
			prefResults.setOnAction(handler);
			prefResults.setDisable(!rfort.isPreffas());
			resultsMenu.getItems().add(prefResults);

			// plot linear equivalent stresses
			Label linearIcon1 = new Label("\uf111");
			linearIcon1.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem linResults = new MenuItem("Linear Prop. Results", linearIcon1);
			linResults.setId("plotRfortLinearResults");
			linResults.setOnAction(handler);
			linResults.setDisable(!rfort.isLinear());
			resultsMenu.getItems().add(linResults);

			// plot average number of peaks
			Label numPeaksIcon = new Label("\uf080");
			numPeaksIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem numPeaks = new MenuItem("Plot Average Number of Peaks", numPeaksIcon);
			numPeaks.setId("plotRfortPeaks");
			numPeaks.setOnAction(handler);
			contextMenu.getItems().add(numPeaks);

			// plot equivalent stress deviations
			Label stressIcon = new Label("\uedaf");
			stressIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			Menu stressesMenu = new Menu("Plot Equivalent Stresses...", stressIcon);
			contextMenu.getItems().add(stressesMenu);

			// plot fatigue equivalent stresses
			Label fatigueIcon = new Label("\uf10c");
			fatigueIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem fatEqStress = new MenuItem("Fatigue Equivalent Stresses", fatigueIcon);
			fatEqStress.setId("plotRfortFatigueStresses");
			fatEqStress.setOnAction(handler);
			fatEqStress.setDisable(!rfort.isFatigue());
			stressesMenu.getItems().add(fatEqStress);

			// plot preffas equivalent stresses
			Label preffasIcon = new Label("\uf192");
			preffasIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem prefEqStress = new MenuItem("Preffas Equivalent Stresses", preffasIcon);
			prefEqStress.setId("plotRfortPreffasStresses");
			prefEqStress.setOnAction(handler);
			prefEqStress.setDisable(!rfort.isPreffas());
			stressesMenu.getItems().add(prefEqStress);

			// plot linear equivalent stresses
			Label linearIcon = new Label("\uf111");
			linearIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem linEqStress = new MenuItem("Linear Prop. Equivalent Stresses", linearIcon);
			linEqStress.setId("plotRfortLinearStresses");
			linEqStress.setOnAction(handler);
			linEqStress.setDisable(!rfort.isLinear());
			stressesMenu.getItems().add(linEqStress);

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());

			// add omissions
			Label addOmissionLevelsIcon = new Label("\uf055");
			addOmissionLevelsIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem addOmissionLevels = new MenuItem("Add Omissions", addOmissionLevelsIcon);
			addOmissionLevels.setId("addRfortOmissions");
			addOmissionLevels.setOnAction(handler);
			contextMenu.getItems().add(addOmissionLevels);

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

		// share
		if (!multipleSelection) {
			Label shareIcon = new Label("\uf064");
			shareIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem share = new MenuItem("Share RFORT Report", shareIcon);
			share.setId("shareRfortReport");
			share.setOnAction(handler);
			contextMenu.getItems().add(share);

			// generate RFORT report
			Label reportIcon = new Label("\uf1c1");
			reportIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem generateReport = new MenuItem("Generate RFORT Report", reportIcon);
			generateReport.setId("generateRfortReport");
			generateReport.setOnAction(handler);
			contextMenu.getItems().add(generateReport);

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());
		}

		// hide
		Label hideIcon = new Label("\uf070");
		hideIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem hide = new MenuItem("Hide", hideIcon);
		hide.setId("hide");
		hide.setOnAction(handler);
		contextMenu.getItems().add(hide);

		// delete
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
