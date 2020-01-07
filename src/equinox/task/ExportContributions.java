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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.SpectrumItem;
import equinox.serverUtilities.Permission;
import equinox.task.GetContributionNames.DamageContributionRequester;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;

/**
 * Class for export damage contributions task.
 *
 * @author Murat Artim
 * @date 20 Aug 2017
 * @time 22:10:53
 *
 */
public class ExportContributions extends InternalEquinoxTask<Path> implements LongRunningTask, DamageContributionRequester, MultipleInputTask<SpectrumItem>, AutomaticTaskOwner<Path> {

	/** Output file. */
	private final File output_;

	/** Spectrum items. This can be either damage contributions or STF file buckets. */
	private final List<SpectrumItem> items_;

	/** Get contribution names completion indicator. */
	private final AtomicBoolean getNamesCompleted_;

	/** Contribution names. */
	private final AtomicReference<List<String>> contributionNames_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates export damage contributions task.
	 *
	 * @param output
	 *            Output file path.
	 * @param items
	 *            Spectrum items. This can be either damage contributions or STF file buckets. This can be null for automatic execution.
	 */
	public ExportContributions(File output, List<SpectrumItem> items) {
		output_ = output;
		items_ = items == null ? Collections.synchronizedList(new ArrayList<>()) : items;
		getNamesCompleted_ = new AtomicBoolean();
		contributionNames_ = new AtomicReference<>(null);
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Path> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Path>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	synchronized public void setInputThreshold(int inputThreshold) {
		inputThreshold_ = inputThreshold;
	}

	@Override
	synchronized public void addAutomaticInput(AutomaticTaskOwner<SpectrumItem> task, SpectrumItem input, ExecutionMode mode) {
		automaticInputAdded(task, input, mode, items_, inputThreshold_);
	}

	@Override
	synchronized public void inputFailed(AutomaticTaskOwner<SpectrumItem> task, ExecutionMode mode) {
		inputThreshold_ = automaticInputFailed(task, mode, items_, inputThreshold_);
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Export damage contributions";
	}

	@Override
	public void setContributions(List<String> contributions) {
		contributionNames_.set(contributions);
		getNamesCompleted_.set(true);
	}

	@Override
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.EXPORT_DAMAGE_CONTRIBUTIONS);

		// update info
		updateMessage("Getting damage contribution names...");

		// get contribution names and wait
		Equinox.SUBTASK_THREADPOOL.submit(new GetContributionNames(this, items_));
		waitForTask();

		// get names
		List<String> names = contributionNames_.get();

		// couldn't get contribution names
		if (names == null || names.isEmpty())
			throw new Exception("Couldn't get common contribution names. Aborting export process.");

		// create options
		boolean[] options = new boolean[17];
		options[SaveDamageContributions.PERCENT] = true;
		options[SaveDamageContributions.FULL] = false;
		options[SaveDamageContributions.INC] = true;
		options[SaveDamageContributions.ONEG] = true;
		options[SaveDamageContributions.GAG] = true;
		options[SaveDamageContributions.DP] = true;
		options[SaveDamageContributions.DT] = true;
		options[SaveDamageContributions.MAT_NAME] = false;
		options[SaveDamageContributions.FAT_P] = false;
		options[SaveDamageContributions.FAT_Q] = false;
		options[SaveDamageContributions.PP_NAME] = true;
		options[SaveDamageContributions.EID] = false;
		options[SaveDamageContributions.SPEC_NAME] = true;
		options[SaveDamageContributions.PROGRAM] = true;
		options[SaveDamageContributions.SECTION] = true;
		options[SaveDamageContributions.MISSION] = true;
		options[SaveDamageContributions.OMISSION] = false;

		// initialize task
		InternalEquinoxTask<?> task = null;

		// check if bucket STF files are given as input
		boolean isBucketSTF = false;
		for (SpectrumItem item : items_) {
			if (item instanceof STFFileBucket) {
				isBucketSTF = true;
				break;
			}
		}

		// STF file bucket
		if (isBucketSTF) {

			// cast to STF file bucket array
			ArrayList<STFFileBucket> buckets = new ArrayList<>();
			for (SpectrumItem item : items_) {
				buckets.add((STFFileBucket) item);
			}

			// create save task
			task = new SaveBucketDamageContributions(buckets, names, options, output_);
		}

		// loadcase damage contributions
		else {

			// cast to loadcase damage contributions
			ArrayList<SpectrumItem> contributions = new ArrayList<>();
			for (SpectrumItem item : items_) {
				contributions.add(item);
			}

			// create save task
			task = new SaveDamageContributions(contributions, names, options, output_);
		}

		// update info
		updateMessage("Saving damage contributions...");

		// submit task
		Equinox.SUBTASK_THREADPOOL.submit(task).get();

		// return
		return output_.toPath();
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// no automatic task
		if (automaticTasks_ == null)
			return;

		// set file info
		try {

			// get output
			Path outputFile = get();

			// manage automatic task
			automaticTaskOwnerSucceeded(outputFile, automaticTasks_, taskPanel_);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	/**
	 * Waits for sub processes to complete.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void waitForTask() throws Exception {

		// loop while analysis is running
		while (!getNamesCompleted_.get()) {

			// task cancelled
			if (isCancelled())
				return;

			// sleep a bit
			try {
				Thread.sleep(500);
			}

			// task interrupted
			catch (InterruptedException e) {
				if (isCancelled())
					return;
			}
		}
	}
}
