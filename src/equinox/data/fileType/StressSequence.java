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
 * Class for stress sequence.
 *
 * @author Murat Artim
 * @date Jan 19, 2014
 * @time 4:34:55 PM
 */
public class StressSequence extends SpectrumItem {

	/**
	 * Creates stress sequence.
	 *
	 * @param name
	 *            Stress sequence name.
	 * @param stressSequenceID
	 *            Stress sequence ID.
	 */
	public StressSequence(String name, int stressSequenceID) {

		// create spectrum item
		super(name, stressSequenceID);

		// create icon label
		final Label iconLabel = new Label("\ueb6d");
		iconLabel.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		setGraphic(iconLabel);
	}

	@Override
	public STFFile getParentItem() {
		return (STFFile) getParent();
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
	public Flights getFlights() {
		for (final TreeItem<String> item : getChildren())
			if (item instanceof Flights) return (Flights) item;
		return null;
	}

	/**
	 * Returns the fatigue equivalent stresses of this stress sequence or null if there are no fatigue equivalent stresses.
	 *
	 * @return The fatigue equivalent stresses of this stress sequence or null if there are no fatigue equivalent stresses.
	 */
	public ArrayList<FatigueEquivalentStress> getFatigueEquivalentStresses() {
		ArrayList<FatigueEquivalentStress> eqStresses = null;
		for (final TreeItem<String> item : getChildren())
			if (item instanceof FatigueEquivalentStress) {
				if (eqStresses == null) eqStresses = new ArrayList<>();
				eqStresses.add((FatigueEquivalentStress) item);
			}
		return eqStresses;
	}

	/**
	 * Returns the Preffas equivalent stresses of this stress sequence or null if there are no Preffas equivalent stresses.
	 *
	 * @return The Preffas equivalent stresses of this stress sequence or null if there are no Preffas equivalent stresses.
	 */
	public ArrayList<PreffasEquivalentStress> getPreffasEquivalentStresses() {
		ArrayList<PreffasEquivalentStress> eqStresses = null;
		for (final TreeItem<String> item : getChildren())
			if (item instanceof PreffasEquivalentStress) {
				if (eqStresses == null) eqStresses = new ArrayList<>();
				eqStresses.add((PreffasEquivalentStress) item);
			}
		return eqStresses;
	}

	/**
	 * Returns the linear equivalent stresses of this stress sequence or null if there are no linear equivalent stresses.
	 *
	 * @return The linear equivalent stresses of this stress sequence or null if there are no linear equivalent stresses.
	 */
	public ArrayList<LinearEquivalentStress> getLinearEquivalentStresses() {
		ArrayList<LinearEquivalentStress> eqStresses = null;
		for (final TreeItem<String> item : getChildren())
			if (item instanceof LinearEquivalentStress) {
				if (eqStresses == null) eqStresses = new ArrayList<>();
				eqStresses.add((LinearEquivalentStress) item);
			}
		return eqStresses;
	}

	/**
	 * Creates and returns context menu for ANA files.
	 *
	 * @param numSelection
	 *            Number of selections.
	 * @param handler
	 *            Action handler.
	 * @return Context menu.
	 */
	public static ContextMenu createContextMenu(int numSelection, ActionHandler handler) {

		// create menu
		final ContextMenu contextMenu = new ContextMenu();

		// select flight
		if (numSelection == 1) {
			final Label statisticsIcon = new Label("\uec04");
			statisticsIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			final Menu select = new Menu("Select Typical Flight", statisticsIcon);
			contextMenu.getItems().add(select);
			final MenuItem longest = new MenuItem("Longest Flight");
			longest.setId("selectFlightLongest");
			longest.setOnAction(handler);
			select.getItems().add(longest);
			final MenuItem shortest = new MenuItem("Shortest Flight");
			shortest.setId("selectFlightShortest");
			shortest.setOnAction(handler);
			select.getItems().add(shortest);
			select.getItems().add(new SeparatorMenuItem());
			final MenuItem maxVal = new MenuItem("Flight with Highest Occurrence");
			maxVal.setId("selectFlightMaxVal");
			maxVal.setOnAction(handler);
			select.getItems().add(maxVal);
			final MenuItem minVal = new MenuItem("Flight with Lowest Occurrence");
			minVal.setId("selectFlightMinVal");
			minVal.setOnAction(handler);
			select.getItems().add(minVal);
			select.getItems().add(new SeparatorMenuItem());
			final Menu maxStress = new Menu("Flight with Highest Stress");
			select.getItems().add(maxStress);
			final MenuItem maxTotal = new MenuItem("Total Stress");
			maxTotal.setId("selectFlightMaxTotal");
			maxTotal.setOnAction(handler);
			maxStress.getItems().add(maxTotal);
			final MenuItem max1g = new MenuItem("1g Stress");
			max1g.setId("selectFlightMax1g");
			max1g.setOnAction(handler);
			maxStress.getItems().add(max1g);
			final MenuItem maxInc = new MenuItem("Increment Stress");
			maxInc.setId("selectFlightMaxInc");
			maxInc.setOnAction(handler);
			maxStress.getItems().add(maxInc);
			final MenuItem maxDp = new MenuItem("Delta-p Stress");
			maxDp.setId("selectFlightMaxDP");
			maxDp.setOnAction(handler);
			maxStress.getItems().add(maxDp);
			final MenuItem maxDt = new MenuItem("Delta-t Stress");
			maxDt.setId("selectFlightMaxDT");
			maxDt.setOnAction(handler);
			maxStress.getItems().add(maxDt);
			final Menu minStress = new Menu("Flight with Lowest Stress");
			select.getItems().add(minStress);
			final MenuItem minTotal = new MenuItem("Total Stress");
			minTotal.setId("selectFlightMinTotal");
			minTotal.setOnAction(handler);
			minStress.getItems().add(minTotal);
			final MenuItem min1g = new MenuItem("1g Stress");
			min1g.setId("selectFlightMin1g");
			min1g.setOnAction(handler);
			minStress.getItems().add(min1g);
			final MenuItem minInc = new MenuItem("Increment Stress");
			minInc.setId("selectFlightMinInc");
			minInc.setOnAction(handler);
			minStress.getItems().add(minInc);
			final MenuItem minDp = new MenuItem("Delta-p Stress");
			minDp.setId("selectFlightMinDP");
			minDp.setOnAction(handler);
			minStress.getItems().add(minDp);
			final MenuItem minDt = new MenuItem("Delta-t Stress");
			minDt.setId("selectFlightMinDT");
			minDt.setOnAction(handler);
			minStress.getItems().add(minDt);
			contextMenu.getItems().add(new SeparatorMenuItem());
		}

		// plot mission profile
		if (numSelection == 1 || numSelection == 2) {
			final Label statisticsIcon = new Label("\uf1fe");
			statisticsIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			final MenuItem statistics = new MenuItem("Plot Mission Profile" + (numSelection == 2 ? "s" : ""), statisticsIcon);
			statistics.setId("plotMissionProfile");
			statistics.setOnAction(handler);
			contextMenu.getItems().add(statistics);
		}

		// plot statistics
		if (numSelection == 1) {
			final Label statisticsIcon = new Label("\uf080");
			statisticsIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			final MenuItem statistics = new MenuItem("Plot Flight Statistics", statisticsIcon);
			statistics.setId("showSpectrumStats");
			statistics.setOnAction(handler);
			contextMenu.getItems().add(statistics);
		}

		// compare stress sequences
		if (numSelection > 1) {
			final Label statisticsIcon = new Label("\uf24e");
			statisticsIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			final MenuItem compare = new MenuItem("Compare Stress Sequences", statisticsIcon);
			compare.setId("compareSpectra");
			compare.setOnAction(handler);
			contextMenu.getItems().add(compare);
		}

		// add separator
		contextMenu.getItems().add(new SeparatorMenuItem());

		// calculate equivalent stress
		final Label eqStressIcon = new Label("\uedaf");
		eqStressIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		final MenuItem equivalentStress = new MenuItem("Equivalent Stress Analysis", eqStressIcon);
		equivalentStress.setId("equivalentStress");
		equivalentStress.setOnAction(handler);
		contextMenu.getItems().add(equivalentStress);

		// add separator
		contextMenu.getItems().add(new SeparatorMenuItem());

		// find
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

		// add separator
		contextMenu.getItems().add(new SeparatorMenuItem());

		// share sequence
		if (numSelection == 1) {
			final Label shareIcon = new Label("\uf064");
			shareIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			final MenuItem share = new MenuItem("Share", shareIcon);
			share.setId("share");
			share.setOnAction(handler);
			contextMenu.getItems().add(share);
		}

		// save sequence
		final Label saveIcon = new Label("\uf0c7");
		saveIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		final MenuItem save = new MenuItem("Save As...", saveIcon);
		save.setId("saveStressSequence");
		save.setOnAction(handler);
		contextMenu.getItems().add(save);

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
