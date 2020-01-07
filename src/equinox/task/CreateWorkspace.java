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

import java.nio.file.Path;
import java.util.ArrayList;

import equinox.Equinox;
import equinox.controller.InfoViewPanel;
import equinox.controller.InputPanel;
import equinox.controller.IntroPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.SpectrumItem;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.ServerUtility;
import equinox.utility.Utility;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

/**
 * Class for create new workspace task.
 *
 * @author Murat Artim
 * @date 2 Aug 2017
 * @time 14:53:47
 *
 */
public class CreateWorkspace extends InternalEquinoxTask<Void> {

	/** Path to new database. */
	private final Path path_;

	/** Intro panel. */
	private final IntroPanel introPanel_;

	/**
	 * Creates create new workspace task.
	 *
	 * @param path
	 *            Path to new database.
	 * @param introPanel
	 *            Intro panel. This can be null if there is already a workspace open.
	 */
	public CreateWorkspace(Path path, IntroPanel introPanel) {
		path_ = path;
		introPanel_ = introPanel;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Create new workspace '" + path_.getFileName() + "'";
	}

	@Override
	protected Void call() throws Exception {

		// extract workspace schema to given directory
		updateMessage("Extracting workspace schema...");
		Utility.extractAllFilesFromZIP(Equinox.WORKSPACE_SCHEMA_PATH, this, path_);

		// there is already a workspace open
		if (Equinox.DBC_POOL != null) {

			// shutdown database connection pool
			updateMessage("Shutting down database connection pool...");
			Equinox.DBC_POOL.close();

			// shutdown the workspace
			updateMessage("Shutting down workspace...");
			Utility.shutdownWorkspace();
		}

		// setup connection to new workspace
		updateMessage("Setting up connection to new workspace...");
		Utility.setupLocalDBPool(path_);

		// add path to last paths
		Equinox.WORKSPACE_PATHS.addPath(path_);

		// return
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set database name as title
		String title = Equinox.OS_ARCH.equals(ServerUtility.X86) ? "Equinox" : "Equinox 64bit";
		title += " - " + FileType.getNameWithoutExtension(Equinox.WORKSPACE_PATHS.getCurrentPath());
		taskPanel_.getOwner().getOwner().getOwner().getStage().setTitle(title);

		// setup open recent menu
		Equinox.WORKSPACE_PATHS.setupOpenRecentMenu(taskPanel_.getOwner().getOwner().getMenuBarPanel());

		// at startup
		if (introPanel_ != null) {
			taskPanel_.getOwner().runTasksSequentially(introPanel_.addTask(new SaveWorkspacePaths()), introPanel_.addTask(new LoadUserAuthentication()), introPanel_.addTask(new ConnectToServers()), introPanel_.addTask(new LoadAllFiles(introPanel_)));
		}

		// not startup
		else {

			// show file list panel
			taskPanel_.getOwner().getOwner().getInputPanel().showSubPanel(InputPanel.FILE_VIEW_PANEL);

			// remove all local files
			ObservableList<TreeItem<String>> list = taskPanel_.getOwner().getOwner().getInputPanel().getFileTreeRoot().getChildren();
			ArrayList<TreeItem<String>> itemsToRemove = new ArrayList<>();
			for (TreeItem<String> item : list) {
				if (item instanceof SpectrumItem) {
					itemsToRemove.add(item);
				}
			}
			list.removeAll(itemsToRemove);

			// clear info view
			InfoViewPanel infoView = (InfoViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			infoView.clearView();

			// disable selected items
			taskPanel_.getOwner().getOwner().getMenuBarPanel().disableSelectedItems(true);

			// check for material updates
			if (Equinox.USER.hasPermission(Permission.CHECK_FOR_MATERIAL_UPDATES, false, taskPanel_.getOwner().getOwner())) {
				taskPanel_.getOwner().runTaskInParallel(new CheckForMaterialUpdates(false));
			}

			// save workspace paths
			taskPanel_.getOwner().runTaskInParallel(new SaveWorkspacePaths());
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// at startup
		if (introPanel_ != null) {
			introPanel_.showSetupWorkspaceDialog("Exception occurred during creating a new workspace.\t\nHow would you like to proceed?");
		}
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// at startup
		if (introPanel_ != null) {
			introPanel_.showSetupWorkspaceDialog("Exception occurred during creating a new workspace.\t\nHow would you like to proceed?");
		}
	}
}
