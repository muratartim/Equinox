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
import java.util.Collection;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.DataInsightsPanel.DataInsightsSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.ui.DataInsightType;
import equinox.dataServer.remote.data.ContributionType;
import equinox.dataServer.remote.message.GetAircraftSectionsForPilotPointsRequest;
import equinox.dataServer.remote.message.GetFatigueMissionsForPilotPointsRequest;
import equinox.task.GetAircraftProgramsForPilotPoints;
import equinox.task.GetAircraftProgramsForPilotPoints.PilotPointAircraftProgramRequestingPanel;
import equinox.task.GetAircraftSectionsForPilotPoints;
import equinox.task.GetAircraftSectionsForPilotPoints.PilotPointAircraftSectionRequestingPanel;
import equinox.task.GetFatigueMissionsForPilotPoints;
import equinox.task.GetFatigueMissionsForPilotPoints.PilotPointFatigueMissionRequestingPanel;
import equinox.task.PlotContributionStatistics;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Class for data insights damage contribution panel controller.
 *
 * @author Murat Artim
 * @date 14 Aug 2017
 * @time 12:25:04
 *
 */
public class DataInsightsDamageContributionPanel implements DataInsightsSubPanel, PilotPointAircraftProgramRequestingPanel, PilotPointAircraftSectionRequestingPanel, PilotPointFatigueMissionRequestingPanel {

	/** The owner panel. */
	private DataInsightsPanel owner_;

	@FXML
	private VBox root_, container_;

	@FXML
	private ComboBox<String> program_, section_, mission_;

	@FXML
	private ComboBox<ContributionType> contribution_;

	@FXML
	private HBox limitContainer_;

	@FXML
	private Spinner<Integer> limit_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// add contribution types
		contribution_.getItems().setAll(ContributionType.values());
		contribution_.getSelectionModel().clearSelection();

		// bind contributions to section selection
		contribution_.disableProperty().bind(section_.getSelectionModel().selectedItemProperty().isNotEqualTo(GetAircraftSectionsForPilotPointsRequest.ALL_SECTIONS));

