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
package equinox.data;

/**
 * Class for mission profile comparison plot attributes.
 *
 * @author Murat Artim
 * @date 7 Sep 2018
 * @time 09:32:55
 */
public class MissionProfileComparisonPlotAttributes {

	/** Plot attributes. */
	private final String title;

	/** Maximum difference in plot to adjust Y axis. */
	private final double maxDiff;

	/**
	 * Creates mission profile comparison plot attributes.
	 *
	 * @param maxDiff
	 *            Maximum difference in plot to adjust Y axis.
	 * @param title
	 *            Chart title.
	 */
	public MissionProfileComparisonPlotAttributes(double maxDiff, String title) {
		this.maxDiff = maxDiff;
		this.title = title;
	}

	/**
	 * Returns the maximum difference in plot to adjust Y axis.
	 *
	 * @return Maximum difference in plot to adjust Y axis.
	 */
	public double getMaxDiff() {
		return maxDiff;
	}

	/**
	 * Chart title.
	 *
	 * @return Chart title.
	 */
	public String getTitle() {
		return title;
	}
}