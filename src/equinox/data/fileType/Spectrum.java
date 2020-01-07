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
 * Class for spectrum.
 *
 * @author Murat Artim
 * @date Jan 20, 2014
 * @time 10:59:51 AM
 */
public class Spectrum extends SpectrumItem {

	/** Spectrum info. */
	private String program_, section_, mission_;

	/** Core spectrum file IDs. */
	private int anaID_, txtID_, cvtID_, flsID_, convTableID_;

	/**
	 * Creates spectrum.
	 *
	 * @param fileName
	 *            File name.
	 * @param fileID
	 *            File ID.
	 */
	public Spectrum(String fileName, int fileID) {

		// create spectrum item
		super(fileName, fileID);

		// create icon label
		Label iconLabel = new Label("\ueb8c");
		iconLabel.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		setGraphic(iconLabel);
	}

	/**
	 * Returns A/C program.
	 *
	 * @return A/C program.
	 */
	public String getProgram() {
		return program_;
	}

	/**
	 * Returns A/C section.
	 *
	 * @return A/C section.
	 */
	public String getSection() {
		return section_;
	}

	/**
	 * Returns fatigue mission.
	 *
	 * @return Fatigue mission.
	 */
	public String getMission() {
		return mission_;
	}

	@Override
	public SpectrumItem getParentItem() {
		return null;
	}

	@Override
	public IconicFont getIconFont() {
		return IconicFont.ICOMOON;
	}

	/**
	 * Returns ANA file ID of this spectrum.
	 *
	 * @return ANA file ID of this spectrum.
	 */
	public int getANAFileID() {
		return anaID_;
	}

	/**
	 * Returns TXT file ID of this spectrum.
	 *
	 * @return TXT file ID of this spectrum.
	 */
	public int getTXTFileID() {
		return txtID_;
	}

	/**
	 * Returns FLS file ID of this spectrum.
	 *
	 * @return FLS file ID of this spectrum.
	 */
	public int getFLSFileID() {
		return flsID_;
	}

	/**
	 * Returns CVT file ID of this spectrum.
	 *
	 * @return CVT file ID of this spectrum.
	 */
	public int getCVTFileID() {
		return cvtID_;
	}

	/**
	 * Returns conversion table ID of this spectrum.
	 *
	 * @return Conversion table ID of this spectrum.
	 */
	public int getConversionTableID() {
		return convTableID_;
	}

