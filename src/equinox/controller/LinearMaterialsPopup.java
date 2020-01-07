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
import equinox.data.material.LinearMaterialItem;
import equinox.font.IconicFont;
import equinox.task.GetLinearMaterials;
import equinox.task.GetLinearMaterials.LinearMaterialRequestingPanel;
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
 * Class for linear materials panel controller.
 *
 * @author Murat Artim
 * @date Dec 3, 2015
 * @time 2:15:31 PM
 */
public class LinearMaterialsPopup implements InputPopup, LinearMaterialRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	/** Requesting panel. */
	private LinearMaterialAddingPanel panel_;

	/** Material list. */
	private final ObservableList<LinearMaterialItem> materials_ = FXCollections.observableArrayList();

	@FXML
	private VBox root_;

	@FXML
	private TableView<LinearMaterialItem> table_;

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
		TableColumn<LinearMaterialItem, String> nameCol = new TableColumn<>("Name");
		nameCol.setCellValueFactory(new PropertyValueFactory<LinearMaterialItem, String>("name"));
		TableColumn<LinearMaterialItem, String> specificationCol = new TableColumn<>("Specification");
		specificationCol.setCellValueFactory(new PropertyValueFactory<LinearMaterialItem, String>("specification"));
		TableColumn<LinearMaterialItem, String> familyCol = new TableColumn<>("Family");
		familyCol.setCellValueFactory(new PropertyValueFactory<LinearMaterialItem, String>("family"));
		TableColumn<LinearMaterialItem, String> orientationCol = new TableColumn<>("Orientation");
		orientationCol.setCellValueFactory(new PropertyValueFactory<LinearMaterialItem, String>("orientation"));
		TableColumn<LinearMaterialItem, String> configurationCol = new TableColumn<>("Configuration");
		configurationCol.setCellValueFactory(new PropertyValueFactory<LinearMaterialItem, String>("configuration"));
		TableColumn<LinearMaterialItem, Double> ceffCol = new TableColumn<>("Ceff");
		ceffCol.setCellValueFactory(new PropertyValueFactory<LinearMaterialItem, Double>("ceff"));
		TableColumn<LinearMaterialItem, Double> mCol = new TableColumn<>("m");
		mCol.setCellValueFactory(new PropertyValueFactory<LinearMaterialItem, Double>("m"));
		TableColumn<LinearMaterialItem, Double> aCol = new TableColumn<>("A");
		aCol.setCellValueFactory(new PropertyValueFactory<LinearMaterialItem, Double>("a"));
		TableColumn<LinearMaterialItem, Double> bCol = new TableColumn<>("B");
		bCol.setCellValueFactory(new PropertyValueFactory<LinearMaterialItem, Double>("b"));
		TableColumn<LinearMaterialItem, Double> cCol = new TableColumn<>("C");
		cCol.setCellValueFactory(new PropertyValueFactory<LinearMaterialItem, Double>("c"));
		TableColumn<LinearMaterialItem, Double> ftuCol = new TableColumn<>("Ftu");
		ftuCol.setCellValueFactory(new PropertyValueFactory<LinearMaterialItem, Double>("ftu"));
		TableColumn<LinearMaterialItem, Double> ftyCol = new TableColumn<>("Fty");
		ftyCol.setCellValueFactory(new PropertyValueFactory<LinearMaterialItem, Double>("fty"));
		table_.getColumns().add(nameCol);
		table_.getColumns().add(specificationCol);
		table_.getColumns().add(familyCol);
		table_.getColumns().add(orientationCol);
		table_.getColumns().add(configurationCol);
		table_.getColumns().add(ceffCol);
		table_.getColumns().add(mCol);
		table_.getColumns().add(aCol);
		table_.getColumns().add(bCol);
		table_.getColumns().add(cCol);
		table_.getColumns().add(ftuCol);
		table_.getColumns().add(ftyCol);

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
	public void setLinearMaterials(ArrayList<LinearMaterialItem> linearMaterials) {

		// set table items
		materials_.clear();
		for (LinearMaterialItem item : linearMaterials) {
			materials_.add(item);
		}
		table_.setItems(materials_);

		// set library version
		libraryVersion_.setText(linearMaterials.isEmpty() ? "N/A" : linearMaterials.get(0).getLibraryVersion());

		// reset
		search_.clear();
		table_.getSelectionModel().clearSelection();

		// show popup
		popOver_.show(panel_.getLinearMaterialPopupNode());

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
	public void show(LinearMaterialAddingPanel panel, ArrowLocation arrowLocation, boolean isNonModal) {

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

		// request Linear materials
		IsamiVersion isamiVersion = (IsamiVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_VERSION);
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetLinearMaterials(this, isamiVersion));
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
		panel_.addLinearMaterials(table_.getSelectionModel().getSelectedItems());

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
		ObservableList<LinearMaterialItem> subentries = FXCollections.observableArrayList();
		for (LinearMaterialItem item : table_.getItems()) {
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
	public static LinearMaterialsPopup load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("LinearMaterialsPopup.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			LinearMaterialsPopup controller = (LinearMaterialsPopup) fxmlLoader.getController();

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
	 * Interface for linear material adding panels.
	 *
	 * @author Murat Artim
	 * @date Dec 12, 2015
	 * @time 2:56:37 PM
	 */
	public interface LinearMaterialAddingPanel {

		/**
		 * Adds selected linear materials.
		 *
		 * @param materials
		 *            Linear materials.
		 */
		void addLinearMaterials(ObservableList<LinearMaterialItem> materials);

		/**
		 * Returns linear material popup node.
		 *
		 * @return Linear material popup node.
		 */
		Node getLinearMaterialPopupNode();
	}
}
