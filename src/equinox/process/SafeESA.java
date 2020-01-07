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
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.Equinox;
import equinox.analysisServer.remote.message.AnalysisFailed;
import equinox.analysisServer.remote.message.AnalysisMessage;
import equinox.analysisServer.remote.message.FullESAComplete;
import equinox.analysisServer.remote.message.SafeESARequest;
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
import equinox.network.AnalysisServerManager;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.ServerUtility;
import equinox.task.AnalysisListenerTask;
import equinox.task.EquivalentStressAnalysis;
import equinox.utility.Utility;
import equinox.utility.exception.ServerAnalysisFailedException;

/**
 * Class for SAFE equivalent stress analysis process.
 *
 * @author Murat Artim
 * @date Jan 30, 2015
 * @time 5:11:08 PM
 */
public class SafeESA implements ESAProcess<Void>, AnalysisListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** The owner task of this process. */
	private final EquivalentStressAnalysis task_;

	/** Path to input STH file. */
	private final Path inputSTH_;

	/** Equivalent stress. */
	private final SpectrumItem eqStress_;

	/** FLS file ID. */
	private final int flsFileID_;

	/** Material. */
	private final Material material_;

	/** Sub processes. */
	private Process writeSigmaProcess_;

	/** Decimal formats. */
	private final DecimalFormat format_ = new DecimalFormat("0.##"), format2_ = new DecimalFormat("0.##E0");

	/** Server analysis completion indicator. */
	private final AtomicBoolean isAnalysisCompleted;

	/** Server analysis message. */
	private final AtomicReference<AnalysisMessage> serverMessageRef;

	/** True to keep analysis output files. */
	private final boolean keepOutputs_;

	/** Output file name. */
	private final String outputFileName_;

	/**
	 * Creates server equivalent stress analysis process.
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
	 * @param keepOutputs
	 *            True to keep analysis output files.
	 * @param outputFileName
	 *            Output file name. This will be used only if the analysis output file is kept.
	 */
	public SafeESA(EquivalentStressAnalysis task, Path inputSTH, SpectrumItem eqStress, int flsFileID, Material material, boolean keepOutputs, String outputFileName) {
		task_ = task;
		inputSTH_ = inputSTH;
		eqStress_ = eqStress;
		flsFileID_ = flsFileID;
		material_ = material;
		isAnalysisCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
		keepOutputs_ = keepOutputs;
		outputFileName_ = outputFileName;
	}

	@Override
	public Void start(Connection localConnection, PreparedStatement... preparedStatements) throws Exception {

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
			inputFiles.add(writeSigmaFile(localConnection));

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
			SafeESARequest request = new SafeESARequest();
			request.setListenerHashCode(hashCode());
			request.setDownloadUrl(downloadUrl);
			request.setFastAnalysis(false);
			request.setUploadOutputFiles(keepOutputs_);

			// fatigue analysis request
			if (material_ instanceof FatigueMaterial) {
				request.setAnalysisType(SafeESARequest.FATIGUE);
			}

			// preffas analysis request
			else if (material_ instanceof PreffasMaterial) {
				request.setAnalysisType(SafeESARequest.PREFFAS);
			}

			// linear analysis request
			else if (material_ instanceof LinearMaterial) {
				request.setAnalysisType(SafeESARequest.LINEAR);
			}

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
			else if (message instanceof FullESAComplete) {

				// cast message
				FullESAComplete completeMessage = (FullESAComplete) message;

				// extract results
				extractResults(completeMessage, localConnection);
			}

			// return
			return null;
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
	 * @param localConnection
	 *            Local database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void extractResults(FullESAComplete message, Connection localConnection) throws Exception {

		// set table name
		String tableName = null;
		if (eqStress_ instanceof FatigueEquivalentStress) {
			tableName = "fatigue_equivalent_stresses";
		}
		else if (eqStress_ instanceof PreffasEquivalentStress) {
			tableName = "preffas_equivalent_stresses";
		}
		else if (eqStress_ instanceof LinearEquivalentStress) {
			tableName = "linear_equivalent_stresses";
		}
		else if (eqStress_ instanceof ExternalFatigueEquivalentStress) {
			tableName = "ext_fatigue_equivalent_stresses";
		}
		else if (eqStress_ instanceof ExternalPreffasEquivalentStress) {
			tableName = "ext_preffas_equivalent_stresses";
		}
		else if (eqStress_ instanceof ExternalLinearEquivalentStress) {
			tableName = "ext_linear_equivalent_stresses";
		}

		// save output file to database (if requested)
		int outputFileID = -1;
		if (keepOutputs_ && message.getDownloadUrl() != null) {

			// create path to local output file
			task_.updateMessage("Downloading analysis output file from server...");
			Path zippedDossier = task_.getWorkingDirectory().resolve(outputFileName_ + ".zip");

			// download data from server
			try (FilerConnection filer = task_.getFilerConnection()) {
				filer.getSftpChannel().get(message.getDownloadUrl(), zippedDossier.toString());
			}

			// extract output file
			task_.updateMessage("Extracting analysis output file...");
			Path dossier = Utility.extractFileFromZIP(zippedDossier, task_, FileType.DOSSIER, null);

			// gzip dossier file
			task_.updateMessage("Saving analysis output file...");
			Path gzippedDossier = dossier.resolveSibling(dossier.getFileName().toString() + FileType.GZ.getExtension());
			Utility.gzipFile(dossier.toFile(), gzippedDossier.toFile());

			// prepare statement
			String sql = "insert into analysis_output_files(file_extension, file_name, data) values(?, ?, ?)";
			try (PreparedStatement update = localConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

		// create query
		String sql = "update " + tableName + " set stress = ?, validity = ?, max_stress = ?, min_stress = ?, " + "r_ratio = ?, total_cycles = ?, output_file_id = ? where id = " + eqStress_.getID();

		// create statement
		try (PreparedStatement update = localConnection.prepareStatement(sql)) {
			update.setDouble(1, message.getEquivalentStress());
			update.setDouble(2, message.getValidity());
			update.setDouble(3, message.getMaximumStress());
			update.setDouble(4, message.getMinimumStress());
			update.setDouble(5, message.getRRatio());
			update.setDouble(6, message.getTotalNumberOfCycles());
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
	 * Writes out input sigma file.
	 *
	 * @param connection
	 *            Database connection.
	 * @return SIGMA file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeSigmaFile(Connection connection) throws Exception {

		// save input STH file
		task_.updateMessage("Saving input STH file...");
		Files.copy(inputSTH_, task_.getWorkingDirectory().resolve("input.sth"), StandardCopyOption.REPLACE_EXISTING);

		// save input FLS file
		task_.updateMessage("Saving input FLS file...");
		saveFLSFile(task_.getWorkingDirectory().resolve("input.fls"), connection);

		// progress info
		task_.updateMessage("Creating sigma file...");

		// get analysis type
		String analysisType = null;
		if (material_ instanceof FatigueMaterial) {
			analysisType = "initiation";
		}
		else if (material_ instanceof PreffasMaterial || material_ instanceof LinearMaterial) {
			analysisType = "propagation";
		}

		// get path to perl script and perl executable
		Path script = Equinox.SCRIPTS_DIR.resolve("writeSigmaFileServer.pl");

		// create process builder for windows
		ProcessBuilder pb = null;
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			Path perl = Equinox.SCRIPTS_DIR.resolve("perl").resolve("bin").resolve("perl.exe");
			pb = new ProcessBuilder(perl.toAbsolutePath().toString(), script.toAbsolutePath().toString(), "input.sth", "input.fls", analysisType);
		}

		// create process builder for macOSX
		else if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), "input.sth", "input.fls", analysisType);
		}
		else if (Equinox.OS_TYPE.equals(ServerUtility.LINUX)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), "input.sth", "input.fls", analysisType);
		}

		// unsupported OS
		if (pb == null)
			throw new Exception("Unsupported operating system.");

		// set output file paths
		Path workingDir = task_.getWorkingDirectory();
		File log = workingDir.resolve("writeSigmaFileServer.log").toFile();

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

		// return material file
		return materialFile;
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

		// read default material file till the end
		String line = null;
		while ((line = reader.readLine()) != null) {

			// task cancelled
			if (task_.isCancelled())
				return;

			// material slope
			else if (line.startsWith("ABREMOD '%MANP'")) {
				writer.write("ABREMOD '%MANP' '" + format_.format(-material.getP()) + "'              ! SLOPE p");
				writer.newLine();
			}

			// material constant
			else if (line.startsWith("ABREMOD '%MANQ'")) {
				writer.write("ABREMOD '%MANQ' '" + format_.format(material.getQ()) + "'                ! f(R) PARAMETER q");
				writer.newLine();
			}

			// material coefficient m
			else if (line.startsWith("ABREMOD '%MANM'")) {
				writer.write("ABREMOD '%MANM' '" + format_.format(material.getM()) + "'                ! MATERIAL COEFFICIENT M");
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

		// read default material file till the end
		String line = null;
		while ((line = reader.readLine()) != null) {

			// yield stress
			if (line.startsWith("ABREMOD '%LIEL'")) {
				writer.write("ABREMOD '%LIEL' '" + format_.format(material.getFty()) + "'              ! YIELD STRESS (MPa)");
				writer.newLine();
			}

			// ultimate stress
			else if (line.startsWith("ABREMOD '%RM'")) {
				writer.write("ABREMOD '%RM'   '" + format_.format(material.getFtu()) + "'              ! ULTIMATE STRESS (MPa)");
				writer.newLine();
			}

			// elber constant A
			else if (line.startsWith("ABREMOD '%ELBA'")) {
				writer.write("ABREMOD '%ELBA' '" + format_.format(material.getA()) + "'                 ! A");
				writer.newLine();
			}

			// elber constant B
			else if (line.startsWith("ABREMOD '%ELBB'")) {
				writer.write("ABREMOD '%ELBB' '" + format_.format(material.getB()) + "'                 ! B");
				writer.newLine();
			}

			// elber constant Ceff
			else if (line.startsWith("ABREMOD '%ELBC'")) {
				writer.write("ABREMOD '%ELBC' '" + format2_.format(material.getCeff()) + "'                 ! Ceff  (MPa m^1/2)");
				writer.newLine();
			}

			// elber constant m
			else if (line.startsWith("ABREMOD '%ELBN'")) {
				writer.write("ABREMOD '%ELBN' '" + format_.format(material.getM()) + "'                 ! m");
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

		// read default material file till the end
		String line = null;
		while ((line = reader.readLine()) != null) {

			// yield stress
			if (line.startsWith("ABREMOD '%LIEL'")) {
				writer.write("ABREMOD '%LIEL' '" + format_.format(material.getFty()) + "'              ! YIELD STRESS (MPa)");
				writer.newLine();
			}

			// ultimate stress
			else if (line.startsWith("ABREMOD '%RM'")) {
				writer.write("ABREMOD '%RM'   '" + format_.format(material.getFtu()) + "'              ! ULTIMATE STRESS (MPa)");
				writer.newLine();
			}

			// elber constant A
			else if (line.startsWith("ABREMOD '%ELBA'")) {
				writer.write("ABREMOD '%ELBA' '" + format_.format(material.getA()) + "'                 ! A");
				writer.newLine();
			}

			// elber constant B
			else if (line.startsWith("ABREMOD '%ELBB'")) {
				writer.write("ABREMOD '%ELBB' '" + format_.format(material.getB()) + "'                 ! B");
				writer.newLine();
			}

			// elber constant Ceff
			else if (line.startsWith("ABREMOD '%ELBC'")) {
				writer.write("ABREMOD '%ELBC' '" + format2_.format(material.getCeff()) + "'                 ! Ceff  (MPa m^1/2)");
				writer.newLine();
			}

			// elber constant m
			else if (line.startsWith("ABREMOD '%ELBN'")) {
				writer.write("ABREMOD '%ELBN' '" + format_.format(material.getM()) + "'                 ! m");
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
