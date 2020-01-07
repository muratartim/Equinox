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

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import equinox.data.DTInterpolation;
import equinox.data.StressComponent;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.plugin.FileType;
import equinox.process.EquinoxProcess;
import equinox.task.TemporaryFileCreatingTask;
import equinox.utility.XMLUtilities;

/**
 * Class for check generate stress sequence input process.
 *
 * @author Murat Artim
 * @date 21 Aug 2018
 * @time 09:47:31
 */
public class CheckGenerateStressSequenceInput implements EquinoxProcess<Boolean> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task;

	/** Input file. */
	private Path inputFile;

	/**
	 * Creates check generate stress sequence input process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input XML/JSON file.
	 */
	public CheckGenerateStressSequenceInput(TemporaryFileCreatingTask<?> task, Path inputFile) {
		this.task = task;
		this.inputFile = inputFile;
	}

	@Override
	public Boolean start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// input file is JSON
		if (FileType.getFileType(inputFile.toFile()).equals(FileType.JSON)) {

			// convert to XML file
			task.updateMessage("Converting input JSON file to XML file...");
			inputFile = new ConvertJSONtoXML(task, inputFile, null).start(connection, preparedStatements);
			task.setFileAsPermanent(inputFile);
		}

		// read input file
		task.updateMessage("Checking generate stress sequence input XML file...");

		// get root input element
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document = saxBuilder.build(inputFile.toUri().toString());
		Element generateStressSequenceInput = document.getRootElement();

		// cannot find root input element
		if (generateStressSequenceInput == null) {
			task.addWarning("Cannot locate root input element 'generateStressSequenceInput' in generate stress sequence input XML file '" + inputFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// overall factors
		if (generateStressSequenceInput.getChild("overallFactors") != null) {
			if (!checkOverallFactors(generateStressSequenceInput))
				return false;
		}

		// loadcase factors
		if (generateStressSequenceInput.getChild("loadcaseFactors") != null) {
			if (!checkLoadcaseFactors(generateStressSequenceInput))
				return false;
		}

		// pressure
		if (generateStressSequenceInput.getChild("pressure") != null) {
			if (!checkPressure(generateStressSequenceInput))
				return false;
		}

		// temperature
		if (generateStressSequenceInput.getChild("temperature") != null) {
			if (!checkTemperature(generateStressSequenceInput))
				return false;
		}

		// stress rotation
		if (generateStressSequenceInput.getChild("stressRotation") != null) {
			if (!checkStressRotation(generateStressSequenceInput))
				return false;
		}

		// sequence name
		if (!XMLUtilities.checkStringValue(task, inputFile, generateStressSequenceInput, "seqeunceName", true))
			return false;

		// check passed
		return true;
	}

	/**
	 * Returns true if <code>stressRotation</code> element passes checks.
	 *
	 * @param generateStressSequenceInput
	 *            Root input element.
	 * @return True if <code>stressRotation</code> element passes checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkStressRotation(Element generateStressSequenceInput) throws Exception {

		// update info
		task.updateMessage("Checking stress rotation...");

		// get parent
		Element stressRotation = generateStressSequenceInput.getChild("stressRotation");

		// check component
		if (!XMLUtilities.checkStringValue(task, inputFile, stressRotation, "component", true, XMLUtilities.getStringArray(StressComponent.values())))
			return false;

		// check rotation angle
		StressComponent comp = StressComponent.getStressComponent(stressRotation.getChild("component").getTextNormalize());
		if (comp.equals(StressComponent.ROTATED)) {
			if (!XMLUtilities.checkDoubleValue(task, inputFile, stressRotation, "rotationAngle", false, null, null))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if <code>temperature</code> element passes checks.
	 *
	 * @param generateStressSequenceInput
	 *            Root input element.
	 * @return True if <code>temperature</code> element passes checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkTemperature(Element generateStressSequenceInput) throws Exception {

		// update info
		task.updateMessage("Checking temperature info...");

		// get parent
		Element temperature = generateStressSequenceInput.getChild("temperature");

		// check interpolation
		if (!XMLUtilities.checkStringValue(task, inputFile, temperature, "dtInterpolation", false, XMLUtilities.getStringArray(DTInterpolation.values())))
			return false;

		// check superior loadcase and reference delta-t
		if (!XMLUtilities.checkStringValue(task, inputFile, temperature, "dtLoadcaseSuperior", false))
			return false;
		if (!XMLUtilities.checkDoubleValue(task, inputFile, temperature, "referenceDtSuperior", false, null, null))
			return false;

		// set inferior loadcase and reference delta-t
		DTInterpolation dtInterpolation = DTInterpolation.getDTInterpolation(temperature.getChild("dtInterpolation").getTextNormalize());
		if (dtInterpolation.equals(DTInterpolation.TWO_POINTS)) {
			if (!XMLUtilities.checkStringValue(task, inputFile, temperature, "dtLoadcaseInferior", false))
				return false;
			if (!XMLUtilities.checkDoubleValue(task, inputFile, temperature, "referenceDtInferior", false, null, null))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if <code>pressure</code> element passes checks.
	 *
	 * @param generateStressSequenceInput
	 *            Root input element.
	 * @return True if <code>pressure</code> element passes checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkPressure(Element generateStressSequenceInput) throws Exception {

		// update info
		task.updateMessage("Checking cabin pressure...");

		// get parent
		Element pressure = generateStressSequenceInput.getChild("pressure");

		// check delta-p loadcase
		if (!XMLUtilities.checkStringValue(task, inputFile, pressure, "dpLoadcase", true))
			return false;

		// check reference delta-p
		if (!XMLUtilities.checkDoubleValue(task, inputFile, pressure, "referenceDp", true, null, null))
			return false;

		// check passed
		return true;
	}

	/**
	 * Returns true if <code>loadcaseFactors</code> element passes checks.
	 *
	 * @param generateStressSequenceInput
	 *            Root input element.
	 * @return True if <code>loadcaseFactors</code> element passes checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkLoadcaseFactors(Element generateStressSequenceInput) throws Exception {

		// update info
		task.updateMessage("Checking loadcase factors...");

		// get parent
		Element loadcaseFactors = generateStressSequenceInput.getChild("loadcaseFactors");

		// check MUT file path
		if (!XMLUtilities.checkInputPathValue(task, inputFile, loadcaseFactors, "mutPath", false, FileType.MUT))
			return false;

		// check table column
		if (!XMLUtilities.checkIntegerValue(task, inputFile, loadcaseFactors, "tableColumn", false, null, null))
			return false;

		// check method
		if (!XMLUtilities.checkStringValue(task, inputFile, loadcaseFactors, "method", false, GenerateStressSequenceInput.MULTIPLY, GenerateStressSequenceInput.ADD, GenerateStressSequenceInput.SET))
			return false;

		// check passed
		return true;
	}

	/**
	 * Returns true if <code>overallFactors</code> element passes checks.
	 *
	 * @param generateStressSequenceInput
	 *            Root input element.
	 * @return True if <code>overallFactors</code> element passes checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkOverallFactors(Element generateStressSequenceInput) throws Exception {

		// update info
		task.updateMessage("Checking overall factors...");

		// get parent
		Element overallFactors = generateStressSequenceInput.getChild("overallFactors");

		// check 1g
		if (overallFactors.getChild("oneg") != null) {
			if (!XMLUtilities.checkDoubleValue(task, inputFile, overallFactors.getChild("oneg"), "value", false, null, null))
				return false;
			if (!XMLUtilities.checkStringValue(task, inputFile, overallFactors.getChild("oneg"), "method", false, GenerateStressSequenceInput.MULTIPLY, GenerateStressSequenceInput.ADD, GenerateStressSequenceInput.SET))
				return false;
		}

		// check increment
		if (overallFactors.getChild("increment") != null) {
			if (!XMLUtilities.checkDoubleValue(task, inputFile, overallFactors.getChild("increment"), "value", false, null, null))
				return false;
			if (!XMLUtilities.checkStringValue(task, inputFile, overallFactors.getChild("increment"), "method", false, GenerateStressSequenceInput.MULTIPLY, GenerateStressSequenceInput.ADD, GenerateStressSequenceInput.SET))
				return false;
		}

		// check delta-p
		if (overallFactors.getChild("deltaP") != null) {
			if (!XMLUtilities.checkDoubleValue(task, inputFile, overallFactors.getChild("deltaP"), "value", false, null, null))
				return false;
			if (!XMLUtilities.checkStringValue(task, inputFile, overallFactors.getChild("deltaP"), "method", false, GenerateStressSequenceInput.MULTIPLY, GenerateStressSequenceInput.ADD, GenerateStressSequenceInput.SET))
				return false;
		}

		// check delta-t
		if (overallFactors.getChild("deltaT") != null) {
			if (!XMLUtilities.checkDoubleValue(task, inputFile, overallFactors.getChild("deltaT"), "value", false, null, null))
				return false;
			if (!XMLUtilities.checkStringValue(task, inputFile, overallFactors.getChild("deltaT"), "method", false, GenerateStressSequenceInput.MULTIPLY, GenerateStressSequenceInput.ADD, GenerateStressSequenceInput.SET))
				return false;
		}

		// check passed
		return true;
	}
}