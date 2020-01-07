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

import equinox.controller.DataInsightsPanel.DataInsightsSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.ui.DataInsightType;
import equinox.dataServer.remote.message.GetFatigueMissionsForPilotPointsRequest;
import equinox.task.GetAircraftProgramsForSpectra;
import equinox.task.GetAircraftProgramsForSpectra.SpectrumAircraftProgramRequestingPanel;
import equinox.task.GetAircraftSectionsForSpectra;
import equinox.task.GetAircraftSectionsForSpectra.SpectrumAircraftSectionRequestingPanel;
import equinox.task.GetFatigueMissionsForSpectra;
import equinox.task.GetFatigueMissionsForSpectra.SpectrumFatigueMissionRequestingPanel;
import equinox.task.PlotSpectrumCount;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

/**
 * Class for data insights spectrum count panel controller.
 *
 * @author Murat Artim
 * @date 26 Jul 2017
 * @time 12:46:08
 *
 */
public class DataInsightsSpectrumCountPanel implements DataInsightsSubPanel, SpectrumAircraftProgramRequestingPanel, SpectrumAircraftSectionRequestingPanel, SpectrumFatigueMissionRequestingPanel {

	/** The owner panel. */
	private DataInsightsPanel owner_;

	@FXML
	private VBox root_, container_;

	@FXML
	private ComboBox<String> program_, section_, mission_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@Override
	public VBox getRoot() {
		return root_;
	}

	@Override
	public DataInsightType getType() {
		return DataInsightType.SPECTRUM_COUNT;
	}

	@Override
	public void showing() {

		// request all aircraft programs from global database
		if (program_.getItems().isEmpty()) {
			owner_.getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(new GetAircraftProgramsForSpectra(this));
		}
	}

	@Override
	public void setAircraftProgramsForSpectra(Collection<String> programs) {

		// reset existing list
		program_.getItems().setAll(programs);
		program_.getItems().add(0, GetAircraftProgramsForSpectra.ALL_PROGRAMS);

		// select all aircrafts
		program_.getSelectionModel().selectFirst();
	}

	@Override
	public void setAircraftSectionsForSpectra(Collection<String> sections) {

		// reset existing list
		section_.getItems().setAll(sections);
		section_.getItems().add(0, GetAircraftSectionsForSpectra.ALL_SECTIONS);

		// select all sections
		section_.getSelectionModel().selectFirst();
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
	public void setFatigueMissionsForSpectra(Collection<String> missions) {

		// reset existing list
		mission_.getItems().setAll(missions);
		mission_.getItems().add(0, GetFatigueMissionsForPilotPointsRequest.ALL_MISSIONS);

		// select all sections
		mission_.getSelectionModel().selectFirst();
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
		if (program_.getSelectionModel().getSelectedIndex() == 0 || program_.getSelectionModel().getSelectedIndex() == -1) {
			program = null;
		}
		String section = section_.getSelectionModel().getSelectedItem();
		if (section_.getSelectionModel().getSelectedIndex() == 0 || section_.getSelectionModel().getSelectedIndex() == -1) {
			section = null;
		}
		String mission = mission_.getSelectionModel().getSelectedItem();
		if (mission_.getSelectionModel().getSelectedIndex() == 0 || mission_.getSelectionModel().getSelectedIndex() == -1) {
			mission = null;
		}

		// plot
		owner_.getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(new PlotSpectrumCount(program, section, mission));
	}

	@FXML
	private void onHelpClicked() {
		// LATER on help clicked
	}

	@FXML
	private void onResetClicked() {

		// animate
		Animator.bouncingScale(20.0, 120.0, 1.0, 0.5, 0.0, (EventHandler<ActionEvent>) event -> {
			mission_.getItems().clear();
			if (container_.getChildren().contains(mission_)) {
				container_.getChildren().remove(mission_);
			}
			section_.getItems().clear();
			if (container_.getChildren().contains(section_)) {
				container_.getChildren().remove(section_);
			}
			program_.getSelectionModel().select(0);
		}, mission_, section_).play();
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

		// all aircraft programs
		if (program.equals(GetAircraftProgramsForSpectra.ALL_PROGRAMS)) {

			// animate
			Animator.bouncingScale(20.0, 120.0, 1.0, 0.5, 0.0, (EventHandler<ActionEvent>) event -> {
				section_.getItems().clear();
				if (container_.getChildren().contains(section_)) {
					container_.getChildren().remove(section_);
				}
				mission_.getItems().clear();
				if (container_.getChildren().contains(mission_)) {
					container_.getChildren().remove(mission_);
				}
			}, mission_, section_).play();
			return;
		}

		// get sections
		owner_.getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(new GetAircraftSectionsForSpectra(this, program));
	}

	@FXML
	private void onSectionSelected() {

		// no selection
		if (section_.getSelectionModel().isEmpty())
			return;

		// get selected program
		String section = section_.getSelectionModel().getSelectedItem();

		// nothing selected
		if (section == null)
			return;

		// all aircraft sections
		if (section.equals(GetAircraftSectionsForSpectra.ALL_SECTIONS)) {

			// animate
			Animator.bouncingScale(20.0, 120.0, 1.0, 0.5, 0.0, (EventHandler<ActionEvent>) event -> {
				mission_.getItems().clear();
				if (container_.getChildren().contains(mission_)) {
					container_.getChildren().remove(mission_);
				}
			}, mission_).play();
			return;
		}

		// get selected program
		String program = program_.getSelectionModel().getSelectedItem();

		// no program selected
		if (program == null)
			return;

		// get sections
		owner_.getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(new GetFatigueMissionsForSpectra(this, program, section));
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static DataInsightsSpectrumCountPanel load(DataInsightsPanel owner) {

		try {

			// load fxml file
			final FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DataInsightsSpectrumCountPanel.fxml"));
			final Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			final DataInsightsSpectrumCountPanel controller = (DataInsightsSpectrumCountPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;

			// remove mission and section combos
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
