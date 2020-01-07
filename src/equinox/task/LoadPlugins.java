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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.EquinoxPluginManager;
import equinox.plugin.FileType;
import equinox.plugin.Plugin;
import equinox.plugin.PluginManager;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for load plugins task.
 *
 * @author Murat Artim
 * @date Mar 24, 2015
 * @time 9:34:44 AM
 */
public class LoadPlugins extends InternalEquinoxTask<ArrayList<Plugin>> implements ShortRunningTask {

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Load plugins";
	}

	@SuppressWarnings("resource")
	@Override
	protected ArrayList<Plugin> call() throws Exception {

		// create list to store plugins
		ArrayList<Plugin> plugins = new ArrayList<>();

		// create list to store jar files
		ArrayList<Path> jars = new ArrayList<>();

		// create plugins directory stream
		try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Equinox.PLUGINS_DIR, new JARFilter())) {

			// get iterator
			Iterator<Path> iterator = dirStream.iterator();

			// loop over files
			while (iterator.hasNext())
				jars.add(iterator.next());
		}

		// no plugin available
		if (jars.isEmpty())
			return plugins;

		// get URLs to jar files
		URL[] urls = new URL[jars.size()];
		for (int i = 0; i < urls.length; i++)
			urls[i] = jars.get(i).toUri().toURL();

		// create class loader
		ClassLoader classLoader = URLClassLoader.newInstance(urls, getClass().getClassLoader());

		// get plugin manager
		EquinoxPluginManager pluginManager = taskPanel_.getOwner().getOwner().getPluginManager();

		// load plugin classes
		for (Path jar : jars) {

			// construct fully qualified class name
			String className = FileType.getNameWithoutExtension(jar) + ".PluginEntry";

			// load class
			Class<?> implClass = Class.forName(className, true, classLoader);
			Class<? extends Plugin> pluginClass = implClass.asSubclass(Plugin.class);
			Constructor<? extends Plugin> pluginConstructor = pluginClass.getConstructor(PluginManager.class);
			Plugin plugin = pluginConstructor.newInstance(pluginManager);
			plugins.add(plugin);
		}

		// return plugins
		return plugins;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get plugins
			ArrayList<Plugin> plugins = get();

			// get plugin manager
			EquinoxPluginManager pluginManager = taskPanel_.getOwner().getOwner().getPluginManager();

			// add plugins
			for (Plugin plugin : plugins)
				pluginManager.addPlugin(plugin);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Class for JAR file filter.
	 *
	 * @author Murat Artim
	 * @date Mar 24, 2015
	 * @time 9:53:07 AM
	 */
	private class JARFilter implements DirectoryStream.Filter<Path> {

		@Override
		public boolean accept(Path entry) throws IOException {
			Path fileNamePath = entry.getFileName();
			if (fileNamePath == null)
				return false;
			String fileName = fileNamePath.toString().toUpperCase();
			if (fileName.endsWith(".JAR"))
				return true;
			return false;
		}
	}
}
