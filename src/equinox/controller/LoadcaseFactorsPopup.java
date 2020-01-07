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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import com.jfoenix.controls.JFXTabPane;

import control.validationField.DoubleValidationField;
import equinox.controller.InputPanel.InputPopup;
import equinox.data.EquinoxTheme;
import equinox.data.LoadcaseFactor;
import equinox.data.LoadcaseItem;
import equinox.data.fileType.Spectrum;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.plugin.FileType;
import equinox.task.GetLoadcases;
import equinox.task.GetLoadcases.LoadcaseRequestingPanel;
import equinox.task.GetMultiplicationTableLoadcases;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

/**
 * Class for loadcase factors popup controller.
 *
 * @author Murat Artim
 * @date Dec 16, 2015
 * @time 8:50:36 AM
 */
public class LoadcaseFactorsPopup implements InputPopup, LoadcaseRequestingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	/** Requesting panel. */
	private LoadcaseFactorAddingPanel panel_;

	/** Spectrum. */
	private Spectrum spectrum_;

	/** Loadcase lists. */
	private final ObservableList<LoadcaseItem> onegLoadcases_ = FXCollections.observableArrayList(), incLoadcases_ = FXCollections.observableArrayList();

	/** Multiplication table loadcase factor mapping. */
	private HashMap<LoadcaseItem, ArrayList<Double>> loadcaseFactorMapping_;

	/** Drop image. */
	private ImageView dropImage_;

	@FXML
	private VBox root_;

	@FXML
	private TableView<LoadcaseItem> onegTable_, incTable_, multTable_;

	@FXML
	private TextField search_;

	@FXML
	private Button cancelSearch_, ok_, browse_;

	@FXML
	private Label multColLabel_;

	@FXML
	private Spinner<Integer> multCol_;

	@FXML
	private Separator separator_;

	@FXML
	private DoubleValidationField modifierValue_;

	@FXML
	private MenuButton modifierMethod_;

	@FXML
	private JFXTabPane loadcaseTypeTab_;

	@FXML
	private StackPane searchStack_;

	@SuppressWarnings("rawtypes")
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// listen for tab selections
		loadcaseTypeTab_.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				tabSelectionChanged(newValue.intValue());
			}
		});

		// create 1G table columns
		TableColumn<LoadcaseItem, String> onegLoadcaseNumberCol = new TableColumn<>("Loadcase");
		onegLoadcaseNumberCol.setCellValueFactory(new PropertyValueFactory<LoadcaseItem, String>("loadcaseNumber"));
		TableColumn<LoadcaseItem, String> onegEventNameCol = new TableColumn<>("Event Name");
		onegEventNameCol.setCellValueFactory(new PropertyValueFactory<LoadcaseItem, String>("eventName"));
		TableColumn<LoadcaseItem, String> onegCommentsCol = new TableColumn<>("Comments");
		onegCommentsCol.setCellValueFactory(new PropertyValueFactory<LoadcaseItem, String>("comments"));
		onegTable_.getColumns().add(onegLoadcaseNumberCol);
		onegTable_.getColumns().add(onegEventNameCol);
		onegTable_.getColumns().add(onegCommentsCol);

		// create increment table columns
		TableColumn<LoadcaseItem, String> incLoadcaseNumberCol = new TableColumn<>("Loadcase");
		incLoadcaseNumberCol.setCellValueFactory(new PropertyValueFactory<LoadcaseItem, String>("loadcaseNumber"));
		TableColumn<LoadcaseItem, String> incEventNameCol = new TableColumn<>("Event Name");
		incEventNameCol.setCellValueFactory(new PropertyValueFactory<LoadcaseItem, String>("eventName"));
		TableColumn<LoadcaseItem, String> incCommentsCol = new TableColumn<>("Comments");
		incCommentsCol.setCellValueFactory(new PropertyValueFactory<LoadcaseItem, String>("comments"));
		incTable_.getColumns().add(incLoadcaseNumberCol);
		incTable_.getColumns().add(incEventNameCol);
		incTable_.getColumns().add(incCommentsCol);

		// create multiplication table columns
		TableColumn<LoadcaseItem, String> multLoadcaseNumberCol = new TableColumn<>("Loadcase");
		multLoadcaseNumberCol.setCellValueFactory(new PropertyValueFactory<LoadcaseItem, String>("loadcaseNumber"));
		TableColumn<LoadcaseItem, String> multEventNameCol = new TableColumn<>("Event Name");
		multEventNameCol.setCellValueFactory(new PropertyValueFactory<LoadcaseItem, String>("eventName"));
		TableColumn<LoadcaseItem, String> multCommentsCol = new TableColumn<>("Comments");
		multCommentsCol.setCellValueFactory(new PropertyValueFactory<LoadcaseItem, String>("comments"));
		multTable_.getColumns().add(multLoadcaseNumberCol);
		multTable_.getColumns().add(multEventNameCol);
		multTable_.getColumns().add(multCommentsCol);

		// create table placeholders
		onegTable_.setPlaceholder(NoResultsPanel.load("Your search did not match any 1G loadcase.", null));
		incTable_.setPlaceholder(NoResultsPanel.load("Your search did not match any increment loadcase.", null));
		multTable_.setPlaceholder(createMultiplicationTablePlaceHolder());

		// set automatic column sizing for tables
		onegTable_.setColumnResizePolicy(new Callback<TableView.ResizeFeatures, Boolean>() {

			@Override
			public Boolean call(TableView.ResizeFeatures param) {
				return true;
			}
		});
		incTable_.setColumnResizePolicy(new Callback<TableView.ResizeFeatures, Boolean>() {

			@Override
			public Boolean call(TableView.ResizeFeatures param) {
				return true;
			}
		});
		multTable_.setColumnResizePolicy(new Callback<TableView.ResizeFeatures, Boolean>() {

			@Override
			public Boolean call(TableView.ResizeFeatures param) {
				return true;
			}
		});

		// setup tables
		onegTable_.setItems(onegLoadcases_);
		onegTable_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		onegTable_.setTableMenuButtonVisible(true);
		incTable_.setItems(incLoadcases_);
		incTable_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		incTable_.setTableMenuButtonVisible(true);
		multTable_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		multTable_.setTableMenuButtonVisible(true);

		// setup search field
		search_.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> ov, String old_Val, String new_val) {
				search(old_Val, new_val);
			}
		});

		// setup validation fields
		modifierValue_.setDefaultValue(null);

		// setup multiplication table column
		multCol_.setValueFactory(new IntegerSpinnerValueFactory(1, 100, 1));
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public void setLoadcases(ArrayList<LoadcaseItem> loadcases) {

		// set table items
		onegLoadcases_.clear();
		incLoadcases_.clear();
		for (LoadcaseItem loadcase : loadcases) {
			if (loadcase.isOneg()) {
				onegLoadcases_.add(loadcase);
			}
			else {
				incLoadcases_.add(loadcase);
			}
		}
		onegTable_.setItems(onegLoadcases_);
		incTable_.setItems(incLoadcases_);

		// reset
		multTable_.getItems().clear();
		multTable_.getSelectionModel().clearSelection();
		multCol_.getValueFactory().setValue(1);
		if (loadcaseFactorMapping_ != null) {
			loadcaseFactorMapping_.clear();
			loadcaseFactorMapping_ = null;
		}
		search_.clear();
		onegTable_.getSelectionModel().clearSelection();
		incTable_.getSelectionModel().clearSelection();
		loadcaseTypeTab_.getSelectionModel().select(0);
		modifierValue_.reset();
		modifierMethod_.setText(GenerateStressSequenceInput.MULTIPLY);
		for (MenuItem item : modifierMethod_.getItems()) {
			RadioMenuItem radio = (RadioMenuItem) item;
			if (radio.getText().equals(GenerateStressSequenceInput.MULTIPLY)) {
				radio.setSelected(true);
				break;
			}
		}

		// show popup
		popOver_.show(panel_.getLoadcaseFactorPopupNode());

		// focus on search
		search_.requestFocus();
	}

	/**
	 * Sets multiplication table loadcases.
	 *
	 * @param loadcases
	 *            Loadcases.
	 */
	public void setMultiplicationTableLoadcases(HashMap<LoadcaseItem, ArrayList<Double>> loadcases) {

		// clear items
		multTable_.getItems().clear();
		multTable_.getSelectionModel().clearSelection();

		// set mapping
		loadcaseFactorMapping_ = loadcases;

		// add loadcases
		int numFactorCols = Integer.MAX_VALUE;
		for (LoadcaseItem loadcase : loadcaseFactorMapping_.keySet()) {
			multTable_.getItems().add(loadcase);
			int size = loadcaseFactorMapping_.get(loadcase).size();
			if (numFactorCols > size) {
				numFactorCols = size;
			}
		}

		// reset table column index
		multCol_.setValueFactory(new IntegerSpinnerValueFactory(1, numFactorCols, 1));
		multCol_.getValueFactory().setValue(1);
	}

	/**
	 * Shows this panel.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param spectrum
	 *            Selected spectrum.
	 */
	public void show(LoadcaseFactorAddingPanel panel, Spectrum spectrum) {

		// already shown
		if (isShown_)
			return;

		// set panel
		panel_ = panel;

		// set spectrum
		spectrum_ = spectrum;

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

		// request loadcases
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetLoadcases(this, spectrum_, GetLoadcases.ALL));
	}

	/**
	 * Called when tab selection changed.
	 *
	 * @param selectedIndex
	 *            Selected tab index.
	 */
	private void tabSelectionChanged(int selectedIndex) {

		// 1G or increment tabs
		if ((selectedIndex == 0) || (selectedIndex == 1)) {
			multCol_.setVisible(false);
			multColLabel_.setVisible(false);
			browse_.setVisible(false);
			separator_.setVisible(false);
			modifierValue_.setVisible(true);
			searchStack_.setVisible(true);
		}

		// multiplication table tab
		else if (selectedIndex == 2) {
			multCol_.setVisible(true);
			multColLabel_.setVisible(true);
			browse_.setVisible(true);
			separator_.setVisible(true);
			modifierValue_.setVisible(false);
			searchStack_.setVisible(false);
		}
	}

	/**
	 * Processes multiplication table file.
	 *
	 * @param file
	 *            Multiplication table file.
	 */
	private void processMUTFile(File file) {
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetMultiplicationTableLoadcases(this, spectrum_, file.toPath()));
	}

	@FXML
	private void onDragOver(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if ((event.getGestureSource() != multTable_) && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.MUT)) {
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
		if ((event.getGestureSource() != multTable_) && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.MUT)) {
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
		if ((event.getGestureSource() != multTable_) && db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.MUT)) {
					processMUTFile(file);
					success = true;
					break;
				}
			}
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}

	@FXML
	private void onBrowseClicked() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.MUT.getExtensionFilter());

		// show open dialog
		File file = fileChooser.showOpenDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if ((file == null) || !file.exists())
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(file);

		// process files
		processMUTFile(file);
	}

	@FXML
	private void onCancelSearchClicked() {
		search_.clear();
	}

	@FXML
	private void onOkClicked() {

		// get tab index
		int tabIndex = loadcaseTypeTab_.getSelectionModel().getSelectedIndex();

		// 1G or increment loadcases
		if ((tabIndex == 0) || (tabIndex == 1)) {
			process1GIncrementInputs(tabIndex == 0);
		}
		else if (tabIndex == 2) {
			processMultiplicationTableInputs();
		}
	}

	/**
	 * Processes multiplication table inputs.
	 */
	private void processMultiplicationTableInputs() {

		// no multiplication table supplied
		if ((loadcaseFactorMapping_ == null) || loadcaseFactorMapping_.isEmpty()) {
			String message = "No loadcase factors file supplied. Please supply a loadcase factors file to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(multTable_);
			return;
		}

		// get multiplication table column
		int multCol = multCol_.getValue() - 1;

		// get modifier value and method
		String method = null;
		for (MenuItem item : modifierMethod_.getItems()) {
			RadioMenuItem radio = (RadioMenuItem) item;
			if (radio.isSelected()) {
				method = radio.getText();
				break;
			}
		}

		// create loadcase factors
		LoadcaseFactor[] loadcaseFactors = new LoadcaseFactor[loadcaseFactorMapping_.size()];
		Iterator<Entry<LoadcaseItem, ArrayList<Double>>> iterator = loadcaseFactorMapping_.entrySet().iterator();
		int index = 0;
		while (iterator.hasNext()) {
			Entry<LoadcaseItem, ArrayList<Double>> entry = iterator.next();
			LoadcaseItem item = entry.getKey();
			loadcaseFactors[index] = new LoadcaseFactor();
			loadcaseFactors[index].setLoadcaseNumber(item.getLoadcaseNumber());
			loadcaseFactors[index].setEventName(item.getEventName());
			loadcaseFactors[index].setComments(item.getComments());
			loadcaseFactors[index].setIsOneg(item.isOneg());
			loadcaseFactors[index].setModifier(method, entry.getValue().get(multCol));
			index++;
		}

		// reset mapping
		loadcaseFactorMapping_.clear();
		loadcaseFactorMapping_ = null;

		// add loadcase factors to requesting panel
		panel_.addLoadcaseFactors(loadcaseFactors);

		// hide
		popOver_.hide();
	}

	/**
	 * Processes 1G or increment loadcase inputs.
	 *
	 * @param isOneg
	 *            True if 1G loadcases.
	 */
	private void process1GIncrementInputs(boolean isOneg) {

		// get selected loadcases
		ObservableList<LoadcaseItem> selected = isOneg ? onegTable_.getSelectionModel().getSelectedItems() : incTable_.getSelectionModel().getSelectedItems();

		// check inputs
		if (!check1GIncrementInputs(isOneg, selected))
			return;

		// get modifier value and method
		double value = Double.parseDouble(modifierValue_.getText());
		String method = null;
		for (MenuItem item : modifierMethod_.getItems()) {
			RadioMenuItem radio = (RadioMenuItem) item;
			if (radio.isSelected()) {
				method = radio.getText();
				break;
			}
		}

		// create loadcase factors
		LoadcaseFactor[] loadcaseFactors = new LoadcaseFactor[selected.size()];
		for (int i = 0; i < selected.size(); i++) {
			LoadcaseItem item = selected.get(i);
			loadcaseFactors[i] = new LoadcaseFactor();
			loadcaseFactors[i].setLoadcaseNumber(item.getLoadcaseNumber());
			loadcaseFactors[i].setEventName(item.getEventName());
			loadcaseFactors[i].setComments(item.getComments());
			loadcaseFactors[i].setIsOneg(item.isOneg());
			loadcaseFactors[i].setModifier(method, value);
		}

		// add loadcase factors to requesting panel
		panel_.addLoadcaseFactors(loadcaseFactors);

		// hide
		popOver_.hide();
	}

	/**
	 * Checks 1G or increment loadcase inputs.
	 *
	 * @param isOneg
	 *            True if current loadcase type is 1G.
	 * @param selectedLoadcases
	 *            Selected loadcases.
	 * @return True if the inputs are valid.
	 */
	private boolean check1GIncrementInputs(boolean isOneg, ObservableList<LoadcaseItem> selectedLoadcases) {

		// no loadcase selected
		if (selectedLoadcases.isEmpty()) {
			String message = "No loadcase selected. Please select at least 1 loadcase to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(isOneg ? onegTable_ : incTable_);
			return false;
		}

		// check loadcase factor
		String message = modifierValue_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(modifierValue_);
			return false;
		}

		// valid inputs
		return true;
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	@SuppressWarnings("static-method")
	@FXML
	private void onMethodSelected(ActionEvent e) {
		RadioMenuItem item = (RadioMenuItem) e.getSource();
		MenuButton owner = (MenuButton) item.getParentPopup().getOwnerNode();
		owner.setText(item.getText());
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

		// get current loadcase type
		boolean isOneg = loadcaseTypeTab_.getSelectionModel().getSelectedIndex() == 0;

		// 1G
		if (isOneg) {
			onegTable_.getSelectionModel().clearSelection();
			if ((old_Val != null) && (new_Val.length() < old_Val.length())) {
				onegTable_.setItems(onegLoadcases_);
			}
			String value = new_Val.toUpperCase();
			ObservableList<LoadcaseItem> subentries = FXCollections.observableArrayList();
			for (LoadcaseItem item : onegTable_.getItems()) {
				if (item.getSearchString().toUpperCase().contains(value)) {
					subentries.add(item);
				}
			}
			onegTable_.setItems(subentries);
			cancelSearch_.setVisible(!new_Val.isEmpty());
			if (new_Val.isEmpty()) {
				onCancelSearchClicked();
				return;
			}
		}

		// increment
		else {
			incTable_.getSelectionModel().clearSelection();
			if ((old_Val != null) && (new_Val.length() < old_Val.length())) {
				incTable_.setItems(incLoadcases_);
			}
			String value = new_Val.toUpperCase();
			ObservableList<LoadcaseItem> subentries = FXCollections.observableArrayList();
			for (LoadcaseItem item : incTable_.getItems()) {
				if (item.getSearchString().toUpperCase().contains(value)) {
					subentries.add(item);
				}
			}
			incTable_.setItems(subentries);
			cancelSearch_.setVisible(!new_Val.isEmpty());
			if (new_Val.isEmpty()) {
				onCancelSearchClicked();
				return;
			}
		}
	}

	/**
	 * Creates and returns multiplication table place holder.
	 *
	 * @return Multiplication table place holder.
	 */
	private Node createMultiplicationTablePlaceHolder() {

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
		Label label = new Label("Drop loadcase factors file (*.mut) here");
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
	 * @return The newly loaded file CDF set panel.
	 */
	public static LoadcaseFactorsPopup load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("LoadcaseFactorsPopup.fxml"));
			fxmlLoader.load();

			// get controller
			LoadcaseFactorsPopup controller = (LoadcaseFactorsPopup) fxmlLoader.getController();

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
	 * Interface for loadcase factor adding panels.
	 *
	 * @author Murat Artim
	 * @date Dec 12, 2015
	 * @time 2:56:37 PM
	 */
	public interface LoadcaseFactorAddingPanel {

		/**
		 * Adds loadcase factors.
		 *
		 * @param loadcaseFactors
		 *            Loadcase factors.
		 */
		void addLoadcaseFactors(LoadcaseFactor[] loadcaseFactors);

		/**
		 * Returns loadcase factor popup node.
		 *
		 * @return Loadcase factor popup node.
		 */
		Node getLoadcaseFactorPopupNode();
	}
}
