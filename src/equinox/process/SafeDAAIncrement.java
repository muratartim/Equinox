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
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.Equinox;
import equinox.analysisServer.remote.message.AnalysisFailed;
import equinox.analysisServer.remote.message.AnalysisMessage;
import equinox.analysisServer.remote.message.DAAIncrementComplete;
import equinox.analysisServer.remote.message.SafeDAAIncrementRequest;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.network.AnalysisServerManager;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.ServerUtility;
import equinox.task.AnalysisListenerTask;
import equinox.task.DamageAngleAnalysis;
import equinox.utility.Utility;
import equinox.utility.exception.ServerAnalysisFailedException;

/**
 * Class for SAFE damage angle analysis increment process.
 *
 * @author Murat Artim
 * @date Feb 11, 2015
 * @time 11:31:40 AM
 */
public class SafeDAAIncrement implements Callable<Double[]>, AnalysisListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** The owner task of this process. */
	private final DamageAngleAnalysis task_;

	/** Path to input STH file. */
	private Path sthFile_, workingDir_;

	/** Path to input FLS file. */
	private final Path flsFile_;

	/** Increment angle. */
	private final int incAngle_, angleIndex_;

	/** Material. */
	private final FatigueMaterial material_;

	/** Decimal formats. */
	private final DecimalFormat format_ = new DecimalFormat("0.##");

	/** Sub processes. */
	private Process writeSigmaProcess_, omissionProcess_;

	/** True to apply omission. */
	private final boolean applyOmission_;

	/** Omission level. */
	private final double omissionLevel_;

	/** Server analysis completion indicator. */
	private final AtomicBoolean isAnalysisCompleted;

	/** Server analysis message. */
	private final AtomicReference<AnalysisMessage> serverMessageRef;

	/**
	 * Creates SAFE damage angle analysis increment process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param sthFile
	 *            Path to input STH file.
	 * @param flsFile
	 *            Path to input FLS file.
	 * @param incAngle
	 *            Increment angle.
	 * @param angleIndex
	 *            Angle index.
	 * @param material
	 *            Material.
	 * @param applyOmission
	 *            True if omission should be applied.
	 * @param omissionLevel
	 *            Omission level.
	 */
	public SafeDAAIncrement(DamageAngleAnalysis task, Path sthFile, Path flsFile, int incAngle, int angleIndex, FatigueMaterial material, boolean applyOmission, double omissionLevel) {
		task_ = task;
		sthFile_ = sthFile;
		flsFile_ = flsFile;
		incAngle_ = incAngle;
		angleIndex_ = angleIndex;
		material_ = material;
		applyOmission_ = applyOmission;
		omissionLevel_ = omissionLevel;
		isAnalysisCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public Double[] call() throws Exception {

		try {

			// create working directory
			workingDir_ = Utility.createWorkingDirectory(getClass().getSimpleName() + "_" + incAngle_);

			// create results array
			Double[] results = new Double[3];
			results[DamageAngleAnalysis.ANGLE_INDEX] = (double) angleIndex_;
			results[DamageAngleAnalysis.ANGLE] = (double) incAngle_;

			// process increment
			processIncrement(workingDir_, results);

			// delete temporary files
			Utility.deleteTemporaryFiles(workingDir_);

			// return results
			return results;
		}

		// exception occurred during execution
		catch (Exception e) {

			// add warning to task
			exceptionOccurred(e);

			// propagate exception
			throw e;
		}
	}

	@Override
	public void respondToAnalysisMessage(AnalysisMessage message) throws Exception {
		processServerAnalysisMessage(message, task_, serverMessageRef, isAnalysisCompleted);
	}

	/**
	 * Cancels this process.
	 */
	public void cancel() {

		// destroy sub processes (if still running)
		if (omissionProcess_ != null && omissionProcess_.isAlive()) {
			omissionProcess_.destroyForcibly();
		}
		if (writeSigmaProcess_ != null && writeSigmaProcess_.isAlive()) {
			writeSigmaProcess_.destroyForcibly();
		}

		// delete temporary files
		try {
			Utility.deleteTemporaryFiles(workingDir_);
		}

		// exception occurred during deleting temporary files
		catch (IOException e) {
			String message = "Exception occurred during deleting temporary files of increment angle " + incAngle_ + ".\n";
			message += e.getMessage() + "\n";
			for (StackTraceElement ste : e.getStackTrace()) {
				message += ste.toString() + "\n";
			}
			task_.addWarning(message);
		}
	}

	/**
	 * Called when exception occurs during analysis.
	 *
	 * @param e
	 *            Exception.
	 */
	private void exceptionOccurred(Exception e) {

		// set message header
		String message = "Analysis for increment angle " + incAngle_ + " has failed due to an exception.\n";

		// server exception
		if (e instanceof ServerAnalysisFailedException) {
			message += ((ServerAnalysisFailedException) e).getServerExceptionMessage();
		}

		// client exception
		else {
			message += e.getMessage() + "\n";
			for (StackTraceElement ste : e.getStackTrace()) {
				message += ste.toString() + "\n";
			}
		}

		// add exception message as warning
		task_.addWarning(message);
	}

	/**
	 * Processes the given increment angle.
	 *
	 * @param workingDir
	 *            Path to working directory.
	 * @param results
	 *            Array containing the stresses.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void processIncrement(Path workingDir, Double[] results) throws Exception {

		// declare network watcher
		AnalysisServerManager watcher = null;
		boolean removeListener = false;

		try {

			// apply omission
			if (applyOmission_ && omissionLevel_ > 0.0) {
				sthFile_ = applyOmission(workingDir);
			}

			// task cancelled
			if (task_.isCancelled())
				return;

			// add input files
			task_.updateMessage("Adding input files for increment angle " + incAngle_ + "...");
			ArrayList<Path> inputFiles = new ArrayList<>();

			// write material file
			inputFiles.add(writeMaterialFile(workingDir));

			// task cancelled
			if (task_.isCancelled())
				return;

			// write sigma file
			inputFiles.add(writeSigmaFile(workingDir));

			// task cancelled
			if (task_.isCancelled())
				return;

			// zip input files
			task_.updateMessage("Zipping input files for increment angle " + incAngle_ + "...");
			Path zipFile = workingDir.resolve("inputs_" + incAngle_ + ".zip");
			Utility.zipFiles(inputFiles, zipFile.toFile(), task_);

			// task cancelled
			if (task_.isCancelled())
				return;

			// upload zip archive to exchange server
			task_.updateMessage("Uploading input files for increment angle " + incAngle_ + "...");
			String downloadUrl = uploadFile(zipFile);

			// task cancelled
			if (task_.isCancelled())
				return;

			// initialize analysis request message
			SafeDAAIncrementRequest request = new SafeDAAIncrementRequest();
			request.setListenerHashCode(hashCode());
			request.setDownloadUrl(downloadUrl);
			request.setUploadOutputFiles(false);

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
				return;

			// get analysis message
			AnalysisMessage message = serverMessageRef.get();

			// analysis failed
			if (message instanceof AnalysisFailed) {
				results[DamageAngleAnalysis.STRESS] = null;
				throw new ServerAnalysisFailedException((AnalysisFailed) message);
			}

			// analysis succeeded
			else if (message instanceof DAAIncrementComplete) {
				DAAIncrementComplete completeMessage = (DAAIncrementComplete) message;
				results[DamageAngleAnalysis.STRESS] = completeMessage.getEquivalentStress();
			}
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}

	/**
	 * Applies omission to input STH file.
	 *
	 * @param workingDir
	 *            Working directory.
	 * @return Modified STH file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path applyOmission(Path workingDir) throws Exception {

		// update info
		task_.updateMessage("Applying omission for increment angle " + incAngle_ + "...");

		// copy input STH file to working directory
		Path sthFile = Files.copy(sthFile_, workingDir.resolve("input.sth"), StandardCopyOption.REPLACE_EXISTING);

		// get path to perl script and perl executable
		Path script = Equinox.SCRIPTS_DIR.resolve("omission.pl");

		// get STH file name
		Path sthFileName = sthFile.getFileName();
		if (sthFileName == null)
			throw new Exception("Cannot get input STH file name.");

		// create process builder for windows
		ProcessBuilder pb = null;
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			Path perl = Equinox.SCRIPTS_DIR.resolve("perl").resolve("bin").resolve("perl.exe");
			pb = new ProcessBuilder(perl.toAbsolutePath().toString(), script.toAbsolutePath().toString(), sthFileName.toString(), Double.toString(omissionLevel_));
		}

		// create process builder for macOSX
		else if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), sthFileName.toString(), Double.toString(omissionLevel_));
		}
		else if (Equinox.OS_TYPE.equals(ServerUtility.LINUX)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), sthFileName.toString(), Double.toString(omissionLevel_));
		}

		// unsupported OS
		if (pb == null)
			throw new Exception("Unsupported operating system.");

		// execute process and wait to end
		pb.directory(workingDir.toFile());
		File log = workingDir.resolve(sthFileName.toString() + "_omission.log").toFile();
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		omissionProcess_ = pb.start();
		assert pb.redirectInput() == Redirect.PIPE;
		assert pb.redirectOutput().file() == log;
		assert omissionProcess_.getInputStream().read() == -1;

		// process failed
		if (omissionProcess_.waitFor() != 0)
			throw new Exception("Stress sequence omission failed! See omission log file for details.");

		// get output file
		Path output = Paths.get(sthFile.toString() + FileType.RFORT.getExtension());

		// output file doesn't exist
		if (!Files.exists(output))
			throw new Exception("Stress sequence omission failed! See omission log file for details.");

		// return output path
		return output;
	}

	/**
	 * Writes out input sigma file.
	 *
	 * @param workingDir
	 *            Working directory.
	 * @return SIGMA file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeSigmaFile(Path workingDir) throws Exception {

		// copy input STH file
		task_.updateMessage("Copying input STH file...");
		Files.copy(sthFile_, workingDir.resolve("input.sth"), StandardCopyOption.REPLACE_EXISTING);

		// copy input FLS file
		task_.updateMessage("Copying input FLS file...");
		Files.copy(flsFile_, workingDir.resolve("input.fls"), StandardCopyOption.REPLACE_EXISTING);

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
		Path output = workingDir.resolve("input.sigma");

		// output file doesn't exist
		if (!Files.exists(output))
			throw new Exception("Writing sigma file failed! See 'writeSigmaFileServer.log' file for details.");

		// return
		return output;
	}

	/**
	 * Writes out material input file.
	 *
	 * @param workingDir
	 *            Working directory.
	 * @return Material file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeMaterialFile(Path workingDir) throws Exception {

		// progress info
		task_.updateMessage("Creating material file...");

		// create path to material file
		Path materialFile = workingDir.resolve("material.mat");

		// get path to default material file
		Path defaultMaterialFile = Equinox.SCRIPTS_DIR.resolve("materialServer.mat");

		// create output file writer
		try (BufferedWriter writer = Files.newBufferedWriter(materialFile, Charset.defaultCharset())) {

			// create file reader
			try (BufferedReader reader = Files.newBufferedReader(defaultMaterialFile, Charset.defaultCharset())) {

				// read default material file till the end
				String line = null;
				while ((line = reader.readLine()) != null)
					// task cancelled
					if (task_.isCancelled())
						return null;

					// material slope
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

		// return material file
		return materialFile;
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

		// get file name
		Path fileName = path.getFileName();
		if (fileName == null)
			throw new Exception("Cannot get file name.");

		// update info
		String downloadUrl = null;

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
}
