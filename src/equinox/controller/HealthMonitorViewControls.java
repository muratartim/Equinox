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

import equinox.data.EquinoxTheme;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Class for health monitor view controls controller.
 *
 * @author Murat Artim
 * @date 23 Jul 2018
 * @time 01:11:38
 */
public class HealthMonitorViewControls implements Initializable {

	/** The owner panel. */
	private HealthMonitorViewPanel owner_;

	@FXML
	private HBox root_;

	@FXML
	private MenuButton period_;

	@FXML
	private RadioMenuItem last2Hours_, last6Hours_, last12Hours_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set root to grow always
		HBox.setHgrow(root_, Priority.ALWAYS);

		// initialize period
		period_.setUserData(120L);
	}

	/**
	 * Returns the owner panel of this sub panel.
	 *
	 * @return The owner panel of this sub panel.
	 */
	public HealthMonitorViewPanel getOwner() {
		return owner_;
	}

	/**
	 * Returns the root of this controller.
	 *
	 * @return The root of this controller.
	 */
	public HBox getRoot() {
		return root_;
	}

	/**
	 * Returns selected data collection period.
	 *
	 * @return Data collection period.
	 */
	public long getPeriod() {
		return (long) period_.getUserData();
	}

	@FXML
	private void onPeriodSelected() {

		// get current period
		long currentPeriod = getPeriod();

		// update period according to selection
		if (last2Hours_.isSelected()) {
			period_.setUserData(120L);
			period_.setText("Show last 2 hours");
		}
		else if (last6Hours_.isSelected()) {
			period_.setUserData(360L);
			period_.setText("Show last 6 hours");
		}
		else if (last12Hours_.isSelected()) {
			period_.setUserData(720L);
			period_.setText("Show last 12 hours");
		}

		// there is change
		if (currentPeriod != getPeriod()) {
			owner_.requestServerStatistics();
		}
	}

	/**
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot text view panel.
	 */
	public static HealthMonitorViewControls load(HealthMonitorViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("HealthMonitorViewControls.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			HealthMonitorViewControls controller = (HealthMonitorViewControls) fxmlLoader.getController();

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