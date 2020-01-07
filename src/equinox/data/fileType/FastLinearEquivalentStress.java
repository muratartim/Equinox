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
 * Class for fast linear equivalent stress.
 *
 * @author Murat Artim
 * @date Jun 14, 2016
 * @time 8:22:37 PM
 */
public class FastLinearEquivalentStress extends SpectrumItem {

	/** Omission level. */
	private final double omissionLevel_;

	/** Material name. */
	private final String materialName_;

	/**
	 * Creates fast linear propagation equivalent stress item.
	 *
	 * @param name
	 *            Name of equivalent stress.
	 * @param id
	 *            ID of item.
	 * @param omissionLevel
	 *            The omission level. -1 should be given if no omission is applied.
	 * @param materialName
	 *            Material name.
	 */
	public FastLinearEquivalentStress(String name, int id, double omissionLevel, String materialName) {

		// create spectrum item
		super(name, id);
		omissionLevel_ = omissionLevel;
		materialName_ = materialName;

		// create icon label
		final Label iconLabel = new Label("\uf111");
		iconLabel.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		setGraphic(iconLabel);
	}

	/**
	 * Returns the omission level. Note that, this will return -1 if no omission is applied.
	 *
	 * @return The omission level.
	 */
	public double getOmissionLevel() {
		return omissionLevel_;
	}

	/**
	 * Returns the material name.
	 *
	 * @return The material name.
	 */
	public String getMaterialName() {
		return materialName_;
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
	 * Creates and returns context menu for ANA files.
	 *
	 * @param multipleSelection
	 *            True if multiple ANA files are selected.
	 * @param handler
	 *            Action handler.
	 * @param is3dEnabled
	 *            True if 3D viewer is enabled.
	 * @return Context menu.
	 */
	public static ContextMenu createContextMenu(boolean multipleSelection, ActionHandler handler, boolean is3dEnabled) {

		// create menu
		final ContextMenu contextMenu = new ContextMenu();

		// multiple selection
		if (multipleSelection) {

			// compare equivalent stresses
			final Label statisticsIcon = new Label("\uf24e");
			statisticsIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			final MenuItem compareStresses = new MenuItem("Compare Equivalent Stresses", statisticsIcon);
			compareStresses.setId("compareEquivalentStress");
			compareStresses.setOnAction(handler);
			contextMenu.getItems().add(compareStresses);

			// generate life factors
			final Label generateLifeFactorsIcon = new Label("\ueb84");
			generateLifeFactorsIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			final MenuItem generateLifeFactors = new MenuItem("Generate Life/Adjustment Factors", generateLifeFactorsIcon);
			generateLifeFactors.setId("generateLifeFactor");
			generateLifeFactors.setOnAction(handler);
			contextMenu.getItems().add(generateLifeFactors);

			// generate equivalent stress ratios
			final Label generateStressRatiosIcon = new Label("\ue902");
			generateStressRatiosIcon.getStylesheets().add(IconicFont.CUSTOM.getStyleSheet());
			final MenuItem generateStressRatios = new MenuItem("Generate Equivalent Stress Ratios", generateStressRatiosIcon);
			generateStressRatios.setId("generateStressRatio");
			generateStressRatios.setOnAction(handler);
			contextMenu.getItems().add(generateStressRatios);

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());

			// generate mission profile plots
			final Label mpIcon = new Label("\uf1fe");
			mpIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			final MenuItem plotMp = new MenuItem("Generate Mission Profile Plots", mpIcon);
			plotMp.setId("generateFastMissionProfile");
			plotMp.setOnAction(handler);
			contextMenu.getItems().add(plotMp);

			// generate level crossing plots
			final Label lcIcon = new Label("\ue901");
			lcIcon.getStylesheets().add(IconicFont.CUSTOM.getStyleSheet());
			final MenuItem plotLc = new MenuItem("Generate Level Crossing Plots", lcIcon);
			plotLc.setId("generateFastLevelCrossing");
			plotLc.setOnAction(handler);
			contextMenu.getItems().add(plotLc);

			// generate typical flight plots
			final Label tfIcon = new Label("\uec04");
			tfIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			final Menu plotTf = new Menu("Generate Typical Flight Plots...", tfIcon);
			contextMenu.getItems().add(plotTf);

			// longest typical flight
			final MenuItem longest = new MenuItem("Longest Flight");
			longest.setId("generateFastLongestFlight");
			longest.setOnAction(handler);
			plotTf.getItems().add(longest);

			// highest occurring typical flight
			final MenuItem ho = new MenuItem("Flight with Highest Occurrence");
			ho.setId("generateFastHOFlight");
			ho.setOnAction(handler);
			plotTf.getItems().add(ho);

			// highest total stress typical flight
			final MenuItem hs = new MenuItem("Flight with Highest Total Stress");
			hs.setId("generateFastHSFlight");
			hs.setOnAction(handler);
			plotTf.getItems().add(hs);

			// plot statistics
			final Label stIcon = new Label("\uf080");
			stIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			final Menu plotSt = new Menu("Generate Statistics Plots...", stIcon);
			contextMenu.getItems().add(plotSt);

			// typical flight number of peaks
			final MenuItem numPeaks = new MenuItem("Typical Flight Number of Peaks");
			numPeaks.setId("generateFastNumPeaks");
			numPeaks.setOnAction(handler);
			plotSt.getItems().add(numPeaks);

			// typical flight occurrences
			final MenuItem occur = new MenuItem("Typical Flight Occurrences");
			occur.setId("generateFastOccurrences");
			occur.setOnAction(handler);
			plotSt.getItems().add(occur);

			// rainflow histogram
			final MenuItem hist = new MenuItem("Rainflow Histogram");
			hist.setId("generateFastHistogram");
			hist.setOnAction(handler);
			plotSt.getItems().add(hist);

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());
		}

