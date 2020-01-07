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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import equinox.Equinox;
import equinox.controller.IntroPanel;
import equinox.data.WorkspacePaths;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.Utility;

/**
 * Class for loading last opened workspace paths.
 *
 * @author Murat Artim
 * @date Dec 3, 2014
 * @time 12:15:23 PM
 */
public class LoadWorkspacePaths extends InternalEquinoxTask<Boolean> implements ShortRunningTask {

	/** Intro panel. */
	private final IntroPanel introPanel_;

	/**
	 * Creates load last workspace paths task.
	 *
	 * @param introPanel
	 *            Intro panel.
	 */
	public LoadWorkspacePaths(IntroPanel introPanel) {
		introPanel_ = introPanel;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Load workspace paths";
	}

	@Override
	protected Boolean call() throws Exception {

		// update info
		updateMessage("Loading last workspace paths...");

		// get path file
		File file = Equinox.WORKSPACE_PATHS_FILE.toFile();

		// file doesn't exist or cannot be read
		if (!file.exists() || !file.canRead())
			return false;

		// read last paths from file
		WorkspacePaths paths = null;
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			paths = (WorkspacePaths) in.readObject();
		}

		// cannot read last workspace paths
		catch (Exception e) {
			return false;
		}

		// no valid path found
		if (paths == null || !paths.hasValidCurrentPath())
			return false;

		// setup local database pool
		updateMessage("Setting up local database pool...");
		Utility.setupLocalDBPool(paths.getCurrentPath());

		// set last paths to Equinox
		Equinox.WORKSPACE_PATHS = paths;

		// return
		return true;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set paths to intro panel
		try {
			introPanel_.databasePathsLoaded(get());
		}

		// exception occurred
		catch (Exception e) {
			introPanel_.databasePathsLoaded(false);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// set null to intro panel
		introPanel_.databasePathsLoaded(false);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// set null to intro panel
		introPanel_.databasePathsLoaded(false);
	}
}
