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
import equinox.data.input.RfortExtendedInput;
import equinox.task.CreateRfortAnalysis;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of create RFORT analysis task.
 *
 * @author Murat Artim
 * @date Oct 12, 2015
 * @time 5:06:13 PM
 */
public class SerializableCreateRfortAnalysis implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Input. */
	private final RfortExtendedInput input_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/** ISAMI version. */
	private IsamiVersion isamiVersion_;

	/** ISAMI sub version. */
	private IsamiSubVersion isamiSubVersion_;

	/** True compression should be applied in propagation analysis. */
	private boolean applyCompression_;

	/**
	 * Creates serializable form of create RFORT analysis task.
	 *
	 * @param input
	 *            Analysis input.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public SerializableCreateRfortAnalysis(RfortExtendedInput input, AnalysisEngine analysisEngine) {
		input_ = input;
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
	 * @return This object.
	 */
	public SerializableCreateRfortAnalysis setIsamiEngineInputs(IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
		return this;
	}

	@Override
	public CreateRfortAnalysis getTask(TreeItem<String> fileTreeRoot) {
		return new CreateRfortAnalysis(input_, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_);
	}
}
