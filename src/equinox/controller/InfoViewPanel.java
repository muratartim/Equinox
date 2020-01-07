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
import java.util.ResourceBundle;

import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.SpectrumItem;
import equinox.data.ui.TableItem;
import equinox.plugin.FileType;
import equinox.task.SaveImage;
import equinox.utility.Utility;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for plot text view panel.
 *
 * @author Murat Artim
 * @date Jan 1, 2014
 * @time 4:56:44 PM
 */
public class InfoViewPanel implements InternalViewSubPanel {

	/** View index. */
	public static final int INFO_VIEW = 0, STF_VIEW = 1;

	/** View index. */
	private int viewIndex_ = INFO_VIEW;

	/** The owner panel. */
	private ViewPanel owner_;

	/** STF info view. */
	private STFInfoViewPanel stfView_;

	/** Controls. */
	private InfoViewControls controls_;

	@FXML
	private VBox root_;

	@FXML
	private StackPane stack_;

	@FXML
	private TreeTableView<TableItem> infoTable_;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		// load STF info view
		stfView_ = STFInfoViewPanel.load(this);
		stack_.getChildren().add(stfView_.getRoot());

		// initialize file info table
		TreeTableColumn<TableItem, String> fileInfoLabelCol = new TreeTableColumn<>("Label");
		fileInfoLabelCol.setCellValueFactory((TreeTableColumn.CellDataFeatures<TableItem, String> param) -> new ReadOnlyStringWrapper(param.getValue().getValue().getLabel()));
		TreeTableColumn<TableItem, String> fileInfoValueCol = new TreeTableColumn<>("Value");
		fileInfoValueCol.setCellValueFactory((TreeTableColumn.CellDataFeatures<TableItem, String> param) -> new ReadOnlyStringWrapper(param.getValue().getValue().getValue()));
		infoTable_.getColumns().add(fileInfoLabelCol);
		infoTable_.getColumns().add(fileInfoValueCol);
		Label infoTablePlaceHolder = new Label("Select a file to see its contents.");
		infoTablePlaceHolder.setStyle("-fx-text-fill:slategray; -fx-font-size:16px");
		infoTable_.setPlaceholder(infoTablePlaceHolder);
		fileInfoLabelCol.prefWidthProperty().bind(infoTable_.widthProperty().divide(5).multiply(2)); // w * 2/6
		fileInfoValueCol.prefWidthProperty().bind(infoTable_.widthProperty().divide(5).multiply(3)); // w * 4/6
		infoTable_.setRoot(new TreeItem<>(new TableItem("Label", "Value")));

		// create controls
		controls_ = InfoViewControls.load(this);

		// show info view
		showInfoView(false, null);
	}

	@Override
	public ViewPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public HBox getControls() {
		return controls_.getRoot();
	}

	@Override
	public String getHeader() {
		return "Info View";
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {
		// no implementation
	}

	@Override
	public void hiding() {
		stfView_.stopAnimation();
	}

	/**
	 * Returns the pilot point view.
	 *
	 * @return The pilot point view.
	 */
	public STFInfoViewPanel getSTFView() {
		return stfView_;
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
	 * Returns current view index.
	 *
	 * @return Current view index.
	 */
	public int getViewIndex() {
		return viewIndex_;
	}

	@Override
	public boolean canSaveView() {
		return true;
	}

	@Override
	public void saveView() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName(getViewName() + FileType.PNG.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File file = FileType.appendExtension(selectedFile, FileType.PNG);

		// take snapshot of the scene
		WritableImage snapshot = root_.snapshot(null, null);

		// create and start task in progress panel
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SaveImage(file, snapshot));
	}

	@Override
	public String getViewName() {

		// get selected files
		ObservableList<TreeItem<String>> selected = owner_.getOwner().getInputPanel().getSelectedFiles();

		// no file selected
		if ((selected == null) || selected.isEmpty())
			return "Info View";

		// get selected file
		SpectrumItem file = (SpectrumItem) selected.get(0);

		// no file selected
		if (file == null)
			return "Info View";

		// return name of file
		return file.getName();
	}

	@Override
	public WritableImage getViewImage() {
		return root_.snapshot(null, null);
	}

	/**
	 * Clears this view.
	 *
	 */
	public void clearView() {
		infoTable_.getRoot().getChildren().clear();
		showInfoView(false, null);
	}

	/**
	 * Shows text view.
	 *
	 * @param stfFile
	 *            STF file to show. This is used for building the bread crumb navigation.
	 *
	 */
	public void showSTFView(STFFile stfFile) {
		stfView_.getRoot().setVisible(true);
		stfView_.getRoot().setMouseTransparent(false);
		infoTable_.setVisible(false);
		infoTable_.setMouseTransparent(true);
		viewIndex_ = STF_VIEW;
		if (owner_ != null) {
			owner_.buildBreadCrumbNavigation(stfFile);
		}
	}

	/**
	 * Shows info view.
	 *
	 * @param showOutputFileButton
	 *            True to show output file button on the controls.
	 * @param item
	 *            Spectrum item to show the info for. This is used for building the bread crumb navigation.
	 */
	public void showInfoView(boolean showOutputFileButton, SpectrumItem item) {
		stfView_.getRoot().setVisible(false);
		stfView_.getRoot().setMouseTransparent(true);
		stfView_.stopAnimation();
		infoTable_.setVisible(true);
		infoTable_.setMouseTransparent(false);
		viewIndex_ = INFO_VIEW;
		controls_.showOutputFileButton(showOutputFileButton);
		if (owner_ != null) {
			owner_.buildBreadCrumbNavigation(item);
		}
	}

	/**
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot text view panel.
	 */
	public static InfoViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("InfoViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			InfoViewPanel controller = (InfoViewPanel) fxmlLoader.getController();

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
