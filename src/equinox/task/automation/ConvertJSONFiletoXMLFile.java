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
package equinox.task.automation;

import java.nio.file.Path;

import equinox.network.AutomationClientHandler;
import equinox.process.automation.ConvertJSONtoXML;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.TemporaryFileCreatingTask;

/**
 * Class for convert JSON file to XML file task.
 *
 * @author Murat Artim
 * @date 12 Sep 2018
 * @time 23:31:19
 */
public class ConvertJSONFiletoXMLFile extends TemporaryFileCreatingTask<Path> implements ShortRunningTask, AutomationTask {

	/** Input and output files. */
	private final Path inputJsonFile, outputXmlFile;

	/** Request id. */
	private int requestId = -1;

	/** Automation client handler. */
	private AutomationClientHandler automationClientHandler = null;

	/**
	 * Creates convert JSON file to XML file process.
	 *
	 * @param inputJsonFile
	 *            Input JSON file.
	 * @param outputXmlFile
	 *            Output XML file.
	 */
	public ConvertJSONFiletoXMLFile(Path inputJsonFile, Path outputXmlFile) {
		this.inputJsonFile = inputJsonFile;
		this.outputXmlFile = outputXmlFile;
	}

	@Override
	public void setAutomationClientHandler(AutomationClientHandler automationClientHandler, int requestId) {
		this.automationClientHandler = automationClientHandler;
		this.requestId = requestId;
	}

	@Override
	public String getTaskTitle() {
		return "Convert JSON file to XML file";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Path call() throws Exception {
		return new ConvertJSONtoXML(this, inputJsonFile, outputXmlFile).start(null);
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// notify client handler of task completion (if set)
		if (automationClientHandler != null) {
			automationClientHandler.taskCompleted(requestId);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// notify client handler of task failure (if set)
		if (automationClientHandler != null) {
			automationClientHandler.taskFailed(requestId);
		}
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// notify client handler of task failure (if set)
		if (automationClientHandler != null) {
			automationClientHandler.taskFailed(requestId);
		}
	}
}