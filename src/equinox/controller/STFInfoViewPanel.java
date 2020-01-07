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
import java.util.HashMap;
import java.util.ResourceBundle;

import org.google.jhsheets.filtered.FilteredTableView;
import org.google.jhsheets.filtered.operators.NumberOperator;
import org.google.jhsheets.filtered.operators.StringOperator;
import org.google.jhsheets.filtered.tablecolumn.ColumnFilterEvent;
import org.google.jhsheets.filtered.tablecolumn.FilterableDoubleTableColumn;
import org.google.jhsheets.filtered.tablecolumn.FilterableIntegerTableColumn;
import org.google.jhsheets.filtered.tablecolumn.FilterableStringTableColumn;

import equinox.data.EquinoxTheme;
import equinox.data.fileType.STFFile;
import equinox.data.ui.STFTableItem;
import equinox.data.ui.TableItem;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.font.IconicFont;
import equinox.utility.Utility;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Class for STF info view panel controller.
 *
 * @author Murat Artim
 * @date Dec 18, 2014
 * @time 11:35:36 AM
 */
public class STFInfoViewPanel implements Initializable {

	/** Maximum image file size. */
	public static final long MAX_IMAGE_SIZE = 2000000L;

	/** The owner panel. */
	private InfoViewPanel owner_;

	/** Image panels. */
	private STFInfoImagePanel[] imagePanels_;

	/** STF file. */
	private STFFile stfFile_;

	/** STF file content. */
	private ArrayList<STFTableItem> content_;

	/** Content table. */
	private FilteredTableView<STFTableItem> contentTable_;

	/** Load case column. */
	private FilterableIntegerTableColumn<STFTableItem, Integer> loadcaseCol_;

	/** Stress columns. */
	private FilterableDoubleTableColumn<STFTableItem, Double> sxCol_, syCol_, sxyCol_;

	/** Event name and comment columns. */
	private FilterableStringTableColumn<STFTableItem, String> typeCol_, eventnameCol_, eventcommentCol_;

	/** Pilot point image timeline. */
	private Timeline imageTimeline_;

	@FXML
	private VBox root_, container_;

	@FXML
	private TreeTableView<TableItem> infoTable_;

	@FXML
	private Pagination pagination_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup pagination page factory
		pagination_.setPageFactory(pageIndex -> imagePanels_[pageIndex].getRoot());

		// initialize file info table
		TreeTableColumn<TableItem, String> fileInfoLabelCol = new TreeTableColumn<>("Label");
		fileInfoLabelCol.setCellValueFactory((TreeTableColumn.CellDataFeatures<TableItem, String> param) -> new ReadOnlyStringWrapper(param.getValue().getValue().getLabel()));
		TreeTableColumn<TableItem, String> fileInfoValueCol = new TreeTableColumn<>("Value");
		fileInfoValueCol.setCellValueFactory((TreeTableColumn.CellDataFeatures<TableItem, String> param) -> new ReadOnlyStringWrapper(param.getValue().getValue().getValue()));
		infoTable_.getColumns().add(fileInfoLabelCol);
		infoTable_.getColumns().add(fileInfoValueCol);
		fileInfoLabelCol.prefWidthProperty().bind(infoTable_.widthProperty().divide(5).multiply(2));
		fileInfoValueCol.prefWidthProperty().bind(infoTable_.widthProperty().divide(5).multiply(3));
		infoTable_.setRoot(new TreeItem<>(new TableItem("Label", "Value")));

		// create content table
		contentTable_ = new FilteredTableView<>();
		contentTable_.setTableMenuButtonVisible(true);
		VBox.setVgrow(contentTable_, Priority.ALWAYS);
		contentTable_.setPrefHeight(FilteredTableView.USE_COMPUTED_SIZE);
		contentTable_.setPrefWidth(FilteredTableView.USE_COMPUTED_SIZE);
		contentTable_.setMaxHeight(Double.MAX_VALUE);
		contentTable_.setMaxWidth(Double.MAX_VALUE);
		container_.getChildren().add(contentTable_);

