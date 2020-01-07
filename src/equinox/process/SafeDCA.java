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
import equinox.data.DamageContribution;
import equinox.data.DamageContributionResult;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.task.LoadcaseDamageContributionAnalysis;

/**
 * Class for SAFE damage contribution analysis.
 *
 * @author Murat Artim
 * @date Apr 4, 2015
 * @time 1:12:14 AM
 */
public class SafeDCA implements ESAProcess<List<DamageContributionResult>> {

	/** The owner task of this process. */
	private final LoadcaseDamageContributionAnalysis task_;

	/** Paths to input STH files. */
	private final Path[] sthFiles_;

	/** Path to input FLS file. */
	private final Path flsFile_;

	/** Damage contributions. */
	private final ArrayList<DamageContribution> contributions_;

	/** Material. */
	private final FatigueMaterial material_;

	/** True to apply omission. */
	private final boolean applyOmission_;

	/** Omission level. */
	private final double omissionLevel_;

	/** Increment tasks. */
	private ArrayList<SafeDCAIncrement> incrementTasks_;

	/**
	 * Creates server equivalent stress analysis process for damage angle analysis.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param sthFiles
	 *            Paths to input STH files.
	 * @param flsFile
	 *            Path to input FLS file.
	 * @param contributions
	 *            Damage contributions.
	 * @param material
	 *            Material.
	 * @param applyOmission
	 *            True if omission should be applied.
	 * @param omissionLevel
	 *            Omission level.
	 */
	public SafeDCA(LoadcaseDamageContributionAnalysis task, Path[] sthFiles, Path flsFile, ArrayList<DamageContribution> contributions, FatigueMaterial material, boolean applyOmission, double omissionLevel) {
		task_ = task;
		sthFiles_ = sthFiles;
		flsFile_ = flsFile;
		contributions_ = contributions;
		material_ = material;
		applyOmission_ = applyOmission;
		omissionLevel_ = omissionLevel;
	}

	@Override
	public List<DamageContributionResult> start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// initialize output array
		List<DamageContributionResult> list = new ArrayList<>();

		// create incremental sub tasks
		incrementTasks_ = new ArrayList<>();
		for (int i = 0; i < contributions_.size() + 1; i++) {
			String name = i == 0 ? "full" : contributions_.get(i - 1).getName();
			incrementTasks_.add(new SafeDCAIncrement(task_, sthFiles_[i], flsFile_, name, i, material_, applyOmission_, omissionLevel_));
		}

		// disable task canceling
		task_.getTaskPanel().updateCancelState(false);

		// execute all tasks and wait to complete
		task_.updateProgress(-1, 100);
		List<Future<DamageContributionResult>> results = Equinox.SUBTASK_THREADPOOL.invokeAll(incrementTasks_);

		// enable task canceling
		task_.getTaskPanel().updateCancelState(true);

		// task cancelled
		if (task_.isCancelled())
			return null;

		// loop over results
		for (Future<DamageContributionResult> result : results) {

			// get result
			try {
				list.add(result.get());
			}

			// exception occurred
			catch (Exception e) {
				// ignore since it is handled within the sub task
			}
		}

		// return stresses
		return list;
	}

	@Override
	public void cancel() {

		// cancel incremental tasks
		if (incrementTasks_ != null && !incrementTasks_.isEmpty()) {
			for (SafeDCAIncrement increment : incrementTasks_) {
				increment.cancel();
			}
		}
	}
}
