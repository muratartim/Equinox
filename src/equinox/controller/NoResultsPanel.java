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
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Class for no results panel controller.
 *
 * @author Murat Artim
 * @date Nov 29, 2015
 * @time 9:57:56 PM
 */
public class NoResultsPanel implements Initializable {

	@FXML
	private Label message_, suggestions_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Loads and returns no results panel parent element.
	 *
	 * @param message
	 *            Message to be displayed on the panel.
	 * @param suggestions
	 *            Suggestions to be displayed. Can be null for standard suggestions.
	 * @return The newly loaded panel.
	 */
	public static VBox load(String message, String suggestions) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("NoResultsPanel.fxml"));
			VBox root = (VBox) fxmlLoader.load();

			// get controller
			NoResultsPanel controller = (NoResultsPanel) fxmlLoader.getController();

			// set attributes
			controller.message_.setText(message);
			if (suggestions == null) {
				suggestions = "Suggestions:\n";
				suggestions += "\t- Make sure all words are spelled correctly.\n";
				suggestions += "\t- Try different keywords.\n";
				suggestions += "\t- Try more general keywords.\n";
			}
			controller.suggestions_.setText(suggestions);

			// return root
			return root;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
