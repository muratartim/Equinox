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
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import equinox.Equinox;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.LinearMaterial;
import equinox.dataServer.remote.data.Material;
import equinox.dataServer.remote.data.PreffasMaterial;
import equinox.plugin.FileType;
import equinox.serverUtilities.ServerUtility;
import equinox.task.EquivalentStressAnalysis;
import equinox.utility.Utility;
import equinox.utility.exception.InternalEngineAnalysisFailedException;

/**
 * Class for inbuilt equivalent stress analysis process.
 *
 * @author Murat Artim
 * @date Feb 3, 2015
 * @time 11:06:00 AM
 */
public class InbuiltESA implements ESAProcess<Void> {

	/** The owner task of this process. */
	private final EquivalentStressAnalysis task_;

	/** Path to input STH file. */
	private final Path inputSTH_;

	/** Paths to output files. */
	private File[] outputFiles_ = null;

	/** Equivalent stress. */
	private final SpectrumItem eqStress_;

	/** FLS file ID. */
	private final int flsFileID_;

	/** Material. */
	private final Material material_;

	/** Sub processes. */
	private Process writeSigmaProcess_, analysisProcess_;

	/** Decimal formats. */
	private final DecimalFormat format_ = new DecimalFormat("0.##"), format2_ = new DecimalFormat("0.##E0");

	/** True to use extended inbuilt analysis engine. Extended inbuilt engine has no limit on number of peaks per typical flight. */
	private final boolean useExtended_, keepOutputs_;

	/** Output file name. */
	private final String outputFileName_;

	/**
	 * Creates inbuilt equivalent stress analysis process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputSTH
	 *            Path to input STH file.
	 * @param eqStress
	 *            Equivalent stress.
	 * @param flsFileID
	 *            FLS file ID.
	 * @param material
	 *            Material.
	 * @param useExtended
	 *            True to use extended inbuilt analysis engine. Extended inbuilt engine has no limit on number of peaks per typical flight.
	 * @param keepOutputs
	 *            True to keep analysis output files.
	 * @param outputFileName
	 *            Output file name. This will be used only if the analysis output file is kept.
	 */
	public InbuiltESA(EquivalentStressAnalysis task, Path inputSTH, SpectrumItem eqStress, int flsFileID, Material material, boolean useExtended, boolean keepOutputs, String outputFileName) {
		task_ = task;
		inputSTH_ = inputSTH;
		eqStress_ = eqStress;
		flsFileID_ = flsFileID;
		material_ = material;
		useExtended_ = useExtended;
		keepOutputs_ = keepOutputs;
		outputFileName_ = outputFileName;
	}

