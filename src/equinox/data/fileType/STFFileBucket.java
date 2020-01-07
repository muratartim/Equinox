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
 * Class for STF file bucket.
 *
 * @author Murat Artim
 * @date 24 Aug 2016
 * @time 11:11:50
 */
public class STFFileBucket extends SpectrumItem {

	/** Number of STF files in the bucket. */
	private int numSTFs_;

	/**
	 * Creates STF file bucket.
	 *
	 * @param spectrumID
	 *            The owner spectrum ID.
	 * @param numSTFs
	 *            Number of STF files in the bucket.
	 */
	public STFFileBucket(int spectrumID, int numSTFs) {

		// create spectrum item
		super(createName(numSTFs), spectrumID);

		// set number of STFs
		numSTFs_ = numSTFs;

		// create icon label
		Label iconLabel = new Label("\uf16c");
		iconLabel.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		setGraphic(iconLabel);
	}

	@Override
	public Spectrum getParentItem() {
		return (Spectrum) getParent();
	}

	@Override
	public IconicFont getIconFont() {
		return IconicFont.FONTAWESOME;
	}

	/**
	 * Returns number of STF files in the bucket.
	 *
	 * @return Number of STF files in the bucket.
	 */
	public int getNumberOfSTFs() {
		return numSTFs_;
	}

	/**
	 * Sets number of STF files in the bucket.
	 *
	 * @param numSTFs
	 *            Number of STF files in the bucket.
	 */
	public void setNumberOfSTFs(int numSTFs) {
		numSTFs_ = numSTFs;
		setName(createName(numSTFs_));
	}

	/**
	 * Creates and returns name of item.
	 *
	 * @param numSTFs
	 *            Number of STF files in the bucket.
	 * @return Name of item.
	 */
	private static String createName(int numSTFs) {
		return numSTFs + " STF files";
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

		// single selection
		if (!multipleSelection) {

			// find
			Label findIcon = new Label("\uecac");
			findIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			MenuItem find = new MenuItem("Show Pilot Points", findIcon);
			find.setId("showSTFs");
			find.setOnAction(handler);
			contextMenu.getItems().add(find);

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());
		}

		// equivalent stress analysis
		Label eqStressIcon = new Label("\uedaf");
		eqStressIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem eqStress = new MenuItem("Equivalent Stress Analysis", eqStressIcon);
		eqStress.setId(multipleSelection ? "equivalentStressSTFNoEventModifiers" : "equivalentStressSTF");
		eqStress.setOnAction(handler);
		contextMenu.getItems().add(eqStress);

		// damage angle analysis
		Label damageAngleIcon = new Label("\ueab0");
		damageAngleIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem damageAngle = new MenuItem("Damage Angle Analysis", damageAngleIcon);
		damageAngle.setId(multipleSelection ? "damageAngleNoEventModifiers" : "damageAngle");
		damageAngle.setOnAction(handler);
		contextMenu.getItems().add(damageAngle);

		// single selection
		if (!multipleSelection) {

			// damage contribution analysis
			Label dameContIcon = new Label("\uf200");
			dameContIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem dameCont = new MenuItem("Damage Contribution Analysis", dameContIcon);
			dameCont.setId("damageContribution");
			dameCont.setOnAction(handler);
			contextMenu.getItems().add(dameCont);
		}

		// add separator
		contextMenu.getItems().add(new SeparatorMenuItem());

		// set pilot point materials
		Label materialIcon = new Label("\uebe7");
		materialIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem setMaterial = new MenuItem("Set Materials...", materialIcon);
		setMaterial.setId("setMaterial");
		setMaterial.setOnAction(handler);
		contextMenu.getItems().add(setMaterial);

		// add separator
		contextMenu.getItems().add(new SeparatorMenuItem());

