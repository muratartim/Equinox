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
package equinox.plugin;

import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;

/**
 * Interface for view sub panel.
 *
 * @author Murat Artim
 * @date Mar 26, 2015
 * @time 6:35:29 PM
 */
public interface ViewSubPanel {

	/**
	 * Called only once, after the object structure is initialized.
	 *
	 */
	void start();

	/**
	 * Called just before the panel is shown.
	 *
	 */
	void showing();

	/**
	 * Called just before the panel is hidden.
	 *
	 */
	void hiding();

	/**
	 * Returns the control panel of this sub panel.
	 *
	 * @return The control panel of this sub panel.
	 */
	HBox getControls();

	/**
	 * Returns the header of this sub panel.
	 *
	 * @return The header of this sub panel.
	 */
	String getHeader();

	/**
	 * Returns the root of this controller.
	 *
	 * @return The root of this controller.
	 */
	Parent getRoot();

	/**
	 * Returns true if the view can be saved.
	 *
	 * @return True if the view can be saved.
	 */
	boolean canSaveView();

	/**
	 * Saves the current view of this panel to output file.
	 */
	void saveView();

	/**
	 * Returns the current view plot name.
	 *
	 * @return The current view plot name.
	 */
	String getViewName();

	/**
	 * Returns the current view image.
	 *
	 * @return The current view image.
	 */
	WritableImage getViewImage();
}
