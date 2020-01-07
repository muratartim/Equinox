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
 * Creates flights folder.
 *
 * @author Murat Artim
 * @date Jul 14, 2014
 * @time 1:11:48 AM
 */
public class Flights extends SpectrumItem {

	/**
	 * Creates flights folder.
	 *
	 * @param id
	 *            Spectrum ID.
	 */
	public Flights(int id) {

		// create spectrum item
		super("Typical Flights", id);

		// create icon label
		Label iconLabel = new Label("\ue9db");
		iconLabel.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		setGraphic(iconLabel);
	}

	@Override
	public StressSequence getParentItem() {
		return (StressSequence) getParent();
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
	public ArrayList<Flight> getFlights() {
		ArrayList<Flight> flights = null;
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof Flight) {
				if (flights == null)
					flights = new ArrayList<>();
				flights.add((Flight) item);
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
			longest.setId("selectFlightLongest");
			longest.setOnAction(handler);
			contextMenu.getItems().add(longest);
			MenuItem shortest = new MenuItem("Select Shortest Flight");
			shortest.setId("selectFlightShortest");
			shortest.setOnAction(handler);
			contextMenu.getItems().add(shortest);
			contextMenu.getItems().add(new SeparatorMenuItem());
			MenuItem maxVal = new MenuItem("Select Flight with Highest Occurrence");
			maxVal.setId("selectFlightMaxVal");
			maxVal.setOnAction(handler);
			contextMenu.getItems().add(maxVal);
			MenuItem minVal = new MenuItem("Select Flight with Lowest Occurrence");
			minVal.setId("selectFlightMinVal");
			minVal.setOnAction(handler);
			contextMenu.getItems().add(minVal);
			contextMenu.getItems().add(new SeparatorMenuItem());
			Menu maxStress = new Menu("Select Flight with Highest Stress");
			contextMenu.getItems().add(maxStress);
			MenuItem maxTotal = new MenuItem("Total Stress");
			maxTotal.setId("selectFlightMaxTotal");
			maxTotal.setOnAction(handler);
			maxStress.getItems().add(maxTotal);
			MenuItem max1g = new MenuItem("1g Stress");
			max1g.setId("selectFlightMax1g");
			max1g.setOnAction(handler);
			maxStress.getItems().add(max1g);
			MenuItem maxInc = new MenuItem("Increment Stress");
			maxInc.setId("selectFlightMaxInc");
			maxInc.setOnAction(handler);
			maxStress.getItems().add(maxInc);
			MenuItem maxDp = new MenuItem("Delta-p Stress");
			maxDp.setId("selectFlightMaxDP");
			maxDp.setOnAction(handler);
			maxStress.getItems().add(maxDp);
			MenuItem maxDt = new MenuItem("Delta-t Stress");
			maxDt.setId("selectFlightMaxDT");
			maxDt.setOnAction(handler);
			maxStress.getItems().add(maxDt);
			Menu minStress = new Menu("Flight with Lowest Stress");
			contextMenu.getItems().add(minStress);
			MenuItem minTotal = new MenuItem("Total Stress");
			minTotal.setId("selectFlightMinTotal");
			minTotal.setOnAction(handler);
			minStress.getItems().add(minTotal);
			MenuItem min1g = new MenuItem("1g Stress");
			min1g.setId("selectFlightMin1g");
			min1g.setOnAction(handler);
			minStress.getItems().add(min1g);
			MenuItem minInc = new MenuItem("Increment Stress");
			minInc.setId("selectFlightMinInc");
			minInc.setOnAction(handler);
			minStress.getItems().add(minInc);
			MenuItem minDp = new MenuItem("Delta-p Stress");
			minDp.setId("selectFlightMinDP");
			minDp.setOnAction(handler);
			minStress.getItems().add(minDp);
			MenuItem minDt = new MenuItem("Delta-t Stress");
			minDt.setId("selectFlightMinDT");
			minDt.setOnAction(handler);
			minStress.getItems().add(minDt);
		}

		// return menu
		return contextMenu;
	}
}
