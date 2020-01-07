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
 * Class for statistics plot attributes.
 *
 * @author Murat Artim
 * @date 2 Sep 2018
 * @time 17:51:28
 */
public class StatisticsPlotAttributes {

	/** Plot parameters. */
	private String title = "Title", subTitle = "Subtitle", xAxisLabel = "X-Axis Label", yAxisLabel = "Y-Axis Label";

	/** Plot parameters. */
	private boolean legendVisible = true, labelsVisible = true, isLayered = false;

	/**
	 * Sets chart title.
	 *
	 * @param title
	 *            Chart title.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Sets chart subtitle.
	 *
	 * @param subTitle
	 *            Chart subtitle.
	 */
	public void setSubTitle(String subTitle) {
		this.subTitle = subTitle;
	}

	/**
	 * Sets X axis label.
	 *
	 * @param xAxisLabel
	 *            X axis label.
	 */
	public void setXAxisLabel(String xAxisLabel) {
		this.xAxisLabel = xAxisLabel;
	}

	/**
	 * Sets Y axis label.
	 *
	 * @param yAxisLabel
	 *            Y axis label.
	 */
	public void setYAxisLabel(String yAxisLabel) {
		this.yAxisLabel = yAxisLabel;
	}

	/**
	 * Sets legend visibility.
	 *
	 * @param legendVisible
	 *            True to set legend visible.
	 */
	public void setLegendVisible(boolean legendVisible) {
		this.legendVisible = legendVisible;
	}

	/**
	 * Sets label visibility.
	 *
	 * @param labelsVisible
	 *            True to set chart labels visible.
	 */
	public void setLabelsVisible(boolean labelsVisible) {
		this.labelsVisible = labelsVisible;
	}

	/**
	 * Sets chart rendering mode.
	 *
	 * @param isLayered
	 *            True to set this chart as layered bar chart.
	 */
	public void setLayered(boolean isLayered) {
		this.isLayered = isLayered;
	}

	/**
	 * Returns chart title.
	 *
	 * @return Chart title.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns chart subtitle.
	 *
	 * @return Chart subtitle.
	 */
	public String getSubTitle() {
		return subTitle;
	}

	/**
	 * Returns X axis label.
	 *
	 * @return X axis label.
	 */
	public String getXAxisLabel() {
		return xAxisLabel;
	}

	/**
	 * Returns Y axis label.
	 *
	 * @return Y axis label.
	 */
	public String getYAxisLabel() {
		return yAxisLabel;
	}

	/**
	 * Returns true if legend is visible.
	 *
	 * @return True if legend is visible.
	 */
	public boolean isLegendVisible() {
		return legendVisible;
	}

	/**
	 * Returns true if labels are visible.
	 *
	 * @return True if labels are visible.
	 */
	public boolean isLabelsVisible() {
		return labelsVisible;
	}

	/**
	 * Returns true if chart has a layered bar renderer.
	 *
	 * @return True if chart has a layered bar renderer.
	 */
	public boolean isLayered() {
		return isLayered;
	}
}