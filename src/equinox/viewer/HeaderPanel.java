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

import equinox.controller.ViewHeaderPanel;
import equinox.utility.MouseDispatcher;
import inf.common.util.vtk.mvtkPanel1;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

/**
 * Class for header panel.
 *
 * @author Murat Artim
 * @date Sep 15, 2015
 * @time 10:21:30 AM
 */
public class HeaderPanel extends JFXPanel {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Owner window. */
	private final Equinox3DViewer owner_;

	/** View header panel. */
	private final ViewHeaderPanel viewHeaderPanel_;

	/**
	 * Creates header panel.
	 *
	 * @param owner
	 *            The owner window.
	 * @param vtk
	 *            VTK panel.
	 */
	public HeaderPanel(Equinox3DViewer owner, mvtkPanel1 vtk) {

		// create panel
		super();

		// set owner
		owner_ = owner;

		// create view header panel
		viewHeaderPanel_ = ViewHeaderPanel.load(HeaderPanel.this);

		// create and set scene to panel
		setScene(new Scene(viewHeaderPanel_.getRoot()));

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
	 * Returns the view header panel.
	 *
	 * @return The view header panel.
	 */
	public ViewHeaderPanel getViewHeaderPanel() {
		return viewHeaderPanel_;
	}
}
