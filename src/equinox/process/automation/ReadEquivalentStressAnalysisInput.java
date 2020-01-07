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
import java.sql.ResultSet;
import java.sql.Statement;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import equinox.data.IsamiVersion;
import equinox.data.input.EquivalentStressInput;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.LinearMaterial;
import equinox.dataServer.remote.data.PreffasMaterial;
import equinox.plugin.FileType;
import equinox.process.EquinoxProcess;
import equinox.task.TemporaryFileCreatingTask;

/**
 * Class for read equivalent stress analysis input process.
 *
 * @author Murat Artim
 * @date 28 Aug 2018
 * @time 16:46:39
 */
public class ReadEquivalentStressAnalysisInput implements EquinoxProcess<EquivalentStressInput> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task;

	/** Input file. */
	private Path inputFile;

	/** ISAMI version. */
	private final IsamiVersion isamiVersion;

	/**
	 * Creates read equivalent stress analysis input process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input XML/JSON file.
	 * @param isamiVersion
	 *            ISAMI version.
	 */
	public ReadEquivalentStressAnalysisInput(TemporaryFileCreatingTask<?> task, Path inputFile, IsamiVersion isamiVersion) {
		this.task = task;
		this.inputFile = inputFile;
		this.isamiVersion = isamiVersion;
	}

	@Override
	public EquivalentStressInput start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// input file is JSON
		if (FileType.getFileType(inputFile.toFile()).equals(FileType.JSON)) {

			// convert to XML file
			task.updateMessage("Converting input JSON file to XML file...");
			inputFile = new ConvertJSONtoXML(task, inputFile, null).start(null);
			task.setFileAsPermanent(inputFile);
		}

		// read input file
		task.updateMessage("Reading equivalent stress analysis input XML file...");

		// create input
		EquivalentStressInput input = new EquivalentStressInput();

		// get root input element
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document = saxBuilder.build(inputFile.toUri().toString());
		Element equivalentStressAnalysisInput = document.getRootElement();

		// material
		material(equivalentStressAnalysisInput, input, connection);

		// omission
		omission(equivalentStressAnalysisInput, input);

		// stress modifier
		stressModifier(equivalentStressAnalysisInput, input);

		// return input
		return input;
	}

	/**
	 * Gets stress modifier inputs.
	 *
	 * @param equivalentStressAnalysisInput
	 *            Root input element.
	 * @param input
	 *            Analysis input.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void stressModifier(Element equivalentStressAnalysisInput, EquivalentStressInput input) throws Exception {

		// update info
		task.updateMessage("Reading omission...");

		// stress modifier inputs given
		if (equivalentStressAnalysisInput.getChild("stressModifier") != null) {

			// get stress modifier element
			Element stressModifier = equivalentStressAnalysisInput.getChild("stressModifier");

			// modification value and method
			double value = Double.parseDouble(stressModifier.getChildTextNormalize("value"));
			String method = stressModifier.getChildTextNormalize("method");

			// set to input
			input.setStressModifier(value, method);
		}
	}

	/**
	 * Gets omission inputs.
	 *
	 * @param equivalentStressAnalysisInput
	 *            Root input element.
	 * @param input
	 *            Analysis input.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void omission(Element equivalentStressAnalysisInput, EquivalentStressInput input) throws Exception {

		// update info
		task.updateMessage("Reading omission...");

		// initialize default values
		boolean removeNegativeStresses = false;
		boolean applyOmission = true;
		double omissionLevel = 0.0;

		// omission inputs given
		if (equivalentStressAnalysisInput.getChild("omission") != null) {

			// get omission element
			Element omission = equivalentStressAnalysisInput.getChild("omission");

			// remove negative stress
			if (omission.getChild("removeNegativeStress") != null) {
				removeNegativeStresses = Boolean.parseBoolean(omission.getChildTextNormalize("removeNegativeStress"));
			}

			// omission level
			if (omission.getChild("omissionLevel") != null) {
				applyOmission = true;
				omissionLevel = Double.parseDouble(omission.getChildTextNormalize("omissionLevel"));
			}
		}

		// set to all inputs
		input.setRemoveNegativeStresses(removeNegativeStresses);
		input.setApplyOmission(applyOmission);
		input.setOmissionLevel(omissionLevel);
	}

	/**
	 * Gets material inputs.
	 *
	 * @param equivalentStressAnalysisInput
	 *            Root input element.
	 * @param input
	 *            Analysis input.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void material(Element equivalentStressAnalysisInput, EquivalentStressInput input, Connection connection) throws Exception {

		// update info
		task.updateMessage("Reading material...");

		// get material element
		Element materialElement = equivalentStressAnalysisInput.getChild("material");

		// get inputs
		String analysisType = materialElement.getChildTextNormalize("analysisType");
		String name = materialElement.getChildTextNormalize("name");
		String specification = materialElement.getChildTextNormalize("specification");
		String orientation = materialElement.getChildTextNormalize("orientation");
		String configuration = materialElement.getChildTextNormalize("configuration");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// fatigue
			if (analysisType.equals("fatigue")) {
				input.setMaterial(getFatigueMaterial(name, specification, orientation, configuration, statement));
			}

			// preffas
			else if (analysisType.equals("preffas")) {
				input.setMaterial(getPreffasMaterial(name, specification, orientation, configuration, statement));
			}

			// linear
			else if (analysisType.equals("linear")) {
				input.setMaterial(getLinearMaterial(name, specification, orientation, configuration, statement));
			}
		}
	}

	/**
	 * Retrieves linear material from database.
	 *
	 * @param name
	 *            Material name.
	 * @param specification
	 *            Material specification.
	 * @param orientation
	 *            Material orientation.
	 * @param configuration
	 *            Material configuration.
	 * @param statement
	 *            Database statement to get material.
	 * @return Linear material.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private LinearMaterial getLinearMaterial(String name, String specification, String orientation, String configuration, Statement statement) throws Exception {

		// initialize material
		LinearMaterial material = null;

		// execute statement
		String sql = "select * from linear_materials where ";
		sql += "name = '" + name + "' and ";
		sql += "specification = '" + specification + "' and ";
		sql += "orientation = '" + orientation + "' and ";
		sql += "configuration = '" + configuration + "' and ";
		sql += "isami_version = '" + isamiVersion.getIsamiVersion() + "'";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			if (resultSet.next()) {
				material = new LinearMaterial(resultSet.getInt("id"));
				material.setName(resultSet.getString("name"));
				material.setSpecification(resultSet.getString("specification"));
				material.setLibraryVersion(resultSet.getString("library_version"));
				material.setFamily(resultSet.getString("family"));
				material.setOrientation(resultSet.getString("orientation"));
				material.setConfiguration(resultSet.getString("configuration"));
				material.setCeff(resultSet.getDouble("par_ceff"));
				material.setM(resultSet.getDouble("par_m"));
				material.setA(resultSet.getDouble("par_a"));
				material.setB(resultSet.getDouble("par_b"));
				material.setC(resultSet.getDouble("par_c"));
				material.setFtu(resultSet.getDouble("par_ftu"));
				material.setFty(resultSet.getDouble("par_fty"));
				material.setIsamiVersion(resultSet.getString("isami_version"));
			}
		}

		// return material
		return material;
	}

	/**
	 * Retrieves preffas material from database.
	 *
	 * @param name
	 *            Material name.
	 * @param specification
	 *            Material specification.
	 * @param orientation
	 *            Material orientation.
	 * @param configuration
	 *            Material configuration.
	 * @param statement
	 *            Database statement to get material.
	 * @return Preffas material.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private PreffasMaterial getPreffasMaterial(String name, String specification, String orientation, String configuration, Statement statement) throws Exception {

		// initialize material
		PreffasMaterial material = null;

		// execute statement
		String sql = "select * from preffas_materials where ";
		sql += "name = '" + name + "' and ";
		sql += "specification = '" + specification + "' and ";
		sql += "orientation = '" + orientation + "' and ";
		sql += "configuration = '" + configuration + "' and ";
		sql += "isami_version = '" + isamiVersion.getIsamiVersion() + "'";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			if (resultSet.next()) {
				material = new PreffasMaterial(resultSet.getInt("id"));
				material.setName(resultSet.getString("name"));
				material.setSpecification(resultSet.getString("specification"));
				material.setLibraryVersion(resultSet.getString("library_version"));
				material.setFamily(resultSet.getString("family"));
				material.setOrientation(resultSet.getString("orientation"));
				material.setConfiguration(resultSet.getString("configuration"));
				material.setCeff(resultSet.getDouble("par_ceff"));
				material.setM(resultSet.getDouble("par_m"));
				material.setA(resultSet.getDouble("par_a"));
				material.setB(resultSet.getDouble("par_b"));
				material.setC(resultSet.getDouble("par_c"));
				material.setFtu(resultSet.getDouble("par_ftu"));
				material.setFty(resultSet.getDouble("par_fty"));
				material.setIsamiVersion(resultSet.getString("isami_version"));
			}
		}

		// return material
		return material;
	}

	/**
	 * Retrieves fatigue material from database.
	 *
	 * @param name
	 *            Material name.
	 * @param specification
	 *            Material specification.
	 * @param orientation
	 *            Material orientation.
	 * @param configuration
	 *            Material configuration.
	 * @param statement
	 *            Database statement to get material.
	 * @return Fatigue material.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private FatigueMaterial getFatigueMaterial(String name, String specification, String orientation, String configuration, Statement statement) throws Exception {

		// initialize material
		FatigueMaterial material = null;

		// execute statement
		String sql = "select * from fatigue_materials where ";
		sql += "name = '" + name + "' and ";
		sql += "specification = '" + specification + "' and ";
		sql += "orientation = '" + orientation + "' and ";
		sql += "configuration = '" + configuration + "' and ";
		sql += "isami_version = '" + isamiVersion.getIsamiVersion() + "'";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			if (resultSet.next()) {
				material = new FatigueMaterial(resultSet.getInt("id"));
				material.setName(resultSet.getString("name"));
				material.setSpecification(resultSet.getString("specification"));
				material.setLibraryVersion(resultSet.getString("library_version"));
				material.setFamily(resultSet.getString("family"));
				material.setOrientation(resultSet.getString("orientation"));
				material.setConfiguration(resultSet.getString("configuration"));
				material.setP(resultSet.getDouble("par_p"));
				material.setQ(resultSet.getDouble("par_q"));
				material.setM(resultSet.getDouble("par_m"));
				material.setIsamiVersion(resultSet.getString("isami_version"));
			}
		}

		// return material
		return material;
	}
}