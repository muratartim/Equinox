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
import java.nio.file.Path;

import equinox.task.AddStressSequence;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of add stress sequence task.
 *
 * @author Murat Artim
 * @date Oct 12, 2015
 * @time 3:18:52 PM
 */
public class SerializableAddStressSequence implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Path to input file. */
	private final File sequenceFile_, flsFile_;

	/**
	 * Creates add stress sequence task.
	 *
	 * @param sthFile
	 *            Path to input STH file.
	 * @param flsFile
	 *            Path to input FLS file.
	 */
	public SerializableAddStressSequence(Path sthFile, Path flsFile) {
		sequenceFile_ = sthFile.toFile();
		flsFile_ = flsFile.toFile();
	}

	@Override
	public AddStressSequence getTask(TreeItem<String> fileTreeRoot) {
		return new AddStressSequence(sequenceFile_.toPath(), flsFile_.toPath());
	}
}
