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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.Equinox;
import equinox.analysisServer.remote.data.IsamiMaterial;
import equinox.analysisServer.remote.message.AnalysisFailed;
import equinox.analysisServer.remote.message.AnalysisMessage;
import equinox.analysisServer.remote.message.FastESAComplete;
import equinox.analysisServer.remote.message.IsamiESARequest;
import equinox.data.FastESAOutput;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.LinearMaterial;
import equinox.dataServer.remote.data.Material;
import equinox.dataServer.remote.data.PreffasMaterial;
import equinox.network.AnalysisServerManager;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.task.AnalysisListenerTask;
import equinox.task.FastEquivalentStressAnalysis;
import equinox.utility.Utility;
import equinox.utility.exception.ServerAnalysisFailedException;

/**
 * Class for ISAMI fast equivalent stress analysis process.
 *
 * @author Murat Artim
 * @date 9 May 2017
 * @time 14:35:40
 *
 */
public class IsamiFastESA implements ESAProcess<FastESAOutput>, AnalysisListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** The owner task. */
	private final FastEquivalentStressAnalysis task_;

	/** Path to input stress sequence file. */
	private final Path sequenceFile_;

	/** Material. */
	private final Material material_;

	/** Server analysis completion indicator. */
	private final AtomicBoolean isAnalysisCompleted;

	/** Server analysis message. */
	private final AtomicReference<AnalysisMessage> serverMessageRef;

	/** True to keep analysis output files. */
	private final boolean keepOutputs_;

	/** Output file name. */
	private final String outputFileName_;

	/** ANA and FLS file IDs. */
	private final int anaFileID_, flsFileID_, validity_;

	/** Parameters. */
	private int readLines_, allLines_, flightNumber_, numPeaks_, rowIndex_ = 0, colIndex_ = 0;

	/** Update message header. */
	private String sthLine_, sigmaLine_;

	/** Number of columns. */
	private static final int NUM_COLS = 10;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.000000E00");

	/** ISAMI version. */
	private final IsamiVersion isamiVersion_;

	/** ISAMI sub version. */
	private final IsamiSubVersion isamiSubVersion_;

	/** True compression should be applied in propagation analysis. */
	private final boolean applyCompression_;

	/**
	 * Creates ISAMI fast equivalent stress analysis process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param sequenceFile
	 *            Path to input stress sequence file. This can be either SIGMA or STH file.
	 * @param material
	 *            Material.
	 * @param keepOutputs
	 *            True to keep analysis output files.
	 * @param outputFileName
	 *            Output file name. This will be used only if the analysis output file is kept.
	 * @param anaFileID
	 *            ANA file ID.
	 * @param flsFileID
	 *            FLS file ID.
	 * @param validity
	 *            Spectrum validity.
	 * @param isamiVersion
	 *            ISAMI version.
	 * @param isamiSubVersion
	 *            ISAMI sub version.
	 * @param applyCompression
	 *            True compression should be applied in propagation analysis.
	 */
	public IsamiFastESA(FastEquivalentStressAnalysis task, Path sequenceFile, Material material, boolean keepOutputs, String outputFileName, int anaFileID, int flsFileID, int validity, IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		task_ = task;
		sequenceFile_ = sequenceFile;
		material_ = material;
		isAnalysisCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
		keepOutputs_ = keepOutputs;
		outputFileName_ = outputFileName;
		anaFileID_ = anaFileID;
		flsFileID_ = flsFileID;
		validity_ = validity;
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
	}

	@Override
	public FastESAOutput start(Connection localConnection, PreparedStatement... preparedStatements) throws Exception {

		// declare network watcher
		AnalysisServerManager watcher = null;
		boolean removeListener = false;

		try {

			// get stress sequence file type
			FileType fileType = FileType.getFileType(sequenceFile_.toFile());

			// write SIGMA file (if necessary)
			Path sigmaFile = fileType.equals(FileType.STH) ? writeSigmaFile(localConnection) : sequenceFile_;

			// task cancelled
			if (task_.isCancelled())
				return null;

			// zip input files
			task_.updateMessage("Zipping input files...");
			Path zipFile = task_.getWorkingDirectory().resolve("inputs.zip");
			Utility.zipFile(sigmaFile, zipFile.toFile(), task_);

			// task cancelled
			if (task_.isCancelled())
				return null;

			// upload input file
			String downloadUrl = uploadFile(zipFile);

			// task cancelled
			if (task_.isCancelled())
				return null;

			// initialize analysis request message
			IsamiESARequest request = new IsamiESARequest();
			request.setListenerHashCode(hashCode());
			request.setDownloadUrl(downloadUrl);
			request.setFastAnalysis(true);
			request.setUploadOutputFiles(keepOutputs_);

			// set analysis type
			int analysisType = -1;
			if (material_ instanceof FatigueMaterial) {
				analysisType = IsamiESARequest.FATIGUE;
			}
			else if (material_ instanceof PreffasMaterial) {
				analysisType = IsamiESARequest.PREFFAS;
			}
			else if (material_ instanceof LinearMaterial) {
				analysisType = IsamiESARequest.LINEAR;
			}
			request.setAnalysisType(analysisType);

			// set ISAMI version
			request.setIsamiVersion(isamiVersion_.getIsamiVersion());

			// set ISAMI sub-version
			request.setIsamiSubVersion(isamiSubVersion_.getInputName());

			// set compression
			request.setApplyCompression(applyCompression_);

			// create and set ISAMI material
			IsamiMaterial material = new IsamiMaterial();
			material.setName(material_.getName());
			material.setSpecification(material_.getSpecification());
			material.setOrientation(material_.getOrientation());
			material.setConfiguration(material_.getConfiguration());
			request.setMaterial(material);

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
			else if (message instanceof FastESAComplete) {

				// cast message
				FastESAComplete completeMessage = (FastESAComplete) message;

				// extract results
				return extractResults(completeMessage, localConnection);
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
		// no implementation
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
	 * @return Analysis output.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private FastESAOutput extractResults(FastESAComplete message, Connection localConnection) throws Exception {

		// save output file to database (if requested)
		Integer outputFileID = null;
		if (keepOutputs_ && message.getDownloadUrl() != null) {

			// create path to local output file
			task_.updateMessage("Downloading analysis output file from server...");
			Path zippedHtml = task_.getWorkingDirectory().resolve(outputFileName_ + ".zip");

			// download data from server
			try (FilerConnection filer = task_.getFilerConnection()) {
				filer.getSftpChannel().get(message.getDownloadUrl(), zippedHtml.toString());
			}

			// extract output file
			task_.updateMessage("Extracting analysis output file...");
			Path html = Utility.extractFileFromZIP(zippedHtml, task_, FileType.HTML, null);

			// gzip html file
			task_.updateMessage("Saving analysis output file...");
			Path gzippedHtml = html.resolveSibling(html.getFileName().toString() + FileType.GZ.getExtension());
			Utility.gzipFile(html.toFile(), gzippedHtml.toFile());

			// prepare statement
			String sql = "insert into analysis_output_files(file_extension, file_name, data) values(?, ?, ?)";
			try (PreparedStatement update = localConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

				// save file
				try (InputStream inputStream = Files.newInputStream(gzippedHtml)) {
					update.setString(1, FileType.HTML.getExtension());
					update.setString(2, outputFileName_);
					update.setBlob(3, inputStream, gzippedHtml.toFile().length());
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
		return new FastESAOutput(message.getEquivalentStress(), outputFileID);
	}

	/**
	 * Uploads CDF set to exchange server.
	 *
	 * @param path
	 *            Path to CDF set.
	 * @return Exchange ID.
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

		// get filer connection1
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
	 * @param connection
	 *            Database connection.
	 * @return SIGMA file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeSigmaFile(Connection connection) throws Exception {

		// update info
		task_.updateMessage("Generating sigma file...");

		// get output file
		Path sigmaFile = task_.getWorkingDirectory().resolve("input.sigma");

		// create file writer
		try (BufferedWriter writer = Files.newBufferedWriter(sigmaFile, Charset.defaultCharset())) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// write SIGMA file header
				writeHeader(statement, writer, anaFileID_);

				// task cancelled
				if (task_.isCancelled())
					return null;

				// write flights sequence
				writeFlightSequence(connection, statement, writer, anaFileID_, flsFileID_);

				// task cancelled
				if (task_.isCancelled())
					return null;
			}

			// get line count of the sequence file
			allLines_ = Utility.countLines(sequenceFile_, task_);

			// create file reader
			try (BufferedReader reader = Files.newBufferedReader(sequenceFile_, Charset.defaultCharset())) {

				// read file till the end
				readLines_ = 0;
				while ((sthLine_ = reader.readLine()) != null) {

					// task cancelled
					if (task_.isCancelled())
						return null;

					// increment read lines
					readLines_++;

					// update progress
					task_.updateProgress(readLines_, allLines_);

					// comment line
					if (readLines_ < 5) {
						continue;
					}

					// write flight header
					writeFlightHeader(reader, writer);

					// write flight peaks
					writeFlightPeaks(reader, writer);
				}
			}

			// pass 1 line
			writer.write("\n");
		}

		// return path to output file
		return sigmaFile;
	}

	/**
	 * Writes out SIGMA file flight peaks.
	 *
	 * @param reader
	 *            STH file reader.
	 * @param writer
	 *            SIGMA file writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeFlightPeaks(BufferedReader reader, BufferedWriter writer) throws Exception {

		// initialize variables
		int rem = numPeaks_ % NUM_COLS;
		int numRows = numPeaks_ / NUM_COLS + (rem == 0 ? 0 : 1);
		rowIndex_ = 0;
		colIndex_ = 0;
		sigmaLine_ = "";

		// initialize number of read peaks
		int readPeaks = 0;
		double peakVal = 0.0;

		// read till the end
		while ((sthLine_ = reader.readLine()) != null) {

			// task cancelled
			if (task_.isCancelled()) {
				break;
			}

			// increment read lines
			readLines_++;

			// update progress
			task_.updateProgress(readLines_, allLines_);

			// split line
			String[] split = sthLine_.trim().split(" ");

			// loop over columns
			for (String col : split) {

				// invalid value
				if (col == null || col.isEmpty()) {
					continue;
				}

				// trim spaces
				col = col.trim();

				// invalid value
				if (col.isEmpty()) {
					continue;
				}

				// get peak value
				peakVal = Double.parseDouble(col);

				// last row
				if (rowIndex_ == numRows - 1) {

					// add peaks
					sigmaLine_ += String.format("%14s", format_.format(peakVal));
					colIndex_++;

					// last column
					if (colIndex_ == (rem == 0 ? NUM_COLS : rem)) {
						writer.write(sigmaLine_);
						writer.write("\n");
						sigmaLine_ = "";
						colIndex_ = 0;
						rowIndex_++;
					}
				}

				// other rows
				else {

					// add peaks
					sigmaLine_ += String.format("%14s", format_.format(peakVal));
					colIndex_++;

					// last column
					if (colIndex_ == NUM_COLS) {
						writer.write(sigmaLine_);
						writer.write("\n");
						sigmaLine_ = "";
						colIndex_ = 0;
						rowIndex_++;
					}
				}

				// increment peak number
				readPeaks++;
			}

			// all peaks read
			if (readPeaks == numPeaks_)
				return;
		}
	}

	/**
	 * Writes out the typical flight header.
	 *
	 * @param reader
	 *            STH file reader.
	 * @param writer
	 *            SIGMA file writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeFlightHeader(BufferedReader reader, BufferedWriter writer) throws Exception {

		// initialize variables
		String flightName = null;
		double validity = 0.0;

		// split line
		String[] split = sthLine_.trim().split(" ");

		// loop over columns
		int index = 0;
		for (String col : split) {

			// invalid value
			if (col == null || col.isEmpty()) {
				continue;
			}

			// trim spaces
			col = col.trim();

			// invalid value
			if (col.isEmpty()) {
				continue;
			}

			// validity
			if (index == 0) {
				validity = Double.parseDouble(col);
			}

			// increment index
			index++;
		}

		// read next line
		sthLine_ = reader.readLine();
		readLines_++;
		task_.updateProgress(readLines_, allLines_);

		// null line
		if (sthLine_ == null)
			throw new Exception("Null line encountered during reading STH file.");

		// split line
		split = sthLine_.trim().split(" ");

		// loop over columns
		index = 0;
		for (String col : split) {

			// invalid value
			if (col == null || col.isEmpty()) {
				continue;
			}

			// trim spaces
			col = col.trim();

			// invalid value
			if (col.isEmpty()) {
				continue;
			}

			// number of peaks
			if (index == 0) {
				numPeaks_ = Integer.parseInt(col);
			}

			// flight name
			else if (index == 1) {
				flightName = col;
			}

			// increment index
			index++;
		}

		// pass 1 line
		writer.write("\n");

		// write first line of flight info
		int flightNum = flightNumber_ + 1;
		String line = "NUVOL ";
		line += String.format("%6s", flightNum);
		line += " ! FLIGHT ";
		line += flightName;
		writer.write(line);
		writer.write("\n");

		// write second line of flight info
		line = "TITLE FLIGHT NB ";
		line += String.format("%6s", flightNum);
		line += " ! FLIGHT ";
		line += flightName;
		writer.write(line);
		writer.write("\n");

		// write third line of flight info
		line = "NBOCCU ";
		line += String.format("%4s", validity);
		writer.write(line);
		writer.write("\n");

		// write fourth line of flight info
		line = "NBVAL ";
		line += String.format("%6s", numPeaks_);
		writer.write(line);
		writer.write("\n");

		// increment flight number
		flightNumber_++;
	}

	/**
	 * Writes out flight sequence.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param writer
	 *            File writer.
	 * @param anaFileID
	 *            ANA file ID.
	 * @param flsFileID
	 *            FLS file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeFlightSequence(Connection connection, Statement statement, BufferedWriter writer, int anaFileID, int flsFileID) throws Exception {

		// update progress info
		task_.updateMessage("Writing flight sequence...");

		// write header
		writer.write("FLIGHTS SEQUENCE");
		writer.write("\n");

		// prepare statement for getting flight numbers
		String sql = "select flight_num from ana_flights where file_id = " + anaFileID + " and name = ?";
		try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

			// get flight names
			sql = "select name from fls_flights where file_id = " + flsFileID + " order by flight_num asc";
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// loop over flights
				int flightCount = 0;
				while (resultSet.next()) {

					// task cancelled
					if (task_.isCancelled())
						return;

					// update progress
					task_.updateProgress(flightCount, validity_);
					flightCount++;

					// get name
					String name = resultSet.getString("name");
					statement2.setString(1, name);

					// get flight numbers
					try (ResultSet resultSet2 = statement2.executeQuery()) {

						// loop over flight numbers
						while (resultSet2.next()) {
							String line = String.format("%6s", resultSet2.getInt("flight_num") + 1);
							line += " ! FLIGHT ";
							line += name;
							writer.write(line);
							writer.write("\n");
						}
					}
				}
			}
		}
	}

	/**
	 * Writes out SIGMA file header.
	 *
	 * @param statement
	 *            Database statement.
	 * @param writer
	 *            File writer.
	 * @param anaFileID
	 *            ANA file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeHeader(Statement statement, BufferedWriter writer, int anaFileID) throws Exception {

		// update progress info
		task_.updateMessage("Writing SIGMA file header...");

		// write total number of flights
		String line = "NBVOL ";
		line += String.format("%6s", validity_);
		line += " ! TOTAL NUMBER OF FLIGHTS";
		writer.write(line);
		writer.write("\n");

		// write total number of flight types
		String sql = "select num_flights from ana_files where file_id = " + anaFileID;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				line = "NBTYPEVOL ";
				int numFlightTypes = resultSet.getInt("num_flights");
				line += String.format("%6s", numFlightTypes);
				line += " ! TOTAL NUMBER OF TYPE FLIGHTS";
				writer.write(line);
				writer.write("\n");
			}
		}

		// pass 1 line
		writer.write("\n");
	}
}
