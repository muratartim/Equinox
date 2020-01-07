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

import java.io.BufferedWriter;
import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.AnalysisEngine;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.StressComponent;
import equinox.data.input.EquivalentStressInput;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.data.input.RfortExtendedInput;
import equinox.data.ui.RfortDirectOmission;
import equinox.data.ui.RfortOmission;
import equinox.data.ui.RfortPercentOmission;
import equinox.data.ui.SerializableRfortPilotPoint;
import equinox.plugin.FileType;
import equinox.serverUtilities.ServerUtility;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;

/**
 * Class for RFORT analysis task.
 *
 * @author Murat Artim
 * @date Jan 16, 2015
 * @time 4:02:18 PM
 */
public class RfortAnalysis extends TemporaryFileCreatingTask<AddSpectrum> implements LongRunningTask {

	/** RFORT extended version. */
	public static final String VERSION = "v3.6";

	/** Input file index. */
	private static final int ANA = 0, TXT = 1, STFS = 2;

	/** Input. */
	private final RfortExtendedInput input_;

	/** Analysis ID. */
	private final int analysisID_;

	/** Maximum stress amplitudes of all pilot points. */
	private final HashMap<String, Double> stressAmplitudes_;

	/** Omission. */
	private final RfortOmission omission_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/** ISAMI version. */
	private IsamiVersion isamiVersion_;

	/** ISAMI sub version. */
	private IsamiSubVersion isamiSubVersion_;

	/** True compression should be applied in propagation analysis. */
	private boolean applyCompression_;

	/** Sub processes. */
	private Process process_;

