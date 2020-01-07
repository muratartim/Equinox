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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;

import equinox.data.ProgramArguments.ArgumentType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for rewrite CFG file task.
 *
 * @author Murat Artim
 * @date 31 Aug 2016
 * @time 17:30:00
 */
public class RewriteCFGFile extends TemporaryFileCreatingTask<Void> implements ShortRunningTask {

	/** Program arguments. */
	private final EnumMap<ArgumentType, String> arguments_;

	/**
	 * Creates rewrite CFG file task.
	 *
	 * @param arguments
	 *            Program arguments.
	 */
	public RewriteCFGFile(EnumMap<ArgumentType, String> arguments) {
		arguments_ = arguments;
	}

	@Override
	public String getTaskTitle() {
		return "Rewrite configuration file";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// update info
		updateMessage("Rewriting configuration file...");

		// get path launch configuration file
		Path configFile = Paths.get(taskPanel_.getOwner().getOwner().getOwner().getLaunchConfigurationFile());

		// cannot find configuration file (remove all white spaces from file name)
		if (!Files.exists(configFile)) {
			configFile = configFile.getParent().resolve(configFile.getFileName().toString().replaceAll("\\s+", ""));
		}

		// no configuration file
		Path cfgFileNamePath = configFile.getFileName();
		if (cfgFileNamePath == null)
			throw new Exception("Cannot find configuration file.");

		// create path to new configuration file
		Path cfgFile = getWorkingDirectory().resolve(cfgFileNamePath.toString());

		// create file writer
		try (BufferedWriter writer = Files.newBufferedWriter(cfgFile, Charset.defaultCharset())) {

			// create file reader
			try (BufferedReader reader = Files.newBufferedReader(configFile, Charset.defaultCharset())) {

				// read file till the end
				String line;
				while ((line = reader.readLine()) != null) {

					// min heap size
					if (line.startsWith("-Xms")) {
						writer.write("-Xms" + arguments_.get(ArgumentType.JVM_MIN_HEAP_SIZE) + "m");
					}
					else if (line.startsWith("-Xmx")) {
						writer.write("-Xmx" + arguments_.get(ArgumentType.JVM_MAX_HEAP_SIZE) + "m");
					}
					else if (line.startsWith("--")) {
						for (ArgumentType type : ArgumentType.values()) {
							if (line.contains(type.getName())) {
								writer.write("--" + type.getName() + "=" + arguments_.get(type));
								break;
							}
						}
					}
					else {
						writer.write(line);
					}

					// new line
					writer.newLine();
				}
			}
		}

		// replace existing CFG file with new one
		Files.copy(cfgFile, configFile, StandardCopyOption.REPLACE_EXISTING);

		// return
		return null;
	}
}
