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
package equinox.data.ui;

import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.data.general.PieDataset;

/**
 * Class for pie chart label generator.
 *
 * @author Murat Artim
 * @date May 6, 2015
 * @time 11:12:11 AM
 */
public class PieLabelGenerator extends StandardPieSectionLabelGenerator {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates pie chart label generator.
	 *
	 * @param labelFormat
	 *            The label format (null not permitted).
	 */
	public PieLabelGenerator(String labelFormat) {
		super(labelFormat);
	}

	@Override
	public String generateSectionLabel(PieDataset dataset, Comparable key) {
		return key.equals("Rest") ? null : super.generateSectionLabel(dataset, key);
	}
}
