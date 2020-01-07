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

import equinox.data.IsamiVersion;
import equinox.dataServer.remote.data.ContributionType;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.Material;
import equinox.plugin.FileType;
import equinox.process.EquinoxProcess;
import equinox.task.TemporaryFileCreatingTask;
import equinox.utility.XMLUtilities;

/**
 * Class for check loadcase damage contribution analysis input process.
 *
 * @author Murat Artim
 * @date 13 Sep 2018
 * @time 23:18:25
 */
public class CheckLoadcaseDamageContributionAnalysisInput implements EquinoxProcess<Boolean> {

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task;

	/** Input file. */
	private Path inputFile;

	/** ISAMI version. */
	private final IsamiVersion isamiVersion;

	/**
	 * Creates check loadcase damage contribution analysis input process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input XML/JSON file.
	 * @param isamiVersion
	 *            ISAMI version.
	 */
	public CheckLoadcaseDamageContributionAnalysisInput(TemporaryFileCreatingTask<?> task, Path inputFile, IsamiVersion isamiVersion) {
		this.task = task;
		this.inputFile = inputFile;
		this.isamiVersion = isamiVersion;
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
		task.updateMessage("Checking loadcase damage contribution analysis input XML file...");

		// get root input element
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document = saxBuilder.build(inputFile.toUri().toString());
		Element loadcaseDamageContributionAnalysisInput = document.getRootElement();

		// cannot find root input element
		if (loadcaseDamageContributionAnalysisInput == null) {
			task.addWarning("Cannot locate root input element 'loadcaseDamageContributionAnalysisInput' in loadcase damage contribution analysis input XML file '" + inputFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// material
		if (!checkMaterial(loadcaseDamageContributionAnalysisInput, connection))
			return false;

		// omission
		if (!checkOmission(loadcaseDamageContributionAnalysisInput))
			return false;

		// contributions
		if (!checkContributions(loadcaseDamageContributionAnalysisInput))
			return false;

		// check passed
		return true;
	}

	/**
	 * Returns true if <code>loadcaseContributions</code> element passes checks.
	 *
	 * @param loadcaseDamageContributionAnalysisInput
	 *            Root input element.
	 * @return True if <code>loadcaseContributions</code> element passes checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkContributions(Element loadcaseDamageContributionAnalysisInput) throws Exception {

		// update info
		task.updateMessage("Checking loadcase damage contributions...");

		// no loadcaseContributions element found
		if (loadcaseDamageContributionAnalysisInput.getChild("loadcaseContributions") == null) {
			task.addWarning("Cannot locate element 'loadcaseContributions' under " + XMLUtilities.getFamilyTree(loadcaseDamageContributionAnalysisInput) + " in instruction set '" + inputFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get contributions element
		Element loadcaseContributions = loadcaseDamageContributionAnalysisInput.getChild("loadcaseContributions");

		// check steady contributions
		if (!XMLUtilities.checkStringValues(task, inputFile, loadcaseContributions, "steadyContribution", 0, true, XMLUtilities.getStringArray(ContributionType.values())))
			return false;

		// loop over increment contributions
		ArrayList<String> groupNames = new ArrayList<>();
		for (Element incrementContributionGroup : loadcaseContributions.getChildren("incrementContributionGroup")) {

			// check group name
			if (!XMLUtilities.checkStringValue(task, inputFile, incrementContributionGroup, "groupName", false))
				return false;

			// group name not distinct
			String groupName = incrementContributionGroup.getChildTextNormalize("groupName");
			if (groupNames.contains(groupName)) {
				task.addWarning("Increment damage contribution group name '" + groupName + "' appears more than once for element " + XMLUtilities.getFamilyTree(loadcaseContributions) + " in instruction set '" + inputFile.toString() + "'. Check failed.");
				return false;
			}

			// check loadcase numbers
			if (!XMLUtilities.checkStringValues(task, inputFile, incrementContributionGroup, "loadcaseNumber", 1, true))
				return false;

			// add to group names
			groupNames.add(groupName);
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if <code>omission</code> element passes checks.
	 *
	 * @param loadcaseDamageContributionAnalysisInput
	 *            Root input element.
	 * @return True if <code>omission</code> element passes checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkOmission(Element loadcaseDamageContributionAnalysisInput) throws Exception {

		// update info
		task.updateMessage("Checking omission...");

		// omission element exists
		if (loadcaseDamageContributionAnalysisInput.getChild("omission") != null) {

			// get omission element
			Element omission = loadcaseDamageContributionAnalysisInput.getChild("omission");

			// remove negative stresses
			if (!XMLUtilities.checkBooleanValue(task, inputFile, omission, "removeNegativeStresses", true))
				return false;

			// stress range
			if (!XMLUtilities.checkDoubleValue(task, inputFile, omission, "stressRange", true, 0.0, null))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if <code>material</code> element passes checks.
	 *
	 * @param loadcaseDamageContributionAnalysisInput
	 *            Root input element.
	 * @param connection
	 *            Database connection.
	 * @return True if <code>material</code> element passes checks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkMaterial(Element loadcaseDamageContributionAnalysisInput, Connection connection) throws Exception {

		// update info
		task.updateMessage("Checking material...");

		// no material element found
		if (loadcaseDamageContributionAnalysisInput.getChild("material") == null) {
			task.addWarning("Cannot locate element 'material' under " + XMLUtilities.getFamilyTree(loadcaseDamageContributionAnalysisInput) + " in instruction set '" + inputFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get parent
		Element materialElement = loadcaseDamageContributionAnalysisInput.getChild("material");

		// name
		if (!XMLUtilities.checkStringValue(task, inputFile, materialElement, "name", false))
			return false;

		// specification
		if (!XMLUtilities.checkStringValue(task, inputFile, materialElement, "specification", false))
			return false;

		// orientation
		if (!XMLUtilities.checkStringValue(task, inputFile, materialElement, "orientation", false))
			return false;

		// configuration
		if (!XMLUtilities.checkStringValue(task, inputFile, materialElement, "configuration", false))
			return false;

		// get inputs
		String name = materialElement.getChildTextNormalize("name");
		String specification = materialElement.getChildTextNormalize("specification");
		String orientation = materialElement.getChildTextNormalize("orientation");
		String configuration = materialElement.getChildTextNormalize("configuration");

		// initialize material
		Material material = null;

		// create statement
		try (Statement statement = connection.createStatement()) {
			material = getFatigueMaterial(name, specification, orientation, configuration, statement);
		}

		// no material found
		if (material == null) {
			task.addWarning("Cannot find material with given parameters in database for element " + XMLUtilities.getFamilyTree(materialElement) + " in instruction set '" + inputFile.toString() + "'. Check failed.");
			return false;
		}

		// check passed
		return true;
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