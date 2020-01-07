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

import equinox.data.input.AdaptDRFInput;
import equinox.task.AdaptDRF;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for the serializable form of adapt DRF plugin task.
 *
 * @author Murat Artim
 * @date 24 Aug 2017
 * @time 11:59:13
 *
 */
public class SerializableAdaptDRF implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input. */
	private final AdaptDRFInput input_;

	/**
	 * Creates MyCheck tool task.
	 *
	 * @param input
	 *            Input.
	 */
	public SerializableAdaptDRF(AdaptDRFInput input) {
		input_ = input;
	}

	@Override
	public AdaptDRF getTask(TreeItem<String> fileTreeRoot) {
		return new AdaptDRF(input_);
	}
}
