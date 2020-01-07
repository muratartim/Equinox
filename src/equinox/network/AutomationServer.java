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
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.logging.Level;

import equinox.Equinox;
import equinox.controller.MainScreen;

/**
 * Class for automation server. Automation server listens for connections from automation clients and executes automation requests as they arrive.
 *
 * @author Murat Artim
 * @date 23 Sep 2018
 * @time 22:50:55
 */
public class AutomationServer extends Thread {

	/** Automation server port. */
	private static final int AUTOMATION_SERVER_PORT = 1789;

	/** The owner of the server. */
	private final MainScreen owner;

	/** Server socket. */
	private ServerSocket socket;

	/**
	 * Creates automation server.
	 *
	 * @param owner
	 *            The owner of this server.
	 */
	public AutomationServer(MainScreen owner) {
		super("Equinox Automation Server");
		this.owner = owner;
	}

	/**
	 * Returns the owner screen.
	 *
	 * @return The owner screen.
	 */
	public MainScreen getOwner() {
		return owner;
	}

	/**
	 * Stops server.
	 */
	public void stopServer() {

		// close socket
		try {
			socket.close();
		}

		// exception occurred during process.
		catch (IOException e) {
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during stopping automation server.", e);
		}
	}

	@Override
	public void run() {

		// create server socket
		try {
			socket = new ServerSocket(AUTOMATION_SERVER_PORT);
			Equinox.LOGGER.info("Automation server socket created on port " + AUTOMATION_SERVER_PORT + ".");
		}

		// exception occurred during process
		catch (IOException e) {
			String message = "Exception occurred during creating automation server socket on port " + AUTOMATION_SERVER_PORT + ".";
			Equinox.LOGGER.log(Level.WARNING, message, e);
			return;
		}

		// wait for incoming connections
		try {
			owner.getInputPanel().automationServiceStatusChanged(true);
			Equinox.LOGGER.info("Automation server started listening for incoming connections on port " + AUTOMATION_SERVER_PORT + ".");
			while (true) {
				Equinox.CACHED_THREADPOOL.submit(new AutomationClientHandler(this, socket.accept()));
			}
		}

		// socket exception
		catch (SocketException e) {

			// notify UI
			owner.getInputPanel().automationServiceStatusChanged(false);

			// socket closed
			if (socket.isClosed()) {
				Equinox.LOGGER.info("Automation server socket closed.");
			}

			// socket failed
			else {
				Equinox.LOGGER.log(Level.WARNING, "Exception occurred during accepting client connection.", e);
			}
		}

		// exception occurred during process
		catch (IOException e) {
			owner.getInputPanel().automationServiceStatusChanged(false);
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during accepting client connection.", e);
		}
	}
}