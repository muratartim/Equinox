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
package equinox.task;

import java.io.Serializable;

import equinox.data.fileType.SpectrumItem;
import equinox.task.serializableTask.SerializableSpectrumItem;
import javafx.scene.control.TreeItem;

/**
 * Interface for serializable task.
 *
 * @author Murat Artim
 * @date Oct 8, 2015
 * @time 9:30:27 AM
 */
public interface SerializableTask extends Serializable {

	/**
	 * Creates and returns the task to be executed, or null if the task could not be created.
	 *
	 * @param fileTreeRoot
	 *            File tree root.
	 * @return The task to be executed, or null if the task could not be created.
	 */
	SavableTask getTask(TreeItem<String> fileTreeRoot);

	/**
	 * Searches in the given file tree root and returns the spectrum item for the given serializable form.
	 *
	 * @param serializableItem
	 *            Serializable form of the searched spectrum item.
	 * @param root
	 *            File tree root.
	 * @param result
	 *            Array containing the search result. The array length should be 1.
	 */
	default void searchSpectrumItem(SerializableSpectrumItem serializableItem, TreeItem<String> root, SpectrumItem[] result) {

		// already found
		if (result[0] != null)
			return;

		// not spectrum item
		if (root instanceof SpectrumItem) {

			// cast to spectrum item
			SpectrumItem spectrumItem = (SpectrumItem) root;

			// found the spectrum item
			if (serializableItem.equals(spectrumItem))
				result[0] = spectrumItem;
		}

		// already found
		if (result[0] != null)
			return;

		// loop over children
		for (TreeItem<String> item : root.getChildren())
			searchSpectrumItem(serializableItem, item, result);
	}
}
