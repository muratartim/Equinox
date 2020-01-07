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

import equinox.controller.DataInsightsPanel.DataInsightsSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.ui.DataInsightType;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

/**
 * Class for data insights no target panel controller.
 *
 * @author Murat Artim
 * @date 26 Jul 2017
 * @time 10:59:47
 *
 */
public class DataInsightsNoTargetPanel implements DataInsightsSubPanel {

	/** The owner panel. */
	private DataInsightsPanel owner_;

	@FXML
	private VBox root_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@Override
	public VBox getRoot() {
		return root_;
	}

	@Override
	public DataInsightType getType() {
		return null;
	}

	@Override
	public void showing() {
		// no implementation
	}

	@FXML
	private void onCancelClicked() {
		owner_.onCancelClicked();
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static DataInsightsNoTargetPanel load(DataInsightsPanel owner) {

		try {

			// load fxml file
			final FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DataInsightsNoTargetPanel.fxml"));
			final Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			final DataInsightsNoTargetPanel controller = (DataInsightsNoTargetPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}
