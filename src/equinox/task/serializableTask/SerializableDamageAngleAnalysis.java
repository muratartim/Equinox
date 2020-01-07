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
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.DamageAngleInput;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.task.DamageAngleAnalysis;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of damage angle analysis task.
 *
 * @author Murat Artim
 * @date Oct 12, 2015
 * @time 11:48:21 AM
 */
public class SerializableDamageAngleAnalysis implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input. */
	private final DamageAngleInput input_;

	/** Serializable STF file. */
	private final SerializableSpectrumItem stfFile_, spectrum_;

	/** STF file ID. */
	private final Integer stfID_, stressTableID_;

	/** STF file name. */
	private final String stfName_;

	/** Material. */
	private final FatigueMaterial material_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/** ISAMI version. */
	private IsamiVersion isamiVersion_;

	/** ISAMI sub version. */
	private IsamiSubVersion isamiSubVersion_;

	/** True compression should be applied in propagation analysis. */
	private boolean applyCompression_;

	/**
	 * Creates serializable form of damage angle analysis task.
	 *
	 * @param stfFile
	 *            STF file.
	 * @param input
	 *            Input.
	 * @param material
	 *            Material.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public SerializableDamageAngleAnalysis(STFFile stfFile, DamageAngleInput input, FatigueMaterial material, AnalysisEngine analysisEngine) {
		stfFile_ = new SerializableSpectrumItem(stfFile);
		input_ = input;
		material_ = material;
		analysisEngine_ = analysisEngine;
		stfID_ = null;
		stressTableID_ = null;
		stfName_ = null;
		spectrum_ = null;
	}

	/**
	 * Creates serializable form of damage angle analysis task.
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
	 *            Input.
	 * @param material
	 *            Material.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public SerializableDamageAngleAnalysis(int stfID, int stressTableID, String stfName, Spectrum spectrum, DamageAngleInput input, FatigueMaterial material, AnalysisEngine analysisEngine) {
		stfID_ = stfID;
		stressTableID_ = stressTableID;
		stfName_ = stfName;
		spectrum_ = new SerializableSpectrumItem(spectrum);
		input_ = input;
		material_ = material;
		stfFile_ = null;
		analysisEngine_ = analysisEngine;
	}

	/**
	 * Sets ISAMI engine inputs.
	 *
	 * @param isamiVersion
	 *            ISAMI version.
	 * @param isamiSubVersion
	 *            ISAMI sub version.
	 * @param applyCompression
	 *            True to apply compression for propagation analyses.
	 * @return This analysis.
	 */
	public SerializableDamageAngleAnalysis setIsamiEngineInputs(IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
		return this;
	}

	@Override
	public DamageAngleAnalysis getTask(TreeItem<String> fileTreeRoot) {

		// there is STF file1
		if (stfFile_ != null) {

			// get STF file
			SpectrumItem[] result = new SpectrumItem[1];
			searchSpectrumItem(stfFile_, fileTreeRoot, result);

			// STF file could not be found
			if (result[0] == null)
				return null;

			// create and return task
			return new DamageAngleAnalysis((STFFile) result[0], input_, material_, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
		}

		// get spectrum
		SpectrumItem[] result = new SpectrumItem[1];
		searchSpectrumItem(spectrum_, fileTreeRoot, result);

		// spectrum could not be found
		if (result[0] == null)
			return null;

		// create and return task
		return new DamageAngleAnalysis(stfID_, stressTableID_, stfName_, (Spectrum) result[0], input_, material_, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
	}
}
