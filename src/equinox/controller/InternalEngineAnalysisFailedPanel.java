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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.apache.commons.text.WordUtils;

import com.jfoenix.controls.JFXTabPane;

import equinox.Equinox;
import equinox.data.EquinoxTheme;
import equinox.plugin.FileType;
import equinox.task.SubmitBugReport;
import equinox.utility.Utility;
import equinox.utility.exception.InternalEngineAnalysisFailedException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.CacheHint;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

/**
 * Class for internal analysis failed panel controller.
 *
 * @author Murat Artim
 * @date 10 Apr 2017
 * @time 12:43:40
 *
 */
public class InternalEngineAnalysisFailedPanel implements Initializable {

	/** The owner main screen. */
	private MainScreen mainScreen_;

	/** List of output files. */
	private File[] outputFiles_;

	/** True if download request sent. */
	private AtomicBoolean outputsAdded_ = new AtomicBoolean();

	@FXML
	private VBox root_;

	@FXML
	private Label title_, message_;

	@FXML
	private ImageView detailsImage_;

	@FXML
	private TitledPane errorPane_;

	@FXML
	private JFXTabPane tabPane_;

	@FXML
	private TextArea exceptionArea_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Sets output files to this panel.
	 *
	 * @param outputFiles
	 *            List of output files or null if no output file was found.
	 */
	private void setOutputFiles(File[] outputFiles) {
		outputFiles_ = outputFiles;
	}

	@FXML
	private void onShowLogClicked() {

		// expand
		errorPane_.setExpanded(!errorPane_.isExpanded());

		// modify arrow icon
		detailsImage_.setImage(errorPane_.isExpanded() ? Utility.getImage("arrowUpWhite.png") : Utility.getImage("arrowDownWhite.png"));

		// scroll top
		exceptionArea_.setScrollTop(Double.MIN_VALUE);

		// output file tabs already added
		if (outputsAdded_.get())
			return;
		outputsAdded_.set(true);

		// no output file available
		if ((outputFiles_ == null))
			return;

		// loop over files
		for (File file : outputFiles_) {
			addFileTab(file, tabPane_);
		}
	}

	@FXML
	private void onReportBugClicked() {

		// report bug
		BugReportPanel panel = (BugReportPanel) mainScreen_.getInputPanel().getSubPanel(InputPanel.BUG_REPORT_PANEL);
		mainScreen_.getActiveTasksPanel().runTaskInParallel(new SubmitBugReport(exceptionArea_.getText(), true, true, panel));
		mainScreen_.getNotificationPane().hide();
	}

	/**
	 * Creates and adds a new tab for the given output file.
	 *
	 * @param file
	 *            Path to output file.
	 * @param tabPane
	 *            Tab pane.
	 */
	private static void addFileTab(File file, JFXTabPane tabPane) {

		// file doesn't exist
		if ((file == null) || !file.exists())
			return;

		// get file type
		FileType type = FileType.getFileType(file);

		// unknown type
		if (type == null)
			return;

		// create tab
		// DEPRECATION using org.apache.commons.text.WordUtils instead of org.apache.commons.lang3.text.WordUtils
		String tabTitle = WordUtils.capitalizeFully(type.getExtension().substring(1));
		Tab tab = new Tab(tabTitle);
		tab.setClosable(false);
		tabPane.getTabs().add(tab);

		// create layout
		WebView view = new WebView();
		view.setCache(true);
		view.setCacheHint(CacheHint.SPEED);
		view.setPrefHeight(300);
		view.setPrefWidth(3000);
		view.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		// create scroll pane
		ScrollPane scroll = new ScrollPane(view);
		scroll.setVbarPolicy(ScrollBarPolicy.NEVER);
		tab.setContent(scroll);

		// load file content to web view
		loadFile(file.toPath(), view, tabTitle, !type.equals(FileType.HTML));
	}

	/**
	 * Loads given file to given web view.
	 *
	 * @param file
	 *            File to load.
	 * @param webView
	 *            Web view.
	 * @param tabTitle
	 *            Tab title.
	 * @param appendExtension
	 *            True if TXT extension should be appended to file name before loading its contents to web viewer. This is required since web viewer can only handle TXT and HTML extension files.
	 */
	private static void loadFile(Path file, WebView webView, String tabTitle, boolean appendExtension) {

		// load file
		try {

			// append extension
			if (appendExtension) {
				file = Files.move(file, file.resolveSibling(file.getFileName().toString() + ".txt"));
			}

			// load contents to viewer
			String url = file.toUri().toURL().toExternalForm();
			webView.getEngine().load(url);
		}

		// exception occurred
		catch (Exception e) {

			// log exception
			String msg = "Exception occurred during loading " + tabTitle + " file.";
			Equinox.LOGGER.log(Level.WARNING, msg, e);

			// get stack trace
			String log = e.getMessage() + "<br>";
			for (StackTraceElement ste : e.getStackTrace()) {
				log += ste.toString() + "<br>";
			}

			// create HTML error page
			String errorPage = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">";
			errorPage += "<html>";
			errorPage += "<head>";
			errorPage += "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">";
			errorPage += "<title>Update Description</title>";
			errorPage += "<style type=\"text/css\">";
			errorPage += "body {padding-left:10px;font-family: Helvetica, Georgia, \"Times New Roman\", Times, serif;font-size:14px;color: black;background-color: whitesmoke;}";
			errorPage += "h1 {font-size:22px;color: steelblue;border-bottom: thin dotted;}";
			errorPage += "par {line-height:130%}";
			errorPage += "</style>";
			errorPage += "</head>";
			errorPage += "<body>";
			errorPage += "<h1>Exception occurred during loding " + tabTitle + " file:</h1>";
			errorPage += "<par>" + log + "</par>";
			errorPage += "</body>";
			errorPage += "</html>";
			webView.getEngine().loadContent(errorPage);
		}
	}

	/**
	 * Loads and returns error notification panel.
	 *
	 * @param mainScreen
	 *            Main screen.
	 * @param title
	 *            Notification title.
	 * @param message
	 *            Message text.
	 * @param e
	 *            Error.
	 * @return The newly loaded error notification panel.
	 */
	public static VBox load(MainScreen mainScreen, String title, String message, InternalEngineAnalysisFailedException e) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("InternalEngineAnalysisFailedPanel.fxml"));
			fxmlLoader.load();

			// get controller
			InternalEngineAnalysisFailedPanel controller = (InternalEngineAnalysisFailedPanel) fxmlLoader.getController();

			// set attributes
			controller.mainScreen_ = mainScreen;
			controller.title_.setText(title);
			controller.message_.setText(message);
			String log = message + "\n";
			log += e.getMessage() + "\n";
			for (StackTraceElement ste : e.getStackTrace()) {
				log += ste.toString() + "\n";
			}
			controller.exceptionArea_.appendText(log);
			controller.setOutputFiles(e.getOuputFiles());

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
}
