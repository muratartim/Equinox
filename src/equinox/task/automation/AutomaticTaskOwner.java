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

import java.util.HashMap;

import equinox.controller.TaskPanel;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.task.InternalEquinoxTask;

/**
 * Interface for automatic task owner tasks. These tasks act as the input suppliers to their automatic tasks. These tasks perform the following actions:
 * <UL>
 * <LI>Set/add input to all automatic tasks in <code>succeeded</code> method,
 * <LI>Execute all <code>SingleInputTask</code>s in <code>succeeded</code> method,
 * <LI>Notify all <code>MultipleInputTask</code>s in <code>failed</code> and <code>cancelled</code> methods that one of the inputs will not arrive.
 * </UL>
 *
 * @author Murat Artim
 * @date 24 Jan 2017
 * @time 14:21:05
 * @param <V>
 *            Output class.
 */
public interface AutomaticTaskOwner<V> {

	/**
	 * Adds single input automatic task.
	 *
	 * @param taskID
	 *            Automatic task ID. This must be unique to the task added.
	 * @param task
	 *            Task to add.
	 */
	void addAutomaticTask(String taskID, EmbeddedTask<V> task);

	/**
	 * Returns a mapping containing the automatic tasks or null if no automatic tasks are defined.
	 *
	 * @return Mapping containing automatic tasks or null if no automatic tasks are defined.
	 */
	HashMap<String, EmbeddedTask<V>> getAutomaticTasks();

	/**
	 * This method should be called from <code>succeeded</code> method of this task. Performs the following operations:
	 * <UL>
	 * <LI>Add input to <code>MultipleInputTask</code>s,
	 * <LI>Set input to <code>SingleInputTask</code>s,
	 * <LI>Execute <code>SingleInputTask</code>s.
	 * </UL>
	 *
	 * @param input
	 *            Automatic task input.
	 * @param automaticTasks
	 *            Automatic tasks.
	 * @param taskPanel
	 *            Task panel.
	 */
	default void automaticTaskOwnerSucceeded(V input, HashMap<String, EmbeddedTask<V>> automaticTasks, TaskPanel taskPanel) {

		// there are no automatic tasks
		if (automaticTasks == null)
			return;

		// loop over automatic tasks
		for (EmbeddedTask<V> task : automaticTasks.values()) {

			// get automatic task
			AutomaticTask<V> aTask = task.getTask();

			// multiple input task (add input)
			if (aTask instanceof MultipleInputTask<?>) {
				((MultipleInputTask<V>) aTask).addAutomaticInput(this, input, task.getExecutionMode());
			}

			// single input task
			else {

				// set input
				((SingleInputTask<V>) aTask).setAutomaticInput(input);

				// no run
				if (task.getExecutionMode().equals(ExecutionMode.NO_RUN)) {
					continue;
				}

				// execute
				taskPanel.getOwner().runTaskSilently((InternalEquinoxTask<?>) aTask, task.getExecutionMode().equals(ExecutionMode.SEQUENTIAL));
			}
		}
	}

	/**
	 * This method should be called from <code>succeeded</code> method of this task, if this task contains inputs <u>specific</u> to each automatic task. Performs the following operations:
	 * <UL>
	 * <LI>Add input to <code>MultipleInputTask</code>,
	 * <LI>Set input to <code>SingleInputTask</code>,
	 * <LI>Execute <code>SingleInputTask</code>.
	 * </UL>
	 *
	 * @param input
	 *            Automatic task input.
	 * @param task
	 *            Automatic task.
	 * @param taskPanel
	 *            Task panel.
	 */
	default void automaticTaskOwnerSucceeded(V input, EmbeddedTask<V> task, TaskPanel taskPanel) {

		// null task
		if (task == null)
			return;

		// get automatic task
		AutomaticTask<V> aTask = task.getTask();

		// multiple input task (add input)
		if (aTask instanceof MultipleInputTask<?>) {
			((MultipleInputTask<V>) aTask).addAutomaticInput(this, input, task.getExecutionMode());
		}

		// single input task
		else {

			// set input
			((SingleInputTask<V>) aTask).setAutomaticInput(input);

			// no run
			if (task.getExecutionMode().equals(ExecutionMode.NO_RUN))
				return;

			// execute
			taskPanel.getOwner().runTaskSilently((InternalEquinoxTask<?>) aTask, task.getExecutionMode().equals(ExecutionMode.SEQUENTIAL));
		}
	}

	/**
	 * This method should be called from <code>failed</code> and <code>cancelled</code> methods of this task. Notifies <code>MultipleInputTask</code>s that one of the inputs will not arrive.
	 *
	 * @param automaticTasks
	 *            Automatic tasks.
	 */
	default void automaticTaskOwnerFailed(HashMap<String, EmbeddedTask<V>> automaticTasks) {

		// there are no automatic tasks
		if (automaticTasks == null)
			return;

		// loop over automatic tasks
		for (EmbeddedTask<V> task : automaticTasks.values()) {

			// get automatic task
			AutomaticTask<V> aTask = task.getTask();

			// multiple input task (add input)
			if (aTask instanceof MultipleInputTask<?>) {
				((MultipleInputTask<V>) aTask).inputFailed(this, task.getExecutionMode());
			}
		}
	}
}
