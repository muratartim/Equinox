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
package equinox.data.ui;

import java.awt.TrayIcon.MessageType;

import org.apache.commons.text.WordUtils;
import org.controlsfx.control.action.Action;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * Inner class for notification.
 *
 * @author Murat Artim
 * @date Nov 9, 2014
 * @time 7:17:49 PM
 */
public class Notification {

	/** Message type to be displayed on system tray. */
	private final MessageType messageType_;

	/** Notification text. */
	private final String text_, systemTrayText_;

	/** Notification graphic. */
	private final Node graphic_;

	/** Notification actions. */
	private final Action[] actions_;

	/** Location of notification. */
	private final boolean hasTimer_, isModal_;

	/**
	 * Creates notification.
	 *
	 * @param messageType
	 *            Message type to be displayed on system tray.
	 * @param text
	 *            Notification text. Can be null.
	 * @param wrapLength
	 *            Wrapping length of text. Can be -1 for displaying text only for system tray messages.
	 * @param graphic
	 *            Notification graphic.
	 * @param hasTimer
	 *            True if notification has timer.
	 * @param isModal
	 *            True if this notification is modal.
	 * @param actions
	 *            Notification actions. Can be null.
	 */
	public Notification(MessageType messageType, String text, int wrapLength, Node graphic, boolean hasTimer, boolean isModal, Action... actions) {

		// set text and location
		messageType_ = messageType;
		text_ = (text == null) || (wrapLength == -1) ? null : WordUtils.wrap(text, wrapLength);
		hasTimer_ = hasTimer;
		isModal_ = isModal;

		// set system tray text
		if (text == null) {
			systemTrayText_ = null;
		}
		else {
			String stt = WordUtils.wrap(text, 40);
			if (stt.length() > 120) {
				systemTrayText_ = stt.substring(0, 117) + "...";
			}
			else {
				systemTrayText_ = stt;
			}
		}

		// set graphic
		VBox container = new VBox(graphic);
		container.setPadding(new Insets(5, 0, 5, 0));
		graphic_ = container;

		// set actions
		actions_ = actions;
		if (actions_ != null) {
			for (Action action : actions_) {
				action.setStyle("-fx-base:steelblue; -fx-text-fill:white; -fx-cursor: hand;");
			}
		}
	}

	/**
	 * Returns message type to be displayed on system tray.
	 *
	 * @return Message type to be displayed on system tray.
	 */
	public MessageType getMessageType() {
		return messageType_;
	}

	/**
	 * Returns notification text.
	 *
	 * @return Notification text.
	 */
	public String getText() {
		return text_;
	}

	/**
	 * Returns notification text for system tray.
	 *
	 * @return Notification text for system tray.
	 */
	public String getSystemTrayText() {
		return systemTrayText_;
	}

	/**
	 * Returns notification graphic.
	 *
	 * @return Notification graphic.
	 */
	public Node getGraphic() {
		return graphic_;
	}

	/**
	 * Returns true if notification has timer.
	 *
	 * @return True if notification has timer.
	 */
	public boolean hasTimer() {
		return hasTimer_;
	}

	/**
	 * Returns true if this notification is modal.
	 *
	 * @return True if this notification is modal.
	 */
	public boolean isModal() {
		return isModal_;
	}

	/**
	 * Returns notification actions or null if no action defined.
	 *
	 * @return Notification actions or null if no action defined.
	 */
	public Action[] getActions() {
		return actions_;
	}
}
