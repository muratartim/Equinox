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
package equinox.data;

import equinox.task.automation.AutomaticTask;

/**
 * Class for embedded task. Embedded tasks are collected by <code>AutomaticTaskOwner</code>s.
 *
 * @author Murat Artim
 * @param <V>
 *            Output class of embedded <code>AutomaticTask</code>.
 * @date 4 Nov 2018
 * @time 13:02:45
 */
public class EmbeddedTask<V> {

	/** Automatic task. */
	private final AutomaticTask<V> task;

	/** Execution mode. */
	private final ExecutionMode executionMode;

	/**
	 * Creates embedded task.
	 * 
	 * @param task
	 *            Automatic task.
	 * @param executionMode
	 *            Execution mode.
	 */
	public EmbeddedTask(AutomaticTask<V> task, ExecutionMode executionMode) {
		this.task = task;
		this.executionMode = executionMode;
	}

	/**
	 * Returns the automatic task.
	 * 
	 * @return The automatic task.
	 */
	public AutomaticTask<V> getTask() {
		return task;
	}

	/**
	 * Returns the execution mode.
	 * 
	 * @return The execution mode.
	 */
	public ExecutionMode getExecutionMode() {
		return executionMode;
	}
}