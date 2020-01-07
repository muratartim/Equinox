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

import java.awt.Color;

import inf.common.util.vtk.VtkConvert;
import vtk.vtkFollower;
import vtk.vtkPolyDataMapper;
import vtk.vtkRenderer;
import vtk.vtkTransform;
import vtk.vtkTransformPolyDataFilter;
import vtk.vtkVectorText;

/**
 * Class for label object to be displayed in the viewer. The label object is registered to the active camera of the viewer in order to make it
 * readable regardless of screen rotation.
 *
 * @version 1.0
 * @author Murat Artim
 * @time 10:03:24 PM
 * @date Nov 29, 2009
 *
 */
public class Label {

	/** Follower of label. */
	private final vtkFollower follower_;

	/** Text of label. */
	private final vtkVectorText text_;

	/**
	 * Creates Label object to be displayed in the given viewer.
	 *
	 * @param text
	 *            Text of label.
	 * @param x
	 *            X coordinate.
	 * @param y
	 *            Y coordinate.
	 * @param z
	 *            Z coordinate.
	 */
	public Label(String text, double x, double y, double z) {

		// create follower
		follower_ = new vtkFollower();

		// create vector text
		text_ = new vtkVectorText();

		// create data mapper
		vtkPolyDataMapper m = new vtkPolyDataMapper();

		// create transform
		vtkTransform t = new vtkTransform();

		// create data filter
		vtkTransformPolyDataFilter f = new vtkTransformPolyDataFilter();

		// set transform to filter
		f.SetTransform(t);

		// set text output to filter
		f.SetInput(text_.GetOutput());

		// set filter output to mapper
		m.SetInput(f.GetOutput());

		// set text to vector text
		text_.SetText(text);

		// set mapper to follower
		follower_.SetMapper(m);

		// set default color to follower
		follower_.GetProperty().SetColor(VtkConvert.convert(Color.BLACK));

		// set default scale to follower
		follower_.SetScale(1.25D);

		// set default position to follower
		follower_.SetPosition(x, y, z);

		// set default visibility
		follower_.SetVisibility(VtkConvert.convert(true));
	}

	/**
	 * Adds this label to the given renderer.
	 *
	 * @param renderer
	 *            Renderer to add this label.
	 */
	public void addToRenderer(vtkRenderer renderer) {

		// add follower to renderer
		renderer.AddActor(follower_);

		// set active camera of renderer to follower
		follower_.SetCamera(renderer.GetActiveCamera());
	}

	/**
	 * Sets given text to label.
	 *
	 * @param text
	 *            Text to set.
	 */
	public void setText(String text) {
		text_.SetText(text);
	}

	/**
	 * Sets given color to label.
	 *
	 * @param c
	 *            Color to set.
	 */
	public void setColor(Color c) {
		follower_.GetProperty().SetColor(VtkConvert.convert(c));
	}

	/**
	 * Sets text height scale to label.
	 *
	 * @param scale
	 *            Text height scale.
	 */
	public void setScale(double scale) {
		follower_.SetScale(scale);
	}

	/**
	 * Sets the position of the label.
	 *
	 * @param x
	 *            X coordinate.
	 * @param y
	 *            Y coordinate.
	 * @param z
	 *            Z coordinate.
	 */
	public void setPosition(double x, double y, double z) {
		follower_.SetPosition(x, y, z);
	}

	/**
	 * Returns the text of label.
	 *
	 * @return The text of label.
	 */
	public String getText() {
		return text_.GetText();
	}

	/**
	 * Returns the color of the label.
	 *
	 * @return The color of the label.
	 */
	public Color getColor() {
		return VtkConvert.convert(follower_.GetProperty().GetColor());
	}

	/**
	 * Returns the position of the label.
	 *
	 * @return Array containing the position of the label.
	 */
	public double[] getPosition() {
		return follower_.GetPosition();
	}

	/**
	 * Unregisters this label from the renderer.
	 *
	 * @param renderer
	 *            Renderer to remove the follower.
	 */
	public void unregister(vtkRenderer renderer) {
		renderer.RemoveActor(follower_);
		text_.UnRegisterAllOutputs();
	}
}
