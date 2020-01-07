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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import equinox.Equinox;
import equinox.controller.ObjectViewPanel;
import inf.common.util.vtk.mvtkPanel1;
import inf.v3d.obj.Circle;
import inf.v3d.view.Viewer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.control.Pagination;
import javafx.stage.Stage;
import vtk.vtkRenderer;

/**
 * This is the 3D viewer class.
 *
 * @author Murat Artim
 * @date Dec 5, 2013
 * @time 4:37:57 PM
 */
public class Equinox3DViewer extends Viewer {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** The owner main screen. */
	private final ObjectViewPanel owner_;

	/** Renderer of the viewer. */
	private final vtkRenderer renderer_;

	/** Vector storing the labels of the plot. */
	private final Vector<Label> labels_ = new Vector<>();

	/** Header panel. */
	private final HeaderPanel headerPanel_;

	/** Legend panel. */
	private final LegendPanel legendPanel_;

	/** Control panel. */
	private final ControlPanel controlPanel_;

	/** True if the color legend is shown. */
	private volatile boolean isLegendShown_ = false;

	/**
	 * Creates Equinox 3D viewer.
	 *
	 * @param owner
	 *            The owner panel.
	 */
	public Equinox3DViewer(ObjectViewPanel owner) {

		// create viewer
		super();

		// remove frame decorations
		dispose();
		setUndecorated(true);
		setType(Type.UTILITY);

		// get stack pane of owner panel
		Pagination pagination = owner.getOwner().getPagination();

		// bind frame bounds to stack pane bounds
		pagination.boundsInLocalProperty().addListener(new ChangeListener<Bounds>() {

			@Override
			public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
				Bounds bounds = pagination.localToScreen(newValue);
				if (bounds != null) {
					int minx = (int) bounds.getMinX() + 10;
					int miny = (int) bounds.getMinY() + 10;
					int width = (int) bounds.getWidth() - 20;
					int height = (int) bounds.getHeight() - 20;
					setBounds(minx, miny, width, height);
				}
			}
		});

