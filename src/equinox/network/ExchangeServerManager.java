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
package equinox.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import equinox.Equinox;
import equinox.controller.MainScreen;
import equinox.controller.NotificationPanel1;
import equinox.data.Settings;
import equinox.exchangeServer.remote.Registry;
import equinox.exchangeServer.remote.listener.ExchangeMessageListener;
import equinox.exchangeServer.remote.message.ExchangeMessage;
import equinox.exchangeServer.remote.message.ExchangeServerStatisticsMessage;
import equinox.exchangeServer.remote.message.HandshakeWithExchangeServer;
import equinox.exchangeServer.remote.message.WhoRequest;
import equinox.serverUtilities.BigMessage;
import equinox.serverUtilities.NetworkMessage;
import equinox.serverUtilities.PartialMessage;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.PermissionDenied;
import equinox.serverUtilities.SplitMessage;
import equinox.utility.Utility;
import javafx.application.Platform;

/**
 * Class for exchange server manager.
 *
 * @author Murat Artim
 * @date 24 Jun 2018
 * @time 22:50:48
 */
public class ExchangeServerManager implements ExchangeMessageListener {

	/** Serial id. */
	private static final long serialVersionUID = 1L;

	/** The kryonet client of network watcher. */
	private final Client kryoNetClient_;

	/** The owner of the network watcher. */
	private final MainScreen owner_;

	/** Analysis message listeners. */
	private List<ExchangeMessageListener> messageListeners_;

	/** Stop indicator of the network watcher. */
	private volatile boolean isHandShaken_ = false;

	/** The thread executor of the network watcher. */
	private final ExecutorService threadExecutor_;

	/** Message queue. */
	private final ConcurrentLinkedQueue<ExchangeMessage> messageQueue_;

	/**
	 * Creates exchange server manager.
	 *
	 * @param owner
	 *            The owner main screen.
	 */
	public ExchangeServerManager(MainScreen owner) {

		// set owner
		owner_ = owner;

		// create message queue
		messageQueue_ = new ConcurrentLinkedQueue<>();

		// create analysis message listener list
		messageListeners_ = Collections.synchronizedList(new ArrayList<ExchangeMessageListener>());

		// create thread executor of the network watcher
		threadExecutor_ = Executors.newSingleThreadExecutor();

		// create kryonet client
		kryoNetClient_ = new Client(65536, 8192);

		// register objects to be sent over network
		Registry.register(kryoNetClient_);

		// add server handler (listener)
		kryoNetClient_.addListener(new ExchangeServerListener());

		// start client
		kryoNetClient_.start();
		Equinox.LOGGER.info("Exchange server client initialized.");
	}

	/**
	 * Adds given exchange message listener.
	 *
	 * @param listener
	 *            Exchange message listener to add.
	 */
	public void addMessageListener(ExchangeMessageListener listener) {
		synchronized (messageListeners_) {
			messageListeners_.add(listener);
		}
	}

	/**
	 * Removes the given exchange message listener.
	 *
	 * @param listener
	 *            Exchange message listener to remove.
	 */
	public void removeMessageListener(ExchangeMessageListener listener) {
		synchronized (messageListeners_) {
			messageListeners_.remove(listener);
		}
	}

	/**
	 * Returns the owner screen.
	 *
	 * @return The owner screen.
	 */
	public MainScreen getOwner() {
		return owner_;
	}

	/**
	 * Connects to analysis server.
	 *
	 * @param message
	 *            Message to be sent once the connection is established. Can be <code>null</code>.
	 * @param showWarning
	 *            True to show warning if exception occurs during connecting.
	 */
	public void connect(ExchangeMessage message, boolean showWarning) {

		// already connected
		if (kryoNetClient_.isConnected()) {
			if (message != null) {
				sendMessage(message);
			}
			return;
		}

		// submit new task
		threadExecutor_.submit(() -> {

			// add message to queue
			if (message != null) {
				messageQueue_.add(message);
			}

			// connect to server
			try {
				String hostname = (String) owner_.getSettings().getValue(Settings.EXCHANGE_SERVER_HOSTNAME);
				int port = Integer.parseInt((String) owner_.getSettings().getValue(Settings.EXCHANGE_SERVER_PORT));
				kryoNetClient_.connect(5000, hostname, port);
				kryoNetClient_.sendTCP(new HandshakeWithExchangeServer(Equinox.USER.getAlias()));
				return true;
			}

			// exception occurred during connecting to server
			catch (IOException e) {

				// log error
				Equinox.LOGGER.log(Level.WARNING, "Exception occurred during connecting to collaboration service.", e);

				// notify UI
				if (showWarning) {
					Platform.runLater(() -> {
						String msg = "Cannot connect to collaboration service. Please check your network connection and collaboration service connection settings.";
						owner_.getNotificationPane().showWarning(msg, null);
						messageQueue_.clear();

					});
				}
				return false;
			}
		});
	}

