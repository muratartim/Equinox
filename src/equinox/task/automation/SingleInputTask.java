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
package equinox.task.automation;

/**
 * Interface for single input automatic tasks. These tasks are executed automatically after owner tasks are <u>successfully</u> completed.
 *
 * @author Murat Artim
 * @param <V>
 *            Input class.
 * @date Mar 7, 2016
 * @time 10:21:57 AM
 */
public interface SingleInputTask<V> extends AutomaticTask<V> {

	/**
	 * Sets automatic task input.
	 *
	 * @param input
	 *            Input.
	 */
	void setAutomaticInput(V input);
}