		// setup limit
		limit_.setValueFactory(new IntegerSpinnerValueFactory(1, 20, 3));
	}

	@Override
	public VBox getRoot() {
		return root_;
	}

	@Override
	public DataInsightType getType() {
		return DataInsightType.DAMAGE_CONTRIBUTIONS;
	}

	@Override
	public void showing() {

		// request all aircraft programs from global database
		if (program_.getItems().isEmpty()) {
			owner_.getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(new GetAircraftProgramsForPilotPoints(this));
		}
	}

	@Override
	public void setAircraftProgramsForPilotPoints(Collection<String> programs) {

		// clear selection
		program_.getSelectionModel().clearSelection();

		// reset existing list
		program_.getItems().setAll(programs);
	}

	@Override
	public void setAircraftSectionsForPilotPoints(Collection<String> sections) {

		// clear section selection
		section_.getSelectionModel().clearSelection();
		mission_.getSelectionModel().clearSelection();
		contribution_.getSelectionModel().clearSelection();

		// reset existing list
		section_.getItems().setAll(sections);
		section_.getItems().add(0, GetAircraftSectionsForPilotPointsRequest.ALL_SECTIONS);

		// remove contributions and missions (if shown)
		if (container_.getChildren().contains(mission_)) {

			// animate
			Animator.bouncingScale(20.0, 120.0, 1.0, 0.5, 0.0, (EventHandler<ActionEvent>) event -> {
				if (container_.getChildren().contains(limitContainer_)) {
					container_.getChildren().remove(limitContainer_);
				}
				contribution_.getSelectionModel().clearSelection();
				if (container_.getChildren().contains(contribution_)) {
					container_.getChildren().remove(contribution_);
				}
				mission_.getItems().clear();
				if (container_.getChildren().contains(mission_)) {
					container_.getChildren().remove(mission_);
				}
			}, limitContainer_, contribution_, mission_).play();
		}

		// add sections combo
		if (!container_.getChildren().contains(section_)) {

			// add section
			container_.getChildren().add(section_);

			// animate
			Animator.bouncingScale(20.0, 120.0, 0.0, 0.5, 1.0, (EventHandler<ActionEvent>) event -> {
				// no implementation
			}, section_).play();
		}
	}

	@Override
	public void setFatigueMissionsForPilotPoints(Collection<String> missions) {

		// clear mission selection
		mission_.getSelectionModel().clearSelection();
		contribution_.getSelectionModel().clearSelection();

		// reset existing list
		mission_.getItems().setAll(missions);
		mission_.getItems().add(0, GetFatigueMissionsForPilotPointsRequest.ALL_MISSIONS);

		// remove contributions (if shown)
		if (container_.getChildren().contains(contribution_)) {

			// animate
			Animator.bouncingScale(20.0, 120.0, 1.0, 0.5, 0.0, (EventHandler<ActionEvent>) event -> {
				if (container_.getChildren().contains(limitContainer_)) {
					container_.getChildren().remove(limitContainer_);
				}
				contribution_.getSelectionModel().clearSelection();
				if (container_.getChildren().contains(contribution_)) {
					container_.getChildren().remove(contribution_);
				}
			}, limitContainer_, contribution_).play();
		}

		// add missions combo
		if (!container_.getChildren().contains(mission_)) {

			// add mission
			container_.getChildren().add(mission_);

			// animate
			Animator.bouncingScale(20.0, 120.0, 0.0, 0.5, 1.0, (EventHandler<ActionEvent>) event -> {
				// no implementation
			}, mission_).play();
		}
	}

	@FXML
	private void onCancelClicked() {
		owner_.onCancelClicked();
	}

	@FXML
	private void onOkClicked() {

		// get inputs
		String program = program_.getSelectionModel().getSelectedItem();
		String section = section_.getSelectionModel().getSelectedItem();
		String mission = mission_.getSelectionModel().getSelectedItem();
		ContributionType contributionType = contribution_.getSelectionModel().getSelectedItem();
		int limit = limit_.getValue();

		// check inputs
		if (!checkInputs(program, section, mission, contributionType))
			return;

		// create task
		PlotContributionStatistics task = new PlotContributionStatistics(program, section, mission, contributionType);
		task.setIncrementLimit(limit);

		// submit
		owner_.getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(task);
	}

	@FXML
	private void onHelpClicked() {
		// LATER on help clicked
	}

	@FXML
	private void onResetClicked() {

		// animate
		Animator.bouncingScale(20.0, 120.0, 1.0, 0.5, 0.0, (EventHandler<ActionEvent>) event -> {
			if (container_.getChildren().contains(limitContainer_)) {
				container_.getChildren().remove(limitContainer_);
			}
			contribution_.getSelectionModel().clearSelection();
			if (container_.getChildren().contains(contribution_)) {
				container_.getChildren().remove(contribution_);
			}
			mission_.getItems().clear();
			if (container_.getChildren().contains(mission_)) {
				container_.getChildren().remove(mission_);
			}
			section_.getItems().clear();
			if (container_.getChildren().contains(section_)) {
				container_.getChildren().remove(section_);
			}
			program_.getSelectionModel().clearSelection();
		}, limitContainer_, contribution_, mission_, section_).play();
	}

	@FXML
	private void onProgramSelected() {

		// no selection
		if (program_.getSelectionModel().isEmpty())
			return;

		// get selected program
		String program = program_.getSelectionModel().getSelectedItem();

		// nothing selected
		if (program == null)
			return;

		// get sections
		owner_.getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(new GetAircraftSectionsForPilotPoints(this, program));
	}

	@FXML
	private void onSectionSelected() {

		// no selection
		if (section_.getSelectionModel().isEmpty())
			return;

		// get selected program and section
		String section = section_.getSelectionModel().getSelectedItem();
		String program = program_.getSelectionModel().getSelectedItem();

		// nothing selected
		if (section == null || program == null)
			return;

		// get missions
		owner_.getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(new GetFatigueMissionsForPilotPoints(this, program, section));
	}

	@FXML
	private void onMissionSelected() {

		// no selection
		if (mission_.getSelectionModel().isEmpty())
			return;

		// clear contributions selection
		contribution_.getSelectionModel().clearSelection();

		// add contributions combo
		if (!container_.getChildren().contains(contribution_)) {

			// add contributions
			container_.getChildren().add(contribution_);

			// animate
			Animator.bouncingScale(20.0, 120.0, 0.0, 0.5, 1.0, (EventHandler<ActionEvent>) event -> {
				// no implementation
			}, contribution_).play();
		}

		// animate
		Animator.bouncingScale(20.0, 120.0, 1.0, 0.5, 0.0, (EventHandler<ActionEvent>) event -> {
			if (container_.getChildren().contains(limitContainer_)) {
				container_.getChildren().remove(limitContainer_);
			}
		}, limitContainer_).play();
	}

	@FXML
	private void onContributionTypeSelected() {

		// no selection
		if (contribution_.getSelectionModel().isEmpty())
			return;

		// not all sections
		if (!section_.getSelectionModel().getSelectedItem().equals(GetAircraftSectionsForPilotPointsRequest.ALL_SECTIONS))
			return;

		// increment
		if (contribution_.getSelectionModel().getSelectedItem().equals(ContributionType.INCREMENT)) {

			// add contributions combo
			if (!container_.getChildren().contains(limitContainer_)) {

				// add contributions
				container_.getChildren().add(limitContainer_);

				// animate
				Animator.bouncingScale(20.0, 120.0, 0.0, 0.5, 1.0, (EventHandler<ActionEvent>) event -> {
					// no implementation
				}, limitContainer_).play();
			}
		}

		// steady
		else {

			// animate
			Animator.bouncingScale(20.0, 120.0, 1.0, 0.5, 0.0, (EventHandler<ActionEvent>) event -> {
				if (container_.getChildren().contains(limitContainer_)) {
					container_.getChildren().remove(limitContainer_);
				}
			}, limitContainer_).play();
		}
	}

	/**
	 * Checks inputs.
	 *
	 * @param program
	 *            Aircraft program.
	 * @param section
	 *            Aircraft section.
	 * @param mission
	 *            Fatigue mission.
	 * @param contributionType
	 *            Contribution type.
	 * @return True if all checks passed.
	 */
	private boolean checkInputs(String program, String section, String mission, ContributionType contributionType) {

		// no program selected
		if (program == null) {
			String message = "Please select an aircraft program to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(program_);
			return false;
		}

		// no section selected
		if (section == null) {
			String message = "Please select an aircraft section to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(section_);
			return false;
		}

		// no mission selected
		if (mission == null) {
			String message = "Please select a fatigue mission to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(mission_);
			return false;
		}

		// no contribution type selected (and all sections are selected)
		if (contributionType == null && section.equals(GetAircraftSectionsForPilotPointsRequest.ALL_SECTIONS)) {
			String message = "Please select a contribution type to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(contribution_);
			return false;
		}

		// all checks passed
		return true;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static DataInsightsDamageContributionPanel load(DataInsightsPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DataInsightsDamageContributionPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			DataInsightsDamageContributionPanel controller = (DataInsightsDamageContributionPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;

			// remove combos
			if (controller.container_.getChildren().contains(controller.limitContainer_)) {
				controller.container_.getChildren().remove(controller.limitContainer_);
			}
			if (controller.container_.getChildren().contains(controller.contribution_)) {
				controller.container_.getChildren().remove(controller.contribution_);
			}
			controller.mission_.getItems().clear();
			if (controller.container_.getChildren().contains(controller.mission_)) {
				controller.container_.getChildren().remove(controller.mission_);
			}
			controller.section_.getItems().clear();
			if (controller.container_.getChildren().contains(controller.section_)) {
				controller.container_.getChildren().remove(controller.section_);
			}

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}
