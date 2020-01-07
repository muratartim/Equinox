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

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.ToggleSwitch;

import control.validationField.IntegerValidationField;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.SearchInput;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.VBox;

/**
 * Class for search engine settings panel controller.
 *
 * @author Murat Artim
 * @date 18 Jan 2017
 * @time 15:37:11
 */
public class SearchEngineSettingsPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private IntegerValidationField maxHits_;

	@FXML
	private ChoiceBox<String> operator_, orderBy_, order_;

	@FXML
	private ToggleSwitch ignoreCase_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// add listener to max hits field
		maxHits_.setDefaultValue(100);
		maxHits_.setMinimumValue(1, true);

		// add logical operators
		operator_.getItems().clear();
		operator_.getItems().add("AND");
		operator_.getItems().add("OR");
		operator_.getSelectionModel().select(0);

		// add order by criteria
		orderBy_.getItems().clear();
		orderBy_.getItems().add(SearchInput.NAME);
		orderBy_.getItems().add(SearchInput.PROGRAM);
		orderBy_.getItems().add(SearchInput.SECTION);
		orderBy_.getItems().add(SearchInput.MISSION);
		orderBy_.getItems().add(SearchInput.DELIVERY);
		orderBy_.getSelectionModel().select(0);

		// add results ordering
		order_.getItems().clear();
		order_.getItems().add("Ascending");
		order_.getItems().add("Descending");
		order_.getSelectionModel().select(0);

		// set data
		maxHits_.setUserData(100);
		operator_.setUserData("AND");
		ignoreCase_.setUserData(true);
		orderBy_.setUserData(SearchInput.NAME);
		order_.setUserData("Ascending");
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {
		maxHits_.setText(((Integer) maxHits_.getUserData()).toString());
		operator_.getSelectionModel().select((String) operator_.getUserData());
		ignoreCase_.setSelected((boolean) ignoreCase_.getUserData());
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public String getHeader() {
		return "Search Engine Settings";
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@FXML
	private void onOkClicked() {

		// check max hits
		String message = maxHits_.validate();
		if (message != null) {
			final PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(maxHits_);
			return;
		}

		// set data
		maxHits_.setUserData(Integer.parseInt(maxHits_.getText()));
		operator_.setUserData(operator_.getSelectionModel().getSelectedItem());
		ignoreCase_.setUserData(ignoreCase_.isSelected());
		orderBy_.setUserData(orderBy_.getSelectionModel().getSelectedItem());
		order_.setUserData(order_.getSelectionModel().getSelectedItem());

		// get back to file panel
		owner_.showSubPanel(owner_.getPreviousSubPanelIndex());
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(owner_.getPreviousSubPanelIndex());
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to search and download spectra from AFM database", "Search Engine Settings");
	}

	@FXML
	private void onResetClicked() {

		// reset options
		maxHits_.reset();
		operator_.getSelectionModel().select(0);
		ignoreCase_.setSelected(true);
		orderBy_.getSelectionModel().select(0);
		order_.getSelectionModel().select(0);

		// reset data
		maxHits_.setUserData(100);
		operator_.setUserData("AND");
		ignoreCase_.setUserData(true);
		orderBy_.setUserData(SearchInput.NAME);
		order_.setUserData("Ascending");
	}

	/**
	 * Sets search engine settings.
	 *
	 * @param input
	 *            Search input.
	 */
	public void setEngineSettings(SearchInput input) {
		input.setCase((boolean) ignoreCase_.getUserData());
		input.setMaxHits((Integer) maxHits_.getUserData());
		input.setOperator(((String) operator_.getUserData()).equals("AND"));
		input.setOrderByCriteria((String) orderBy_.getUserData());
		input.setOrder(((String) order_.getUserData()).equals("Ascending"));
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static SearchEngineSettingsPanel load(InputPanel owner) {

		try {

			// load fxml file
			final FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("SearchEngineSettingsPanel.fxml"));
			final Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			final SearchEngineSettingsPanel controller = (SearchEngineSettingsPanel) fxmlLoader.getController();

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
