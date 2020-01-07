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
 * Class for mission parameter plot attributes.
 *
 * @author Murat Artim
 * @date 8 Sep 2018
 * @time 19:07:20
 */
public class MissionParameterPlotAttributes {

	/** Labels. */
	private String title = "Title", xAxisLabel = "X-Axis Label", yAxisLabel = "Y-Axis Label";

	/** Axis modes. */
	private boolean xAxisInverted = false, yAxisInverted = false;

	/**
	 * Returns title.
	 *
	 * @return Title.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns x axis label.
	 *
	 * @return X axis label.
	 */
	public String getxAxisLabel() {
		return xAxisLabel;
	}

	/**
	 * Returns y axis label.
	 *
	 * @return Y axis label.
	 */
	public String getyAxisLabel() {
		return yAxisLabel;
	}

	/**
	 * True if x axis should be inverted.
	 *
	 * @return True if x axis should be inverted.
	 */
	public boolean isxAxisInverted() {
		return xAxisInverted;
	}

	/**
	 * True if y axis should be inverted.
	 *
	 * @return True if y axis should be inverted.
	 */
	public boolean isyAxisInverted() {
		return yAxisInverted;
	}

	/**
	 * Sets title.
	 *
	 * @param title
	 *            Title.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Sets x axis label.
	 *
	 * @param xAxisLabel
	 *            X axis label.
	 */
	public void setxAxisLabel(String xAxisLabel) {
		this.xAxisLabel = xAxisLabel;
	}

	/**
	 * Sets y axis label.
	 *
	 * @param yAxisLabel
	 *            Y axis label.
	 */
	public void setyAxisLabel(String yAxisLabel) {
		this.yAxisLabel = yAxisLabel;
	}

	/**
	 * Sets x axis mode.
	 *
	 * @param xAxisInverted
	 *            True if x axis should be inverted.
	 */
	public void setxAxisInverted(boolean xAxisInverted) {
		this.xAxisInverted = xAxisInverted;
	}

	/**
	 * Sets y axis mode.
	 *
	 * @param yAxisInverted
	 *            True if y axis should be inverted.
	 */
	public void setyAxisInverted(boolean yAxisInverted) {
		this.yAxisInverted = yAxisInverted;
	}
}