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
package equinox.process.automation;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;
import org.json.XML;

import equinox.plugin.FileType;
import equinox.process.EquinoxProcess;
import equinox.task.TemporaryFileCreatingTask;

/**
 * Class for convert XML to JSON file process.
 *
 * @author Murat Artim
 * @date 12 Sep 2018
 * @time 23:08:04
 */
public class ConvertXMLtoJSON implements EquinoxProcess<Path> {

	/** Indent factor. */
	private static final int PRETTY_PRINT_INDENT_FACTOR = 4;

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task;

	/** Input and output files. */
	private Path inputXmlFile, outputJsonFile;

	/**
	 * Creates convert JSON file to XML file process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputXmlFile
	 *            Input XML file.
	 * @param outputJsonFile
	 *            Output JSON file. Can be null to output to a temporary file.
	 */
	public ConvertXMLtoJSON(TemporaryFileCreatingTask<?> task, Path inputXmlFile, Path outputJsonFile) {
		this.task = task;
		this.inputXmlFile = inputXmlFile;
		this.outputJsonFile = outputJsonFile;
	}

	@Override
	public Path start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// create path to output JSON file
		if (outputJsonFile == null) {
			outputJsonFile = task.getWorkingDirectory().resolve(FileType.getNameWithoutExtension(inputXmlFile) + ".json");
		}

		// read input file and create JSON object
		task.updateMessage("Reading input XML file...");
		String xmlString = new String(Files.readAllBytes(inputXmlFile));
		JSONObject jsonObject = XML.toJSONObject(xmlString);

		// task cancelled
		if (task.isCancelled())
			return null;

		// write JSON object to JSON file
		task.updateMessage("Writing to JSON file...");
		try (FileWriter fileWriter = new FileWriter(outputJsonFile.toFile())) {
			fileWriter.write(jsonObject.toString(PRETTY_PRINT_INDENT_FACTOR));
		}

		// return output JSON file
		return outputJsonFile;
	}
}