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
import java.nio.file.Paths;
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

/**
 * Class for read generate stress sequence input process.
 *
 * @author Murat Artim
 * @date 18 Aug 2018
 * @time 09:17:00
 */
public class ReadGenerateStressSequenceInput implements EquinoxProcess<GenerateStressSequenceInput> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task;

	/** Input file. */
	private Path inputFile;

	/**
	 * Creates read generate stress sequence input process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input XML/JSON file.
	 */
	public ReadGenerateStressSequenceInput(TemporaryFileCreatingTask<?> task, Path inputFile) {
		this.task = task;
		this.inputFile = inputFile;
	}

	@Override
	public GenerateStressSequenceInput start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// input file is JSON
		if (FileType.getFileType(inputFile.toFile()).equals(FileType.JSON)) {

			// convert to XML file
			task.updateMessage("Converting input JSON file to XML file...");
			inputFile = new ConvertJSONtoXML(task, inputFile, null).start(null);
			task.setFileAsPermanent(inputFile);
		}

		// read input file
		task.updateMessage("Reading generate stress sequence input XML file...");

		// create input
		GenerateStressSequenceInput input = new GenerateStressSequenceInput();

		// get root input element
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document = saxBuilder.build(inputFile.toUri().toString());
		Element generateStressSequenceInput = document.getRootElement();

		// overall factors
		if (generateStressSequenceInput.getChild("overallFactors") != null) {
			overallFactors(generateStressSequenceInput, input);
		}

		// loadcase factors
		if (generateStressSequenceInput.getChild("loadcaseFactors") != null) {
			loadcaseFactors(generateStressSequenceInput, input);
		}

		// pressure
		if (generateStressSequenceInput.getChild("pressure") != null) {
			pressure(generateStressSequenceInput, input);
		}

		// temperature
		if (generateStressSequenceInput.getChild("temperature") != null) {
			temperature(generateStressSequenceInput, input);
		}

		// stress rotation
		if (generateStressSequenceInput.getChild("stressRotation") != null) {
			stressRotation(generateStressSequenceInput, input);
		}

		// sequence name
		if (generateStressSequenceInput.getChild("sequenceName") != null) {
			String sequenceName = generateStressSequenceInput.getChildTextNormalize("sequenceName");
			input.setFileName(sequenceName);
		}

		// return input
		return input;
	}

	/**
	 * Reads stress rotation information from the input file.
	 *
	 * @param generateStressSequenceInput
	 *            Root input element.
	 * @param input
	 *            Input data to add factors to.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void stressRotation(Element generateStressSequenceInput, GenerateStressSequenceInput input) throws Exception {

		// update info
		task.updateMessage("Reading stress rotation info...");

		// get inputs
		Element stressRotation = generateStressSequenceInput.getChild("stressRotation");
		StressComponent comp = StressComponent.getStressComponent(stressRotation.getChild("component").getTextNormalize());
		input.setStressComponent(comp);
		if (comp.equals(StressComponent.ROTATED)) {
			input.setRotationAngle(Double.parseDouble(stressRotation.getChild("rotationAngle").getTextNormalize()));
		}
	}

	/**
	 * Reads temperature information from the input file.
	 *
	 * @param generateStressSequenceInput
	 *            Root input element.
	 * @param input
	 *            Input data to add factors to.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void temperature(Element generateStressSequenceInput, GenerateStressSequenceInput input) throws Exception {

		// update info
		task.updateMessage("Reading temparature info...");

		// get temperature
		Element temperature = generateStressSequenceInput.getChild("temperature");

		// set interpolation
		DTInterpolation dtInterpolation = DTInterpolation.getDTInterpolation(temperature.getChild("dtInterpolation").getTextNormalize());
		input.setDTInterpolation(dtInterpolation);

		// set superior loadcase and reference delta-t
		input.setDTLoadcaseSup(temperature.getChild("dtLoadcaseSuperior").getTextNormalize());
		input.setReferenceDTSup(Double.parseDouble(temperature.getChild("referenceDtSuperior").getTextNormalize()));

		// set inferior loadcase and reference delta-t
		if (dtInterpolation.equals(DTInterpolation.TWO_POINTS)) {
			input.setDTLoadcaseInf(temperature.getChild("dtLoadcaseInferior").getTextNormalize());
			input.setReferenceDTInf(Double.parseDouble(temperature.getChild("referenceDtInferior").getTextNormalize()));
		}
	}

	/**
	 * Reads cabin pressure from the input file.
	 *
	 * @param generateStressSequenceInput
	 *            Root input element.
	 * @param input
	 *            Input data to add factors to.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void pressure(Element generateStressSequenceInput, GenerateStressSequenceInput input) throws Exception {

		// update info
		task.updateMessage("Reading cabin pressure...");

		// get factors
		Element pressure = generateStressSequenceInput.getChild("pressure");
		if (pressure.getChild("dpLoadcase") != null) {
			String dpLoadcase = pressure.getChild("dpLoadcase").getTextNormalize();
			input.setDPLoadcase(dpLoadcase);
		}
		if (pressure.getChild("referenceDp") != null) {
			double referenceDp = Double.parseDouble(pressure.getChild("referenceDp").getTextNormalize());
			input.setReferenceDP(referenceDp);
		}
	}

	/**
	 * Reads loadcase factors from the input file.
	 *
	 * @param generateStressSequenceInput
	 *            Root input element.
	 * @param input
	 *            Input data to add factors to.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadcaseFactors(Element generateStressSequenceInput, GenerateStressSequenceInput input) throws Exception {

		// update info
		task.updateMessage("Reading loadcase factors...");

		// get loadcase factors element
		Element loadcaseFactors = generateStressSequenceInput.getChild("loadcaseFactors");

		// get inputs
		Path mutPath = Paths.get(loadcaseFactors.getChild("mutPath").getTextNormalize());
		int tableColumn = Integer.parseInt(loadcaseFactors.getChild("tableColumn").getTextNormalize());
		String method = loadcaseFactors.getChild("method").getTextNormalize();

		// set loadcase factors
		input.setLoadcaseFactors(new ReadMultiplicationTable(task, mutPath, tableColumn, method).start(null));
	}

	/**
	 * Reads overall factors from the input file.
	 *
	 * @param generateStressSequenceInput
	 *            Root input element.
	 * @param input
	 *            Input data to add factors to.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void overallFactors(Element generateStressSequenceInput, GenerateStressSequenceInput input) throws Exception {

		// update info
		task.updateMessage("Reading overall factors...");

		// get factors
		Element overallFactors = generateStressSequenceInput.getChild("overallFactors");
		if (overallFactors.getChild("oneg") != null) {
			double value = Double.parseDouble(overallFactors.getChild("oneg").getChild("value").getTextNormalize());
			String method = overallFactors.getChild("oneg").getChild("method").getTextNormalize();
			input.setStressModifier(GenerateStressSequenceInput.ONEG, value, method);
		}
		if (overallFactors.getChild("increment") != null) {
			double value = Double.parseDouble(overallFactors.getChild("increment").getChild("value").getTextNormalize());
			String method = overallFactors.getChild("increment").getChild("method").getTextNormalize();
			input.setStressModifier(GenerateStressSequenceInput.INCREMENT, value, method);
		}
		if (overallFactors.getChild("deltaP") != null) {
			double value = Double.parseDouble(overallFactors.getChild("deltaP").getChild("value").getTextNormalize());
			String method = overallFactors.getChild("deltaP").getChild("method").getTextNormalize();
			input.setStressModifier(GenerateStressSequenceInput.DELTAP, value, method);
		}
		if (overallFactors.getChild("deltaT") != null) {
			double value = Double.parseDouble(overallFactors.getChild("deltaT").getChild("value").getTextNormalize());
			String method = overallFactors.getChild("deltaT").getChild("method").getTextNormalize();
			input.setStressModifier(GenerateStressSequenceInput.DELTAT, value, method);
		}
	}
}