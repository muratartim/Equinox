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

import equinox.controller.ViewControlPanel;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

/**
 * Class for control panel.
 *
 * @author Murat Artim
 * @date Sep 16, 2015
 * @time 9:28:50 AM
 */
public class ControlPanel extends JFXPanel {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Owner window. */
	private final Equinox3DViewer owner_;

	/** View control panel. */
	private final ViewControlPanel viewControlPanel_;

	/**
	 * Creates header panel.
	 *
	 * @param owner
	 *            The owner window.
	 */
	public ControlPanel(Equinox3DViewer owner) {

		// create panel
		super();

		// set owner
		owner_ = owner;

		// create view header panel
		viewControlPanel_ = ViewControlPanel.load(ControlPanel.this, owner_);

		// create and set scene to panel
		setScene(new Scene(viewControlPanel_.getRoot()));
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
	 * Returns the view control panel.
	 *
	 * @return The view control panel.
	 */
	public ViewControlPanel getViewControlPanel() {
		return viewControlPanel_;
	}
}
