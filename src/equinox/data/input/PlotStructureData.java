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
package equinox.data.input;

import javafx.scene.paint.Color;

/**
 * Class for A/C structure plot data.
 *
 * @author Murat Artim
 * @date Jul 10, 2015
 * @time 2:42:57 PM
 */
public class PlotStructureData {

	/** True if selected. */
	private final boolean selected_;

	/** Color of data. */
	private final Color color_;

	/**
	 * Creates A/C structure plot data.
	 * 
	 * @param selected
	 *            True if selected.
	 * @param color
	 *            Color of data.
	 */
	public PlotStructureData(boolean selected, Color color) {
		selected_ = selected;
		color_ = color;
	}

	/**
	 * Returns true if selected.
	 * 
	 * @return True if selected.
	 */
	public boolean isSelected() {
		return selected_;
	}

	/**
	 * Returns color.
	 * 
	 * @return Color.
	 */
	public Color getColor() {
		return color_;
	}
}
