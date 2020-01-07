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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.data.EquinoxTheme;
import equinox.data.material.FatigueMaterialItem;
import equinox.data.material.LinearMaterialItem;
import equinox.data.material.PreffasMaterialItem;
import equinox.data.ui.RfortPilotPoint;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.LinearMaterial;
import equinox.dataServer.remote.data.PreffasMaterial;
import equinox.plugin.FileType;
import equinox.task.GetFatigueMaterials.FatigueMaterialRequestingPanel;
import equinox.task.GetLinearMaterials.LinearMaterialRequestingPanel;
import equinox.task.GetPreffasMaterials.PreffasMaterialRequestingPanel;
import equinox.utility.RfortMaterialTableCell;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.util.Callback;

/**
 * Class for RFORT pilot points popup.
 *
 * @author Murat Artim
 * @date Mar 1, 2016
 * @time 11:01:19 AM
 */
public class RfortPilotPointsPopup implements Initializable, FatigueMaterialRequestingPanel, PreffasMaterialRequestingPanel, LinearMaterialRequestingPanel {

	/** The owner panel. */
	private RfortExtendedPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	/** Drop image. */
	private ImageView dropImage_;

	/** Fatigue material table column. */
	private TableColumn<RfortPilotPoint, FatigueMaterial> fatigueMaterialColumn_;

	/** Preffas material table column. */
	private TableColumn<RfortPilotPoint, PreffasMaterial> preffasMaterialColumn_;

	/** Linear material table column. */
	private TableColumn<RfortPilotPoint, LinearMaterial> linearMaterialColumn_;

	@FXML
	private VBox root_;

	@FXML
	private TableView<RfortPilotPoint> table_;

	@FXML
	private Button close_, delete_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// initialize mission parameters table
		table_.setEditable(true);
		TableColumn<RfortPilotPoint, String> nameCol = new TableColumn<>("File");
		nameCol.setCellValueFactory(new PropertyValueFactory<RfortPilotPoint, String>("name"));
		TableColumn<RfortPilotPoint, Boolean> rfortCol = new TableColumn<>("Include in\nRFORT");
		rfortCol.setCellValueFactory(new PropertyValueFactory<RfortPilotPoint, Boolean>("includeinrfort"));
		rfortCol.setCellFactory(CheckBoxTableCell.forTableColumn(rfortCol));
		TableColumn<RfortPilotPoint, String> factorCol = new TableColumn<>("Stress\nFactor");
		factorCol.setCellValueFactory(new PropertyValueFactory<RfortPilotPoint, String>("factor"));
		factorCol.setCellFactory(TextFieldTableCell.<RfortPilotPoint>forTableColumn());
		factorCol.setOnEditCommit((CellEditEvent<RfortPilotPoint, String> t) -> {
			t.getTableView().getItems().get(t.getTablePosition().getRow()).setFactor(t.getNewValue());
		});
		fatigueMaterialColumn_ = new TableColumn<>("Fatigue Material");
		fatigueMaterialColumn_.setCellValueFactory(new PropertyValueFactory<RfortPilotPoint, FatigueMaterial>("fatiguematerial"));
		preffasMaterialColumn_ = new TableColumn<>("Preffas Material");
		preffasMaterialColumn_.setCellValueFactory(new PropertyValueFactory<RfortPilotPoint, PreffasMaterial>("preffasmaterial"));
		linearMaterialColumn_ = new TableColumn<>("Linear Prop. Material");
		linearMaterialColumn_.setCellValueFactory(new PropertyValueFactory<RfortPilotPoint, LinearMaterial>("linearmaterial"));
		table_.getColumns().add(nameCol);
		table_.getColumns().add(rfortCol);
		table_.getColumns().add(factorCol);
		table_.getColumns().add(fatigueMaterialColumn_);
		table_.getColumns().add(preffasMaterialColumn_);
		table_.getColumns().add(linearMaterialColumn_);
		nameCol.setPrefWidth(250.0);
		rfortCol.setPrefWidth(70.0);
		factorCol.setPrefWidth(80.0);
		fatigueMaterialColumn_.setPrefWidth(220.0);
		preffasMaterialColumn_.setPrefWidth(220.0);
		linearMaterialColumn_.setPrefWidth(250.0);
		table_.setPlaceholder(createTablePlaceHolder());

		// bind components
		delete_.setDisable(true);
		table_.getSelectionModel().getSelectedItems().addListener((ListChangeListener<RfortPilotPoint>) c -> delete_.setDisable(table_.getSelectionModel().getSelectedItems().isEmpty()));

