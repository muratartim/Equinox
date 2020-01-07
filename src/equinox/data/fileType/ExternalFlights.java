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
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;

/**
 * Class for external flights folder.
 *
 * @author Murat Artim
 * @date Mar 11, 2015
 * @time 9:59:51 AM
 */
public class ExternalFlights extends SpectrumItem {

	/**
	 * Creates external flights folder.
	 *
	 * @param id
	 *            Spectrum ID.
	 */
	public ExternalFlights(int id) {

		// create spectrum item
		super("Typical Flights", id);

		// create icon label
		Label iconLabel = new Label("\ue9db");
		iconLabel.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		setGraphic(iconLabel);
	}

	@Override
	public ExternalStressSequence getParentItem() {
		return (ExternalStressSequence) getParent();
	}

	@Override
	public IconicFont getIconFont() {
		return IconicFont.ICOMOON;
	}

	/**
	 * Returns the flights of this spectrum.
	 *
	 * @return The flights of this spectrum.
	 */
	public ArrayList<ExternalFlight> getFlights() {
		ArrayList<ExternalFlight> flights = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof ExternalFlight) {
				if (flights == null)
					flights = new ArrayList<>();
				flights.add((ExternalFlight) item);
			}
		}
		return flights;
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
			MenuItem longest = new MenuItem("Select Longest Flight");
			longest.setId("selectExternalFlightLongest");
			longest.setOnAction(handler);
			contextMenu.getItems().add(longest);
			MenuItem shortest = new MenuItem("Select Shortest Flight");
			shortest.setId("selectExternalFlightShortest");
			shortest.setOnAction(handler);
			contextMenu.getItems().add(shortest);
			contextMenu.getItems().add(new SeparatorMenuItem());
			MenuItem maxVal = new MenuItem("Select Flight with Highest Occurrence");
			maxVal.setId("selectExternalFlightMaxVal");
			maxVal.setOnAction(handler);
			contextMenu.getItems().add(maxVal);
			MenuItem minVal = new MenuItem("Select Flight with Lowest Occurrence");
			minVal.setId("selectExternalFlightMinVal");
			minVal.setOnAction(handler);
			contextMenu.getItems().add(minVal);
			contextMenu.getItems().add(new SeparatorMenuItem());
			MenuItem maxTotal = new MenuItem("Flight with Highest Stress");
			maxTotal.setId("selectExternalFlightMaxPeak");
			maxTotal.setOnAction(handler);
			contextMenu.getItems().add(maxTotal);
			MenuItem minTotal = new MenuItem("Flight with Lowest Stress");
			minTotal.setId("selectExternalFlightMinPeak");
			minTotal.setOnAction(handler);
			contextMenu.getItems().add(minTotal);
		}

		// return menu
		return contextMenu;
	}
}
