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
import equinox.analysisServer.remote.message.FullESAComplete;
import equinox.analysisServer.remote.message.IsamiESARequest;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.ExternalStressSequence;
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
import equinox.task.AnalysisListenerTask;
import equinox.task.EquivalentStressAnalysis;
import equinox.utility.Utility;
import equinox.utility.exception.ServerAnalysisFailedException;

/**
 * Class for ISAMI equivalent stress analysis process.
 *
 * @author Murat Artim
 * @date 13 Jun 2017
 * @time 07:32:58
 *
 */
public class IsamiESA implements ESAProcess<Void>, AnalysisListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** The owner task of this process. */
	private final EquivalentStressAnalysis task_;

	/** Path to input STH file. */
	private final Path inputSTH_;

	/** Equivalent stress. */
	private final SpectrumItem stressSequence_, eqStress_;

	/** FLS file ID. */
	private final int flsFileID_;

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

	/** Parameters. */
	private int readLines_, allLines_, numFlights_, flightNumber_, numPeaks_, rowIndex_ = 0, colIndex_ = 0;

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
	 * Creates ISAMI equivalent stress analysis process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputSTH
	 *            Path to input STH file.
	 * @param stressSequence
	 *            Stress sequence.
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
	 * @param isamiVersion
	 *            ISAMI version.
	 * @param isamiSubVersion
	 *            ISAMI sub version.
	 * @param applyCompression
	 *            True compression should be applied in propagation analysis.
	 */
	public IsamiESA(EquivalentStressAnalysis task, Path inputSTH, SpectrumItem stressSequence, SpectrumItem eqStress, int flsFileID, Material material, boolean keepOutputs, String outputFileName, IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		task_ = task;
		inputSTH_ = inputSTH;
		stressSequence_ = stressSequence;
		eqStress_ = eqStress;
		flsFileID_ = flsFileID;
		material_ = material;
		isAnalysisCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
		keepOutputs_ = keepOutputs;
		outputFileName_ = outputFileName;
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
	}

	@Override
	public Void start(Connection localConnection, PreparedStatement... preparedStatements) throws Exception {

		// declare network watcher
		AnalysisServerManager manager = null;
		boolean removeListener = false;

		try {

			// check if external stress sequence
			boolean isExternal = stressSequence_ instanceof ExternalStressSequence;

			// write SIGMA file
			Path sigmaFile = writeSigmaFile(localConnection, isExternal);

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
			request.setFastAnalysis(false);
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
			manager = task_.getTaskPanel().getOwner().getOwner().getAnalysisServerManager();
			manager.addMessageListener(this);
			removeListener = true;
			manager.sendMessage(request);

			// wait for analysis to complete
			waitForAnalysisServer(task_, isAnalysisCompleted);

			// remove from network watcher
			manager.removeMessageListener(this);
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
			if (manager != null && removeListener) {
				manager.removeMessageListener(this);
			}
		}
	}

	@Override
	public void respondToAnalysisMessage(AnalysisMessage message) throws Exception {
		processServerAnalysisMessage(message, task_, serverMessageRef, isAnalysisCompleted);
	}

	@Override
	public void cancel() {
		// no implementation
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
			Path zippedHtml = task_.getWorkingDirectory().resolve(outputFileName_ + ".zip");

			// download data from server
			try (FilerConnection filer = task_.getFilerConnection()) {
				filer.getSftpChannel().get(message.getDownloadUrl(), zippedHtml.toString());
			}

			// extract output file
			task_.updateMessage("Extracting analysis output file...");
			Path html = Utility.extractFileFromZIP(zippedHtml, task_, FileType.HTML, null);

			// gzip dossier file
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
	 * Uploads CDF set to exchange server. Note that, in order to ensure the uniqueness of the uploaded file name, the following convention is used to create a file name for the uploaded file:
	 * <p>
	 * <code>userAlias_simpleClassName_currentTimeMillis.zip</code>
	 *
	 * @param path
	 *            Path to CDF set.
	 * @return URL to uploaded file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String uploadFile(Path path) throws Exception {

		// update info
		task_.updateMessage("Uploading input files to exchange server...");

		// initialize URL
		String url = null;

		// get file name
		Path fileName = path.getFileName();
		if (fileName == null)
			throw new Exception("Cannot get file name.");

		// get filer connection
		try (FilerConnection filer = task_.getFilerConnection()) {

			// set path to destination file
			// INFO construct file name with this convention: userAlias_simpleClassName_currentTimeMillis.zip
			url = filer.getDirectoryPath(FilerConnection.EXCHANGE) + "/" + Equinox.USER.getAlias() + "_" + this.getClass().getSimpleName() + "_" + System.currentTimeMillis() + ".zip";

			// upload file to filer
			filer.getSftpChannel().put(path.toString(), url);
		}

		// return download URL
		return url;
	}

	/**
	 * Writes out input sigma file.
	 *
	 * @param connection
	 *            Database connection.
	 * @param isExternal
	 *            True if stress sequence is external.
	 * @return SIGMA file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeSigmaFile(Connection connection, boolean isExternal) throws Exception {

		// update info
		task_.updateMessage("Generating sigma file...");

		// get output file
		Path sigmaFile = task_.getWorkingDirectory().resolve("input.sigma");

		// create file writer
		try (BufferedWriter writer = Files.newBufferedWriter(sigmaFile, Charset.defaultCharset())) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// write SIGMA file header
				writeHeader(statement, writer, isExternal);

				// task cancelled
				if (task_.isCancelled())
					return null;

				// write flights sequence
				writeFlightSequence(connection, statement, writer, isExternal);

				// task cancelled
				if (task_.isCancelled())
					return null;
			}

			// get line count of the STH file
			allLines_ = Utility.countLines(inputSTH_, task_);

			// create file reader
			try (BufferedReader reader = Files.newBufferedReader(inputSTH_, Charset.defaultCharset())) {

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
	 * Writes out SIGMA file header.
	 *
	 * @param statement
	 *            Database statement.
	 * @param writer
	 *            File writer.
	 * @param isExternal
	 *            True if stress sequence is external.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeHeader(Statement statement, BufferedWriter writer, boolean isExternal) throws Exception {

		// update progress info
		task_.updateMessage("Writing SIGMA file header...");

		// set table names
		String sthFlights = isExternal ? "ext_sth_flights" : "sth_flights";
		String sthFiles = isExternal ? "ext_sth_files" : "sth_files";

		// write total number of flights
		String sql = "select sum(validity) as totalFlights from " + sthFlights + " where file_id = " + stressSequence_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				String line = "NBVOL ";
				numFlights_ = (int) resultSet.getDouble("totalFlights");
				line += String.format("%6s", numFlights_);
				line += " ! TOTAL NUMBER OF FLIGHTS";
				writer.write(line);
				writer.write("\n");
			}
		}

		// write total number of flight types
		sql = "select num_flights from " + sthFiles + " where file_id = " + stressSequence_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				String line = "NBTYPEVOL ";
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

	/**
	 * Writes out flight sequence.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param writer
	 *            File writer.
	 * @param isExternal
	 *            True if stress sequence is external.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeFlightSequence(Connection connection, Statement statement, BufferedWriter writer, boolean isExternal) throws Exception {

		// update progress info
		task_.updateMessage("Writing flight sequence...");

		// set table names
		String sthFlights = isExternal ? "ext_sth_flights" : "sth_flights";
		String flsFlights = isExternal ? "ext_fls_flights" : "fls_flights";

		// write header
		writer.write("FLIGHTS SEQUENCE");
		writer.write("\n");

		// prepare statement for getting flight numbers
		String sql = "select flight_num from " + sthFlights + " where file_id = " + stressSequence_.getID() + " and name = ?";
		try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

			// get flight names
			sql = "select name from " + flsFlights + " where file_id = " + flsFileID_ + " order by flight_num asc";
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// loop over flights
				int flightCount = 0;
				while (resultSet.next()) {

					// task cancelled
					if (task_.isCancelled())
						return;

					// update progress
					task_.updateProgress(flightCount, numFlights_);
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

		// pass 1 line
		writer.write("\n");
	}
}
