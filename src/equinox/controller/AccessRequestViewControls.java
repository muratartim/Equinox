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
import equinox.dataServer.remote.message.GetAccessRequestsRequest;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Class for access request view controls controller.
 *
 * @author Murat Artim
 * @date 15 Apr 2018
 * @time 14:36:29
 */
public class AccessRequestViewControls implements Initializable {

	/** The owner panel. */
	private AccessRequestViewPanel owner_;

	@FXML
	private HBox root_;

	@FXML
	private MenuButton status_;

	@FXML
	private RadioMenuItem all_, pending_, granted_, rejected_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set root to grow always
		HBox.setHgrow(root_, Priority.ALWAYS);
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
	public HBox getRoot() {
		return root_;
	}

	/**
	 * Resets this panel.
	 *
	 * @param status
	 *            Status index.
	 */
	public void reset(int status) {
		if (status == GetAccessRequestsRequest.ALL) {
			all_.setSelected(true);
			status_.setText(all_.getText());
		}
		else if (status == GetAccessRequestsRequest.PENDING) {
			pending_.setSelected(true);
			status_.setText(pending_.getText());
		}
		else if (status == GetAccessRequestsRequest.GRANTED) {
			granted_.setSelected(true);
			status_.setText(granted_.getText());
		}
		else if (status == GetAccessRequestsRequest.REJECTED) {
			rejected_.setSelected(true);
			status_.setText(rejected_.getText());
		}
	}

	@FXML
	private void onStatusSelected() {

		// update period according to selection
		if (all_.isSelected()) {
			if (!status_.getText().equals(all_.getText())) {
				status_.setText(all_.getText());
				owner_.onStatusSelected(GetAccessRequestsRequest.ALL);
			}
		}
		else if (pending_.isSelected()) {
			if (!status_.getText().equals(pending_.getText())) {
				status_.setText(pending_.getText());
				owner_.onStatusSelected(GetAccessRequestsRequest.PENDING);
			}
		}
		else if (granted_.isSelected()) {
			if (!status_.getText().equals(granted_.getText())) {
				status_.setText(granted_.getText());
				owner_.onStatusSelected(GetAccessRequestsRequest.GRANTED);
			}
		}
		else if (rejected_.isSelected()) {
			if (!status_.getText().equals(rejected_.getText())) {
				status_.setText(rejected_.getText());
				owner_.onStatusSelected(GetAccessRequestsRequest.REJECTED);
			}
		}
	}

	/**
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot text view panel.
	 */
	public static AccessRequestViewControls load(AccessRequestViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("AccessRequestViewControls.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			AccessRequestViewControls controller = (AccessRequestViewControls) fxmlLoader.getController();

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