	/**
	 * Sends given message to the server.
	 *
	 * @param message
	 *            Message to send.
	 */
	synchronized public void sendMessage(ExchangeMessage message) {

		// null message
		if (message == null)
			return;

		// not connected
		if (!kryoNetClient_.isConnected()) {
			Platform.runLater(() -> {
				String msg = "Collaboration service is currently not available. Please connect to the service and try again.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.BOTTOM_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(msg, 40, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(owner_.getInputPanel().getExchangeServiceButton());
			});
			return;
		}

		// submit new task
		threadExecutor_.submit(() -> {

			try {

				// not a big message
				if (message instanceof BigMessage == false) {
					kryoNetClient_.sendTCP(message);
					return;
				}

				// not big
				if (!((BigMessage) message).isReallyBig()) {
					kryoNetClient_.sendTCP(message);
					return;
				}

				// split message into partial messages
				PartialMessage[] parts = SplitMessage.splitMessage((BigMessage) message);

				// no need to split message
				if (parts == null) {
					kryoNetClient_.sendTCP(message);
					return;
				}

				// send parts
				for (PartialMessage part : parts) {
					kryoNetClient_.sendTCP(part);
				}
			}

			// exception occurred during sending message
			catch (Exception e) {

				// log error
				Equinox.LOGGER.log(Level.SEVERE, "Exception occurred during sending network message to collaboration service.", e);

				// show warning
				Platform.runLater(() -> {
					String msg = "Exception occurred during sending message to collaboration service: " + e.getLocalizedMessage();
					msg += " Click 'Details' for more information.";
					owner_.getNotificationPane().showError("Problem encountered", msg, e);
				});
			}
		});
	}

	/**
	 * Disconnects from server. Note that, this method doesn't stop the network thread.
	 */
	public void disconnect() {

		// disconnect kryonet client
		kryoNetClient_.close();
		Equinox.LOGGER.info("Disconnected from collaboration service.");
	}

	/**
	 * Disconnects from server and stops network thread.
	 */
	public void stop() {

		// stop thread executor
		Utility.shutdownThreadExecutor(threadExecutor_);
		Equinox.LOGGER.info("Exchange server manager network thread executor stopped.");

		// stop kryonet client
		kryoNetClient_.stop();
		Equinox.LOGGER.info("Disconnected from collaboration service.");
	}

	/**
	 * Returns true if there is active connection to server.
	 *
	 * @return True if there is active connection to server.
	 */
	public boolean isConnected() {
		return kryoNetClient_.isConnected() && isHandShaken_;
	}

	@Override
	public void respondToExchangeMessage(ExchangeMessage message) throws Exception {

		// handshake
		if (message instanceof HandshakeWithExchangeServer) {
			processHandshake((HandshakeWithExchangeServer) message);
		}
	}

	/**
	 * Processes handshake message from the server.
	 *
	 * @param handshake
	 *            Handshake message.
	 */
	private void processHandshake(HandshakeWithExchangeServer handshake) {

		// successful
		if (handshake.isHandshakeSuccessful()) {

			// log
			Equinox.LOGGER.info("Successfully connected to collaboration service.");
			isHandShaken_ = true;

			// send who request
			if (Equinox.USER.hasPermission(Permission.SEE_CONNECTED_USERS, false, null)) {
				sendMessage(new WhoRequest());
			}

			// send all queued message (if any)
			ExchangeMessage message;
			while ((message = messageQueue_.poll()) != null) {
				sendMessage(message);
			}
		}

		// unsuccessful
		else {
			String text = "Cannot connect to collaboration service. Please check your network connection and collaboration service connection settings.";
			owner_.getNotificationPane().showWarning(text, null);
			messageQueue_.clear();
		}
	}

	/**
	 * Inner class for exchange server listener. Server handler listens for incoming messages and passes them to registered listener.
	 *
	 * @author Murat Artim
	 * @time 5:51:08 PM
	 * @date Jul 11, 2011
	 */
	private class ExchangeServerListener extends Listener {

