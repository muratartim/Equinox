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

import equinox.Equinox;
import equinox.controller.InputPanel.InputPopup;
import equinox.data.EquinoxTheme;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.exchangeServer.remote.message.ChatMessage;
import equinox.exchangeServer.remote.message.ShareFile;
import equinox.exchangeServer.remote.message.StatusChange;
import equinox.serverUtilities.Permission;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

/**
 * Class for chat panel controller.
 *
 * @author Murat Artim
 * @date Sep 18, 2014
 * @time 11:31:41 AM
 */
public class ChatPopup implements InputPopup, ListChangeListener<ExchangeUser> {

	/** The owner panel. */
	private InputPanel owner_;

	/** Pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	@FXML
	private VBox root_, messageContainer_;

	@FXML
	private ComboBox<ExchangeUser> recipient_;

	@FXML
	private TextArea message_;

	@FXML
	private ScrollPane scroll_;

	@FXML
	private Button send_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// bind scroll position to container height
		messageContainer_.heightProperty().addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> scroll_.setVvalue(scroll_.getVmax()));

		// set button handler
		send_.setOnAction(actionEvent -> sendMessage());

		// set text area handler
		message_.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				sendMessage();
			}
		});
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public void onChanged(javafx.collections.ListChangeListener.Change<? extends ExchangeUser> c) {

		// get currently selected member
		ExchangeUser selected = recipient_.getValue();

		// clear items and add new items
		recipient_.getItems().setAll(c.getList());

		// null selection
		if (selected == null) {
			recipient_.getSelectionModel().clearSelection();
			recipient_.setValue(null);
		}

		// there was selection
		else {
			if (recipient_.getItems().contains(selected)) {
				recipient_.getSelectionModel().select(selected);
				recipient_.setValue(selected);
			}
			else {
				recipient_.getSelectionModel().clearSelection();
				recipient_.setValue(null);
			}
		}
	}

	/**
	 * Shows chat panel.
	 *
	 * @param recipient
	 *            Initial selection.
	 */
	public void show(ExchangeUser recipient) {

		// not shown
		if (!isShown_) {

			// select recipient
			if (recipient != null) {
				if (recipient_.getItems().contains(recipient)) {
					recipient_.getSelectionModel().select(recipient);
					recipient_.setValue(recipient);
				}
			}

			// create pop-over
			popOver_ = new PopOver();
			popOver_.setDetached(true);
			popOver_.setTitle("Messages");
			popOver_.setContentNode(root_);

			// set showing handler
			popOver_.setOnShowing(event -> isShown_ = true);

			// set hidden handler
			popOver_.setOnHidden(event -> isShown_ = false);

			// show
			popOver_.show(owner_.getOwner().getOwner().getStage());
		}
	}

	/**
	 * Returns true if this panel is shown.
	 *
	 * @return True if this panel is shown.
	 */
	public boolean isShown() {
		return isShown_;
	}

	/**
	 * Adds given chat message to this panel.
	 *
	 * @param message
	 *            Chat message to add.
	 */
	public void addChatMessage(ChatMessage message) {
		messageContainer_.getChildren().add(ChatMessagePanel.load(message.getMessage(), message.getSender()));
	}

	/**
	 * Adds given share file message to this panel.
	 *
	 * @param message
	 *            Share file chat message to add.
	 */
	public void addShareFileChatMessage(ShareFile message) {
		messageContainer_.getChildren().add(ShareFileChatMessagePanel.load(this, message));
	}

	/**
	 * Called when send button is clicked or enter is pressed in text field.
	 */
	private void sendMessage() {

		// get inputs
		ExchangeUser recipient = recipient_.getValue();
		String messageText = message_.getText();
		messageText = messageText != null ? messageText.trim() : messageText;

		// check inputs
		if (!checkInputs(recipient, messageText))
			return;

		// no permission
		if (!Equinox.USER.hasPermission(Permission.SEND_CHAT_MESSAGE, true, owner_.getOwner()))
			return;

		// create message
		ChatMessage message = new ChatMessage(messageText, recipient);
		message.setSender(Equinox.USER.createExchangeUser());

		// send message
		owner_.getOwner().getExchangeServerManager().sendMessage(message);

		// clear text area
		message_.clear();
	}

	/**
	 * Checks message inputs and displays warning message if needed.
	 *
	 * @param recipient
	 *            Recipient of message.
	 * @param messageText
	 *            Message text.
	 * @return True if message is acceptable.
	 */
	private boolean checkInputs(ExchangeUser recipient, String messageText) {

		// initialize warning message
		String warning = null;
		Node node = null;

		// this user is not available
		if (!owner_.getOwner().isAvailable()) {

			// create confirmation action
			PopOver popOver = new PopOver();
			EventHandler<ActionEvent> handler = event -> {
				owner_.getOwner().getExchangeServerManager().sendMessage(new StatusChange(Equinox.USER.createExchangeUser(), true));
				popOver.hide();
			};

			// show question
			warning = "Your status is currently set to 'Busy'. Would you like to set it to 'Available' to send messages?";
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel2.load(popOver, warning, 50, "Yes", handler, NotificationPanel2.QUESTION));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(send_);
			return false;
		}

		// recipient
		else if (recipient == null) {
			warning = "Please select a recipient to send the message.";
			node = recipient_;
		}

		// message text
		else if (messageText == null || messageText.isEmpty()) {
			warning = "Please enter a message to send.";
			node = message_;
		}

		// all valid inputs
		if (warning == null)
			return true;

		// show warning
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel1.load(warning, 30, NotificationPanel1.WARNING));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(node);
		return false;
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static ChatPopup load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ChatPopup.fxml"));
			fxmlLoader.load();

			// get controller
			ChatPopup controller = (ChatPopup) fxmlLoader.getController();

			// set attributes
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
