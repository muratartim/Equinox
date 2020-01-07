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
import equinox.plugin.FileType;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;

/**
 * Class for STF file.
 *
 * @author Murat Artim
 * @date Jan 20, 2014
 * @time 2:40:44 PM
 */
public class STFFile extends SpectrumItem {

	/** True if 2D stress state. */
	private final boolean is2d_;

	/** Fatigue mission. */
	private String mission_, eid_ = null;

	/** Stress table ID. */
	private final int stressTableID_;

	/**
	 * Creates STF file.
	 *
	 * @param fileName
	 *            File name.
	 * @param fileID
	 *            File ID.
	 * @param is2d
	 *            True if 2D stress state.
	 * @param stressTableID
	 *            Stress table ID.
	 */
	public STFFile(String fileName, int fileID, boolean is2d, int stressTableID) {

		// create spectrum item
		super(fileName, fileID);

		// set attributes
		is2d_ = is2d;

		// set stress table ID
		stressTableID_ = stressTableID;

		// create icon label
		Label iconLabel = new Label("\uf1c4");
		iconLabel.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		setGraphic(iconLabel);
	}

	/**
	 * Sets element ID.
	 *
	 * @param eid
	 *            Element ID.
	 */
	public void setEID(String eid) {
		eid_ = eid;
	}

	/**
	 * Sets fatigue mission.
	 *
	 * @param mission
	 *            Fatigue mission.
	 */
	public void setMission(String mission) {
		mission_ = mission;
	}

	/**
	 * Returns true if 2D stress state.
	 *
	 * @return True if 2D stress state.
	 */
	public boolean is2D() {
		return is2d_;
	}

