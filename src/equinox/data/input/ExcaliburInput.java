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
package equinox.data.input;

import java.io.File;
import java.io.Serializable;
import java.util.logging.Level;

import equinox.data.ExcaliburStressSortingCriteria;
import equinox.data.ExcaliburStressType;

/**
 * Class for excalibur stress sorting plugin input data.
 *
 * @author Murat Artim
 * @date 29 Nov 2017
 * @time 23:55:09
 */
public class ExcaliburInput implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input files. */
	private File attributesTable, loadcaseKeysFile, stressDirectory, outputDirectory;

	/** Attributes table sheet name. */
	private String attributesTableSheet;

	/** Stress type. */
	private ExcaliburStressType stressType;

	/** Stress sorting criteria. */
	private ExcaliburStressSortingCriteria stressSortingCriteria;

	/** Rotation angle in degrees. */
	private double rotationAngle;

	/** Log level. */
	private Level logLevel;

	/** Delta-p loadcase number. */
	private Integer dpLoadcaseNumber;

	/** True if all tasks shall be run in parallel. */
	private boolean runInParallel;

	/**
	 * Returns true if all tasks shall be run in parallel.
	 *
	 * @return True if all tasks shall be run in parallel.
	 */
	public boolean isRunInParallel() {
		return runInParallel;
	}

	/**
	 * Returns delta-p loadcase number.
	 *
	 * @return Delta-p loadcase number.
	 */
	public Integer getDpLoadcaseNumber() {
		return dpLoadcaseNumber;
	}

	/**
	 * Returns the log level.
	 *
	 * @return The log level.
	 */
	public Level getLogLevel() {
		return logLevel;
	}

	/**
	 * Returns attributes table file.
	 *
	 * @return Attributes table file.
	 */
	public File getAttributesTable() {
		return attributesTable;
	}

	/**
	 * Returns loadcase keys file.
	 *
	 * @return Loadcase keys file.
	 */
	public File getLoadcaseKeysFile() {
		return loadcaseKeysFile;
	}

	/**
	 * Returns stress directory.
	 *
	 * @return Stress directory.
	 */
	public File getStressDirectory() {
		return stressDirectory;
	}

	/**
	 * Returns output directory.
	 *
	 * @return Output directory.
	 */
	public File getOutputDirectory() {
		return outputDirectory;
	}

	/**
	 * Returns attributes table sheet name.
	 *
	 * @return Attributes table sheet name.
	 */
	public String getAttributesTableSheet() {
		return attributesTableSheet;
	}

	/**
	 * Returns stress type.
	 *
	 * @return Stress type.
	 */
	public ExcaliburStressType getStressType() {
		return stressType;
	}

	/**
	 * Returns stress sorting criteria.
	 *
	 * @return Stress sorting criteria.
	 */
	public ExcaliburStressSortingCriteria getStressSortingCriteria() {
		return stressSortingCriteria;
	}

	/**
	 * Returns rotation angle in degrees.
	 *
	 * @return Rotation angle in degrees.
	 */
	public double getRotationAngle() {
		return rotationAngle;
	}

	/**
	 * Sets attributes table file.
	 *
	 * @param attributesTable
	 *            Attributes table file.
	 */
	public void setAttributesTable(File attributesTable) {
		this.attributesTable = attributesTable;
	}

	/**
	 * Sets loadcase keys file.
	 *
	 * @param loadcaseKeysFile
	 *            Loadcase keys file.
	 */
	public void setLoadcaseKeysFile(File loadcaseKeysFile) {
		this.loadcaseKeysFile = loadcaseKeysFile;
	}

	/**
	 * Sets stress directory.
	 *
	 * @param stressDirectory
	 *            Stress directory.
	 */
	public void setStressDirectory(File stressDirectory) {
		this.stressDirectory = stressDirectory;
	}

	/**
	 * Sets output directory.
	 *
	 * @param outputDirectory
	 *            Output directory.
	 */
	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	/**
	 * Sets attributes table sheet.
	 *
	 * @param attributesTableSheet
	 *            Attributes table sheet.
	 */
	public void setAttributesTableSheet(String attributesTableSheet) {
		this.attributesTableSheet = attributesTableSheet;
	}

	/**
	 * Sets stress type.
	 *
	 * @param stressType
	 *            Stress type.
	 */
	public void setStressType(ExcaliburStressType stressType) {
		this.stressType = stressType;
	}

	/**
	 * Sets stress sorting criteria.
	 *
	 * @param stressSortingCriteria
	 *            Stress sorting criteria.
	 */
	public void setStressSortingCriteria(ExcaliburStressSortingCriteria stressSortingCriteria) {
		this.stressSortingCriteria = stressSortingCriteria;
	}

	/**
	 * Sets rotation angle in degrees.
	 *
	 * @param rotationAngle
	 *            Rotation angle in degrees.
	 */
	public void setRotationAngle(double rotationAngle) {
		this.rotationAngle = rotationAngle;
	}

	/**
	 * Sets log level.
	 *
	 * @param logLevel
	 *            Log level.
	 */
	public void setLogLevel(Level logLevel) {
		this.logLevel = logLevel;
	}

	/**
	 * Sets delta-p loadcase number.
	 *
	 * @param dpLoadcaseNumber
	 *            Delta-p loadcase number.
	 */
	public void setDpLoadcaseNumber(Integer dpLoadcaseNumber) {
		this.dpLoadcaseNumber = dpLoadcaseNumber;
	}

	/**
	 * Sets whether all tasks shall be run in parallel.
	 *
	 * @param runInParallel
	 *            True if all tasks shall be run in parallel.
	 */
	public void setRunInParallel(boolean runInParallel) {
		this.runInParallel = runInParallel;
	}
}
