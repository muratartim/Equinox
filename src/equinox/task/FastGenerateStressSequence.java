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

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.ActiveTasksPanel;
import equinox.data.AnalysisEngine;
import equinox.data.EmbeddedTask;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.Settings;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.FastEquivalentStressInput;
import equinox.dataServer.remote.data.Material;
import equinox.plugin.FileType;
import equinox.process.EquinoxProcess;
import equinox.process.FastGenerateSigma;
import equinox.process.FastGenerateSth;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.ServerUtility;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.task.serializableTask.SerializableFastGenerateStressSequence;
import equinox.utility.Utility;

/**
 * Class for fast generate stress sequence task.
 *
 * @author Murat Artim
 * @date Jun 14, 2016
 * @time 9:21:27 PM
 */
public class FastGenerateStressSequence extends TemporaryFileCreatingTask<ArrayList<FastEquivalentStressAnalysis>> implements LongRunningTask, SavableTask, SingleInputTask<STFFile>, AutomaticTaskOwner<SpectrumItem> {

	/** The owner STF file. */
	private STFFile stfFile_;

	/** STF file ID. */
	private final Integer stfID_, stressTableID_;

	/** STF file name. */
	private final String stfName_;

	/** The owner spectrum. */
	private final Spectrum spectrum_;

	/** Input. */
	private final FastEquivalentStressInput input_;

	/** Materials. */
	private final List<Material> materials_;

	/** True if typical flight damage contribution analysis is requested. */
	private final boolean isFlightDamageContributionAnalysis_;

	/** Omission process. */
	private Process omission_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/** ISAMI version. */
	private IsamiVersion isamiVersion_;

	/** ISAMI sub version. */
	private IsamiSubVersion isamiSubVersion_;

	/** True compression should be applied in propagation analysis. */
	private boolean applyCompression_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<SpectrumItem>> automaticTasks_ = null;

	/**
	 * Creates fast generate stress sequence task.
	 *
	 * @param stfFile
	 *            The owner STF file. Can be null for automatic execution.
	 * @param input
	 *            Analysis input.
	 * @param materials
	 *            Materials.
	 * @param isFlightDamageContributionAnalysis
	 *            True if typical flight damage contribution analysis is requested.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public FastGenerateStressSequence(STFFile stfFile, FastEquivalentStressInput input, List<Material> materials, boolean isFlightDamageContributionAnalysis, AnalysisEngine analysisEngine) {
		stfFile_ = stfFile;
		input_ = input;
		materials_ = materials;
		isFlightDamageContributionAnalysis_ = isFlightDamageContributionAnalysis;
		stfID_ = null;
		stressTableID_ = null;
		stfName_ = null;
		spectrum_ = null;
		analysisEngine_ = analysisEngine;
	}

	/**
	 * Creates fast generate stress sequence task.
	 *
	 * @param stfID
	 *            STF file ID.
	 * @param stressTableID
	 *            STF stress table ID.
	 * @param stfName
	 *            STF file name.
	 * @param spectrum
	 *            The owner spectrum.
	 * @param input
	 *            Analysis input.
	 * @param materials
	 *            Materials.
	 * @param isFlightDamageContributionAnalysis
	 *            True if typical flight damage contribution analysis is requested.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public FastGenerateStressSequence(int stfID, int stressTableID, String stfName, Spectrum spectrum, FastEquivalentStressInput input, List<Material> materials, boolean isFlightDamageContributionAnalysis, AnalysisEngine analysisEngine) {
		stfID_ = stfID;
		stressTableID_ = stressTableID;
		stfName_ = stfName;
		spectrum_ = spectrum;
		input_ = input;
		materials_ = materials;
		isFlightDamageContributionAnalysis_ = isFlightDamageContributionAnalysis;
		stfFile_ = null;
		analysisEngine_ = analysisEngine;
	}

	/**
	 * Sets ISAMI engine inputs.
	 *
	 * @param isamiVersion
	 *            ISAMI version.
	 * @param isamiSubVersion
	 *            ISAMI sub version.
	 * @param applyCompression
	 *            True to apply compression for propagation analyses.
	 * @return This analysis.
	 */
	public FastGenerateStressSequence setIsamiEngineInputs(IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
		return this;
	}

