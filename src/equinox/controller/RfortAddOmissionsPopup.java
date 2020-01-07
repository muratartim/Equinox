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

import equinox.data.EquinoxTheme;
import equinox.data.ui.RfortDirectOmission;
import equinox.data.ui.RfortOmission;
import equinox.data.ui.RfortPilotPointOmission;
import equinox.font.IconicFont;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;

/**
 * Class for add RFORT omissions popup controller.
 *
 * @author Murat Artim
 * @date Apr 15, 2016
 * @time 2:57:20 PM
 */
public class RfortAddOmissionsPopup implements Initializable {

	/** The owner panel. */
	private RfortDirectOmissionAddingPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	@FXML
	private VBox root_;

	@FXML
	private TextField name_;

	@FXML
	private Button ok_;

	@FXML
	private TableView<RfortPilotPointOmission> table_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// initialize mission parameters table
		table_.setEditable(true);
		TableColumn<RfortPilotPointOmission, String> nameCol = new TableColumn<>("Pilot Point");
		nameCol.setCellValueFactory(new PropertyValueFactory<RfortPilotPointOmission, String>("name"));
		TableColumn<RfortPilotPointOmission, String> omissionCol = new TableColumn<>("Omission Value");
		omissionCol.setCellValueFactory(new PropertyValueFactory<RfortPilotPointOmission, String>("omission"));
		omissionCol.setCellFactory(TextFieldTableCell.<RfortPilotPointOmission>forTableColumn());
		omissionCol.setOnEditCommit((CellEditEvent<RfortPilotPointOmission, String> t) -> {
			t.getTableView().getItems().get(t.getTablePosition().getRow()).setOmission(t.getNewValue());
		});
		table_.getColumns().add(nameCol);
		table_.getColumns().add(omissionCol);
		nameCol.prefWidthProperty().bind(table_.widthProperty().divide(3).multiply(2));
		omissionCol.prefWidthProperty().bind(table_.widthProperty().divide(3).subtract(5));
		table_.setPlaceholder(new Label("No pilot point defined."));
	}

	/**
	 * Returns the owner panel.
	 *
	 * @return The owner panel.
	 */
	public RfortDirectOmissionAddingPanel getOwner() {
		return owner_;
	}

	/**
	 * Shows this panel.
	 *
	 * @param pilotPoints
	 *            Pilot point names. Note that, this list should only contain pilot points which are included in RFORT omission.
	 * @param node
	 *            Popup node.
	 */
	public void show(ArrayList<String> pilotPoints, Node node) {

		// already shown
		if (isShown_)
			return;

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

		// clear name and pilot points
		name_.clear();
		table_.getItems().clear();

		// set pilot points
		for (String pp : pilotPoints) {
			table_.getItems().add(new RfortPilotPointOmission(pp));
		}

		// show popup
		popOver_.show(node);
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	@FXML
	private void onOkClicked() {

		// get name of omission
		String name = name_.getText();

		// get omissions
		ObservableList<RfortPilotPointOmission> directOmissions = table_.getItems();

		// no omissions defined
		if ((directOmissions == null) || directOmissions.isEmpty()) {
			popOver_.hide();
			return;
		}

		// check omissions
		if (!checkOmissions(name, directOmissions))
			return;

		// create array to store RFORT omissions
		RfortDirectOmission omission = new RfortDirectOmission(name.trim());
		for (RfortPilotPointOmission ppOmission : directOmissions) {
			omission.addOmission(ppOmission.getName(), Double.parseDouble(ppOmission.getOmission()));
		}

		// add them to owner panel
		owner_.addOmissions(omission);

		// hide popup
		popOver_.hide();
	}

	/**
	 * Checks omissions and returns true if they are acceptable.
	 *
	 * @param name
	 *            Name of omissions.
	 * @param omissions
	 *            Omissions to check.
	 * @return True if omissions are acceptable.
	 */
	private boolean checkOmissions(String name, ObservableList<RfortPilotPointOmission> omissions) {

		// check name
		if ((name == null) || name.trim().isEmpty()) {
			String message = "No name given for the omission. Please enter a name to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(name_);
			return false;
		}

		// loop over omissions
		for (RfortPilotPointOmission omission : omissions) {

			// check omission
			try {
				Double.parseDouble(omission.getOmission());
			}

			// invalid omission value
			catch (Exception e) {
				String message = "Pilot point '" + omission.getName() + "' has invalid omission value. Please supply a valid value.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(ok_);
				return false;
			}
		}

		// acceptable
		return true;
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static RfortAddOmissionsPopup load(RfortDirectOmissionAddingPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("RfortAddOmissionsPopup.fxml"));
			fxmlLoader.setResources(IconicFont.FONT_KEYS);
			fxmlLoader.load();

			// get controller
			RfortAddOmissionsPopup controller = (RfortAddOmissionsPopup) fxmlLoader.getController();

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
	 * Interface for RFORT direct omission adding panels.
	 *
	 * @author Murat Artim
	 * @date Apr 20, 2016
	 * @time 10:05:48 AM
	 */
	public interface RfortDirectOmissionAddingPanel {

		/**
		 * Returns the owner of this panel.
		 *
		 * @return The owner of this panel.
		 */
		InputPanel getOwner();

		/**
		 * Adds omissions.
		 *
		 * @param omissions
		 *            Omissions to add.
		 */
		void addOmissions(RfortOmission... omissions);
	}
}
