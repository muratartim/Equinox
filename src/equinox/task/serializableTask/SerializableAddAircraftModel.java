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

import equinox.dataServer.remote.data.AircraftModelInfo;
import equinox.dataServer.remote.data.AircraftModelInfo.AircraftModelInfoType;
import equinox.task.AddAircraftModel;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for the serializable form of add A/C model task.
 *
 * @author Murat Artim
 * @date Oct 12, 2015
 * @time 3:39:38 PM
 */
public class SerializableAddAircraftModel implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** A/C program and model name. */
	private final String program_, modelName_;

	/** Element and grid data files. */
	private final File f06File_, f07File_, grpFile_;

	/**
	 * Creates serializable form of add A/C model task.
	 *
	 * @param program
	 *            A/C program.
	 * @param modelName
	 *            A/C model name.
	 * @param f06File
	 *            Element data file.
	 * @param f07File
	 *            Grid data file.
	 * @param grpFile
	 *            Element groups data file. Can be null.
	 */
	public SerializableAddAircraftModel(String program, String modelName, Path f06File, Path f07File, Path grpFile) {
		program_ = program;
		modelName_ = modelName;
		f06File_ = f06File == null ? null : f06File.toFile();
		f07File_ = f07File == null ? null : f07File.toFile();
		grpFile_ = grpFile == null ? null : grpFile.toFile();
	}

	@Override
	public AddAircraftModel getTask(TreeItem<String> fileTreeRoot) {
		AircraftModelInfo info = new AircraftModelInfo();
		info.setInfo(AircraftModelInfoType.AC_PROGRAM, program_);
		info.setInfo(AircraftModelInfoType.MODEL_NAME, modelName_);
		return new AddAircraftModel(info, f06File_ == null ? null : f06File_.toPath(), f07File_ == null ? null : f07File_.toPath(), grpFile_ == null ? null : grpFile_.toPath());
	}
}
