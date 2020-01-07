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

import java.io.File;

import equinox.task.SerializableTask;
import equinox.task.automation.CheckInstructionSet;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of check instruction set task.
 *
 * @author Murat Artim
 * @date 22 Sep 2018
 * @time 12:48:28
 */
public class SerializableCheckInstructionSet implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input XML file. */
	private File inputFile;

	/** Task mode. */
	private final int taskMode;

	/**
	 * Creates check instruction set task.
	 *
	 * @param inputFile
	 *            Input XML file.
	 * @param taskMode
	 *            Task mode.
	 */
	public SerializableCheckInstructionSet(File inputFile, int taskMode) {
		this.inputFile = inputFile;
		this.taskMode = taskMode;
	}

	@Override
	public CheckInstructionSet getTask(TreeItem<String> fileTreeRoot) {
		return new CheckInstructionSet(inputFile.toPath(), taskMode);
	}
}