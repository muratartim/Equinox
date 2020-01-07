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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.Equinox;
import equinox.data.EquinoxTheme;
import equinox.data.Settings;
import equinox.dataServer.remote.data.Wish;
import equinox.dataServer.remote.data.Wish.WishInfo;
import equinox.serverUtilities.Permission;
import equinox.task.LikeWish;
import equinox.task.SaveSettings;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Class for wish panel.
 *
 * @author Murat Artim
 * @date May 16, 2014
 * @time 2:09:00 PM
 */
public class WishPanel implements Initializable {

	/** The owner panel. */
	private RoadmapViewPanel owner_;

	/** Wish. */
	private Wish wish_;

	/** Close link. */
	private Hyperlink close_;

	/** Closure label. */
	private Label closure_, closedBy_;

	@FXML
	private VBox root_;

	@FXML
	private Label title_, description_, info_, status_;

	@FXML
	private Hyperlink like_;

	@FXML
	private HBox linkContainer_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create closure label
		closure_ = new Label();
		closure_.setTextFill(Color.GREY);
		closure_.setWrapText(true);
		closure_.setPadding(new Insets(5, 0, 0, 0));
		VBox.setVgrow(closure_, Priority.ALWAYS);

		// create closed by label
		closedBy_ = new Label();
		closedBy_.setTextFill(Color.GREY);

		// create close link
		ImageView image = new ImageView(Utility.getImage("cancel.png"));
		image.setFitWidth(12);
		image.setFitHeight(12);
		close_ = new Hyperlink("Close", image);

		// set on action
		close_.setOnAction(event -> {

			// show popup
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
			popOver.setTitle("Close Wish");
			popOver.setContentNode(CloseWishPanel.load(popOver, WishPanel.this, wish_));
			popOver.setHideOnEscape(true);
			popOver.show(close_);
		});
	}

	/**
	 * Returns the owner panel of this sub panel.
	 *
	 * @return The owner panel of this sub panel.
	 */
	public RoadmapViewPanel getOwner() {
		return owner_;
	}

	/**
	 * Returns the root of this controller.
	 *
	 * @return The root of this controller.
	 */
	public VBox getRoot() {
		return root_;
	}

	/**
	 * Sets wish to panel.
	 *
	 * @param wish
	 *            Wish to set.
	 */
	public void setWish(Wish wish) {

		// set data
		wish_ = wish;

		// setup components
		title_.setText((String) wish_.getInfo(WishInfo.TITLE));
		String description = (String) wish_.getInfo(WishInfo.DESCRIPTION);
		if (description != null && !description.isEmpty()) {
			description_.setText(description);
		}
		else {
			root_.getChildren().remove(description_);
		}
		String status = (String) wish_.getInfo(WishInfo.STATUS);
		status_.setText(status);
		status_.setTextFill(status.equals(Wish.OPEN) ? Color.GREEN : Color.FIREBRICK);
		String closure = (String) wish_.getInfo(WishInfo.CLOSURE);
		if (closure != null && !closure.isEmpty()) {
			closure_.setText("Closure:\n" + closure);
			root_.getChildren().add(2, closure_);
		}
		String owner = (String) wish_.getInfo(WishInfo.OWNER);
		String info = "Owner: " + (owner == null ? "unknown" : owner);
		info += ", Date: " + new SimpleDateFormat("dd/MM/yyyy").format((Timestamp) wish_.getInfo(WishInfo.RECORDED));
		info += ", Likes: " + (int) wish_.getInfo(WishInfo.LIKES);
		info_.setText(info);

		// setup like button
		Settings settings = owner_.getOwner().getOwner().getSettings();
		like_.setVisible(!settings.isWishLiked((long) wish_.getInfo(WishInfo.ID)));

		// add close button for administrator
		if (status.equals(Wish.OPEN)) {
			if (Equinox.USER.isLoggedAsAdministrator()) {
				if (Equinox.USER.hasPermission(Permission.CLOSE_WISH, false, null)) {
					linkContainer_.getChildren().add(close_);
				}
			}
		}

		// add closed by
		if (status.equals(Wish.CLOSED)) {
			closedBy_.setText("Closed by: " + (String) wish_.getInfo(WishInfo.CLOSED_BY));
			linkContainer_.getChildren().clear();
			linkContainer_.getChildren().add(closedBy_);
		}
	}

	/**
	 * Called after the like task is successfully completed.
	 *
	 */
	public void wishLiked() {

		// setup like button
		like_.setVisible(false);

		// set info
		String owner = (String) wish_.getInfo(WishInfo.OWNER);
		String info = "Owner: " + (owner == null ? "unknown" : owner);
		info += ", Date: " + new SimpleDateFormat("dd/MM/yyyy").format((Timestamp) wish_.getInfo(WishInfo.RECORDED));
		info += ", Likes: " + ((int) wish_.getInfo(WishInfo.LIKES) + 1);
		info_.setText(info);

		// add wish ID to liked wishes
		Settings settings = owner_.getOwner().getOwner().getSettings();
		settings.addToLikedWishes((long) wish_.getInfo(WishInfo.ID));

		// save settings and get all wishes
		owner_.getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(new SaveSettings(settings, false));
	}

	@FXML
	private void onLikeClicked() {
		owner_.getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(new LikeWish(wish_, this));
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static WishPanel load(RoadmapViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("WishPanel.fxml"));
			fxmlLoader.load();

			// get controller
			WishPanel controller = (WishPanel) fxmlLoader.getController();

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
