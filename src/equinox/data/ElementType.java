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

import javafx.scene.paint.Color;

/**
 * Class for element type.
 *
 * @author Murat Artim
 * @date Jul 15, 2015
 * @time 2:14:44 PM
 */
public class ElementType {

	/** Element type names. */
	public static final String BEAM = "BEAM", QUAD = "QUAD", ROD = "ROD", TRIA = "TRIA", SHEAR = "SHEAR";

	/** Type name. */
	private final String name_;

	/** Opacity. */
	private double opacity_;

	/** True if outlines should be drawn. */
	private boolean selected_, outlines_;

	/** Type color. */
	private Color color_;

	/**
	 * Creates element type.
	 *
	 * @param name
	 *            Type name.
	 */
	public ElementType(String name) {
		name_ = name;
	}

	/**
	 * Sets selected option.
	 *
	 * @param selected
	 *            True if element type should be drawn.
	 */
	public void setSelected(boolean selected) {
		selected_ = selected;
	}

	/**
	 * Sets opacity.
	 *
	 * @param opacity
	 *            Opacity to set.
	 */
	public void setOpacity(int opacity) {
		opacity_ = opacity / 100.0;
	}

	/**
	 * Sets draw outlines option.
	 *
	 * @param outlines
	 *            True to draw element outlines.
	 */
	public void setOutlines(boolean outlines) {
		outlines_ = outlines;
	}

	/**
	 * Sets element type color.
	 *
	 * @param color
	 *            Color.
	 */
	public void setColor(Color color) {
		color_ = color;
	}

	/**
	 * Returns element type name.
	 *
	 * @return Element type name.
	 */
	public String getName() {
		return name_;
	}

	/**
	 * Returns true if element type is selected.
	 *
	 * @return True if element type is selected.
	 */
	public boolean getSelected() {
		return selected_;
	}

	/**
	 * Returns opacity.
	 *
	 * @return Opacity.
	 */
	public double getOpacity() {
		return opacity_;
	}

	/**
	 * Returns true if outlines should be drawn.
	 *
	 * @return True if outlines should be drawn.
	 */
	public boolean getOutlines() {
		return outlines_;
	}

	/**
	 * Returns element type color.
	 *
	 * @return Element type color.
	 */
	public Color getColor() {
		return color_;
	}
}
