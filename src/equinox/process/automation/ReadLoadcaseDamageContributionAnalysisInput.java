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
import java.util.ArrayList;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import equinox.data.DamageContribution;
import equinox.data.IsamiVersion;
import equinox.data.LoadcaseFactor;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.data.input.LoadcaseDamageContributionInput;
import equinox.dataServer.remote.data.ContributionType;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.plugin.FileType;
import equinox.process.EquinoxProcess;
import equinox.task.TemporaryFileCreatingTask;

/**
 * Class for read loadcase damage contribution analysis input process.
 *
 * @author Murat Artim
 * @date 13 Sep 2018
 * @time 14:06:24
 */
public class ReadLoadcaseDamageContributionAnalysisInput implements EquinoxProcess<LoadcaseDamageContributionInput> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task;

	/** Input file. */
	private Path inputFile;

	/** ISAMI version. */
	private final IsamiVersion isamiVersion;

	/**
	 * Creates read generate stress sequence input process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input XML/JSON file.
	 * @param isamiVersion
	 *            ISAMI version.
	 */
	public ReadLoadcaseDamageContributionAnalysisInput(TemporaryFileCreatingTask<?> task, Path inputFile, IsamiVersion isamiVersion) {
		this.task = task;
		this.inputFile = inputFile;
		this.isamiVersion = isamiVersion;
	}

	@Override
	public LoadcaseDamageContributionInput start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// input file is JSON
		if (FileType.getFileType(inputFile.toFile()).equals(FileType.JSON)) {

			// convert to XML file
			task.updateMessage("Converting input JSON file to XML file...");
			inputFile = new ConvertJSONtoXML(task, inputFile, null).start(null);
			task.setFileAsPermanent(inputFile);
		}

		// read input file
		task.updateMessage("Reading loadcase damage contribution analysis input XML file...");

		// create input
		LoadcaseDamageContributionInput input = new LoadcaseDamageContributionInput();

		// get root input element
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document = saxBuilder.build(inputFile.toUri().toString());
		Element loadcaseDamageContributionAnalysisInput = document.getRootElement();

		// material
		material(loadcaseDamageContributionAnalysisInput, input, connection);

		// omission
		omission(loadcaseDamageContributionAnalysisInput, input);

		// contributions
		contributions(loadcaseDamageContributionAnalysisInput, input);

		// return input
		return input;
	}

	/**
	 * Gets contribution inputs.
	 *
	 * @param loadcaseDamageContributionAnalysisInput
	 *            Root input element.
	 * @param input
	 *            Analysis input.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void contributions(Element loadcaseDamageContributionAnalysisInput, LoadcaseDamageContributionInput input) throws Exception {

		// update info
		task.updateMessage("Reading loadcase contributions...");

		// get contributions element
		Element loadcaseContributions = loadcaseDamageContributionAnalysisInput.getChild("loadcaseContributions");

		// loop over steady contributions
		for (Element steadyContribution : loadcaseContributions.getChildren("steadyContribution")) {

			// get contribution name
			String contributionName = steadyContribution.getTextNormalize();

			// add steady contribution
			for (ContributionType type : ContributionType.values()) {
				if (type.getName().equals(contributionName)) {
					input.addContribution(new DamageContribution(contributionName, null, type));
					break;
				}
			}
		}

		// loop over increment contributions
		for (Element incrementContributionGroup : loadcaseContributions.getChildren("incrementContributionGroup")) {

			// get group name
			String groupName = incrementContributionGroup.getChildTextNormalize("groupName");
			ArrayList<LoadcaseFactor> loadcaseFactors = new ArrayList<>();

			// loop over loadcase numbers
			for (Element loadcaseNumber : incrementContributionGroup.getChildren("loadcaseNumber")) {
				LoadcaseFactor lf = new LoadcaseFactor();
				lf.setIsOneg(false);
				lf.setLoadcaseNumber(loadcaseNumber.getTextNormalize());
				lf.setEventName(null);
				lf.setComments(null);
				lf.setModifier(GenerateStressSequenceInput.MULTIPLY, 0.0);
				loadcaseFactors.add(lf);
			}

			// add increment contribution
			input.addContribution(new DamageContribution(groupName, loadcaseFactors, ContributionType.INCREMENT));
		}
	}

	/**
	 * Gets omission inputs.
	 *
	 * @param loadcaseDamageContributionAnalysisInput
	 *            Root input element.
	 * @param input
	 *            Analysis input.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void omission(Element loadcaseDamageContributionAnalysisInput, LoadcaseDamageContributionInput input) throws Exception {

		// update info
		task.updateMessage("Reading omission...");

		// initialize default values
		boolean removeNegativeStresses = false;
		boolean applyOmission = true;
		double omissionLevel = 0.0;

		// omission inputs given
		if (loadcaseDamageContributionAnalysisInput.getChild("omission") != null) {

			// get omission element
			Element omission = loadcaseDamageContributionAnalysisInput.getChild("omission");

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
	 * @param loadcaseDamageContributionAnalysisInput
	 *            Root input element.
	 * @param input
	 *            Analysis input.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void material(Element loadcaseDamageContributionAnalysisInput, LoadcaseDamageContributionInput input, Connection connection) throws Exception {

		// update info
		task.updateMessage("Reading material...");

		// get material element
		Element materialElement = loadcaseDamageContributionAnalysisInput.getChild("material");

		// get inputs
		String name = materialElement.getChildTextNormalize("name");
		String specification = materialElement.getChildTextNormalize("specification");
		String orientation = materialElement.getChildTextNormalize("orientation");
		String configuration = materialElement.getChildTextNormalize("configuration");

		// create statement
		try (Statement statement = connection.createStatement()) {
			input.setMaterial(getFatigueMaterial(name, specification, orientation, configuration, statement));
		}
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