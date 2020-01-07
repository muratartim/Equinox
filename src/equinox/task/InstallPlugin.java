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

import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.ViewPanel;
import equinox.data.ClientPluginInfo;
import equinox.data.EquinoxPluginManager;
import equinox.dataServer.remote.data.ServerPluginInfo.PluginInfoType;
import equinox.plugin.FileType;
import equinox.plugin.Plugin;
import equinox.plugin.PluginManager;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;

/**
 * Class for install plugin task.
 *
 * @author Murat Artim
 * @date Mar 31, 2015
 * @time 3:31:43 PM
 */
public class InstallPlugin extends InternalEquinoxTask<Plugin> implements LongRunningTask {

	/** Plugin info. */
	private final ClientPluginInfo pluginInfo_;

	/**
	 * Creates install plugin task.
	 *
	 * @param pluginInfo
	 *            Plugin info.
	 */
	public InstallPlugin(ClientPluginInfo pluginInfo) {
		pluginInfo_ = pluginInfo;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Install plugin '" + (String) pluginInfo_.getInfo(PluginInfoType.NAME) + "'";
	}

	@Override
	protected Plugin call() throws Exception {

		// check permission
		checkPermission(Permission.INSTALL_EQUINOX_PLUGIN);

		// update progress info
		updateTitle("Installing plugin '" + (String) pluginInfo_.getInfo(PluginInfoType.NAME) + "'");

		// create path to plugin jar file
		Path jarFile = Equinox.PLUGINS_DIR.resolve((String) pluginInfo_.getInfo(PluginInfoType.JAR_NAME));

		// download plugin data
		updateMessage("Downloading plugin package...");
		try (FilerConnection filer = getFilerConnection()) {
			String url = (String) pluginInfo_.getInfo(PluginInfoType.DATA_URL);
			if (url != null) {
				if (filer.fileExists(url)) {
					filer.getSftpChannel().get(url, jarFile.toString());
				}
			}
		}

		// get URL to jar file
		updateMessage("Loading plugin classes...");
		URL[] urls = { jarFile.toUri().toURL() };

		// create class loader
		ClassLoader classLoader = URLClassLoader.newInstance(urls, getClass().getClassLoader());

		// get plugin manager
		EquinoxPluginManager pluginManager = taskPanel_.getOwner().getOwner().getPluginManager();

		// construct fully qualified class name
		String className = FileType.getNameWithoutExtension(jarFile) + ".PluginEntry";

		// load class
		Class<?> implClass = Class.forName(className, true, classLoader);
		Class<? extends Plugin> pluginClass = implClass.asSubclass(Plugin.class);
		Constructor<? extends Plugin> pluginConstructor = pluginClass.getConstructor(PluginManager.class);
		Plugin plugin = pluginConstructor.newInstance(pluginManager);

		// return plugin
		return plugin;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get plugin
			Plugin plugin = get();

			// get plugin manager
			EquinoxPluginManager pluginManager = taskPanel_.getOwner().getOwner().getPluginManager();

			// add plugin
			pluginManager.addPlugin(plugin);

			// update plugin view
			if (Equinox.USER.hasPermission(Permission.GET_EQUINOX_PLUGIN_INFO, false, taskPanel_.getOwner().getOwner())) {
				if (taskPanel_.getOwner().getOwner().getViewPanel().getCurrentSubPanelIndex() == ViewPanel.PLUGIN_VIEW) {
					taskPanel_.getOwner().runTaskInParallel(new GetPlugins());
				}
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
