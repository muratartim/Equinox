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
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import equinox.Equinox;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.serverUtilities.ServerUtility;
import equinox.task.FastEquivalentStressAnalysis;
import equinox.utility.Utility;
import equinox.utility.exception.InternalEngineAnalysisFailedException;

/**
 * Class for inbuilt typical flight damage contribution analysis process.
 *
 * @author Murat Artim
 * @date 14 Oct 2016
 * @time 13:51:39
 */
public class InbuiltFlightDCA implements ESAProcess<Object[]> {

	/** The owner task. */
	private final FastEquivalentStressAnalysis task_;

	/** Path to input STH and FLS files. */
	private final Path sthFile_, flsFile_;

	/** Paths to output files. */
	private File[] outputFiles_ = null;

	/** Material. */
	private final FatigueMaterial material_;

	/** Spectrum validity. */
	private final int validity_, flsFileID_;

	/** Sub processes. */
	private Process writeSigmaProcess_, analysisProcess_;

	/** Decimal formats. */
	private final DecimalFormat format_ = new DecimalFormat("0.##");

	/** True to use extended inbuilt analysis engine. Extended inbuilt engine has no limit on number of peaks per typical flight. */
	private final boolean useExtended_, keepFailedOutputs_;

	/**
	 * Creates inbuilt typical flight damage contribution analysis process.
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
	 * @param validity
	 *            Spectrum validity.
	 * @param useExtended
	 *            True to use extended inbuilt analysis engine. Extended inbuilt engine has no limit on number of peaks per typical flight.
	 * @param keepFailedOutputs
	 *            True to keep output files in case analysis fails.
	 */
	public InbuiltFlightDCA(FastEquivalentStressAnalysis task, Path sthFile, Path flsFile, int flsFileID, FatigueMaterial material, int validity, boolean useExtended, boolean keepFailedOutputs) {
		task_ = task;
		sthFile_ = sthFile;
		flsFile_ = flsFile;
		flsFileID_ = flsFileID;
		material_ = material;
		validity_ = validity;
		useExtended_ = useExtended;
		keepFailedOutputs_ = keepFailedOutputs;
	}

	@Override
	public Object[] start(Connection connection, PreparedStatement... preparedStatements) throws InternalEngineAnalysisFailedException {

		try {

			// write input material file
			writeMaterialFile();

			// task cancelled
			if (task_.isCancelled())
				return null;

			// write input sigma file
			writeSigmaFile();

			// task cancelled
			if (task_.isCancelled())
				return null;

			// run analysis
			Path output = runAnalysis();

			// task cancelled
			if (task_.isCancelled())
				return null;

			// extract and return results
			return extractResults(output, connection);
		}

		// analysis failed
		catch (Exception e) {

			// set output files as permanent
			if (outputFiles_ != null && keepFailedOutputs_) {
				for (File file : outputFiles_) {
					task_.setFileAsPermanent(file.toPath());
				}
			}

			// throw exception
			throw new InternalEngineAnalysisFailedException(e, outputFiles_);
		}
	}

	@Override
	public void cancel() {

		// destroy sub processes (if still running)
		if (writeSigmaProcess_ != null && writeSigmaProcess_.isAlive()) {
			writeSigmaProcess_.destroyForcibly();
		}
		if (analysisProcess_ != null && analysisProcess_.isAlive()) {
			analysisProcess_.destroyForcibly();
		}
	}

