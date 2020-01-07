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
import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;

import equinox.Equinox;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.plugin.FileType;
import equinox.serverUtilities.ServerUtility;
import equinox.task.TemporaryFileCreatingTask;
import equinox.utility.exception.InternalEngineAnalysisFailedException;

/**
 * Class for rainflow process.
 *
 * @author Murat Artim
 * @date Jan 30, 2015
 * @time 3:16:23 PM
 */
public class Rainflow implements ESAProcess<Void> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** Path to input STH file. */
	private final Path inputSTH_;

	/** Paths to output files. */
	private File[] outputFiles_ = null;

	/** Equivalent stress ID. */
	private final SpectrumItem equivalentStress_;

	/** Rainflow cycles table name. */
	private final String tableName_;

	/** Sub processes. */
	private Process rainflowProcess_;

	/**
	 * Creates rainflow process.
	 *
	 * @param task
	 *            The owner task.
	 * @param inputSTH
	 *            Path to input STH file.
	 * @param equivalentStress
	 *            Equivalent stress.
	 */
	public Rainflow(TemporaryFileCreatingTask<?> task, Path inputSTH, SpectrumItem equivalentStress) {
		task_ = task;
		inputSTH_ = inputSTH;
		equivalentStress_ = equivalentStress;
		tableName_ = null;
	}

	/**
	 * Creates rainflow process.
	 *
	 * @param task
	 *            The owner task.
	 * @param inputSTH
	 *            Path to input STH file.
	 * @param equivalentStress
	 *            Equivalent stress.
	 * @param tableName
	 *            Rainflow cycles table name.
	 */
	public Rainflow(TemporaryFileCreatingTask<?> task, Path inputSTH, SpectrumItem equivalentStress, String tableName) {
		task_ = task;
		inputSTH_ = inputSTH;
		equivalentStress_ = equivalentStress;
		tableName_ = tableName;
	}

	@Override
	public Void start(Connection connection, PreparedStatement... preparedStatements) throws InternalEngineAnalysisFailedException {

		try {

			// run rainflow process
			Path rainflowOutput = runRainflowProcess();

			// task cancelled
			if (task_.isCancelled())
				return null;

			// save rainflow cycles to database
			saveRainflowCycles(rainflowOutput, connection);

			// return
			return null;
		}

		// analysis failed
		catch (Exception e) {

			// set output files as permanent
			if (outputFiles_ != null) {
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
		if (rainflowProcess_ != null && rainflowProcess_.isAlive()) {
			rainflowProcess_.destroyForcibly();
		}
	}

	/**
	 * Runs rainflow process.
	 *
	 * @return Path to rainflow output file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path runRainflowProcess() throws Exception {

		// get path to input file name
		Path inputFileNamePath = inputSTH_.getFileName();
		if (inputFileNamePath == null)
			throw new Exception("Cannot get input STH file name.");

		// update info
		String inputFileName = inputFileNamePath.toString();
		task_.updateMessage("Rainflowing stress sequence '" + inputFileName + "'...");

		// get path to perl script and perl executable
		Path script = Equinox.SCRIPTS_DIR.resolve("rainflow.pl");

		// create process builder for windows
		ProcessBuilder pb = null;
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS)) {
			Path perl = Equinox.SCRIPTS_DIR.resolve("perl").resolve("bin").resolve("perl.exe");
			pb = new ProcessBuilder(perl.toAbsolutePath().toString(), script.toAbsolutePath().toString(), inputFileName, "DATIG");
		}

		// create process builder for macOSX
		else if (Equinox.OS_TYPE.equals(ServerUtility.MACOS)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), inputFileName, "DATIG");
		}
		else if (Equinox.OS_TYPE.equals(ServerUtility.LINUX)) {
			pb = new ProcessBuilder("perl", script.toAbsolutePath().toString(), inputFileName, "DATIG");
		}

		// unsupported OS
		if (pb == null)
			throw new Exception("Unsupported operating system.");

		// set output file paths
		Path workingDir = task_.getWorkingDirectory();
		File log = workingDir.resolve("rainflow.log").toFile();
		Path output = Paths.get(inputSTH_.toString() + FileType.RFLOW.getExtension());
		outputFiles_ = new File[] { log, output.toFile() };

		// execute process and wait to end
		pb.directory(workingDir.toFile());
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		rainflowProcess_ = pb.start();
		assert pb.redirectInput() == Redirect.PIPE;
		assert pb.redirectOutput().file() == log;
		assert rainflowProcess_.getInputStream().read() == -1;

		// task cancelled
		if (task_.isCancelled())
			return null;

		// process failed
		if (rainflowProcess_.waitFor() != 0)
			throw new Exception("Rainflow failed! See LOG file for details.");

		// task cancelled
		if (task_.isCancelled())
			return null;

		// output file doesn't exist
		if (!Files.exists(output))
			throw new Exception("Rainflow failed! See LOG file for details.");

		// return output file
		return output;
	}

	/**
	 * Saves rainflow cycles to database.
	 *
	 * @param rainflowOutput
	 *            Rainflow output file.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveRainflowCycles(Path rainflowOutput, Connection connection) throws Exception {

		// update info
		task_.updateMessage("Saving rainflow cycles to database...");

		// set table name
		String tableName = null;
		if (tableName_ == null) {
			if (equivalentStress_ instanceof FatigueEquivalentStress) {
				tableName = "fatigue_rainflow_cycles";
			}
			else if (equivalentStress_ instanceof PreffasEquivalentStress) {
				tableName = "preffas_rainflow_cycles";
			}
			else if (equivalentStress_ instanceof LinearEquivalentStress) {
				tableName = "linear_rainflow_cycles";
			}
			else if (equivalentStress_ instanceof ExternalFatigueEquivalentStress) {
				tableName = "ext_fatigue_rainflow_cycles";
			}
			else if (equivalentStress_ instanceof ExternalPreffasEquivalentStress) {
				tableName = "ext_preffas_rainflow_cycles";
			}
			else if (equivalentStress_ instanceof ExternalLinearEquivalentStress) {
				tableName = "ext_linear_rainflow_cycles";
			}
		}
		else {
			tableName = tableName_;
		}

		// prepare statement
		String sql = "insert into " + tableName + "(stress_id, cycle_num, num_cycles, max_val, min_val, mean_val, r_ratio, amp_val, range_val) values(?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql)) {

			// set stress ID
			update.setInt(1, equivalentStress_.getID());

			// create file reader
			try (BufferedReader reader = Files.newBufferedReader(rainflowOutput, Charset.defaultCharset())) {

				// read file till the end
				String line;
				int cycleNum = 0;
				while ((line = reader.readLine()) != null) {

					// task cancelled
					if (task_.isCancelled())
						return;

					// end line
					if (line.startsWith("         1")) {
						break;
					}

					// set cycle number
					update.setInt(2, cycleNum);
					cycleNum++;

					// split line
					String[] split = line.trim().split(" ");

					// loop over columns
					int index = 0;
					double max = 0.0, min = 0.0, cycles = 0.0;
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

						// number of cycles
						if (index == 0) {
							cycles = Double.parseDouble(col);
							update.setDouble(3, cycles);
						}

						// max value
						else if (index == 1) {

							// get maximum value
							max = Double.parseDouble(col);

							// zero maximum value
							if (max == 0.0) {

								// set maximum to 0.0001
								max = 0.0001;

								// log warning
								String warning = task_.getTaskTitle() + " produced 0 maximum stress. Maximum stress is set to 0.0001.";
								task_.addWarning(warning);
								Equinox.LOGGER.warning(warning);
							}

							// set maximum value
							update.setDouble(4, max);
						}

						// min value
						else if (index == 2) {

							// set min stress
							min = Double.parseDouble(col);
							update.setDouble(5, min);

							// set mean value, r-ratio, amplitude and range
							update.setDouble(6, (max + min) / 2.0);
							update.setDouble(7, min / max);
							update.setDouble(8, (max - min) / 2.0);
							update.setDouble(9, max - min);
						}

						// increment index
						index++;
					}

					// execute update
					update.executeUpdate();
				}
			}
		}
	}
}
