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

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import equinox.Equinox;
import equinox.data.Settings;
import equinox.serverUtilities.FilerConnection;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get server connection info task.
 *
 * @author Murat Artim
 * @date 3 Aug 2016
 * @time 15:44:35
 */
public class GetServerConnectionInfo extends TemporaryFileCreatingTask<HashMap<String, String>> implements ShortRunningTask {

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Getting server connection info...";
	}

	@Override
	protected HashMap<String, String> call() throws Exception {

		// update info
		updateMessage("Getting server connection info...");

		// create mapping
		HashMap<String, String> connectionInfo = new HashMap<>();

		// connect to file server
		try (FilerConnection filer = getFilerConnection()) {

			// data server
			getDataServerConnectionInfo(connectionInfo, filer);

			// exchange server
			getExchangeServerConnectionInfo(connectionInfo, filer);

			// analysis server
			getAnalysisServerConnectionInfo(connectionInfo, filer);
		}

		// return connection info
		return connectionInfo;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get connection info
			HashMap<String, String> info = get();

			// null info
			if (info == null || info.isEmpty()) {
				String msg = "Server connection information could not be obtained.";
				Equinox.LOGGER.warning(msg);
				taskPanel_.getOwner().getOwner().getNotificationPane().showWarning(msg, null);
				return;
			}

			// get connection info
			Settings settings = taskPanel_.getOwner().getOwner().getSettings();

			// set to settings
			if (info.containsKey("dataServerHostname") && info.containsKey("dataServerPort")) {
				settings.setValue(Settings.DATA_SERVER_HOSTNAME, info.get("dataServerHostname"));
				settings.setValue(Settings.DATA_SERVER_PORT, info.get("dataServerPort"));
			}
			if (info.containsKey("exchangeServerHostname") && info.containsKey("exchangeServerPort")) {
				settings.setValue(Settings.EXCHANGE_SERVER_HOSTNAME, info.get("exchangeServerHostname"));
				settings.setValue(Settings.EXCHANGE_SERVER_PORT, info.get("exchangeServerPort"));
			}
			if (info.containsKey("analysisServerHostname") && info.containsKey("analysisServerPort")) {
				settings.setValue(Settings.ANALYSIS_SERVER_HOSTNAME, info.get("analysisServerHostname"));
				settings.setValue(Settings.ANALYSIS_SERVER_PORT, info.get("analysisServerPort"));
			}

			// save settings
			taskPanel_.getOwner().runTaskInParallel(new SaveSettings(settings, false));
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}

		// connect to services
		finally {
			taskPanel_.getOwner().getOwner().getAnalysisServerManager().connect(null, false);
			taskPanel_.getOwner().getOwner().getExchangeServerManager().connect(null, false);
			taskPanel_.getOwner().getOwner().getDataServerManager().connect(null, false);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// connect to services
		taskPanel_.getOwner().getOwner().getAnalysisServerManager().connect(null, false);
		taskPanel_.getOwner().getOwner().getExchangeServerManager().connect(null, false);
		taskPanel_.getOwner().getOwner().getDataServerManager().connect(null, false);
	}

	/**
	 * Retrieves data server connection information.
	 *
	 * @param connectionInfo
	 *            Mapping to store the information.
	 * @param filer
	 *            File server connection.
	 */
	private void getDataServerConnectionInfo(HashMap<String, String> connectionInfo, FilerConnection filer) {

		try {

			// create path to connection settings file
			Path connSettingsFile = getWorkingDirectory().resolve(FilerConnection.DATA_SERVER_CONN_FILE);

			// get URL to connection settings file
			String url = filer.getDirectoryPath(FilerConnection.SETTINGS) + "/" + FilerConnection.DATA_SERVER_CONN_FILE;

			// download
			if (filer.fileExists(url)) {
				filer.getSftpChannel().get(url, connSettingsFile.toString());
			}

			// cannot locate connection settings file
			else
				throw new Exception("Cannot locate data server connection settings file on filer. Cannot connect to data server.");

			// create file reader
			try (BufferedReader reader = Files.newBufferedReader(connSettingsFile, Charset.defaultCharset())) {

				// read file till the end
				String line;
				while ((line = reader.readLine()) != null) {

					// empty or comment line
					if (line.startsWith("#") || line.trim().isEmpty()) {
						continue;
					}

					// split line
					String[] split = line.split("=");
					connectionInfo.put(split[0].trim().replaceFirst("server", "dataServer"), split[1].trim());
				}
			}
		}

		// exception occurred
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Cannot retrieve data server connection information.", e);
		}
	}

	/**
	 * Retrieves exchange server connection information.
	 *
	 * @param connectionInfo
	 *            Mapping to store the information.
	 * @param filer
	 *            File server connection.
	 */
	private void getExchangeServerConnectionInfo(HashMap<String, String> connectionInfo, FilerConnection filer) {

		try {

			// create path to connection settings file
			Path connSettingsFile = getWorkingDirectory().resolve(FilerConnection.EXCHANGE_SERVER_CONN_FILE);

			// get URL to connection settings file
			String url = filer.getDirectoryPath(FilerConnection.SETTINGS) + "/" + FilerConnection.EXCHANGE_SERVER_CONN_FILE;

			// download
			if (filer.fileExists(url)) {
				filer.getSftpChannel().get(url, connSettingsFile.toString());
			}

			// cannot locate connection settings file
			else
				throw new Exception("Cannot locate exchange server connection settings file on filer. Cannot connect to exchange server.");

			// create file reader
			try (BufferedReader reader = Files.newBufferedReader(connSettingsFile, Charset.defaultCharset())) {

				// read file till the end
				String line;
				while ((line = reader.readLine()) != null) {

					// empty or comment line
					if (line.startsWith("#") || line.trim().isEmpty()) {
						continue;
					}

					// split line
					String[] split = line.split("=");
					connectionInfo.put(split[0].trim().replaceFirst("server", "exchangeServer"), split[1].trim());
				}
			}
		}

		// exception occurred
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Cannot retrieve exchange server connection information.", e);
		}
	}

	/**
	 * Retrieves analysis server connection information.
	 *
	 * @param connectionInfo
	 *            Mapping to store the information.
	 * @param filer
	 *            File server connection.
	 */
	private void getAnalysisServerConnectionInfo(HashMap<String, String> connectionInfo, FilerConnection filer) {

		try {

			// create path to connection settings file
			Path connSettingsFile = getWorkingDirectory().resolve(FilerConnection.ANALYSIS_SERVER_CONN_FILE);

			// get URL to connection settings file
			String url = filer.getDirectoryPath(FilerConnection.SETTINGS) + "/" + FilerConnection.ANALYSIS_SERVER_CONN_FILE;

			// download
			if (filer.fileExists(url)) {
				filer.getSftpChannel().get(url, connSettingsFile.toString());
			}

			// cannot locate connection settings file
			else
				throw new Exception("Cannot locate analysis server connection settings file on filer. Cannot connect to analysis server.");

			// create file reader
			try (BufferedReader reader = Files.newBufferedReader(connSettingsFile, Charset.defaultCharset())) {

				// read file till the end
				String line;
				while ((line = reader.readLine()) != null) {

					// empty or comment line
					if (line.startsWith("#") || line.trim().isEmpty()) {
						continue;
					}

					// split line
					String[] split = line.split("=");
					connectionInfo.put(split[0].trim().replaceFirst("server", "analysisServer"), split[1].trim());
				}
			}
		}

		// exception occurred
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Cannot retrieve analysis server connection information.", e);
		}
	}
}
