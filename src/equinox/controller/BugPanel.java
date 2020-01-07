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
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.Equinox;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.BugReport;
import equinox.dataServer.remote.data.BugReport.BugReportInfo;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Class for bug panel controller.
 *
 * @author Murat Artim
 * @date Sep 12, 2014
 * @time 4:54:52 PM
 */
public class BugPanel implements Initializable {

	/** Owner panel. */
	private BugReportViewPanel owner_;

	/** Bug report. */
	private BugReport report_;

	/** Close link. */
	private Hyperlink close_;

	/** Closure label. */
	private Label closure_, closedBy_;

	@FXML
	private VBox root_;

	@FXML
	private Label title_, description_, info_, status_;

	@FXML
	private HBox linkContainer_;

	@FXML
	private Hyperlink eventLog_, sysInfo_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create closure label
		closure_ = new Label();
		closure_.setTextFill(Color.GREY);
		closure_.setWrapText(true);
		closure_.setPadding(new Insets(5, 0, 0, 0));
		VBox.setVgrow(closure_, Priority.ALWAYS);

		// create closed by label
		closedBy_ = new Label();
		closedBy_.setTextFill(Color.GREY);

		// create close link
		ImageView image = new ImageView(Utility.getImage("cancel.png"));
		image.setFitWidth(12);
		image.setFitHeight(12);
		close_ = new Hyperlink("Close", image);

		// set on action
		close_.setOnAction(event -> {

			// show popup
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
			popOver.setTitle("Close Bug Report");
			popOver.setContentNode(CloseBugReportPanel.load(popOver, BugPanel.this, report_));
			popOver.setHideOnEscape(true);
			popOver.show(close_);
		});
	}

	/**
	 * Returns the owner panel of this sub panel.
	 *
	 * @return The owner panel of this sub panel.
	 */
	public BugReportViewPanel getOwner() {
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
	 * Sets bug report to panel.
	 *
	 * @param report
	 *            Report to set.
	 */
	public void setReport(BugReport report) {

		// set data
		report_ = report;

		// setup components
		title_.setText(report_.getTitle());
		String description = (String) report_.getInfo(BugReportInfo.REPORT);
		if (description != null && !description.isEmpty()) {
			description_.setText(description);
		}
		else {
			root_.getChildren().remove(description_);
		}
		String status = (String) report_.getInfo(BugReportInfo.STATUS);
		status_.setText(status);
		status_.setTextFill(status.equals(BugReport.OPEN) ? Color.GREEN : Color.FIREBRICK);
		String solution = (String) report_.getInfo(BugReportInfo.SOLUTION);
		if (solution != null && !solution.isEmpty()) {
			closure_.setText("Closure:\n" + solution);
			root_.getChildren().add(2, closure_);
		}
		String owner = (String) report_.getInfo(BugReportInfo.OWNER);
		String info = "Owner: " + (owner == null ? "unknown" : owner);
		info += ", Date: " + new SimpleDateFormat("dd/MM/yyyy").format(report_.getInfo(BugReportInfo.RECORDED));
		info_.setText(info);

		// add close button
		if (status.equals(BugReport.OPEN)) { // report open
			if (Equinox.USER.isLoggedAsAdministrator()) { // logged in as administrator
				linkContainer_.getChildren().add(close_);
			}
		}

		// add closed by label
		if (status.equals(BugReport.CLOSED)) {
			closedBy_.setText("Closed by: " + (String) report_.getInfo(BugReportInfo.CLOSED_BY));
			linkContainer_.getChildren().clear();
			linkContainer_.getChildren().add(closedBy_);
		}
	}

	@FXML
	private void onSysInfoClicked() {
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(InfoPanel.load((String) report_.getInfo(BugReportInfo.SYS_INFO)));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(sysInfo_);
	}

	@FXML
	private void onEventLogClicked() {
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(InfoPanel.load((String) report_.getInfo(BugReportInfo.EVENT_LOG)));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);
		popOver.show(eventLog_);
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static BugPanel load(BugReportViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("BugPanel.fxml"));
			fxmlLoader.load();

			// get controller
			BugPanel controller = (BugPanel) fxmlLoader.getController();

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
