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

import java.awt.Container;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import equinox.data.EquinoxTheme;
import equinox.utility.PersistentButtonToggleGroup;
import equinox.viewer.ControlPanel;
import equinox.viewer.Equinox3DViewer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/**
 * Class for view control panel controller.
 *
 * @author Murat Artim
 * @date Sep 16, 2015
 * @time 9:23:00 AM
 */
public class ViewControlPanel implements Initializable {

	/** Owner panel. */
	private ControlPanel owner_;

	/** Abstract buttons. */
	private AbstractButton rotateButton_, zoomWindowButton_, zoomInButton_, zoomOutButton_, zoomExtendsButton_, topButton_, bottomButton_, leftButton_, rightButton_, frontButton_, backButton_, swButton_, seButton_, neButton_, nwButton_, saveButton_, axesButton_;

	@FXML
	private HBox root_;

	@FXML
	private ToggleButton rotate_, window_;

	@FXML
	private ColorPicker bgColor_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create and add toggle group to toggle buttons
		PersistentButtonToggleGroup tg = new PersistentButtonToggleGroup();
		rotate_.setToggleGroup(tg);
		rotate_.setSelected(true);
		window_.setToggleGroup(tg);

		// add change listener to toggle group
		tg.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {

			@Override
			public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {

				// rotate mode
				if (newValue.equals(rotate_)) {
					SwingUtilities.invokeLater(() -> {
						rotateButton_.doClick();
					});
				}

				// zoom window mode
				else if (newValue.equals(window_)) {
					SwingUtilities.invokeLater(() -> {
						zoomWindowButton_.doClick();
					});
				}
			}
		});
	}

	/**
	 * Returns the root container of this panel.
	 *
	 * @return The root container of this panel.
	 */
	public HBox getRoot() {
		return root_;
	}

	/**
	 * Returns the parent panel of this panel.
	 *
	 * @return The parent panel of this panel.
	 */
	public ControlPanel getOwner() {
		return owner_;
	}

	@FXML
	private void onZoomInClicked() {
		SwingUtilities.invokeLater(() -> {
			zoomInButton_.doClick();
		});
	}

	@FXML
	private void onZoomOutClicked() {
		SwingUtilities.invokeLater(() -> {
			zoomOutButton_.doClick();
		});
	}

	@FXML
	public void onZoomExtendsClicked() {
		SwingUtilities.invokeLater(() -> {
			zoomExtendsButton_.doClick();
		});
	}

	@FXML
	private void onTopViewClicked() {
		SwingUtilities.invokeLater(() -> {
			topButton_.doClick();
		});
	}

	@FXML
	private void onBottomViewClicked() {
		SwingUtilities.invokeLater(() -> {
			bottomButton_.doClick();
		});
	}

	@FXML
	private void onLeftViewClicked() {
		SwingUtilities.invokeLater(() -> {
			leftButton_.doClick();
		});
	}

	@FXML
	private void onRightViewClicked() {
		SwingUtilities.invokeLater(() -> {
			rightButton_.doClick();
		});
	}

	@FXML
	private void onFrontViewClicked() {
		SwingUtilities.invokeLater(() -> {
			frontButton_.doClick();
		});
	}

	@FXML
	private void onBackViewClicked() {
		SwingUtilities.invokeLater(() -> {
			backButton_.doClick();
		});
	}

	@FXML
	private void onSWViewClicked() {
		SwingUtilities.invokeLater(() -> {
			swButton_.doClick();
		});
	}

	@FXML
	private void onSEViewClicked() {
		SwingUtilities.invokeLater(() -> {
			seButton_.doClick();
		});
	}

	@FXML
	private void onNEViewClicked() {
		SwingUtilities.invokeLater(() -> {
			neButton_.doClick();
		});
	}

	@FXML
	private void onNWViewClicked() {
		SwingUtilities.invokeLater(() -> {
			nwButton_.doClick();
		});
	}

	@FXML
	private void onAxesClicked() {
		SwingUtilities.invokeLater(() -> {
			axesButton_.doClick();
		});
	}

	@FXML
	private void onSaveClicked() {
		SwingUtilities.invokeLater(() -> {
			saveButton_.doClick();
		});
	}

	@FXML
	private void onBackgroundColorClicked() {
		SwingUtilities.invokeLater(() -> {
			Color color = bgColor_.getValue();
			owner_.getOwner().getRenderer().SetBackground(color.getRed(), color.getGreen(), color.getBlue());
		});
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @param viewer
	 *            Viewer.
	 * @return The newly loaded plot column panel.
	 */
	public static ViewControlPanel load(ControlPanel owner, Equinox3DViewer viewer) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ViewControlPanel.fxml"));
			fxmlLoader.load();

			// get controller
			ViewControlPanel controller = (ViewControlPanel) fxmlLoader.getController();

			// set attributes
			controller.owner_ = owner;

			// set abstract buttons
			setAbstractButtons(controller, viewer);

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sets abstract buttons.
	 *
	 * @param controller
	 *            Controller.
	 * @param viewer
	 *            Viewer.
	 */
	private static void setAbstractButtons(ViewControlPanel controller, Equinox3DViewer viewer) {

		// get tool bars
		Container container = viewer.getContentPane();
		JToolBar toolBar = (JToolBar) container.getComponent(2);
		JToolBar tb1 = (JToolBar) toolBar.getComponent(0);
		JToolBar tb2 = (JToolBar) toolBar.getComponent(1);

		// set buttons
		controller.rotateButton_ = (AbstractButton) tb1.getComponent(0);
		controller.zoomWindowButton_ = (AbstractButton) tb1.getComponent(1);
		controller.zoomInButton_ = (AbstractButton) tb1.getComponent(3);
		controller.zoomOutButton_ = (AbstractButton) tb1.getComponent(4);
		controller.zoomExtendsButton_ = (AbstractButton) tb1.getComponent(5);
		controller.topButton_ = (AbstractButton) tb2.getComponent(0);
		controller.bottomButton_ = (AbstractButton) tb2.getComponent(1);
		controller.leftButton_ = (AbstractButton) tb2.getComponent(2);
		controller.rightButton_ = (AbstractButton) tb2.getComponent(3);
		controller.frontButton_ = (AbstractButton) tb2.getComponent(4);
		controller.backButton_ = (AbstractButton) tb2.getComponent(5);
		controller.swButton_ = (AbstractButton) tb2.getComponent(7);
		controller.seButton_ = (AbstractButton) tb2.getComponent(8);
		controller.neButton_ = (AbstractButton) tb2.getComponent(9);
		controller.nwButton_ = (AbstractButton) tb2.getComponent(10);
		controller.saveButton_ = viewer.getJMenuBar().getMenu(0).getItem(0);
		controller.axesButton_ = viewer.getJMenuBar().getMenu(1).getItem(0);
	}
}
