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
import java.util.ArrayList;
import java.util.List;

import equinox.data.fileType.SpectrumItem;
import equinox.task.SaveDamageContributions;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for the serializable form of save damage contributions task.
 *
 * @author Murat Artim
 * @date Oct 13, 2015
 * @time 11:10:03 AM
 */
public class SerializableSaveDamageContributions implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Damage contributions. */
	private final List<SerializableSpectrumItem> contributions_;

	/** Damage contribution names. */
	private final List<String> contributionNames_;

	/** Options. */
	private final boolean[] options_;

	/** Output file. */
	private final File output_;

	/**
	 * Creates save damage contributions task.
	 *
	 * @param contributions
	 *            Damage contributions.
	 * @param contributionNames
	 *            Damage contribution names.
	 * @param options
	 *            Options.
	 * @param output
	 *            Output file.
	 */
	public SerializableSaveDamageContributions(List<SpectrumItem> contributions, List<String> contributionNames, boolean[] options, File output) {
		contributions_ = new ArrayList<>();
		for (SpectrumItem item : contributions) {
			contributions_.add(new SerializableSpectrumItem(item));
		}
		options_ = options;
		output_ = output;
		contributionNames_ = contributionNames;
	}

	@Override
	public SaveDamageContributions getTask(TreeItem<String> fileTreeRoot) {

		// get contributions
		List<SpectrumItem> contributions = new ArrayList<>();
		for (SerializableSpectrumItem item : contributions_) {

			// get contribution
			SpectrumItem[] result = new SpectrumItem[1];
			searchSpectrumItem(item, fileTreeRoot, result);

			// contribution could not be found
			if (result[0] == null)
				return null;

			// add to contributions
			contributions.add(result[0]);
		}

		// create and return task
		return new SaveDamageContributions(contributions, contributionNames_, options_, output_);
	}
}
