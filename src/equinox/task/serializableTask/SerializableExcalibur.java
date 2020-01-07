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
package equinox.task.serializableTask;

import equinox.data.input.ExcaliburInput;
import equinox.task.Excalibur;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for the serializable form of create Excalibur tables task.
 *
 * @author Murat Artim
 * @date 30 Nov 2017
 * @time 00:39:57
 */
public class SerializableExcalibur implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input. */
	private final ExcaliburInput input_;

	/**
	 * Creates serializable form of create Excalibur tables task.
	 *
	 * @param input
	 *            Analysis input.
	 */
	public SerializableExcalibur(ExcaliburInput input) {
		input_ = input;
	}

	@Override
	public Excalibur getTask(TreeItem<String> fileTreeRoot) {
		return new Excalibur(input_);
	}
}
