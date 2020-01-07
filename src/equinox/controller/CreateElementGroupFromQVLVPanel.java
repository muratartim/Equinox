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
import org.google.jhsheets.filtered.FilteredTableView;
import org.google.jhsheets.filtered.operators.StringOperator;
import org.google.jhsheets.filtered.tablecolumn.ColumnFilterEvent;
import org.google.jhsheets.filtered.tablecolumn.FilterableStringTableColumn;

import equinox.Equinox;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftModel;
import equinox.data.ui.QVLVPosition;
import equinox.task.CreateElementGroupFromQVLVPositions;
import equinox.task.GetQVLVPositions;
import equinox.task.GetQVLVPositions.QVLVPositionRequestingPanel;
import equinox.utility.Utility;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Class for create element group from frame/stringer positions panel controller.
 *
 * @author Murat Artim
 * @date Aug 4, 2015
 * @time 11:34:26 AM
 */
public class CreateElementGroupFromQVLVPanel implements InternalInputSubPanel, QVLVPositionRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Position table. */
	private FilteredTableView<QVLVPosition> positionTable_;

	/** Position columns. */
	private FilterableStringTableColumn<QVLVPosition, String> frameCol_, stringerCol_;

	/** Positions. */
	private ArrayList<QVLVPosition> positions_ = new ArrayList<>();

	@FXML
	private VBox root_, container_;

	@FXML
	private TextField name_;

	@SuppressWarnings("rawtypes")
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create content table
		positionTable_ = new FilteredTableView<>();
		positionTable_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		positionTable_.setTableMenuButtonVisible(false);
		positionTable_.setPrefHeight(FilteredTableView.USE_COMPUTED_SIZE);
		positionTable_.setPrefWidth(FilteredTableView.USE_COMPUTED_SIZE);
		positionTable_.setMaxHeight(Double.MAX_VALUE);
		positionTable_.setMaxWidth(Double.MAX_VALUE);
		positionTable_.getStylesheets().add(Equinox.class.getResource("css/HiddenScrollTable.css").toString());
		VBox.setVgrow(positionTable_, Priority.ALWAYS);
		container_.getChildren().add(positionTable_);

		// setup content table
		frameCol_ = new FilterableStringTableColumn<>("Frame");
		frameCol_.setCellValueFactory(new PropertyValueFactory<QVLVPosition, String>("framepos"));
		stringerCol_ = new FilterableStringTableColumn<>("Stringer");
		stringerCol_.setCellValueFactory(new PropertyValueFactory<QVLVPosition, String>("stringerpos"));
		positionTable_.getColumns().add(frameCol_);
		positionTable_.getColumns().add(stringerCol_);
		Label placeholder = new Label("Loading frame/stringer positions...");
		placeholder.setStyle("-fx-text-fill:slategray; -fx-font-size:13px; -fx-text-alignment:center");
		positionTable_.setPlaceholder(placeholder);
		frameCol_.prefWidthProperty().bind(positionTable_.widthProperty().divide(2));
		stringerCol_.prefWidthProperty().bind(positionTable_.widthProperty().divide(2));

		// listen for changes to the table's filters
		positionTable_.addEventHandler(ColumnFilterEvent.FILTER_CHANGED_EVENT, new EventHandler<ColumnFilterEvent>() {

			@Override
			public void handle(ColumnFilterEvent t) {
				applyFilters();
			}
		});
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public void showing() {

		// get selected A/C model
		AircraftModel selected = (AircraftModel) owner_.getSelectedFiles().get(0);

		// get element groups and positions
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetQVLVPositions(this, selected));
	}

	@Override
	public String getHeader() {
		return "Create Element Group";
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void setQVLVPositions(ArrayList<QVLVPosition> positions) {
		positions_ = positions;
		positionTable_.getItems().setAll(positions_);
		name_.clear();
	}

	@FXML
	private void onOkClicked() {

		// get inputs
		String name = name_.getText();
		ObservableList<QVLVPosition> positions = positionTable_.getSelectionModel().getSelectedItems();

		// no name given
		if ((name == null) || name.trim().isEmpty()) {
			String message = "Please enter a group name to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(name_);
			return;
		}

		// no position selected
		if ((positions == null) || positions.isEmpty()) {
			String message = "Please select at least 1 position to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(positionTable_);
			return;
		}

		// get selected model
		AircraftModel model = (AircraftModel) owner_.getSelectedFiles().get(0);

		// create group
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new CreateElementGroupFromQVLVPositions(model, name, positions));

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {
		name_.clear();
		positionTable_.getSelectionModel().clearSelection();
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to create element group from frame/stringer positions", null);
	}

	/**
	 * Applies column filters to content table.
	 *
	 */
	private void applyFilters() {

		// create new content
		ArrayList<QVLVPosition> newContent = createNewContent();

		// filter columns
		filterFrameColumn(newContent, frameCol_.getFilters());
		filterStringerColumn(newContent, stringerCol_.getFilters());

		// set filtered items to table
		positionTable_.getItems().setAll(newContent);
	}

	/**
	 * Creates and returns new content.
	 *
	 * @return New content.
	 */
	private ArrayList<QVLVPosition> createNewContent() {

		// create content list
		ArrayList<QVLVPosition> newContent = new ArrayList<>();

		// loop over positions
		for (QVLVPosition position : positions_) {
			newContent.add(new QVLVPosition(position.getFramepos(), position.getStringerpos()));
		}

		// return new content
		return newContent;
	}

	/**
	 * Filters loadcase type column data according to selected criteria.
	 *
	 * @param newContent
	 *            New data content.
	 * @param filters
	 *            Column filters.
	 */
	private static void filterFrameColumn(ArrayList<QVLVPosition> newContent, ObservableList<StringOperator> filters) {

		// create list to remove
		ArrayList<QVLVPosition> remove = new ArrayList<>();

		// loop over filters
		for (StringOperator filter : filters) {

			// loop over content items
			for (QVLVPosition item : newContent) {

				// get event name and value
				String att2 = item.getFramepos().toUpperCase();
				String value = filter.getValue().toUpperCase();

				// filter
				if (filter.getType() == StringOperator.Type.EQUALS) {
					if (!att2.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.NOTEQUALS) {
					if (att2.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.CONTAINS) {
					if (!att2.contains(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.ENDSWITH) {
					if (!att2.endsWith(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.STARTSWITH) {
					if (!att2.startsWith(value)) {
						remove.add(item);
					}
				}
			}
		}

		// remove filtered items
		newContent.removeAll(remove);
	}

	/**
	 * Filters loadcase type column data according to selected criteria.
	 *
	 * @param newContent
	 *            New data content.
	 * @param filters
	 *            Column filters.
	 */
	private static void filterStringerColumn(ArrayList<QVLVPosition> newContent, ObservableList<StringOperator> filters) {

		// create list to remove
		ArrayList<QVLVPosition> remove = new ArrayList<>();

		// loop over filters
		for (StringOperator filter : filters) {

			// loop over content items
			for (QVLVPosition item : newContent) {

				// get event name and value
				String att2 = item.getStringerpos().toUpperCase();
				String value = filter.getValue().toUpperCase();

				// filter
				if (filter.getType() == StringOperator.Type.EQUALS) {
					if (!att2.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.NOTEQUALS) {
					if (att2.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.CONTAINS) {
					if (!att2.contains(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.ENDSWITH) {
					if (!att2.endsWith(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.STARTSWITH) {
					if (!att2.startsWith(value)) {
						remove.add(item);
					}
				}
			}
		}

		// remove filtered items
		newContent.removeAll(remove);
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static CreateElementGroupFromQVLVPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CreateElementGroupFromQVLVPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CreateElementGroupFromQVLVPanel controller = (CreateElementGroupFromQVLVPanel) fxmlLoader.getController();

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
