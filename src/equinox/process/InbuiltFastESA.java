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
import java.io.InputStream;
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
import java.sql.Statement;
import java.text.DecimalFormat;

import equinox.Equinox;
import equinox.data.FastESAOutput;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.LinearMaterial;
import equinox.dataServer.remote.data.Material;
import equinox.dataServer.remote.data.PreffasMaterial;
import equinox.plugin.FileType;
import equinox.serverUtilities.ServerUtility;
import equinox.task.FastEquivalentStressAnalysis;
import equinox.utility.Utility;
import equinox.utility.exception.InternalEngineAnalysisFailedException;

/**
 * Class for inbuilt fast equivalent stress analysis process.
 *
 * @author Murat Artim
 * @date Jun 15, 2016
 * @time 12:13:18 AM
 */
public class InbuiltFastESA implements ESAProcess<FastESAOutput> {

	/** The owner task. */
	private final FastEquivalentStressAnalysis task_;

	/** Path to input STH and FLS files. */
	private final Path sthFile_, flsFile_;

	/** Paths to output files. */
	private File[] outputFiles_ = null;

	/** Material. */
	private final Material material_;

	/** Spectrum validity. */
	private final int validity_;

	/** Sub processes. */
	private Process writeSigmaProcess_, analysisProcess_;

	/** Decimal formats. */
	private final DecimalFormat format_ = new DecimalFormat("0.##"), format2_ = new DecimalFormat("0.##E0");

	/** True to use extended inbuilt analysis engine. Extended inbuilt engine has no limit on number of peaks per typical flight. */
	private final boolean useExtended_, keepOutputs_;

	/** Output file name. */
	private final String outputFileName_;

	/**
	 * Creates inbuilt fast equivalent stress analysis process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param sthFile
	 *            Path to input STH file.
	 * @param flsFile
	 *            Path to input FLS file.
	 * @param material
	 *            Material.
	 * @param validity
	 *            Spectrum validity.
	 * @param useExtended
	 *            True to use extended inbuilt analysis engine. Extended inbuilt engine has no limit on number of peaks per typical flight.
	 * @param keepOutputs
	 *            True to keep analysis output files.
	 * @param outputFileName
	 *            Output file name. This is used only if the analysis output file is kept.
	 */
	public InbuiltFastESA(FastEquivalentStressAnalysis task, Path sthFile, Path flsFile, Material material, int validity, boolean useExtended, boolean keepOutputs, String outputFileName) {
		task_ = task;
		sthFile_ = sthFile;
		flsFile_ = flsFile;
		material_ = material;
		validity_ = validity;
		useExtended_ = useExtended;
		keepOutputs_ = keepOutputs;
		outputFileName_ = outputFileName;
	}

	@Override
	public FastESAOutput start(Connection connection, PreparedStatement... preparedStatements) throws InternalEngineAnalysisFailedException {

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
			Path dossier = runAnalysis();

			// task cancelled
			if (task_.isCancelled())
				return null;

			// extract results
			return extractResults(dossier, connection);
		}

