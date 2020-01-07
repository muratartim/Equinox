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
package equinox.task;

import equinox.Equinox;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.exchangeServer.remote.message.ChatMessage;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for send text message task.
 *
 * @author Murat Artim
 * @date 21 Sep 2018
 * @time 15:33:55
 */
public class SendTextMessage extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** Message text and recipient username. */
	private final String messageText;

	/** Recipient. */
	private final ExchangeUser recipient;

	/**
	 * Creates send text message task.
	 *
	 * @param message
	 *            Message text.
	 * @param recipient
	 *            Recipient username.
	 */
	public SendTextMessage(String message, ExchangeUser recipient) {
		this.messageText = message;
		this.recipient = recipient;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Send text message";
	}

	@Override
	protected Void call() throws Exception {

		// check inputs
		if (!checkInputs(recipient, messageText))
			return null;

		// no permission
		if (!Equinox.USER.hasPermission(Permission.SEND_CHAT_MESSAGE, true, taskPanel_.getOwner().getOwner()))
			return null;

		// create message
		ChatMessage message = new ChatMessage(messageText, recipient);
		message.setSender(Equinox.USER.createExchangeUser());

		// send message
		taskPanel_.getOwner().getOwner().getExchangeServerManager().sendMessage(message);

		// return
		return null;
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

		// this user is not available
		if (!taskPanel_.getOwner().getOwner().isAvailable()) {
			addWarning("Your status is currently set to 'Busy'. Please set it to 'Available' to send messages.");
			return false;
		}

		// recipient
		else if (recipient == null) {
			addWarning("Please supply a recipient to send the message.");
			return false;
		}

		// message text
		else if (messageText == null || messageText.isEmpty()) {
			addWarning("Please supply a message text to send the message.");
			return false;
		}

		// valid inputs
		return true;
	}
}