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

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import equinox.Equinox;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.task.DamageAngleAnalysis;

/**
 * Class for SAFE damage angle analysis.
 *
 * @author Murat Artim
 * @date Feb 6, 2015
 * @time 11:24:11 AM
 */
public class SafeDAA implements ESAProcess<Double[][]> {

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

	/** Increment tasks. */
	private ArrayList<SafeDAAIncrement> incrementTasks_;

	/** True to apply omission. */
	private final boolean applyOmission_;

	/** Omission level. */
	private final double omissionLevel_;

	/**
	 * Creates SAFE damage angle analysis.
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
	 * @param applyOmission
	 *            True if omission should be applied.
	 * @param omissionLevel
	 *            Omission level.
	 */
	public SafeDAA(DamageAngleAnalysis task, Path[] sthFiles, Path flsFile, int[] incAngles, FatigueMaterial material, boolean applyOmission, double omissionLevel) {
		task_ = task;
		sthFiles_ = sthFiles;
		flsFile_ = flsFile;
		incAngles_ = incAngles;
		material_ = material;
		applyOmission_ = applyOmission;
		omissionLevel_ = omissionLevel;
	}

	@Override
	public Double[][] start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// initialize output array
		Double[][] outputs = new Double[incAngles_.length][];

		// create incremental sub tasks
		incrementTasks_ = new ArrayList<>();
		for (int i = 0; i < incAngles_.length; i++) {
			incrementTasks_.add(new SafeDAAIncrement(task_, sthFiles_[i], flsFile_, incAngles_[i], i, material_, applyOmission_, omissionLevel_));
		}

		// disable task canceling
		task_.getTaskPanel().updateCancelState(false);

		// execute all tasks and wait to complete
		task_.updateProgress(-1, 100);
		List<Future<Double[]>> results = Equinox.SUBTASK_THREADPOOL.invokeAll(incrementTasks_);

		// enable task canceling
		task_.getTaskPanel().updateCancelState(true);

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
			for (SafeDAAIncrement increment : incrementTasks_) {
				increment.cancel();
			}
		}
	}
}
