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

import org.controlsfx.control.ToggleSwitch;

import equinox.data.fileType.DamageAngle;
import equinox.data.fileType.SpectrumItem;
import equinox.task.SaveDamageAngles;
import equinox.task.SerializableTask;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of save damage angle info task.
 *
 * @author Murat Artim
 * @date Oct 13, 2015
 * @time 10:57:09 AM
 */
public class SerializableSaveDamageAngles implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Damage angles. */
	private final List<SerializableSpectrumItem> angles_;

	/** Options. */
	private final boolean[] options_;

	/** Option names. */
	private final String[] optionNames_;

	/** Output file. */
	private final File output_;

	/**
	 * Creates save damage angles task.
	 *
	 * @param angles
	 *            Damage angles.
	 * @param options
	 *            Options.
	 * @param output
	 *            Output file.
	 */
	public SerializableSaveDamageAngles(List<DamageAngle> angles, BooleanProperty[] options, File output) {
		angles_ = new ArrayList<>();
		for (SpectrumItem item : angles) {
			angles_.add(new SerializableSpectrumItem(item));
		}
		options_ = new boolean[options.length];
		optionNames_ = new String[options.length];
		for (int i = 0; i < options.length; i++) {
			options_[i] = options[i].get();
			optionNames_[i] = ((ToggleSwitch) options[i].getBean()).getText();
		}
		output_ = output;
	}

	@Override
	public SaveDamageAngles getTask(TreeItem<String> fileTreeRoot) {

		// get angles
		List<DamageAngle> angles = new ArrayList<>();
		for (SerializableSpectrumItem item : angles_) {

			// get angle
			SpectrumItem[] result = new SpectrumItem[1];
			searchSpectrumItem(item, fileTreeRoot, result);

			// angle could not be found
			if (result[0] == null)
				return null;

			// add to angles
			angles.add((DamageAngle) result[0]);
		}

		// get options
		BooleanProperty[] options = new BooleanProperty[options_.length];
		for (int i = 0; i < options.length; i++) {
			ToggleSwitch checkbox = new ToggleSwitch(optionNames_[i]);
			checkbox.setSelected(options_[i]);
			options[i] = checkbox.selectedProperty();
		}

		// create and return task
		return new SaveDamageAngles(angles, options, output_);
	}
}
