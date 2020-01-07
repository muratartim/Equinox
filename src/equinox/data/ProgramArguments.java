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

import java.lang.management.ManagementFactory;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import equinox.Equinox;
import equinox.serverUtilities.ServerUtility;

/**
 * Class for program arguments.
 *
 * @author Murat Artim
 * @date 30 Aug 2016
 * @time 13:36:10
 */
public class ProgramArguments {

	/**
	 * Enumeration for program argument types.
	 *
	 * @author Murat Artim
	 * @date 30 Aug 2016
	 * @time 11:58:58
	 */
	public enum ArgumentType {

		/** Program argument type. */
		MAX_PARALLEL_TASKS("maxParallelTasks"), MAX_PARALLEL_SUBTASKS("maxParallelSubtasks"), DATABASE_PAGE_SIZE("databasePageSize"), DATABASE_PAGE_CACHE_SIZE("databasePageCacheSize"), MAX_VISIBLE_STFS_PER_SPECTRUM("maxVisibleSTFsPerSpectrum"), JVM_MIN_HEAP_SIZE("minJVMHeapSize"),
		JVM_MAX_HEAP_SIZE("maxJVMHeapSize"), COLOR_THEME("colorTheme");

		/** Name of argument. */
		private final String name_;

		/**
		 * Creates program argument type.
		 *
		 * @param name
		 *            Name of argument.
		 */
		ArgumentType(String name) {
			name_ = name;
		}

		/**
		 * Returns argument type name.
		 *
		 * @return Argument type name.
		 */
		public String getName() {
			return name_;
		}
	}

	/** Array storing the arguments. */
	private final EnumMap<ArgumentType, String> arguments_;

	/**
	 * Creates program arguments.
	 *
	 * @param application
	 *            Application to get the arguments.
	 */
	public ProgramArguments(Equinox application) {

		// create arguments
		arguments_ = new EnumMap<>(ArgumentType.class);

		// get user and JVM arguments
		Map<String, String> userArgs = application.getApplicationParameters().getNamed();
		List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();

		// loop over argument types
		for (ArgumentType argumentType : ArgumentType.values()) {

			// minimum heap size
			if (argumentType.equals(ArgumentType.JVM_MIN_HEAP_SIZE)) {
				for (String jvmArg : jvmArgs) {
					if (jvmArg.startsWith("-Xms")) {
						arguments_.put(argumentType, jvmArg.substring(4, jvmArg.length() - 1));
						break;
					}
				}
			}

			// maximum heap size
			else if (argumentType.equals(ArgumentType.JVM_MAX_HEAP_SIZE)) {
				for (String jvmArg : jvmArgs) {
					if (jvmArg.startsWith("-Xmx")) {
						arguments_.put(argumentType, jvmArg.substring(4, jvmArg.length() - 1));
						break;
					}
				}
			}

			// maximum parallel tasks
			else if (argumentType.equals(ArgumentType.MAX_PARALLEL_TASKS)) {
				arguments_.put(argumentType, userArgs.get(argumentType.getName()));
			}

			// maximum parallel sub-tasks
			else if (argumentType.equals(ArgumentType.MAX_PARALLEL_SUBTASKS)) {
				arguments_.put(argumentType, userArgs.get(argumentType.getName()));
			}

			// other arguments
			else {
				arguments_.put(argumentType, userArgs.get(argumentType.getName()));
			}
		}
	}

	/**
	 * Returns the demanded program argument.
	 *
	 * @param argumentType
	 *            Argument type.
	 * @return The argument value.
	 */
	public String getArgument(ArgumentType argumentType) {
		return arguments_.get(argumentType);
	}

	/**
	 * Echoes program arguments to the log.
	 */
	public void echoArguments() {
		String echo = "Program arguments";
		Iterator<Entry<ArgumentType, String>> arguments = arguments_.entrySet().iterator();
		while (arguments.hasNext()) {
			Entry<ArgumentType, String> entry = arguments.next();
			echo += ", " + entry.getKey().getName() + ": " + entry.getValue();
		}
		Equinox.LOGGER.info(echo);
	}

	/**
	 * Checks heap size arguments.
	 *
	 * @param minHeap
	 *            Minimum heap size in MB.
	 * @param maxHeap
	 *            Maximum heap size in MB.
	 * @return Null if given sizes are valid, or warning message if they are invalid.
	 */
	public static String checkHeapSize(int minHeap, int maxHeap) {

		// check boundaries
		if (minHeap < 128)
			return "Invalid minimum heap size supplied. Minimum heap size must be at least 128MB.";
		if (Equinox.OS_ARCH.equals(ServerUtility.X86)) {
			if (maxHeap > 1024)
				return "Invalid maximum heap size supplied. Maximum heap size must be at most 1024MB.";
		}
		else {
			if (maxHeap > 6144)
				return "Invalid maximum heap size supplied. Maximum heap size must be at most 6144MB.";
		}

		// compare sizes
		if (minHeap > maxHeap)
			return "Invalid heap sizes supplied. Minimum heap size must be smaller than maximum heap size.";

		// valid values
		return null;
	}

	/**
	 * Checks parallel tasks arguments.
	 *
	 * @param maxTasks
	 *            Maximum parallel tasks.
	 * @param maxSubTasks
	 *            Maximum parallel sub-tasks.
	 * @return Null if given arguments are valid, or warning message if they are invalid.
	 */
	public static String checkParallelTasks(int maxTasks, int maxSubTasks) {

		// check boundaries
		if (maxTasks < 2)
			return "Invalid maximum parallel tasks supplied. Maximum parallel tasks must be at least 2.";
		if (maxSubTasks < 2)
			return "Invalid maximum parallel sub-tasks supplied. Maximum parallel sub-tasks must be at least 2.";

		// get number of cores
		int limit = 2 * Runtime.getRuntime().availableProcessors();
		if (maxTasks > limit)
			return "Invalid maximum parallel tasks supplied. Maximum parallel tasks must be at most " + limit + ".";
		if (maxSubTasks > limit)
			return "Invalid maximum parallel sub-tasks supplied. Maximum parallel sub-tasks must be at most " + limit + ".";

		// valid
		return null;
	}
}
