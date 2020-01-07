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
package equinox.process;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.Equinox;
import equinox.analysisServer.remote.message.AnalysisFailed;
import equinox.analysisServer.remote.message.AnalysisMessage;
import equinox.analysisServer.remote.message.FlightDCAComplete;
import equinox.analysisServer.remote.message.SafeFlightDCARequest;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.network.AnalysisServerManager;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.ServerUtility;
import equinox.task.AnalysisListenerTask;
import equinox.task.FastEquivalentStressAnalysis;
import equinox.utility.Utility;
import equinox.utility.exception.ServerAnalysisFailedException;

/**
 * Class for SAFE flight damage contribution analysis process.
 *
 * @author Murat Artim
 * @date 3 May 2017
 * @time 16:02:04
 *
 */
public class SafeFlightDCA implements ESAProcess<Object[]>, AnalysisListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** The owner task. */
	private final FastEquivalentStressAnalysis task_;

	/** Path to input STH and FLS files. */
	private final Path sthFile_, flsFile_;

	/** Material. */
	private final FatigueMaterial material_;

	/** FLS file ID. */
	private final int flsFileID_;

	/** Sub processes. */
	private Process writeSigmaProcess_;

	/** Decimal formats. */
	private final DecimalFormat format_ = new DecimalFormat("0.##");

	/** Server analysis completion indicator. */
	private final AtomicBoolean isAnalysisCompleted;

	/** Server analysis message. */
	private final AtomicReference<AnalysisMessage> serverMessageRef;

	/** True to keep output files in case analysis fails. */
	private final boolean keepFailedOutputs_;

	/**
	 * Creates SAFE typical flight damage contribution analysis process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param sthFile
	 *            Path to input STH file.
	 * @param flsFile
	 *            Path to input FLS file.
	 * @param flsFileID
	 *            FLS file ID.
	 * @param material
	 *            Material.
	 * @param keepFailedOutputs
	 *            True to keep output files in case analysis fails.
	 */
	public SafeFlightDCA(FastEquivalentStressAnalysis task, Path sthFile, Path flsFile, int flsFileID, FatigueMaterial material, boolean keepFailedOutputs) {
		task_ = task;
		sthFile_ = sthFile;
		flsFile_ = flsFile;
		flsFileID_ = flsFileID;
		material_ = material;
		isAnalysisCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
		keepFailedOutputs_ = keepFailedOutputs;
	}

	@Override
	public Object[] start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// declare network watcher
		AnalysisServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create array to store input files
			ArrayList<Path> inputFiles = new ArrayList<>();

			// write material file
			inputFiles.add(writeMaterialFile());

			// task cancelled
			if (task_.isCancelled())
				return null;

			// write sigma file
			inputFiles.add(writeSigmaFile());

			// task cancelled
			if (task_.isCancelled())
				return null;

			// zip input files
			task_.updateMessage("Zipping input files...");
			Path zipFile = task_.getWorkingDirectory().resolve("inputs.zip");
			Utility.zipFiles(inputFiles, zipFile.toFile(), task_);

			// task cancelled
			if (task_.isCancelled())
				return null;

			// upload zip archive to exchange server
			String downloadUrl = uploadFile(zipFile);

			// task cancelled
			if (task_.isCancelled())
				return null;

			// initialize analysis request message
			SafeFlightDCARequest request = new SafeFlightDCARequest();
			request.setListenerHashCode(hashCode());
			request.setDownloadUrl(downloadUrl);
			request.setUploadOutputFiles(keepFailedOutputs_);

			// disable task canceling
			task_.getTaskPanel().updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = task_.getTaskPanel().getOwner().getOwner().getAnalysisServerManager();
			watcher.addMessageListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for analysis to complete
			waitForAnalysisServer(task_, isAnalysisCompleted);

			// remove from network watcher
			watcher.removeMessageListener(this);
			removeListener = false;

			// enable task canceling
			task_.getTaskPanel().updateCancelState(true);

			// task cancelled
			if (task_.isCancelled())
				return null;

			// get analysis message
			AnalysisMessage message = serverMessageRef.get();

			// analysis failed
			if (message instanceof AnalysisFailed)
				throw new ServerAnalysisFailedException((AnalysisFailed) message);

			// analysis succeeded
			else if (message instanceof FlightDCAComplete) {

				// cast message
				FlightDCAComplete completeMessage = (FlightDCAComplete) message;

				// extract results
				return extractResults(completeMessage, connection);
			}

			// invalid message
			else
				throw new Exception("Invalid server message received. Aborting analysis.");
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}

	@Override
	public void cancel() {

		// destroy sub processes (if still running)
		if (writeSigmaProcess_ != null && writeSigmaProcess_.isAlive()) {
			writeSigmaProcess_.destroyForcibly();
		}
	}

	@Override
	public void respondToAnalysisMessage(AnalysisMessage message) throws Exception {
		processServerAnalysisMessage(message, task_, serverMessageRef, isAnalysisCompleted);
	}

	/**
	 * Extracts analysis results and saves them to database.
	 *
	 * @param message
	 *            Server message.
	 * @param connection
	 *            Database connection.
	 * @return Analysis output.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Object[] extractResults(FlightDCAComplete message, Connection connection) throws Exception {

		// update info
		task_.updateMessage("Parsing typical flight damages and saving to database...");

		// get get flight damages
		Iterator<Entry<Integer, Double>> damages = message.getDamages().entrySet().iterator();

		// initialize variables
		Map<String, Double> withOccurrences = new HashMap<>();
		Map<String, Double> withoutOccurrences = new HashMap<>();
		double totalDamageWithOccurrences = message.getTotalDamage();

		// prepare statement to get typical flight name
		String sql = "select name from fls_flights where file_id = " + flsFileID_ + " and flight_num = ?";
		try (PreparedStatement getFlightName = connection.prepareStatement(sql)) {

			// iterate over the damages
			while (damages.hasNext()) {

				// get entry
				Entry<Integer, Double> entry = damages.next();
				double damage = entry.getValue();

				// get flight name
				getFlightName.setInt(1, entry.getKey());
				try (ResultSet resultSet = getFlightName.executeQuery()) {
					while (resultSet.next()) {

						// get flight name
						String flightName = resultSet.getString("name");

						// store damage with occurrences
						Double existingDamage = withOccurrences.get(flightName);
						if (existingDamage == null) {
							withOccurrences.put(flightName, damage);
						}
						else {
							withOccurrences.put(flightName, existingDamage + damage);
						}

						// without occurrences
						withoutOccurrences.put(flightName, damage);
					}
				}
			}
		}

		// replace damages with damage percentages with occurrences
		Iterator<Entry<String, Double>> iteratorWithOccurrences = withOccurrences.entrySet().iterator();
		while (iteratorWithOccurrences.hasNext()) {
			Entry<String, Double> entry = iteratorWithOccurrences.next();
			withOccurrences.put(entry.getKey(), entry.getValue() * 100 / totalDamageWithOccurrences);
		}

		// replace damages with damage percentages without occurrences
		double totalDamageWithoutOccurrences = 0.0;
		Iterator<Entry<String, Double>> iteratorWithoutOccurrences = withoutOccurrences.entrySet().iterator();
		while (iteratorWithoutOccurrences.hasNext()) {
			totalDamageWithoutOccurrences += iteratorWithoutOccurrences.next().getValue();
		}
		Iterator<Entry<String, Double>> iteratorWithoutOccurrences1 = withoutOccurrences.entrySet().iterator();
		while (iteratorWithoutOccurrences1.hasNext()) {
			Entry<String, Double> entry = iteratorWithoutOccurrences1.next();
			withoutOccurrences.put(entry.getKey(), entry.getValue() * 100 / totalDamageWithoutOccurrences);
		}

		// sort in descending order
		withOccurrences = Utility.sortByValue(withOccurrences, true);
		withoutOccurrences = Utility.sortByValue(withoutOccurrences, true);

		// return mappings array
		return new Object[] { withOccurrences, withoutOccurrences };
	}

	/**
	 * Uploads CDF set to exchange server.
	 *
	 * @param path
	 *            Path to CDF set.
	 * @return Download URL.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String uploadFile(Path path) throws Exception {

		// update info
		task_.updateMessage("Uploading input files to exchange server...");
		String downloadUrl = null;

		// get file name
		Path fileName = path.getFileName();
		if (fileName == null)
			throw new Exception("Cannot get file name.");

		// get filer connection
		try (FilerConnection filer = task_.getFilerConnection()) {

			// set path to destination file
			// INFO construct file name with this convention: userAlias_simpleClassName_currentTimeMillis.zip
			downloadUrl = filer.getDirectoryPath(FilerConnection.EXCHANGE) + "/" + Equinox.USER.getAlias() + "_" + getClass().getSimpleName() + "_" + System.currentTimeMillis() + ".zip";

			// upload file to filer
			filer.getSftpChannel().put(path.toString(), downloadUrl);
		}

		// return download URL
		return downloadUrl;
	}

	/**
	 * Writes out input sigma file.
	 *
	 * @return SIGMA file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeSigmaFile() throws Exception {

		// save input STH file
		task_.updateMessage("Saving input STH file...");
		Files.copy(sthFile_, task_.getWorkingDirectory().resolve("input.sth"), StandardCopyOption.REPLACE_EXISTING);

		// save input FLS file
		task_.updateMessage("Saving input FLS file...");
		Files.copy(flsFile_, task_.getWorkingDirectory().resolve("input.fls"), StandardCopyOption.REPLACE_EXISTING);

		// progress info
		task_.updateMessage("Creating sigma file...");

		// get path to perl script and perl executable
		Path script = Equinox.SCRIPTS_DIR.resolve("writeSigmaFileServer.pl");

		// create process builder for windows
		ProcessBuilder pb = null;
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			Path perl = Equinox.SCRIPTS_DIR.resolve("perl").resolve("bin").resolve("perl.exe");
			pb = new ProcessBuilder(perl.toAbsolutePath().toString(), script.toAbsolutePath().toString(), "input.sth", "input.fls", "initiation");
		}

		// create process builder for macOSX
		else if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), "input.sth", "input.fls", "initiation");
		}
		else if (Equinox.OS_TYPE.equals(ServerUtility.LINUX)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), "input.sth", "input.fls", "initiation");
		}

		// unsupported OS
		if (pb == null)
			throw new Exception("Unsupported operating system.");

		// execute process and wait to end
		Path workingDir = task_.getWorkingDirectory();
		pb.directory(workingDir.toFile());
		File log = workingDir.resolve("writeSigmaFileServer.log").toFile();
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		writeSigmaProcess_ = pb.start();
		assert pb.redirectInput() == Redirect.PIPE;
		assert pb.redirectOutput().file() == log;
		assert writeSigmaProcess_.getInputStream().read() == -1;

		// task cancelled
		if (task_.isCancelled())
			return null;

		// process failed
		if (writeSigmaProcess_.waitFor() != 0)
			throw new Exception("Writing sigma file failed! See 'writeSigmaFileServer.log' file for details.");

		// task cancelled
		if (task_.isCancelled())
			return null;

		// get output file
		Path output = task_.getWorkingDirectory().resolve("input.sigma");

		// output file doesn't exist
		if (!Files.exists(output))
			throw new Exception("Writing sigma file failed! See 'writeSigmaFileServer.log' file for details.");

		// return
		return output;
	}

	/**
	 * Writes out material input file.
	 *
	 * @return Material file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeMaterialFile() throws Exception {

		// progress info
		task_.updateMessage("Creating material file...");

		// create path to material file
		Path materialFile = task_.getWorkingDirectory().resolve("material.mat");

		// get path to default material file
		Path defaultMaterialFile = Equinox.SCRIPTS_DIR.resolve("materialServer.mat");

		// create output file writer
		try (BufferedWriter writer = Files.newBufferedWriter(materialFile, Charset.defaultCharset())) {

			// create file reader
			try (BufferedReader reader = Files.newBufferedReader(defaultMaterialFile, Charset.defaultCharset())) {

				// read default material file till the end
				String line = null;
				while ((line = reader.readLine()) != null) {

					// task cancelled
					if (task_.isCancelled()) {
						break;
					}
					else if (line.startsWith("ABREMOD '%MANP'")) {
						writer.write("ABREMOD '%MANP' '" + format_.format(-material_.getP()) + "'              ! SLOPE p");
						writer.newLine();
					}

					// material constant
					else if (line.startsWith("ABREMOD '%MANQ'")) {
						writer.write("ABREMOD '%MANQ' '" + format_.format(material_.getQ()) + "'                ! f(R) PARAMETER q");
						writer.newLine();
					}

					// material coefficient m
					else if (line.startsWith("ABREMOD '%MANM'")) {
						writer.write("ABREMOD '%MANM' '" + format_.format(material_.getM()) + "'                ! MATERIAL COEFFICIENT M");
						writer.newLine();
					}

					// other
					else {
						writer.write(line);
						writer.newLine();
					}
				}
			}
		}

		// return material file
		return materialFile;
	}
}
