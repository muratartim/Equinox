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
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.controlsfx.control.PopOver;

import equinox.Equinox;
import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.ActionHandler;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftEquivalentStresses;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.fileType.AircraftLinearEquivalentStress;
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.fileType.AircraftLoadCases;
import equinox.data.fileType.AircraftModel;
import equinox.data.fileType.AircraftPreffasEquivalentStress;
import equinox.data.fileType.DamageAngle;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalFlight;
import equinox.data.fileType.ExternalFlights;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.Flight;
import equinox.data.fileType.FlightDamageContributions;
import equinox.data.fileType.Flights;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.data.fileType.PilotPoint;
import equinox.data.fileType.PilotPoints;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.Rfort;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.fileType.StressSequence;
import equinox.data.ui.FileListCell;
import equinox.data.ui.FileTreeCell;
import equinox.font.IconicFont;
import equinox.plugin.FileType;
import equinox.task.AddFiles;
import equinox.task.AddSpectrum;
import equinox.task.AddStressSequence;
import equinox.task.GetAircraftEquivalentStressInfo;
import equinox.task.GetAircraftModelInfo;
import equinox.task.GetDamageAngleInfo;
import equinox.task.GetDamageContributionInfo;
import equinox.task.GetExternalFatigueEquivalentStressInfo;
import equinox.task.GetExternalFlightInfo;
import equinox.task.GetExternalLinearEquivalentStressInfo;
import equinox.task.GetExternalPreffasEquivalentStressInfo;
import equinox.task.GetExternalStressSequenceInfo;
import equinox.task.GetFastFatigueEquivalentStressInfo;
import equinox.task.GetFastLinearEquivalentStressInfo;
import equinox.task.GetFastPreffasEquivalentStressInfo;
import equinox.task.GetFatigueEquivalentStressInfo;
import equinox.task.GetFlightDamageContributionInfo;
import equinox.task.GetFlightInfo;
import equinox.task.GetLinearEquivalentStressInfo;
import equinox.task.GetLoadCaseInfo;
import equinox.task.GetPreffasEquivalentStressInfo;
import equinox.task.GetRfortInfo;
import equinox.task.GetSTFInfo1;
import equinox.task.GetSpectrumInfo;
import equinox.task.GetStressSequenceInfo;
import equinox.task.automation.CheckInstructionSet;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Class for file view panel controller.
 *
 * @author Murat Artim
 * @date Jan 17, 2014
 * @time 8:00:31 PM
 */