	/**
	 * Creates RFORT analysis task.
	 *
	 * @param input
	 *            Input.
	 * @param omission
	 *            Omission.
	 * @param analysisID
	 *            Analysis ID.
	 * @param stressAmplitudes
	 *            Maximum stress amplitudes of all pilot points.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public RfortAnalysis(RfortExtendedInput input, RfortOmission omission, int analysisID, HashMap<String, Double> stressAmplitudes, AnalysisEngine analysisEngine) {
		input_ = input;
		omission_ = omission;
		analysisID_ = analysisID;
		stressAmplitudes_ = stressAmplitudes;
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
	public RfortAnalysis setIsamiEngineInputs(IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
		return this;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "RFORT analysis for '" + omission_.toString() + "' for '" + input_.getANAFile().getFileName() + "'";
	}

	@Override
	protected AddSpectrum call() throws Exception {

		// copy input files into working directory
		Path[] inputFiles = copyInputFiles();

		// task cancelled
		if (isCancelled() || inputFiles == null)
			return null;

		// write input file
		Path inputFile = writeInputFile(inputFiles);

		// task cancelled
		if (isCancelled() || inputFile == null)
			return null;

		// run tool
		runTool(inputFile);

		// task cancelled
		if (isCancelled())
			return null;

		// get output ANA file
		Path ana = getOutputANAFile(inputFiles[ANA]);

		// create and return add spectrum task
		Path txt = input_.getTXTFile();
		Path cvt = input_.getCVTFile();
		Path fls = input_.getFLSFile();
		Path xls = input_.getXLSFile();
		String sheet = input_.getConversionTableSheet();
		AddSpectrum task = new AddSpectrum(null, null, null, null, null, null, null);

		// ANA file name could be found
		Path anaFileNamePath = ana.getFileName();
		if (anaFileNamePath == null)
			throw new Exception("ANA file name couldn't be obtained.");

		// copy ANA file
		Path anaCopy = task.getWorkingDirectory().resolve(anaFileNamePath.toString());
		Files.copy(ana, anaCopy, StandardCopyOption.REPLACE_EXISTING);

		// set spectrum files to task
		task.setSpectrumFiles(anaCopy, txt, cvt, fls, xls, sheet);

		// return task
		return task;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// start equivalent stress analysis
		try {
			startEquivalentStressAnalysis(get());
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
		if (process_ != null && process_.isAlive()) {
			process_.destroyForcibly();
		}
	}

	/**
	 * Starts equivalent stress analysis with given ANA file.
	 *
	 * @param addSpectrum
	 *            Add spectrum task.
	 */
	private void startEquivalentStressAnalysis(AddSpectrum addSpectrum) {

		// create and add set spectrum mission task
		addSpectrum.addAutomaticTask("Set Spectrum Mission", new EmbeddedTask<>(new SetSpectrumMission(null, omission_.toString()), ExecutionMode.PARALLEL));

		// create add STF files tasks
		ArrayList<File> stfFiles = new ArrayList<>();
		for (SerializableRfortPilotPoint pp : input_.getPilotPoints()) {
			stfFiles.add(pp.getFile());
		}
		AddSTFFiles addSTFFiles = new AddSTFFiles(stfFiles, null, null);

		// loop over pilot points
		for (SerializableRfortPilotPoint pp : input_.getPilotPoints()) {

			// create generate stress sequence task
			GenerateStressSequenceInput generate = new GenerateStressSequenceInput();

			// set stress factors
			double ppFactor = Double.parseDouble(pp.getFactor());
			double stressFactor = input_.getOverallFactor() * ppFactor;
			double dpStressFactor = input_.getAddDP() ? input_.getDPFactor() * input_.getOverallFactor() * ppFactor : 0.0;
			if (input_.getAddDP()) {
				generate.setReferenceDP(input_.getRefDP());
			}
			generate.setStressModifier(GenerateStressSequenceInput.ONEG, stressFactor, GenerateStressSequenceInput.MULTIPLY);
			generate.setStressModifier(GenerateStressSequenceInput.INCREMENT, stressFactor, GenerateStressSequenceInput.MULTIPLY);
			generate.setStressModifier(GenerateStressSequenceInput.DELTAP, dpStressFactor, GenerateStressSequenceInput.MULTIPLY);
			generate.setStressModifier(GenerateStressSequenceInput.DELTAT, stressFactor, GenerateStressSequenceInput.MULTIPLY);
			generate.setFileName(null);
			generate.setStressComponent(input_.getComponent());
			generate.setRotationAngle(input_.getRotation());
			GenerateStressSequence generateStressSequence = new GenerateStressSequence(null, generate);

			// fatigue equivalent stress analysis
			if (input_.getRunFatigueAnalysis()) {
				EquivalentStressInput fatigueInput = new EquivalentStressInput(false, true, 0.0, pp.getFatigueMaterial());
				EquivalentStressAnalysis fatigueAnalysis = new EquivalentStressAnalysis(null, fatigueInput, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
				SaveRfortInfo saveFatigueInfo = new SaveRfortInfo(input_, null, analysisID_, omission_.toString(), omission_.getOmissionType(), analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
				fatigueAnalysis.addAutomaticTask(Integer.toString(saveFatigueInfo.hashCode()), new EmbeddedTask<>(saveFatigueInfo, ExecutionMode.PARALLEL));
				generateStressSequence.addAutomaticTask(Integer.toString(fatigueAnalysis.hashCode()), new EmbeddedTask<>(fatigueAnalysis, ExecutionMode.PARALLEL));
			}

			// preffas equivalent stress analysis
			if (input_.getRunPreffasAnalysis()) {
				EquivalentStressInput preffasInput = new EquivalentStressInput(false, true, 0.0, pp.getPreffasMaterial());
				EquivalentStressAnalysis preffasAnalysis = new EquivalentStressAnalysis(null, preffasInput, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
				SaveRfortInfo savePreffasInfo = new SaveRfortInfo(input_, null, analysisID_, omission_.toString(), omission_.getOmissionType(), analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
				preffasAnalysis.addAutomaticTask(Integer.toString(savePreffasInfo.hashCode()), new EmbeddedTask<>(savePreffasInfo, ExecutionMode.PARALLEL));
				generateStressSequence.addAutomaticTask(Integer.toString(preffasAnalysis.hashCode()), new EmbeddedTask<>(preffasAnalysis, ExecutionMode.PARALLEL));
			}

			// linear prop. equivalent stress analysis
			if (input_.getRunLinearAnalysis()) {
				EquivalentStressInput linearInput = new EquivalentStressInput(false, true, 0.0, pp.getLinearMaterial());
				EquivalentStressAnalysis linearAnalysis = new EquivalentStressAnalysis(null, linearInput, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
				SaveRfortInfo saveLinearInfo = new SaveRfortInfo(input_, null, analysisID_, omission_.toString(), omission_.getOmissionType(), analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
				linearAnalysis.addAutomaticTask(Integer.toString(saveLinearInfo.hashCode()), new EmbeddedTask<>(saveLinearInfo, ExecutionMode.PARALLEL));
				generateStressSequence.addAutomaticTask(Integer.toString(linearAnalysis.hashCode()), new EmbeddedTask<>(linearAnalysis, ExecutionMode.PARALLEL));
			}

			// add generate stress sequence task to add STF files task
			addSTFFiles.addAutomaticTask(pp.getName(), new EmbeddedTask<>(generateStressSequence, ExecutionMode.PARALLEL));
		}

		// add add STF files task to add spectrum task
		addSpectrum.addAutomaticTask("Add STF Files", new EmbeddedTask<>(addSTFFiles, ExecutionMode.PARALLEL));

		// run task
		taskPanel_.getOwner().runTaskInParallel(addSpectrum);
	}

	/**
	 * Returns path to output ANA file.
	 *
	 * @param inputANAFile
	 *            Path to input ANA file.
	 * @return Path to output ANA file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path getOutputANAFile(Path inputANAFile) throws Exception {

		// get output ANA file
		Path outputANA = getWorkingDirectory().resolve(FileType.getNameWithoutExtension(inputANAFile) + "_output_RfExD.ana");

		// output file doesn't exist
		if (!Files.exists(outputANA))
			throw new Exception("Output ANA file '" + outputANA.getFileName() + "' could not be found.");

		// get output ANA file name
		Path outputANAFileName = outputANA.getFileName();
		if (outputANAFileName == null)
			throw new Exception("Cannot get output ANA file name.");

		// get parent directory
		Path parentDir = outputANA.getParent();
		if (parentDir == null)
			throw new Exception("Cannot get output ANA file parent directory.");

		// rename output ANA file
		String newName = outputANAFileName.toString().replaceFirst("_output_RfExD", "_" + omission_.toString());
		Path renamedANA = parentDir.resolve(newName);
		return Files.move(outputANA, renamedANA, StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * Copies input files into working directory.
	 *
	 * @return Paths to input files.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path[] copyInputFiles() throws Exception {

		// progress info
		updateMessage("Copying input files to working directory...");

		// get TXT file name
		Path txtFileName = input_.getTXTFile().getFileName();
		if (txtFileName == null)
			throw new Exception("Cannot get TXT file name.");

		// create input file paths
		Path[] inputFiles = new Path[3];
		inputFiles[ANA] = extractANAFile();
		inputFiles[TXT] = Files.copy(input_.getTXTFile(), getWorkingDirectory().resolve(txtFileName.toString()));
		inputFiles[STFS] = Files.createDirectory(getWorkingDirectory().resolve("pilotPoints"));

		// copy STF files
		for (SerializableRfortPilotPoint pp : input_.getPilotPoints())
			if (pp.getIncludeInRfort()) {
				Path file = inputFiles[STFS].resolve(pp.getFile().getName());
				Files.copy(pp.getFile().toPath(), file, StandardCopyOption.REPLACE_EXISTING);
			}

		// return input files
		return inputFiles;
	}

	/**
	 * Extracts the ANA file into working directory.
	 *
	 * @return Extracted ANA file in the working directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path extractANAFile() throws Exception {

		// initialize input file and type
		Path anaFile = null;
		FileType type = FileType.getFileType(input_.getANAFile().toFile());

		// input file is a ZIP file
		if (type.equals(FileType.ZIP)) {
			updateMessage("Extracting zipped ANA file...");
			anaFile = Utility.extractFileFromZIP(input_.getANAFile(), this, FileType.ANA, null);
		}

		// input file is a GZIP file
		else if (type.equals(FileType.GZ)) {
			anaFile = getWorkingDirectory().resolve(FileType.appendExtension(FileType.getNameWithoutExtension(input_.getANAFile()), FileType.ANA));
			updateMessage("Extracting zipped ANA file...");
			Utility.extractFileFromGZIP(input_.getANAFile(), anaFile);
		}

		// ANA file
		else if (type.equals(FileType.ANA)) {

			// get ANA file name
			Path anaFileName = input_.getANAFile().getFileName();
			if (anaFileName == null)
				throw new Exception("Cannot get ANA file name.");

			// get ANA file
			anaFile = getWorkingDirectory().resolve(anaFileName.toString());
			Files.copy(input_.getANAFile(), anaFile);
		}

		// return ANA file
		return anaFile;
	}

	/**
	 * Writes out input file.
	 *
	 * @param inputFiles
	 *            Input files.
	 * @return Path to input file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeInputFile(Path[] inputFiles) throws Exception {

		// update progress info
		updateMessage("Writing input file for RFORT Extended " + VERSION + "...");

		// get file names
		Path ppDirName = inputFiles[STFS].getFileName();
		Path txtFileName = inputFiles[TXT].getFileName();
		Path anaFileName = inputFiles[ANA].getFileName();
		if (ppDirName == null || txtFileName == null || anaFileName == null)
			throw new Exception("Cannot get input file name(s).");

		// create path to input file (same location as ANA file)
		Path inputFile = inputFiles[ANA].resolveSibling("rfort_extended_" + VERSION + ".input");

		// create file writer
		try (BufferedWriter writer = Files.newBufferedWriter(inputFile, Charset.defaultCharset())) {

			// write header info
			writer.write("#  This is the input file for RFORT Extended " + VERSION + ", Level 3 & 4 Test Spectra Reduction Plugin of Equinox");
			writer.newLine();
			writer.write("#  Input format allows different omission levels per pilot point");
			writer.newLine();
			writer.write("#  This file is automatically generated by Equinox Version " + Equinox.VERSION.toString() + ", Date: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
			writer.newLine();
			writer.newLine();

			// write global inputs
			writer.write("begin GLOBAL_INPUTS");
			writer.newLine();
			writer.newLine();
			writer.write("             job_name                 =   output");
			writer.newLine();
			writer.write("             date                     =   " + new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
			writer.newLine();
			writer.write("			 pilot_points_directory   =   " + ppDirName.toString());
			writer.newLine();
			writer.write("             index_file               =   " + txtFileName.toString());
			writer.newLine();
			writer.write("             flugablauf_file          =   " + anaFileName.toString());
			writer.newLine();
			writer.write("			 add2sth_Delta_P          =      " + (input_.getAddDP() ? "1" : "0"));
			writer.newLine();
			writer.write("			 reference_Delta_P        = " + input_.getRefDP());
			writer.newLine();
			writer.write("			 enable_SLOG_mode         =      " + (input_.getEnableSlogMode() ? "1" : "0"));
			writer.newLine();
			int column = 1;
			StressComponent comp = input_.getComponent();
			if (comp.equals(StressComponent.NORMAL_X)) {
				column = 1;
			}
			else if (comp.equals(StressComponent.NORMAL_Y)) {
				column = 2;
			}
			else if (comp.equals(StressComponent.SHEAR_XY)) {
				column = 3;
			}
			else if (comp.equals(StressComponent.ROTATED)) {
				column = 0;
			}
			int rotation = column > 0 ? 0 : input_.getRotation();
			writer.write("			 rotation_degrees         =      " + rotation);
			writer.newLine();
			writer.write("			 use_stress_column        =      " + column);
			writer.newLine();
			writer.write("			 set_it_zero              =     NO");
			writer.newLine();
			writer.write("			 fatigue_fac_all          =    " + input_.getOverallFactor());
			writer.newLine();
			writer.write("			 delta_p_fac_only         =    " + input_.getDPFactor());
			writer.newLine();
			writer.write("	    attack_only_these_flights     =    " + (input_.getTargetFlights().isEmpty() ? "ALL" : input_.getTargetFlights()));
			writer.newLine();
			writer.newLine();
			writer.write("end	GLOBAL_INPUTS");
			writer.newLine();
			writer.newLine();

			// write pilot points
			writer.write("begin PILOT_POINTS");
			writer.newLine();
			writer.newLine();
			for (SerializableRfortPilotPoint pp : input_.getPilotPoints())
				if (pp.getIncludeInRfort()) {
					String ppName = pp.getName();
					double omissionValue = getOmissionValue(ppName, stressAmplitudes_.get(pp.getName()));
					writer.write(" " + omissionValue + " = " + ppName + " = " + pp.getFactor() + " = 0.0 = -1e9");
					writer.newLine();
				}
			writer.newLine();
			writer.write("end PILOT_POINTS");
			writer.newLine();
		}

		// return input file
		return inputFile;
	}

	/**
	 * Returns omission value.
	 *
	 * @param ppName
	 *            Pilot point name.
	 * @param maxStressAmp
	 *            Maximum stress amplitude.
	 * @return Omission value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double getOmissionValue(String ppName, double maxStressAmp) throws Exception {

		// percent omission
		if (omission_ instanceof RfortPercentOmission) {
			RfortPercentOmission percentOmission = (RfortPercentOmission) omission_;
			return maxStressAmp * percentOmission.getPercentOmission() / 100.0;
		}

		// direct omission
		else if (omission_ instanceof RfortDirectOmission) {
			RfortDirectOmission directOmission = (RfortDirectOmission) omission_;
			Iterator<Entry<String, Double>> omissions = directOmission.getOmissions().entrySet().iterator();
			while (omissions.hasNext()) {
				Entry<String, Double> o = omissions.next();
				if (ppName.equals(o.getKey()))
					return o.getValue();
			}
		}

		// cannot obtain omission value
		String message = "Cannot get omission value for pilot point '" + ppName;
		message += "' for omission '" + omission_.toString() + "'.";
		throw new Exception(message);
	}

	/**
	 * Runs RFORT extended tool with given input file.
	 *
	 * @param inputFile
	 *            Path to input file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void runTool(Path inputFile) throws Exception {

		// update progress info
		updateMessage("Running RFORT Extended " + VERSION + "...");

		// get run till flight option
		String runTill = input_.getRunTillFlight() == null ? "1e6" : Integer.toString(input_.getRunTillFlight());

		// get path to perl script and perl executable
		Path script = Equinox.SCRIPTS_DIR.resolve("rfort_extended_v3.6.pl");

		// get input file name
		Path inputFileName = inputFile.getFileName();
		if (inputFileName == null)
			throw new Exception("Cannot get input file name.");

		// create process builder for windows
		ProcessBuilder pb = null;
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			Path perl = Equinox.SCRIPTS_DIR.resolve("perl").resolve("bin").resolve("perl.exe");
			pb = new ProcessBuilder(perl.toAbsolutePath().toString(), script.toAbsolutePath().toString(), inputFileName.toString(), runTill);
		}

		// create process builder for macOSX
		else if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), inputFileName.toString(), runTill);
		}
		else if (Equinox.OS_TYPE.equals(ServerUtility.LINUX)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), inputFileName.toString(), runTill);
		}

		// unsupported OS
		if (pb == null)
			throw new Exception("Unsupported operation system.");

		// execute process and wait to end
		Path workingDir = getWorkingDirectory();
		pb.directory(workingDir.toFile());
		File log = workingDir.resolve("rfort_extended_v3.6.log").toFile();
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		process_ = pb.start();
		assert pb.redirectInput() == Redirect.PIPE;
		assert pb.redirectOutput().file() == log;
		assert process_.getInputStream().read() == -1;

		// process failed
		if (process_.waitFor() != 0)
			throw new Exception("RFORT Extended failed! See log file '" + log.getAbsolutePath() + "'for details.");
	}
}
