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
import equinox.task.SubmitBugReport;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

/**
 * Class for warning notification panel.
 *
 * @author Murat Artim
 * @date Nov 17, 2014
 * @time 4:44:24 PM
 */
public class WarningPanel implements Initializable {

	/** The owner main screen. */
	private MainScreen mainScreen_;

	@FXML
	private VBox root_;

	@FXML
	private Label title_, message_;

	@FXML
	private TextArea warningText_;

	@FXML
	private TitledPane warningPane_;

	@FXML
	private ImageView detailsImage_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onShowWarningsClicked() {
		warningPane_.setExpanded(!warningPane_.isExpanded());
		detailsImage_.setImage(warningPane_.isExpanded() ? Utility.getImage("arrowUpWhite.png") : Utility.getImage("arrowDownWhite.png"));
	}

	@FXML
	private void onReportBugClicked() {
		BugReportPanel panel = (BugReportPanel) mainScreen_.getInputPanel().getSubPanel(InputPanel.BUG_REPORT_PANEL);
		mainScreen_.getActiveTasksPanel().runTaskInParallel(new SubmitBugReport(warningText_.getText(), true, true, panel));
		mainScreen_.getNotificationPane().hide();
	}

	/**
	 * Loads and returns warning notification panel.
	 *
	 * @param mainScreen
	 *            Main screen.
	 * @param title
	 *            Notification title.
	 * @param message
	 *            Message text.
	 * @param warnings
	 *            Warning messages.
	 * @return The newly loaded warning notification panel.
	 */
	public static VBox load(MainScreen mainScreen, String title, String message, String warnings) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("WarningPanel.fxml"));
			fxmlLoader.load();

			// get controller
			WarningPanel controller = (WarningPanel) fxmlLoader.getController();

			// set attributes
			controller.mainScreen_ = mainScreen;
			controller.title_.setText(title);
			controller.message_.setText(message);
			controller.warningText_.appendText(warnings);

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
}
