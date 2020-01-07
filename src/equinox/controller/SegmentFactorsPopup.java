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

import control.validationField.DoubleValidationField;
import equinox.controller.InputPanel.InputPopup;
import equinox.data.EquinoxTheme;
import equinox.data.Segment;
import equinox.data.SegmentFactor;
import equinox.data.fileType.Spectrum;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.task.GetSegments;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;

/**
 * Class for segment factors panel.
 *
 * @author Murat Artim
 * @date Dec 15, 2015
 * @time 9:43:50 AM
 */
public class SegmentFactorsPopup implements InputPopup {

	/** The owner panel. */
	private InputPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	/** Requesting panel. */
	private SegmentFactorAddingPanel panel_;

	/** Segment list. */
	private final ObservableList<Segment> segments_ = FXCollections.observableArrayList();

	@FXML
	private VBox root_;

	@FXML
	private TextField search_;

	@FXML
	private Button cancelSearch_, ok_;

	@FXML
	private DoubleValidationField onegSegmentFactor_, incSegmentFactor_, dpSegmentFactor_, dtSegmentFactor_;

	@FXML
	private MenuButton onegSegmentMethod_, incSegmentMethod_, dpSegmentMethod_, dtSegmentMethod_;

	@FXML
	private ListView<Segment> segmentList_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup validation fields
		onegSegmentFactor_.setDefaultValue(null);
		incSegmentFactor_.setDefaultValue(null);
		dpSegmentFactor_.setDefaultValue(null);
		dtSegmentFactor_.setDefaultValue(null);

		// setup segment list
		segmentList_.setItems(segments_);
		segmentList_.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		segmentList_.setPlaceholder(NoResultsPanel.load("Your search did not match any segment.", null));

