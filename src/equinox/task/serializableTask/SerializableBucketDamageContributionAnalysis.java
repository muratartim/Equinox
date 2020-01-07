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
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.LoadcaseDamageContributionInput;
import equinox.task.BucketDamageContributionAnalysis;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serialized form of bucket damage contribution analysis task.
 *
 * @author Murat Artim
 * @date 1 Sep 2016
 * @time 15:52:20
 */
public class SerializableBucketDamageContributionAnalysis implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input. */
	private final LoadcaseDamageContributionInput input_;

	/** Serializable STF file bucket. */
	private final SerializableSpectrumItem bucket_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/**
	 * Creates serialized form of bucket damage contribution analysis task.
	 *
	 * @param bucket
	 *            STF file bucket.
	 * @param input
	 *            Analysis input.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public SerializableBucketDamageContributionAnalysis(STFFileBucket bucket, LoadcaseDamageContributionInput input, AnalysisEngine analysisEngine) {
		bucket_ = new SerializableSpectrumItem(bucket);
		input_ = input;
		analysisEngine_ = analysisEngine;
	}

	@Override
	public BucketDamageContributionAnalysis getTask(TreeItem<String> fileTreeRoot) {

		// get STF file
		SpectrumItem[] result = new SpectrumItem[1];
		searchSpectrumItem(bucket_, fileTreeRoot, result);

		// STF file could not be found
		if (result[0] == null)
			return null;

		// create and return task
		return new BucketDamageContributionAnalysis((STFFileBucket) result[0], input_, analysisEngine_);
	}
}