public class FileViewPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Action handler. */
	private ActionHandler handler_;

	/** Files list. */
	private final ObservableList<TreeItem<String>> files_ = FXCollections.observableArrayList(), selectedFiles_ = FXCollections.observableArrayList();

	/** Filter panel. */
	private FilterPanel filterPanel_;

	/** The thread executor of the network watcher. */
	private ExecutorService threadExecutor_;

	@FXML
	private VBox root_;

	@FXML
	private TreeView<String> fileTree_;

	@FXML
	private ListView<TreeItem<String>> fileList_;

	@FXML
	private TextField search_;

	@FXML
	private Button cancel_, filter_;

	@FXML
	private ImageView noFileLabel_, searchIcon_;

	@FXML
	private StackPane stackPane_;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		// create thread executor of the network watcher
		threadExecutor_ = Executors.newSingleThreadExecutor();

		// setup file tree
		fileTree_.setRoot(new TreeItem<String>());
		fileTree_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// add selection listener to file tree
		fileTree_.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<String>>) arg01 -> {
			fileTreeSelectionChanged();
			updateSelectionLabel();
		});

		// add change listener to file tree
		fileTree_.getRoot().addEventHandler(TreeItem.childrenModificationEvent(), event -> fileTreeFilesChanged());

		// set cell factory to file tree
		fileTree_.setCellFactory(param -> new FileTreeCell(owner_.getOwner()));

		// enable multi-selection
		fileList_.setItems(files_);
		fileList_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// add selection listener to flight list
		fileList_.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<String>>) arg01 -> {
			fileListSelectionChanged();
			updateSelectionLabel();
		});

		// set cell factory to file list
		fileList_.setCellFactory(param -> new FileListCell(owner_.getOwner()));

		// setup search field
		search_.textProperty().addListener((ChangeListener<String>) (ov, old_Val, new_val) -> {

			// show file list
			if (!fileList_.isVisible()) {
				fileList_.setVisible(true);
				fileTree_.setVisible(false);
				fileTree_.setMouseTransparent(true);
				updateSelectionLabel();
			}

			// search
			search(old_Val, new_val);
		});

		// load filter panel
		filterPanel_ = FilterPanel.load(this);
	}

	@Override
	public void start() {
		handler_ = new ActionHandler(owner_.getOwner());
	}

	/**
	 * Stops this panel.
	 *
	 */
	public void stop() {

		// stop thread executor
		Utility.shutdownThreadExecutor(threadExecutor_);
		Equinox.LOGGER.info("File View Panel thread executor stopped.");
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public void showing() {
		// no implementation
	}

	@Override
	public String getHeader() {
		return "Files";
	}

	/**
	 * Clears file selection.
	 *
	 */
	public void clearFileSelection() {
		fileTree_.getSelectionModel().clearSelection();
		fileList_.getSelectionModel().clearSelection();
	}

	/**
	 * Returns file tree root.
	 *
	 * @return File tree root.
	 */
	public TreeItem<String> getFileTreeRoot() {
		return fileTree_.getRoot();
	}

	/**
	 * Returns the search icon of this panel.
	 *
	 * @return Search icon of this panel.
	 */
	public ImageView getSearchIcon() {
		return searchIcon_;
	}

	/**
	 * Returns selected files.
	 *
	 * @return Selected files.
	 */
	public ObservableList<TreeItem<String>> getSelectedFiles() {
		return selectedFiles_;
	}

	/**
	 * Shows pop-over on the selected item.
	 *
	 * @param popOver
	 *            Pop-over to show.
	 * @param title
	 *            Title of popup. This is only needed in case the popup cannot be shown on the item.
	 */
	public void showPopOverOnSelectedItem(PopOver popOver, String title) {

		// file list is visible
		if (fileList_.isVisible()) {
			popOver.setDetached(true);
			popOver.setTitle(title);
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(owner_.getOwner().getOwner().getStage());
			return;
		}

		// get first selected item
		SpectrumItem item = (SpectrumItem) fileTree_.getSelectionModel().getSelectedItem();

		// null item or null graphic
		if (item == null || item.getGraphic() == null) {
			popOver.setDetached(true);
			popOver.setTitle(title);
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(owner_.getOwner().getOwner().getStage());
			return;
		}

		// try showing it on the item
		try {
			popOver.show(item.getGraphic());
		}

		// item not visible
		catch (Exception e) {
			popOver.setDetached(true);
			popOver.setTitle(title);
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(owner_.getOwner().getOwner().getStage());
		}
	}

	/**
	 * Finds similar items.
	 *
	 * @param items
	 *            Items to search for.
	 */
	public void search(SpectrumItem... items) {
		String name = items[0].getName();
		for (int i = 1; i < items.length; i++) {
			name += "|" + items[i].getName();
		}
		search_.setText(name);
	}

	/**
	 * Selects given file in the file tree.
	 *
	 * @param selected
	 *            Selected file.
	 * @param treeItem
	 *            File tree item to start search.
	 */
	public void selectFile(SpectrumItem selected, TreeItem<String> treeItem) {

		// not root
		if (!treeItem.equals(fileTree_.getRoot())) {
			if (selected.equals(treeItem)) {
				onCancelSearchClicked();
				fileTree_.getSelectionModel().clearSelection();
				fileTree_.getSelectionModel().select(treeItem);
				fileTree_.scrollTo(fileTree_.getSelectionModel().getSelectedIndex());
				return;
			}
		}

		// loop over children
		for (TreeItem<String> item : treeItem.getChildren()) {
			selectFile(selected, item);
		}
	}

	/**
	 * Filters files for the given filter class.
	 *
	 * @param fileClasses
	 *            File filter classes.
	 */
	public void filter(Class<?>... fileClasses) {
		fileList_.getSelectionModel().clearSelection();
		ObservableList<TreeItem<String>> subentries = FXCollections.observableArrayList();
		for (TreeItem<String> item : fileList_.getItems()) {
			Class<?> itemClass = item.getClass();
			for (Class<?> fileClass : fileClasses) {
				if (itemClass.equals(fileClass)) {
					subentries.add(item);
					break;
				}
			}
		}
		fileList_.setItems(subentries);
		filter_.setVisible(false);
	}

	@SuppressWarnings("static-method")
	@FXML
	private void onDragOver(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		if (db.hasFiles()) {

			// check file types
			for (File file : db.getFiles()) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// not recognized
				if (fileType == null) {
					continue;
				}

				// accepted type
				if (fileType.equals(FileType.ANA) || fileType.equals(FileType.GZ) || fileType.equals(FileType.ZIP) || fileType.equals(FileType.CVT) || fileType.equals(FileType.FLS) || fileType.equals(FileType.XLS) || fileType.equals(FileType.TXT) || fileType.equals(FileType.STH)
						|| fileType.equals(FileType.SIGMA) || fileType.equals(FileType.F07) || fileType.equals(FileType.F06) || fileType.equals(FileType.GRP) || fileType.equals(FileType.SPEC) || fileType.equals(FileType.XML) || fileType.equals(FileType.JSON)) {
					event.acceptTransferModes(TransferMode.ANY);
					break;
				}
			}
		}

		// consume event
		event.consume();
	}

	@FXML
	private void onDragDropped(DragEvent event) {

		// get drag board
		Dragboard db = event.getDragboard();

		// files
		boolean success = false;
		if (db.hasFiles()) {

			// get files
			List<File> files = db.getFiles();

			// loop over files
			boolean addSTH = false, addSIGMA = false, addACModel = false, addSpectrum = false, addSpec = false, runXml = false, runJson = false;
			for (File file : files) {

				// get file type
				FileType fileType = FileType.getFileType(file);

				// STH
				if (fileType.equals(FileType.STH)) {
					addSTH = true;
					break;
				}

				// SIGMA
				else if (fileType.equals(FileType.SIGMA)) {
					addSIGMA = true;
					break;
				}

				// A/C model file
				else if (fileType.equals(FileType.F07) || fileType.equals(FileType.F06) || fileType.equals(FileType.GRP)) {
					addACModel = true;
					break;
				}

				// spectrum
				else if (fileType.equals(FileType.ANA) || fileType.equals(FileType.GZ) || fileType.equals(FileType.ZIP) || fileType.equals(FileType.CVT) || fileType.equals(FileType.FLS) || fileType.equals(FileType.XLS) || fileType.equals(FileType.TXT)) {
					addSpectrum = true;
					break;
				}

				// SPEC file
				else if (fileType.equals(FileType.SPEC)) {
					addSpec = true;
					break;
				}

				// XML file
				else if (fileType.equals(FileType.XML)) {
					runXml = true;
					break;
				}

				// JSON file
				else if (fileType.equals(FileType.JSON)) {
					runJson = true;
					break;
				}
			}

			// SPEC file
			if (addSpec) {

				// get progress panel
				ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

				// loop over files
				for (File file : files) {

					// get file type
					FileType fileType = FileType.getFileType(file);

					// add spectrum
					if (fileType.equals(FileType.SPEC)) {
						tm.runTaskInParallel(new AddSpectrum(file.toPath()));
					}
				}
			}

			// add spectrum
			else if (addSpectrum) {

				// show add spectrum panel
				owner_.onAddCDFSetClicked();

				// process files
				AddSpectrumPanel panel = (AddSpectrumPanel) owner_.getSubPanel(InputPanel.ADD_SPECTRUM_PANEL);
				success = panel.processFiles(files);
			}

			// add stress sequence
			else if (addSTH || addSIGMA) {

				// get progress panel
				ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

				// loop over files
				for (File file : files) {

					// get file type
					FileType fileType = FileType.getFileType(file);

					// add STH
					if (addSTH && fileType.equals(FileType.STH)) {

						// show panel
						owner_.showSubPanel(InputPanel.ADD_STH_PANEL);

						// process files
						AddSTHPanel panel = (AddSTHPanel) owner_.getSubPanel(InputPanel.ADD_STH_PANEL);
						success = panel.processFiles(files);
						break;
					}

					// add SIGMA
					else if (addSIGMA && fileType.equals(FileType.SIGMA)) {
						tm.runTaskInParallel(new AddStressSequence(file.toPath()));
					}
				}
			}

			// add A/C model
			else if (addACModel) {

				// show add A/C model panel
				owner_.onAddACModelClicked();

				// process files
				AddAircraftModelPanel panel = (AddAircraftModelPanel) owner_.getSubPanel(InputPanel.ADD_AC_MODEL_PANEL);
				success = panel.processFiles(files);
			}

			// run XML or JSON
			else if (runXml || runJson) {

				// get progress panel
				ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

				// loop over files
				for (File file : files) {

					// get file type
					FileType fileType = FileType.getFileType(file);

					// check & run instruction set
					if (fileType.equals(FileType.XML) || fileType.equals(FileType.JSON)) {
						tm.runTaskInParallel(new CheckInstructionSet(file.toPath(), CheckInstructionSet.RUN));
					}
				}
			}
		}

		// notify event source
		event.setDropCompleted(success);

		// consume event
		event.consume();
	}

	@FXML
	private void onCancelSearchClicked() {
		search_.clear();
		fileList_.setVisible(false);
		fileTree_.setVisible(true);
		fileTree_.setMouseTransparent(false);
		fileTree_.getSelectionModel().clearSelection();
	}

	@FXML
	private void onFilterFilesClicked() {
		filterPanel_.show(filter_);
	}

	/**
	 * Searches file list for given new value.
	 *
	 * @param old_Val
	 *            Old text value.
	 * @param new_Val
	 *            New text value.
	 */
	private void search(String old_Val, String new_Val) {
		fileList_.getSelectionModel().clearSelection();
		if (old_Val != null && new_Val.length() < old_Val.length()) {
			fileList_.setItems(files_);
		}
		String value = new_Val.toUpperCase();
		String[] split = null;
		if (value.contains("|")) {
			split = value.split("\\|");
		}
		ObservableList<TreeItem<String>> subentries = FXCollections.observableArrayList();
		for (TreeItem<String> item : fileList_.getItems()) {
			if (split == null) {
				if (item.toString().toUpperCase().contains(value)) {
					subentries.add(item);
				}
			}
			else {
				String val = item.toString().toUpperCase();
				for (String val2 : split) {
					if (val.contains(val2)) {
						subentries.add(item);
						break;
					}
				}
			}
		}
		fileList_.setItems(subentries);
		cancel_.setVisible(!new_Val.isEmpty());
		filter_.setVisible(!new_Val.isEmpty());
		if (new_Val.isEmpty()) {
			onCancelSearchClicked();
			return;
		}
	}

	/**
	 * Called when file tree selection has changed.
	 */
	private void fileTreeSelectionChanged() {

		// no selection
		if (fileTree_.getSelectionModel().isEmpty()) {
			selectedFiles_.clear();
			fileTree_.setContextMenu(null);
			owner_.getOwner().getMenuBarPanel().setSelectedMenu(null, handler_);
			InfoViewPanel textViewPanel = (InfoViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			textViewPanel.clearView();
		}

		// there is new selection
		else {

			// set selected files
			selectedFiles_.setAll(fileTree_.getSelectionModel().getSelectedItems());

			// create & set context menu to file tree
			fileTree_.setContextMenu(getContextMenu());

			// create & set context menu to menu bar (selected menu)
			owner_.getOwner().getMenuBarPanel().setSelectedMenu(getContextMenu(), handler_);

			// setup file view
			setupFileView((SpectrumItem) fileTree_.getSelectionModel().getSelectedItem());
		}
	}

	/**
	 * Called when file list selection has changed.
	 */
	private void fileListSelectionChanged() {

		// no selection
		if (fileList_.getSelectionModel().isEmpty()) {
			selectedFiles_.clear();
			fileList_.setContextMenu(null);
			owner_.getOwner().getMenuBarPanel().setSelectedMenu(null, handler_);
			InfoViewPanel textViewPanel = (InfoViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			textViewPanel.clearView();
		}

		// there is new selection
		else {

			// set selected files
			selectedFiles_.setAll(fileList_.getSelectionModel().getSelectedItems());

			// create & set context menu to file tree
			fileList_.setContextMenu(getContextMenu());

			// create & set context menu to menu bar (selected menu)
			owner_.getOwner().getMenuBarPanel().setSelectedMenu(getContextMenu(), handler_);

			// setup file view
			setupFileView((SpectrumItem) fileList_.getSelectionModel().getSelectedItem());
		}
	}

	/**
	 * Sets up file view according to current selection.
	 *
	 * @param selected
	 *            Selected file.
	 */
	public void setupFileView(SpectrumItem selected) {

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();

		// spectrum
		if (selected instanceof Spectrum) {
			tm.runTaskInParallel(new GetSpectrumInfo((Spectrum) selected));
		}
		else if (selected instanceof STFFile) {
			tm.runTaskInParallel(new GetSTFInfo1((STFFile) selected));
		}
		else if (selected instanceof StressSequence) {
			tm.runTaskInParallel(new GetStressSequenceInfo((StressSequence) selected));
		}
		else if (selected instanceof Flight) {
			tm.runTaskInParallel(new GetFlightInfo((Flight) selected));
		}
		else if (selected instanceof FatigueEquivalentStress) {
			tm.runTaskInParallel(new GetFatigueEquivalentStressInfo((FatigueEquivalentStress) selected));
		}
		else if (selected instanceof PreffasEquivalentStress) {
			tm.runTaskInParallel(new GetPreffasEquivalentStressInfo((PreffasEquivalentStress) selected));
		}
		else if (selected instanceof LinearEquivalentStress) {
			tm.runTaskInParallel(new GetLinearEquivalentStressInfo((LinearEquivalentStress) selected));
		}
		else if (selected instanceof FastFatigueEquivalentStress) {
			tm.runTaskInParallel(new GetFastFatigueEquivalentStressInfo((FastFatigueEquivalentStress) selected));
		}
		else if (selected instanceof FastPreffasEquivalentStress) {
			tm.runTaskInParallel(new GetFastPreffasEquivalentStressInfo((FastPreffasEquivalentStress) selected));
		}
		else if (selected instanceof FastLinearEquivalentStress) {
			tm.runTaskInParallel(new GetFastLinearEquivalentStressInfo((FastLinearEquivalentStress) selected));
		}
		else if (selected instanceof DamageAngle) {
			tm.runTaskInParallel(new GetDamageAngleInfo((DamageAngle) selected));
		}
		else if (selected instanceof LoadcaseDamageContributions) {
			tm.runTaskInParallel(new GetDamageContributionInfo((LoadcaseDamageContributions) selected));
		}
		else if (selected instanceof FlightDamageContributions) {
			tm.runTaskInParallel(new GetFlightDamageContributionInfo((FlightDamageContributions) selected));
		}
		else if (selected instanceof ExternalStressSequence) {
			tm.runTaskInParallel(new GetExternalStressSequenceInfo((ExternalStressSequence) selected));
		}
		else if (selected instanceof ExternalFlight) {
			tm.runTaskInParallel(new GetExternalFlightInfo((ExternalFlight) selected));
		}
		else if (selected instanceof ExternalFatigueEquivalentStress) {
			tm.runTaskInParallel(new GetExternalFatigueEquivalentStressInfo((ExternalFatigueEquivalentStress) selected));
		}
		else if (selected instanceof ExternalPreffasEquivalentStress) {
			tm.runTaskInParallel(new GetExternalPreffasEquivalentStressInfo((ExternalPreffasEquivalentStress) selected));
		}
		else if (selected instanceof ExternalLinearEquivalentStress) {
			tm.runTaskInParallel(new GetExternalLinearEquivalentStressInfo((ExternalLinearEquivalentStress) selected));
		}
		else if (selected instanceof AircraftModel) {
			tm.runTaskInParallel(new GetAircraftModelInfo((AircraftModel) selected));
		}
		else if (selected instanceof AircraftLoadCase) {
			tm.runTaskInParallel(new GetLoadCaseInfo((AircraftLoadCase) selected));
		}
		else if (selected instanceof AircraftFatigueEquivalentStress || selected instanceof AircraftPreffasEquivalentStress || selected instanceof AircraftLinearEquivalentStress) {
			tm.runTaskInParallel(new GetAircraftEquivalentStressInfo(selected));
		}
		else if (selected instanceof Rfort) {
			tm.runTaskInParallel(new GetRfortInfo((Rfort) selected));
		}
	}

	/**
	 * Creates and returns context menu according to current selection.
	 *
	 * @return Context menu.
	 */
	private ContextMenu getContextMenu() {

		// count selected item types
		int cdf = 0, stf = 0, sth = 0, flight = 0, fatigueEqStress = 0, preffasEqStress = 0, linearEqStress = 0, flights = 0, damAngle = 0, sthExt = 0, flightExt = 0, fatigueEqStressExt = 0, preffasEqStressExt = 0, linearEqStressExt = 0, flightsExt = 0, loadcaseDamCont = 0, flightDamCont = 0,
				acModel = 0, loadCases = 0, loadCase = 0, pilotPoints = 0, pilotPoint = 0, acEquivalentStresses = 0, acFatigueEquivalentStress = 0, acPreffasEquivalentStress = 0, acLinearEquivalentStress = 0, rfort = 0, fastFatigueEqStress = 0, fastPreffasEqStress = 0, fastLinearEqStress = 0,
				stfBucket = 0;
		for (TreeItem<String> item : selectedFiles_) {
			if (item instanceof Spectrum) {
				cdf++;
			}
			else if (item instanceof STFFile) {
				stf++;
			}
			else if (item instanceof StressSequence) {
				sth++;
			}
			else if (item instanceof Flights) {
				flights++;
			}
			else if (item instanceof Flight) {
				flight++;
			}
			else if (item instanceof FatigueEquivalentStress) {
				fatigueEqStress++;
			}
			else if (item instanceof PreffasEquivalentStress) {
				preffasEqStress++;
			}
			else if (item instanceof LinearEquivalentStress) {
				linearEqStress++;
			}
			else if (item instanceof FastFatigueEquivalentStress) {
				fastFatigueEqStress++;
			}
			else if (item instanceof FastPreffasEquivalentStress) {
				fastPreffasEqStress++;
			}
			else if (item instanceof FastLinearEquivalentStress) {
				fastLinearEqStress++;
			}
			else if (item instanceof DamageAngle) {
				damAngle++;
			}
			else if (item instanceof ExternalStressSequence) {
				sthExt++;
			}
			else if (item instanceof ExternalFlights) {
				flightsExt++;
			}
			else if (item instanceof ExternalFlight) {
				flightExt++;
			}
			else if (item instanceof ExternalFatigueEquivalentStress) {
				fatigueEqStressExt++;
			}
			else if (item instanceof ExternalPreffasEquivalentStress) {
				preffasEqStressExt++;
			}
			else if (item instanceof ExternalLinearEquivalentStress) {
				linearEqStressExt++;
			}
			else if (item instanceof LoadcaseDamageContributions) {
				loadcaseDamCont++;
			}
			else if (item instanceof FlightDamageContributions) {
				flightDamCont++;
			}
			else if (item instanceof AircraftModel) {
				acModel++;
			}
			else if (item instanceof AircraftLoadCases) {
				loadCases++;
			}
			else if (item instanceof AircraftLoadCase) {
				loadCase++;
			}
			else if (item instanceof PilotPoints) {
				pilotPoints++;
			}
			else if (item instanceof PilotPoint) {
				pilotPoint++;
			}
			else if (item instanceof AircraftEquivalentStresses) {
				acEquivalentStresses++;
			}
			else if (item instanceof AircraftFatigueEquivalentStress) {
				acFatigueEquivalentStress++;
			}
			else if (item instanceof AircraftPreffasEquivalentStress) {
				acPreffasEquivalentStress++;
			}
			else if (item instanceof AircraftLinearEquivalentStress) {
				acLinearEquivalentStress++;
			}
			else if (item instanceof Rfort) {
				rfort++;
			}
			else if (item instanceof STFFileBucket) {
				stfBucket++;
			}
		}

		// get 3D view availability
		boolean is3dEnabled = ((ObjectViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.OBJECT_VIEW)).isEnabled();

		// set context menu
		int all = selectedFiles_.size();
		if (cdf == all)
			return Spectrum.createContextMenu(all > 1, handler_, selectedFiles_);
		else if (stf == all)
			return STFFile.createContextMenu(all > 1, handler_, selectedFiles_);
		else if (sth == all)
			return StressSequence.createContextMenu(all, handler_);
		else if (flights == all)
			return Flights.createContextMenu(all > 1, handler_);
		else if (flight == all)
			return Flight.createContextMenu(all > 1, handler_, selectedFiles_);
		else if (fatigueEqStress == all)
			return FatigueEquivalentStress.createContextMenu(all > 1, handler_, is3dEnabled);
		else if (preffasEqStress == all)
			return PreffasEquivalentStress.createContextMenu(all > 1, handler_, is3dEnabled);
		else if (linearEqStress == all)
			return LinearEquivalentStress.createContextMenu(all > 1, handler_, is3dEnabled);
		else if (fastFatigueEqStress == all)
			return FastFatigueEquivalentStress.createContextMenu(all > 1, handler_, is3dEnabled);
		else if (fastPreffasEqStress == all)
			return FastPreffasEquivalentStress.createContextMenu(all > 1, handler_, is3dEnabled);
		else if (fastLinearEqStress == all)
			return FastLinearEquivalentStress.createContextMenu(all > 1, handler_, is3dEnabled);
		else if (damAngle == all)
			return DamageAngle.createContextMenu(all > 1, handler_);
		else if (sthExt == all)
			return ExternalStressSequence.createContextMenu(all > 1, handler_);
		else if (flightsExt == all)
			return ExternalFlights.createContextMenu(all > 1, handler_);
		else if (flightExt == all)
			return ExternalFlight.createContextMenu(all > 1, handler_, selectedFiles_);
		else if (fatigueEqStressExt == all)
			return ExternalFatigueEquivalentStress.createContextMenu(all > 1, handler_, is3dEnabled);
		else if (preffasEqStressExt == all)
			return ExternalPreffasEquivalentStress.createContextMenu(all > 1, handler_, is3dEnabled);
		else if (linearEqStressExt == all)
			return ExternalLinearEquivalentStress.createContextMenu(all > 1, handler_, is3dEnabled);
		else if (loadcaseDamCont == all)
			return LoadcaseDamageContributions.createContextMenu(all > 1, handler_);
		else if (flightDamCont == all)
			return FlightDamageContributions.createContextMenu(all > 1, handler_);
		else if (acModel == all)
			return AircraftModel.createContextMenu(all > 1, handler_, selectedFiles_, is3dEnabled);
		else if (loadCases == all)
			return AircraftLoadCases.createContextMenu(all > 1, handler_);
		else if (loadCase == all)
			return AircraftLoadCase.createContextMenu(all > 1, handler_, selectedFiles_, is3dEnabled);
		else if (pilotPoints == all)
			return PilotPoints.createContextMenu(all > 1, handler_, selectedFiles_);
		else if (pilotPoint == all)
			return PilotPoint.createContextMenu(all > 1, handler_);
		else if (acEquivalentStresses == all)
			return AircraftEquivalentStresses.createContextMenu(all > 1, handler_);
		else if (acFatigueEquivalentStress == all)
			return AircraftFatigueEquivalentStress.createContextMenu(all > 1, handler_, is3dEnabled);
		else if (acPreffasEquivalentStress == all)
			return AircraftPreffasEquivalentStress.createContextMenu(all > 1, handler_, is3dEnabled);
		else if (acLinearEquivalentStress == all)
			return AircraftLinearEquivalentStress.createContextMenu(all > 1, handler_, is3dEnabled);
		else if (rfort == all)
			return Rfort.createContextMenu(all > 1, handler_, selectedFiles_);
		else if (stfBucket == all)
			return STFFileBucket.createContextMenu(all > 1, handler_);
		else
			return null;
	}

	/**
	 * Called when files in the file tree have changed.
	 *
	 */
	private void fileTreeFilesChanged() {

		// remove context menu if no files
		boolean isEmpty = fileTree_.getRoot().getChildren().isEmpty();
		if (!isEmpty && stackPane_.getChildren().contains(noFileLabel_)) {
			stackPane_.getChildren().remove(noFileLabel_);
		}
		else if (isEmpty && !stackPane_.getChildren().contains(noFileLabel_)) {
			stackPane_.getChildren().add(noFileLabel_);
		}
		if (isEmpty) {
			fileTree_.setContextMenu(null);
			fileList_.setContextMenu(null);
		}

		// clear file list
		fileList_.getSelectionModel().clearSelection();
		files_.clear();

		// add files to file list
		threadExecutor_.submit(new AddFiles(fileTree_.getRoot(), files_, fileList_, search_));
	}

	/**
	 * Updates selection label.
	 *
	 */
	private void updateSelectionLabel() {

		// get status label
		Label statusLabel = owner_.getStatusLabel();

		// file tree is visible
		if (fileTree_.isVisible()) {

			// no selection
			if (fileTree_.getSelectionModel().isEmpty()) {
				statusLabel.setText("");
				statusLabel.setVisible(false);
			}

			// items selected
			else {
				int numSelected = fileTree_.getSelectionModel().getSelectedItems().size();
				statusLabel.setText(numSelected + (numSelected == 1 ? " item selected" : " items selected"));
				statusLabel.setVisible(true);
			}
		}

		// file list is visible
		else if (fileList_.isVisible()) {

			// no selection
			if (fileList_.getSelectionModel().isEmpty()) {
				statusLabel.setText("");
				statusLabel.setVisible(false);
			}

			// items selected
			else {
				int numSelected = fileList_.getSelectionModel().getSelectedItems().size();
				statusLabel.setText(numSelected + (numSelected == 1 ? " item selected" : " items selected"));
				statusLabel.setVisible(true);
			}
		}

		// not visible
		else {
			statusLabel.setText("");
			statusLabel.setVisible(false);
		}
	}

	/**
	 * Loads and returns files panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded files panel.
	 */
	public static FileViewPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader();
			fxmlLoader.setLocation(EquinoxTheme.getFXMLResource("FileViewPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, "noFileLabel_");

			// get controller
			FileViewPanel controller = (FileViewPanel) fxmlLoader.getController();

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