		// single selection
		if (!multipleSelection) {

			// create share menu
			Label shareIcon = new Label("\uf064");
			shareIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			Menu shareMenu = new Menu("Share...", shareIcon);
			contextMenu.getItems().add(shareMenu);

			// share STF files
			Label stfIcon = new Label("\uf1c4");
			stfIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem shareSTF = new MenuItem("STF Files", stfIcon);
			shareSTF.setId("shareBucketSTFs");
			shareSTF.setOnAction(handler);
			shareMenu.getItems().add(shareSTF);

			// add separator
			shareMenu.getItems().add(new SeparatorMenuItem());

			// share fatigue analysis output files
			Label outputIcon = new Label("\ueec6");
			outputIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			MenuItem shareFatigueOutput = new MenuItem("Fatigue Analysis Output Files", outputIcon);
			shareFatigueOutput.setId("shareBucketFatigueAnalysisOutputFiles");
			shareFatigueOutput.setOnAction(handler);
			shareMenu.getItems().add(shareFatigueOutput);

			// share preffas analysis output files
			MenuItem sharePreffasOutput = new MenuItem("Preffas Prop. Analysis Output Files");
			sharePreffasOutput.setId("shareBucketPreffasAnalysisOutputFiles");
			sharePreffasOutput.setOnAction(handler);
			shareMenu.getItems().add(sharePreffasOutput);

			// share linear analysis output files
			MenuItem shareLinearOutput = new MenuItem("Linear Prop. Analysis Output Files");
			shareLinearOutput.setId("shareBucketLinearAnalysisOutputFiles");
			shareLinearOutput.setOnAction(handler);
			shareMenu.getItems().add(shareLinearOutput);

			// add separator
			shareMenu.getItems().add(new SeparatorMenuItem());

			// share fatigue equivalent stresses
			Label fatigueIcon = new Label("\uf10c");
			fatigueIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem shareFat = new MenuItem("Fatigue Equivalent Stresses", fatigueIcon);
			shareFat.setId("shareBucketFatigueEqStresses");
			shareFat.setOnAction(handler);
			shareMenu.getItems().add(shareFat);

			// share preffas equivalent stresses
			Label preffasIcon = new Label("\uf192");
			preffasIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem sharePref = new MenuItem("Preffas Prop. Equivalent Stresses", preffasIcon);
			sharePref.setId("shareBucketPreffasEqStresses");
			sharePref.setOnAction(handler);
			shareMenu.getItems().add(sharePref);

			// share linear equivalent stresses
			Label linearIcon = new Label("\uf111");
			linearIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem shareLin = new MenuItem("Linear Prop. Equivalent Stresses", linearIcon);
			shareLin.setId("shareBucketLinearEqStresses");
			shareLin.setOnAction(handler);
			shareMenu.getItems().add(shareLin);

			// add separator
			shareMenu.getItems().add(new SeparatorMenuItem());

			// share damage angles
			Label damageAngleIcon2 = new Label("\ueab0");
			damageAngleIcon2.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			MenuItem shareAngle = new MenuItem("Damage Angles", damageAngleIcon2);
			shareAngle.setId("shareDamageAngle");
			shareAngle.setOnAction(handler);
			shareMenu.getItems().add(shareAngle);

			// share damage contributions
			Label dameContIcon2 = new Label("\uf200");
			dameContIcon2.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem shareCont = new MenuItem("Loadcase Damage Contributions", dameContIcon2);
			shareCont.setId("shareDamageContributions");
			shareCont.setOnAction(handler);
			shareMenu.getItems().add(shareCont);

			// share flight damage contributions
			Label dameContIcon3 = new Label("\ueb7d");
			dameContIcon3.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			MenuItem shareCont2 = new MenuItem("Typical Flight Damage Contributions", dameContIcon3);
			shareCont2.setId("shareFlightDamageContributions");
			shareCont2.setOnAction(handler);
			shareMenu.getItems().add(shareCont2);
		}

		// create save menu
		Label saveIcon = new Label("\uf0c7");
		saveIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		Menu saveMenu = new Menu("Save...", saveIcon);
		contextMenu.getItems().add(saveMenu);

		// save STF files
		Label stfIcon2 = new Label("\uf1c4");
		stfIcon2.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem saveSTF = new MenuItem("STF Files", stfIcon2);
		saveSTF.setId("saveSTFBucket");
		saveSTF.setOnAction(handler);
		saveMenu.getItems().add(saveSTF);