		/** Map containing the partial messages and their ancestors' hash codes. */
		private final HashMap<Integer, PartialMessage[]> partialMessages_ = new HashMap<>();

		@Override
		public void connected(Connection connection) {

			// log connection
			Equinox.LOGGER.info("Successfully connected to collaboration service.");

			// set 20 seconds timeout for connection (this will allow 12 seconds of network latency)
			connection.setTimeout(20000);

			// notify UI
			Platform.runLater(() -> {
				owner_.getInputPanel().exchangeServiceConnectionStatusChanged(true);
			});
		}

		@Override
		public void received(Connection connection, final Object object) {

			// unsupported protocol
			if (object == null || object instanceof NetworkMessage == false)
				return;

			// respond
			threadExecutor_.submit(() -> {

				try {

					// partial message
					if (object instanceof PartialMessage) {
						receivePartialMessage((PartialMessage) object);
					}

					// permission denied message
					else if (object instanceof PermissionDenied) {
						Platform.runLater(() -> {
							owner_.getNotificationPane().showPermissionDenied(((PermissionDenied) object).getPermission());
						});
					}

					// exchange message
					else if (object instanceof ExchangeMessage) {
						respond((ExchangeMessage) object);
					}
				}

				// exception occurred during responding to message
				catch (Exception e) {

					// log warning
					Equinox.LOGGER.log(Level.WARNING, "Exception occurred during responding to collaboration service message.", e);

					// show warning
					Platform.runLater(() -> {
						String message = "Exception occurred during responding to collaboration service message: " + e.getLocalizedMessage();
						message += " Click 'Details' for more information.";
						owner_.getNotificationPane().showError("Problem encountered", message, e);
					});
				}
			});
		}

		@Override
		public void disconnected(Connection connection) {

			// log warning
			Equinox.LOGGER.log(Level.WARNING, "Connection to collaboration service lost.");

			// notify UI
			Platform.runLater(() -> {
				owner_.getInputPanel().exchangeServiceConnectionStatusChanged(false);
			});
		}

		/**
		 * Responds to partial messages received from the server.
		 *
		 * @param part
		 *            Partial message to respond.
		 * @throws Exception
		 *             If exception occurs during process.
		 */
		private void receivePartialMessage(PartialMessage part) throws Exception {

			// get message parameters
			int id = part.getID();
			int numParts = part.getNumParts();
			int index = part.getIndex();

			// get all current parts
			PartialMessage[] parts = partialMessages_.get(id);

			// ID not contained
			if (parts == null) {
				parts = new PartialMessage[numParts];
				parts[index] = part;
				partialMessages_.put(id, parts);
			}

			// contained
			else {
				parts[index] = part;
			}

			// check whether all parts completed
			for (PartialMessage p : parts)
				if (p == null)
					return;

			// combine parts and respond
			BigMessage combinedMessage = SplitMessage.combineMessages(parts);

			// exchange message
			if (combinedMessage instanceof ExchangeMessage) {
				respond((ExchangeMessage) combinedMessage);
			}

			// remove parts
			partialMessages_.remove(id);
		}

		/**
		 * Responds to given exchange network message.
		 *
		 * @param message
		 *            Exchange message to respond to.
		 * @throws Exception
		 *             If exception occurs during process.
		 */
		private void respond(ExchangeMessage message) throws Exception {

			// handshake (respond with server manager)
			if (message instanceof HandshakeWithExchangeServer) {
				respondToExchangeMessage(message);
			}

			// other messages
			else if (message instanceof ExchangeServerStatisticsMessage) {

				// cast
				ExchangeServerStatisticsMessage statisticMessage = (ExchangeServerStatisticsMessage) message;

				// sync over listeners
				synchronized (messageListeners_) {

					// get listeners
					Iterator<ExchangeMessageListener> i = messageListeners_.iterator();

					// loop over listeners
					while (i.hasNext()) {

						// get listener
						ExchangeMessageListener c = i.next();

						// listener hash code matches to message analysis ID
						if (c.hashCode() == statisticMessage.getListenerHashCode()) {
							c.respondToExchangeMessage(message);
							break;
						}
					}
				}
			}

			// other messages (respond with main screen)
			else {
				owner_.respondToExchangeMessage(message);
			}
		}
	}
}