	@Override
	public void setAutomaticInput(STFFile stfFile) {
		stfFile_ = stfFile;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<SpectrumItem> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<SpectrumItem>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public String getTaskTitle() {
		return "Generate stress sequence";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public SerializableTask getSerializableTask() {
		if (stfFile_ != null)
			return new SerializableFastGenerateStressSequence(stfFile_, input_, materials_, isFlightDamageContributionAnalysis_, analysisEngine_);
		return new SerializableFastGenerateStressSequence(stfID_, stressTableID_, stfName_, spectrum_, input_, materials_, isFlightDamageContributionAnalysis_, analysisEngine_);
	}

	@Override
	protected ArrayList<FastEquivalentStressAnalysis> call() throws Exception {

		// check permission
		checkPermission(Permission.GENERATE_STRESS_SEQUENCE);

		// declare variables
		Path sequenceFile, flsFile = null;
		int validity;
		boolean useExtended;

		// get FLS file ID
		Spectrum cdfSet = stfFile_ == null ? spectrum_ : stfFile_.getParentItem();
		int anaFileID = cdfSet.getANAFileID();
		int flsFileID = cdfSet.getFLSFileID();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// check if extended analysis engine should be used for equivalent stress analysis
			useExtended = isUseExtendedEngine(connection);

			// check if ISAMI analysis engine will be used
			boolean isIsami = isIsamiEngine(useExtended);

			// check if SIGMA file to generated
			boolean generateSigmaFile = isGenerateSigma(useExtended);

			// get spectrum validity
			validity = getValidity(connection);

			// generate stress sequence file
			sequenceFile = generateStressSequence(connection, generateSigmaFile, validity);

			// task cancelled
			if (isCancelled() || sequenceFile == null)
				return null;

			// apply omission (if selected and not 0-level)
			if (input_.isApplyOmission() && input_.getOmissionLevel() > 0.0) {
				sequenceFile = applyOmission(sequenceFile);
			}

			// task cancelled
			if (isCancelled())
				return null;

			// save FLS file (if not ISAMI analysis)
			if (!isIsami) {
				flsFile = saveFLSFile(getWorkingDirectory().resolve("input.fls"), connection, flsFileID);
			}
		}

		// create and return equivalent stress analysis tasks
		return createEquivalentStressAnalysisTasks(sequenceFile, flsFile, validity, anaFileID, flsFileID, useExtended);
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// execute subsequent tasks
		try {
			ActiveTasksPanel tm = taskPanel_.getOwner();
			ArrayList<FastEquivalentStressAnalysis> tasks = get();
			for (FastEquivalentStressAnalysis task : tasks) {

				// add automatic tasks
				if (automaticTasks_ != null) {
					Iterator<Entry<String, EmbeddedTask<SpectrumItem>>> iterator = automaticTasks_.entrySet().iterator();
					while (iterator.hasNext()) {
						Entry<String, EmbeddedTask<SpectrumItem>> entry = iterator.next();
						task.addAutomaticTask(entry.getKey(), entry.getValue());
					}
				}

				// run task
				tm.runTaskSilently(task, false);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// destroy sub processes (if still running)
		if (omission_ != null && omission_.isAlive()) {
			omission_.destroyForcibly();
		}

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// destroy sub processes (if still running)
		if (omission_ != null && omission_.isAlive()) {
			omission_.destroyForcibly();
		}

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	/**
	 * Creates and returns equivalent stress analysis tasks.
	 *
	 * @param sequenceFile
	 *            Stress sequence file.
	 * @param flsFile
	 *            FLS file.
	 * @param validity
	 *            Spectrum validity.
	 * @param anaFileID
	 *            ANA file ID.
	 * @param flsFileID
	 *            FLS file ID.
	 * @param useExtended
	 *            True if maximum number of peaks exceeded the allowed number of peaks per typical flight and that the extended inbuilt analysis engine shall be used.
	 * @return Array list containing the newly created equivalent stress tasks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<FastEquivalentStressAnalysis> createEquivalentStressAnalysisTasks(Path sequenceFile, Path flsFile, int validity, int anaFileID, int flsFileID, boolean useExtended) throws Exception {

		// progress info
		updateMessage("Creating equivalent stress analysis tasks...");

		// create subsequent tasks
		ArrayList<FastEquivalentStressAnalysis> tasks = new ArrayList<>();

		// get task parameters
		boolean applyOmission = input_.isApplyOmission();
		double omissionLevel = input_.getOmissionLevel();

		// loop over materials
		for (int i = 0; i < materials_.size(); i++) {

			// progress info
			updateProgress(i, materials_.size());

			// create task
			FastEquivalentStressAnalysis task = null;
			if (stfFile_ == null) {
				task = new FastEquivalentStressAnalysis(stfID_, anaFileID, flsFileID, stfName_, validity, materials_.get(i), isFlightDamageContributionAnalysis_, useExtended, applyOmission, omissionLevel, input_, analysisEngine_);
				task.setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
			}
			else {
				task = new FastEquivalentStressAnalysis(stfFile_, anaFileID, flsFileID, validity, materials_.get(i), isFlightDamageContributionAnalysis_, useExtended, applyOmission, omissionLevel, input_, analysisEngine_);
				task.setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
			}

			// copy and set stress sequence file
			FileType sequenceFileType = FileType.getFileType(sequenceFile.toFile());
			Path sequenceFileCopy = task.getWorkingDirectory().resolve("input." + (sequenceFileType.equals(FileType.SIGMA) ? "sigma" : "sth"));
			task.setSequenceFile(Files.copy(sequenceFile, sequenceFileCopy, StandardCopyOption.REPLACE_EXISTING));

			// copy and set FLS file (if not null)
			if (flsFile != null) {
				Path flsFileCopy = task.getWorkingDirectory().resolve("input.fls");
				task.setFLSFile(Files.copy(flsFile, flsFileCopy, StandardCopyOption.REPLACE_EXISTING));
			}

			// add to tasks
			tasks.add(task);
		}

		// return tasks
		return tasks;
	}

	/**
	 * Runs omission script on a separate process. This method will wait till the process has ended.
	 *
	 * @param inputSTH
	 *            Input STH file.
	 * @return Path to omission output file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path applyOmission(Path inputSTH) throws Exception {

		// update info
		updateMessage("Applying omission to stress sequence '" + inputSTH.getFileName() + "'...");
		updateProgress(-1, 100);

		// get path to perl script and perl executable
		Path script = Equinox.SCRIPTS_DIR.resolve("omission.pl");

		// get input STH file name
		Path inputSTHFileName = inputSTH.getFileName();
		if (inputSTHFileName == null)
			throw new Exception("Cannot get input STH file name.");

		// create process builder for windows
		ProcessBuilder pb = null;
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			Path perl = Equinox.SCRIPTS_DIR.resolve("perl").resolve("bin").resolve("perl.exe");
			pb = new ProcessBuilder(perl.toAbsolutePath().toString(), script.toAbsolutePath().toString(), inputSTHFileName.toString(), Double.toString(input_.getOmissionLevel()));
		}

		// create process builder for macOSX
		else if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), inputSTHFileName.toString(), Double.toString(input_.getOmissionLevel()));
		}
		else if (Equinox.OS_TYPE.equals(ServerUtility.LINUX)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), inputSTHFileName.toString(), Double.toString(input_.getOmissionLevel()));
		}

		// unsupported OS
		if (pb == null)
			throw new Exception("Unsupported operating system.");

		// execute process and wait till it ends
		Path workingDir = getWorkingDirectory();
		pb.directory(workingDir.toFile());
		File log = workingDir.resolve("omission.log").toFile();
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		omission_ = pb.start();
		assert pb.redirectInput() == Redirect.PIPE;
		assert pb.redirectOutput().file() == log;
		assert omission_.getInputStream().read() == -1;

		// process failed
		if (omission_.waitFor() != 0)
			throw new Exception("Stress sequence omission failed! See omission log file for details.");

		// get output file
		Path output = Paths.get(inputSTH.toString() + FileType.RFORT.getExtension());

		// output file doesn't exist
		if (!Files.exists(output))
			throw new Exception("Stress sequence omission failed! See omission log file for details.");

		// return output path
		return output;
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
		updateMessage("Getting spectrum validity from database...");

		// initialize validity
		int validity = -1;
		int flsFileID = stfFile_ == null ? spectrum_.getFLSFileID() : stfFile_.getParentItem().getFLSFileID();

		// create statement
		try (Statement statement = connection.createStatement()) {

			// execute query
			statement.setMaxRows(1);
			try (ResultSet resultSet = statement.executeQuery("select flight_num from fls_flights where file_id = " + flsFileID + " order by flight_num desc")) {
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
	 * @param flsFileID
	 *            FLS file ID.
	 * @return Output FLS file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path saveFLSFile(Path output, Connection connection, int flsFileID) throws Exception {

		// update info
		updateMessage("Saving input FLS file...");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// execute query
			try (ResultSet resultSet = statement.executeQuery("select name, data from fls_files where file_id = " + flsFileID)) {

				// get data
				if (resultSet.next()) {

					// get file name
					String name = resultSet.getString("name");

					// get blob
					Blob blob = resultSet.getBlob("data");

					// FLS file format
					Path zipFile = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
					Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
					Path flsFile = Utility.extractFileFromZIP(zipFile, this, FileType.FLS, null);
					Files.copy(flsFile, output, StandardCopyOption.REPLACE_EXISTING);

					// free blob
					blob.free();
				}
			}
		}

		// return output path
		return output;
	}

	/**
	 * Returns true if ISAMI analysis engine will be used for analysis.
	 *
	 * @param useExtended
	 *            True if extended inbuilt engine should be used.
	 * @return true if ISAMI analysis engine will be used for analysis.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean isIsamiEngine(boolean useExtended) throws Exception {

		// maximum allowed number of peaks exceeded
		if (useExtended)
			return false;

		// flight damage contribution analysis
		if (isFlightDamageContributionAnalysis_)
			return false;

		// get fall back option
		boolean isFallback = (boolean) taskPanel_.getOwner().getOwner().getSettings().getValue(Settings.FALLBACK_TO_INBUILT);

		// ISAMI engine
		if (analysisEngine_.equals(AnalysisEngine.ISAMI)) {

			// connected to server (ISAMI engine)
			if (taskPanel_.getOwner().getOwner().getAnalysisServerManager().isConnected())
				return true;

			// not connected (Equinox engine)
			else if (isFallback)
				return false;

			// fallback not selected
			else
				throw new Exception("Cannot connect to Equinox server.");
		}

		// other engine (SAFE or Equinox)
		return false;
	}

	/**
	 * Returns true if SIGMA file is to be generated, false if STH file should be generated.
	 *
	 * @param useExtended
	 *            True if extended inbuilt engine should be used.
	 * @return True if SIGMA file is to be generated, false if STH file should be generated.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean isGenerateSigma(boolean useExtended) throws Exception {

		// maximum allowed number of peaks exceeded
		if (useExtended)
			return false;

		// flight damage contribution analysis
		if (isFlightDamageContributionAnalysis_)
			return false;

		// get selected analysis engine
		boolean isFallback = (boolean) taskPanel_.getOwner().getOwner().getSettings().getValue(Settings.FALLBACK_TO_INBUILT);

		// ISAMI engine
		if (analysisEngine_.equals(AnalysisEngine.ISAMI)) {

			// connected to server (ISAMI engine)
			if (taskPanel_.getOwner().getOwner().getAnalysisServerManager().isConnected()) {

				// none-zero omission to be applied
				if (input_.isApplyOmission() && input_.getOmissionLevel() > 0.0)
					return false;

				// no omission or zero omission
				return true;
			}

			// not connected (Equinox engine)
			else if (isFallback)
				return false;

			// fallback not selected
			else
				throw new Exception("Cannot connect to Equinox server.");
		}

		// other engine (SAFE or Equinox)
		return false;
	}

	/**
	 * Returns true if maximum number of peaks exceeded the allowed number of peaks per typical flight and that the extended inbuilt analysis engine shall be used.
	 *
	 * @param connection
	 *            Database connection.
	 * @return True if maximum number of peaks exceeded the allowed number of peaks per typical flight and that the extended inbuilt analysis engine shall be used.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean isUseExtendedEngine(Connection connection) throws Exception {

		// get spectrum file IDs
		Spectrum cdfSet = stfFile_ == null ? spectrum_ : stfFile_.getParentItem();
		int anaFileID = cdfSet.getANAFileID();
		int maxPeaks = 0;

		// get maximum number of peaks per typical flight
		try (Statement statement = connection.createStatement()) {
			String sql = "select num_peaks from ana_flights where file_id = " + anaFileID + " order by num_peaks desc";
			statement.setMaxRows(1);
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					maxPeaks = resultSet.getInt("num_peaks");
				}
			}
			statement.setMaxRows(0);
		}

		// return
		return maxPeaks > EquivalentStressAnalysis.MAX_PEAKS;
	}

	/**
	 * Generates stress sequence and writes it out to either SIGMA or STH file.
	 *
	 * @param connection
	 *            Database connection.
	 * @param generateSigmaFile
	 *            True if SIGMA file should be generated.
	 * @param validity
	 *            Spectrum validity.
	 * @return Path to generated stress sequence file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path generateStressSequence(Connection connection, boolean generateSigmaFile, int validity) throws Exception {

		// initialize process
		EquinoxProcess<?> process;

		// generate SIGMA file
		if (generateSigmaFile) {
			process = new FastGenerateSigma(this, input_, stfFile_, stfID_, stressTableID_, spectrum_, validity);
		}

		// generate STH file
		else {
			process = new FastGenerateSth(this, input_, stfFile_, stfName_, stfID_, stressTableID_, spectrum_);
		}

		// start process and return path to output file
		return (Path) process.start(connection);
	}
}