		// setup search field
		search_.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> ov, String old_Val, String new_val) {
				search(old_Val, new_val);
			}
		});
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	/**
	 * Sets segments to this panel.
	 *
	 * @param segments
	 *            Flight segments.
	 */
	public void setSegments(ArrayList<Segment> segments) {

		// set table items
		segments_.clear();
		for (Segment segment : segments) {
			segments_.add(segment);
		}
		segmentList_.setItems(segments_);

		// reset
		search_.clear();
		segmentList_.getSelectionModel().clearSelection();
		onegSegmentFactor_.reset();
		incSegmentFactor_.reset();
		dpSegmentFactor_.reset();
		dtSegmentFactor_.reset();
		onegSegmentMethod_.setText(GenerateStressSequenceInput.MULTIPLY);
		incSegmentMethod_.setText(GenerateStressSequenceInput.MULTIPLY);
		dpSegmentMethod_.setText(GenerateStressSequenceInput.MULTIPLY);
		dtSegmentMethod_.setText(GenerateStressSequenceInput.MULTIPLY);
		for (MenuItem item : onegSegmentMethod_.getItems()) {
			RadioMenuItem radio = (RadioMenuItem) item;
			if (radio.getText().equals(GenerateStressSequenceInput.MULTIPLY)) {
				radio.setSelected(true);
				break;
			}
		}
		for (MenuItem item : incSegmentMethod_.getItems()) {
			RadioMenuItem radio = (RadioMenuItem) item;
			if (radio.getText().equals(GenerateStressSequenceInput.MULTIPLY)) {
				radio.setSelected(true);
				break;
			}
		}
		for (MenuItem item : dpSegmentMethod_.getItems()) {
			RadioMenuItem radio = (RadioMenuItem) item;
			if (radio.getText().equals(GenerateStressSequenceInput.MULTIPLY)) {
				radio.setSelected(true);
				break;
			}
		}
		for (MenuItem item : dtSegmentMethod_.getItems()) {
			RadioMenuItem radio = (RadioMenuItem) item;
			if (radio.getText().equals(GenerateStressSequenceInput.MULTIPLY)) {
				radio.setSelected(true);
				break;
			}
		}

		// show popup
		popOver_.show(panel_.getSegmentFactorPopupNode());

		// focus on search
		search_.requestFocus();
	}

	/**
	 * Shows this panel.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param spectrum
	 *            Selected spectrum.
	 */
	public void show(SegmentFactorAddingPanel panel, Spectrum spectrum) {

		// already shown
		if (isShown_)
			return;

		// set panel
		panel_ = panel;

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

		// request segments
		ActiveTasksPanel tm = owner_.getOwner().getActiveTasksPanel();
		tm.runTaskInParallel(new GetSegments(this, spectrum));
	}

	@FXML
	private void onOkClicked() {

		// get selected segments
		ObservableList<Segment> selected = segmentList_.getSelectionModel().getSelectedItems();

		// check inputs
		if (!checkInputs(selected))
			return;

		// get modifier values and methods
		double onegFactor = Double.parseDouble(onegSegmentFactor_.getText());
		String onegMethod = null;
		for (MenuItem item : onegSegmentMethod_.getItems()) {
			RadioMenuItem radio = (RadioMenuItem) item;
			if (radio.isSelected()) {
				onegMethod = radio.getText();
				break;
			}
		}
		double incFactor = Double.parseDouble(incSegmentFactor_.getText());
		String incMethod = null;
		for (MenuItem item : incSegmentMethod_.getItems()) {
			RadioMenuItem radio = (RadioMenuItem) item;
			if (radio.isSelected()) {
				incMethod = radio.getText();
				break;
			}
		}
		double dpFactor = Double.parseDouble(dpSegmentFactor_.getText());
		String dpMethod = null;
		for (MenuItem item : dpSegmentMethod_.getItems()) {
			RadioMenuItem radio = (RadioMenuItem) item;
			if (radio.isSelected()) {
				dpMethod = radio.getText();
				break;
			}
		}
		double dtFactor = Double.parseDouble(dtSegmentFactor_.getText());
		String dtMethod = null;
		for (MenuItem item : dtSegmentMethod_.getItems()) {
			RadioMenuItem radio = (RadioMenuItem) item;
			if (radio.isSelected()) {
				dtMethod = radio.getText();
				break;
			}
		}

		// create segment factors
		SegmentFactor[] segmentFactors = new SegmentFactor[selected.size()];
		for (int i = 0; i < selected.size(); i++) {
			segmentFactors[i] = new SegmentFactor(selected.get(i));
			segmentFactors[i].setModifier(onegMethod, onegFactor, GenerateStressSequenceInput.ONEG);
			segmentFactors[i].setModifier(incMethod, incFactor, GenerateStressSequenceInput.INCREMENT);
			segmentFactors[i].setModifier(dpMethod, dpFactor, GenerateStressSequenceInput.DELTAP);
			segmentFactors[i].setModifier(dtMethod, dtFactor, GenerateStressSequenceInput.DELTAT);
		}

		// add segment factors to requesting panel
		panel_.addSegmentFactors(segmentFactors);

		// hide
		popOver_.hide();
	}

	/**
	 * Checks inputs.
	 *
	 * @param selectedSegments
	 *            Selected segments.
	 * @return True if the inputs are valid.
	 */
	private boolean checkInputs(ObservableList<Segment> selectedSegments) {

		// no segment selected
		if (selectedSegments.isEmpty()) {
			String message = "No segment selected. Please select at least 1 segment to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(segmentList_);
			return false;
		}

		// check 1g segment factor
		String message = onegSegmentFactor_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(onegSegmentFactor_);
			return false;
		}

		// check increment segment factor
		message = incSegmentFactor_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(incSegmentFactor_);
			return false;
		}

		// check DP segment factor
		message = dpSegmentFactor_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(dpSegmentFactor_);
			return false;
		}

		// check DT segment factor
		message = dtSegmentFactor_.validate();
		if (message != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(dtSegmentFactor_);
			return false;
		}

		// valid inputs
		return true;
	}

	@FXML
	private void onCancelSearchClicked() {
		search_.clear();
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
		segmentList_.getSelectionModel().clearSelection();
		if ((old_Val != null) && (new_Val.length() < old_Val.length())) {
			segmentList_.setItems(segments_);
		}
		String value = new_Val.toUpperCase();
		ObservableList<Segment> subentries = FXCollections.observableArrayList();
		for (Segment item : segmentList_.getItems()) {
			if (item.toString().toUpperCase().contains(value)) {
				subentries.add(item);
			}
		}
		segmentList_.setItems(subentries);
		cancelSearch_.setVisible(!new_Val.isEmpty());
		if (new_Val.isEmpty()) {
			onCancelSearchClicked();
			return;
		}
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static SegmentFactorsPopup load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("SegmentFactorsPopup.fxml"));
			fxmlLoader.load();

			// get controller
			SegmentFactorsPopup controller = (SegmentFactorsPopup) fxmlLoader.getController();

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
	 * Interface for segment factor adding panels.
	 *
	 * @author Murat Artim
	 * @date Dec 12, 2015
	 * @time 2:56:37 PM
	 */
	public interface SegmentFactorAddingPanel {

		/**
		 * Adds segment factors.
		 *
		 * @param segmentFactors
		 *            Segment factors.
		 */
		void addSegmentFactors(SegmentFactor[] segmentFactors);

		/**
		 * Returns segment factor popup node.
		 *
		 * @return Segment factor popup node.
		 */
		Node getSegmentFactorPopupNode();
	}
}
