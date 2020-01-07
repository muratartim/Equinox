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

import equinox.data.fileType.AircraftEquivalentStresses;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.AircraftEquivalentStressType;
import equinox.task.AddAircraftEquivalentStresses;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of add aircraft equivalent stresses task.
 *
 * @author Murat Artim
 * @date May 6, 2016
 * @time 10:58:27 PM
 */
public class SerializableAddAircraftEquivalentStresses implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input file. */
	private final File inputFile_;

	/** Equivalent stress type. */
	private final AircraftEquivalentStressType stressType_;

	/** Serializable equivalent stresses folder. */
	private final SerializableSpectrumItem folder_;

	/**
	 * Creates add aircraft model equivalent stresses task.
	 *
	 * @param inputFile
	 *            Input file.
	 * @param stressType
	 *            Equivalent stress type.
	 * @param folder
	 *            Equivalent stresses folder.
	 */
	public SerializableAddAircraftEquivalentStresses(File inputFile, AircraftEquivalentStressType stressType, AircraftEquivalentStresses folder) {
		inputFile_ = inputFile;
		stressType_ = stressType;
		folder_ = new SerializableSpectrumItem(folder);
	}

	@Override
	public AddAircraftEquivalentStresses getTask(TreeItem<String> fileTreeRoot) {

		// get equivalent stresses folder
		SpectrumItem[] result = new SpectrumItem[1];
		searchSpectrumItem(folder_, fileTreeRoot, result);

		// equivalent stresses folder could not be found
		if (result[0] == null)
			return null;

		// create and return task
		return new AddAircraftEquivalentStresses(inputFile_.toPath(), stressType_, (AircraftEquivalentStresses) result[0]);
	}
}
