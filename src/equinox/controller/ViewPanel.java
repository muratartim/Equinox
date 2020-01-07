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

import org.controlsfx.control.BreadCrumbBar;

import equinox.Equinox;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.SpectrumItem;
import equinox.plugin.ViewSubPanel;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/**
 * Class for plot panel.
 *
 * @author Murat Artim
 * @date Dec 31, 2013
 * @time 5:50:03 PM
 */
public class ViewPanel implements Initializable {

	/** Sub-panel index. */
	public static final int INFO_VIEW = 0, PLOT_VIEW = 1, STATS_VIEW = 2, DOWNLOAD_VIEW = 3, WEB_VIEW = 4, ROADMAP_VIEW = 5, LEVEL_CROSSING_VIEW = 6, BUG_REPORT_VIEW = 7, COMPARE_FLIGHTS_VIEW = 8, IMAGE_VIEW = 9, MISSION_PARAMETERS_VIEW = 10, MISSION_PROFILE_VIEW = 11, EXTERNAL_PLOT_VIEW = 12,
			PLUGIN_VIEW = 13, DAMAGE_CONTRIBUTION_VIEW = 14, COMPARE_DAMAGE_CONTRIBUTIONS_VIEW = 15, OBJECT_VIEW = 16, MISSION_PROFILE_COMPARISON_VIEW = 17, TIME_STATS_VIEW = 18, ACCESS_REQUEST_VIEW = 19, HEALTH_MONITOR_VIEW = 20, EXECUTION_TREE_VIEW_PANEL = 21;

	/** The main screen of the application. */
	private MainScreen owner_;

	/** Pagination control. */
	private Pagination pagination_;

	/** Sub panel controllers. */
	private HashMap<Integer, ViewSubPanel> subPanels_;

	/** Share view panel. */
	private ShareViewPanel shareViewPanel_;

	@FXML
	private VBox root_;

	@FXML
	private HBox controlPanel_;

	@FXML
	private Button saveAsButton_, shareButton_;

	@FXML
	private ToolBar toolbar_;

	@FXML
	private BreadCrumbBar<SpectrumItem> navigation_;

	@FXML
	private Label header_;

	@FXML
	private StackPane stack_;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		// load sub panels
		subPanels_ = new HashMap<>();
		subPanels_.put(INFO_VIEW, InfoViewPanel.load(this));
		subPanels_.put(PLOT_VIEW, PlotViewPanel.load(this));
		subPanels_.put(DOWNLOAD_VIEW, DownloadViewPanel.load(this));
		subPanels_.put(WEB_VIEW, WebViewPanel.load(this));
		subPanels_.put(ROADMAP_VIEW, RoadmapViewPanel.load(this));
		subPanels_.put(LEVEL_CROSSING_VIEW, LevelCrossingViewPanel.load(this));
		subPanels_.put(BUG_REPORT_VIEW, BugReportViewPanel.load(this));
		subPanels_.put(COMPARE_FLIGHTS_VIEW, CompareFlightsViewPanel.load(this));
		subPanels_.put(IMAGE_VIEW, ImageViewPanel.load(this));
		subPanels_.put(MISSION_PARAMETERS_VIEW, MissionParameterPlotViewPanel.load(this));
		subPanels_.put(MISSION_PROFILE_VIEW, MissionProfileViewPanel.load(this));
		subPanels_.put(EXTERNAL_PLOT_VIEW, ExternalPlotViewPanel.load(this));
		subPanels_.put(PLUGIN_VIEW, PluginViewPanel.load(this));
		subPanels_.put(DAMAGE_CONTRIBUTION_VIEW, DamageContributionViewPanel.load(this));
		subPanels_.put(COMPARE_DAMAGE_CONTRIBUTIONS_VIEW, CompareDamageContributionsViewPanel.load(this));
		subPanels_.put(STATS_VIEW, StatisticsViewPanel.load(this));
		subPanels_.put(OBJECT_VIEW, ObjectViewPanel.load(this));
		subPanels_.put(MISSION_PROFILE_COMPARISON_VIEW, MissionProfileComparisonViewPanel.load(this));
		subPanels_.put(TIME_STATS_VIEW, TimeStatisticsViewPanel.load(this));
		subPanels_.put(ACCESS_REQUEST_VIEW, AccessRequestViewPanel.load(this));
		subPanels_.put(HEALTH_MONITOR_VIEW, HealthMonitorViewPanel.load(this));
		subPanels_.put(EXECUTION_TREE_VIEW_PANEL, TaskExecutionPlanViewPanel.load(this));