		// setup table
		table_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}

	/**
	 * Returns the owner panel.
	 *
	 * @return The owner panel.
	 */
	public RfortExtendedPanel getOwner() {
		return owner_;
	}

	/**
	 * Shows this panel.
	 *
	 * @param fatigue
	 *            True if fatigue materials can be selected.
	 * @param preffas
	 *            True if preffas materials can be selected.
	 * @param linear
	 *            True if linear materials can be selected.
	 */
	public void show(boolean fatigue, boolean preffas, boolean linear) {

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
		popOver_.setOnShowing(event -> {
			owner_.getOwner().getRoot().setMouseTransparent(true);
			isShown_ = true;
		});

		// set hidden handler
		popOver_.setOnHidden(event -> {

			// get pilot points
			ObservableList<RfortPilotPoint> pps = table_.getItems();

			// check inputs
			if (!checkInputs(pps, false)) {
				table_.getItems().clear();
			}

			// enable owner panel
			owner_.getOwner().getRoot().setMouseTransparent(false);
			isShown_ = false;
		});

		// setup material column visibilities
		fatigueMaterialColumn_.setVisible(fatigue);
		preffasMaterialColumn_.setVisible(preffas);
		linearMaterialColumn_.setVisible(linear);

		// show popup
		popOver_.show(owner_.getPilotPointPopupNode());
	}

	@Override
	public void setFatigueMaterials(ArrayList<FatigueMaterialItem> fatigueMaterials) {

		// get materials
		ObservableList<FatigueMaterial> materials = FXCollections.observableArrayList();
		for (FatigueMaterialItem item : fatigueMaterials) {
			materials.add(item.getMaterial());
		}

		// set cell factory
		fatigueMaterialColumn_.setCellFactory(param -> new RfortMaterialTableCell<>(materials, "Select fatigue material..."));
	}

	@Override
	public void setPreffasMaterials(ArrayList<PreffasMaterialItem> preffasMaterials) {

		// get materials
		ObservableList<PreffasMaterial> materials = FXCollections.observableArrayList();
		for (PreffasMaterialItem item : preffasMaterials) {
			materials.add(item.getMaterial());
		}

		// set cell factory
		preffasMaterialColumn_.setCellFactory(new Callback<TableColumn<RfortPilotPoint, PreffasMaterial>, TableCell<RfortPilotPoint, PreffasMaterial>>() {

			@Override
			public TableCell<RfortPilotPoint, PreffasMaterial> call(TableColumn<RfortPilotPoint, PreffasMaterial> param) {
				return new RfortMaterialTableCell<>(materials, "Select preffas material...");
			}
		});
	}

	@Override
	public void setLinearMaterials(ArrayList<LinearMaterialItem> linearMaterials) {

		// get materials
		ObservableList<LinearMaterial> materials = FXCollections.observableArrayList();
		for (LinearMaterialItem item : linearMaterials) {
			materials.add(item.getMaterial());
		}

		// set cell factory
		linearMaterialColumn_.setCellFactory(new Callback<TableColumn<RfortPilotPoint, LinearMaterial>, TableCell<RfortPilotPoint, LinearMaterial>>() {

			@Override
			public TableCell<RfortPilotPoint, LinearMaterial> call(TableColumn<RfortPilotPoint, LinearMaterial> param) {
				return new RfortMaterialTableCell<>(materials, "Select linear prop. material...");
			}
		});
	}

	@FXML
	private void onDeleteClicked() {
		table_.getItems().removeAll(table_.getSelectionModel().getSelectedItems());
	}

	@FXML
	private void onCloseClicked() {

		// get pilot points
		ObservableList<RfortPilotPoint> pps = table_.getItems();

		// no items
		if (pps == null || pps.isEmpty()) {
			popOver_.hide();
			return;
		}

		// check inputs
		if (!checkInputs(pps, true))
			return;

		// hide
		popOver_.hide();
	}

	/**
	 * Checks inputs.
	 *
	 * @param pps
	 *            Pilot points.
	 * @param showWarning
	 *            True to show warning.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(ObservableList<RfortPilotPoint> pps, boolean showWarning) {

		// less than 2 pilot points
		if (pps.size() < 2) {
			if (showWarning) {
				String message = "Please supply at least 2 pilot points to proceed.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(close_);
			}
			return false;
		}

		// get required material data types
		boolean fatigue = fatigueMaterialColumn_.isVisible();
		boolean preffas = preffasMaterialColumn_.isVisible();
		boolean linear = linearMaterialColumn_.isVisible();

		// loop over pilot points
		int numIncludeInRfort = 0;
		for (RfortPilotPoint pp : pps) {

			// STF file doesn't exist
			if (!pp.getFile().exists()) {
				if (showWarning) {
					String message = "Pilot point '" + pp.getName() + "' doesn't exist. Please select valid pilot points.";
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(close_);
				}
				return false;
			}

			// check if pilot point should be included in RFORT analysis
			if (pp.isIncludeInRfort()) {
				numIncludeInRfort++;
			}

			// check factor
			try {
				Double.parseDouble(pp.getFactor());
			}

			// invalid factor value
			catch (Exception e) {
				if (showWarning) {
					String message = "Pilot point '" + pp.getName() + "' has invalid factor value. Please supply a valid value.";
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(close_);
				}
				return false;
			}

			// no fatigue material
			if (fatigue && pp.getFatigueMaterial() == null) {
				if (showWarning) {
					String message = "Pilot point '" + pp.getName() + "' has no fatigue material. Please select a fatigue material.";
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(close_);
				}
				return false;
			}

			// no preffas material
			if (preffas && pp.getPreffasMaterial() == null) {
				if (showWarning) {
					String message = "Pilot point '" + pp.getName() + "' has no preffas material. Please select a preffas material.";
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(close_);
				}
				return false;
			}

			// no linear material
			if (linear && pp.getLinearMaterial() == null) {
				if (showWarning) {
					String message = "Pilot point '" + pp.getName() + "' has no linear propagation material. Please select a linear propagation material.";
					PopOver popOver = new PopOver();
					popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
					popOver.setDetachable(false);
					popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
					popOver.setHideOnEscape(true);
					popOver.setAutoHide(true);
					popOver.show(close_);
				}
				return false;
			}
		}

		// no pilot point is included in RFORT analysis
		if (numIncludeInRfort == 0) {
			if (showWarning) {
				String message = "Please select at least 1 pilot point to be included in RFORT analysis.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(close_);
			}
			return false;
		}

		// inputs are acceptable
		return true;
	}

	@FXML
	private void onBrowseClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getOwner().getFileChooser(FileType.STF.getExtensionFilter());

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getOwner().getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
			return;

		// set initial directory
		processFiles(files);
	}

	@FXML
	private void onDragOver(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if (event.getGestureSource() != table_ && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.STF)) {
					event.acceptTransferModes(TransferMode.ANY);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragEntered(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if (event.getGestureSource() != table_ && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.STF)) {
					dropImage_.setImage(AddSpectrumPanel.DROP_ZONE_ACTIVE);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragExited(DragEvent event) {
		dropImage_.setImage(AddSpectrumPanel.DROP_ZONE);
		event.consume();
	}

	@FXML
	private void onDragDropped(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		boolean success = false;
		if (event.getGestureSource() != table_ && db.hasFiles()) {

			// process files
			success = processFiles(db.getFiles());
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}

	/**
	 * Processes selected files.
	 *
	 * @param files
	 *            Selected files.
	 * @return True if process completed successfully.
	 */
	private boolean processFiles(List<File> files) {

		// clear list
		table_.getItems().clear();

		// check file types
		boolean success = false;
		for (File file : files) {

			// get file type
			FileType fileType = FileType.getFileType(file);

			// not recognized
			if (fileType == null) {
				continue;
			}

			// STF
			if (fileType.equals(FileType.STF)) {
				owner_.getOwner().getOwner().setInitialDirectory(file);
				RfortPilotPoint item = new RfortPilotPoint(file);
				item.setFactor("1.0");
				item.setIncludeInRfort(true);
				if (!table_.getItems().contains(item)) {
					table_.getItems().add(item);
				}
				success = true;
			}
		}

		// return
		return success;
	}

	/**
	 * Creates and returns multiplication table place holder.
	 *
	 * @return Multiplication table place holder.
	 */
	private Node createTablePlaceHolder() {

		// create vertical box
		VBox vBox = new VBox();
		vBox.setAlignment(Pos.CENTER);
		vBox.setSpacing(10.0);
		vBox.setMaxWidth(Double.MAX_VALUE);

		// create drop image
		dropImage_ = new ImageView(AddSpectrumPanel.DROP_ZONE);
		dropImage_.setPreserveRatio(true);
		dropImage_.setFitWidth(64.0);
		dropImage_.setFitHeight(64.0);
		vBox.getChildren().add(dropImage_);

		// create label
		Label label = new Label("Drop STF files here");
		label.setFont(Font.font(16.0));
		label.setTextFill(Color.GREY);
		label.setTextAlignment(TextAlignment.CENTER);
		label.setMouseTransparent(true);
		vBox.getChildren().add(label);

		// return place holder
		return vBox;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @param pilotPointList
	 *            Pilot points list.
	 * @return The newly loaded file CDF set panel.
	 */
	public static RfortPilotPointsPopup load(RfortExtendedPanel owner, ObservableList<RfortPilotPoint> pilotPointList) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("RfortPilotPointsPopup.fxml"));
			fxmlLoader.load();

			// get controller
			RfortPilotPointsPopup controller = (RfortPilotPointsPopup) fxmlLoader.getController();

			// set attributes
			controller.owner_ = owner;
			controller.table_.setItems(pilotPointList);

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
