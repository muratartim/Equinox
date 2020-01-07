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

import java.io.File;
import java.util.concurrent.Future;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 * Interface for plugin manager.
 *
 * @author Murat Artim
 * @date Mar 26, 2015
 * @time 5:03:24 PM
 */
public interface PluginManager {

	/**
	 * Shows input panel of the given plugin.
	 *
	 * @param plugin
	 *            Plugin to show its input panel. <code>null</code> should be given for showing files sub-panel.
	 */
	void showPluginInputPanel(Plugin plugin);

	/**
	 * Shows view panel of the given plugin.
	 *
	 * @param plugin
	 *            Plugin to show its view panel. <code>null</code> should be given for showing info sub-panel.
	 */
	void showPluginViewPanel(Plugin plugin);

	/**
	 * Runs given process sequentially.
	 *
	 * @param process
	 *            Process to be executed.
	 * @return Returns a Future representing that task. The Future's get method will return null upon successful completion.
	 */
	Future<?> runProcessSequentially(PluginProcess process);

	/**
	 * Runs given process in parallel execution mode.
	 *
	 * @param process
	 *            Process to be executed in parallel mode.
	 * @return Returns a Future representing that task. The Future's get method will return null upon successful completion.
	 */
	Future<?> runProcessInParallel(PluginProcess process);

	/**
	 * Runs given processes in sequential mode. Processes are executed in the given order.
	 *
	 * @param processes
	 *            Processes to be executed in sequential mode.
	 */
	void runProcessesSequentially(PluginProcess... processes);

	/**
	 * Runs given process silently (i.e. no task submission notification will be shown). This method is useful to execute tasks directly from within other tasks (i.e. not from FX application thread).
	 *
	 * @param process
	 *            Process to be executed.
	 * @param isSequential
	 *            True if task should be executed within the sequential task queue.
	 * @return Returns a Future representing that task. The Future's get method will return null upon successful completion.
	 */
	Future<?> runProcessSilently(PluginProcess process, boolean isSequential);

	/**
	 * Returns the file chooser.
	 *
	 * @param filters
	 *            Extension filters.
	 * @return The file chooser.
	 */
	FileChooser getFileChooser(ExtensionFilter... filters);

	/**
	 * Returns the directory chooser.
	 *
	 * @return The directory chooser.
	 */
	DirectoryChooser getDirectoryChooser();

	/**
	 * Sets initial directory for the file chooser.
	 *
	 * @param initialDirectory
	 *            Initial directory to set.
	 */
	void setInitialDirectory(File initialDirectory);

	/**
	 * Returns application stage.
	 *
	 * @return Application stage.
	 */
	Stage getStage();

	/**
	 * Shows help page of plugin.
	 *
	 * @param address
	 *            Help page address.
	 */
	void showHelp(String address);

	/**
	 * Downloads sample inputs for the given name key.
	 *
	 * @param name
	 *            Name key for sample input.
	 */
	void downloadSampleInput(String name);
}
