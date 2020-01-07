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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import equinox.Equinox;
import equinox.data.input.AdaptDRFInput;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.ServerUtility;
import equinox.task.InternalEquinoxTask.DirectoryOutputtingTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableAdaptDRF;

/**
 * Class for adapt DRF plugin task.
 *
 * @author Murat Artim
 * @date 24 Aug 2017
 * @time 11:36:56
 *
 */
public class AdaptDRF extends InternalEquinoxTask<Void> implements LongRunningTask, DirectoryOutputtingTask, SavableTask {

	/** AdaptDRF version. */
	public static final String VERSION = "v1.08";

	/** Input. */
	private final AdaptDRFInput input_;

	/** Sub processes. */
	private Process process_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.0");

	/**
	 * Creates adapt DRF plugin task.
	 *
	 * @param input
	 *            Input.
	 */
	public AdaptDRF(AdaptDRFInput input) {
		input_ = input;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Run AdaptDRF " + VERSION;
	}

	@Override
	public Path getOutputDirectory() {
		return input_.getOutputDirectory();
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableAdaptDRF(input_);
	}

	@Override
	public String getOutputMessage() {
		String message = getTaskTitle() + " is successfully completed. ";
		message += "Click 'Outputs' to see outputs of the task.";
		return message;
	}

	@Override
	public String getOutputButtonText() {
		return "Outputs";
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.RUN_ADAPT_DRF_PLUGIN);

		// update progress info
		updateTitle("Running AdaptDRF " + VERSION);

		// create input file
		Path inputFile = input_.getOutputDirectory().resolve("00_adapt_DRF_inputfile.txt");

		// copy input files to output directory
		updateMessage("Copying input files to output directory...");
		Path txtFile = Files.copy(input_.getTXTFile(), input_.getOutputDirectory().resolve(input_.getTXTFile().getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
		Path anaFile = Files.copy(input_.getANAFile(), input_.getOutputDirectory().resolve(input_.getANAFile().getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);

		// create file writer
		try (BufferedWriter writer = Files.newBufferedWriter(inputFile, Charset.defaultCharset())) {

			// write header info
			writeHeader(writer);

			// write parameters
			int runTill = input_.getRunTillFlight();
			writer.write("begin GLOBAL_INPUTS");
			writer.newLine();
			writer.newLine();
			writer.write("             date                       =   " + new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
			writer.newLine();
			writer.write("             spectra_txt_file           =   " + txtFile.getFileName().toString());
			writer.newLine();
			writer.write("             spectra_ana_file           =   " + anaFile.getFileName().toString());
			writer.newLine();
			writer.write("             fatigue_event_affected     =   " + input_.getTargetEvent());
			writer.newLine();
			writer.write("             DRF_OLD_value              =   " + format_.format(input_.getCurrentDRF()));
			writer.newLine();
			writer.write("             DRF_NEW_value              =   " + format_.format(input_.getNewDRF()));
			writer.newLine();
			writer.write("             run_only_till_flight_num   =   " + (runTill == -1 ? "1e6" : runTill));
			writer.newLine();
			writer.write("             add_comments_to_anafile    =   " + (input_.getAddComments() ? "1" : "0"));
			writer.newLine();
			writer.newLine();
			writer.write("end GLOBAL_INPUTS         ");
			writer.newLine();
		}

		// run process
		runProcess(inputFile);

		// return
		return null;
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
	 * Runs process.
	 *
	 * @param inputFile
	 *            Input file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void runProcess(Path inputFile) throws Exception {

		// update progress info
		updateMessage("Running AdaptDRF " + VERSION + "...");

		// get path to perl script and perl executable
		Path script = Equinox.SCRIPTS_DIR.resolve("adaptDRF_v1.08.pl");

		// create process builder for windows
		ProcessBuilder pb = null;
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			Path perl = Equinox.SCRIPTS_DIR.resolve("perl").resolve("bin").resolve("perl.exe");
			pb = new ProcessBuilder(perl.toAbsolutePath().toString(), script.toAbsolutePath().toString());
		}

		// create process builder for macOSX
		else if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString());
		}

		// create process builder for linux
		else if (Equinox.OS_TYPE.equals(ServerUtility.LINUX)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString());
		}

		// unsupported OS
		if (pb == null)
			throw new Exception("Unsupported operation system.");

		// execute process and wait to end
		Path workingDir = input_.getOutputDirectory();
		pb.directory(workingDir.toFile());
		File log = workingDir.resolve("adaptDRF_v1.08.log").toFile();
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		process_ = pb.start();
		assert pb.redirectInput() == Redirect.PIPE;
		assert pb.redirectOutput().file() == log;
		assert process_.getInputStream().read() == -1;

		// process failed
		if (process_.waitFor() != 0)
			throw new Exception("AdaptDRF failed! See log file '" + log.getAbsolutePath() + "'for details.");
	}

	/**
	 * Writes header info.
	 *
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeHeader(BufferedWriter writer) throws Exception {
		updateMessage("Writing header information to input file...");
		writer.write("# This is the input file for AdaptDRF " + VERSION + ", DRF Adaptation Plugin of Equinox");
		writer.newLine();
		writer.write("# This file is automatically generated by Equinox Version " + Equinox.VERSION.toString() + ", Date: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
		writer.newLine();
		writer.newLine();
	}
}
