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

/**
 * Class for adapt DRF plugin input data.
 *
 * @author Murat Artim
 * @date 24 Aug 2017
 * @time 11:39:52
 *
 */
public class AdaptDRFInput implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input files. */
	private File ana_, txt_, outputDirectory_;

	/** Target fatigue event. */
	private String targetEvent_;

	/** Current and new DRF values. */
	private double currentDRF_, newDRF_;

	/** Run till flight count. */
	private int runTillFlight_;

	/** True to add comments to output ANA file. */
	private boolean addComments_;

	/**
	 * Sets input ANA file path.
	 *
	 * @param ana
	 *            Input ANA file path.
	 */
	public void setANAFile(Path ana) {
		ana_ = ana.toFile();
	}

	/**
	 * Sets input TXT file path.
	 *
	 * @param txt
	 *            Input TXT file path.
	 */
	public void setTXTFile(Path txt) {
		txt_ = txt.toFile();
	}

	/**
	 * Sets target fatigue event.
	 *
	 * @param targetEvent
	 *            Target fatigue event.
	 */
	public void setTargetEvent(String targetEvent) {
		targetEvent_ = targetEvent;
	}

	/**
	 * Sets current DRF value.
	 *
	 * @param currentDRF
	 *            Current DRF value.
	 */
	public void setCurrentDRF(double currentDRF) {
		currentDRF_ = currentDRF;
	}

	/**
	 * Sets new DRF value.
	 *
	 * @param newDRF
	 *            New DRF value.
	 */
	public void setNewDRF(double newDRF) {
		newDRF_ = newDRF;
	}

	/**
	 * Sets output directory.
	 *
	 * @param outputDirectory
	 *            Output directory.
	 */
	public void setOutputDirectory(Path outputDirectory) {
		outputDirectory_ = outputDirectory.toFile();
	}

	/**
	 * Sets run till flight number.
	 *
	 * @param runTillFlight
	 *            Run till flight number.
	 */
	public void setRunTillFlight(int runTillFlight) {
		runTillFlight_ = runTillFlight;
	}

	/**
	 * Sets add comments.
	 *
	 * @param addComments
	 *            rue to add comments to output ANA file.
	 */
	public void setAddComments(boolean addComments) {
		addComments_ = addComments;
	}

	/**
	 * Returns run till flight number.
	 *
	 * @return Run till flight number.
	 */
	public int getRunTillFlight() {
		return runTillFlight_;
	}

	/**
	 * Returns ANA file.
	 *
	 * @return ANA file.
	 */
	public Path getANAFile() {
		return ana_.toPath();
	}

	/**
	 * Returns TXT file.
	 *
	 * @return TXT file.
	 */
	public Path getTXTFile() {
		return txt_.toPath();
	}

	/**
	 * Returns target event.
	 *
	 * @return Target fatigue event.
	 */
	public String getTargetEvent() {
		return targetEvent_;
	}

	/**
	 * Returns current DRF.
	 *
	 * @return Current DRF.
	 */
	public double getCurrentDRF() {
		return currentDRF_;
	}

	/**
	 * Returns new DRF.
	 *
	 * @return New DRF.
	 */
	public double getNewDRF() {
		return newDRF_;
	}

	/**
	 * Returns true if comments should be added to output ANA file.
	 * 
	 * @return True if comments should be added to output ANA file.
	 */
	public boolean getAddComments() {
		return addComments_;
	}

	/**
	 * Returns output directory.
	 *
	 * @return Output directory.
	 */
	public Path getOutputDirectory() {
		return outputDirectory_.toPath();
	}
}
