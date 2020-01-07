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
import equinox.controller.InputPanel.InputPopup;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftModel;
import equinox.data.ui.PilotPointTableItem;
import equinox.task.GetPilotPoints;
import equinox.task.LinkPilotPoints;
import equinox.utility.Utility;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

/**
 * Class for connect pilot points panel controller.
 *
 * @author Murat Artim
 * @date Aug 27, 2015
 * @time 3:13:55 PM
 */
public class LinkPilotPointsPopup implements InputPopup {

	/** The owner panel. */
	private InputPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	/** The calling A/C model. */
	private AircraftModel model_;

	/** Pilot points. */
	private ArrayList<PilotPointTableItem> pilotPoints_;

	/** STF file table. */
	private FilteredTableView<PilotPointTableItem> table_;

	/** Table columns. */
	private FilterableStringTableColumn<PilotPointTableItem, String> nameCol_, programCol_, sectionCol_, missionCol_, eidCol_;

	@FXML
	private VBox root_;

	@FXML
	private Button link_;

	@FXML
	private Label infoLabel_;

	@FXML
	private ImageView infoImage_;

	@SuppressWarnings("rawtypes")
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create content table
		table_ = new FilteredTableView<>();
		table_.getStylesheets().add(Equinox.class.getResource("css/TableView.css").toString());
		table_.setTableMenuButtonVisible(false);
		VBox.setVgrow(table_, Priority.ALWAYS);
		table_.setPrefHeight(FilteredTableView.USE_COMPUTED_SIZE);
		table_.setPrefWidth(FilteredTableView.USE_COMPUTED_SIZE);
		table_.setMaxHeight(Double.MAX_VALUE);
		table_.setMaxWidth(Double.MAX_VALUE);
		root_.getChildren().add(0, table_);

		// setup content table
		nameCol_ = new FilterableStringTableColumn<>("STF File");
		nameCol_.setCellValueFactory(new PropertyValueFactory<PilotPointTableItem, String>("stfname"));
		programCol_ = new FilterableStringTableColumn<>("Program");
		programCol_.setCellValueFactory(new PropertyValueFactory<PilotPointTableItem, String>("program"));
		sectionCol_ = new FilterableStringTableColumn<>("Section");
		sectionCol_.setCellValueFactory(new PropertyValueFactory<PilotPointTableItem, String>("section"));
		missionCol_ = new FilterableStringTableColumn<>("Mission");
		missionCol_.setCellValueFactory(new PropertyValueFactory<PilotPointTableItem, String>("mission"));
		eidCol_ = new FilterableStringTableColumn<>("EID");
		eidCol_.setCellValueFactory(new PropertyValueFactory<PilotPointTableItem, String>("eid"));
		table_.getColumns().add(nameCol_);
		table_.getColumns().add(programCol_);
		table_.getColumns().add(sectionCol_);
		table_.getColumns().add(missionCol_);
		table_.getColumns().add(eidCol_);

		// create table placeholder
		table_.setPlaceholder(NoResultsPanel.load("Your search did not match any STF files.", null));

		// set automatic column sizing
		table_.setColumnResizePolicy(new Callback<TableView.ResizeFeatures, Boolean>() {

			@Override
			public Boolean call(TableView.ResizeFeatures param) {
				return true;
			}
		});