		// add separator
		saveMenu.getItems().add(new SeparatorMenuItem());

		// save fatigue analysis output files
		Label outputIcon = new Label("\ueec6");
		outputIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem saveFatigueOutput = new MenuItem("Fatigue Analysis Output Files", outputIcon);
		saveFatigueOutput.setId("saveBucketFatigueAnalysisOutputFiles");
		saveFatigueOutput.setOnAction(handler);
		saveMenu.getItems().add(saveFatigueOutput);

		// save preffas propagation analysis output files
		MenuItem savePreffasOutput = new MenuItem("Preffas Prop. Analysis Output Files");
		savePreffasOutput.setId("saveBucketPreffasAnalysisOutputFiles");
		savePreffasOutput.setOnAction(handler);
		saveMenu.getItems().add(savePreffasOutput);

		// save linear propagation analysis output files
		MenuItem saveLinearOutput = new MenuItem("Linear Prop. Analysis Output Files");
		saveLinearOutput.setId("saveBucketLinearAnalysisOutputFiles");
		saveLinearOutput.setOnAction(handler);
		saveMenu.getItems().add(saveLinearOutput);

		// add separator
		saveMenu.getItems().add(new SeparatorMenuItem());

		// save fatigue equivalent stresses
		Label fatigueIcon2 = new Label("\uf10c");
		fatigueIcon2.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem saveFat = new MenuItem("Fatigue Equivalent Stresses", fatigueIcon2);
		saveFat.setId("saveBucketFatigueEqStresses");
		saveFat.setOnAction(handler);
		saveMenu.getItems().add(saveFat);

		// save preffas equivalent stresses
		Label preffasIcon2 = new Label("\uf192");
		preffasIcon2.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem savePref = new MenuItem("Preffas Prop. Equivalent Stresses", preffasIcon2);
		savePref.setId("saveBucketPreffasEqStresses");
		savePref.setOnAction(handler);
		saveMenu.getItems().add(savePref);

		// save linear equivalent stresses
		Label linearIcon2 = new Label("\uf111");
		linearIcon2.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem saveLin = new MenuItem("Linear Prop. Equivalent Stresses", linearIcon2);
		saveLin.setId("saveBucketLinearEqStresses");
		saveLin.setOnAction(handler);
		saveMenu.getItems().add(saveLin);

		// add separator
		saveMenu.getItems().add(new SeparatorMenuItem());

		// save damage angles
		Label damageAngleIcon3 = new Label("\ueab0");
		damageAngleIcon3.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem saveAngle = new MenuItem("Damage Angles", damageAngleIcon3);
		saveAngle.setId("saveDamageAngle");
		saveAngle.setOnAction(handler);
		saveMenu.getItems().add(saveAngle);

		// save damage contributions
		Label dameContIcon3 = new Label("\uf200");
		dameContIcon3.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem saveCont = new MenuItem("Loadcase Damage Contributions", dameContIcon3);
		saveCont.setId("saveDamageContributions");
		saveCont.setOnAction(handler);
		saveMenu.getItems().add(saveCont);

		// save typical flight damage contributions
		Label dameContIcon4 = new Label("\ueb7d");
		dameContIcon4.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem saveCont2 = new MenuItem("Typical Flight Damage Contributions", dameContIcon4);
		saveCont2.setId("saveFlightDamageContributions");
		saveCont2.setOnAction(handler);
		saveMenu.getItems().add(saveCont2);

		// add separator
		contextMenu.getItems().add(new SeparatorMenuItem());

		// export pilot points
		Label exportIcon = new Label("\uea98");
		exportIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem export = new MenuItem("Export Pilot Points...", exportIcon);
		export.setId("exportMultipleSTFs");
		export.setOnAction(handler);
		contextMenu.getItems().add(export);

		// export damage contributions
		MenuItem exportDC = new MenuItem("Export Damage Contributions...");
		exportDC.setId("exportDamageContributions");
		exportDC.setOnAction(handler);
		contextMenu.getItems().add(exportDC);

		// add separator
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
