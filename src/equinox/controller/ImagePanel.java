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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

/**
 * Class for image panel controller.
 *
 * @author Murat Artim
 * @date Dec 22, 2014
 * @time 6:07:23 PM
 */
public class ImagePanel implements Initializable {

	@FXML
	private VBox root_;

	@FXML
	private ImageView image_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param image
	 *            Image to display.
	 * @return The root component.
	 */
	public static VBox load(Image image) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ImagePanel.fxml"));
			fxmlLoader.load();

			// get controller
			ImagePanel controller = (ImagePanel) fxmlLoader.getController();

			// set attributes
			controller.image_.setFitHeight(image.getHeight());
			controller.image_.setFitWidth(image.getWidth());
			controller.image_.setImage(image);

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
