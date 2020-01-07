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
import java.util.List;

import equinox.task.ExportMultiplicationTables;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of export multiplication tables task.
 *
 * @author Murat Artim
 * @date Feb 19, 2016
 * @time 12:51:33 PM
 */
public class SerializableExportMultiplicationTables implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Attributes. */
	private final String spectrumName_, pilotPointName_, program_, section_, mission_, issue_, delRef_, description_;

	/** MUT files. */
	private final List<File> mutFiles_;

	/** Path to output ZIP file. */
	private final File output_;

	/**
	 * Creates export multiplication tables task.
	 *
	 * @param mutFiles
	 *            Multiplication table files.
	 * @param spectrumName
	 *            Spectrum name.
	 * @param pilotPointName
	 *            Pilot point name.
	 * @param program
	 *            A/C program.
	 * @param section
	 *            A/C section.
	 * @param mission
	 *            Fatigue mission.
	 * @param issue
	 *            Multiplication table issue.
	 * @param delRef
	 *            Delivery reference.
	 * @param description
	 *            Description.
	 * @param output
	 *            Path to output ZIP file.
	 */
	public SerializableExportMultiplicationTables(List<File> mutFiles, String spectrumName, String pilotPointName, String program, String section,
			String mission, String issue, String delRef, String description, File output) {
		mutFiles_ = mutFiles;
		spectrumName_ = spectrumName;
		pilotPointName_ = pilotPointName;
		program_ = program;
		section_ = section;
		mission_ = mission;
		issue_ = issue;
		delRef_ = delRef;
		description_ = description;
		output_ = output;
	}

	@Override
	public ExportMultiplicationTables getTask(TreeItem<String> fileTreeRoot) {
		return new ExportMultiplicationTables(mutFiles_, spectrumName_, pilotPointName_, program_, section_, mission_, issue_, delRef_, description_,
				output_);
	}
}
