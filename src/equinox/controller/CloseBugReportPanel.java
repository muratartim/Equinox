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

import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.BugReport;
import equinox.task.CloseBugReport;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Class for close bug report panel.
 *
 * @author Murat Artim
 * @date Sep 12, 2014
 * @time 5:26:57 PM
 */
public class CloseBugReportPanel implements Initializable {

	/** The owner panel. */
	private BugPanel panel_;

	/** Bug report to close. */
	private BugReport report_;

	/** The owner pop-over. */
	private PopOver popOver_;

	@FXML
	private VBox root_;

	@FXML
	private TextArea closureText_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onOkClicked() {
		popOver_.hide();
		BugReportPanel roadmapPanel = (BugReportPanel) panel_.getOwner().getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.BUG_REPORT_PANEL);
		panel_.getOwner().getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(new CloseBugReport(report_, closureText_.getText(), roadmapPanel));
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param popOver
	 *            The owner pop-over.
	 * @param panel
	 *            The owner panel.
	 * @param report
	 *            Report to close.
	 * @return The newly loaded plot column panel.
	 */
	public static VBox load(PopOver popOver, BugPanel panel, BugReport report) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CloseBugReportPanel.fxml"));
			fxmlLoader.load();

			// get controller
			CloseBugReportPanel controller = (CloseBugReportPanel) fxmlLoader.getController();

			// set attributes
			controller.popOver_ = popOver;
			controller.panel_ = panel;
			controller.report_ = report;

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
