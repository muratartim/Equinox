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
package equinox.viewer;

import equinox.controller.ColorLegendPanel;
import equinox.utility.MouseDispatcher;
import inf.common.util.vtk.mvtkPanel1;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

/**
 * Class for legend panel.
 *
 * @author Murat Artim
 * @date Sep 15, 2015
 * @time 9:44:41 AM
 */
public class LegendPanel extends JFXPanel {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Owner window. */
	private final Equinox3DViewer owner_;

	/** Color legend panel. */
	private final ColorLegendPanel colorLegendPanel_;

	/**
	 * Creates legend panel.
	 *
	 * @param owner
	 *            The owner window.
	 * @param vtk
	 *            VTK panel.
	 */
	public LegendPanel(Equinox3DViewer owner, mvtkPanel1 vtk) {

		// create panel
		super();

		// set owner
		owner_ = owner;

		// create color legend panel
		colorLegendPanel_ = ColorLegendPanel.load(LegendPanel.this);

		// create and set scene to panel
		setScene(new Scene(colorLegendPanel_.getRoot()));

		// set mouse dispatcher
		MouseDispatcher.setMouseDispatcher(this, vtk);
	}

	/**
	 * Returns the owner window.
	 *
	 * @return The owner window.
	 */
	public Equinox3DViewer getOwner() {
		return owner_;
	}

	/**
	 * Returns the color legend panel.
	 *
	 * @return The color legend panel.
	 */
	public ColorLegendPanel getColorLegendPanel() {
		return colorLegendPanel_;
	}
}
