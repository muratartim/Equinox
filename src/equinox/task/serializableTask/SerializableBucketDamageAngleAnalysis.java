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
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.DamageAngleInput;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.task.BucketDamageAngleAnalysis;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serialized form of bucket damage angle analysis task.
 *
 * @author Murat Artim
 * @date 30 Aug 2016
 * @time 14:43:35
 */
public class SerializableBucketDamageAngleAnalysis implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input. */
	private final DamageAngleInput input_;

	/** Serializable STF file bucket. */
	private final SerializableSpectrumItem bucket_;

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
	 * Creates serialized form of bucket damage angle analysis task.
	 *
	 * @param bucket
	 *            STF file bucket.
	 * @param input
	 *            Analysis input.
	 * @param material
	 *            Material.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public SerializableBucketDamageAngleAnalysis(STFFileBucket bucket, DamageAngleInput input, FatigueMaterial material, AnalysisEngine analysisEngine) {
		bucket_ = new SerializableSpectrumItem(bucket);
		input_ = input;
		material_ = material;
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
	public SerializableBucketDamageAngleAnalysis setIsamiEngineInputs(IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
		return this;
	}

	@Override
	public BucketDamageAngleAnalysis getTask(TreeItem<String> fileTreeRoot) {

		// get STF file
		SpectrumItem[] result = new SpectrumItem[1];
		searchSpectrumItem(bucket_, fileTreeRoot, result);

		// STF file could not be found
		if (result[0] == null)
			return null;

		// create and return task
		return new BucketDamageAngleAnalysis((STFFileBucket) result[0], input_, material_, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
	}
}
