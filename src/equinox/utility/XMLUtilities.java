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
package equinox.utility;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.jdom2.Element;

import equinox.Equinox;
import equinox.plugin.FileType;
import equinox.task.InternalEquinoxTask;

/**
 * Class for supplying utility methods for checking instruction sets.
 *
 * @author Murat Artim
 * @date 21 Aug 2018
 * @time 09:58:49
 */
public class XMLUtilities {

	/**
	 * Creates task logger.
	 *
	 * @param logDirectory
	 *            Directory where the log file will be stored.
	 * @param task
	 *            Task for which the log file path will be created.
	 * @param logLevel
	 *            Log level.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void createLogger(Path logDirectory, InternalEquinoxTask<?> task, Level logLevel) throws Exception {

		// get salt (i.e. task simple class name and hash code)
		String className = task.getClass().getSimpleName();
		int hashCode = task.hashCode();

		// create log file
		String fileName = className + "_" + Equinox.USER.getAlias() + "_" + hashCode + ".log";
		Path logFile = logDirectory.resolve(fileName);

		// create logger
		task.createLogger(fileName, logFile, logLevel);
	}

	/**
	 * Extracts and returns search keywords.
	 *
	 * @param keywords
	 *            User input.
	 * @return List of keywords.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static ArrayList<String> extractSearchKeywords(String keywords) throws Exception {

		// create input list
		ArrayList<String> inputs = new ArrayList<>();

		// multiple keywords
		if (keywords.contains(",")) {
			StringTokenizer st = new StringTokenizer(keywords, ",");
			while (st.hasMoreTokens()) {
				String word = st.nextToken().trim();
				if (!word.isEmpty()) {
					inputs.add(word);
				}
			}
		}

		// single keyword
		else {
			inputs.add(keywords);
		}

		// return list
		return inputs;
	}

	/**
	 * Returns true if given element has valid search entries.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param validAttributeNames
	 *            List of valid attribute names.
	 * @return True if given element has valid search entries.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkSearchEntries(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String... validAttributeNames) throws Exception {

		// no search entry found
		if (parentElement.getChild("searchEntry") == null) {
			task.addWarning("Cannot locate element 'searchEntry' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. At least one of this element is obligatory. Check failed.");
			return false;
		}

		// loop over search entries
		for (Element searchEntry : parentElement.getChildren("searchEntry")) {

			// check attribute name
			if (!checkStringValue(task, xmlFile, searchEntry, "attributeName", false, validAttributeNames))
				return false;

			// check keyword
			if (!checkStringValue(task, xmlFile, searchEntry, "keyword", false))
				return false;

			// check criteria
			if (!checkStringValue(task, xmlFile, searchEntry, "criteria", true, "Contains", "Equals", "Starts with", "Ends with"))
				return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if given element has an online username.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @return True if if given element has an online username.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkRecipient(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get recipient
		String recipient = element.getTextNormalize();

		// null or empty value
		if (recipient == null || recipient.isEmpty()) {
			task.addWarning("Empty value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// this user
		if (recipient.equals(Equinox.USER.getUsername())) {
			task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// recipient is offline
		if (!task.getTaskPanel().getOwner().getOwner().isUserAvailable(recipient)) {
			task.addWarning("Recipient of " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "' is not online. Check failed.");
			return false;
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if the dependency of the given element is satisfied.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param root
	 *            Root input element.
	 * @param element
	 *            Element to check its dependency.
	 * @param dependencyName
	 *            Dependency name.
	 * @param targetElementName
	 *            Target element name.
	 * @param minDependecies
	 *            Minimum number of required dependencies.
	 * @return True if the dependency of the given element is satisfied.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkDependencies(InternalEquinoxTask<?> task, Path xmlFile, Element root, Element element, String dependencyName, String targetElementName, int minDependecies) throws Exception {

		// get dependencies
		List<Element> sourceElements = element.getChildren(dependencyName);

		// less than minimum required dependencies
		if (sourceElements.size() < minDependecies) {
			task.addWarning("Minimum " + minDependecies + " dependencies are required for element " + XMLUtilities.getFamilyTree(element) + "." + dependencyName + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// loop over source elements
		for (Element sourceElement : sourceElements) {

			// source element not found
			if (sourceElement == null) {
				task.addWarning("Cannot locate element '" + dependencyName + "' under " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
				return false;
			}

			// get source id
			String sourceId = sourceElement.getTextNormalize();

			// empty id
			if (sourceId.isEmpty()) {
				task.addWarning("Empty value supplied for " + XMLUtilities.getFamilyTree(sourceElement) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
				return false;
			}

			// search for referenced element
			boolean found = false;
			for (Element targetElement : root.getChildren(targetElementName)) {
				Element id = targetElement.getChild("id");
				if (id != null) {
					if (sourceId.equals(id.getTextNormalize())) {
						found = true;
						break;
					}
				}
			}

			// referenced element not found
			if (!found) {
				task.addWarning("Cannot find element with task id '" + sourceId + "' which appears to be a dependency of " + XMLUtilities.getFamilyTree(sourceElement) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
				return false;
			}
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if the dependency of the given element is satisfied.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param root
	 *            Root input element.
	 * @param element
	 *            Element to check its dependency.
	 * @param dependencyName
	 *            Dependency name.
	 * @param targetElementName
	 *            Target element name. Can be null for searching all elements.
	 * @return True if the dependency of the given element is satisfied.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkDependency(InternalEquinoxTask<?> task, Path xmlFile, Element root, Element element, String dependencyName, String targetElementName) throws Exception {

		// get source element
		Element sourceElement = element.getChild(dependencyName);

		// source element not found
		if (sourceElement == null) {
			task.addWarning("Cannot locate element '" + dependencyName + "' under " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get source id
		String sourceId = sourceElement.getTextNormalize();

		// empty id
		if (sourceId.isEmpty()) {
			task.addWarning("Empty value supplied for " + XMLUtilities.getFamilyTree(sourceElement) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// search for referenced element
		if (targetElementName != null) {
			for (Element targetElement : root.getChildren(targetElementName)) {
				Element id = targetElement.getChild("id");
				if (id != null) {
					if (sourceId.equals(id.getTextNormalize()))
						return true;
				}
			}
		}

		// search for all elements
		else {
			for (Element targetElement : root.getChildren()) {
				Element id = targetElement.getChild("id");
				if (id != null) {
					if (sourceId.equals(id.getTextNormalize()))
						return true;
				}
			}
		}

		// referenced element not found
		task.addWarning("Cannot find element with task id '" + sourceId + "' which appears to be a dependency of " + XMLUtilities.getFamilyTree(sourceElement) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
		return false;
	}

	/**
	 * Returns true if the dependency of the given element is satisfied.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param root
	 *            Root input element.
	 * @param element
	 *            Element to check its dependency.
	 * @param dependencyName
	 *            Dependency name.
	 * @param targetElementNamePrefixes
	 *            Target element name prefixes.
	 * @return True if the dependency of the given element is satisfied.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkDependencyStartingWith(InternalEquinoxTask<?> task, Path xmlFile, Element root, Element element, String dependencyName, String... targetElementNamePrefixes) throws Exception {

		// get source element
		Element sourceElement = element.getChild(dependencyName);

		// source element not found
		if (sourceElement == null) {
			task.addWarning("Cannot locate element '" + dependencyName + "' under " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get source id
		String sourceId = sourceElement.getTextNormalize();

		// empty id
		if (sourceId.isEmpty()) {
			task.addWarning("Empty value supplied for " + XMLUtilities.getFamilyTree(sourceElement) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// search for referenced element
		for (Element targetElement : root.getChildren()) {
			for (String prefix : targetElementNamePrefixes) {
				if (targetElement.getName().startsWith(prefix)) {
					Element id = targetElement.getChild("id");
					if (id != null) {
						if (sourceId.equals(id.getTextNormalize()))
							return true;
					}
				}
			}
		}

		// referenced element not found
		task.addWarning("Cannot find element with task id '" + sourceId + "' which appears to be a dependency of " + XMLUtilities.getFamilyTree(sourceElement) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
		return false;
	}

	/**
	 * Returns true if given element has at least 1 valid search keyword value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @return True if given element has at least 1 valid search keyword value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkSearchKeywords(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get value of element
		String keywords = element.getTextNormalize();

		// empty value
		if (keywords.isEmpty()) {
			task.addWarning("Empty value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// multiple keywords
		if (keywords.contains(",")) {

			// create tokens
			ArrayList<String> tokens = new ArrayList<>();
			StringTokenizer st = new StringTokenizer(keywords, ",");
			while (st.hasMoreTokens()) {
				String word = st.nextToken().trim();
				if (!word.isEmpty()) {
					tokens.add(word);
				}
			}

			// no keywords found
			if (tokens.isEmpty()) {
				task.addWarning("no valid keyword supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
				return false;
			}
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if the given element has a valid task id.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param root
	 *            Root input element.
	 * @param element
	 *            Element to be checked.
	 * @return True if the given element has a valid task id.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkElementId(InternalEquinoxTask<?> task, Path xmlFile, Element root, Element element) throws Exception {

		// no task id
		if (element.getChild("id") == null) {
			task.addWarning("No task id specified for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// get id
		String id = element.getChild("id").getTextNormalize();

		// invalid id
		if (id.isEmpty()) {
			task.addWarning("Invalid id specified for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// loop over elements
		for (Element c : root.getChildren()) {

			// pivot
			if (c.equals(element)) {
				continue;
			}

			// no id
			if (c.getChild("id") == null) {
				continue;
			}

			// same id
			if (c.getChild("id").getTextNormalize().equals(id)) {
				task.addWarning("Non-unique id specified for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
				return false;
			}
		}

		// check passed
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>Path</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @param fileTypes
	 *            List of allowed file types. Can be skipped if all file types are allowed.
	 * @return True if given element has a valid <code>Path</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkInputPathValue(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement, FileType... fileTypes) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get input path
		Path input = Paths.get(element.getTextNormalize());

		// invalid value
		if (!Files.exists(input)) {
			task.addWarning("Invalid file path supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// check file type
		if (fileTypes != null && fileTypes.length != 0) {
			if (!Arrays.asList(fileTypes).contains(FileType.getFileType(input.toFile()))) {
				task.addWarning("Invalid file type supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Allowed file types are:" + Arrays.toString(fileTypes) + ". Check failed.");
				return false;
			}
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>Path</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @return True if given element has a valid <code>Path</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkInputDirectoryPathValue(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get input path
		Path input = Paths.get(element.getTextNormalize());

		// invalid value
		if (!Files.exists(input)) {
			task.addWarning("Invalid directory path supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// not a directory
		if (!Files.isDirectory(input)) {
			task.addWarning("Invalid directory path supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>Path</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @param overwriteFiles
	 *            True to allow overwriting existing files.
	 * @param fileTypes
	 *            List of allowed file types. Can be skipped if all file types are allowed.
	 * @return True if given element has a valid <code>Path</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkOutputPathValue(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement, boolean overwriteFiles, FileType... fileTypes) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get path
		Path output = Paths.get(element.getTextNormalize());

		// parent directory doesn't exist
		if (!Files.exists(output.getParent())) {
			task.addWarning("Invalid file path supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// check file type
		if (fileTypes != null && fileTypes.length != 0) {
			if (!Arrays.asList(fileTypes).contains(FileType.getFileType(output.toFile()))) {
				task.addWarning("Invalid file type supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Allowed file types are:" + Arrays.toString(fileTypes) + ". Check failed.");
				return false;
			}
		}

		// overwrite is false and file already exists
		if (!overwriteFiles && Files.exists(output)) {
			task.addWarning("Output file supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "' already exists. Check failed.");
			return false;
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>String</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @param validValues
	 *            List of valid values. Can be skipped for checking only against empty values.
	 * @return True if given element has a valid <code>String</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkStringValue(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement, String... validValues) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get value of element
		String value = element.getTextNormalize();

		// empty value
		if (value.isEmpty()) {
			task.addWarning("Empty value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// invalid value
		if (validValues != null && validValues.length != 0 && !Arrays.asList(validValues).contains(value)) {
			task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Valid values are: " + Arrays.toString(validValues) + ". Check failed.");
			return false;
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>String</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param minElements
	 *            Minimum number of elements.
	 * @param isDistinct
	 *            True if the element should have distinct values among same elements.
	 * @param validValues
	 *            List of valid values. Can be skipped for checking only against empty values.
	 * @return True if given element has a valid <code>String</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkStringValues(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, int minElements, boolean isDistinct, String... validValues) throws Exception {

		// get elements
		List<Element> elements = parentElement.getChildren(elementName);

		// less than minimum required number of elements
		if (elements.size() < minElements) {
			task.addWarning("Minimum " + minElements + " elements are required for element " + XMLUtilities.getFamilyTree(parentElement) + "." + elementName + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
			return false;
		}

		// loop over source elements
		ArrayList<String> values = new ArrayList<>();
		for (Element element : elements) {

			// get value of element
			String value = element.getTextNormalize();

			// already contained and should be distinct
			if (isDistinct && values.contains(value)) {
				task.addWarning("Value '" + value + "' appears more than once for element " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
				return false;
			}

			// empty value
			if (value.isEmpty()) {
				task.addWarning("Empty value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Check failed.");
				return false;
			}

			// invalid value
			if (validValues != null && validValues.length != 0 && !Arrays.asList(validValues).contains(value)) {
				task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Valid values are: " + Arrays.toString(validValues) + ". Check failed.");
				return false;
			}

			// ad to values
			values.add(value);
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>boolean</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @return True if given element has a valid <code>boolean</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkBooleanValue(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// get value of element
		String value = element.getTextNormalize();

		// invalid value
		if (!value.equals("true") && !value.equals("false")) {
			task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Valid values are 'true' or 'false'. Check failed.");
			return false;
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>double</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @param minimum
	 *            Minimum allowed value (exclusive). Can be <code>null</code> if there is no minimum boundary.
	 * @param maximum
	 *            Maximum allowed value (exclusive). Can be <code>null</code> if there is no maximum boundary.
	 * @return True if given element has a valid <code>double</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkDoubleValue(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement, Double minimum, Double maximum) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// parse value
		try {

			// parse value
			double value = Double.parseDouble(element.getTextNormalize());

			// check against minimum
			if (minimum != null) {
				if (value < minimum) {
					task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Value must be greater than '" + minimum + "'. Check failed.");
					return false;
				}
			}

			// check against maximum
			if (maximum != null) {
				if (value > maximum) {
					task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Value must be smaller than '" + maximum + "'. Check failed.");
					return false;
				}
			}
		}

		// invalid value
		catch (NumberFormatException e) {
			task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Valid values are 'true' or 'false'. Check failed.");
			return false;
		}

		// valid value
		return true;
	}

	/**
	 * Returns true if given element has a valid <code>int</code> value.
	 *
	 * @param task
	 *            Calling task. Used for adding warnings.
	 * @param xmlFile
	 *            Path to input XML file. Used for warning content.
	 * @param parentElement
	 *            Parent of the element to check.
	 * @param elementName
	 *            Name of the element to check.
	 * @param isOptionalElement
	 *            True if the element is optional.
	 * @param minimum
	 *            Minimum allowed value (exclusive). Can be <code>null</code> if there is no minimum boundary.
	 * @param maximum
	 *            Maximum allowed value (exclusive). Can be <code>null</code> if there is no maximum boundary.
	 * @return True if given element has a valid <code>int</code> value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static boolean checkIntegerValue(InternalEquinoxTask<?> task, Path xmlFile, Element parentElement, String elementName, boolean isOptionalElement, Integer minimum, Integer maximum) throws Exception {

		// get element
		Element element = parentElement.getChild(elementName);

		// element not found
		if (element == null) {

			// optional
			if (isOptionalElement)
				return true;

			// obligatory
			task.addWarning("Cannot locate element '" + elementName + "' under " + XMLUtilities.getFamilyTree(parentElement) + " in instruction set '" + xmlFile.toString() + "'. This element is obligatory. Check failed.");
			return false;
		}

		// parse value
		try {

			// parse value
			int value = Integer.parseInt(element.getTextNormalize());

			// check against minimum
			if (minimum != null) {
				if (value < minimum) {
					task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Value must be greater than '" + minimum + "'. Check failed.");
					return false;
				}
			}

			// check against maximum
			if (maximum != null) {
				if (value > maximum) {
					task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. Value must be smaller than '" + maximum + "'. Check failed.");
					return false;
				}
			}
		}

		// invalid value
		catch (NumberFormatException e) {
			task.addWarning("Invalid value supplied for " + XMLUtilities.getFamilyTree(element) + " in instruction set '" + xmlFile.toString() + "'. A numeric value is expected. Check failed.");
			return false;
		}

		// valid value
		return true;
	}

	/**
	 * Returns <code>String</code> representation of the family tree of the given element.
	 *
	 * @param element
	 *            Target element.
	 * @return <code>String</code> representation of the family tree of the given element.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static String getFamilyTree(Element element) throws Exception {
		String tree = element.getName();
		Element p = element.getParentElement();
		while (p != null) {
			tree = p.getName() + "." + tree;
			p = p.getParentElement();
		}
		return tree;
	}

	/**
	 * Converts given object array to string array.
	 *
	 * @param objects
	 *            Array of objects.
	 * @return Array of strings.
	 */
	public static String[] getStringArray(Object[] objects) {
		String[] strings = new String[objects.length];
		for (int i = 0; i < objects.length; i++) {
			strings[i] = objects[i].toString();
		}
		return strings;
	}
}