		// create pagination control
		pagination_ = new Pagination(subPanels_.size(), INFO_VIEW);
		pagination_.getStylesheets().add(Equinox.class.getResource("css/HiddenPagination.css").toString());
		VBox.setVgrow(pagination_, Priority.ALWAYS);

		// setup pagination page factory
		pagination_.setPageFactory(pageIndex -> {
			subPanels_.get(pageIndex).showing();
			return subPanels_.get(pageIndex).getRoot();
		});

		// add pagination to root container
		root_.getChildren().add(pagination_);

		navigation_.setOnCrumbAction(bae -> {
			SpectrumItem item = bae.getSelectedCrumb().getValue();
			if (item != null) {
				FileViewPanel panel = (FileViewPanel) owner_.getInputPanel().getSubPanel(InputPanel.FILE_VIEW_PANEL);
				panel.setupFileView(item);
			}
		});

		// setup bread crumb navigation
		Callback<TreeItem<SpectrumItem>, Button> bcFactory = navigation_.getCrumbFactory();
		navigation_.setCrumbFactory(p -> {
			SpectrumItem item = p.getValue();
			Label gr = (Label) item.getGraphic();
			Label iconLabel = new Label(gr.getText());
			iconLabel.getStylesheets().add(item.getIconFont().getWhiteStyleSheet());
			Button b = bcFactory.call(p);
			String name = item.getName();
			b.setText(name.length() <= 10 ? name : name.substring(0, 7) + "...");
			b.setGraphic(iconLabel);

			return b;
		});

		// show text view
		showSubPanel(INFO_VIEW);

