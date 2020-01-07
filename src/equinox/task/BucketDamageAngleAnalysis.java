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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.concurrent.Future;

import equinox.Equinox;
import equinox.data.AnalysisEngine;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.ProgramArguments.ArgumentType;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.Spectrum;
import equinox.data.input.DamageAngleInput;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableBucketDamageAngleAnalysis;

/**
 * Class for bucket damage angle analysis task.
 *
 * @author Murat Artim
 * @date 30 Aug 2016
 * @time 14:38:16
 */
public class BucketDamageAngleAnalysis extends InternalEquinoxTask<Void> implements SavableTask, LongRunningTask, BucketTask {

	/** The owner STF file bucket. */
	private final STFFileBucket bucket_;

	/** Input. */
	private final DamageAngleInput input_;

	/** Material. */
	private final FatigueMaterial material_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/** ISAMI version. */
	private IsamiVersion isamiVersion_;

	/** ISAMI sub version. */
	private IsamiSubVersion isamiSubVersion_;

	/** True compression should be applied in propagation analysis. */
	private boolean applyCompression_;

	/** Number of completed analyses. */
	private int completed_ = 0;

	/**
	 * Creates bucket damage angle analysis task.
	 *
	 * @param bucket
	 *            STF file bucket.
	 * @param input
	 *            Analysis input.
	 * @param material
	 *            Material.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public BucketDamageAngleAnalysis(STFFileBucket bucket, DamageAngleInput input, FatigueMaterial material, AnalysisEngine analysisEngine) {
		bucket_ = bucket;
		input_ = input;
		material_ = material;
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
	public BucketDamageAngleAnalysis setIsamiEngineInputs(IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
		return this;
	}

	@Override
	public String getTaskTitle() {
		return "Overall damage angle analysis for spectrum '" + bucket_.getParentItem().getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableBucketDamageAngleAnalysis(bucket_, input_, material_, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.DAMAGE_ANGLE_ANALYSIS);

		// update message
		updateMessage("Running analyses...");

		// get number of parallel processes
		int maxParallel = Integer.parseInt(Equinox.ARGUMENTS.getArgument(ArgumentType.MAX_PARALLEL_TASKS));

		// get owner spectrum
		Spectrum spectrum = bucket_.getParentItem();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement for getting STF files incrementally
			String sql = "select file_id, stress_table_id, name from stf_files where cdf_id = " + spectrum.getID();
			sql += " and is_2d = 1 and file_id > ? order by file_id asc";
			try (PreparedStatement statement = connection.prepareStatement(sql)) {

				// set limit to rows returned
				statement.setMaxRows(maxParallel);

				// execute as long as all tasks are completed
				int fileID = 0;
				while (fileID >= 0) {

					// task cancelled
					if (isCancelled()) {
						break;
					}

					// execute
					statement.setInt(1, fileID);
					fileID = executeTasks(statement, spectrum);
				}

				// reset statement
				statement.setMaxRows(0);
			}
		}

		// return
		return null;
	}

	/**
	 * Executes tasks and waits for all of them to complete.
	 *
	 * @param statement
	 *            Database statement.
	 * @param spectrum
	 *            Spectrum.
	 * @return Maximum STF file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int executeTasks(PreparedStatement statement, Spectrum spectrum) throws Exception {

		// initialize variables
		int maxFileID = -1;
		ArrayList<Future<?>> results = null;

		// get next STF files
		try (ResultSet resultSet = statement.executeQuery()) {

			// loop over STF files
			while (resultSet.next()) {

				// task cancelled
				if (isCancelled()) {
					break;
				}

				// get STF file info
				int stfID = resultSet.getInt("file_id");
				int stressTableID = resultSet.getInt("stress_table_id");
				String stfName = resultSet.getString("name");

				// execute task silently and in parallel
				if (results == null) {
					results = new ArrayList<>();
				}
				results.add(taskPanel_.getOwner().runTaskSilently(new DamageAngleAnalysis(stfID, stressTableID, stfName, spectrum, input_, material_, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_), false));

				// update maximum file ID
				if (stfID >= maxFileID) {
					maxFileID = stfID;
				}
			}
		}

		// there are results
		if (results != null) {

			// loop over results
			for (Future<?> result : results) {

				// task cancelled
				if (isCancelled()) {
					result.cancel(false);
				}
				else {
					try {
						result.get();
						completed_++;
						updateProgress(completed_, bucket_.getNumberOfSTFs());
					}

					// exception occurred (ignore since it is handled within the task)
					catch (Exception e) {
						completed_++;
						updateProgress(completed_, bucket_.getNumberOfSTFs());
					}
				}
			}
		}

		// return maximum file ID
		return maxFileID;
	}
}