		// analysis failed
		catch (Exception e) {

			// set output files as permanent
			if (outputFiles_ != null && keepOutputs_) {
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
	 * Extracts and returns analysis results.
	 *
	 * @param dossier
	 *            Path to analysis output file.
	 * @param connection
	 *            Database connection.
	 * @return Analysis output.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private FastESAOutput extractResults(Path dossier, Connection connection) throws Exception {

		// update info
		task_.updateMessage("Parsing analysis results and saving to database...");

		// initialize variables
		int validity = -1;
		double fatigue = -1.0, cgPreffas = -1.0, cgLinEff = -1.0;

		// create decoder
		CharsetDecoder decoder = Charset.defaultCharset().newDecoder();
		decoder.onMalformedInput(CodingErrorAction.IGNORE);

		// create file reader
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(dossier), decoder))) {

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
				if (sequenceResultsFound)
					// validity
					if (line.contains("NOMBRE DE VOLS :")) {
						validity = Integer.parseInt(line.split(":")[2].trim());
					}
					else if (line.contains("SMAX equivalent amor (MPa)")) {
						fatigue = Double.parseDouble(line.split(":")[2].trim());
					}
					else if (line.contains("SMAX equivalent propa (MPa)")) {
						cgPreffas = Double.parseDouble(line.split(":")[2].trim());
					}
					else if (line.contains("SEQ EFFICACITE LINE (MPa**n)")) {
						cgLinEff = Double.parseDouble(line.split(":")[2].trim());
					}
			}
		}

		// cannot find fatigue equivalent stress
		if (material_ instanceof FatigueMaterial && fatigue == -1.0)
			throw new Exception("Cannot find fatigue equivalent stress in output dossier file.");

		// cannot find preffas equivalent stress
		if (material_ instanceof PreffasMaterial && cgPreffas == -1.0)
			throw new Exception("Cannot find preffas equivalent stress in output dossier file.");

		// cannot find linear equivalent stress
		if (material_ instanceof LinearMaterial && cgLinEff == -1.0)
			throw new Exception("Cannot find linear propagation equivalent stress in output dossier file.");

		// set stress and table name
		Double stress = null;
		if (material_ instanceof FatigueMaterial) {
			stress = fatigue;
		}
		else if (material_ instanceof PreffasMaterial) {
			stress = cgPreffas;
		}
		else if (material_ instanceof LinearMaterial) {
			LinearMaterial material = (LinearMaterial) material_;
			double a = material.getA();
			double b = material.getB();
			double m = material.getM();
			double c = 0.9 * (a + b * 0.1);
			stress = Math.pow(cgLinEff / validity, 1.0 / m) / c;
		}

		// save output file to database (if requested)
		Integer outputFileID = null;
		if (keepOutputs_) {

			// gzip dossier file
			Path gzippedDossier = dossier.resolveSibling(dossier.getFileName().toString() + FileType.GZ.getExtension());
			Utility.gzipFile(dossier.toFile(), gzippedDossier.toFile());

			// prepare statement
			String sql = "insert into analysis_output_files(file_extension, file_name, data) values(?, ?, ?)";
			try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

				// save file
				try (InputStream inputStream = Files.newInputStream(gzippedDossier)) {
					update.setString(1, FileType.DOSSIER.getExtension());
					update.setString(2, outputFileName_);
					update.setBlob(3, inputStream, gzippedDossier.toFile().length());
					update.executeUpdate();
				}

				// get output file ID
				try (ResultSet resultSet = update.getGeneratedKeys()) {
					if (resultSet.next()) {
						outputFileID = resultSet.getBigDecimal(1).intValue();
					}
				}
			}
		}

		// return output
		return new FastESAOutput(stress, outputFileID);
	}

	/**
	 * Runs equivalent stress analysis process.
	 *
	 * @return Path to output file.
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

		// return output file
		return dossier;
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
		String analysisType = null;
		if (material_ instanceof FatigueMaterial) {
			analysisType = "AMORCAGE";
		}
		else if (material_ instanceof PreffasMaterial || material_ instanceof LinearMaterial) {
			analysisType = "PROPAGATION";
		}

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

		// execute process and wait to end
		Path workingDir = task_.getWorkingDirectory();
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

				// fatigue material
				if (material_ instanceof FatigueMaterial) {
					writeFatigueMaterialFile((FatigueMaterial) material_, reader, writer);
				}
				else if (material_ instanceof PreffasMaterial) {
					writePreffasMaterialFile((PreffasMaterial) material_, reader, writer);
				}
				else if (material_ instanceof LinearMaterial) {
					writeLinearMaterialFile((LinearMaterial) material_, reader, writer);
				}
			}
		}
	}

	/**
	 * Writes out fatigue material file.
	 *
	 * @param material
	 *            Fatigue material.
	 * @param reader
	 *            File reader.
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeFatigueMaterialFile(FatigueMaterial material, BufferedReader reader, BufferedWriter writer) throws Exception {

		// read file till the end
		String line;
		while ((line = reader.readLine()) != null) {

			// task cancelled
			if (task_.isCancelled())
				return;

			// fatigue material slope p
			if (line.contains("%MANP")) {
				writer.write("ABREMOD \"%MANP\" \"" + format_.format(-material.getP()) + "\" ! PENTE DE LA LOI");
				writer.newLine();
			}

			// fatigue material coefficient q
			else if (line.contains("%MANQ")) {
				writer.write("ABREMOD \"%MANQ\" \"" + format_.format(material.getQ()) + "\" ! COEFFICIENT f(R)");
				writer.newLine();
			}

			// fatigue material coefficient M
			else if (line.contains("%MANM")) {
				writer.write("ABREMOD \"%MANM\" \"" + format_.format(material.getM()) + "\" ! M INFLUENCE DU MATERIAU");
				writer.newLine();
			}

			// other
			else {
				writer.write(line);
				writer.newLine();
			}
		}
	}

	/**
	 * Writes out preffas material file.
	 *
	 * @param material
	 *            Preffas material.
	 * @param reader
	 *            File reader.
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writePreffasMaterialFile(PreffasMaterial material, BufferedReader reader, BufferedWriter writer) throws Exception {

		// read file till the end
		String line;
		while ((line = reader.readLine()) != null) {

			// task cancelled
			if (task_.isCancelled())
				return;

			// yield strength
			if (line.contains("%LIEL")) {
				writer.write("ABREMOD \"%LIEL\" \"" + format_.format(material.getFty()) + "\" ! LIMITE ELASTIQUE (MPa)");
				writer.newLine();
			}

			// ultimate strength
			else if (line.contains("%RM")) {
				writer.write("ABREMOD \"%RM\" \"" + format_.format(material.getFtu()) + "\" ! CONTRAINTE A RUPTURE (MPa)");
				writer.newLine();
			}

			// propagation material coefficient Ceff
			else if (line.contains("Ceff (MPa m1/2)")) {
				writer.write("ABREMOD \"%ELCE\" \"" + format2_.format(material.getCeff()) + "\" ! Ceff (MPa m1/2)");
				writer.newLine();
			}

			// propagation material coefficient A
			else if (line.contains("ABREMOD \"%ELA\"")) {
				writer.write("ABREMOD \"%ELA\" \"" + format_.format(material.getA()) + "\" ! A");
				writer.newLine();
			}

			// propagation material coefficient B
			else if (line.contains("ABREMOD \"%ELB\"")) {
				writer.write("ABREMOD \"%ELB\" \"" + format_.format(material.getB()) + "\" ! B");
				writer.newLine();
			}

			// propagation material coefficient m
			else if (line.contains("ABREMOD \"%ELN\"")) {
				writer.write("ABREMOD \"%ELN\" \"" + format_.format(material.getM()) + "\" ! n");
				writer.newLine();
			}

			// other
			else {
				writer.write(line);
				writer.newLine();
			}
		}
	}

	/**
	 * Writes out linear material file.
	 *
	 * @param material
	 *            Linear material.
	 * @param reader
	 *            File reader.
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeLinearMaterialFile(LinearMaterial material, BufferedReader reader, BufferedWriter writer) throws Exception {

		// read file till the end
		String line;
		while ((line = reader.readLine()) != null) {

			// task cancelled
			if (task_.isCancelled())
				return;

			// yield strength
			if (line.contains("%LIEL")) {
				writer.write("ABREMOD \"%LIEL\" \"" + format_.format(material.getFty()) + "\" ! LIMITE ELASTIQUE (MPa)");
				writer.newLine();
			}

			// ultimate strength
			else if (line.contains("%RM")) {
				writer.write("ABREMOD \"%RM\" \"" + format_.format(material.getFtu()) + "\" ! CONTRAINTE A RUPTURE (MPa)");
				writer.newLine();
			}

			// propagation material coefficient Ceff
			else if (line.contains("Ceff (MPa m1/2)")) {
				writer.write("ABREMOD \"%ELCE\" \"" + format2_.format(material.getCeff()) + "\" ! Ceff (MPa m1/2)");
				writer.newLine();
			}

			// propagation material coefficient A
			else if (line.contains("ABREMOD \"%ELA\"")) {
				writer.write("ABREMOD \"%ELA\" \"" + format_.format(material.getA()) + "\" ! A");
				writer.newLine();
			}

			// propagation material coefficient B
			else if (line.contains("ABREMOD \"%ELB\"")) {
				writer.write("ABREMOD \"%ELB\" \"" + format_.format(material.getB()) + "\" ! B");
				writer.newLine();
			}

			// propagation material coefficient m
			else if (line.contains("ABREMOD \"%ELN\"")) {
				writer.write("ABREMOD \"%ELN\" \"" + format_.format(material.getM()) + "\" ! n");
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
