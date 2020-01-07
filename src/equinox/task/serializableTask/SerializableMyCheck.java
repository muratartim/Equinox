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

import equinox.data.input.MyCheckInput;
import equinox.task.MyCheck;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of MyCheck task.
 *
 * @author Murat Artim
 * @date Oct 12, 2015
 * @time 5:40:42 PM
 */
public class SerializableMyCheck implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input. */
	private final MyCheckInput input_;

	/**
	 * Creates MyCheck tool task.
	 *
	 * @param input
	 *            Input.
	 */
	public SerializableMyCheck(MyCheckInput input) {
		input_ = input;
	}

	@Override
	public MyCheck getTask(TreeItem<String> fileTreeRoot) {
		return new MyCheck(input_);
	}
}
