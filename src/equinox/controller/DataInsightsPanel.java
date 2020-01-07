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

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.ui.DataInsightType;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Pagination;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/**
 * Class for data insights panel controller.
 *
 * @author Murat Artim
 * @date 26 Jul 2017
 * @time 10:41:48
 *
 */
public class DataInsightsPanel implements InternalInputSubPanel {

	/** Sub panel index. */
	public static final int NO_TARGET_PANEL = 0, SPECTRUM_COUNT_PANEL = 1, SPECTRUM_SIZE_PANEL = 2, PILOT_POINT_COUNT_PANEL = 3, DAMAGE_CONTRIBUTION_PANEL = 4;

	/** The owner panel. */
	private InputPanel owner_;

	/** Sub panels. */
	private DataInsightsSubPanel[] subPanels_;

	@FXML
	private VBox root_;

	@FXML
	private ComboBox<DataInsightType> target_;

	@FXML
	private Pagination pagination_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set data insight types
		target_.getItems().setAll(DataInsightType.values());

		// setup pagination page factory
		pagination_.setPageFactory(new Callback<Integer, Node>() {

			@Override
			public Node call(Integer pageIndex) {
				subPanels_[pageIndex].showing();
				return subPanels_[pageIndex].getRoot();
			}
		});

		// set current page
		pagination_.setCurrentPageIndex(0);
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {
		// no implementation
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public String getHeader() {
		return "AFT Data Insights";
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	/**
	 * Returns the sub panel at the demanded index.
	 *
	 * @param index
	 *            Index of the demanded panel.
	 * @return The sub panel at the demanded index.
	 */
	public DataInsightsSubPanel getSubPanel(int index) {
		return subPanels_[index];
	}

	/**
	 * Called when cancel clicked from within a sub panel.
	 */
	protected void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onTargetEntitySelected() {

		// no selection
		if (target_.getSelectionModel().isEmpty())
			return;

		// get selected image type
		DataInsightType selected = target_.getSelectionModel().getSelectedItem();

		// nothing selected
		if (selected == null)
			return;

		// set selected image panel
		for (int i = 0; i < subPanels_.length; i++) {
			if (selected.equals(subPanels_[i].getType())) {
				pagination_.setCurrentPageIndex(i);
				break;
			}
		}
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static DataInsightsPanel load(InputPanel owner) {

		try {

			// load fxml file
			final FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DataInsightsPanel.fxml"));
			final Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			final DataInsightsPanel controller = (DataInsightsPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;
			controller.subPanels_ = new DataInsightsSubPanel[5];
			controller.subPanels_[NO_TARGET_PANEL] = DataInsightsNoTargetPanel.load(controller);
			controller.subPanels_[SPECTRUM_COUNT_PANEL] = DataInsightsSpectrumCountPanel.load(controller);
			controller.subPanels_[SPECTRUM_SIZE_PANEL] = DataInsightsSpectrumSizePanel.load(controller);
			controller.subPanels_[PILOT_POINT_COUNT_PANEL] = DataInsightsPilotPointCountPanel.load(controller);
			controller.subPanels_[DAMAGE_CONTRIBUTION_PANEL] = DataInsightsDamageContributionPanel.load(controller);

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Interface for the data insights sub panel controllers.
	 *
	 * @author Murat Artim
	 * @date 26 Jul 2017
	 * @time 11:03:46
	 *
	 */
	public interface DataInsightsSubPanel extends Initializable {

		/**
		 * Returns the root container of this sub panel.
		 *
		 * @return The root container of this sub panel.
		 */
		VBox getRoot();

		/**
		 * Returns data insight type.
		 *
		 * @return Data insight type.
		 */
		DataInsightType getType();

		/**
		 * Called just before the panel is shown.
		 *
		 */
		void showing();
	}
}
