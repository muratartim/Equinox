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
package equinox.task;

import equinox.controller.ObjectViewPanel;
import equinox.controller.ViewPanel;

/**
 * Abstract class for plot 3D tasks.
 *
 * @param <V>
 *            Task output class.
 * @author Murat Artim
 * @date Apr 26, 2016
 * @time 2:43:24 PM
 */
public abstract class Plot3DTask<V> extends InternalEquinoxTask<V> {

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// notify view panel
		getViewPanel().setTaskStatus(this, false);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// notify view panel
		getViewPanel().setTaskStatus(this, false);
	}

	/**
	 * This method should be called as the first statement within the <code>call</code> method of a <code>Plot3DTask</code>. It first sets task status
	 * of the view panel to <code>running</code>, hides 3D viewer and clears canvas.
	 */
	protected void startTask() {

		// get view panel
		ObjectViewPanel panel = getViewPanel();

		// notify task start
		panel.setTaskStatus(this, true);

		// clear canvas
		panel.clearCanvas();
	}

	/**
	 * This method should be called as the last statement within the <code>succeeded</code> method of a <code>Plot3DTask</code>. It first sets the
	 * task status of the view panel to <code>end</code>, sets up 3D viewer with given parameters and shows the view panel.
	 *
	 * @param title
	 *            Title text.
	 * @param subTitle
	 *            Sub-title text.
	 * @param showLegend
	 *            True if the color legend should be shown.
	 * @param minVal
	 *            Minimum value of color legend (only used if legend is shown).
	 * @param maxVal
	 *            Maximum value of color legend (only used if legend is shown).
	 */
	protected void endTask(String title, String subTitle, boolean showLegend, double minVal, double maxVal) {

		// get view panel
		ObjectViewPanel panel = getViewPanel();

		// notify view panel
		panel.setTaskStatus(this, false);

		// setup viewer
		panel.setupViewer(title, subTitle, showLegend, minVal, maxVal);

		// show view panel
		panel.show();
	}

	/**
	 * Adds the given label to the viewer.
	 *
	 * @param label
	 *            Label to add to the viewer.
	 */
	protected void addLabel(equinox.viewer.Label label) {
		getViewPanel().addLabel(label);
	}

	/**
	 * Returns view panel.
	 *
	 * @return View panel.
	 */
	private ObjectViewPanel getViewPanel() {
		return (ObjectViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.OBJECT_VIEW);
	}
}
