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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

import equinox.Equinox;
import equinox.data.DamageContributionResult;
import equinox.plugin.FileType;
import equinox.task.LoadcaseDamageContributionAnalysis;
import equinox.utility.Utility;
import equinoxServer.remote.utility.ServerUtility;

/**
 * Class for inbuilt damage contribution increment process.
 *
 * @author Murat Artim
 * @date Apr 4, 2015
 * @time 12:58:36 PM
 */
public class InbuiltDCAIncrement implements Callable<DamageContributionResult> {

	/** The owner task of this process. */
	private final LoadcaseDamageContributionAnalysis task_;

	/** Input STH file. */
	private Path sthFile_, workingDir_;

	/** Path to input files. */
	private final Path flsFile_, materialFile_;

	/** Increment angle. */
	private final int contributionIndex_;

	/** Spectrum validity. */
	private final String contributionName_, validity_;

	/** Sub processes. */
	private Process writeSigmaProcess_, analysisProcess_, omissionProcess_;

	/** True to use extended inbuilt analysis engine. Extended inbuilt engine has no limit on number of peaks per typical flight. */
	private final boolean useExtended_, applyOmission_;

	/** Omission level. */
	private final double omissionLevel_;

	/**
	 * Creates inbuilt damage analysis process for damage contribution analysis.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param sthFile
	 *            Path to input STH file.
	 * @param flsFile
	 *            Path to input FLS file.
	 * @param materialFile
	 *            Path to input material file.
	 * @param contributionName
	 *            Damage contribution name.
	 * @param contributionIndex
	 *            Contribution index.
	 * @param validity
	 *            Spectrum validity.
	 * @param useExtended
	 *            True to use extended inbuilt analysis engine. Extended inbuilt engine has no limit on number of peaks per typical flight.
	 * @param applyOmission
	 *            True if omission should be applied.
	 * @param omissionLevel
	 *            Omission level.
	 */
	public InbuiltDCAIncrement(LoadcaseDamageContributionAnalysis task, Path sthFile, Path flsFile, Path materialFile, String contributionName, int contributionIndex, String validity, boolean useExtended, boolean applyOmission, double omissionLevel) {
		task_ = task;
		sthFile_ = sthFile;
		flsFile_ = flsFile;
		materialFile_ = materialFile;
		contributionName_ = contributionName;
		contributionIndex_ = contributionIndex;
		validity_ = validity;
		useExtended_ = useExtended;
		applyOmission_ = applyOmission;
		omissionLevel_ = omissionLevel;
	}

	@Override
	public DamageContributionResult call() throws Exception {

		try {

			// create working directory
			workingDir_ = Utility.createWorkingDirectory(getClass().getSimpleName() + "_" + contributionName_);

			// create results array
			DamageContributionResult results = new DamageContributionResult();
			results.setContributionIndex(contributionIndex_);

			// process increment
			DamageContributionResult result = processIncrement(workingDir_, results);

			// delete temporary files
			Utility.deleteTemporaryFiles(workingDir_);

			// return result
			return result;
		}

		// exception occurred during execution
		catch (Exception e) {

			// add warning to task
			exceptionOccurred(e);

			// propagate exception
			throw e;
		}
	}

	/**
	 * Cancels this process.
	 */
	public void cancel() {

		// destroy sub processes (if still running)
		if ((omissionProcess_ != null) && omissionProcess_.isAlive()) {
			omissionProcess_.destroyForcibly();
		}
		if ((writeSigmaProcess_ != null) && writeSigmaProcess_.isAlive()) {
			writeSigmaProcess_.destroyForcibly();
		}
		if ((analysisProcess_ != null) && analysisProcess_.isAlive()) {
			analysisProcess_.destroyForcibly();
		}

		// delete temporary files
		try {
			Utility.deleteTemporaryFiles(workingDir_);
		}

		// exception occurred during deleting temporary files
		catch (IOException e) {
			String message = "Exception occurred during deleting temporary files of damage contribution " + contributionName_ + ".\n";
			message += e.getMessage() + "\n";
			for (StackTraceElement ste : e.getStackTrace()) {
				message += ste.toString() + "\n";
			}
			task_.addWarning(message);
		}
	}

