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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.EquinoxUpdate;
import equinox.dataServer.remote.data.EquinoxUpdate.EquinoxUpdateInfoType;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.web.WebView;

/**
 * Class for update details panel controller.
 *
 * @author Murat Artim
 * @date May 15, 2016
 * @time 12:35:55 PM
 */
public class UpdateDetailsPanel implements Initializable {

	/** The owner panel. */
	private UpdatePanel owner_;

	@FXML
	private TitledPane root_;

	@FXML
	private WebView view_;

	@FXML
	private Label versionLabel_, dateLabel_, sizeLabel_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onInstallClicked() {
		owner_.onInstallClicked();
	}

	/**
	 * Loads and returns error notification panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @param update
	 *            Update message.
	 * @return The newly loaded error notification panel.
	 */
	public static TitledPane load(UpdatePanel owner, EquinoxUpdate update) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("UpdateDetailsPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			UpdateDetailsPanel controller = (UpdateDetailsPanel) fxmlLoader.getController();

			// set labels
			controller.owner_ = owner;
			controller.versionLabel_.setText("Version: " + (double) update.getInfo(EquinoxUpdateInfoType.VERSION_NUMBER));
			controller.dateLabel_.setText("Date: " + new SimpleDateFormat("dd/MM/yyyy").format(update.getInfo(EquinoxUpdateInfoType.UPLOAD_DATE)));
			controller.sizeLabel_.setText("Download size: " + new DecimalFormat("#.##").format((long) update.getInfo(EquinoxUpdateInfoType.DATA_SIZE) / (1024.0 * 1024.0)) + "MB");

			// load version description page
			controller.view_.getEngine().loadContent((String) update.getInfo(EquinoxUpdateInfoType.VERSION_DESCRIPTION));

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
}