	/**
	 * Returns the STF files of this spectrum or null if there are no STF files.
	 *
	 * @return The STF files of this spectrum or null if there are no STF files.
	 */
	public ArrayList<STFFile> getSTFFiles() {
		ArrayList<STFFile> stfFiles = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof STFFile) {
				if (stfFiles == null)
					stfFiles = new ArrayList<>();
				stfFiles.add((STFFile) item);
			}
		}
		return stfFiles;
	}

	/**
	 * Returns STF file bucket or null if there is no STF file bucket.
	 *
	 * @return STF file bucket or null if there is no STF file bucket.
	 */
	public STFFileBucket getSTFFileBucket() {
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof STFFileBucket)
				return (STFFileBucket) item;
		}
		return null;
	}

	/**
	 * Sets ANA file ID.
	 *
	 * @param anaID
	 *            ANA file ID.
	 */
	public void setANAFileID(int anaID) {
		anaID_ = anaID;
	}

	/**
	 * Sets TXT file ID.
	 *
	 * @param txtID
	 *            TXT file ID.
	 */
	public void setTXTFileID(int txtID) {
		txtID_ = txtID;
	}

	/**
	 * Sets CVT file ID.
	 *
	 * @param cvtID
	 *            CVT file ID.
	 */
	public void setCVTFileID(int cvtID) {
		cvtID_ = cvtID;
	}

	/**
	 * Sets FLS file ID.
	 *
	 * @param flsID
	 *            FLS file ID.
	 */
	public void setFLSFileID(int flsID) {
		flsID_ = flsID;
	}

	/**
	 * Sets conversion table ID.
	 *
	 * @param convTableID
	 *            Conversion table ID.
	 */
	public void setConvTableID(int convTableID) {
		convTableID_ = convTableID;
	}

	/**
	 * Sets A/C program.
	 *
	 * @param program
	 *            A/C program.
	 */
	public void setProgram(String program) {
		program_ = program;
	}

	/**
	 * Sets A/C section.
	 *
	 * @param section
	 *            A/C section.
	 */
	public void setSection(String section) {
		section_ = section;
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
	 * Creates and returns context menu for ANA files.
	 *
	 * @param multipleSelection
	 *            True if multiple ANA files are selected.
	 * @param handler
	 *            Action handler.
	 * @param selected
	 *            Selected spectra.
	 * @return Context menu.
	 */
	public static ContextMenu createContextMenu(boolean multipleSelection, ActionHandler handler, ObservableList<TreeItem<String>> selected) {

		// create menu
		ContextMenu contextMenu = new ContextMenu();

		// add STF files
		if (!multipleSelection) {

			// create add STF menu
			Label addSTFIcon = new Label("\uf055");
			addSTFIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			Menu addSTFMenu = new Menu("Add Stress Input Files...", addSTFIcon);
			contextMenu.getItems().add(addSTFMenu);

			// select STF files
			Label stfIcon = new Label("\uf1c4");
			stfIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem selectSTFFiles = new MenuItem("Select STF Files", stfIcon);
			selectSTFFiles.setId("addSTF");
			selectSTFFiles.setOnAction(handler);
			addSTFMenu.getItems().add(selectSTFFiles);

			// select directory
			Label directoryIcon = new Label("\uf115");
			directoryIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem selectSTFDirectory = new MenuItem("Select Directory", directoryIcon);
			selectSTFDirectory.setId("addSTFFromDirectory");
			selectSTFDirectory.setOnAction(handler);
			addSTFMenu.getItems().add(selectSTFDirectory);

			// download sample STF file
			addSTFMenu.getItems().add(new SeparatorMenuItem());
			Label sampleSTFIcon = new Label("\uf0ed");
			sampleSTFIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem sampleSTF = new MenuItem("Download Sample Stress Input File", sampleSTFIcon);
			sampleSTF.setId("downloadSampleSTF");
			sampleSTF.setOnAction(handler);
			addSTFMenu.getItems().add(sampleSTF);

			// create dummy STF file (only if all STFs are visible)
			if (((Spectrum) selected.get(0)).getSTFFileBucket() == null) {
				Label dummySTFIcon = new Label("\ueb6a");
				dummySTFIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
				MenuItem dummySTF = new MenuItem("Create Dummy Stress Input File", dummySTFIcon);
				dummySTF.setId("dummySTF");
				dummySTF.setOnAction(handler);
				contextMenu.getItems().add(dummySTF);
			}

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());
		}

		// edit info
		if (!multipleSelection) {
			Label editIcon = new Label("\uf044");
			editIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem edit = new MenuItem("Edit Spectrum Info", editIcon);
			edit.setId("editSpectrumInfo");
			edit.setOnAction(handler);
			contextMenu.getItems().add(edit);
		}

		// assign mission parameters
		if (!multipleSelection) {
			Label missionParametersIcon = new Label("\uf1de");
			missionParametersIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem missionParameters = new MenuItem("Assign Mission Parameters", missionParametersIcon);
			missionParameters.setId("assignMissionParameters");
			missionParameters.setOnAction(handler);
			contextMenu.getItems().add(missionParameters);
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

		// share
		if (!multipleSelection) {

			// share spectrum
			contextMenu.getItems().add(new SeparatorMenuItem());
			Label shareIcon = new Label("\uf064");
			shareIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem share = new MenuItem("Share Spectrum", shareIcon);
			share.setId("share");
			share.setOnAction(handler);
			contextMenu.getItems().add(share);

			// save spectrum file
			Menu shareMenu = new Menu("Share Spectrum File...");
			contextMenu.getItems().add(shareMenu);
			MenuItem shareANA = new MenuItem("Share ANA File");
			shareANA.setId("shareANA");
			shareANA.setOnAction(handler);
			shareMenu.getItems().add(shareANA);
			MenuItem shareTXT = new MenuItem("Share TXT File");
			shareTXT.setId("shareTXT");
			shareTXT.setOnAction(handler);
			shareMenu.getItems().add(shareTXT);
			MenuItem shareCVT = new MenuItem("Share CVT File");
			shareCVT.setId("shareCVT");
			shareCVT.setOnAction(handler);
			shareMenu.getItems().add(shareCVT);
			MenuItem shareFLS = new MenuItem("Share FLS File");
			shareFLS.setId("shareFLS");
			shareFLS.setOnAction(handler);
			shareMenu.getItems().add(shareFLS);
			MenuItem shareConv = new MenuItem("Share Conversion Table");
			shareConv.setId("shareCONV");
			shareConv.setOnAction(handler);
			shareMenu.getItems().add(shareConv);
		}

		// save spectrum
		contextMenu.getItems().add(new SeparatorMenuItem());
		Label saveSpectrumIcon = new Label("\uf0c7");
		saveSpectrumIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem saveSpectrum = new MenuItem("Save Spectrum As...", saveSpectrumIcon);
		saveSpectrum.setId("saveSpectrum");
		saveSpectrum.setOnAction(handler);
		contextMenu.getItems().add(saveSpectrum);

		// save spectrum file
		Menu saveMenu = new Menu("Save Spectrum File...");
		contextMenu.getItems().add(saveMenu);
		MenuItem saveANA = new MenuItem("Save ANA File As...");
		saveANA.setId("saveANA");
		saveANA.setOnAction(handler);
		saveMenu.getItems().add(saveANA);
		MenuItem saveTXT = new MenuItem("Save TXT File As...");
		saveTXT.setId("saveTXT");
		saveTXT.setOnAction(handler);
		saveMenu.getItems().add(saveTXT);
		MenuItem saveCVT = new MenuItem("Save CVT File As...");
		saveCVT.setId("saveCVT");
		saveCVT.setOnAction(handler);
		saveMenu.getItems().add(saveCVT);
		MenuItem saveFLS = new MenuItem("Save FLS File As...");
		saveFLS.setId("saveFLS");
		saveFLS.setOnAction(handler);
		saveMenu.getItems().add(saveFLS);
		MenuItem saveConv = new MenuItem("Save Conversion Table As...");
		saveConv.setId("saveCONV");
		saveConv.setOnAction(handler);
		saveMenu.getItems().add(saveConv);

		// export
		contextMenu.getItems().add(new SeparatorMenuItem());
		Label exportIcon = new Label("\uea98");
		exportIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem export = new MenuItem(multipleSelection ? "Export Spectra..." : "Export Spectrum...", exportIcon);
		export.setId(multipleSelection ? "exportMultipleSpectra" : "exportSpectrum");
		export.setOnAction(handler);
		contextMenu.getItems().add(export);

		// export multiplication tables
		if (!multipleSelection) {
			MenuItem exportMultTable = new MenuItem("Export Loadcase Factor Files...");
			exportMultTable.setId("exportMultTables");
			exportMultTable.setOnAction(handler);
			contextMenu.getItems().add(exportMultTable);
		}

		// hide
		contextMenu.getItems().add(new SeparatorMenuItem());
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
