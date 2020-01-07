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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import equinox.dataServer.remote.Registry;
import equinox.dataServer.remote.listener.DataMessageListener;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.HandshakeWithDataServer;
import equinox.dataServer.remote.message.LoginFailed;
import equinox.dataServer.remote.message.LoginSuccessful;
import equinox.serverUtilities.BigMessage;
import equinox.serverUtilities.NetworkMessage;
import equinox.serverUtilities.PartialMessage;
import equinox.serverUtilities.PermissionDenied;
import equinox.serverUtilities.SplitMessage;
import equinox.task.CheckUserAuthentication;
import equinox.task.SaveUserAuthentication;
import equinox.utility.Utility;
import javafx.application.Platform;

/**
 * Class for data server manager.
 *
 * @author Murat Artim
 * @date 25 Jun 2018
 * @time 13:46:41
 */
public class DataServerManager implements DataMessageListener {

	/** Serial id. */
	private static final long serialVersionUID = 1L;

	/** The kryonet client of network watcher. */
	private final Client kryoNetClient_;

	/** The owner of the network watcher. */
	private final MainScreen owner_;

	/** Analysis message listeners. */
	private List<DataMessageListener> messageListeners_;

	/** Stop indicator of the network watcher. */
	private volatile boolean isHandShaken_ = false;

	/** The thread executor of the network watcher. */
	private final ExecutorService threadExecutor_;

	/** Message queue. */
	private final ConcurrentLinkedQueue<DataMessage> messageQueue_;

	/**
	 * Creates data server manager.
	 *
	 * @param owner
	 *            The owner main screen.
	 */
	public DataServerManager(MainScreen owner) {

		// set owner
		owner_ = owner;

		// create message queue
		messageQueue_ = new ConcurrentLinkedQueue<>();

		// create analysis message listener list
		messageListeners_ = Collections.synchronizedList(new ArrayList<DataMessageListener>());

		// create thread executor of the network watcher
		threadExecutor_ = Executors.newSingleThreadExecutor();

		// create kryonet client
		kryoNetClient_ = new Client(65536, 8192);

		// register objects to be sent over network
		Registry.register(kryoNetClient_);

		// add server handler (listener)
		kryoNetClient_.addListener(new DataServerListener());

		// start client
		kryoNetClient_.start();

		// add this class to message listeners
		addMessageListener(this);
		Equinox.LOGGER.info("Data server client initialized.");
	}

	/**
	 * Adds given data message listener.
	 *
	 * @param listener
	 *            Data message listener to add.
	 */
	public void addMessageListener(DataMessageListener listener) {
		synchronized (messageListeners_) {
			messageListeners_.add(listener);
		}
	}

