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

import org.controlsfx.control.ToggleSwitch;

import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.task.SaveAircraftLifeFactors;
import equinox.task.SerializableTask;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of save A/C life factors task.
 *
 * @author Murat Artim
 * @date Oct 13, 2015
 * @time 12:12:16 PM
 */
public class SerializableSaveAircraftLifeFactors implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Equivalent stress. */
	private final SerializableSpectrumItem eqStress_;

	/** Options. */
	private final boolean[] options_;

	/** Option names. */
	private final String[] optionNames_;

	/** Output file. */
	private final File output_;

	/** Basis mission. */
	private final String basisMission_;

	/**
	 * Creates save A/C model life factors task.
	 *
	 * @param eqStress
	 *            Equivalent stress.
	 * @param options
	 *            Options.
	 * @param output
	 *            Output file.
	 * @param basisMission
	 *            Basis mission.
	 */
	public SerializableSaveAircraftLifeFactors(AircraftFatigueEquivalentStress eqStress, BooleanProperty[] options, File output, String basisMission) {
		eqStress_ = new SerializableSpectrumItem(eqStress);
		options_ = new boolean[options.length];
		optionNames_ = new String[options.length];
		for (int i = 0; i < options.length; i++) {
			options_[i] = options[i].get();
			optionNames_[i] = ((ToggleSwitch) options[i].getBean()).getText();
		}
		output_ = output;
		basisMission_ = basisMission;
	}

	@Override
	public SaveAircraftLifeFactors getTask(TreeItem<String> fileTreeRoot) {

		// get stress
		SpectrumItem[] result = new SpectrumItem[1];
		searchSpectrumItem(eqStress_, fileTreeRoot, result);

		// stress could not be found
		if (result[0] == null)
			return null;

		// get options
		BooleanProperty[] options = new BooleanProperty[options_.length];
		for (int i = 0; i < options.length; i++) {
			ToggleSwitch checkbox = new ToggleSwitch(optionNames_[i]);
			checkbox.setSelected(options_[i]);
			options[i] = checkbox.selectedProperty();
		}

		// create and return task
		return new SaveAircraftLifeFactors((AircraftFatigueEquivalentStress) result[0], options, output_, basisMission_);
	}
}
