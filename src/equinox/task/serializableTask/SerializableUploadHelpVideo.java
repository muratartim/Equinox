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
import equinox.task.UploadHelpVideo;
import javafx.scene.control.TreeItem;

/**
 * Class for the serializable form of upload help video task.
 *
 * @author Murat Artim
 * @date Mar 25, 2016
 * @time 2:34:05 PM
 */
public class SerializableUploadHelpVideo implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Help video movie file. */
	private final File movFile_;

	/** Help video name, duration and description. */
	private final String name_, duration_, description_;

	/**
	 * Creates serializable form of upload help video task.
	 *
	 * @param name
	 *            Help video name.
	 * @param duration
	 *            Help video duration.
	 * @param description
	 *            Help video description.
	 * @param movFile
	 *            Help video movie file.
	 */
	public SerializableUploadHelpVideo(String name, String duration, String description, File movFile) {
		name_ = name;
		duration_ = duration;
		description_ = description;
		movFile_ = movFile;
	}

	@Override
	public UploadHelpVideo getTask(TreeItem<String> fileTreeRoot) {
		return new UploadHelpVideo(name_, duration_, description_, movFile_.toPath());
	}
}
