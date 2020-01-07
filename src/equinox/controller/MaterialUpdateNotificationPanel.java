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

import equinox.data.EquinoxTheme;
import equinox.task.UpdateMaterialLibrary;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;

/**
 * Class for material update notification panel controller.
 *
 * @author Murat Artim
 * @date 31 May 2017
 * @time 15:20:58
 *
 */
public class MaterialUpdateNotificationPanel implements Initializable {

	/** The owner main screen. */
	private MainScreen mainScreen_;

	/** List containing the material ISAMI versions to be downloaded. */
	private ArrayList<String> toBeDownloaded_ = null;

	@FXML
	private VBox root_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onYesClicked() {
		mainScreen_.getNotificationPane().hide();
		mainScreen_.getActiveTasksPanel().runTaskInParallel(new UpdateMaterialLibrary(toBeDownloaded_));
	}

	@FXML
	private void onNoClicked() {
		mainScreen_.getNotificationPane().hide();
	}

	/**
	 * Loads material update notification panel.
	 *
	 * @param mainScreen
	 *            Main screen.
	 * @param toBeDownloaded
	 *            List containing the material ISAMI versions to be downloaded.
	 * @return The newly loaded panel root.
	 */
	public static VBox load(MainScreen mainScreen, ArrayList<String> toBeDownloaded) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("MaterialUpdateNotificationPanel.fxml"));
			fxmlLoader.load();

			// get controller
			MaterialUpdateNotificationPanel controller = (MaterialUpdateNotificationPanel) fxmlLoader.getController();

			// set attributes
			controller.mainScreen_ = mainScreen;
			controller.toBeDownloaded_ = toBeDownloaded;

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
}
