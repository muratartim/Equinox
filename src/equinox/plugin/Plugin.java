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
package equinox.plugin;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Abstract class for all plugins.
 *
 * @author Murat Artim
 * @date Mar 26, 2015
 * @time 4:29:48 PM
 */
public abstract class Plugin {

	/** Plugin manager. */
	private final PluginManager pluginManager_;

	/**
	 * Creates plugin.
	 *
	 * @param pluginManager
	 *            Plugin manager.
	 */
	public Plugin(PluginManager pluginManager) {
		pluginManager_ = pluginManager;
	}

	/**
	 * Returns plugin manager.
	 *
	 * @return Plugin manager.
	 */
	public PluginManager getPluginManager() {
		return pluginManager_;
	}

	/**
	 * Returns the name of plugin.
	 *
	 * @return Name of plugin.
	 */
	public abstract String getName();

	/**
	 * Returns the version of plugin.
	 *
	 * @return The version of plugin.
	 */
	public abstract double getVersion();

	/**
	 * Returns the JAR file name of plugin.
	 *
	 * @return The JAR file name of plugin.
	 */
	public abstract String getJarFileName();

	/**
	 * Returns the icon of plugin. This should be a 16x16 image.
	 *
	 * @return The icon of plugin.
	 */
	public abstract PluginIcon getIcon();

	/**
	 * Creates and returns plugin input panel.
	 *
	 * @return Plugin input panel.
	 */
	public abstract InputSubPanel createInputPanel();

	/**
	 * Creates and returns plugin view panel.
	 *
	 * @return Plugin view panel.
	 */
	public abstract ViewSubPanel createViewPanel();

	@Override
	public int hashCode() {
		return new HashCodeBuilder(33, 51).append(getName()).toHashCode();
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof Plugin) {
			Plugin p = (Plugin) o;
			if (p.getName().equals(getName()))
				return true;
		}
		return false;
	}
}
