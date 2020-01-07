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

/**
 * Interface for savable task.
 *
 * @author Murat Artim
 * @date Oct 7, 2015
 * @time 4:10:25 PM
 */
public interface SavableTask {

	/**
	 * Creates and returns serializable form of this task.
	 *
	 * @return Serializable form of this task.
	 */
	SerializableTask getSerializableTask();

	/**
	 * Returns the task title. This is used to set to task panel before the task is executed.
	 *
	 * @return The task title.
	 */
	String getTaskTitle();
}
