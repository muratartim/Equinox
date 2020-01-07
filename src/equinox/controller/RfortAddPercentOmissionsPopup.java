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

import control.validationField.IntegerValidationField;
import equinox.data.EquinoxTheme;
import equinox.data.ui.RfortOmission;
import equinox.data.ui.RfortPercentOmission;
import equinox.font.IconicFont;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;

/**
 * Class for add percentage omissions popup controller.
 *
 * @author Murat Artim
 * @date Apr 14, 2016
 * @time 3:40:34 PM
 */
public class RfortAddPercentOmissionsPopup implements Initializable {

	/** The owner panel. */
	private RfortPercentOmissionAddingPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	@FXML
	private VBox root_;

	@FXML
	private IntegerValidationField omission_;

	@FXML
	private ListView<RfortPercentOmission> omissions_;

	@FXML
	private Button reset_, delete_, ok_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup omission levels list
		omissions_.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<RfortPercentOmission>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends RfortPercentOmission> c) {
				delete_.setDisable(omissions_.getSelectionModel().getSelectedItems().isEmpty());
			}
		});
		omissions_.getItems().addListener(new ListChangeListener<RfortPercentOmission>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends RfortPercentOmission> c) {
				reset_.setDisable(omissions_.getItems().isEmpty());
			}
		});
		omissions_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		omissions_.setPlaceholder(new Label("No percent omission added."));

		// setup validation fields
		omission_.setDefaultValue(null);
		omission_.setMinimumValue(1, true);
		omission_.setMaximumValue(100, true);
	}

	/**
	 * Returns the owner panel.
	 *
	 * @return The owner panel.
	 */
	public RfortPercentOmissionAddingPanel getOwner() {
		return owner_;
	}

	/**
	 * Shows this panel.
	 *
	 * @param node
	 *            Popup node.
	 */
	public void show(Node node) {

		// already shown
		if (isShown_)
			return;

		// create pop-over
		popOver_ = new PopOver();
		popOver_.setArrowLocation(ArrowLocation.LEFT_TOP);
		popOver_.setDetached(false);
		popOver_.setHideOnEscape(false);
		popOver_.setAutoHide(false);
		popOver_.setContentNode(root_);

		// set showing handler
		popOver_.setOnShowing(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				owner_.getOwner().getRoot().setMouseTransparent(true);
				isShown_ = true;
			}
		});

		// set hidden handler
		popOver_.setOnHidden(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				owner_.getOwner().getRoot().setMouseTransparent(false);
				isShown_ = false;
			}
		});

		// reset panel
		omission_.reset();
		omissions_.getItems().clear();

		// show popup
		popOver_.show(node);
	}

	@FXML
	private void onAddOmissionClicked() {

		// get omission level
		try {

			// check value
			String message = omission_.validate();
			if (message != null) {
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(omission_);
				return;
			}

			// get omission level
			RfortPercentOmission ol = new RfortPercentOmission(Integer.parseInt(omission_.getText()));
			if (!omissions_.getItems().contains(ol)) {
				omissions_.getItems().add(ol);
			}
			omission_.reset();
		}

		// invalid value entered (ignore)
		catch (NumberFormatException e) {
			String message = "Please enter a valid omission level to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(omission_);
		}
	}

	@FXML
	private void onDeleteClicked() {
		omissions_.getItems().removeAll(omissions_.getSelectionModel().getSelectedItems());
	}

	@FXML
	private void onResetClicked() {
		omissions_.getItems().clear();
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	@FXML
	private void onOkClicked() {

		// get percent omissions
		ObservableList<RfortPercentOmission> percentOmissions = omissions_.getItems();

		// no omission added
		if ((percentOmissions == null) || percentOmissions.isEmpty()) {
			popOver_.hide();
			return;
		}

		// create array to store RFORT omissions
		RfortOmission[] omissions = new RfortOmission[percentOmissions.size()];
		for (int i = 0; i < percentOmissions.size(); i++) {
			omissions[i] = percentOmissions.get(i);
		}

		// add them to owner panel
		owner_.addOmissions(omissions);

		// hide popup
		popOver_.hide();
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static RfortAddPercentOmissionsPopup load(RfortPercentOmissionAddingPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("RfortAddPercentOmissionsPopup.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			RfortAddPercentOmissionsPopup controller = (RfortAddPercentOmissionsPopup) fxmlLoader.getController();

			// set attributes
			controller.owner_ = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Interface for RFORT percent omission adding panels.
	 *
	 * @author Murat Artim
	 * @date Apr 20, 2016
	 * @time 9:55:20 AM
	 */
	public interface RfortPercentOmissionAddingPanel {

		/**
		 * Returns the owner of this panel.
		 *
		 * @return The owner of this panel.
		 */
		InputPanel getOwner();

		/**
		 * Adds omissions.
		 *
		 * @param omissions
		 *            Omissions to add.
		 */
		void addOmissions(RfortOmission... omissions);
	}
}
