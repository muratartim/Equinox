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
import equinox.dataServer.remote.data.AccessRequest;
import equinox.dataServer.remote.data.AccessRequest.AccessRequestInfo;
import equinox.serverUtilities.Permission;
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
 * Class for user access request panel controller.
 *
 * @author Murat Artim
 * @date 15 Apr 2018
 * @time 14:59:34
 */
public class AccessRequestPanel implements Initializable {

	/** The owner panel. */
	private AccessRequestViewPanel owner_;

	/** Access request. */
	private AccessRequest request_;

	/** Action links. */
	private Hyperlink grant_, reject_;

	/** Closure label. */
	private Label closure_, closedBy_;

	@FXML
	private VBox root_;

	@FXML
	private Label permission_, status_, username_, alias_, organization_, email_, date_;

	@FXML
	private HBox container_;

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

		// create reject link
		ImageView rejectImage = new ImageView(Utility.getImage("cancel.png"));
		rejectImage.setFitWidth(12);
		rejectImage.setFitHeight(12);
		reject_ = new Hyperlink("Reject", rejectImage);

		// create grant link
		ImageView grantImage = new ImageView(Utility.getImage("grant.png"));
		grantImage.setFitWidth(12);
		grantImage.setFitHeight(12);
		grant_ = new Hyperlink("Grant", grantImage);

		// set on action
		reject_.setOnAction(event -> closeRequest(reject_));
		grant_.setOnAction(event -> closeRequest(grant_));
	}

	/**
	 * Returns the owner panel of this sub panel.
	 *
	 * @return The owner panel of this sub panel.
	 */
	public AccessRequestViewPanel getOwner() {
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
	 * Sets access request to panel.
	 *
	 * @param request
	 *            Access request to set.
	 */
	public void setRequest(AccessRequest request) {

		// set data
		request_ = request;

		// setup components
		permission_.setText((String) request_.getInfo(AccessRequestInfo.PERMISSION_NAME));
		username_.setText("User name: " + (String) request_.getInfo(AccessRequestInfo.USERNAME));
		alias_.setText("User alias: " + (String) request_.getInfo(AccessRequestInfo.USER_ALIAS));
		organization_.setText("User organization: " + (String) request_.getInfo(AccessRequestInfo.USER_ORGANIZATION));
		email_.setText("User email: " + (String) request_.getInfo(AccessRequestInfo.USER_EMAIL));
		String status = (String) request_.getInfo(AccessRequestInfo.STATUS);
		status_.setText(status);
		status_.setTextFill(status.equals(AccessRequest.PENDING) ? Color.FIREBRICK : Color.GREEN);
		String closure = (String) request_.getInfo(AccessRequestInfo.CLOSURE);
		if (closure != null && !closure.isEmpty()) {
			closure_.setText("Closure:\n" + closure);
			root_.getChildren().add(5, closure_);
		}
		date_.setText("Request date: " + new SimpleDateFormat("dd/MM/yyyy").format((Timestamp) request_.getInfo(AccessRequestInfo.RECORDED)));

		// add action buttons for administrator
		if (status.equals(AccessRequest.PENDING)) {
			if (Equinox.USER.isLoggedAsAdministrator()) {
				if (Equinox.USER.hasPermission(Permission.CLOSE_ACCESS_REQUEST, false, null)) {
					container_.getChildren().add(reject_);
					container_.getChildren().add(grant_);
				}
			}
		}

		// add closed by
		else {
			closedBy_.setText("Closed by: " + (String) request_.getInfo(AccessRequestInfo.CLOSED_BY));
			container_.getChildren().clear();
			container_.getChildren().add(closedBy_);
		}
	}

	/**
	 * Closes this request.
	 *
	 * @param link
	 *            Action link.
	 */
	private void closeRequest(Hyperlink link) {
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
		popOver.setTitle(link.equals(grant_) ? "Grant Access" : "Reject Request");
		popOver.setContentNode(CloseAccessRequestPanel.load(popOver, AccessRequestPanel.this, request_, link.equals(grant_)));
		popOver.setHideOnEscape(true);
		popOver.show(link);
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static AccessRequestPanel load(AccessRequestViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("AccessRequestPanel.fxml"));
			fxmlLoader.load();

			// get controller
			AccessRequestPanel controller = (AccessRequestPanel) fxmlLoader.getController();

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
