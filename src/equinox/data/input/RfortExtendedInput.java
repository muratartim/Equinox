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
import java.nio.file.Path;
import java.util.ArrayList;

import equinox.data.StressComponent;
import equinox.data.ui.RfortOmission;
import equinox.data.ui.RfortPilotPoint;
import equinox.data.ui.SerializableRfortPilotPoint;

/**
 * Class for RFORT extended tool input.
 *
 * @author Murat Artim
 * @date Jan 16, 2015
 * @time 4:07:30 PM
 */
public class RfortExtendedInput implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input files. */
	private File anaFile_, txtFile_, cvtFile_, flsFile_, xlsFile_;

	/** Pilot points. */
	private ArrayList<SerializableRfortPilotPoint> pilotPoints_;

	/** Add delta-p and enable SLOG mode options. */
	private boolean addDP_, enableSlog_, fatigueAnalysis_, preffasAnalysis_, linearAnalysis_;

	/** Numeric options. */
	private double refDP_, dpFac_, overallFac_;

	/** Rotation angle. */
	private int rotation_;

	/** End flight number. */
	private Integer runTillFlight_;

	/** Stress component. */
	private StressComponent component_;

	/** Target flights. */
	private String targetFlights_, sheet_;

	/** Omissions. */
	private ArrayList<RfortOmission> omissions_;

	/**
	 * Sets ANA file.
	 *
	 * @param anaFile
	 *            ANA file.
	 */
	public void setANAFile(Path anaFile) {
		anaFile_ = anaFile.toFile();
	}

	/**
	 * Sets TXT file.
	 *
	 * @param txtFile
	 *            TXT file.
	 */
	public void setTXTFile(Path txtFile) {
		txtFile_ = txtFile.toFile();
	}

	/**
	 * Sets CVT file.
	 *
	 * @param cvtFile
	 *            CVT file.
	 */
	public void setCVTFile(Path cvtFile) {
		cvtFile_ = cvtFile.toFile();
	}

	/**
	 * Sets FLS file.
	 *
	 * @param flsFile
	 *            FLS file.
	 */
	public void setFLSFile(Path flsFile) {
		flsFile_ = flsFile.toFile();
	}

	/**
	 * Sets XLS file.
	 *
	 * @param xlsFile
	 *            XLS file.
	 */
	public void setXLSFile(Path xlsFile) {
		xlsFile_ = xlsFile.toFile();
	}

	/**
	 * Adds pilot point.
	 *
	 * @param pilotPoint
	 *            Pilot point.
	 */
	public void addPilotPoint(RfortPilotPoint pilotPoint) {
		if (pilotPoints_ == null)
			pilotPoints_ = new ArrayList<>();
		pilotPoints_.add(pilotPoint.getSerializableForm());
	}

	/**
	 * Adds omission.
	 *
	 * @param omission
	 *            Omission.
	 */
	public void addOmission(RfortOmission omission) {
		if (omissions_ == null)
			omissions_ = new ArrayList<>();
		omissions_.add(omission);
	}

	/**
	 * Sets add delta-p option.
	 *
	 * @param addDP
	 *            Add delta-p option.
	 */
	public void setAddDP(boolean addDP) {
		addDP_ = addDP;
	}

	/**
	 * Sets enable SLOG mode option.
	 *
	 * @param enableSlogMode
	 *            True to enable SLOG mode.
	 */
	public void setEnableSlogMode(boolean enableSlogMode) {
		enableSlog_ = enableSlogMode;
	}

	/**
	 * Sets reference DP.
	 *
	 * @param refDP
	 *            Reference DP.
	 */
	public void setRefDP(double refDP) {
		refDP_ = refDP;
	}

	/**
	 * Sets delta-p factor.
	 *
	 * @param dpFac
	 *            Delta-p factor.
	 */
	public void setDPFactor(double dpFac) {
		dpFac_ = dpFac;
	}

	/**
	 * Sets overall factor.
	 *
	 * @param overallFac
	 *            Overall factor.
	 */
	public void setOverallFactor(double overallFac) {
		overallFac_ = overallFac;
	}

	/**
	 * Sets rotation angle.
	 *
	 * @param rotation
	 *            Rotation angle in degrees.
	 */
	public void setRotation(int rotation) {
		rotation_ = rotation;
	}

	/**
	 * Sets stress component.
	 *
	 * @param component
	 *            Stress component.
	 */
	public void setComponent(StressComponent component) {
		component_ = component;
	}

	/**
	 * Sets run till flight option.
	 *
	 * @param runTillFlight
	 *            Run till flight option.
	 */
	public void setRunTillFlight(Integer runTillFlight) {
		runTillFlight_ = runTillFlight;
	}

	/**
	 * Sets target flights.
	 *
	 * @param targetFlights
	 *            Target flights.
	 */
	public void setTargetFlights(String targetFlights) {
		targetFlights_ = targetFlights;
	}

	/**
	 * Sets conversion table sheet.
	 *
	 * @param sheet
	 *            Conversion table sheet.
	 */
	public void setConversionTableSheet(String sheet) {
		sheet_ = sheet;
	}

	/**
	 * Sets analysis types.
	 *
	 * @param fatigue
	 *            True to run fatigue analysis.
	 * @param preffas
	 *            True to run preffas analysis.
	 * @param linear
	 *            True to run linear prop. analysis.
	 */
	public void setAnalysisTypes(boolean fatigue, boolean preffas, boolean linear) {
		fatigueAnalysis_ = fatigue;
		preffasAnalysis_ = preffas;
		linearAnalysis_ = linear;
	}

	/**
	 * Returns ANA file.
	 *
	 * @return ANA file.
	 */
	public Path getANAFile() {
		return anaFile_.toPath();
	}

	/**
	 * Returns TXT file.
	 *
	 * @return TXT file.
	 */
	public Path getTXTFile() {
		return txtFile_.toPath();
	}

	/**
	 * Returns CVT file.
	 *
	 * @return CVT file.
	 */
	public Path getCVTFile() {
		return cvtFile_.toPath();
	}

	/**
	 * Returns FLS file.
	 *
	 * @return FLS file.
	 */
	public Path getFLSFile() {
		return flsFile_.toPath();
	}

	/**
	 * Returns XLS file.
	 *
	 * @return XLS file.
	 */
	public Path getXLSFile() {
		return xlsFile_.toPath();
	}

	/**
	 * Returns pilot points.
	 *
	 * @return Pilot points.
	 */
	public ArrayList<SerializableRfortPilotPoint> getPilotPoints() {
		return pilotPoints_;
	}

	/**
	 * Returns omissions.
	 *
	 * @return Omissions.
	 */
	public ArrayList<RfortOmission> getOmissions() {
		return omissions_;
	}

	/**
	 * Returns add delta-p option.
	 *
	 * @return Add delta-p option.
	 */
	public boolean getAddDP() {
		return addDP_;
	}

	/**
	 * Returns enable SLOG mode option.
	 *
	 * @return Enable SLOG mode option.
	 */
	public boolean getEnableSlogMode() {
		return enableSlog_;
	}

	/**
	 * Returns reference delta-p.
	 *
	 * @return Reference delta-p.
	 */
	public double getRefDP() {
		return refDP_;
	}

	/**
	 * Returns delta-p factor.
	 *
	 * @return Delta-p factor.
	 */
	public double getDPFactor() {
		return dpFac_;
	}

	/**
	 * Returns overall factor.
	 *
	 * @return Overall factor.
	 */
	public double getOverallFactor() {
		return overallFac_;
	}

	/**
	 * Returns rotation angle.
	 *
	 * @return Rotation angle.
	 */
	public int getRotation() {
		return rotation_;
	}

	/**
	 * Returns stress component.
	 *
	 * @return Stress component.
	 */
	public StressComponent getComponent() {
		return component_;
	}

	/**
	 * Returns run till flight option.
	 *
	 * @return Run till flight option.
	 */
	public Integer getRunTillFlight() {
		return runTillFlight_;
	}

	/**
	 * Returns target flights.
	 *
	 * @return Target flights.
	 */
	public String getTargetFlights() {
		return targetFlights_;
	}

	/**
	 * Returns the conversion table sheet.
	 *
	 * @return The conversion table sheet.
	 */
	public String getConversionTableSheet() {
		return sheet_;
	}

	/**
	 * Returns true if fatigue analysis should be run.
	 *
	 * @return True if fatigue analysis should be run.
	 */
	public boolean getRunFatigueAnalysis() {
		return fatigueAnalysis_;
	}

	/**
	 * Returns true if preffas analysis should be run.
	 *
	 * @return True if preffas analysis should be run.
	 */
	public boolean getRunPreffasAnalysis() {
		return preffasAnalysis_;
	}

	/**
	 * Returns true if linear prop. analysis should be run.
	 *
	 * @return True if linear prop. analysis should be run.
	 */
	public boolean getRunLinearAnalysis() {
		return linearAnalysis_;
	}
}
