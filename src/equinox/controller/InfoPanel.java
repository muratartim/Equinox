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
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Class for info panel controller.
 *
 * @author Murat Artim
 * @date Sep 12, 2014
 * @time 5:34:46 PM
 */
public class InfoPanel implements Initializable {

	@FXML
	private VBox root_;

	@FXML
	private TextArea info_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param infoText
	 *            Information text.
	 * @return The newly loaded plot column panel.
	 */
	public static VBox load(String infoText) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("InfoPanel.fxml"));
			fxmlLoader.load();

			// get controller
			InfoPanel controller = (InfoPanel) fxmlLoader.getController();

			// set attributes
			if ((infoText == null) || infoText.isEmpty()) {
				infoText = "No information is available.";
			}
			controller.info_.setText(infoText);

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
