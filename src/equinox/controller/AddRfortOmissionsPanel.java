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

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.controller.RfortAddOmissionsPopup.RfortDirectOmissionAddingPanel;
import equinox.controller.RfortAddPercentOmissionsPopup.RfortPercentOmissionAddingPanel;
import equinox.data.AnalysisEngine;
import equinox.data.EquinoxTheme;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.Settings;
import equinox.data.fileType.Rfort;
import equinox.data.ui.RfortDirectOmission;
import equinox.data.ui.RfortOmission;
import equinox.font.IconicFont;
import equinox.task.AddRfortOmissions;
import equinox.task.GetRfortOmissions;
import equinox.task.GetRfortOmissions.RfortOmissionsRequestingPanel;
import equinox.task.GetRfortPilotPoints;
import equinox.task.GetRfortPilotPoints.RfortPilotPointsRequestingPanel;
import equinox.utility.Utility;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.VBox;

/**
 * Class for add RFORT omissions panel controller.
 *
 * @author Murat Artim
 * @date Mar 11, 2016
 * @time 4:22:38 PM
 */
public class AddRfortOmissionsPanel implements InternalInputSubPanel, RfortOmissionsRequestingPanel, RfortPilotPointsRequestingPanel, RfortPercentOmissionAddingPanel, RfortDirectOmissionAddingPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Add percent omissions popup. */
	private RfortAddPercentOmissionsPopup addPercentOmissionsPopup_;

	/** Add direct omissions popup. */
	private RfortAddOmissionsPopup addDirectOmissionsPopup_;

	/** Pilot point names. */
	private ArrayList<String> ppNames_ = new ArrayList<>();

	@FXML
	private VBox root_;

	@FXML
	private ListView<RfortOmission> omissions_;

	@FXML
	private Button removeOmissions_, resetOmissions_;

	@FXML
	private MenuButton addOmissions_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// load popups
		addPercentOmissionsPopup_ = RfortAddPercentOmissionsPopup.load(this);
		addDirectOmissionsPopup_ = RfortAddOmissionsPopup.load(this);

		// setup omission levels list
		omissions_.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<RfortOmission>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends RfortOmission> c) {

				// get selected omissions
				ObservableList<RfortOmission> selected = omissions_.getSelectionModel().getSelectedItems();

				// list empty
				if (selected.isEmpty()) {
					removeOmissions_.setDisable(true);
					return;
				}

				// check if any uneditable omission selected
				for (RfortOmission omission : selected) {
					if (!omission.canBeEdited()) {
						removeOmissions_.setDisable(true);
						return;
					}
				}

				// enable
				removeOmissions_.setDisable(false);
			}
		});
		omissions_.getItems().addListener(new ListChangeListener<RfortOmission>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends RfortOmission> c) {
				resetOmissions_.setDisable(omissions_.getItems().isEmpty());
			}
		});
		omissions_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		omissions_.setPlaceholder(new Label("No omission added."));
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
	public void start() {
		// no implementation
	}

	@Override
	public String getHeader() {
		return "Add RFORT Omissions";
	}

	@Override
	public void showing() {
		onResetClicked();
	}

	@Override
	public void setOmissions(ArrayList<String> omissions) {

		// reset omissions
		omissions_.getSelectionModel().clearSelection();
		omissions_.getItems().clear();

		// create dummy omissions
		for (String omissionName : omissions) {
			RfortDirectOmission omission = new RfortDirectOmission(omissionName);
			omission.setCanBeEdited(false);
			omissions_.getItems().add(omission);
		}
	}

	@Override
	public void setPilotPoints(ArrayList<String> ppNames) {
		ppNames_ = ppNames;
	}

	@Override
	public void addOmissions(RfortOmission... omissions) {

		// get current omissions
		ObservableList<RfortOmission> current = omissions_.getItems();

		// loop over omissions
		for (RfortOmission omission : omissions) {

			// omission already exists
			if (current.contains(omission)) {
				String message = "Omission '" + omission.toString() + "' already exists. ";
				message += "Please supply distinct omissions to add.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(omissions_);
				return;
			}
		}

		// add omissions
		for (RfortOmission omission : omissions) {
			current.add(omission);
		}
	}

	@FXML
	private void onPercentageOmissionsClicked() {
		addPercentOmissionsPopup_.show(addOmissions_);
	}

	@FXML
	private void onOmissionValuesClicked() {
		addDirectOmissionsPopup_.show(ppNames_, addOmissions_);
	}

	@FXML
	private void onRemoveOmissionsClicked() {
		omissions_.getItems().removeAll(omissions_.getSelectionModel().getSelectedItems());
	}

	@FXML
	private void onResetOmissionsClicked() {
		ArrayList<RfortOmission> toBeRemoved = new ArrayList<>();
		for (RfortOmission omission : omissions_.getItems()) {
			if (omission.canBeEdited()) {
				toBeRemoved.add(omission);
			}
		}
		omissions_.getItems().removeAll(toBeRemoved);
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to add RFORT omissions", null);
	}

	@FXML
	private void onOkClicked() {

		// check omission levels
		ArrayList<RfortOmission> toBeAdded = new ArrayList<>();
		for (RfortOmission omission : omissions_.getItems()) {
			if (omission.canBeEdited()) {
				toBeAdded.add(omission);
			}
		}

		// no new omission defined
		if (toBeAdded.isEmpty()) {
			String message = "Please add at least 1 omission to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(omissions_);
			return;
		}

		// get selected RFORT file
		Rfort rfort = (Rfort) owner_.getSelectedFiles().get(0);

		// get analysis engine
		AnalysisEngine engine = (AnalysisEngine) owner_.getOwner().getSettings().getValue(Settings.ANALYSIS_ENGINE);
		IsamiVersion isamiVersion = (IsamiVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_VERSION);
		IsamiSubVersion isamiSubVersion = (IsamiSubVersion) owner_.getOwner().getSettings().getValue(Settings.ISAMI_SUB_VERSION);
		boolean applyCompression = (boolean) owner_.getOwner().getSettings().getValue(Settings.APPLY_COMPRESSION);

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new AddRfortOmissions(rfort, toBeAdded, engine).setIsamiEngineInputs(isamiVersion, isamiSubVersion, applyCompression));

		// get back to files view
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {

		// get selected RFORT file
		Rfort rfort = (Rfort) owner_.getSelectedFiles().get(0);

		// get RFORT omissions
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetRfortOmissions(rfort, this));
		tm.runTaskInParallel(new GetRfortPilotPoints(rfort, this, true));
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static AddRfortOmissionsPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("AddRfortOmissionsPanel.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			AddRfortOmissionsPanel controller = (AddRfortOmissionsPanel) fxmlLoader.getController();

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
