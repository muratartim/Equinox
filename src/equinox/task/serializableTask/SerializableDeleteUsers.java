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

import equinox.task.DeleteUsers;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of delete users task.
 *
 * @author Murat Artim
 * @date 4 Apr 2018
 * @time 18:13:34
 */
public class SerializableDeleteUsers implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** User aliases. */
	private final String aliases_;

	/**
	 * Creates serializable form of delete users task.
	 *
	 * @param aliases
	 *            User aliases.
	 */
	public SerializableDeleteUsers(String aliases) {
		aliases_ = aliases;
	}

	@Override
	public DeleteUsers getTask(TreeItem<String> fileTreeRoot) {
		return new DeleteUsers(aliases_);
	}
}
