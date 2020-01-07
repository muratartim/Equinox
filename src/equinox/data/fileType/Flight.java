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

import org.apache.commons.lang3.builder.HashCodeBuilder;

import equinox.data.ActionHandler;
import equinox.font.IconicFont;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;

/**
 * Class for flight.
 *
 * @author Murat Artim
 * @date Jan 19, 2014
 * @time 3:18:58 PM
 */
public class Flight extends SpectrumItem {

	/**
	 * Creates flight.
	 *
	 * @param flightName
	 *            Flight name.
	 * @param flightID
	 *            Flight ID.
	 */
	public Flight(String flightName, int flightID) {

		// create spectrum item
		super(flightName, flightID);

		// create icon label
		Label iconLabel = new Label("\uec04");
		iconLabel.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		setGraphic(iconLabel);
	}

	@Override
	public Flights getParentItem() {
		return (Flights) getParent();
	}

	@Override
	public IconicFont getIconFont() {
		return IconicFont.ICOMOON;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(33, 51).append(getName()).append(getID()).toHashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Flight) {
			Flight flight = (Flight) object;
			return getName().equals(flight.getName()) && (getID() == flight.getID());
		}
		return super.equals(object);
	}

	/**
	 * Creates and returns context menu for ANA files.
	 *
	 * @param multipleSelection
	 *            True if multiple ANA files are selected.
	 * @param handler
	 *            Action handler.
	 * @param selected
	 *            Selected flights.
	 * @return Context menu.
	 */
	public static ContextMenu createContextMenu(boolean multipleSelection, ActionHandler handler, ObservableList<TreeItem<String>> selected) {

		// create menu
		ContextMenu contextMenu = new ContextMenu();

		// add plot flights menu item
		Label plotIcon = new Label("\ue900");
		plotIcon.getStylesheets().add(IconicFont.CUSTOM.getStyleSheet());
		MenuItem plot = new MenuItem("Plot Flight" + (multipleSelection ? "s" : ""), plotIcon);
		plot.setId("plotFlight");
		plot.setOnAction(handler);
		contextMenu.getItems().add(plot);

		// add compare flights menu item
		if (multipleSelection) {
			Label statisticsIcon = new Label("\uf24e");
			statisticsIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem compare = new MenuItem("Compare Flights", statisticsIcon);
			compare.setId("compareFlights");
			compare.setOnAction(handler);
			contextMenu.getItems().add(compare);
		}

		// check if selected flights are from the same stress sequence
		boolean fromSameSequence = true;
		if (multipleSelection) {
			StressSequence sequence = null;
			for (TreeItem<String> item : selected) {
				Flight flight = (Flight) item;
				StressSequence seq = flight.getParentItem().getParentItem();
				if ((sequence != null) && !sequence.equals(seq)) {
					fromSameSequence = false;
					break;
				}
				sequence = seq;
			}
		}

		// add statistics menu item
		if (fromSameSequence) {
			Label statisticsIcon = new Label("\uf080");
			statisticsIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			String name = multipleSelection ? "Plot Flight Statistics" : "Plot Event Statistics";
			MenuItem statistics = new MenuItem(name, statisticsIcon);
			statistics.setId("showSpectrumStats");
			statistics.setOnAction(handler);
			contextMenu.getItems().add(statistics);
		}

		// find
		contextMenu.getItems().add(new SeparatorMenuItem());
		Label findIcon = new Label("\uf002");
		findIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem find = new MenuItem("Find Similar Files", findIcon);
		find.setId("find");
		find.setOnAction(handler);
		contextMenu.getItems().add(find);

		// return menu
		return contextMenu;
	}
}
