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

import equinox.task.InternalEquinoxTask;

/**
 * Inner class for task identifier.
 *
 * @author Murat Artim
 * @date 20 Aug 2018
 * @time 16:16:30
 */
public class InstructedTask {

	/** The actual task. */
	private final InternalEquinoxTask<?> task;

	/** True if task is embedded in another task. */
	private final boolean isEmbedded;

	/**
	 * Creates task identifier.
	 *
	 * @param task
	 *            Task implementation.
	 * @param isEmbedded
	 *            True if task is embedded in another task.
	 */
	public InstructedTask(InternalEquinoxTask<?> task, boolean isEmbedded) {
		this.task = task;
		this.isEmbedded = isEmbedded;
	}

	/**
	 * Returns task implementation.
	 *
	 * @return Task implementation.
	 */
	public InternalEquinoxTask<?> getTask() {
		return task;
	}

	/**
	 * Returns true if task is embedded in another task.
	 *
	 * @return True if task is embedded in another task.
	 */
	public boolean isEmbedded() {
		return isEmbedded;
	}
}