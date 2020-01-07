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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.SpectrumItem;
import equinox.task.SaveBucketDamageContributions;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of save damage contributions from STF file buckets task.
 *
 * @author Murat Artim
 * @date 5 Sep 2016
 * @time 10:21:03
 */
public class SerializableSaveBucketDamageContributions implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** STF file buckets. */
	private final List<SerializableSpectrumItem> buckets_;

	/** Damage contribution names. */
	private final List<String> contributionNames_;

	/** Options. */
	private final boolean[] options_;

	/** Output file. */
	private final File output_;

	/**
	 * Creates serializable form of save damage contributions from STF file buckets task.
	 *
	 * @param buckets
	 *            STF file buckets.
	 * @param contributionNames
	 *            Damage contribution names.
	 * @param options
	 *            Options.
	 * @param output
	 *            Output file.
	 */
	public SerializableSaveBucketDamageContributions(List<STFFileBucket> buckets, List<String> contributionNames, boolean[] options, File output) {
		buckets_ = new ArrayList<>();
		for (SpectrumItem item : buckets) {
			buckets_.add(new SerializableSpectrumItem(item));
		}
		options_ = options;
		output_ = output;
		contributionNames_ = contributionNames;
	}

	@Override
	public SaveBucketDamageContributions getTask(TreeItem<String> fileTreeRoot) {

		// get buckets
		List<STFFileBucket> buckets = new ArrayList<>();
		for (SerializableSpectrumItem item : buckets_) {

			// get bucket
			SpectrumItem[] result = new SpectrumItem[1];
			searchSpectrumItem(item, fileTreeRoot, result);

			// bucket could not be found
			if (result[0] == null)
				return null;

			// add to buckets
			buckets.add((STFFileBucket) result[0]);
		}

		// create and return task
		return new SaveBucketDamageContributions(buckets, contributionNames_, options_, output_);
	}
}
