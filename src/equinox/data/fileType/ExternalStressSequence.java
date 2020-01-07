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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;

/**
 * Class for external stress sequence.
 *
 * @author Murat Artim
 * @date Mar 11, 2015
 * @time 9:50:35 AM
 */
public class ExternalStressSequence extends SpectrumItem {

	/** Sequence info. */
	private String program_, section_, mission_;

	/**
	 * Creates external stress sequence.
	 *
	 * @param name
	 *            Stress sequence name.
	 * @param stressSequenceID
	 *            Stress sequence ID.
	 */
	public ExternalStressSequence(String name, int stressSequenceID) {

		// create spectrum item
		super(name, stressSequenceID);

		// create icon label
		Label iconLabel = new Label("\ueb6d");
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
	 * Returns the flights folder of this spectrum.
	 *
	 * @return The flights folder of this spectrum.
	 */
	public ExternalFlights getFlights() {
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof ExternalFlights)
				return (ExternalFlights) item;
		}
		return null;
	}

	/**
	 * Returns the fatigue equivalent stresses of this stress sequence or null if there are no fatigue equivalent stresses.
	 *
	 * @return The fatigue equivalent stresses of this stress sequence or null if there are no fatigue equivalent stresses.
	 */
	public ArrayList<ExternalFatigueEquivalentStress> getFatigueEquivalentStresses() {
		ArrayList<ExternalFatigueEquivalentStress> eqStresses = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof ExternalFatigueEquivalentStress) {
				if (eqStresses == null) {
					eqStresses = new ArrayList<>();
				}
				eqStresses.add((ExternalFatigueEquivalentStress) item);
			}
		}
		return eqStresses;
	}

	/**
	 * Returns the Preffas equivalent stresses of this stress sequence or null if there are no Preffas equivalent stresses.
	 *
	 * @return The Preffas equivalent stresses of this stress sequence or null if there are no Preffas equivalent stresses.
	 */
	public ArrayList<ExternalPreffasEquivalentStress> getPreffasEquivalentStresses() {
		ArrayList<ExternalPreffasEquivalentStress> eqStresses = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof ExternalPreffasEquivalentStress) {
				if (eqStresses == null) {
					eqStresses = new ArrayList<>();
				}
				eqStresses.add((ExternalPreffasEquivalentStress) item);
			}
		}
		return eqStresses;
	}

	/**
	 * Returns the linear equivalent stresses of this stress sequence or null if there are no linear equivalent stresses.
	 *
	 * @return The linear equivalent stresses of this stress sequence or null if there are no linear equivalent stresses.
	 */
	public ArrayList<ExternalLinearEquivalentStress> getLinearEquivalentStresses() {
		ArrayList<ExternalLinearEquivalentStress> eqStresses = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof ExternalLinearEquivalentStress) {
				if (eqStresses == null) {
					eqStresses = new ArrayList<>();
				}
				eqStresses.add((ExternalLinearEquivalentStress) item);
			}
		}
		return eqStresses;
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
	 * Extracts and returns EID from file name (if EID exists).
	 *
	 * @param fileName
	 *            Stress sequence file name.
	 * @return EID or null if no EID found in file name.
	 */
	public static String getEID(String fileName) {

		// initialize EID
		String eid = null;

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
	 * @return Context menu.
	 */
	public static ContextMenu createContextMenu(boolean multipleSelection, ActionHandler handler) {

		// create menu
		ContextMenu contextMenu = new ContextMenu();

		// select flight
		if (!multipleSelection) {
			Label statisticsIcon = new Label("\uec04");
			statisticsIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			Menu select = new Menu("Select Typical Flight", statisticsIcon);
			contextMenu.getItems().add(select);
			MenuItem longest = new MenuItem("Longest Flight");
			longest.setId("selectExternalFlightLongest");
			longest.setOnAction(handler);
			select.getItems().add(longest);
			MenuItem shortest = new MenuItem("Shortest Flight");
			shortest.setId("selectExternalFlightShortest");
			shortest.setOnAction(handler);
			select.getItems().add(shortest);
			select.getItems().add(new SeparatorMenuItem());
			MenuItem maxVal = new MenuItem("Flight with Highest Occurrence");
			maxVal.setId("selectExternalFlightMaxVal");
			maxVal.setOnAction(handler);
			select.getItems().add(maxVal);
			MenuItem minVal = new MenuItem("Flight with Lowest Occurrence");
			minVal.setId("selectExternalFlightMinVal");
			minVal.setOnAction(handler);
			select.getItems().add(minVal);
			select.getItems().add(new SeparatorMenuItem());
			MenuItem maxTotal = new MenuItem("Flight with Highest Stress");
			maxTotal.setId("selectExternalFlightMaxPeak");
			maxTotal.setOnAction(handler);
			select.getItems().add(maxTotal);
			MenuItem minTotal = new MenuItem("Flight with Lowest Stress");
			minTotal.setId("selectExternalFlightMinPeak");
			minTotal.setOnAction(handler);
			select.getItems().add(minTotal);
			contextMenu.getItems().add(new SeparatorMenuItem());
		}

		// plot statistics
		if (!multipleSelection) {
			Label statisticsIcon = new Label("\uf080");
			statisticsIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem statistics = new MenuItem("Plot Flight Statistics", statisticsIcon);
			statistics.setId("showExternalSpectrumStats");
			statistics.setOnAction(handler);
			contextMenu.getItems().add(statistics);
		}

		// compare stress sequences
		if (multipleSelection) {
			Label statisticsIcon = new Label("\uf24e");
			statisticsIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem compare = new MenuItem("Compare Stress Sequences", statisticsIcon);
			compare.setId("compareExternalSpectra");
			compare.setOnAction(handler);
			contextMenu.getItems().add(compare);
		}

		// calculate equivalent stress
		Label eqStressIcon = new Label("\uedaf");
		eqStressIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem equivalentStress = new MenuItem("Equivalent Stress Analysis", eqStressIcon);
		equivalentStress.setId("equivalentStress");
		equivalentStress.setOnAction(handler);
		contextMenu.getItems().add(equivalentStress);
		contextMenu.getItems().add(new SeparatorMenuItem());

		// edit info
		if (!multipleSelection) {
			Label editIcon = new Label("\uf044");
			editIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem edit = new MenuItem("Edit Sequence Info", editIcon);
			edit.setId("editSequenceInfo");
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

		// assign mission parameters
		if (!multipleSelection) {
			Label missionParametersIcon = new Label("\uf1de");
			missionParametersIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem missionParameters = new MenuItem("Assign Mission Parameters", missionParametersIcon);
			missionParameters.setId("assignMissionParameters");
			missionParameters.setOnAction(handler);
			contextMenu.getItems().add(missionParameters);
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

		// share
		if (!multipleSelection) {

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());

			// share sequence
			Label shareIcon = new Label("\uf064");
			shareIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem share = new MenuItem("Share Stress Sequence", shareIcon);
			share.setId("share");
			share.setOnAction(handler);
			contextMenu.getItems().add(share);

			// share FLS file
			MenuItem shareFLS = new MenuItem("Share FLS File");
			shareFLS.setId("shareExternalFLS");
			shareFLS.setOnAction(handler);
			contextMenu.getItems().add(shareFLS);
		}

		// add separator
		contextMenu.getItems().add(new SeparatorMenuItem());

		// save
		Label saveIcon = new Label("\uf0c7");
		saveIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem save = new MenuItem("Save Stress Sequence As...", saveIcon);
		save.setId("saveStressSequence");
		save.setOnAction(handler);
		contextMenu.getItems().add(save);

		// save FLS
		MenuItem saveFLS = new MenuItem("Save FLS File As...");
		saveFLS.setId("saveFLS");
		saveFLS.setOnAction(handler);
		contextMenu.getItems().add(saveFLS);

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
		MenuItem remove = new MenuItem("Delete", deleteIcon);
		remove.setId("delete");
		remove.setOnAction(handler);
		contextMenu.getItems().add(remove);

		// return menu
		return contextMenu;
	}
}
