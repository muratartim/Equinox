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
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.fileType.AircraftLinearEquivalentStress;
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.fileType.AircraftModel;
import equinox.data.fileType.AircraftPreffasEquivalentStress;
import equinox.data.fileType.DamageAngle;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalFlight;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.Flight;
import equinox.data.fileType.FlightDamageContributions;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.data.fileType.PilotPoint;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.Rfort;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.StressSequence;
import equinox.data.ui.FilterItem;
import equinox.data.ui.FilterListCell;
import equinox.font.IconicFont;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

/**
 * Class for filter panel controller.
 *
 * @author Murat Artim
 * @date Nov 18, 2014
 * @time 1:58:47 PM
 */
public class FilterPanel implements Initializable {

	/** The owner panel. */
	private FileViewPanel owner_;

	/** Pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	@FXML
	private VBox root_;

	@FXML
	private ListView<FilterItem> list_;

	@FXML
	private Button ok_;

	@FXML
	private Label info_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set cell factory to file list
		list_.setCellFactory(new Callback<ListView<FilterItem>, ListCell<FilterItem>>() {

			@Override
			public ListCell<FilterItem> call(ListView<FilterItem> param) {
				return new FilterListCell();
			}
		});

		// add selection listener to flight list
		list_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		list_.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<FilterItem>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends FilterItem> arg0) {

				// get selected filters
				ObservableList<FilterItem> selected = list_.getSelectionModel().getSelectedItems();

				// no selection
				if ((selected == null) || selected.isEmpty()) {
					info_.setText("No file type selected.");
					ok_.setDisable(true);
				}

				// there is selection
				else {
					info_.setText(selected.size() + " file type(s) selected.");
					ok_.setDisable(false);
				}
			}
		});

		// create filters
		list_.getItems().add(new FilterItem("Spectra", "\ueb8c", IconicFont.ICOMOON, Spectrum.class));
		list_.getItems().add(new FilterItem("STF Files", "\uf1c4", IconicFont.FONTAWESOME, STFFile.class));
		list_.getItems().add(new FilterItem("STF File Buckets", "\uf16c", IconicFont.FONTAWESOME, STFFileBucket.class));
		list_.getItems().add(new FilterItem("Damage Angles", "\ueab0", IconicFont.ICOMOON, DamageAngle.class));
		list_.getItems().add(new FilterItem("Loadcase Damage Contributions", "\uf200", IconicFont.FONTAWESOME, LoadcaseDamageContributions.class));
		list_.getItems().add(new FilterItem("Flight Damage Contributions", "\ueb7d", IconicFont.ICOMOON, FlightDamageContributions.class));
		list_.getItems().add(new FilterItem("Stress Sequences", "\ueb6d", IconicFont.ICOMOON, StressSequence.class));
		list_.getItems().add(new FilterItem("Typical Flights", "\uec04", IconicFont.ICOMOON, Flight.class));
		list_.getItems().add(new FilterItem("Fatigue Equivalent Stresses", "\uf10c", IconicFont.FONTAWESOME, FatigueEquivalentStress.class));
		list_.getItems().add(new FilterItem("Preffas Equivalent Stresses", "\uf192", IconicFont.FONTAWESOME, PreffasEquivalentStress.class));
		list_.getItems().add(new FilterItem("Linear Propagation Equivalent Stresses", "\uf111", IconicFont.FONTAWESOME, LinearEquivalentStress.class));
		list_.getItems().add(new FilterItem("Fast Fatigue Equivalent Stresses", "\uf10c", IconicFont.FONTAWESOME, FastFatigueEquivalentStress.class));
		list_.getItems().add(new FilterItem("Fast Preffas Equivalent Stresses", "\uf192", IconicFont.FONTAWESOME, FastPreffasEquivalentStress.class));
		list_.getItems().add(new FilterItem("Fast Linear Propagation Equivalent Stresses", "\uf111", IconicFont.FONTAWESOME, FastLinearEquivalentStress.class));
		list_.getItems().add(new FilterItem("External Stress Sequences", "\ueb6d", IconicFont.ICOMOON, ExternalStressSequence.class));
		list_.getItems().add(new FilterItem("External Typical Flights", "\uec04", IconicFont.ICOMOON, ExternalFlight.class));
		list_.getItems().add(new FilterItem("External Fatigue Equivalent Stresses", "\uf10c", IconicFont.FONTAWESOME, ExternalFatigueEquivalentStress.class));
		list_.getItems().add(new FilterItem("External Preffas Equivalent Stresses", "\uf192", IconicFont.FONTAWESOME, ExternalPreffasEquivalentStress.class));
		list_.getItems().add(new FilterItem("External Linear Propagation Equivalent Stresses", "\uf111", IconicFont.FONTAWESOME, ExternalLinearEquivalentStress.class));
		list_.getItems().add(new FilterItem("A/C Models", "\uec02", IconicFont.ICOMOON, AircraftModel.class));
		list_.getItems().add(new FilterItem("Load Cases", "\uebec", IconicFont.ICOMOON, AircraftLoadCase.class));
		list_.getItems().add(new FilterItem("Linked Pilot Points", "\uec96", IconicFont.ICOMOON, PilotPoint.class));
		list_.getItems().add(new FilterItem("A/C Fatigue Equivalent Stresses", "\uf10c", IconicFont.FONTAWESOME, AircraftFatigueEquivalentStress.class));
		list_.getItems().add(new FilterItem("A/C Preffas Equivalent Stresses", "\uf192", IconicFont.FONTAWESOME, AircraftPreffasEquivalentStress.class));
		list_.getItems().add(new FilterItem("A/C Linear Propagation Equivalent Stresses", "\uf111", IconicFont.FONTAWESOME, AircraftLinearEquivalentStress.class));
		list_.getItems().add(new FilterItem("RFORT Files", "\uf0b0", IconicFont.FONTAWESOME, Rfort.class));
	}

	/**
	 * Shows chat panel.
	 *
	 * @param node
	 *            Node to show this panel on.
	 */
	public void show(Node node) {

		// not shown
		if (!isShown_) {

			// clear selections
			list_.getSelectionModel().clearSelection();

			// create pop-over
			popOver_ = new PopOver();
			popOver_.setArrowLocation(ArrowLocation.TOP_CENTER);
			popOver_.setDetachable(false);
			popOver_.setContentNode(root_);
			popOver_.setHideOnEscape(true);
			popOver_.setAutoHide(true);

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

			// show
			popOver_.show(node);
		}
	}

	/**
	 * Returns true if this panel is shown.
	 *
	 * @return True if this panel is shown.
	 */
	public boolean isShown() {
		return isShown_;
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	@FXML
	private void onOkClicked() {

		// get selected filters
		ObservableList<FilterItem> selected = list_.getSelectionModel().getSelectedItems();

		// no selection
		if ((selected == null) || selected.isEmpty())
			return;

		// get file classes
		Class<?>[] fileClasses = new Class<?>[selected.size()];
		for (int i = 0; i < selected.size(); i++) {
			fileClasses[i] = selected.get(i).getFileClass();
		}

		// hide panel
		popOver_.hide();

		// filter
		owner_.filter(fileClasses);
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static FilterPanel load(FileViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("FilterPanel.fxml"));
			fxmlLoader.load();

			// get controller
			FilterPanel controller = (FilterPanel) fxmlLoader.getController();

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
