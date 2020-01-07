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
import equinox.data.StressComponent;
import equinox.data.input.MyCheckInput;
import equinox.data.ui.MyCheckMission;
import equinox.data.ui.MyCheckMission.MissionType;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.ServerUtility;
import equinox.task.InternalEquinoxTask.DirectoryOutputtingTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableMyCheck;
import equinox.utility.Utility;
import jxl.Sheet;
import jxl.Workbook;

/**
 * Class for MyCheck tool task.
 *
 * @author Murat Artim
 * @date Jan 18, 2015
 * @time 1:49:32 PM
 */
public class MyCheck extends InternalEquinoxTask<Void> implements LongRunningTask, DirectoryOutputtingTask, SavableTask {

	/** MyCheck version. */
	public static final String VERSION = "v3.0";

	/** Input. */
	private final MyCheckInput input_;

	/** Sub processes. */
	private Process process_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.0");

	/**
	 * Creates MyCheck tool task.
	 *
	 * @param input
	 *            Input.
	 */
	public MyCheck(MyCheckInput input) {
		input_ = input;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Run MyCheck " + VERSION;
	}

	@Override
	public Path getOutputDirectory() {
		return input_.getOutputDirectory();
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableMyCheck(input_);
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
		checkPermission(Permission.RUN_MYCHECK_PLUGIN);

		// update progress info
		updateTitle("Running MyCheck " + VERSION);

		// create input file
		Path inputFile = input_.getOutputDirectory().resolve("HQ_GLOBAL_INPUT_FILE.hqx");

		// create file writer
		try (BufferedWriter writer = Files.newBufferedWriter(inputFile, Charset.defaultCharset())) {

			// write header info
			writeHeader(writer);

			// write mix-single mission comparison inputs
			writeMixSingleComparisonInputs(writer);

			// write global inputs
			writeGlobalInputs(writer);

			// write missions
			writeMissions(writer);

			// write count ANA options
			writeCountANAOptions(writer);

			// write generate STH options
			writeGenerateSTHOptions(writer);

			// write plot options
			writePlotOptions(writer);

			// write rotation MLG codes
			writeRotationMLGCodes(writer);
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
	 * Writes mix-single mission comparison inputs.
	 *
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeMixSingleComparisonInputs(BufferedWriter writer) throws Exception {

		// only 1 mission given
		if (input_.getMissions().size() == 1)
			return;

		// get mix mission
		MyCheckMission mixMission = null;
		for (MyCheckMission mission : input_.getMissions()) {
			if (mission.getMissionType().equals(MissionType.MX)) {
				mixMission = mission;
				break;
			}
		}

		// no mix mission found
		if (mixMission == null)
			return;

		// get TXT and conversion table of mix mission
		Path mixTXTFileName = mixMission.getTXTFile().getFileName();
		if (mixTXTFileName == null)
			throw new Exception("Cannot get TXT file name for mix mission.");
		String missionCode = mixMission.getMissionType().getCode();
		Path missionDir = input_.getOutputDirectory().resolve(missionCode);
		Path mixTxt = missionDir.resolve(mixTXTFileName.toString());
		Path mixConv = missionDir.resolve(missionCode + "_CONVTABLE.log");

		// loop over missions
		for (MyCheckMission mission : input_.getMissions()) {

			// mix mission
			if (mission.getMissionType().equals(MissionType.MX)) {
				continue;
			}

			// get mission directory
			missionCode = mission.getMissionType().getCode();
			missionDir = input_.getOutputDirectory().resolve(missionCode);

			// get TXT file path
			Path txtFileName = mission.getTXTFile().getFileName();
			if (txtFileName == null)
				throw new Exception("Cannot get TXT file name.");
			Path txt = missionDir.resolve(txtFileName.toString());

			// write inputs
			writer.newLine();
			writer.write("BEGIN_GLOBAL");
			writer.newLine();
			writer.write("    MIX                =        " + mixTxt.toAbsolutePath().toString());
			writer.newLine();
			writer.write("    SINGLE             =        " + txt.toAbsolutePath().toString());
			writer.newLine();
			writer.write("    CONVTABLE_TXT      =        " + mixConv.toAbsolutePath().toString());
			writer.newLine();
			writer.write("END_TXT");
			writer.newLine();
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
		updateMessage("Running MyCheck " + VERSION + "...");

		// get path to perl script and perl executable
		Path script = Equinox.SCRIPTS_DIR.resolve("myCheck_v3.0.pl");

		// create process builder for windows
		ProcessBuilder pb = null;
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			Path perl = Equinox.SCRIPTS_DIR.resolve("perl").resolve("bin").resolve("perl.exe");
			pb = new ProcessBuilder(perl.toAbsolutePath().toString(), script.toAbsolutePath().toString());
		}

		// unsupported OS
		if (pb == null)
			throw new Exception("Unsupported operation system. MyCheck plugin supports only Windows operating system.");

		// execute process and wait to end
		Path workingDir = input_.getOutputDirectory();
		pb.directory(workingDir.toFile());
		File log = workingDir.resolve("myCheck_v3.0.log").toFile();
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		process_ = pb.start();
		assert pb.redirectInput() == Redirect.PIPE;
		assert pb.redirectOutput().file() == log;
		assert process_.getInputStream().read() == -1;

		// process failed
		if (process_.waitFor() != 0)
			throw new Exception("MyCheck failed! See log file '" + log.getAbsolutePath() + "'for details.");
	}

	/**
	 * Writes rotation and MLG codes to input file.
	 *
	 * @param writer
	 *            Input file writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeRotationMLGCodes(BufferedWriter writer) throws Exception {
		updateMessage("Writing rotation and MLG codes to input file...");
		writer.newLine();
		writer.write("BEGIN_THIS_AC_4_DIGIT_ROT_MLG_CODES");
		writer.newLine();
		writer.write("       ROT   = " + input_.getAircraftProgram().getRotationCodes());
		writer.newLine();
		writer.write("       MLG   = " + input_.getAircraftProgram().getMLGCodes());
		writer.newLine();
		writer.write("END CODES");
		writer.newLine();
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
		writer.write("# This is the input file for MyCheck " + VERSION + ", Spectrum Validation Plugin of Equinox");
		writer.newLine();
		writer.write("# This file is automatically generated by Equinox Version " + Equinox.VERSION.toString() + ", Date: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
		writer.newLine();
		writer.newLine();
	}

	/**
	 * Writes plot options.
	 *
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writePlotOptions(BufferedWriter writer) throws Exception {
		updateMessage("Writing plot options to input file...");
		boolean[] booleanOptions = input_.getPlotBooleans();
		int[] integerOptions = input_.getPlotIntegers();
		double[] doubleOptions = input_.getPlotDoubles();
		writer.newLine();
		writer.write("BEGIN_OPTIONS_PLOTS");
		writer.newLine();
		writer.write("      flights_per_page          = " + integerOptions[0]);
		writer.newLine();
		writer.write("      load_flights_up2num       = " + integerOptions[1]);
		writer.newLine();
		writer.write("      show_text_more            = " + (booleanOptions[0] ? "1" : "0"));
		writer.newLine();
		writer.write("      do_fixed_scale_x          = " + (booleanOptions[1] ? "YES" : "NO"));
		writer.newLine();
		writer.write("      force_scale_x_to          = " + format_.format(doubleOptions[0]));
		writer.newLine();
		writer.write("      do_fixed_scale_y          = " + (booleanOptions[2] ? "YES" : "NO"));
		writer.newLine();
		writer.write("      force_scale_y_to          = " + format_.format(doubleOptions[1]));
		writer.newLine();
		writer.write("      shift_Y_text_to           = " + integerOptions[2]);
		writer.newLine();
		writer.write("      neutral_axis_y_shift      = " + integerOptions[3]);
		writer.newLine();
		writer.write("END OPTIONS");
		writer.newLine();
	}

	/**
	 * Writes generate STH options.
	 *
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeGenerateSTHOptions(BufferedWriter writer) throws Exception {
		updateMessage("Writing generate STH options to input file...");
		boolean[] booleanOptions = input_.getGenerateSTHBooleans();
		double[] doubleOptions = input_.getGenerateSTHDoubles();
		int runTill = input_.getRunTillFlightSTH();
		writer.newLine();
		writer.write("BEGIN_OPTIONS_GENERATE_STH");
		writer.newLine();
		writer.write("        add2sth_Delta_P               = " + (booleanOptions[4] ? "1" : "0"));
		writer.newLine();
		writer.write("        reference_Delta_P             = " + format_.format(doubleOptions[0]));
		writer.newLine();
		writer.write("        reference_Delta_T             = " + format_.format(doubleOptions[4]));
		writer.newLine();
		writer.write("        overwrite_loadfile            = " + checkOverrideLoad());
		writer.newLine();
		writer.write("        enable_SLOG_mode              = " + (booleanOptions[0] ? "1" : "0"));
		writer.newLine();
		writer.write("        esg_on                        = " + (booleanOptions[1] ? "1" : "0"));
		writer.newLine();
		int column = 1;
		StressComponent comp = input_.getStressComponent();
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
		writer.write("        rotation_degrees              = " + (column > 0 ? "0" : format_.format(doubleOptions[3])));
		writer.newLine();
		writer.write("        use_stress_column             = " + column);
		writer.newLine();
		writer.write("        set_it_zero                   = " + (booleanOptions[3] ? "YES" : "NO"));
		writer.newLine();
		writer.write("        fatigue_fac_all               = " + format_.format(doubleOptions[2]));
		writer.newLine();
		writer.write("        delta_p_fac_only              = " + format_.format(doubleOptions[1]));
		writer.newLine();
		writer.write("        high_accuracy_of_results      = 8");
		writer.newLine();
		writer.write("        warn_if_li_nl_cases_comb      = " + (booleanOptions[2] ? "1" : "0"));
		writer.newLine();
		writer.write("        run_only_till_flight_num      = " + (runTill == -1 ? "1e6" : runTill));
		writer.newLine();
		writer.write("END OPTIONS");
		writer.newLine();
	}

	/**
	 * Returns 1 if STF files should be overridden.
	 *
	 * @return 1 if STF files should be overridden.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String checkOverrideLoad() throws Exception {
		for (MyCheckMission mission : input_.getMissions()) {
			if (mission.getSTFFile() == null)
				return "1";
		}
		return "0";
	}

	/**
	 * Writes count ANA options.
	 *
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeCountANAOptions(BufferedWriter writer) throws Exception {
		updateMessage("Writing count ANA options to input file...");
		boolean[] booleanOptions = input_.getCountANABooleans();
		int[] integerOptions = input_.getCountANAIntegers();
		writer.newLine();
		writer.write("BEGIN_OPTIONS_COUNT_ANA");
		writer.newLine();
		writer.write("        count_flight_separate       = " + (booleanOptions[6] ? "1" : "0"));
		writer.newLine();
		writer.write("        max_pointsper_flight        = " + integerOptions[0]);
		writer.newLine();
		writer.write("        esg_switch                  = " + (booleanOptions[0] ? "1" : "0"));
		writer.newLine();
		writer.write("        cvt_factor_warn             = " + integerOptions[2]);
		writer.newLine();
		writer.write("        check_round_the_clock       = " + (booleanOptions[1] ? "1" : "0"));
		writer.newLine();
		writer.write("        check_4_return_2_1g         = " + (booleanOptions[5] ? "1" : "0"));
		writer.newLine();
		writer.write("        print_special_fsftest       = " + (booleanOptions[2] ? "1" : "0"));
		writer.newLine();
		writer.write("        print_factors_in_log        = " + (booleanOptions[3] ? "1" : "0"));
		writer.newLine();
		writer.write("        run_only_till_flight_num    = " + (integerOptions[1] == -1 ? "1e6" : integerOptions[1]));
		writer.newLine();
		writer.write("        write_all_1g_sequence       = " + (booleanOptions[4] ? "1" : "0"));
		writer.newLine();
		writer.write("END OPTIONS");
		writer.newLine();
	}

	/**
	 * Writes missions to input file.
	 *
	 * @param writer
	 *            Input file writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeMissions(BufferedWriter writer) throws Exception {

		// progress info
		updateMessage("Writing mission information to input file...");

		// loop over missions
		for (MyCheckMission mission : input_.getMissions()) {

			// get file names
			Path txtFileName = mission.getTXTFile().getFileName();
			Path cvtFileName = mission.getCVTFile().getFileName();
			if (txtFileName == null || cvtFileName == null)
				throw new Exception("Cannot get CDF set file name.");

			// create mission directory
			String missionCode = mission.getMissionType().getCode();
			Path missionDir = Files.createDirectory(input_.getOutputDirectory().resolve(missionCode));

			// copy mission files
			Path ana = extractANAFile(mission.getANAFile(), missionDir);
			Path txt = Files.copy(mission.getTXTFile(), missionDir.resolve(txtFileName.toString()), StandardCopyOption.REPLACE_EXISTING);
			Path cvt = Files.copy(mission.getCVTFile(), missionDir.resolve(cvtFileName.toString()), StandardCopyOption.REPLACE_EXISTING);
			Path stf = mission.getSTFFile();
			if (stf != null) {

				// get STF file name
				Path stfFileName = stf.getFileName();
				if (stfFileName == null)
					throw new Exception("Cannot get STF file name.");

				// copy file
				stf = Files.copy(stf, missionDir.resolve(stfFileName.toString()), StandardCopyOption.REPLACE_EXISTING);
			}
			else {
				stf = missionDir.resolve(missionCode + ".stf");
			}
			Path convTable = extractConversionTableSheet(missionCode, missionDir, mission.getConversionTableFile(), mission.getConversionTableWorksheet());

			// write mission
			writer.newLine();
			writer.write("BEGIN_MISSION");
			writer.newLine();
			writer.write("   MISSION            =           " + missionCode);
			writer.newLine();
			writer.write("   ANA_FILE           =           " + ana.toAbsolutePath().toString());
			writer.newLine();
			writer.write("   TXT_FILE           =           " + txt.toAbsolutePath().toString());
			writer.newLine();
			writer.write("   CVT_FILE           =           " + cvt.toAbsolutePath().toString());
			writer.newLine();
			writer.write("   STF_FILE           =           " + stf.toAbsolutePath().toString());
			writer.newLine();
			writer.write("   CONVTABLE_TXT      =           " + convTable.toAbsolutePath().toString());
			writer.newLine();
			writer.write("END_MISSION");
			writer.newLine();
		}
	}

	/**
	 * Extracts the ANA file into mission directory.
	 *
	 * @param ana
	 *            ANA file to extract.
	 * @param missionDir
	 *            Mission directory.
	 * @return Extracted ANA file in the mission directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path extractANAFile(Path ana, Path missionDir) throws Exception {

		// initialize input file and type
		Path anaFile = null;
		FileType type = FileType.getFileType(ana.toFile());

		// input file is a ZIP file
		if (type.equals(FileType.ZIP)) {

			// extract ANA file
			updateMessage("Extracting zipped ANA file...");
			anaFile = Utility.extractFileFromZIP(ana, this, FileType.ANA, null);

			// get ANA file name
			Path anaFileName = anaFile.getFileName();
			if (anaFileName == null)
				throw new Exception("Cannot get ANA file name.");

			// copy file
			anaFile = Files.copy(anaFile, missionDir.resolve(anaFileName.toString()), StandardCopyOption.REPLACE_EXISTING);
		}

		// input file is a GZIP file
		else if (type.equals(FileType.GZ)) {
			anaFile = missionDir.resolve(FileType.appendExtension(FileType.getNameWithoutExtension(ana), FileType.ANA));
			updateMessage("Extracting zipped ANA file...");
			Utility.extractFileFromGZIP(ana, anaFile);
		}

		// ANA file
		else if (type.equals(FileType.ANA)) {

			// get ANA file name
			Path anaFileName = ana.getFileName();
			if (anaFileName == null)
				throw new Exception("Cannot get ANA file name.");

			// copy file
			anaFile = Files.copy(ana, missionDir.resolve(anaFileName.toString()), StandardCopyOption.REPLACE_EXISTING);
		}

		// return ANA file
		return anaFile;
	}

	/**
	 * Extracts conversion table sheet and writes it to a text file.
	 *
	 * @param missionCode
	 *            Mission code.
	 * @param missionDir
	 *            Mission directory.
	 * @param conversionTable
	 *            Path to conversion table.
	 * @param sheetName
	 *            Sheet name.
	 * @return path to extracted sheet.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Path extractConversionTableSheet(String missionCode, Path missionDir, Path conversionTable, String sheetName) throws Exception {

		// initialize variables
		Workbook workbook = null;

		try {

			// create path to output file
			Path outputFile = missionDir.resolve(missionCode + "_CONVTABLE.log");

			// get workbook
			workbook = Workbook.getWorkbook(conversionTable.toFile());

			// get sheet
			Sheet sheet = workbook.getSheet(sheetName);

			// create file writer
			try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {

				// set location
				int startRow = 7, endRow = sheet.getRows() - 1;

				// loop over rows
				for (int i = startRow; i <= endRow; i++) {

					// get issy code
					String issyCode = sheet.getCell(3, i).getContents();

					// no issy code found
					if (issyCode == null || issyCode.trim().isEmpty()) {
						continue;
					}

					// get flight phase
					String flightPhase = sheet.getCell(4, i).getContents();

					// no issy code found
					if (flightPhase == null || flightPhase.trim().isEmpty()) {
						continue;
					}

					// get event
					String event = sheet.getCell(5, i).getContents();

					// no issy code found
					if (event == null || event.trim().isEmpty()) {
						continue;
					}

					// write
					writer.write(issyCode + "     " + flightPhase + "     " + event);
					writer.newLine();
				}
			}

			// return output file
			return outputFile;
		}

		// close workbook
		finally {
			if (workbook != null) {
				workbook.close();
			}
		}
	}

	/**
	 * Writes global inputs to input file.
	 *
	 * @param writer
	 *            Input file writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeGlobalInputs(BufferedWriter writer) throws Exception {

		// get conversion table file name
		Path convTableFileName = input_.getMissions().get(0).getConversionTableFile().getFileName();
		if (convTableFileName == null)
			throw new Exception("Cannot get conversion table file name.");

		// write inputs
		updateMessage("Writing global inputs to input file...");
		writer.newLine();
		writer.write("BEGIN_GLOBAL");
		writer.newLine();
		writer.write("   AIRCRAFT_TYPE      =           " + input_.getAircraftProgram().toString());
		writer.newLine();
		writer.write("   GLOBAL_LOGFILE     =           " + input_.getOutputDirectory().resolve("HQ_global_outputLogFile.log").toAbsolutePath().toString());
		writer.newLine();
		writer.write("   CONV_TABLE_FILE    =           " + convTableFileName.toString());
		writer.newLine();
		writer.write("   DATE               =           " + new SimpleDateFormat("dd_MM_yyyy").format(new Date()));
		writer.newLine();
		writer.write("   WORKING_DIRECTORY  =           " + input_.getOutputDirectory().toAbsolutePath().toString());
		writer.newLine();
		writer.write("END_GLOBAL");
		writer.newLine();
	}
}