		// single selection
		else {

			// plot mission profile
			final Label mpIcon = new Label("\uf1fe");
			mpIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			final MenuItem plotMp = new MenuItem("Plot Mission Profile", mpIcon);
			plotMp.setId("plotFastMissionProfile");
			plotMp.setOnAction(handler);
			contextMenu.getItems().add(plotMp);

			// plot level crossing
			final Label lcIcon = new Label("\ue901");
			lcIcon.getStylesheets().add(IconicFont.CUSTOM.getStyleSheet());
			final MenuItem plotLc = new MenuItem("Plot Level Crossings", lcIcon);
			plotLc.setId("plotFastLevelCrossing");
			plotLc.setOnAction(handler);
			contextMenu.getItems().add(plotLc);

			// plot typical flights
			final Label tfIcon = new Label("\uec04");
			tfIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			final Menu plotTf = new Menu("Plot Typical Flight...", tfIcon);
			contextMenu.getItems().add(plotTf);

			// longest typical flight
			final MenuItem longest = new MenuItem("Longest Flight");
			longest.setId("plotFastLongestFlight");
			longest.setOnAction(handler);
			plotTf.getItems().add(longest);

			// highest occurring typical flight
			final MenuItem ho = new MenuItem("Flight with Highest Occurrence");
			ho.setId("plotFastHOFlight");
			ho.setOnAction(handler);
			plotTf.getItems().add(ho);

			// highest total stress typical flight
			final MenuItem hs = new MenuItem("Flight with Highest Total Stress");
			hs.setId("plotFastHSFlight");
			hs.setOnAction(handler);
			plotTf.getItems().add(hs);

			// plot statistics
			final Label stIcon = new Label("\uf080");
			stIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			final Menu plotSt = new Menu("Plot Statistics...", stIcon);
			contextMenu.getItems().add(plotSt);

			// typical flight number of peaks
			final MenuItem numPeaks = new MenuItem("Typical Flight Number of Peaks");
			numPeaks.setId("plotFastNumPeaks");
			numPeaks.setOnAction(handler);
			plotSt.getItems().add(numPeaks);

			// typical flight occurrences
			final MenuItem occur = new MenuItem("Typical Flight Occurrences");
			occur.setId("plotFastOccurrences");
			occur.setOnAction(handler);
			plotSt.getItems().add(occur);

			// rainflow histogram
			final MenuItem hist = new MenuItem("Rainflow Histogram");
			hist.setId("plotFastHistogram");
			hist.setOnAction(handler);
			plotSt.getItems().add(hist);

			// show analysis output file
			final Label outputIcon = new Label("\ueec6");
			outputIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			final MenuItem output = new MenuItem("Show Analysis Output File", outputIcon);
			output.setId("showAnalysisOutputFile");
			output.setOnAction(handler);
			contextMenu.getItems().add(output);

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());
		}

		// create share menu
		final Label shareIcon = new Label("\uf064");
		shareIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		final Menu share = new Menu("Share...", shareIcon);
		contextMenu.getItems().add(share);

		// share equivalent stress
		Label eqStressIcon = new Label("\uedaf");
		eqStressIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		final MenuItem shareStress = new MenuItem(multipleSelection ? "Equivalent Stresses" : "Equivalent Stress", eqStressIcon);
		shareStress.setId("shareEquivalentStress");
		shareStress.setOnAction(handler);
		share.getItems().add(shareStress);

		// share analysis output file
		if (!multipleSelection) {
			final Label outputIcon = new Label("\ueec6");
			outputIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			final MenuItem shareOutput = new MenuItem("Analysis Output File", outputIcon);
			shareOutput.setId("share");
			shareOutput.setOnAction(handler);
			share.getItems().add(shareOutput);
		}

		// multiple selection
		if (multipleSelection) {

			// share life factors
			final Label generateLifeFactorsIcon = new Label("\ueb84");
			generateLifeFactorsIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			final MenuItem shareLifeFactors = new MenuItem("Life/Adjustment Factors", generateLifeFactorsIcon);
			shareLifeFactors.setId("shareLifeFactors");
			shareLifeFactors.setOnAction(handler);
			share.getItems().add(shareLifeFactors);

			// share equivalent stress ratios
			final Label generateStressRatiosIcon = new Label("\ue902");
			generateStressRatiosIcon.getStylesheets().add(IconicFont.CUSTOM.getStyleSheet());
			final MenuItem shareStressRatios = new MenuItem("Equivalent Stress Ratios", generateStressRatiosIcon);
			shareStressRatios.setId("shareStressRatios");
			shareStressRatios.setOnAction(handler);
			share.getItems().add(shareStressRatios);
		}

		// create save menu
		final Label saveIcon = new Label("\uf0c7");
		saveIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		final Menu save = new Menu("Save...", saveIcon);
		contextMenu.getItems().add(save);

		// save equivalent stress
		Label eqStressIcon2 = new Label("\uedaf");
		eqStressIcon2.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		final MenuItem saveStress = new MenuItem(multipleSelection ? "Equivalent Stresses" : "Equivalent Stress", eqStressIcon2);
		saveStress.setId("saveEqStress");
		saveStress.setOnAction(handler);
		save.getItems().add(saveStress);

		// save analysis output file
		final Label outputIcon = new Label("\ueec6");
		outputIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		final MenuItem outputFile = new MenuItem(multipleSelection ? "Analysis Output Files" : "Analysis Output File", outputIcon);
		outputFile.setId("saveAnalysisOutputFile");
		outputFile.setOnAction(handler);
		save.getItems().add(outputFile);

		// multiple selection
		if (multipleSelection) {

			// save life factors
			final Label generateLifeFactorsIcon = new Label("\ueb84");
			generateLifeFactorsIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			final MenuItem saveLifeFactors = new MenuItem("Life/Adjustment Factors", generateLifeFactorsIcon);
			saveLifeFactors.setId("saveLifeFactors");
			saveLifeFactors.setOnAction(handler);
			save.getItems().add(saveLifeFactors);

			// save equivalent stress ratios
			final Label generateStressRatiosIcon = new Label("\ue902");
			generateStressRatiosIcon.getStylesheets().add(IconicFont.CUSTOM.getStyleSheet());
			final MenuItem saveStressRatios = new MenuItem("Equivalent Stress Ratios", generateStressRatiosIcon);
			saveStressRatios.setId("saveStressRatios");
			saveStressRatios.setOnAction(handler);
			save.getItems().add(saveStressRatios);
		}

		// find
		contextMenu.getItems().add(new SeparatorMenuItem());
		final Label findIcon = new Label("\uf002");
		findIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		final MenuItem find = new MenuItem("Find Similar Files", findIcon);
		find.setId("find");
		find.setOnAction(handler);
		contextMenu.getItems().add(find);

		// rename
		final Label renameIcon = new Label("\uf246");
		renameIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		final MenuItem rename = new MenuItem("Rename...", renameIcon);
		rename.setId("rename");
		rename.setOnAction(handler);
		contextMenu.getItems().add(rename);

		// delete
		contextMenu.getItems().add(new SeparatorMenuItem());
		final Label deleteIcon = new Label("\uf014");
		deleteIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		final MenuItem remove = new MenuItem("Delete", deleteIcon);
		remove.setId("delete");
		remove.setOnAction(handler);
		contextMenu.getItems().add(remove);

		// return menu
		return contextMenu;
	}
}
