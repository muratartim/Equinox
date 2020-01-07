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
import org.controlsfx.control.ToggleSwitch;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.message.GetBugReportsRequest;
import equinox.task.GetBugReports;
import equinox.task.SubmitBugReport;
import equinox.utility.Utility;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Class for bug report panel.
 *
 * @author Murat Artim
 * @date May 11, 2014
 * @time 4:07:47 PM
 */
public class BugReportPanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private TextArea report_;

	@FXML
	private ToggleSwitch sysInfo_, eventLog_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
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
	public void showing() {
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetBugReports(GetBugReportsRequest.OPEN));
	}

	@Override
	public String getHeader() {
		return "Bug Report";
	}

	/**
	 * Sets bug description.
	 *
	 * @param description
	 *            Bug description.
	 */
	public void setDescription(String description) {
		report_.setText(description);
	}

	/**
	 * Called when a report is successfully submitted.
	 *
	 */
	public void reportSubmitted() {

		// get bug reports
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetBugReports(GetBugReportsRequest.OPEN));

		// reset panel
		onResetClicked();
	}

	/**
	 * Called when a report is closed.
	 *
	 */
	public void reportClosed() {
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetBugReports(GetBugReportsRequest.CLOSED));
	}

	@FXML
	private void onOkClicked() {

		// get inputs
		String report = report_.getText();
		boolean sysInfo = sysInfo_.isSelected();
		boolean eventLog = eventLog_.isSelected();

		// no description given
		if (report == null || report.isEmpty()) {
			String message = "No bug description given. Please describe the problem in order to proceed submitting your bug report.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 50, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(report_);
			return;
		}

		// start task
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SubmitBugReport(report, sysInfo, eventLog, this));
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onResetClicked() {
		report_.clear();
		if (!sysInfo_.isSelected()) {
			sysInfo_.setSelected(true);
		}
		if (!eventLog_.isSelected()) {
			eventLog_.setSelected(true);
		}
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to report a bug", null);
	}

	@FXML
	private void onSystemInformationClicked() {
		owner_.getOwner().showHelp("How to report a bug", "Options");
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded file CDF set panel.
	 */
	public static BugReportPanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("BugReportPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			BugReportPanel controller = (BugReportPanel) fxmlLoader.getController();

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
