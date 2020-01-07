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
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import equinox.analysisServer.remote.listener.AnalysisMessageListener;
import equinox.analysisServer.remote.message.AnalysisMessage;
import equinox.analysisServer.remote.message.RestartAnalysisServerRequest;
import equinox.analysisServer.remote.message.RestartAnalysisServerRequestFailed;
import equinox.analysisServer.remote.message.RestartAnalysisServerResponse;
import equinox.analysisServer.remote.message.StopAnalysisServerRequest;
import equinox.analysisServer.remote.message.StopAnalysisServerRequestFailed;
import equinox.analysisServer.remote.message.StopAnalysisServerResponse;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.utility.Utility;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

/**
 * Class for manage service panel controller.
 *
 * @author Murat Artim
 * @date 12 Jul 2018
 * @time 01:36:54
 */
public class ManageServicePanel implements InternalInputSubPanel, AnalysisMessageListener {

	/** Serial id. */
	private static final long serialVersionUID = 1L;

	/** Service index. */
	public static final int ANALYSIS_SERVICE = 0, DATA_SERVICE = 1, EXCHANGE_SERVICE = 2;

	/** The owner panel. */
	private InputPanel owner_;

	/** Service index. */
	private int service_ = ANALYSIS_SERVICE;

	@FXML
	private VBox root_;

	@FXML
	private PasswordField password_;

	@FXML
	private ComboBox<String> operation_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// add operations
		operation_.getItems().setAll("Stop service", "Restart service");
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

		// register as message listeners
		owner_.getOwner().getAnalysisServerManager().addMessageListener(this);
		// TODO register as data message listener
		// TODO register as exchange message listener
	}

	@Override
	public void showing() {
		onResetClicked();
	}

	@Override
	public String getHeader() {
		return "Manage Service";
	}

	/**
	 * Sets service to manage.
	 *
	 * @param serviceIndex
	 *            Index of service to manage.
	 */
	public void setService(int serviceIndex) {
		service_ = serviceIndex;
	}

	@Override
	public void respondToAnalysisMessage(AnalysisMessage message) throws Exception {

		// run in javafx thread
		Platform.runLater(() -> {

			// stop analysis server response
			if (message instanceof StopAnalysisServerResponse) {
				boolean succeeded = ((StopAnalysisServerResponse) message).isStopped();
				if (succeeded) {
					String title = "Stop Analysis Service";
					String msg = "Analysis service successfully stopped.";
					owner_.getOwner().getNotificationPane().showOk(title, msg);
				}
				else {
					String title = "Stop Analysis Service";
					String msg = "Analysis service could not be stopped due to incorrect service password. Please retry with a valid service password.";
					owner_.getOwner().getNotificationPane().showWarning(msg, title);
				}
			}

			// stop analysis server failed
			else if (message instanceof StopAnalysisServerRequestFailed) {
				String exceptionMsg = ((StopAnalysisServerRequestFailed) message).getExceptionMessage();
				String title = "Stop Analysis Service";
				String msg = "Exception occurred during stopping analysis service. Click 'Details' for more information.";
				owner_.getOwner().getNotificationPane().showError(title, msg, new Exception(exceptionMsg));
			}

			// restart analysis server response
			else if (message instanceof RestartAnalysisServerResponse) {
				boolean succeeded = ((RestartAnalysisServerResponse) message).isRestarted();
				if (succeeded) {
					String title = "Restart Analysis Service";
					String msg = "Analysis service successfully restarted.";
					owner_.getOwner().getNotificationPane().showOk(title, msg);
				}
				else {
					String title = "Restart Analysis Service";
					String msg = "Analysis service could not be restarted due to incorrect service password. Please retry with a valid service password.";
					owner_.getOwner().getNotificationPane().showWarning(msg, title);
				}
			}

			// restart analysis server failed
			else if (message instanceof RestartAnalysisServerRequestFailed) {
				String exceptionMsg = ((RestartAnalysisServerRequestFailed) message).getExceptionMessage();
				String title = "Restart Analysis Service";
				String msg = "Exception occurred during restarting analysis service. Click 'Details' for more information.";
				owner_.getOwner().getNotificationPane().showError(title, msg, new Exception(exceptionMsg));
			}
		});
	}

	@FXML
	private void onOkClicked() {

		// get inputs
		String password = password_.getText();
		String operation = operation_.getSelectionModel().getSelectedItem();

		// check invalid values
		if (!checkInvalidValues(password, operation))
			return;

		// create encryptor
		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();

		// analysis service
		if (service_ == ANALYSIS_SERVICE) {

			// set encryptor password
			encryptor.setPassword("EquinoxAnalysisServer_2018");

			// stop
			if (operation.equals("Stop service")) {
				StopAnalysisServerRequest request = new StopAnalysisServerRequest();
				request.setListenerHashCode(hashCode());
				request.setPassword(encryptor.encrypt(password));
				owner_.getOwner().getAnalysisServerManager().sendMessage(request);
			}

			// restart
			else if (operation.equals("Restart service")) {
				RestartAnalysisServerRequest request = new RestartAnalysisServerRequest();
				request.setListenerHashCode(hashCode());
				request.setPassword(encryptor.encrypt(password));
				owner_.getOwner().getAnalysisServerManager().sendMessage(request);
			}
		}

		// data service
		else if (service_ == DATA_SERVICE) {
			// TODO manage data service
		}

		// exchange service
		else if (service_ == EXCHANGE_SERVICE) {
			// TODO manage exchange service
		}

		// show file view panel
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {
		password_.clear();
	}

	/**
	 * Checks for invalid input values.
	 *
	 * @param password
	 *            Password.
	 * @param operation
	 *            Operation to perform.
	 * @return True if the values are valid.
	 */
	private boolean checkInvalidValues(String password, String operation) {

		// initialize message
		String message = null;
		Node node = null;

		// check password
		if (password == null || password.trim().isEmpty()) {
			message = "Invalid service password. Please enter valid service password to proceed.";
			node = password_;
		}

		// check operation
		else if (operation == null || operation.trim().isEmpty()) {
			message = "No operation selected. Please select a service operation to proceed.";
			node = operation_;
		}

		// invalid value found
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(node);
			return false;
		}

		// valid values
		return true;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static ManageServicePanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ManageServicePanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			ManageServicePanel controller = (ManageServicePanel) fxmlLoader.getController();

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