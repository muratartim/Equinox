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

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.Settings;
import equinox.dataServer.remote.message.GetWishesRequest;
import equinox.task.GetWishes;
import equinox.task.SaveSettings;
import equinox.task.SubmitWish;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Class for roadmap panel.
 *
 * @author Murat Artim
 * @date May 16, 2014
 * @time 12:16:50 PM
 */
public class RoadmapPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_, roadmapPane_;

	@FXML
	private TextField title_;

	@FXML
	private TextArea description_;

	@FXML
	private Label label1_, label2_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public void start() {

		// bind label widths
		label1_.prefWidthProperty().bind(root_.widthProperty().subtract(44.0));
		label2_.prefWidthProperty().bind(root_.widthProperty().subtract(44.0));
	}

	@Override
	public void showing() {
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetWishes(GetWishesRequest.OPEN));
	}

	@Override
	public String getHeader() {
		return "Roadmap";
	}

	/**
	 * Called when a wish is successfully submitted.
	 *
	 * @param wishID
	 *            Wish ID.
	 */
	public void wishSubmitted(long wishID) {

		// add wish ID to liked wishes
		Settings settings = owner_.getSettings();
		settings.addToLikedWishes(wishID);

		// save settings and get all wishes
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new SaveSettings(settings, false));

		// get wishes
		tm.runTaskInParallel(new GetWishes(GetWishesRequest.OPEN));

		// reset panel
		onResetClicked();
	}

	/**
	 * Called when a wish is closed.
	 *
	 */
	public void wishClosed() {
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetWishes(GetWishesRequest.CLOSED));
	}

	@FXML
	private void onOkClicked() {

		// get inputs
		String title = title_.getText();
		String description = description_.getText().isEmpty() ? null : description_.getText();

		// no title given
		if (title == null || title.isEmpty()) {
			String message = "No title given. Please enter a title in order to proceed submitting your wish.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(title_);
			return;
		}

		// start task
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SubmitWish(title, description, this));
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {
		title_.clear();
		description_.clear();
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("Equinox roadmap", null);
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static RoadmapPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("RoadmapPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			RoadmapPanel controller = (RoadmapPanel) fxmlLoader.getController();

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
