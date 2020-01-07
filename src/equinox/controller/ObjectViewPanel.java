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
package equinox.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import equinox.Equinox;
import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.ui.TableItem;
import equinox.serverUtilities.ServerUtility;
import equinox.task.Plot3DTask;
import equinox.utility.Utility;
import equinox.viewer.Equinox3DViewer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.Pagination;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Class for 3D view panel controller.
 *
 * @author Murat Artim
 * @date Apr 26, 2016
 * @time 1:17:02 PM
 */
public class ObjectViewPanel implements InternalViewSubPanel {

	/** The owner panel. */
	private ViewPanel owner_;

	/** 3D viewer. */
	private Equinox3DViewer viewer_;

	/** Progress panel. */
	private Progress3DPanel progressPanel_;

	/** Task run indicator. */
	private volatile boolean taskRunning_ = false;

	@FXML
	private VBox root_;

	@FXML
	private StackPane stack_;

	@FXML
	private TreeTableView<TableItem> table_;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		// load progress panel
		progressPanel_ = Progress3DPanel.load(this);

		// remove place holder
		table_.setPlaceholder(null);
	}

	@Override
	public ViewPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public HBox getControls() {
		return null;
	}

	@Override
	public String getHeader() {
		return "3D Object View";
	}

	@Override
	public void start() {

		// create viewer
		viewer_ = create3DViewer();
	}

	@Override
	public void showing() {
		if (viewer_ != null && !viewer_.isVisible() && !taskRunning_) {

			// set bounds
			Pagination pagination = owner_.getPagination();
			Bounds bounds = pagination.localToScreen(pagination.getBoundsInLocal());
			if (bounds != null) {
				int minx = (int) bounds.getMinX() + 10;
				int miny = (int) bounds.getMinY() + 10;
				int width = (int) bounds.getWidth() - 20;
				int height = (int) bounds.getHeight() - 20;
				viewer_.setBounds(minx, miny, width, height);
			}

			// show viewer
			viewer_.setVisible(true);

			// zoom extends
			viewer_.getControlPanel().getViewControlPanel().onZoomExtendsClicked();
		}
	}

	@Override
	public void hiding() {
		if (viewer_ != null && viewer_.isVisible()) {
			viewer_.setVisible(false);
		}
	}

	@Override
	public boolean canSaveView() {
		return false;
	}

	@Override
	public void saveView() {
		// no implementation
	}

	@Override
	public String getViewName() {
		return null;
	}

	@Override
	public WritableImage getViewImage() {
		return null;
	}

	/**
	 * Returns true if 3D viewer is enabled.
	 *
	 * @return True if 3D viewer is enabled.
	 */
	public boolean isEnabled() {
		return viewer_ != null;
	}

	/**
	 * Shows this panel.
	 */
	public void show() {

		// get current panel index
		int index = owner_.getCurrentSubPanelIndex();

		// already shown
		if (index == ViewPanel.OBJECT_VIEW) {
			showing();
		}
		else {
			owner_.showSubPanel(ViewPanel.OBJECT_VIEW);
		}
	}

	/**
	 * Sets task status.
	 *
	 * @param task
	 *            The calling task.
	 * @param isRunning
	 *            True if task is running.
	 */
	public void setTaskStatus(Plot3DTask<?> task, boolean isRunning) {

		// set status
		taskRunning_ = isRunning;

		// set place holder
		Platform.runLater(() -> {

			// bind task to progress indicator
			if (taskRunning_) {
				progressPanel_.getProgress().progressProperty().unbind();
				progressPanel_.getProgress().progressProperty().bind(task.progressProperty());
			}
			else {
				progressPanel_.getProgress().progressProperty().unbind();
			}

			// set place holder
			table_.setPlaceholder(taskRunning_ ? progressPanel_.getRoot() : null);
		});
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
		viewer_.setupViewer(title, subTitle, showLegend, minVal, maxVal);
	}

	/**
	 * Hides 3D viewer and clears canvas.
	 */
	public void clearCanvas() {
		if (viewer_ != null) {
			if (viewer_.isVisible()) {
				viewer_.setVisible(false);
			}
			viewer_.clear();
		}
	}

	/**
	 * Adds the given label to the viewer.
	 *
	 * @param label
	 *            Label to add to the viewer.
	 */
	public void addLabel(equinox.viewer.Label label) {
		viewer_.addLabel(label);
	}

	/**
	 * Creates and returns 3D viewer. Note that viewer is created only for Windows OS with 32bit architecture.
	 *
	 * @return 3D viewer, or null if it is not Windows OS with 32bit architecture.
	 */
	private Equinox3DViewer create3DViewer() {
		if (Equinox.OS_TYPE.equals(ServerUtility.WINDOWS) && Equinox.OS_ARCH.equals(ServerUtility.X86))
			return new Equinox3DViewer(this);
		return null;
	}

	/**
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot text view panel.
	 */
	public static ObjectViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ObjectViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			ObjectViewPanel controller = (ObjectViewPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
