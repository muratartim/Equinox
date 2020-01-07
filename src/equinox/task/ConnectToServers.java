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
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import equinox.Equinox;
import equinox.data.Settings;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for connect to servers task.
 *
 * @author Murat Artim
 * @date 24 Sep 2019
 * @time 22:58:37
 */
public class ConnectToServers extends TemporaryFileCreatingTask<Void> implements ShortRunningTask {

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Connect to servers";
	}

	@Override
	protected Void call() throws Exception {

		// download server connection info from AWS S3 bucket
		String downloadServerConnectionInfo = System.getenv("downloadServerConnectionInfo");
		if (downloadServerConnectionInfo == null || Boolean.parseBoolean(downloadServerConnectionInfo))
			downloadServerConnectionInfo();

		// update info
		updateMessage("Connecting to Equinox servers...");

		// connect to services
		taskPanel_.getOwner().getOwner().getAnalysisServerManager().connect(null, false);
		taskPanel_.getOwner().getOwner().getExchangeServerManager().connect(null, false);
		taskPanel_.getOwner().getOwner().getDataServerManager().connect(null, false);
		return null;
	}

	/**
	 * Downloads server connection information from AWS S3 bucket.
	 */
	private void downloadServerConnectionInfo() {

		// update info
		updateMessage("Downloading server connection info...");

		try {

			// get variables
			String awsAccessKey = ""; // FIXME AWS S3 access
			String awsSecretKey = ""; // FIXME AWS S3 access
			Regions awsRegion = Regions.EU_CENTRAL_1;
			String awsS3BucketName = "equinox-digital-twin";
			String connectionInfoFileName = "connection.set";

			// get settings
			Settings settings = taskPanel_.getOwner().getOwner().getSettings();

			// create path to temporary connection settings file
			Path tempFile = getWorkingDirectory().resolve(connectionInfoFileName);

			// create basic AWS credentials
			AWSCredentials credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);

			// configure the client
			AmazonS3 s3client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(awsRegion).build();

			// get object in S3 bucket
			try (S3Object s3object = s3client.getObject(awsS3BucketName, connectionInfoFileName)) {
				try (S3ObjectInputStream inputStream = s3object.getObjectContent()) {

					// copy file
					Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

					// create file reader
					try (BufferedReader reader = Files.newBufferedReader(tempFile, Charset.defaultCharset())) {

						// read file till the end
						String line;
						while ((line = reader.readLine()) != null) {

							// empty or comment line
							if (line.startsWith("#") || line.trim().isEmpty()) {
								continue;
							}

							// split line
							String[] split = line.split("=");
							String key = split[0].trim();
							String val = split[1].trim();

							// hostname
							if (key.equals("hostname")) {
								settings.setValue(Settings.DATA_SERVER_HOSTNAME, val);
								settings.setValue(Settings.EXCHANGE_SERVER_HOSTNAME, val);
								settings.setValue(Settings.ANALYSIS_SERVER_HOSTNAME, val);
								settings.setValue(Settings.FILER_HOSTNAME, val);
							}

							// data server port
							else if (key.equals("dataServerPort"))
								settings.setValue(Settings.DATA_SERVER_PORT, val);

							// exchange server port
							else if (key.equals("exchangeServerPort"))
								settings.setValue(Settings.EXCHANGE_SERVER_PORT, val);

							// analysis server port
							else if (key.equals("analysisServerPort"))
								settings.setValue(Settings.ANALYSIS_SERVER_PORT, val);

							// file server port
							else if (key.equals("fileServerPort"))
								settings.setValue(Settings.FILER_PORT, val);
						}
					}
				}
			}
		}

		// exception occurred
		catch (Throwable e) {
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during downloading server connection information from AWS S3 bucket.", e);
		}
	}
}