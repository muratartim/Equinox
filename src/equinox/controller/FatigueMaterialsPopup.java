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
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.InputPanel.InputPopup;
import equinox.data.EquinoxTheme;
import equinox.data.IsamiVersion;
import equinox.data.Settings;
import equinox.data.material.FatigueMaterialItem;
import equinox.font.IconicFont;
import equinox.task.GetFatigueMaterials;
import equinox.task.GetFatigueMaterials.FatigueMaterialRequestingPanel;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

/**
 * Class for fatigue materials panel controller.
 *
 * @author Murat Artim
 * @date Dec 1, 2015
 * @time 2:45:05 PM
 */
public class FatigueMaterialsPopup implements InputPopup, FatigueMaterialRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Requesting panel. */
	private FatigueMaterialAddingPanel panel_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	/** Material list. */
	private final ObservableList<FatigueMaterialItem> materials_ = FXCollections.observableArrayList();

	@FXML
	private VBox root_;

	@FXML
	private TableView<FatigueMaterialItem> table_;

	@FXML
	private TextField search_;

	@FXML
	private Button cancelSearch_, ok_;

	@FXML
	private Label libraryVersion_;

	@SuppressWarnings("rawtypes")
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create table columns
		TableColumn<FatigueMaterialItem, String> nameCol = new TableColumn<>("Name");
		nameCol.setCellValueFactory(new PropertyValueFactory<FatigueMaterialItem, String>("name"));
		TableColumn<FatigueMaterialItem, String> specificationCol = new TableColumn<>("Specification");
		specificationCol.setCellValueFactory(new PropertyValueFactory<FatigueMaterialItem, String>("specification"));
		TableColumn<FatigueMaterialItem, String> familyCol = new TableColumn<>("Family");
		familyCol.setCellValueFactory(new PropertyValueFactory<FatigueMaterialItem, String>("family"));
		TableColumn<FatigueMaterialItem, String> orientationCol = new TableColumn<>("Orientation");
		orientationCol.setCellValueFactory(new PropertyValueFactory<FatigueMaterialItem, String>("orientation"));
		TableColumn<FatigueMaterialItem, String> configurationCol = new TableColumn<>("Configuration");
		configurationCol.setCellValueFactory(new PropertyValueFactory<FatigueMaterialItem, String>("configuration"));
		TableColumn<FatigueMaterialItem, Double> pCol = new TableColumn<>("p");
		pCol.setCellValueFactory(new PropertyValueFactory<FatigueMaterialItem, Double>("p"));
		TableColumn<FatigueMaterialItem, Double> qCol = new TableColumn<>("q");
		qCol.setCellValueFactory(new PropertyValueFactory<FatigueMaterialItem, Double>("q"));
		TableColumn<FatigueMaterialItem, Double> mCol = new TableColumn<>("M");
		mCol.setCellValueFactory(new PropertyValueFactory<FatigueMaterialItem, Double>("M"));
		table_.getColumns().add(nameCol);
		table_.getColumns().add(specificationCol);
		table_.getColumns().add(familyCol);
		table_.getColumns().add(orientationCol);
		table_.getColumns().add(configurationCol);
		table_.getColumns().add(pCol);
		table_.getColumns().add(qCol);
		table_.getColumns().add(mCol);

		// create table placeholder
		table_.setPlaceholder(NoResultsPanel.load("Your search did not match any material.", null));

		// set automatic column sizing for table
		table_.setColumnResizePolicy(new Callback<TableView.ResizeFeatures, Boolean>() {

			@Override
			public Boolean call(TableView.ResizeFeatures param) {
				return true;
			}
		});

		// enable multiple selection for table
		table_.setItems(materials_);
		table_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table_.setTableMenuButtonVisible(true);

		// setup search field
		search_.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> ov, String old_Val, String new_val) {
				search(old_Val, new_val);
			}
		});
	}

	/**
	 * Returns the root of this panel.
	 *
	 * @return The root of this panel.
	 */
	public VBox getRoot() {
		return root_;
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public void setFatigueMaterials(ArrayList<FatigueMaterialItem> fatigueMaterials) {

		// set table items
		materials_.clear();
		for (FatigueMaterialItem item : fatigueMaterials) {
			materials_.add(item);
		}
		table_.setItems(materials_);

		// set library version
		libraryVersion_.setText(fatigueMaterials.isEmpty() ? "N/A" : fatigueMaterials.get(0).getLibraryVersion());

		// reset
		search_.clear();
		table_.getSelectionModel().clearSelection();

		// show popup
		popOver_.show(panel_.getFatigueMaterialPopupNode());

		// focus on search
		search_.requestFocus();
	}

	/**
	 * Shows this panel.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param arrowLocation
	 *            Arrow location of the popup.
	 * @param isNonModal
	 *            True if popup should NOT be modal.
	 */
	public void show(FatigueMaterialAddingPanel panel, ArrowLocation arrowLocation, boolean isNonModal) {

		// already shown
		if (isShown_)
			return;

		// set panel
		panel_ = panel;

		// create pop-over
		popOver_ = new PopOver();
		popOver_.setArrowLocation(arrowLocation);
		popOver_.setDetached(false);
		popOver_.setHideOnEscape(isNonModal);
		popOver_.setAutoHide(isNonModal);
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

		// request fatigue materials
		IsamiVersion isamiVersion = (IsamiVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_VERSION);
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetFatigueMaterials(this, isamiVersion));
	}

	@FXML
	private void onCancelSearchClicked() {
		search_.clear();
	}

	@FXML
	private void onOkClicked() {

		// no selection
		if (table_.getSelectionModel().isEmpty()) {
			String message = "No material selected. Please select at least 1 material to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(ok_);
			return;
		}

		// add selected materials to requesting panel
		panel_.addFatigueMaterials(table_.getSelectionModel().getSelectedItems());

		// hide
		popOver_.hide();
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	/**
	 * Searches material list for given new value.
	 *
	 * @param old_Val
	 *            Old text value.
	 * @param new_Val
	 *            New text value.
	 */
	private void search(String old_Val, String new_Val) {
		table_.getSelectionModel().clearSelection();
		if ((old_Val != null) && (new_Val.length() < old_Val.length())) {
			table_.setItems(materials_);
		}
		String value = new_Val.toUpperCase();
		ObservableList<FatigueMaterialItem> subentries = FXCollections.observableArrayList();
		for (FatigueMaterialItem item : table_.getItems()) {
			if (item.getSearchString().toUpperCase().contains(value)) {
				subentries.add(item);
			}
		}
		table_.setItems(subentries);
		cancelSearch_.setVisible(!new_Val.isEmpty());
		if (new_Val.isEmpty()) {
			onCancelSearchClicked();
			return;
		}
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static FatigueMaterialsPopup load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("FatigueMaterialsPopup.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			FatigueMaterialsPopup controller = (FatigueMaterialsPopup) fxmlLoader.getController();

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
	 * Interface for fatigue material adding panels.
	 *
	 * @author Murat Artim
	 * @date Dec 12, 2015
	 * @time 2:56:37 PM
	 */
	public interface FatigueMaterialAddingPanel {

		/**
		 * Adds selected fatigue materials.
		 *
		 * @param materials
		 *            Fatigue materials.
		 */
		void addFatigueMaterials(ObservableList<FatigueMaterialItem> materials);

		/**
		 * Returns fatigue material popover node.
		 *
		 * @return Fatigue material popover node.
		 */
		Node getFatigueMaterialPopupNode();
	}
}
