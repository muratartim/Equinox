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
package equinox.data;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import equinox.controller.MenuBarPanel;
import equinox.plugin.FileType;
import equinox.task.OpenWorkspace;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

/**
 * Class for storing the paths to last opened workspaces.
 *
 * @author Murat Artim
 * @date Dec 3, 2014
 * @time 11:49:19 AM
 */
public class WorkspacePaths implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 2L;

	/** List of paths. */
	private final ArrayList<String> paths_ = new ArrayList<>();

	/** List of database names. */
	private final ArrayList<String> names_ = new ArrayList<>();

	/**
	 * Adds given database path to recent paths.
	 *
	 * @param path
	 *            Database path to add.
	 */
	public void addPath(Path path) {

		// file doesn't exist
		if (!Files.exists(path))
			return;

		// convert to string
		String string = path.toString();

		// path already exists
		int index = paths_.indexOf(string);
		if (index != -1) {
			paths_.remove(index);
			names_.remove(index);
		}

		// list is empty
		if (paths_.isEmpty()) {
			paths_.add(string);
			names_.add(FileType.getNameWithoutExtension(path));
		}

		// add at the beginning
		else {
			paths_.add(0, string);
			names_.add(0, FileType.getNameWithoutExtension(path));
		}

		// list size exceeded the limit
		if (paths_.size() == 11) {
			paths_.remove(10);
			names_.remove(10);
		}
	}

	/**
	 * Returns the current database path or null if no path is available.
	 *
	 * @return The current database path or null if no path is available.
	 */
	public Path getCurrentPath() {
		return paths_.isEmpty() ? null : Paths.get(paths_.get(0));
	}

	/**
	 * Returns true if current path is valid.
	 *
	 * @return True if current path is valid.
	 */
	public boolean hasValidCurrentPath() {

		// no path available
		if (paths_.isEmpty())
			return false;

		// check if path is valid and exists
		try {
			return Files.exists(Paths.get(paths_.get(0)));
		}

		// invalid current path
		catch (InvalidPathException e) {
			return false;
		}
	}

	/**
	 * Sets up open recent menu.
	 *
	 * @param menuBarPanel
	 *            Menu bar panel.
	 */
	public void setupOpenRecentMenu(MenuBarPanel menuBarPanel) {

		// get open recent menu
		Menu openRecentmenu = menuBarPanel.getOpenRecentMenu();

		// clear menu
		openRecentmenu.getItems().clear();

		// create list of to-be-removed paths
		ArrayList<String> invalidPaths = new ArrayList<>();
		ArrayList<String> invalidNames = new ArrayList<>();

		// loop over recent database paths
		for (int i = 0; i < paths_.size(); i++) {

			// get path string
			String path = paths_.get(i);
			String name = names_.get(i);

			// path doesn't exist
			if (!Files.exists(Paths.get(path))) {
				invalidPaths.add(path);
				invalidNames.add(name);
				continue;
			}

			// create menu item
			MenuItem item = new MenuItem(name + " - " + path);

			// set on action
			item.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {

					// get Path
					Path dbPath = Paths.get(path);

					// path doesn't exist
					if (!Files.exists(dbPath)) {
						String msg = "Cannot find the workspace in '" + path + "'. Please make sure the path is correct.";
						menuBarPanel.getOwner().getNotificationPane().showWarning(msg, null);
						return;
					}

					// database already open
					if (dbPath.equals(getCurrentPath())) {
						String title = "No operation";
						String msg = "Workspace '" + dbPath.getFileName() + "' is already open.";
						menuBarPanel.getOwner().getNotificationPane().showInfo(title, msg);
						return;
					}

					// open workspace
					menuBarPanel.getOwner().getActiveTasksPanel().runTasksSequentially(new OpenWorkspace(dbPath, null));
				}
			});

			// add menu item to open recent menu
			openRecentmenu.getItems().add(item);
		}

		// remove invalid paths and names
		paths_.removeAll(invalidPaths);
		names_.removeAll(invalidNames);
	}
}