	/**
	 * Returns the stress table ID.
	 *
	 * @return The stress table ID.
	 */
	public int getStressTableID() {
		return stressTableID_;
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
	 * Returns the stress sequences of this STF file or null if there are no stress sequences.
	 *
	 * @return The stress sequences of this STF file or null if there are no stress sequences.
	 */
	public ArrayList<StressSequence> getStressSequences() {
		ArrayList<StressSequence> spectra = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof StressSequence) {
				if (spectra == null) {
					spectra = new ArrayList<>();
				}
				spectra.add((StressSequence) item);
			}
		}
		return spectra;
	}

	/**
	 * Returns the damage angles of this STF file or null if there are no angles.
	 *
	 * @return The damage angles of this STF file or null if there are no angles.
	 */
	public ArrayList<DamageAngle> getDamageAngles() {
		ArrayList<DamageAngle> angles = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof DamageAngle) {
				if (angles == null) {
					angles = new ArrayList<>();
				}
				angles.add((DamageAngle) item);
			}
		}
		return angles;
	}

	/**
	 * Returns the loadcase damage contributions of this STF file or null if there are no contributions.
	 *
	 * @return The loadcase damage contributions of this STF file or null if there are no contributions.
	 */
	public ArrayList<LoadcaseDamageContributions> getLoadcaseDamageContributions() {
		ArrayList<LoadcaseDamageContributions> contributions = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof LoadcaseDamageContributions) {
				if (contributions == null) {
					contributions = new ArrayList<>();
				}
				contributions.add((LoadcaseDamageContributions) item);
			}
		}
		return contributions;
	}

	/**
	 * Returns the typical flight damage contributions of this STF file or null if there are no contributions.
	 *
	 * @return The typical flight damage contributions of this STF file or null if there are no contributions.
	 */
	public ArrayList<FlightDamageContributions> getFlightDamageContributions() {
		ArrayList<FlightDamageContributions> contributions = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof FlightDamageContributions) {
				if (contributions == null) {
					contributions = new ArrayList<>();
				}
				contributions.add((FlightDamageContributions) item);
			}
		}
		return contributions;
	}

	/**
	 * Returns the fast fatigue equivalent stresses of this stress sequence or null if there are no fast fatigue equivalent stresses.
	 *
	 * @return The fast fatigue equivalent stresses of this stress sequence or null if there are no fast fatigue equivalent stresses.
	 */
	public ArrayList<FastFatigueEquivalentStress> getFastFatigueEquivalentStresses() {
		ArrayList<FastFatigueEquivalentStress> eqStresses = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof FastFatigueEquivalentStress) {
				if (eqStresses == null) {
					eqStresses = new ArrayList<>();
				}
				eqStresses.add((FastFatigueEquivalentStress) item);
			}
		}
		return eqStresses;
	}

	/**
	 * Returns the fast Preffas equivalent stresses of this stress sequence or null if there are no fast Preffas equivalent stresses.
	 *
	 * @return The fast Preffas equivalent stresses of this stress sequence or null if there are no fast Preffas equivalent stresses.
	 */
	public ArrayList<FastPreffasEquivalentStress> getFastPreffasEquivalentStresses() {
		ArrayList<FastPreffasEquivalentStress> eqStresses = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof FastPreffasEquivalentStress) {
				if (eqStresses == null) {
					eqStresses = new ArrayList<>();
				}
				eqStresses.add((FastPreffasEquivalentStress) item);
			}
		}
		return eqStresses;
	}

	/**
	 * Returns the fast linear equivalent stresses of this stress sequence or null if there are no fast linear equivalent stresses.
	 *
	 * @return The fast linear equivalent stresses of this stress sequence or null if there are no fast linear equivalent stresses.
	 */
	public ArrayList<FastLinearEquivalentStress> getFastLinearEquivalentStresses() {
		ArrayList<FastLinearEquivalentStress> eqStresses = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof FastLinearEquivalentStress) {
				if (eqStresses == null) {
					eqStresses = new ArrayList<>();
				}
				eqStresses.add((FastLinearEquivalentStress) item);
			}
		}
		return eqStresses;
	}

	/**
	 * Returns fatigue mission.
	 *
	 * @return Fatigue mission.
	 */
	public String getMission() {
		return mission_ == null ? getParentItem().getMission() : mission_;
	}

	/**
	 * Returns element ID.
	 *
	 * @return Element ID.
	 */
	public String getEID() {
		return eid_;
	}

	/**
	 * Extracts and returns EID from file name (if EID exists).
	 *
	 * @param fileName
	 *            STF file name.
	 * @return EID or null if no EID found in file name.
	 */
	public static String getEID(String fileName) {

		// initialize EID
		String eid = null;

		// get file name without extension
		fileName = FileType.getNameWithoutExtension(fileName);

		// to upper case
		fileName = fileName.toUpperCase().trim();

		// contains underscore
		if (fileName.contains("_")) {

			// split from underscore
			String[] split = fileName.split("_");

			// loop over parts
			for (String part : split) {

				// trim white spaces
				part = part.trim();

				// starts with EID
				if (part.startsWith("EID") && (part.length() > 3)) {
					eid = part.substring(3).trim();
					break;
				}
			}
		}

		// no underscore
		else {

			// starts with EID
			if (fileName.startsWith("EID") && (fileName.length() > 3)) {
				eid = fileName.substring(3).trim();
			}
		}

		// return EID
		return eid;
	}

	/**
	 * Creates and returns context menu for ANA files.
	 *
	 * @param multipleSelection
	 *            True if multiple ANA files are selected.
	 * @param handler
	 *            Action handler.
	 * @param selected
	 *            Selected STF files.
	 * @return Context menu.
	 */
	public static ContextMenu createContextMenu(boolean multipleSelection, ActionHandler handler, ObservableList<TreeItem<String>> selected) {

		// create menu
		ContextMenu contextMenu = new ContextMenu();

		// single selection
		if (!multipleSelection) {

			// edit pilot point info
			Label editIcon = new Label("\uf044");
			editIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem edit = new MenuItem("Edit Pilot Point Info", editIcon);
			edit.setId("editSTFInfo");
			edit.setOnAction(handler);
			contextMenu.getItems().add(edit);
		}

		// set pilot point materials
		Label materialIcon = new Label("\uebe7");
		materialIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem setMaterial = new MenuItem("Set Materials...", materialIcon);
		setMaterial.setId("setMaterial");
		setMaterial.setOnAction(handler);
		contextMenu.getItems().add(setMaterial);

		// single selection
		if (!multipleSelection) {

			// set pilot point image
			Menu setImageMenu = new Menu("Set Pilot Point Info Image...");
			contextMenu.getItems().add(setImageMenu);

			// image
			Label imageIcon = new Label("\uf03e");
			imageIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem setImage = new MenuItem("Pilot Point Image", imageIcon);
			setImage.setId("setSTFImage");
			setImage.setOnAction(handler);
			setImageMenu.getItems().add(setImage);

			// mission profile
			Label mpIcon = new Label("\uf1fe");
			mpIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem setMP = new MenuItem("Mission Profile Plot", mpIcon);
			setMP.setId("setSTFImageMP");
			setMP.setOnAction(handler);
			setImageMenu.getItems().add(setMP);

			// typical flight plots
			Label tfIcon = new Label("\uec04");
			tfIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			Menu setTFMenu = new Menu("Typical Flight Plot...", tfIcon);
			setImageMenu.getItems().add(setTFMenu);

			// longest flight
			MenuItem setTFLongest = new MenuItem("Longest Typical Flight");
			setTFLongest.setId("setSTFImageTFL");
			setTFLongest.setOnAction(handler);
			setTFMenu.getItems().add(setTFLongest);

			// flight with highest occurrence
			MenuItem setTFHO = new MenuItem("Typical Flight With Highest Occurrence");
			setTFHO.setId("setSTFImageTFHO");
			setTFHO.setOnAction(handler);
			setTFMenu.getItems().add(setTFHO);

			// flight with highest total stress
			MenuItem setTFHS = new MenuItem("Typical Flight With Highest Total Stress");
			setTFHS.setId("setSTFImageTFHS");
			setTFHS.setOnAction(handler);
			setTFMenu.getItems().add(setTFHS);

			// level crossing
			Label lcIcon = new Label("\ue901");
			lcIcon.getStylesheets().add(IconicFont.CUSTOM.getStyleSheet());
			MenuItem setLC = new MenuItem("Level Crossings Plot", lcIcon);
			setLC.setId("setSTFImageLC");
			setLC.setOnAction(handler);
			setImageMenu.getItems().add(setLC);

			// damage angle
			Label daIcon = new Label("\ueab0");
			daIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			MenuItem setDA = new MenuItem("Damage Angles Plot", daIcon);
			setDA.setId("setSTFImageDA");
			setDA.setOnAction(handler);
			setImageMenu.getItems().add(setDA);

			// statistic plots
			Label stIcon = new Label("\uf080");
			stIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			Menu setSTMenu = new Menu("Statistics Plot...", stIcon);
			setImageMenu.getItems().add(setSTMenu);

			// number of peaks
			MenuItem setSTNOP = new MenuItem("Typical Flight Number Of Peaks");
			setSTNOP.setId("setSTFImageSTNOP");
			setSTNOP.setOnAction(handler);
			setSTMenu.getItems().add(setSTNOP);

			// typical flight occurrences
			MenuItem setSTFO = new MenuItem("Typical Flight Occurrences");
			setSTFO.setId("setSTFImageSTFO");
			setSTFO.setOnAction(handler);
			setSTMenu.getItems().add(setSTFO);

			// rainflow histogram
			MenuItem setSTRH = new MenuItem("Rainflow Histogram");
			setSTRH.setId("setSTFImageSTRH");
			setSTRH.setOnAction(handler);
			setSTMenu.getItems().add(setSTRH);

			// damage contribution plots
			Label dcIcon = new Label("\uf200");
			dcIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			Menu setDCMenu = new Menu("Damage Contribution Plot...", dcIcon);
			setImageMenu.getItems().add(setDCMenu);

			// loadcase damage contribution
			MenuItem setLoadcaseDC = new MenuItem("Loadcase Damage Contributions");
			setLoadcaseDC.setId("setSTFImageDC");
			setLoadcaseDC.setOnAction(handler);
			setDCMenu.getItems().add(setLoadcaseDC);

			// typical flight damage contribution
			MenuItem setFlightDC = new MenuItem("Typical Flight Damage Contributions");
			setFlightDC.setId("setSTFImageTFDC");
			setFlightDC.setOnAction(handler);
			setDCMenu.getItems().add(setFlightDC);

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());

			// override fatigue mission
			Label overrideMissionIcon = new Label("\uf040");
			overrideMissionIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem overrideMission = new MenuItem("Override Mission", overrideMissionIcon);
			overrideMission.setId("overrideMission");
			overrideMission.setOnAction(handler);
			contextMenu.getItems().add(overrideMission);

			// override mission parameters
			Label missionParametersIcon = new Label("\uf1de");
			missionParametersIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem missionParameters = new MenuItem("Override Mission Parameters", missionParametersIcon);
			missionParameters.setId("overrideMissionParameters");
			missionParameters.setOnAction(handler);
			contextMenu.getItems().add(missionParameters);
		}

		// add separator
		contextMenu.getItems().add(new SeparatorMenuItem());

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
			MenuItem share = new MenuItem("Share", shareIcon);
			share.setId("share");
			share.setOnAction(handler);
			contextMenu.getItems().add(share);
		}

		// save
		Label saveIcon = new Label("\uf0c7");
		saveIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem save = new MenuItem("Save As...", saveIcon);
		save.setId("saveSTF");
		save.setOnAction(handler);
		contextMenu.getItems().add(save);

		// save as 1D STF
		if (!multipleSelection && ((STFFile) selected.get(0)).is2D()) {
			MenuItem saveAs1D = new MenuItem("Save As 1D STF...");
			saveAs1D.setId("saveAs1D");
			saveAs1D.setOnAction(handler);
			contextMenu.getItems().add(saveAs1D);
		}

		// export
		contextMenu.getItems().add(new SeparatorMenuItem());
		Label exportIcon = new Label("\uea98");
		exportIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem export = new MenuItem(multipleSelection ? "Export Pilot Points..." : "Export Pilot Point...", exportIcon);
		export.setId(multipleSelection ? "exportMultipleSTFs" : "exportSTF");
		export.setOnAction(handler);
		contextMenu.getItems().add(export);

		// export multiplication tables
		if (!multipleSelection) {
			MenuItem exportMultTables = new MenuItem("Export Loadcase Factor Files...");
			exportMultTables.setId("exportMultTables");
			exportMultTables.setOnAction(handler);
			contextMenu.getItems().add(exportMultTables);
		}

		// delete
		contextMenu.getItems().add(new SeparatorMenuItem());
		Label deleteIcon = new Label("\uf014");
		deleteIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem remove = new MenuItem("Delete", deleteIcon);
		remove.setId("delete");
		remove.setOnAction(handler);
		contextMenu.getItems().add(remove);

		// initialize separator
		int separatorIndex = 0;

		// generate stress sequence
		separatorIndex += addGenerateStressSequencesItem(contextMenu, handler, selected);

		// damage contribution
		separatorIndex += addDamageContributionItem(contextMenu, handler, selected);

		// damage angle
		separatorIndex += addDamageAngleItem(contextMenu, handler, selected);

		// calculate equivalent stress
		separatorIndex += addCalculateEquivalentStressItem(contextMenu, handler, selected);

		// add separator
		if (separatorIndex != 0) {
			contextMenu.getItems().add(separatorIndex, new SeparatorMenuItem());
		}

		// return menu
		return contextMenu;
	}

	/**
	 * Adds damage contribution item to context menu.
	 *
	 * @param contextMenu
	 *            Context menu.
	 * @param handler
	 *            Action handler.
	 * @param selected
	 *            Selected STF files.
	 * @return Separator index.
	 */
	private static int addDamageContributionItem(ContextMenu contextMenu, ActionHandler handler, ObservableList<TreeItem<String>> selected) {

		// loop over selected files
		Spectrum cdfSet = null;
		for (TreeItem<String> item : selected) {

			// get STF file
			STFFile stfFile = (STFFile) item;

			// selected STF files are not from the same CDF set
			Spectrum set = stfFile.getParentItem();
			if ((cdfSet != null) && !cdfSet.equals(set))
				return 0;
			cdfSet = set;
		}

		// add item
		Label dameContIcon = new Label("\uf200");
		dameContIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem dameCont = new MenuItem("Damage Contribution Analysis", dameContIcon);
		dameCont.setId("damageContribution");
		dameCont.setOnAction(handler);
		contextMenu.getItems().add(0, dameCont);
		return 1;
	}

	/**
	 * Adds rainflow item to context menu.
	 *
	 * @param contextMenu
	 *            Context menu.
	 * @param handler
	 *            Action handler.
	 * @param selected
	 *            Selected STF files.
	 * @return Separator index.
	 */
	private static int addDamageAngleItem(ContextMenu contextMenu, ActionHandler handler, ObservableList<TreeItem<String>> selected) {

		// loop over selected files
		for (TreeItem<String> item : selected) {

			// get STF file
			STFFile stfFile = (STFFile) item;

			// not 2D STF file
			if (!stfFile.is2D())
				return 0;
		}

		// initialize action ID
		String id = "damageAngle";

		// selected STF files are not from the same CDF set
		Spectrum cdfSet = null;
		for (TreeItem<String> item : selected) {
			STFFile stfFile = (STFFile) item;
			Spectrum set = stfFile.getParentItem();
			if ((cdfSet != null) && !cdfSet.equals(set)) {
				id = "damageAngleNoEventModifiers";
				break;
			}
			cdfSet = set;
		}

		// add item
		Label damageAngleIcon = new Label("\ueab0");
		damageAngleIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem damageAngle = new MenuItem("Damage Angle Analysis", damageAngleIcon);
		damageAngle.setId(id);
		damageAngle.setOnAction(handler);
		contextMenu.getItems().add(0, damageAngle);
		return 1;
	}

	/**
	 * Adds rainflow item to context menu.
	 *
	 * @param contextMenu
	 *            Context menu.
	 * @param handler
	 *            Action handler.
	 * @param selected
	 *            Selected STF files.
	 * @return Separator index.
	 */
	private static int addGenerateStressSequencesItem(ContextMenu contextMenu, ActionHandler handler, ObservableList<TreeItem<String>> selected) {

		// initialize action ID
		String id = "generateSpectrum";

		// selected STF files are not from the same CDF set
		Spectrum cdfSet = null;
		for (TreeItem<String> item : selected) {
			STFFile stfFile = (STFFile) item;
			Spectrum set = stfFile.getParentItem();
			if ((cdfSet != null) && !cdfSet.equals(set)) {
				id = "generateSpectrumNoEventModifiers";
				break;
			}
			cdfSet = set;
		}

		// add generate spectrum item
		Label generateIcon = new Label("\ueb6d");
		generateIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem generate = new MenuItem("Generate Stress Sequence", generateIcon);
		generate.setId(id);
		generate.setOnAction(handler);
		contextMenu.getItems().add(0, generate);
		return 1;
	}

	/**
	 * Adds rainflow item to context menu.
	 *
	 * @param contextMenu
	 *            Context menu.
	 * @param handler
	 *            Action handler.
	 * @param selected
	 *            Selected STF files.
	 * @return Separator index.
	 */
	private static int addCalculateEquivalentStressItem(ContextMenu contextMenu, ActionHandler handler, ObservableList<TreeItem<String>> selected) {

		// initialize action ID
		String id = "equivalentStressSTF";

		// loop over selected files
		Spectrum cdfSet = null;
		for (TreeItem<String> item : selected) {

			// get STF file
			STFFile stfFile = (STFFile) item;

			// selected STF files are not from the same CDF set
			Spectrum set = stfFile.getParentItem();
			if ((cdfSet != null) && !cdfSet.equals(set)) {
				id = "equivalentStressSTFNoEventModifiers";
				break;
			}
			cdfSet = set;
		}

		// add equivalent stress item
		Label eqStressIcon = new Label("\uedaf");
		eqStressIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem eqStress = new MenuItem("Equivalent Stress Analysis", eqStressIcon);
		eqStress.setId(id);
		eqStress.setOnAction(handler);
		contextMenu.getItems().add(0, eqStress);
		return 1;
	}
}