	/**
	 * Extracts a mapping containing typical flight names versus their damage percentages in descending order.
	 *
	 * @param output
	 *            Output log file of the equivalent stress analysis.
	 * @param connection
	 *            Database connection.
	 * @return An array containing the typical flight damage mappings with and without flight occurrences, respectively.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Object[] extractResults(Path output, Connection connection) throws Exception {

		// update info
		task_.updateMessage("Parsing typical flight damages and saving to database...");

		// initialize variables
		Map<String, Double> withOccurrences = new HashMap<>();
		Map<String, Double> withoutOccurrences = new HashMap<>();
		double totalDamageWithOccurrences = 0.0;

		// prepare statement to get typical flight name
		String sql = "select name from fls_flights where file_id = " + flsFileID_ + " and flight_num = ?";
		try (PreparedStatement getFlightName = connection.prepareStatement(sql)) {

			// read damages from log file
			CharsetDecoder decoder = Charset.defaultCharset().newDecoder();
			decoder.onMalformedInput(CodingErrorAction.IGNORE);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(output), decoder))) {

				// read file till the end
				int flightNum = 0;
				String line;
				while ((line = reader.readLine()) != null) {

					// typical flight damage
					if (line.contains("ENDOMMAGEMENT =")) {

						// increment flight number
						flightNum++;

						// all flights read
						if (flightNum > validity_) {
							continue;
						}

						// get flight name
						getFlightName.setInt(1, flightNum);
						try (ResultSet resultSet = getFlightName.executeQuery()) {
							while (resultSet.next()) {

								// get flight name and damage
								String flightName = resultSet.getString("name");
								double damage = Double.parseDouble(line.split("=")[1].trim());

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

					// total damage
					else if (line.contains("ENDOMMAGEMENT SEQUENCE")) {
						totalDamageWithOccurrences = Double.parseDouble(line.split("=")[1].trim());
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
	 * Runs equivalent stress analysis process.
	 *
	 * @return Path to output log file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path runAnalysis() throws Exception {

		// progress info
		task_.updateMessage("Running analysis...");

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

		// set output file paths
		Path workingDir = task_.getWorkingDirectory();
		File log = workingDir.resolve("engine.log").toFile();
		Path dossier = workingDir.resolve("jobstpa_SIGMA_proto.dossier");
		Path erreurs = workingDir.resolve("jobstpa_SIGMA_proto.erreurs");
		outputFiles_ = new File[] { log, erreurs.toFile(), dossier.toFile() };

		// execute process and wait to end
		pb.directory(workingDir.toFile());
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
			throw new Exception("Analysis failed! See LOG file for details.");

		// task cancelled
		if (task_.isCancelled())
			return null;

		// output file doesn't exist
		if (Files.exists(erreurs) || !Files.exists(dossier))
			throw new Exception("Analysis failed! See LOG file for details.");

		// return output log file
		return log.toPath();
	}

	/**
	 * Writes out input sigma file.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeSigmaFile() throws Exception {

		// save input STH file
		task_.updateMessage("Saving input STH file...");
		Files.copy(sthFile_, task_.getWorkingDirectory().resolve("jobstpa_SIGMA_proto.sth"), StandardCopyOption.REPLACE_EXISTING);

		// save input FLS file
		task_.updateMessage("Saving input FLS file...");
		Files.copy(flsFile_, task_.getWorkingDirectory().resolve("jobstpa_SIGMA_proto.fls"), StandardCopyOption.REPLACE_EXISTING);

		// progress info
		task_.updateMessage("Creating sigma file...");

		// get analysis type
		String analysisType = "AMORCAGE";

		// get path to perl script and perl executable
		Path script = Equinox.SCRIPTS_DIR.resolve("writeSigmaFile.pl");

		// create process builder for windows
		ProcessBuilder pb = null;
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			Path perl = Equinox.SCRIPTS_DIR.resolve("perl").resolve("bin").resolve("perl.exe");
			pb = new ProcessBuilder(perl.toAbsolutePath().toString(), script.toAbsolutePath().toString(), "jobstpa_SIGMA_proto.sth", "jobstpa_SIGMA_proto.fls", Integer.toString(validity_), analysisType);
		}

		// create process builder for macOSX
		else if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), "jobstpa_SIGMA_proto.sth", "jobstpa_SIGMA_proto.fls", Integer.toString(validity_), analysisType);
		}
		else if (Equinox.OS_TYPE.equals(ServerUtility.LINUX)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), "jobstpa_SIGMA_proto.sth", "jobstpa_SIGMA_proto.fls", Integer.toString(validity_), analysisType);
		}

		// unsupported OS
		if (pb == null)
			throw new Exception("Unsupported operating system.");

		// set file paths
		Path workingDir = task_.getWorkingDirectory();
		File log = workingDir.resolve("writeSigmaFile.log").toFile();
		outputFiles_ = new File[] { log };

		// execute process and wait to end
		pb.directory(workingDir.toFile());
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		writeSigmaProcess_ = pb.start();
		assert pb.redirectInput() == Redirect.PIPE;
		assert pb.redirectOutput().file() == log;
		assert writeSigmaProcess_.getInputStream().read() == -1;

		// task cancelled
		if (task_.isCancelled())
			return;

		// process failed
		if (writeSigmaProcess_.waitFor() != 0)
			throw new Exception("Writing sigma file failed! See 'writeSigmaFile.log' file for details.");

		// task cancelled
		if (task_.isCancelled())
			return;

		// get output file
		Path output = task_.getWorkingDirectory().resolve("jobstpa_SIGMA_proto.sigma");

		// output file doesn't exist
		if (!Files.exists(output))
			throw new Exception("Writing sigma file failed! See 'writeSigmaFile.log' file for details.");
	}

	/**
	 * Writes out material input file.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeMaterialFile() throws Exception {

		// progress info
		task_.updateMessage("Creating material file...");

		// create path to material file
		Path materialFile = task_.getWorkingDirectory().resolve("material.mat");

		// get path to default material file
		Path defaultMaterialFile = Equinox.SCRIPTS_DIR.resolve("material.mat");

		// create output file writer
		try (BufferedWriter writer = Files.newBufferedWriter(materialFile, Charset.defaultCharset())) {

			// create file reader
			try (BufferedReader reader = Files.newBufferedReader(defaultMaterialFile, Charset.defaultCharset())) {

				// read file till the end
				String line;
				while ((line = reader.readLine()) != null) {

					// task cancelled
					if (task_.isCancelled())
						return;

					// fatigue material slope p
					if (line.contains("%MANP")) {
						writer.write("ABREMOD \"%MANP\" \"" + format_.format(-material_.getP()) + "\" ! PENTE DE LA LOI");
						writer.newLine();
					}

					// fatigue material coefficient q
					else if (line.contains("%MANQ")) {
						writer.write("ABREMOD \"%MANQ\" \"" + format_.format(material_.getQ()) + "\" ! COEFFICIENT f(R)");
						writer.newLine();
					}

					// fatigue material coefficient M
					else if (line.contains("%MANM")) {
						writer.write("ABREMOD \"%MANM\" \"" + format_.format(material_.getM()) + "\" ! M INFLUENCE DU MATERIAU");
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
	}
}
