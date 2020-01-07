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

import org.apache.commons.text.WordUtils;

import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.EquinoxUpdate;
import equinox.task.DownloadContainerUpdate;
import equinox.utility.Utility;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Class for update notification panel.
 *
 * @author Murat Artim
 * @date May 14, 2016
 * @time 2:29:40 PM
 */
public class UpdatePanel implements Initializable {

	/** The owner main screen. */
	private MainScreen mainScreen_;

	/** Details pane. */
	private TitledPane detailsPane_;

	/** Update message. */
	private EquinoxUpdate update_;

	@FXML
	private VBox root_;

	@FXML
	private Label message_;

	@FXML
	private ImageView detailsImage_;

	@FXML
	private Button install_;

	@FXML
	private StackPane stack_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	public void onInstallClicked() {

		// hide notification
		mainScreen_.getNotificationPane().hide();

		// start update
		mainScreen_.getActiveTasksPanel().runTaskInParallel(new DownloadContainerUpdate(update_));

		// show modal task panel
		mainScreen_.getActiveTasksPanel().showModal();
	}

	@FXML
	private void onDetailsClicked() {

		// no details pane
		if (detailsPane_ == null) {

			// run later
			Platform.runLater(() -> {

				// load details pane
				detailsPane_ = UpdateDetailsPanel.load(UpdatePanel.this, update_);
				stack_.getChildren().add(0, detailsPane_);

				// expand
				detailsPane_.setExpanded(!detailsPane_.isExpanded());
				detailsImage_.setImage(detailsPane_.isExpanded() ? Utility.getImage("arrowUpWhite.png") : Utility.getImage("arrowDownWhite.png"));
			});
		}

		// details pane already loaded
		else {
			detailsPane_.setExpanded(!detailsPane_.isExpanded());
			detailsImage_.setImage(detailsPane_.isExpanded() ? Utility.getImage("arrowUpWhite.png") : Utility.getImage("arrowDownWhite.png"));
		}
	}

	/**
	 * Loads and returns error notification panel.
	 *
	 * @param mainScreen
	 *            Main screen.
	 * @param message
	 *            Message text.
	 * @param update
	 *            Update message.
	 * @return The newly loaded error notification panel.
	 */
	public static VBox load(MainScreen mainScreen, String message, EquinoxUpdate update) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("UpdatePanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			UpdatePanel controller = (UpdatePanel) fxmlLoader.getController();

			// set attributes
			controller.mainScreen_ = mainScreen;
			controller.message_.setText(WordUtils.wrap(message, 100));
			controller.update_ = update;

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
}