		// setup content table
		loadcaseCol_ = new FilterableIntegerTableColumn<>("Load Case");
		loadcaseCol_.setCellValueFactory(new PropertyValueFactory<STFTableItem, Integer>("loadcase"));
		sxCol_ = new FilterableDoubleTableColumn<>("Normal Stress X");
		sxCol_.setCellValueFactory(new PropertyValueFactory<STFTableItem, Double>("sx"));
		syCol_ = new FilterableDoubleTableColumn<>("Normal Stress Y");
		syCol_.setCellValueFactory(new PropertyValueFactory<STFTableItem, Double>("sy"));
		sxyCol_ = new FilterableDoubleTableColumn<>("Shear Stress XY");
		sxyCol_.setCellValueFactory(new PropertyValueFactory<STFTableItem, Double>("sxy"));
		typeCol_ = new FilterableStringTableColumn<>("Loadcase Type");
		typeCol_.setCellValueFactory(new PropertyValueFactory<STFTableItem, String>("type"));
		eventnameCol_ = new FilterableStringTableColumn<>("Event Name");
		eventnameCol_.setCellValueFactory(new PropertyValueFactory<STFTableItem, String>("eventname"));
		eventcommentCol_ = new FilterableStringTableColumn<>("Event Comment");
		eventcommentCol_.setCellValueFactory(new PropertyValueFactory<STFTableItem, String>("eventcomment"));
		contentTable_.getColumns().add(loadcaseCol_);
		contentTable_.getColumns().add(sxCol_);
		contentTable_.getColumns().add(syCol_);
		contentTable_.getColumns().add(sxyCol_);
		contentTable_.getColumns().add(typeCol_);
		contentTable_.getColumns().add(eventnameCol_);
		contentTable_.getColumns().add(eventcommentCol_);
		Label placeholder = new Label("No match found for given filters.");
		placeholder.setStyle("-fx-text-fill:slategray; -fx-font-size:16px");
		contentTable_.setPlaceholder(placeholder);
		loadcaseCol_.setPrefWidth(150.0);
		sxCol_.setPrefWidth(150.0);
		syCol_.setPrefWidth(150.0);
		sxyCol_.setPrefWidth(150.0);
		typeCol_.setPrefWidth(150.0);
		eventnameCol_.setPrefWidth(150.0);
		eventcommentCol_.setPrefWidth(150.0);

		// Listen for changes to the table's filters
		contentTable_.addEventHandler(ColumnFilterEvent.FILTER_CHANGED_EVENT, t -> applyFilters());

