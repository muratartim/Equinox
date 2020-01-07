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
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.apache.commons.text.WordUtils;

import equinox.data.ClientPluginInfo;
import equinox.data.EquinoxTheme;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Class for plugin update notification panel controller.
 *
 * @author Murat Artim
 * @date Apr 1, 2015
 * @time 12:06:13 PM
 */
public class PluginUpdateNotificationPanel implements Initializable {

	/** The owner main screen. */
	private MainScreen mainScreen_;

	/** To-be-updated plugins. */
	private ArrayList<ClientPluginInfo> toBeUpdated_;

	@FXML
	private VBox root_;

	@FXML
	private Label text_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onButtonClicked() {
		PluginViewPanel panel = (PluginViewPanel) mainScreen_.getViewPanel().getSubPanel(ViewPanel.PLUGIN_VIEW);
		panel.setPlugins(toBeUpdated_);
		mainScreen_.getViewPanel().showSubPanel(ViewPanel.PLUGIN_VIEW);
		mainScreen_.getNotificationPane().hide();
	}

	/**
	 * Loads and returns shared file notification panel.
	 *
	 * @param mainScreen
	 *            Main screen.
	 * @param message
	 *            Message.
	 * @param toBeUpdated
	 *            To-be-updated plugins.
	 * @return The newly loaded shared file panel.
	 */
	public static VBox load(MainScreen mainScreen, String message, ArrayList<ClientPluginInfo> toBeUpdated) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("PluginUpdateNotificationPanel.fxml"));
			fxmlLoader.load();

			// get controller
			PluginUpdateNotificationPanel controller = (PluginUpdateNotificationPanel) fxmlLoader.getController();

			// set attributes
			controller.mainScreen_ = mainScreen;
			controller.toBeUpdated_ = toBeUpdated;
			// DEPRECATION using org.apache.commons.text.WordUtils instead of org.apache.commons.lang3.text.WordUtils
			controller.text_.setText(WordUtils.wrap(message, 100));

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
}