		// create share view panel
		shareViewPanel_ = ShareViewPanel.load(this);
	}

	/**
	 * Starts this panel.
	 *
	 */
	public void start() {

		// set minimum tool bar size
		toolbar_.minWidthProperty().bind(stack_.widthProperty().add(controlPanel_.minWidthProperty()).add(saveAsButton_.widthProperty()).add(shareButton_.widthProperty()).add(2 * 11.0 + 3 * 4.0));

		// bind region width to tool bar width
		controlPanel_.prefWidthProperty().bind(toolbar_.widthProperty().subtract(stack_.widthProperty()).subtract(saveAsButton_.widthProperty()).subtract(shareButton_.widthProperty()).subtract(2 * 11.0 + 3 * 4.0));

		// start sub panels
		for (ViewSubPanel panel : subPanels_.values()) {
			panel.start();
		}

		// set initial position
		root_.setTranslateX(root_.getWidth());
	}

	/**
	 * Returns the main screen of the application.
	 *
	 * @return The main screen of the application.
	 */
	public MainScreen getOwner() {
		return owner_;
	}

	/**
	 * Returns the root of this controller.
	 *
	 * @return The root of this controller.
	 */
	public VBox getRoot() {
		return root_;
	}

	/**
	 * Returns the demanded sub panel of this panel.
	 *
	 * @param index
	 *            Index of the demanded sub panel.
	 * @return The demanded sub panel of this panel.
	 */
	public ViewSubPanel getSubPanel(int index) {
		return subPanels_.get(index);
	}

	/**
	 * Returns the current sub panel index.
	 *
	 * @return The current sub panel index.
	 */
	public int getCurrentSubPanelIndex() {
		return pagination_.getCurrentPageIndex();
	}

	/**
	 * Returns the share button.
	 *
	 * @return The share button.
	 */
	public Button getShareButton() {
		return shareButton_;
	}

	/**
	 * Returns share view panel.
	 *
	 * @return Share view panel.
	 */
	public ShareViewPanel getShareViewPanel() {
		return shareViewPanel_;
	}

	/**
	 * Returns the pagination of the view panel.
	 *
	 * @return The pagination of the view panel.
	 */
	public Pagination getPagination() {
		return pagination_;
	}

	/**
	 * Shows demanded sub panel.
	 *
	 * @param index
	 *            Index of demanded sub panel.
	 */
	public void showSubPanel(int index) {
		if (getCurrentSubPanelIndex() != index) {
			subPanels_.get(getCurrentSubPanelIndex()).hiding();
		}
		controlPanel_.getChildren().clear();
		header_.setText(subPanels_.get(index).getHeader());
		saveAsButton_.setDisable(!subPanels_.get(index).canSaveView());
		shareButton_.setDisable(!subPanels_.get(index).canSaveView());
		pagination_.setCurrentPageIndex(index);
		HBox controls = subPanels_.get(index).getControls();
		if (controls != null) {
			controlPanel_.getChildren().addAll(controls);
			controlPanel_.minWidthProperty().set(controls.getMinWidth());
		}
		else {
			controlPanel_.minWidthProperty().set(0);
		}
		if (index == INFO_VIEW) {
			header_.setVisible(false);
			navigation_.setVisible(true);
		}
		else {
			header_.setVisible(true);
			navigation_.setVisible(false);
		}
	}

	/**
	 * Adds sub panel.
	 *
	 * @param viewPanel
	 *            View sub panel.
	 * @return Index of added sub panel.
	 */
	public int addViewPanel(ViewSubPanel viewPanel) {
		if (viewPanel == null)
			return -1;
		int index = pagination_.getPageCount();
		subPanels_.put(index, viewPanel);
		pagination_.setPageCount(index + 1);
		return index;
	}

	/**
	 * Removes view panel with the given index.
	 *
	 * @param index
	 *            Index of the view panel to be removed.
	 */
	public void removeViewPanel(int index) {
		subPanels_.remove(index);
	}

	@FXML
	public void onSaveAsClicked() {
		getSubPanel(pagination_.getCurrentPageIndex()).saveView();
	}

	@FXML
	public void onShareClicked() {
		shareViewPanel_.show();
	}

	/**
	 * Builds bread crumb navigation from given spectrum item file.
	 *
	 * @param file
	 *            Spectrum item file.
	 */
	public void buildBreadCrumbNavigation(SpectrumItem file) {

		// null file
		if (file == null) {
			header_.setText("File View");
			header_.setVisible(true);
			navigation_.setVisible(false);
			return;
		}
		header_.setVisible(false);
		navigation_.setVisible(true);

		// get all parents
		ArrayList<SpectrumItem> list = getTreeItems(file, null);

		// build bread crumb model
		TreeItem<SpectrumItem> model = BreadCrumbBar.buildTreeModel(list.toArray(new SpectrumItem[list.size()]));
		navigation_.setSelectedCrumb(model);
	}

	/**
	 * Recursively collects all parents of the given file.
	 *
	 * @param file
	 *            File to collect parents for.
	 * @param list
	 *            List containing all parents. Null should be given.
	 * @return List containing all parents.
	 */
	private static ArrayList<SpectrumItem> getTreeItems(SpectrumItem file, ArrayList<SpectrumItem> list) {

		// create list (if null)
		if (list == null) {
			list = new ArrayList<>();
		}

		// get parent item
		SpectrumItem parent = file.getParentItem();

		// get parent's parent
		if (parent != null) {
			getTreeItems(parent, list);
		}

		// add file to list
		list.add(file);

		// return list
		return list;
	}

	/**
	 * Loads and returns plot panel.
	 *
	 * @param owner
	 *            Main screen.
	 * @return The newly loaded plot panel.
	 */
	public static ViewPanel load(MainScreen owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			ViewPanel controller = (ViewPanel) fxmlLoader.getController();

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

	/**
	 * Interface for internal view sub panel.
	 *
	 * @author Murat Artim
	 * @date Dec 27, 2013
	 * @time 11:22:20 PM
	 */
	public interface InternalViewSubPanel extends ViewSubPanel, Initializable {

		/**
		 * Returns the owner panel of this sub panel.
		 *
		 * @return The owner panel of this sub panel.
		 */
		ViewPanel getOwner();
	}
}