		// create timeline animation for pilot point images
		imageTimeline_ = new Timeline();
		imageTimeline_.setCycleCount(Timeline.INDEFINITE);
		int pageCount = pagination_.getPageCount();
		KeyValue kv = new KeyValue(pagination_.currentPageIndexProperty(), pageCount - 1);
		KeyFrame kf = new KeyFrame(Duration.seconds(8.0 * pageCount), kv);
		imageTimeline_.getKeyFrames().add(kf);
	}

	/**
	 * Returns the owner of this panel.
	 *
	 * @return The owner of this panel.
	 */
	public InfoViewPanel getOwner() {
		return owner_;
	}

	/**
	 * Returns the root container.
	 *
	 * @return The root container.
	 */
	public VBox getRoot() {
		return root_;
	}

	/**
	 * Returns the info table of the panel.
	 *
	 * @return The info table of the panel.
	 */
	public TreeTableView<TableItem> getInfoTable() {
		return infoTable_;
	}

	/**
	 * Returns STF file of this panel.
	 *
	 * @return STF file of this panel.
	 */
	public STFFile getSTFFile() {
		return stfFile_;
	}

	/**
	 * Sets pilot point image.
	 *
	 * @param imageType
	 *            Type of image.
	 */
	public void setImage(PilotPointImageType imageType) {
		for (STFInfoImagePanel panel : imagePanels_) {
			if (panel.getImageType().equals(imageType)) {
				panel.onSetImageClicked();
				break;
			}
		}
	}

	/**
	 * Stops animation of this panel.
	 */
	public void stopAnimation() {
		imageTimeline_.stop();
	}

	/**
	 * Sets STF file info to this panel.
	 *
	 * @param stfFile
	 *            STF file.
	 * @param images
	 *            STF images.
	 * @param info
	 *            STF info.
	 */
	public void setInfo(STFFile stfFile, HashMap<PilotPointImageType, Image> images, ArrayList<TreeItem<TableItem>> info) {

		// set STF file
		stfFile_ = stfFile;

		// set images
		for (STFInfoImagePanel panel : imagePanels_) {
			panel.setImage(images.get(panel.getImageType()));
		}

		// set info
		infoTable_.getRoot().getChildren().setAll(info);

		// play image animation
		imageTimeline_.stop();
		pagination_.setCurrentPageIndex(0);
		imageTimeline_.play();
	}

	/**
	 * Sets content.
	 *
	 * @param content
	 *            Content to set.
	 * @param is2D
	 *            True if 2D STF file content is set.
	 */
	public void setContent(ArrayList<STFTableItem> content, boolean is2D) {
		content_ = content;
		contentTable_.getItems().setAll(content);
		syCol_.setVisible(is2D);
		sxyCol_.setVisible(is2D);
	}

	@FXML
	private void onImageClicked() {
		pagination_.setCurrentPageIndex(0);
		imageTimeline_.stop();
	}

	@FXML
	private void onMissionProfileClicked() {
		pagination_.setCurrentPageIndex(1);
		imageTimeline_.stop();
	}

	@FXML
	private void onLogestFlightClicked() {
		pagination_.setCurrentPageIndex(2);
		imageTimeline_.stop();
	}

	@FXML
	private void onHighestOccurrenceFlightClicked() {
		pagination_.setCurrentPageIndex(3);
		imageTimeline_.stop();
	}

	@FXML
	private void onHighestStressFlightClicked() {
		pagination_.setCurrentPageIndex(4);
		imageTimeline_.stop();
	}

	@FXML
	private void onLevelCrossingsClicked() {
		pagination_.setCurrentPageIndex(5);
		imageTimeline_.stop();
	}

	@FXML
	private void onDamageAngleClicked() {
		pagination_.setCurrentPageIndex(6);
		imageTimeline_.stop();
	}

	@FXML
	private void onNumberOfPeaksClicked() {
		pagination_.setCurrentPageIndex(7);
		imageTimeline_.stop();
	}

	@FXML
	private void onFlightOccurrencesClicked() {
		pagination_.setCurrentPageIndex(8);
		imageTimeline_.stop();
	}

	@FXML
	private void onRainflowHistogramClicked() {
		pagination_.setCurrentPageIndex(9);
		imageTimeline_.stop();
	}

	@FXML
	private void onLoadcaseDamageContributionClicked() {
		pagination_.setCurrentPageIndex(10);
		imageTimeline_.stop();
	}

	@FXML
	private void onFlightDamageContributionClicked() {
		pagination_.setCurrentPageIndex(11);
		imageTimeline_.stop();
	}

	/**
	 * Applies column filters to content table.
	 *
	 */
	private void applyFilters() {

		// create new content
		ArrayList<STFTableItem> newContent = createNewContent();

		// filter columns
		filterLoadcaseColumn(newContent, loadcaseCol_.getFilters());
		filterStressColumn(newContent, sxCol_.getFilters(), STFTableItem.SX);
		filterStressColumn(newContent, syCol_.getFilters(), STFTableItem.SY);
		filterStressColumn(newContent, sxyCol_.getFilters(), STFTableItem.SXY);
		filterTypeColumn(newContent, typeCol_.getFilters());
		filterEventnameColumn(newContent, eventnameCol_.getFilters());
		filterEventcommentColumn(newContent, eventcommentCol_.getFilters());

		// set filtered items to table
		contentTable_.getItems().setAll(newContent);
	}

	/**
	 * Creates and returns new content.
	 *
	 * @return New content.
	 */
	private ArrayList<STFTableItem> createNewContent() {

		// create content list
		ArrayList<STFTableItem> newContent = new ArrayList<>();

		// copy content
		for (STFTableItem item : content_) {
			STFTableItem newItem = new STFTableItem();
			newItem.setLoadcase(item.getLoadcase());
			newItem.setSx(item.getSx());
			newItem.setSy(item.getSy());
			newItem.setSxy(item.getSxy());
			newItem.setType(item.getType());
			newItem.setEventname(item.getEventname());
			newItem.setEventcomment(item.getEventcomment());
			newContent.add(newItem);
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
	private static void filterTypeColumn(ArrayList<STFTableItem> newContent, ObservableList<StringOperator> filters) {

		// create list to remove
		ArrayList<STFTableItem> remove = new ArrayList<>();

		// loop over filters
		for (StringOperator filter : filters) {

			// loop over content items
			for (STFTableItem item : newContent) {

				// get event name and value
				String lcType = item.getType().toUpperCase();
				String value = filter.getValue().toUpperCase();

				// filter
				if (filter.getType() == StringOperator.Type.EQUALS) {
					if (!lcType.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.NOTEQUALS) {
					if (lcType.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.CONTAINS) {
					if (!lcType.contains(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.ENDSWITH) {
					if (!lcType.endsWith(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.STARTSWITH) {
					if (!lcType.startsWith(value)) {
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
	private static void filterEventnameColumn(ArrayList<STFTableItem> newContent, ObservableList<StringOperator> filters) {

		// create list to remove
		ArrayList<STFTableItem> remove = new ArrayList<>();

		// loop over filters
		for (StringOperator filter : filters) {

			// loop over content items
			for (STFTableItem item : newContent) {

				// null filter
				if (item.getEventname() == null) {
					continue;
				}

				// get event name and value
				String eventName = item.getEventname().toUpperCase();
				String value = filter.getValue().toUpperCase();

				if (filter.getType() == StringOperator.Type.EQUALS) {
					if (!eventName.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.NOTEQUALS) {
					if (eventName.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.CONTAINS) {
					if (!eventName.contains(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.ENDSWITH) {
					if (!eventName.endsWith(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.STARTSWITH) {
					if (!eventName.startsWith(value)) {
						remove.add(item);
					}
				}
			}
		}

		// remove filtered items
		newContent.removeAll(remove);
	}

	/**
	 * Filters event comment column data according to selected criteria.
	 *
	 * @param newContent
	 *            New data content.
	 * @param filters
	 *            Column filters.
	 */
	private static void filterEventcommentColumn(ArrayList<STFTableItem> newContent, ObservableList<StringOperator> filters) {

		// create list to remove
		ArrayList<STFTableItem> remove = new ArrayList<>();

		// loop over filters
		for (StringOperator filter : filters) {

			// loop over content items
			for (STFTableItem item : newContent) {

				// null filter
				if (item.getEventcomment() == null) {
					continue;
				}

				// get event comment and value
				String eventComment = item.getEventcomment().toUpperCase();
				String value = filter.getValue().toUpperCase();

				// filter
				if (filter.getType() == StringOperator.Type.EQUALS) {
					if (!eventComment.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.NOTEQUALS) {
					if (eventComment.equals(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.CONTAINS) {
					if (!eventComment.contains(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.ENDSWITH) {
					if (!eventComment.endsWith(value)) {
						remove.add(item);
					}
				}
				else if (filter.getType() == StringOperator.Type.STARTSWITH) {
					if (!eventComment.startsWith(value)) {
						remove.add(item);
					}
				}
			}
		}

		// remove filtered items
		newContent.removeAll(remove);
	}

	/**
	 * Filters load case column data according to selected criteria.
	 *
	 * @param newContent
	 *            New data content.
	 * @param filters
	 *            Column filters.
	 */
	private static void filterLoadcaseColumn(ArrayList<STFTableItem> newContent, ObservableList<NumberOperator<Integer>> filters) {

		// create list to remove
		ArrayList<STFTableItem> remove = new ArrayList<>();

		// loop over filters
		for (NumberOperator<Integer> filter : filters) {

			// loop over content items
			for (STFTableItem item : newContent) {
				if (filter.getType() == NumberOperator.Type.EQUALS) {
					if (item.getLoadcase() != filter.getValue()) {
						remove.add(item);
					}
				}
				else if (filter.getType() == NumberOperator.Type.NOTEQUALS) {
					if (item.getLoadcase() == filter.getValue()) {
						remove.add(item);
					}
				}
				else if (filter.getType() == NumberOperator.Type.GREATERTHAN) {
					if (item.getLoadcase() <= filter.getValue()) {
						remove.add(item);
					}
				}
				else if (filter.getType() == NumberOperator.Type.GREATERTHANEQUALS) {
					if (item.getLoadcase() < filter.getValue()) {
						remove.add(item);
					}
				}
				else if (filter.getType() == NumberOperator.Type.LESSTHAN) {
					if (item.getLoadcase() >= filter.getValue()) {
						remove.add(item);
					}
				}
				else if (filter.getType() == NumberOperator.Type.LESSTHANEQUALS) {
					if (item.getLoadcase() > filter.getValue()) {
						remove.add(item);
					}
				}
			}
		}

		// remove filtered items
		newContent.removeAll(remove);
	}

	/**
	 * Filters stress column data according to selected criteria.
	 *
	 * @param newContent
	 *            New data content.
	 * @param filters
	 *            Column filters.
	 * @param stressColumn
	 *            Stress column index.
	 */
	private static void filterStressColumn(ArrayList<STFTableItem> newContent, ObservableList<NumberOperator<Double>> filters, int stressColumn) {

		// create list to remove
		ArrayList<STFTableItem> remove = new ArrayList<>();

		// loop over filters
		for (NumberOperator<Double> filter : filters) {

			// loop over content items
			for (STFTableItem item : newContent) {
				if (filter.getType() == NumberOperator.Type.EQUALS) {
					if (item.getStress(stressColumn) != filter.getValue()) {
						remove.add(item);
					}
				}
				else if (filter.getType() == NumberOperator.Type.NOTEQUALS) {
					if (item.getStress(stressColumn) == filter.getValue()) {
						remove.add(item);
					}
				}
				else if (filter.getType() == NumberOperator.Type.GREATERTHAN) {
					if (item.getStress(stressColumn) <= filter.getValue()) {
						remove.add(item);
					}
				}
				else if (filter.getType() == NumberOperator.Type.GREATERTHANEQUALS) {
					if (item.getStress(stressColumn) < filter.getValue()) {
						remove.add(item);
					}
				}
				else if (filter.getType() == NumberOperator.Type.LESSTHAN) {
					if (item.getStress(stressColumn) >= filter.getValue()) {
						remove.add(item);
					}
				}
				else if (filter.getType() == NumberOperator.Type.LESSTHANEQUALS) {
					if (item.getStress(stressColumn) > filter.getValue()) {
						remove.add(item);
					}
				}
			}
		}

		// remove filtered items
		newContent.removeAll(remove);
	}

	/**
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot text view panel.
	 */
	public static STFInfoViewPanel load(InfoViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("STFInfoViewPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			STFInfoViewPanel controller = (STFInfoViewPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;
			controller.imagePanels_ = new STFInfoImagePanel[12];
			controller.imagePanels_[0] = STFInfoImagePanel.load(controller, PilotPointImageType.IMAGE);
			controller.imagePanels_[1] = STFInfoImagePanel.load(controller, PilotPointImageType.MISSION_PROFILE);
			controller.imagePanels_[2] = STFInfoImagePanel.load(controller, PilotPointImageType.LONGEST_FLIGHT);
			controller.imagePanels_[3] = STFInfoImagePanel.load(controller, PilotPointImageType.FLIGHT_WITH_HIGHEST_OCCURRENCE);
			controller.imagePanels_[4] = STFInfoImagePanel.load(controller, PilotPointImageType.FLIGHT_WITH_MAX_TOTAL_STRESS);
			controller.imagePanels_[5] = STFInfoImagePanel.load(controller, PilotPointImageType.LEVEL_CROSSING);
			controller.imagePanels_[6] = STFInfoImagePanel.load(controller, PilotPointImageType.DAMAGE_ANGLE);
			controller.imagePanels_[7] = STFInfoImagePanel.load(controller, PilotPointImageType.NUMBER_OF_PEAKS);
			controller.imagePanels_[8] = STFInfoImagePanel.load(controller, PilotPointImageType.FLIGHT_OCCURRENCE);
			controller.imagePanels_[9] = STFInfoImagePanel.load(controller, PilotPointImageType.RAINFLOW_HISTOGRAM);
			controller.imagePanels_[10] = STFInfoImagePanel.load(controller, PilotPointImageType.LOADCASE_DAMAGE_CONTRIBUTION);
			controller.imagePanels_[11] = STFInfoImagePanel.load(controller, PilotPointImageType.FLIGHT_DAMAGE_CONTRIBUTION);

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
