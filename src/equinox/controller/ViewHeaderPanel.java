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
import equinox.viewer.HeaderPanel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Class for 3D Viewer header panel controller.
 *
 * @author Murat Artim
 * @date Sep 15, 2015
 * @time 9:40:56 AM
 */
public class ViewHeaderPanel implements Initializable {

	/** Owner panel. */
	private HeaderPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private Label title_, subTitle_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Returns the root container of this panel.
	 *
	 * @return The root container of this panel.
	 */
	public VBox getRoot() {
		return root_;
	}

	/**
	 * Returns the parent panel of this panel.
	 *
	 * @return The parent panel of this panel.
	 */
	public HeaderPanel getOwner() {
		return owner_;
	}

	/**
	 * Sets header of this panel.
	 *
	 * @param title
	 *            Title.
	 * @param subTitle
	 *            Sub-title.
	 */
	public void setHeader(String title, String subTitle) {
		title_.setText(title);
		subTitle_.setText(subTitle);
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static ViewHeaderPanel load(HeaderPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ViewHeaderPanel.fxml"));
			fxmlLoader.load();

			// get controller
			ViewHeaderPanel controller = (ViewHeaderPanel) fxmlLoader.getController();

			// set attributes
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
