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

import java.util.ArrayList;

import equinox.data.AnalysisEngine;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.FastEquivalentStressInput;
import equinox.dataServer.remote.data.Material;
import equinox.task.BucketFastEquivalentStressAnalysis;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of fast equivalent stress analysis from STF file bucket task.
 *
 * @author Murat Artim
 * @date 25 Aug 2016
 * @time 11:22:36
 */
public class SerializableBucketFastEquivalentStressAnalysis implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input. */
	private final FastEquivalentStressInput input_;

	/** Serializable STF file bucket. */
	private final SerializableSpectrumItem bucket_;

	/** Materials. */
	private final ArrayList<Material> materials_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/** ISAMI version. */
	private IsamiVersion isamiVersion_;

	/** ISAMI sub version1. */
	private IsamiSubVersion isamiSubVersion_;

	/** True compression should be applied in propagation analysis. */
	private boolean applyCompression_;

	/** True if typical flight damage contribution analysis is requested. */
	private final boolean isFlightDamageContributionAnalysis_;

	/**
	 * Creates serializable form of fast equivalent stress analysis from STF file bucket task.
	 *
	 * @param bucket
	 *            STF file bucket.
	 * @param input
	 *            Analysis input.
	 * @param materials
	 *            Materials.
	 * @param isFlightDamageContributionAnalysis
	 *            True if typical flight damage contribution analysis is requested.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public SerializableBucketFastEquivalentStressAnalysis(STFFileBucket bucket, FastEquivalentStressInput input, ArrayList<Material> materials, boolean isFlightDamageContributionAnalysis, AnalysisEngine analysisEngine) {
		bucket_ = new SerializableSpectrumItem(bucket);
		input_ = input;
		materials_ = materials;
		isFlightDamageContributionAnalysis_ = isFlightDamageContributionAnalysis;
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
	public SerializableBucketFastEquivalentStressAnalysis setIsamiEngineInputs(IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
		return this;
	}

	@Override
	public BucketFastEquivalentStressAnalysis getTask(TreeItem<String> fileTreeRoot) {

		// get STF file
		SpectrumItem[] result = new SpectrumItem[1];
		searchSpectrumItem(bucket_, fileTreeRoot, result);

		// STF file could not be found
		if (result[0] == null)
			return null;

		// create and return task
		return new BucketFastEquivalentStressAnalysis((STFFileBucket) result[0], input_, materials_, isFlightDamageContributionAnalysis_, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
	}
}
