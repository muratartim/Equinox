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
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

import equinox.Equinox;
import equinox.data.EquinoxTheme;
import equinox.exchangeServer.remote.data.ExchangeUser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Class for chat message controller.
 *
 * @author Murat Artim
 * @date Sep 18, 2014
 * @time 2:32:05 PM
 */
public class ChatMessagePanel implements Initializable {

	@FXML
	private VBox root_, baloon_;

	@FXML
	private Label id_, time_, message_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Loads and returns chat message panel.
	 *
	 * @param message
	 *            Message text.
	 * @param sender
	 *            Sender.
	 * @return Chat message panel.
	 */
	public static VBox load(String message, ExchangeUser sender) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ChatMessagePanel.fxml"));
			fxmlLoader.load();

			// get controller
			ChatMessagePanel controller = (ChatMessagePanel) fxmlLoader.getController();

			// set attributes
			boolean isSent = sender.equals(Equinox.USER.createExchangeUser());
			controller.message_.setText(message);
			controller.message_.setTextFill(isSent ? Color.WHITE : Color.BLACK);
			controller.id_.setText(isSent ? "You" : sender.toString());
			controller.id_.setTextFill(isSent ? Color.STEELBLUE : Color.BLACK);
			DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US);
			controller.time_.setText(format.format(new Date()));
			controller.baloon_.getStyleClass().add(isSent ? "chatBaloonSent" : "chatBaloonReceived");

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
