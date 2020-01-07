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
import java.sql.Connection;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.fileType.ExternalStressSequence;
import equinox.process.SaveExternalSTH;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for save external stress sequence as STH task.
 *
 * @author Murat Artim
 * @date Mar 13, 2015
 * @time 11:26:53 AM
 */
public class SaveExternalStressSequenceAsSTH extends InternalEquinoxTask<Path> implements LongRunningTask, SingleInputTask<ExternalStressSequence>, AutomaticTaskOwner<Path> {

	/** Stress sequence to save. */
	private ExternalStressSequence sequence;

	/** Output file. */
	private final File output;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save external stress sequence as STH task.
	 *
	 * @param sequence
	 *            Stress sequence to save. Can be null for automatic execution.
	 * @param output
	 *            Output file.
	 */
	public SaveExternalStressSequenceAsSTH(ExternalStressSequence sequence, File output) {
		this.sequence = sequence;
		this.output = output;
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
	public void setAutomaticInput(ExternalStressSequence input) {
		this.sequence = input;
	}

	@Override
	public String getTaskTitle() {
		return "Save external stress sequence";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.SAVE_FILE);

		// start process
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			new SaveExternalSTH(this, sequence, output).start(connection);
		}

		// return
		return output.toPath();
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get output file
			Path file = get();

			// manage automatic tasks
			automaticTaskOwnerSucceeded(file, automaticTasks_, taskPanel_);
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
}