	@Override
	public Void start(Connection connection, PreparedStatement... preparedStatements) throws InternalEngineAnalysisFailedException {

		try {

			// write input material file
			writeMaterialFile();

			// task cancelled
			if (task_.isCancelled())
				return null;

			// write input sigma file
			writeSigmaFile(connection);

			// task cancelled
			if (task_.isCancelled())
				return null;

			// run analysis
			Path dossier = runAnalysis();

			// task cancelled
			if (task_.isCancelled())
				return null;

			// extract results
			extractResults(dossier, connection);

			// return
			return null;
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

	/**
	 * Writes out input sigma file.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeSigmaFile(Connection connection) throws Exception {

		// get validity of spectrum
		String validity = Integer.toString(getValidity(connection));

		// save input STH file
		task_.updateMessage("Saving input STH file...");
		Files.copy(inputSTH_, task_.getWorkingDirectory().resolve("jobstpa_SIGMA_proto.sth"), StandardCopyOption.REPLACE_EXISTING);

		// save input FLS file
		task_.updateMessage("Saving input FLS file...");
		saveFLSFile(task_.getWorkingDirectory().resolve("jobstpa_SIGMA_proto.fls"), connection);

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
			pb = new ProcessBuilder(perl.toAbsolutePath().toString(), script.toAbsolutePath().toString(), "jobstpa_SIGMA_proto.sth", "jobstpa_SIGMA_proto.fls", validity, analysisType);
		}

		// create process builder for macOSX
		else if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), "jobstpa_SIGMA_proto.sth", "jobstpa_SIGMA_proto.fls", validity, analysisType);
		}
		else if (Equinox.OS_TYPE.equals(ServerUtility.LINUX)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), "jobstpa_SIGMA_proto.sth", "jobstpa_SIGMA_proto.fls", validity, analysisType);
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

		// return
		return;
	}

	/**
	 * Runs equivalent stress analysis process.
	 *
	 * @return The dossier file.
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

		// return output files
		return dossier;
	}

	/**
	 * Extracts analysis results and saves them to database.
	 *
	 * @param dossier
	 *            Path to analysis output dossier file.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void extractResults(Path dossier, Connection connection) throws Exception {

		// update info
		task_.updateMessage("Parsing analysis results and saving to database...");

		// initialize variables
		int validity = -1, totalCycles = -1;
		double minStress = -1.0, maxStress = -1.0, rRatio = -1.0, fatigue = -1.0, cgPreffas = -1.0, cgLinEff = -1.0;

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
					else if (line.contains("NOMBRE DE CONTRAINTES TOTALES")) {
						totalCycles = Integer.parseInt(line.split(":")[2].trim());
					}
					else if (line.contains("SMIN :")) {
						minStress = Double.parseDouble(line.split(":")[2].trim());
					}
					else if (line.contains("SMAX :")) {
						maxStress = Double.parseDouble(line.split(":")[2].trim());
					}
					else if (line.contains("RAPPORT R")) {
						rRatio = Double.parseDouble(line.split(":")[2].trim());
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
			throw new Exception("Analysis failed! Cannot find fatigue equivalent stress in output dossier file.");

		// cannot find preffas equivalent stress
		if (material_ instanceof PreffasMaterial && cgPreffas == -1.0)
			throw new Exception("Analysis failed! Cannot find preffas equivalent stress in output dossier file.");

		// cannot find linear equivalent stress
		if (material_ instanceof LinearMaterial && cgLinEff == -1.0)
			throw new Exception("Analysis failed! Cannot find linear propagation equivalent stress in output dossier file.");

		// set stress and table name
		double stress = -1.0;
		String tableName = null;
		if (eqStress_ instanceof FatigueEquivalentStress) {
			stress = fatigue;
			tableName = "fatigue_equivalent_stresses";
		}
		else if (eqStress_ instanceof PreffasEquivalentStress) {
			stress = cgPreffas;
			tableName = "preffas_equivalent_stresses";
		}
		else if (eqStress_ instanceof LinearEquivalentStress) {
			LinearMaterial material = (LinearMaterial) material_;
			double a = material.getA();
			double b = 1.0 - a;
			double m = material.getM();
			double c = 0.9 * (a + b * 0.1);
			stress = Math.pow(cgLinEff / validity, 1.0 / m) / c;
			tableName = "linear_equivalent_stresses";
		}
		else if (eqStress_ instanceof ExternalFatigueEquivalentStress) {
			stress = fatigue;
			tableName = "ext_fatigue_equivalent_stresses";
		}
		else if (eqStress_ instanceof ExternalPreffasEquivalentStress) {
			stress = cgPreffas;
			tableName = "ext_preffas_equivalent_stresses";
		}
		else if (eqStress_ instanceof ExternalLinearEquivalentStress) {
			LinearMaterial material = (LinearMaterial) material_;
			double a = material.getA();
			double b = material.getB();
			double m = material.getM();
			double c = 0.9 * (a + b * 0.1);
			stress = Math.pow(cgLinEff / validity, 1.0 / m) / c;
			tableName = "ext_linear_equivalent_stresses";
		}

		// save output file to database (if requested)
		int outputFileID = -1;
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

		// update equivalent stress table
		String sql = "update " + tableName + " set stress = ?, validity = ?, max_stress = ?, " + "min_stress = ?, r_ratio = ?, total_cycles = ?, output_file_id = ? where id = " + eqStress_.getID();
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			update.setDouble(1, stress);
			update.setDouble(2, validity);
			update.setDouble(3, maxStress);
			update.setDouble(4, minStress);
			update.setDouble(5, rRatio);
			update.setDouble(6, totalCycles);
			if (outputFileID == -1) {
				update.setNull(7, java.sql.Types.INTEGER);
			}
			else {
				update.setInt(7, outputFileID);
			}
			update.executeUpdate();
		}
	}

	/**
	 * Retrieves and returns the validity of spectrum from FLS file.
	 *
	 * @param connection
	 *            Database connection.
	 * @return Validity.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int getValidity(Connection connection) throws Exception {

		// progress info
		task_.updateMessage("Getting spectrum validity from database...");

		// initialize validity
		int validity = 0;

		// create statement
		try (Statement statement = connection.createStatement()) {

			// set table name, column name and FLS file ID
			String tableName = null, colName = null;
			if (eqStress_ instanceof FatigueEquivalentStress || eqStress_ instanceof PreffasEquivalentStress || eqStress_ instanceof LinearEquivalentStress) {
				tableName = "fls_flights";
				colName = "file_id";
			}
			else if (eqStress_ instanceof ExternalFatigueEquivalentStress || eqStress_ instanceof ExternalPreffasEquivalentStress || eqStress_ instanceof ExternalLinearEquivalentStress) {
				tableName = "ext_fls_flights";
				colName = "sth_id";
			}

			// execute query
			statement.setMaxRows(1);
			try (ResultSet resultSet = statement.executeQuery("select flight_num from " + tableName + " where " + colName + " = " + flsFileID_ + " order by flight_num desc")) {
				while (resultSet.next()) {
					validity = resultSet.getInt("flight_num");
				}
			}

			// reset statement
			statement.setMaxRows(0);
		}

		// return validity
		return validity;
	}

	/**
	 * Saves FLS file into output path.
	 *
	 * @param output
	 *            Output FLS file.
	 * @param connection
	 *            Database connection.
	 * @return Output FLS file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path saveFLSFile(Path output, Connection connection) throws Exception {

		// update info
		task_.updateMessage("Saving input FLS file...");

		// equivalent stress
		if (eqStress_ instanceof FatigueEquivalentStress || eqStress_ instanceof PreffasEquivalentStress || eqStress_ instanceof LinearEquivalentStress) {
			saveFLSForEquivalentStress(output, connection);
		}
		else if (eqStress_ instanceof ExternalFatigueEquivalentStress || eqStress_ instanceof ExternalPreffasEquivalentStress || eqStress_ instanceof ExternalLinearEquivalentStress) {
			saveFLSForExternalEquivalentStress(output, connection);
		}

		// return output path
		return output;
	}

	/**
	 * Saves FLS file for external equivalent stress.
	 *
	 * @param output
	 *            Output path.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveFLSForExternalEquivalentStress(Path output, Connection connection) throws Exception {

		// create statement
		try (Statement statement = connection.createStatement()) {

			// create output file writer
			try (BufferedWriter writer = Files.newBufferedWriter(output, Charset.defaultCharset())) {

				// write header
				writer.write("# --------------------------------------------------------------");
				writer.newLine();
				writer.write("# date:   " + new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
				writer.newLine();
				writer.write("# time:     " + new SimpleDateFormat("hh:mm:ss").format(new Date()));
				writer.newLine();
				writer.write("# FLS file generated by Equinox Version " + Equinox.VERSION.toString());
				writer.newLine();
				writer.write("# --------------------------------------------------------------");
				writer.newLine();
				writer.write("# Flight number, identification: TF_UNIQUENUMBER_ID(flight+temperature+severity)");
				writer.newLine();

				// write sequence
				String sql = "select * from ext_fls_flights where sth_id = " + flsFileID_ + " order by flight_num asc";
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						String line = String.format("%6s", resultSet.getInt("flight_num"));
						String severity = resultSet.getString("severity");
						line += "  " + resultSet.getString("name") + " " + (severity.isEmpty() ? "AHAAHHHCHA" : severity);
						writer.write(line);
						writer.newLine();
					}
				}
			}
		}
	}

	/**
	 * Saves FLS file for equivalent stress.
	 *
	 * @param output
	 *            Output path.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveFLSForEquivalentStress(Path output, Connection connection) throws Exception {

		// create statement
		try (Statement statement = connection.createStatement()) {

			// execute query
			try (ResultSet resultSet = statement.executeQuery("select name, data from fls_files where file_id = " + flsFileID_)) {

				// get data
				if (resultSet.next()) {

					// get file name
					String name = resultSet.getString("name");

					// get blob
					Blob blob = resultSet.getBlob("data");

					// FLS file format
					Path zipFile = task_.getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
					Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
					Path flsFile = Utility.extractFileFromZIP(zipFile, task_, FileType.FLS, null);
					Files.copy(flsFile, output, StandardCopyOption.REPLACE_EXISTING);

					// free blob
					blob.free();
				}
			}
		}
	}
}
