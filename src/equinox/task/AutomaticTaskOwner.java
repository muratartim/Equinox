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

import java.util.HashMap;

/**
 * Interface for automatic task owner tasks. These tasks execute the automatic tasks after their process is complete.
 *
 * @author Murat Artim
 * @date 24 Jan 2017
 * @time 14:21:05
 * @param <V>
 *            Output class.
 */
public interface AutomaticTaskOwner<V> {

	/**
	 * Adds automatic task.
	 * 
	 * @param taskID
	 *            Automatic task ID. This must be unique to the task added.
	 * @param task
	 *            Task to add.
	 */
	void addAutomaticTask(String taskID, AutomaticTask<V> task);

	/**
	 * Returns a mapping containing the automatic tasks or null if no automatic tasks are defined.
	 *
	 * @return Mapping containing automatic tasks or null if no automatic tasks are defined.
	 */
	HashMap<String, AutomaticTask<V>> getAutomaticTasks();
}