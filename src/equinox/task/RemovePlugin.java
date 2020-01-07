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

import java.nio.file.Files;

import equinox.Equinox;
import equinox.controller.ViewPanel;
import equinox.plugin.Plugin;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for remove plugin task.
 *
 * @author Murat Artim
 * @date Mar 25, 2015
 * @time 2:06:53 PM
 */
public class RemovePlugin extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** Plugin to remove. */
	private final Plugin plugin_;

	/**
	 * Creates remove plugin task.
	 *
	 * @param plugin
	 *            Plugin to remove.
	 */
	public RemovePlugin(Plugin plugin) {
		plugin_ = plugin;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Remove plugin '" + plugin_.getName() + "'";
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.REMOVE_PLUGIN);

		// remove plugin
		updateTitle("Removing plugin '" + plugin_.getName() + "'...");
		updateMessage("Deleting plugin jar file...");
		Files.delete(Equinox.PLUGINS_DIR.resolve(plugin_.getJarFileName()));
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// remove plugin from plugin manager
		taskPanel_.getOwner().getOwner().getPluginManager().removePlugin(plugin_);

		// show info
		String title = "Remove plugin";
		String message = "Plugin '" + plugin_.getName() + "' successfully removed from Equinox platform.";
		taskPanel_.getOwner().getOwner().getNotificationPane().showOk(title, message);

		// update plugins view (if necessary)
		if (Equinox.USER.hasPermission(Permission.GET_EQUINOX_PLUGIN_INFO, false, taskPanel_.getOwner().getOwner())) {
			if (taskPanel_.getOwner().getOwner().getViewPanel().getCurrentSubPanelIndex() == ViewPanel.PLUGIN_VIEW) {
				taskPanel_.getOwner().runTaskInParallel(new GetPlugins());
			}
		}
	}
}
