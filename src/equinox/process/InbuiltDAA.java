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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import equinox.Equinox;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.task.DamageAngleAnalysis;

/**
 * Class for inbuilt damage angle analysis.
 *
 * @author Murat Artim
 * @date Feb 6, 2015
 * @time 1:18:58 PM
 */
public class InbuiltDAA implements ESAProcess<Double[][]> {

	/** The owner task of this process. */
	private final DamageAngleAnalysis task_;

	/** Paths to input STH files. */
	private final Path[] sthFiles_;

	/** Path to input FLS file. */
	private final Path flsFile_;

	/** Increment angles. */
	private final int[] incAngles_;

	/** Material. */
	private final FatigueMaterial material_;

	/** Spectrum validity. */
	private final int validity_;

	/** Increment tasks. */
	private ArrayList<InbuiltDAAIncrement> incrementTasks_;

	/** Decimal formats. */
	private final DecimalFormat format_ = new DecimalFormat("0.##");

	/** True to use extended inbuilt analysis engine. Extended inbuilt engine has no limit on number of peaks per typical flight. */
	private final boolean useExtended_, applyOmission_;

	/** Omission level. */
	private final double omissionLevel_;

	/**
	 * Creates inbuilt damage angle analysis.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param sthFiles
	 *            Paths to input STH files.
	 * @param flsFile
	 *            Path to input FLS file.
	 * @param incAngles
	 *            Increment angles.
	 * @param material
	 *            Material.
	 * @param validity
	 *            Spectrum validity.
	 * @param useExtended
	 *            True to use extended inbuilt analysis engine. Extended inbuilt engine has no limit on number of peaks per typical flight.
	 * @param applyOmission
	 *            True if omission should be applied.
	 * @param omissionLevel
	 *            Omission level.
	 */
	public InbuiltDAA(DamageAngleAnalysis task, Path[] sthFiles, Path flsFile, int[] incAngles, FatigueMaterial material, int validity, boolean useExtended, boolean applyOmission, double omissionLevel) {
		task_ = task;
		sthFiles_ = sthFiles;
		flsFile_ = flsFile;
		incAngles_ = incAngles;
		material_ = material;
		validity_ = validity;
		useExtended_ = useExtended;
		applyOmission_ = applyOmission;
		omissionLevel_ = omissionLevel;
	}

	@Override
	public Double[][] start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// write input material file
		Path materialFile = writeMaterialFile();

		// task cancelled
		if (task_.isCancelled())
			return null;

		// initialize output array
		Double[][] outputs = new Double[incAngles_.length][];

		// create incremental sub tasks
		incrementTasks_ = new ArrayList<>();
		for (int i = 0; i < incAngles_.length; i++) {
			incrementTasks_.add(new InbuiltDAAIncrement(task_, sthFiles_[i], flsFile_, materialFile, incAngles_[i], i, validity_, useExtended_, applyOmission_, omissionLevel_));
		}

		// execute all tasks and wait to complete
		task_.updateProgress(-1, 100);
		List<Future<Double[]>> results = Equinox.SUBTASK_THREADPOOL.invokeAll(incrementTasks_);

		// task cancelled
		if (task_.isCancelled())
			return null;

		// loop over results
		for (Future<Double[]> result : results) {

			// get result
			try {
				Double[] output = result.get();
				outputs[output[DamageAngleAnalysis.ANGLE_INDEX].intValue()] = output;
			}

			// exception occurred
			catch (Exception e) {
				// ignore since it is handled within the sub task
			}
		}

		// return stresses
		return outputs;
	}

	@Override
	public void cancel() {

		// cancel incremental tasks
		if (incrementTasks_ != null && !incrementTasks_.isEmpty()) {
			for (InbuiltDAAIncrement increment : incrementTasks_) {
				increment.cancel();
			}
		}
	}

	/**
	 * Writes out material input file.
	 *
	 * @return Path to material input file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeMaterialFile() throws Exception {

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

				// read file till the end
				String line;
				while ((line = reader.readLine()) != null) {

					// task cancelled
					if (task_.isCancelled())
						return null;

					// fatigue material slope p
					if (line.contains("%MANP")) {
						writer.write("ABREMOD \"%MANP\" \"" + format_.format(-material_.getP()) + "\" ! PENTE DE LA LOI");
						writer.newLine();
					}

					// fatigue material coefficient q
					else if (line.contains("%MANQ")) {
						writer.write("ABREMOD \"%MANQ\" \"" + format_.format(material_.getQ()) + "\" ! COEFFICIENT f(R)");
						writer.newLine();
					}

					// fatigue material coefficient M
					else if (line.contains("%MANM")) {
						writer.write("ABREMOD \"%MANM\" \"" + format_.format(material_.getM()) + "\" ! M INFLUENCE DU MATERIAU");
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

		// return material file
		return materialFile;
	}
}