	/**
	 * Removes the given data message listener.
	 *
	 * @param listener
	 *            Data message listener to remove.
	 */
	public void removeMessageListener(DataMessageListener listener) {
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
	 * Connects to data server.
	 *
	 * @param message
	 *            Message to be sent once the connection is established. Can be <code>null</code>.
	 * @param showWarning
	 *            True to show warning if exception occurs during connecting.
	 */
	public void connect(DataMessage message, boolean showWarning) {

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

				// connect to kryonet server
				String hostname = (String) owner_.getSettings().getValue(Settings.DATA_SERVER_HOSTNAME);
				int port = Integer.parseInt((String) owner_.getSettings().getValue(Settings.DATA_SERVER_PORT));
				kryoNetClient_.connect(5000, hostname, port);

				// create handshake message
				HandshakeWithDataServer handshake = new HandshakeWithDataServer(Equinox.USER.getAlias());
				handshake.setListenerHashCode(hashCode());

				// send handshake message to server
				kryoNetClient_.sendTCP(handshake);
				return true;
			}

			// exception occurred during connecting to server
			catch (IOException e) {

				// log error
				Equinox.LOGGER.log(Level.WARNING, "Exception occurred during connecting to data service.", e);

				// notify UI
				if (showWarning) {
					Platform.runLater(() -> {
						String msg = "Cannot connect to data service. Please check your network connection and data service connection settings.";
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
	synchronized public void sendMessage(DataMessage message) {

		// null message
		if (message == null)
			return;

		// not connected
		if (!kryoNetClient_.isConnected()) {
			Platform.runLater(() -> {
				String msg = "Data service is currently not available. Please connect to the service and try again.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.BOTTOM_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(msg, 40, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(owner_.getInputPanel().getDataServiceButton());
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
				Equinox.LOGGER.log(Level.SEVERE, "Exception occurred during sending network message to data service.", e);

				// show warning
				Platform.runLater(() -> {
					String msg = "Exception occurred during sending message to data service: " + e.getLocalizedMessage();
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
		kryoNetClient_.close();
		Equinox.LOGGER.info("Disconnected from data service.");
	}

	/**
	 * Disconnects from server and stops network thread.
	 */
	public void stop() {

		// stop thread executor
		Utility.shutdownThreadExecutor(threadExecutor_);
		Equinox.LOGGER.info("Data server manager network thread executor stopped.");

		// stop kryonet client
		kryoNetClient_.stop();
		Equinox.LOGGER.info("Disconnected from data service.");
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
	public void respondToDataMessage(DataMessage message) throws Exception {

		// handshake
		if (message instanceof HandshakeWithDataServer) {
			processHandshake((HandshakeWithDataServer) message);
		}
	}

	/**
	 * Processes handshake message from the server.
	 *
	 * @param handshake
	 *            Handshake message.
	 */
	private void processHandshake(HandshakeWithDataServer handshake) {

		// successful
		if (handshake.isHandshakeSuccessful()) {

			// log
			Equinox.LOGGER.info("Successfully connected to data service.");
			isHandShaken_ = true;

			// set user attributes
			Equinox.USER.setUsername(handshake.getUsername());
			Equinox.USER.setAsAdministrator(handshake.isAdministrator());

			// add non-administrative permissions
			handshake.getPermissionNames().forEach(p -> Equinox.USER.addPermission(p));

			// setup administrator menu
			owner_.getMenuBarPanel().setupAdministratorMenu(handshake.isAdministrator());

			// send all queued message (if any)
			DataMessage message;
			while ((message = messageQueue_.poll()) != null) {
				sendMessage(message);
			}

			// save user authentication
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveUserAuthentication(Equinox.USER));

			// start scheduled thread pool
			CheckUserAuthentication check = new CheckUserAuthentication(owner_.getInputPanel());
			((ScheduledExecutorService) Equinox.SCHEDULED_THREADPOOL).scheduleAtFixedRate(check, 60, 120, TimeUnit.SECONDS);
		}

		// unsuccessful
		else {
			String text = "Cannot connect to data service. Please check your network connection and data service connection settings.";
			owner_.getNotificationPane().showWarning(text, null);
			messageQueue_.clear();
		}
	}

	/**
	 * Inner class for data server listener. Server handler listens for incoming messages and passes them to registered listener.
	 *
	 * @author Murat Artim
	 * @time 5:51:08 PM
	 * @date Jul 11, 2011
	 */
	private class DataServerListener extends Listener {

		/** Map containing the partial messages and their ancestors' hash codes. */
		private final HashMap<Integer, PartialMessage[]> partialMessages_ = new HashMap<>();

		@Override
		public void connected(Connection connection) {

			// log connection
			Equinox.LOGGER.info("Successfully connected to data service.");

			// set 20 seconds timeout for connection (this will allow 12 seconds of network latency)
			connection.setTimeout(20000);

			// notify UI
			Platform.runLater(() -> {
				owner_.getInputPanel().dataServiceConnectionStatusChanged(true);
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

					// data message
					else if (object instanceof DataMessage) {
						respond((DataMessage) object);
					}
				}

				// exception occurred during responding to message
				catch (Exception e) {

					// log warning
					Equinox.LOGGER.log(Level.WARNING, "Exception occurred during responding to data service message.", e);

					// show warning
					Platform.runLater(() -> {
						String message = "Exception occurred during responding to data service message: " + e.getLocalizedMessage();
						message += " Click 'Details' for more information.";
						owner_.getNotificationPane().showError("Problem encountered", message, e);
					});
				}
			});
		}

		@Override
		public void disconnected(Connection connection) {

			// reset handshake
			isHandShaken_ = false;

			// log warning
			Equinox.LOGGER.log(Level.WARNING, "Connection to data service lost.");

			// notify UI
			Platform.runLater(() -> {
				owner_.getInputPanel().dataServiceConnectionStatusChanged(false);
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

			// data message
			if (combinedMessage instanceof DataMessage) {
				respond((DataMessage) combinedMessage);
			}

			// remove parts
			partialMessages_.remove(id);
		}

		/**
		 * Responds to given data network message.
		 *
		 * @param message
		 *            Data message to respond to.
		 * @throws Exception
		 *             If exception occurs during process.
		 */
		private void respond(DataMessage message) throws Exception {

			// login message
			if (message instanceof LoginSuccessful || message instanceof LoginFailed) {
				owner_.respondToDataMessage(message);
			}

			// other messages
			else {

				// sync over listeners
				synchronized (messageListeners_) {

					// get listeners
					Iterator<DataMessageListener> i = messageListeners_.iterator();

					// loop over listeners
					while (i.hasNext()) {

						// get listener
						DataMessageListener c = i.next();

						// listener hash code matches to message analysis ID
						if (c.hashCode() == message.getListenerHashCode()) {
							c.respondToDataMessage(message);
							break;
						}
					}
				}
			}
		}
	}
}