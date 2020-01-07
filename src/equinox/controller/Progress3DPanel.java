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
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

/**
 * Class for progress 3D panel controller.
 *
 * @author Murat Artim
 * @date Apr 28, 2016
 * @time 12:50:00 PM
 */
public class Progress3DPanel implements Initializable {

	/** The owner panel. */
	private ObjectViewPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private ProgressIndicator progress_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Returns the owner panel.
	 *
	 * @return The owner panel.
	 */
	public ObjectViewPanel getOwner() {
		return owner_;
	}

	/**
	 * Returns the root container.
	 *
	 * @return The root container.
	 */
	public Parent getRoot() {
		return root_;
	}

	/**
	 * Returns the progress indicator.
	 *
	 * @return The progress indicator.
	 */
	public ProgressIndicator getProgress() {
		return progress_;
	}

	/**
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot text view panel.
	 */
	public static Progress3DPanel load(ObjectViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("Progress3DPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			Progress3DPanel controller = (Progress3DPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
