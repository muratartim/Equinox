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

import equinox.task.SerializableTask;
import equinox.task.UploadAppUpdate;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of upload app update task.
 *
 * @author Murat Artim
 * @date 26 May 2018
 * @time 20:27:34
 */
public class SerializableUploadAppUpdate implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input files. */
	private final File manifest_, jar_, libs_, resources_, dlls_, verDesc_;

	/**
	 * Creates upload application update task.
	 *
	 * @param manifest
	 *            Path to manifest file.
	 * @param jar
	 *            Path to jar package.
	 * @param libs
	 *            Path to libs package.
	 * @param resources
	 *            Path to resources package.
	 * @param dlls
	 *            Path to dlls package.
	 * @param verDesc
	 *            Path to version description file.
	 */
	public SerializableUploadAppUpdate(File manifest, File jar, File libs, File resources, File dlls, File verDesc) {
		manifest_ = manifest;
		jar_ = jar;
		libs_ = libs;
		resources_ = resources;
		dlls_ = dlls;
		verDesc_ = verDesc;
	}

	@Override
	public UploadAppUpdate getTask(TreeItem<String> fileTreeRoot) {
		Path jar = jar_ == null ? null : jar_.toPath();
		Path libs = libs_ == null ? null : libs_.toPath();
		Path resources = resources_ == null ? null : resources_.toPath();
		Path dlls = dlls_ == null ? null : dlls_.toPath();
		Path verDesc = verDesc_ == null ? null : verDesc_.toPath();
		return new UploadAppUpdate(manifest_.toPath(), jar, libs, resources, dlls, verDesc);
	}
}
