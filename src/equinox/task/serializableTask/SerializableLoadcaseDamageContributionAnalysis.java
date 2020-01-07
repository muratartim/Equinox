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
package equinox.task.serializableTask;

import equinox.data.AnalysisEngine;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.LoadcaseDamageContributionInput;
import equinox.task.LoadcaseDamageContributionAnalysis;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of loadcase damage contribution analysis task.
 *
 * @author Murat Artim
 * @date Oct 12, 2015
 * @time 12:06:50 PM
 */
public class SerializableLoadcaseDamageContributionAnalysis implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input. */
	private final LoadcaseDamageContributionInput input_;

	/** Serializable STF file. */
	private final SerializableSpectrumItem stfFile_, spectrum_;

	/** STF file ID. */
	private final Integer stfID_, stressTableID_;

	/** STF file name. */
	private final String stfName_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/**
	 * Creates serializable form of loadcase damage contribution analysis task.
	 *
	 * @param stfFile
	 *            The owner STF file.
	 * @param input
	 *            Analysis input.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public SerializableLoadcaseDamageContributionAnalysis(STFFile stfFile, LoadcaseDamageContributionInput input, AnalysisEngine analysisEngine) {
		stfFile_ = new SerializableSpectrumItem(stfFile);
		input_ = input;
		stfID_ = null;
		stressTableID_ = null;
		stfName_ = null;
		spectrum_ = null;
		analysisEngine_ = analysisEngine;
	}

	/**
	 * Creates serializable form of loadcase damage contribution analysis task.
	 *
	 * @param stfID
	 *            STF file ID.
	 * @param stressTableID
	 *            STF stress table ID.
	 * @param stfName
	 *            STF file name.
	 * @param spectrum
	 *            The owner spectrum.
	 * @param input
	 *            Analysis input.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public SerializableLoadcaseDamageContributionAnalysis(int stfID, int stressTableID, String stfName, Spectrum spectrum, LoadcaseDamageContributionInput input, AnalysisEngine analysisEngine) {
		stfID_ = stfID;
		stressTableID_ = stressTableID;
		stfName_ = stfName;
		spectrum_ = new SerializableSpectrumItem(spectrum);
		input_ = input;
		stfFile_ = null;
		analysisEngine_ = analysisEngine;
	}

	@Override
	public LoadcaseDamageContributionAnalysis getTask(TreeItem<String> fileTreeRoot) {

		// there is STF file
		if (stfFile_ != null) {

			// get STF file
			SpectrumItem[] result = new SpectrumItem[1];
			searchSpectrumItem(stfFile_, fileTreeRoot, result);

			// STF file could not be found
			if (result[0] == null)
				return null;

			// create and return task
			return new LoadcaseDamageContributionAnalysis((STFFile) result[0], input_, analysisEngine_);
		}

		// get spectrum
		SpectrumItem[] result = new SpectrumItem[1];
		searchSpectrumItem(spectrum_, fileTreeRoot, result);

		// spectrum could not be found
		if (result[0] == null)
			return null;

		// create and return task
		return new LoadcaseDamageContributionAnalysis(stfID_, stressTableID_, stfName_, (Spectrum) result[0], input_, analysisEngine_);
	}
}