		// listen selections in table
		table_.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<PilotPointTableItem>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends PilotPointTableItem> arg0) {
				showInfo(table_.getSelectionModel().getSelectedItems());
			}
		});

		// Listen for changes to the table's filters
		table_.addEventHandler(ColumnFilterEvent.FILTER_CHANGED_EVENT, new EventHandler<ColumnFilterEvent>() {

			@Override
			public void handle(ColumnFilterEvent t) {
				applyFilters();
			}
		});

		// enable multiple selection
		table_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	/**
	 * Shows this panel.
	 *
	 * @param model
	 *            The calling A/C model.
	 */
	public void show(AircraftModel model) {

		// already shown
		if (isShown_)
			return;

		// set model
		model_ = model;

		// create pop-over
		popOver_ = new PopOver();
		popOver_.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver_.setDetached(true);
		popOver_.setTitle("Link Pilot Points");
		popOver_.setHideOnEscape(true);
		popOver_.setAutoHide(true);
		popOver_.setContentNode(root_);

		// set showing handler
		popOver_.setOnShowing(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				isShown_ = true;
			}
		});

		// set hidden handler
		popOver_.setOnHidden(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				isShown_ = false;
			}
		});

		// request pilot points
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetPilotPoints(this));
	}

	/**
	 * Sets pilot points to this panel.
	 *
	 * @param pilotPoints
	 *            Pilot points.
	 */
	public void setPilotPoints(ArrayList<PilotPointTableItem> pilotPoints) {
		pilotPoints_ = pilotPoints;
		table_.getItems().setAll(pilotPoints);
		popOver_.show(owner_.getOwner().getOwner().getStage());
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	@FXML
	private void onLinkClicked() {

		// get selected pilot points
		ObservableList<PilotPointTableItem> selected = table_.getSelectionModel().getSelectedItems();

		// check inputs
		if (!checkInputs(selected))
			return;

		// link pilot points
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new LinkPilotPoints(model_, selected));

		// hide panel
		popOver_.hide();
	}

	/**
	 * Checks inputs.
	 *
	 * @param selected
	 *            Selected pilot points.
	 * @return True if inputs are acceptable.
	 */
	private boolean checkInputs(ObservableList<PilotPointTableItem> selected) {

		// no model set
		if (model_ == null)
			return false;

		// no selection made
		if ((selected == null) || selected.isEmpty()) {
			showWarning("Please select at least 1 pilot point to link.", 30, link_);
			return false;
		}

		// create list to check mission - EID uniqueness
		ArrayList<String> missionEIDs = new ArrayList<>();

		// loop over pilot points
		for (PilotPointTableItem item : selected) {

			// pilot points with different A/C program
			if ((item.getProgram() == null) || !item.getProgram().equals(model_.getProgram())) {
				showWarning("There are pilot points with different A/C programs. Pilot points must belong to same A/C program with the A/C model.", 50, link_);
				return false;
			}

			// pilot points without EID
			if ((item.getEid() == null) || item.getEid().equals("N/A")) {
				showWarning("There are pilot points without EID. Element ID is required to link the pilot point to A/C model.", 50, link_);
				return false;
			}

			// mission - EID pair already exists
			String missionEID = item.getMission() + "_" + item.getEid();
			if (missionEIDs.contains(missionEID)) {
				showWarning("There are duplicate pilot points. Element IDs must be unique for each fatigue mission.", 50, link_);
				return false;
			}
			missionEIDs.add(missionEID);
		}

		// acceptable
		return true;
	}

	/**
	 * Checks inputs.
	 *
	 * @param selected
	 *            Selected pilot points.
	 */
	private void showInfo(ObservableList<PilotPointTableItem> selected) {

		// no model set
		if (model_ == null)
			return;

		// no selection made
		if ((selected == null) || selected.isEmpty()) {
			infoLabel_.setText("Select STF files from the list and link them to A/C model.");
			infoImage_.setImage(Utility.getImage("info.png"));
			return;
		}

		// create list to check mission - EID uniqueness
		ArrayList<String> missionEIDs = new ArrayList<>();

		// loop over pilot points
		for (PilotPointTableItem item : selected) {

			// pilot points with different A/C program
			if ((item.getProgram() == null) || !item.getProgram().equals(model_.getProgram())) {
				infoLabel_.setText("Selection contains pilot points for different A/C programs.");
				infoImage_.setImage(Utility.getImage("warningSmall.png"));
				return;
			}

			// pilot points without EID
			if ((item.getEid() == null) || item.getEid().equals("N/A")) {
				infoLabel_.setText("Selection contains pilot points without EIDs.");
				infoImage_.setImage(Utility.getImage("warningSmall.png"));
				return;
			}

			// mission - EID pair already exists
			String missionEID = item.getMission() + "_" + item.getEid();
			if (missionEIDs.contains(missionEID)) {
				infoLabel_.setText("Selection contains duplicate pilot points with same EIDs.");
				infoImage_.setImage(Utility.getImage("warningSmall.png"));
				return;
			}
			missionEIDs.add(missionEID);
		}

		// acceptable
		infoLabel_.setText(selected.size() + " files are selected to link to A/C model.");
		infoImage_.setImage(Utility.getImage("info.png"));
	}

	/**
	 * Applies column filters to content table.
	 *
	 */
	private void applyFilters() {

		// create new content
		ArrayList<PilotPointTableItem> newContent = createNewContent();

		// filter columns
		filterNameColumn(newContent, nameCol_.getFilters());
		filterProgramColumn(newContent, programCol_.getFilters());
		filterSectionColumn(newContent, sectionCol_.getFilters());
		filterMissionColumn(newContent, missionCol_.getFilters());
		filterEIDColumn(newContent, eidCol_.getFilters());

		// set filtered items to table
		table_.getItems().setAll(newContent);
	}

	/**
	 * Creates and returns new content.
	 *
	 * @return New content.
	 */
	private ArrayList<PilotPointTableItem> createNewContent() {

		// create content list
		ArrayList<PilotPointTableItem> newContent = new ArrayList<>();

		// copy content
		for (PilotPointTableItem item : pilotPoints_) {
			PilotPointTableItem newItem = new PilotPointTableItem(item.getSTFFileID());
			newItem.setStfname(item.getStfname());
			newItem.setProgram(item.getProgram());
			newItem.setSection(item.getSection());
			newItem.setMission(item.getMission());
			newItem.setEid(item.getEid());
			newContent.add(newItem);
		}

		// return new content
		return newContent;
	}

	/**
	 * Filters event name column data according to selected criteria.
	 *
	 * @param newContent
	 *            New data content.
	 * @param filters
	 *            Column filters.
	 */
	private static void filterNameColumn(ArrayList<PilotPointTableItem> newContent, ObservableList<StringOperator> filters) {

		// create list to remove
		ArrayList<PilotPointTableItem> remove = new ArrayList<>();

		// loop over filters
		for (StringOperator filter : filters) {

			// loop over content items
			for (PilotPointTableItem item : newContent) {

				// null filter
				if (item.getStfname() == null) {
					continue;
				}

				// get name and value
				String name = item.getStfname().toUpperCase();
				String value = filter.getValue().toUpperCase();

				if (filter.getType() == StringOperator.Type.EQUALS) {
					if (!name.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.NOTEQUALS) {
					if (name.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.CONTAINS) {
					if (!name.contains(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.ENDSWITH) {
					if (!name.endsWith(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.STARTSWITH) {
					if (!name.startsWith(value)) {
						remove.add(item);
					}
				}
			}
		}

		// remove filtered items
		newContent.removeAll(remove);
	}

	/**
	 * Filters event name column data according to selected criteria.
	 *
	 * @param newContent
	 *            New data content.
	 * @param filters
	 *            Column filters.
	 */
	private static void filterProgramColumn(ArrayList<PilotPointTableItem> newContent, ObservableList<StringOperator> filters) {

		// create list to remove
		ArrayList<PilotPointTableItem> remove = new ArrayList<>();

		// loop over filters
		for (StringOperator filter : filters) {

			// loop over content items
			for (PilotPointTableItem item : newContent) {

				// null filter
				if (item.getProgram() == null) {
					continue;
				}

				// get program and value
				String program = item.getProgram().toUpperCase();
				String value = filter.getValue().toUpperCase();

				if (filter.getType() == StringOperator.Type.EQUALS) {
					if (!program.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.NOTEQUALS) {
					if (program.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.CONTAINS) {
					if (!program.contains(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.ENDSWITH) {
					if (!program.endsWith(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.STARTSWITH) {
					if (!program.startsWith(value)) {
						remove.add(item);
					}
				}
			}
		}

		// remove filtered items
		newContent.removeAll(remove);
	}

	/**
	 * Filters event name column data according to selected criteria.
	 *
	 * @param newContent
	 *            New data content.
	 * @param filters
	 *            Column filters.
	 */
	private static void filterSectionColumn(ArrayList<PilotPointTableItem> newContent, ObservableList<StringOperator> filters) {

		// create list to remove
		ArrayList<PilotPointTableItem> remove = new ArrayList<>();

		// loop over filters
		for (StringOperator filter : filters) {

			// loop over content items
			for (PilotPointTableItem item : newContent) {

				// null filter
				if (item.getSection() == null) {
					continue;
				}

				// get section and value
				String section = item.getSection().toUpperCase();
				String value = filter.getValue().toUpperCase();

				if (filter.getType() == StringOperator.Type.EQUALS) {
					if (!section.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.NOTEQUALS) {
					if (section.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.CONTAINS) {
					if (!section.contains(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.ENDSWITH) {
					if (!section.endsWith(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.STARTSWITH) {
					if (!section.startsWith(value)) {
						remove.add(item);
					}
				}
			}
		}

		// remove filtered items
		newContent.removeAll(remove);
	}

	/**
	 * Filters event name column data according to selected criteria.
	 *
	 * @param newContent
	 *            New data content.
	 * @param filters
	 *            Column filters.
	 */
	private static void filterMissionColumn(ArrayList<PilotPointTableItem> newContent, ObservableList<StringOperator> filters) {

		// create list to remove
		ArrayList<PilotPointTableItem> remove = new ArrayList<>();

		// loop over filters
		for (StringOperator filter : filters) {

			// loop over content items
			for (PilotPointTableItem item : newContent) {

				// null filter
				if (item.getMission() == null) {
					continue;
				}

				// get mission and value
				String mission = item.getMission().toUpperCase();
				String value = filter.getValue().toUpperCase();

				if (filter.getType() == StringOperator.Type.EQUALS) {
					if (!mission.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.NOTEQUALS) {
					if (mission.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.CONTAINS) {
					if (!mission.contains(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.ENDSWITH) {
					if (!mission.endsWith(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.STARTSWITH) {
					if (!mission.startsWith(value)) {
						remove.add(item);
					}
				}
			}
		}

		// remove filtered items
		newContent.removeAll(remove);
	}

	/**
	 * Filters event name column data according to selected criteria.
	 *
	 * @param newContent
	 *            New data content.
	 * @param filters
	 *            Column filters.
	 */
	private static void filterEIDColumn(ArrayList<PilotPointTableItem> newContent, ObservableList<StringOperator> filters) {

		// create list to remove
		ArrayList<PilotPointTableItem> remove = new ArrayList<>();

		// loop over filters
		for (StringOperator filter : filters) {

			// loop over content items
			for (PilotPointTableItem item : newContent) {

				// null filter
				if (item.getEid() == null) {
					continue;
				}

				// get EID and value
				String eid = item.getEid().toUpperCase();
				String value = filter.getValue().toUpperCase();

				if (filter.getType() == StringOperator.Type.EQUALS) {
					if (!eid.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.NOTEQUALS) {
					if (eid.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.CONTAINS) {
					if (!eid.contains(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.ENDSWITH) {
					if (!eid.endsWith(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.STARTSWITH) {
					if (!eid.startsWith(value)) {
						remove.add(item);
					}
				}
			}
		}

		// remove filtered items
		newContent.removeAll(remove);
	}

	/**
	 * Shows warning message.
	 *
	 * @param warning
	 *            Message.
	 * @param wrapLength
	 *            Wrap length.
	 * @param node
	 *            Node to show the warning on.
	 */
	private static void showWarning(String warning, int wrapLength, Node node) {
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel1.load(warning, wrapLength, NotificationPanel1.WARNING));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(node);
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static LinkPilotPointsPopup load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("LinkPilotPointsPopup.fxml"));
			fxmlLoader.load();

			// get controller
			LinkPilotPointsPopup controller = (LinkPilotPointsPopup) fxmlLoader.getController();

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
}