		// bind frame position to stage coordinates
		Stage stage = owner.getOwner().getOwner().getOwner().getStage();
		stage.xProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				Bounds bounds = pagination.localToScreen(pagination.getBoundsInLocal());
				if (bounds != null) {
					int minx = (int) bounds.getMinX() + 10;
					int miny = (int) bounds.getMinY() + 10;
					int width = (int) bounds.getWidth() - 20;
					int height = (int) bounds.getHeight() - 20;
					setBounds(minx, miny, width, height);
				}
			}
		});
		stage.yProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				Bounds bounds = pagination.localToScreen(pagination.getBoundsInLocal());
				if (bounds != null) {
					int minx = (int) bounds.getMinX() + 10;
					int miny = (int) bounds.getMinY() + 10;
					int width = (int) bounds.getWidth() - 20;
					int height = (int) bounds.getHeight() - 20;
					setBounds(minx, miny, width, height);
				}
			}
		});

		// set owner
		owner_ = owner;

		// set title and icon
		setTitle("Equinox 3D Viewer");
		setIconImage(new ImageIcon(Equinox.class.getResource("image/3dviewer.png")).getImage());

		// set canvas properties
		getCanvas().GetRenderer().GetVTKWindow().GlobalWarningDisplayOff();
		getCanvas().GetRenderer().SetBackground(1.0, 1.0, 1.0);

		// get VTK panel
		JPanel vtkPanel = (JPanel) getContentPane().getComponent(1);
		mvtkPanel1 vtk = (mvtkPanel1) vtkPanel.getComponent(0);
		vtkPanel.removeAll();

		// create header and legend panels
		headerPanel_ = new HeaderPanel(this, vtk);
		legendPanel_ = new LegendPanel(this, vtk);
		controlPanel_ = new ControlPanel(this);

		// create layered pane and add panels
		JLayeredPane layeredPane = new JLayeredPane();
		layeredPane.add(vtk, Integer.valueOf(0));
		layeredPane.add(headerPanel_, Integer.valueOf(1));
		layeredPane.add(legendPanel_, Integer.valueOf(2));
		layeredPane.add(controlPanel_, Integer.valueOf(3));

		// listen for layered pane size changes
		layeredPane.addComponentListener(new ComponentListener() {

			@Override
			public void componentResized(ComponentEvent e) {
				vtk.setBounds(layeredPane.getBounds());
				headerPanel_.setBounds(10, 10, 300, 44);
				legendPanel_.setBounds(10, 64, 100, 312);
				int x = Math.round((layeredPane.getWidth() - 579) / 2f);
				int y = layeredPane.getHeight() - 54 - 10;
				controlPanel_.setBounds(x, y, 579, 54);
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				// no implementation
			}

			@Override
			public void componentShown(ComponentEvent e) {
				vtk.setBounds(layeredPane.getBounds());
				headerPanel_.setBounds(10, 10, 300, 44);
				legendPanel_.setBounds(10, 64, 100, 312);
				int x = Math.round((layeredPane.getWidth() - 579) / 2f);
				int y = layeredPane.getHeight() - 54 - 10;
				controlPanel_.setBounds(x, y, 579, 54);
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				// no implementation
			}
		});

		// add layered pane to VTK panel
		vtkPanel.setLayout(new BorderLayout(0, 0));
		vtkPanel.add(layeredPane, BorderLayout.CENTER);

		// set renderer
		renderer_ = vtk.GetRenderer();

		// remove tool bar
		getContentPane().remove(2);

		// remove menu
		setJMenuBar(null);

		// set size of frame
		setPreferredSize(new Dimension(894, 694));
		setAlwaysOnTop(true);

		// clear viewer
		clear();

		// pack window
		pack();

		// set window close operation
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		// set location to center of screen
		setLocationRelativeTo(null);

		// hide header and legend panels
		headerPanel_.setVisible(false);
		legendPanel_.setVisible(false);
	}

	/**
	 * Returns the owner panel of the viewer.
	 *
	 * @return The owner panel of the viewer.
	 */
	public ObjectViewPanel getOwnerPanel() {
		return owner_;
	}

	/**
	 * Returns VTK renderer.
	 *
	 * @return VTK renderer.
	 */
	public vtkRenderer getVTKRenderer() {
		return renderer_;
	}

	/**
	 * Returns control panel.
	 *
	 * @return Control panel.
	 */
	public ControlPanel getControlPanel() {
		return controlPanel_;
	}

	/**
	 * Adds the given label to the viewer.
	 *
	 * @param label
	 *            Label to add to the viewer.
	 */
	public void addLabel(Label label) {
		labels_.add(label);
		label.addToRenderer(renderer_);
	}

	@Override
	public void clear() {

		// call super method
		super.clear();

		// unregister labels
		for (int i = 0; i < labels_.size(); i++) {
			labels_.get(i).unregister(renderer_);
		}

		// remove all labels
		labels_.clear();

		// draw invisible circle
		Circle c = new Circle(0.0);
		c.setCenter(0.0, 0.0, 0.0);
	}

	/**
	 * Sets up viewer.
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
	public void setupViewer(String title, String subTitle, boolean showLegend, double minVal, double maxVal) {

		// set header
		headerPanel_.getViewHeaderPanel().setHeader(title, subTitle);
		headerPanel_.setVisible(true);

		// setup legend
		if (showLegend) {
			legendPanel_.getColorLegendPanel().setupLegend(minVal, maxVal);
		}

		// do not show color legend
		if (!showLegend && isLegendShown_) {
			legendPanel_.setVisible(false);
			isLegendShown_ = false;
		}

		// show color legend
		else if (showLegend && !isLegendShown_) {
			legendPanel_.setVisible(true);
			isLegendShown_ = true;
		}
	}
}
