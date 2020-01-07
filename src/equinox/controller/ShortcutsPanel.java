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
package equinox.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import equinox.data.EquinoxTheme;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Class for shortcuts panel controller.
 *
 * @author Murat Artim
 * @date Dec 2, 2014
 * @time 9:39:40 AM
 */
public class ShortcutsPanel implements Initializable {

	@FXML
	private VBox root_, container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param menuBar
	 *            The menu bar.
	 * @return The newly loaded plot column panel.
	 */
	public static VBox load(MenuBar menuBar) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ShortcutsPanel.fxml"));
			fxmlLoader.load();

			// get controller
			ShortcutsPanel controller = (ShortcutsPanel) fxmlLoader.getController();

			// create shortcuts
			for (Menu menu : menuBar.getMenus()) {
				processMenu(menu, controller);
			}

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Processes menu recursively.
	 *
	 * @param menu
	 *            Menu to process.
	 * @param controller
	 *            Controller.
	 */
	private static void processMenu(Menu menu, ShortcutsPanel controller) {

		for (MenuItem menuItem : menu.getItems()) {

			// menu
			if (menuItem instanceof Menu) {
				processMenu((Menu) menuItem, controller);
			}
			else {

				// has accelerator
				if (menuItem.getAccelerator() != null) {

					// create labels
					Label label1 = new Label(menuItem.getText());
					Label label2 = new Label(menuItem.getAccelerator().getDisplayText());
					HBox.setHgrow(label2, Priority.NEVER);

					// create containers
					HBox hbox1 = new HBox();
					hbox1.setAlignment(Pos.CENTER_RIGHT);
					hbox1.setMaxWidth(Double.MAX_VALUE);
					HBox hbox2 = new HBox();
					hbox2.setAlignment(Pos.CENTER_LEFT);
					hbox2.setMaxWidth(Double.MAX_VALUE);
					HBox.setHgrow(hbox2, Priority.ALWAYS);
					hbox2.getChildren().add(label1);
					hbox1.getChildren().add(hbox2);
					hbox1.getChildren().add(label2);
					controller.container_.getChildren().add(hbox1);
				}
			}
		}
	}
}
