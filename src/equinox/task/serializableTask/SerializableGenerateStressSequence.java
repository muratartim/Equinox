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

import equinox.data.fileType.STFFile;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.task.GenerateStressSequence;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of generate stress sequence task.
 *
 * @author Murat Artim
 * @date Oct 11, 2015
 * @time 6:19:47 PM
 */
public class SerializableGenerateStressSequence implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input. */
	private final GenerateStressSequenceInput input_;

	/** Serializable STF file. */
	private final SerializableSpectrumItem stfFile_;

	/**
	 * Creates serializable form of generate stress sequence task.
	 *
	 * @param stfFile
	 *            The owner STF file.
	 * @param input
	 *            Analysis input.
	 */
	public SerializableGenerateStressSequence(STFFile stfFile, GenerateStressSequenceInput input) {
		stfFile_ = new SerializableSpectrumItem(stfFile);
		input_ = input;
	}

	@Override
	public GenerateStressSequence getTask(TreeItem<String> fileTreeRoot) {

		// get STF file
		SpectrumItem[] result = new SpectrumItem[1];
		searchSpectrumItem(stfFile_, fileTreeRoot, result);

		// STF file could not be found
		if (result[0] == null)
			return null;

		// create and return task
		return new GenerateStressSequence((STFFile) result[0], input_);
	}
}
