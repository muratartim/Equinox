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

import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.AccessRequest;
import equinox.task.CloseAccessRequest;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Class for close access request panel controller.
 *
 * @author Murat Artim
 * @date 15 Apr 2018
 * @time 20:04:31
 */
public class CloseAccessRequestPanel implements Initializable {

	/** The owner panel. */
	private AccessRequestPanel panel_;

	/** Request to close. */
	private AccessRequest request_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** True if access is granted. */
	private boolean isGrantAccess_;

	@FXML
	private VBox root_;

	@FXML
	private TextArea closureText_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onOkClicked() {

		// no closure text supplied
		String closure = closureText_.getText();

		// no title given
		if (closure == null || closure.trim().isEmpty()) {
			String message = "Please enter reason for closure in order to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(closureText_);
			return;
		}

		// send request
		popOver_.hide();
		panel_.getOwner().getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(new CloseAccessRequest(request_, closureText_.getText(), isGrantAccess_));
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param popOver
	 *            The owner pop-over.
	 * @param panel
	 *            The owner panel.
	 * @param request
	 *            Access request to close.
	 * @param isGrantAccess
	 *            True if access is granted.
	 * @return The newly loaded plot column panel.
	 */
	public static VBox load(PopOver popOver, AccessRequestPanel panel, AccessRequest request, boolean isGrantAccess) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CloseAccessRequestPanel.fxml"));
			fxmlLoader.load();

			// get controller
			CloseAccessRequestPanel controller = (CloseAccessRequestPanel) fxmlLoader.getController();

			// set attributes
			controller.popOver_ = popOver;
			controller.panel_ = panel;
			controller.request_ = request;
			controller.isGrantAccess_ = isGrantAccess;

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
