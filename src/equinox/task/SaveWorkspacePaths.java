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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import equinox.Equinox;

/**
 * Class for saving opened workspace paths to file.
 *
 * @author Murat Artim
 * @date Dec 3, 2014
 * @time 11:54:38 AM
 */
public class SaveWorkspacePaths extends InternalEquinoxTask<Void> {

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save workspace paths";
	}

	@Override
	protected Void call() throws Exception {
		try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(Equinox.WORKSPACE_PATHS_FILE.toFile())))) {
			out.writeObject(Equinox.WORKSPACE_PATHS);
		}
		return null;
	}
}
