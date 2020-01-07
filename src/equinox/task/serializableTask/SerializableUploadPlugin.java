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

import equinox.data.ClientPluginInfo;
import equinox.task.SerializableTask;
import equinox.task.UploadPlugin;
import javafx.scene.control.TreeItem;

/**
 * Class for the serializable form of upload plugin task.
 *
 * @author Murat Artim
 * @date Oct 13, 2015
 * @time 2:17:13 PM
 */
public class SerializableUploadPlugin implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Plugin info. */
	private final ClientPluginInfo pluginInfo_;

	/** Plugin jar file. */
	private final File jarFile_;

	/**
	 * Creates upload plugin task.
	 *
	 * @param pluginInfo
	 *            Plugin info.
	 * @param jarFile
	 *            Plugin jar file.
	 */
	public SerializableUploadPlugin(ClientPluginInfo pluginInfo, Path jarFile) {
		pluginInfo_ = pluginInfo;
		jarFile_ = jarFile.toFile();
	}

	@Override
	public UploadPlugin getTask(TreeItem<String> fileTreeRoot) {
		return new UploadPlugin(pluginInfo_, jarFile_.toPath());
	}
}