	/**
	 * Processes the given increment angle.
	 *
	 * @param workingDir
	 *            Working directory.
	 * @param results
	 *            Array containing the stresses.
	 * @return Results.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DamageContributionResult processIncrement(Path workingDir, DamageContributionResult results) throws Exception {

		// apply omission
		if (applyOmission_ && (omissionLevel_ > 0.0)) {
			sthFile_ = applyOmission(workingDir);
		}

		// task cancelled
		if (task_.isCancelled())
			return null;

		// save material file
		task_.updateMessage("Saving input material file for damage contribution " + contributionName_ + "...");
		Files.copy(materialFile_, workingDir.resolve("material.mat"), StandardCopyOption.REPLACE_EXISTING);

		// write input sigma file
		Path sigmaFile = writeSigmaFile(workingDir);

		// task cancelled
		if (task_.isCancelled())
			return null;

		// run analysis
		Path[] outputs = runAnalysis(materialFile_, sigmaFile, workingDir);

		// task cancelled
		if (task_.isCancelled())
			return null;

		// extract results
		return extractResults(outputs, results);
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
		task_.updateMessage("Applying omission for damage contribution " + contributionName_ + "...");

		// copy input STH file to working directory
		Path sthFile = Files.copy(sthFile_, workingDir.resolve("input.sth"), StandardCopyOption.REPLACE_EXISTING);

		// get path to perl script and perl executable
		Path script = Equinox.SCRIPTS_DIR.resolve("omission.pl");

		// get STH file name path
		Path sthFileName = sthFile.getFileName();
		if (sthFileName == null)
			throw new Exception("Cannot get stress sequence file name.");

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
	 * Extracts analysis results.
	 *
	 * @param outputs
	 *            Paths to analysis output files (.dossier and .log).
	 * @param results
	 *            Array containing the stresses.
	 * @return Results.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DamageContributionResult extractResults(Path[] outputs, DamageContributionResult results) throws Exception {

		// update info
		task_.updateMessage("Parsing analysis results and saving to database for damage contribution " + contributionName_ + "...");

		// extract damage
		results.setDamage(extractDamage(outputs[1]));

		// extract equivalent stress
		results.setStress(extractEquivalentStress(outputs[0]));

		// return results
		return results;
	}

	/**
	 * Extracts fatigue equivalent stress from dossier file.
	 *
	 * @param dossierFile
	 *            Dossier file.
	 * @return Fatigue equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Double extractEquivalentStress(Path dossierFile) throws Exception {

		// initialize stress
		Double stress = null;

		// create decoder
		CharsetDecoder decoder = Charset.defaultCharset().newDecoder();
		decoder.onMalformedInput(CodingErrorAction.IGNORE);

		// create file reader
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(dossierFile), decoder))) {

			// read file till the end
			boolean sequenceResultsFound = false;
			String line;
			while ((line = reader.readLine()) != null) {

				// sequence results found
				if (!sequenceResultsFound && line.contains("ANALYSE DE LA SEQUENCE")) {
					sequenceResultsFound = true;
					continue;
				}

				// sequence results
				if (sequenceResultsFound) {

					// fatigue equivalent stress
					if (line.contains("SMAX equivalent amor (MPa)")) {
						stress = Double.parseDouble(line.split(":")[2].trim());
						break;
					}
				}
			}
		}

		// no sequence damage found
		if (stress == null)
			throw new Exception("Extracting analysis results for for damage contribution " + contributionName_ + " has failed! See 'engine.log' file for details.");

		// return stress
		return stress;
	}

	/**
	 * Extracts fatigue damage from the log file.
	 *
	 * @param logFile
	 *            Path to log file.
	 * @return Fatigue damage.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Double extractDamage(Path logFile) throws Exception {

		// initialize damage
		Double damage = null;

		// create decoder
		CharsetDecoder decoder = Charset.defaultCharset().newDecoder();
		decoder.onMalformedInput(CodingErrorAction.IGNORE);

		// create file reader
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(logFile), decoder))) {

			// read file till the end
			String line;
			while ((line = reader.readLine()) != null) {

				// damage
				if (line.contains("ENDOMMAGEMENT SEQUENCE")) {
					damage = Double.parseDouble(line.split("=")[1].trim());
					break;
				}
			}
		}

		// no sequence damage found
		if (damage == null)
			throw new Exception("Extracting analysis results for for damage contribution " + contributionName_ + " has failed! See 'engine.log' file for details.");

		// return damage
		return damage;
	}

	/**
	 * Runs equivalent stress analysis process.
	 *
	 * @param materialFile
	 *            Input material file.
	 * @param sigmaFile
	 *            Input sigma file.
	 * @param workingDir
	 *            Working directory.
	 * @return Paths to dossier and log files, respectively.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path[] runAnalysis(Path materialFile, Path sigmaFile, Path workingDir) throws Exception {

		// progress info
		task_.updateMessage("Running analysis for damage contribution " + contributionName_ + "...");

		// create process builder for windows
		ProcessBuilder pb = null;
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			Path engine = Equinox.SCRIPTS_DIR.resolve(useExtended_ ? "spectre_proto_CG_extended.exe" : "spectre_proto_CG.exe");
			pb = new ProcessBuilder(engine.toAbsolutePath().toString(), "jobstpa_SIGMA_proto");
		}

		// create process builder for macOSX
		else if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
			Path engine = Equinox.SCRIPTS_DIR.resolve(useExtended_ ? "spectre_proto_CG_extended_mac" : "spectre_proto_CG_mac");
			engine.toFile().setExecutable(true);
			pb = new ProcessBuilder(engine.toAbsolutePath().toString(), "jobstpa_SIGMA_proto");
			Path fortranLibs = Equinox.SCRIPTS_DIR.resolve("fortran");
			pb.environment().put("DYLD_LIBRARY_PATH", fortranLibs.toAbsolutePath().toString());
		}

		// create process builder for linux
		else if (Equinox.OS_TYPE.equals(ServerUtility.LINUX)) {
			Path engine = Equinox.SCRIPTS_DIR.resolve(useExtended_ ? "spectre_proto_CG_extended_linux" : "spectre_proto_CG_linux");
			engine.toFile().setExecutable(true);
			pb = new ProcessBuilder(engine.toAbsolutePath().toString(), "jobstpa_SIGMA_proto");
		}

		// unsupported OS
		if (pb == null)
			throw new Exception("Unsupported operating system.");

		// execute process and wait to end
		pb.directory(workingDir.toFile());
		File log = workingDir.resolve("engine.log").toFile();
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		analysisProcess_ = pb.start();
		assert pb.redirectInput() == Redirect.PIPE;
		assert pb.redirectOutput().file() == log;
		assert analysisProcess_.getInputStream().read() == -1;

		// task cancelled
		if (task_.isCancelled())
			return null;

		// process failed
		if (analysisProcess_.waitFor() != 0)
			throw new Exception("Analysis for damage contribution " + contributionName_ + " has failed! See 'engine.log' file for details.");

		// task cancelled
		if (task_.isCancelled())
			return null;

		// get output file
		Path dossier = workingDir.resolve("jobstpa_SIGMA_proto.dossier");
		Path erreurs = workingDir.resolve("jobstpa_SIGMA_proto.erreurs");

		// output file doesn't exist
		if (Files.exists(erreurs) || !Files.exists(dossier))
			throw new Exception("Analysis for damage contribution " + contributionName_ + " has failed! See 'engine.log' file for details.");

		// return log file
		return new Path[] { dossier, log.toPath() };
	}

	/**
	 * Writes out input sigma file.
	 *
	 * @param workingDir
	 *            Working directory.
	 * @return Path to input sigma file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeSigmaFile(Path workingDir) throws Exception {

		// save input STH file
		task_.updateMessage("Saving input STH file for damage contribution " + contributionName_ + "...");
		Files.copy(sthFile_, workingDir.resolve("jobstpa_SIGMA_proto.sth"), StandardCopyOption.REPLACE_EXISTING);

		// save input FLS file
		task_.updateMessage("Saving input FLS file for damage contribution " + contributionName_ + "...");
		Files.copy(flsFile_, workingDir.resolve("jobstpa_SIGMA_proto.fls"), StandardCopyOption.REPLACE_EXISTING);

		// progress info
		task_.updateMessage("Creating sigma file for damage contribution " + contributionName_ + "...");

		// get path to perl script and perl executable
		Path script = Equinox.SCRIPTS_DIR.resolve("writeSigmaFile.pl");

		// create process builder for windows
		ProcessBuilder pb = null;
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			Path perl = Equinox.SCRIPTS_DIR.resolve("perl").resolve("bin").resolve("perl.exe");
			pb = new ProcessBuilder(perl.toAbsolutePath().toString(), script.toAbsolutePath().toString(), "jobstpa_SIGMA_proto.sth", "jobstpa_SIGMA_proto.fls", validity_, "AMORCAGE");
		}

		// create process builder for macOSX
		else if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), "jobstpa_SIGMA_proto.sth", "jobstpa_SIGMA_proto.fls", validity_, "AMORCAGE");
		}
		else if (Equinox.OS_TYPE.equals(ServerUtility.LINUX)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), "jobstpa_SIGMA_proto.sth", "jobstpa_SIGMA_proto.fls", validity_, "AMORCAGE");
		}

		// unsupported OS
		if (pb == null)
			throw new Exception("Unsupported operating system.");

		// execute process and wait to end
		pb.directory(workingDir.toFile());
		File log = workingDir.resolve("writeSigmaFile.log").toFile();
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
			throw new Exception("Writing sigma file for damage contribution " + contributionName_ + " has failed! See 'writeSigmaFile.log' file for details.");

		// task cancelled
		if (task_.isCancelled())
			return null;

		// get output file
		Path output = workingDir.resolve("jobstpa_SIGMA_proto.sigma");

		// output file doesn't exist
		if (!Files.exists(output))
			throw new Exception("Writing sigma file for damage contribution " + contributionName_ + " has failed! See 'writeSigmaFile.log' file for details.");

		// return output file
		return output;
	}

	/**
	 * Called when exception occurs during analysis.
	 *
	 * @param e
	 *            Exception.
	 */
	private void exceptionOccurred(Exception e) {
		String message = "Analysis for damage contribution " + contributionName_ + " has failed due to an exception.\n";
		message += e.getMessage() + "\n";
		for (StackTraceElement ste : e.getStackTrace()) {
			message += ste.toString() + "\n";
		}
		task_.addWarning(message);
	}
}
