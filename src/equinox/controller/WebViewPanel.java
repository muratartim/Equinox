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

import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.logging.Level;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import equinox.Equinox;
import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.plugin.FileType;
import equinox.task.DownloadHelpVideo;
import equinox.task.SaveImage;
import equinox.utility.Shapes;
import equinox.utility.Utility;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import netscape.javascript.JSObject;

/**
 * Class for help view panel.
 *
 * @author Murat Artim
 * @date May 8, 2014
 * @time 10:19:18 AM
 */
public class WebViewPanel implements InternalViewSubPanel {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Control panel. */
	private WebViewControls controls_;

	/** Current address. */
	private EquinoxAddress currentAddress_;

	/** Back and next url lists. */
	private ObservableList<EquinoxAddress> backList_, nextList_;

	@FXML
	private VBox root_;

	@FXML
	private HBox banner_;

	@FXML
	private WebView view_;

	@FXML
	private StackPane stack_;

	@FXML
	private Button back_, next_;

	@FXML
	private ScrollPane scrollPane_;

	/** No output file panel. */
	private VBox noOutputFilePanel_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create controls
		controls_ = WebViewControls.load(this);

		// process page loading
		WebEngine we = view_.getEngine();
		we.getLoadWorker().stateProperty().addListener((ObservableValue<? extends State> ov, State oldState, State newState) -> {
			if (newState == State.SUCCEEDED) {
				JSObject win = (JSObject) we.executeScript("window");
				win.setMember("app", new JavaApp());
			}
		});

		// create lists
		backList_ = FXCollections.observableArrayList();
		nextList_ = FXCollections.observableArrayList();

		// setup arrow buttons
		Shapes.createArrowButton(next_, false);
		Shapes.createArrowButton(back_, true);

		// bind navigation buttons to lists
		backList_.addListener(new ListChangeListener<EquinoxAddress>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends EquinoxAddress> c) {
				back_.setDisable(backList_.isEmpty());
			}
		});
		nextList_.addListener(new ListChangeListener<EquinoxAddress>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends EquinoxAddress> c) {
				next_.setDisable(nextList_.isEmpty());
			}
		});

		// create no output file panel
		String message = "No output file is associated with the selected result.";
		String suggestions = "Make sure to check the analysis engine settings. In order to keep analysis output files:\n";
		suggestions += "\t- Go to File --> Settings,\n";
		suggestions += "\t- Select 'Analysis Engine' tab,\n";
		suggestions += "\t- Switch on 'Keep analysis output files' option.\n";
		noOutputFilePanel_ = NoResultsPanel.load(message, suggestions);
		noOutputFilePanel_.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		StackPane.setAlignment(noOutputFilePanel_, Pos.TOP_LEFT);
		stack_.getChildren().add(noOutputFilePanel_);
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {
		// no implementation
	}

	@Override
	public void hiding() {
		showHelp("EmptyPage", null);
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public ViewPanel getOwner() {
		return owner_;
	}

	@Override
	public String getHeader() {
		if (currentAddress_ == null)
			return "Help";
		return currentAddress_.header_;
	}

	@Override
	public HBox getControls() {
		return controls_.getRoot();
	}

	@Override
	public boolean canSaveView() {
		return true;
	}

	@Override
	public void saveView() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName(getViewName() + FileType.PNG.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File file = FileType.appendExtension(selectedFile, FileType.PNG);

		// take snapshot of the scene
		WritableImage snapshot = root_.snapshot(null, null);

		// create and start task in progress panel
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SaveImage(file, snapshot));
	}

	@Override
	public String getViewName() {
		return currentAddress_ == null ? "Web View" : currentAddress_.header_;
	}

	@Override
	public WritableImage getViewImage() {
		return root_.snapshot(null, null);
	}

	/**
	 * Loads given page to help view.
	 *
	 * @param page
	 *            Page name. Not that, this doesn't include the file extension.
	 * @param location
	 *            Location within the page. This can be null for start of page.
	 */
	public void showHelp(String page, String location) {

		// set background color
		root_.setStyle("-fx-background-color:transparent;");

		// add current url to back list
		if (currentAddress_ != null)
			if (backList_.isEmpty()) {
				backList_.add(currentAddress_);
			}
			else if (!backList_.get(backList_.size() - 1).equals(currentAddress_)) {
				backList_.add(currentAddress_);
			}

		// clear next list
		nextList_.clear();

		// update current page
		currentAddress_ = new EquinoxAddress(page, location, "Help", EquinoxAddress.HELP_PAGE);

		// load page
		currentAddress_.loadPage();
	}

	/**
	 * Shows given web address in the web view panel.
	 *
	 * @param address
	 *            Internet address.
	 * @param location
	 *            Location within the web page. This can be null for start of page.
	 * @param header
	 *            Panel header.
	 * @param bgColor
	 *            Viewer background color.
	 */
	public void showAddress(String address, String location, String header, String bgColor) {

		// set background color
		root_.setStyle("-fx-background-color:" + bgColor + ";");

		// add current url to back list
		if (currentAddress_ != null)
			if (backList_.isEmpty()) {
				backList_.add(currentAddress_);
			}
			else if (!backList_.get(backList_.size() - 1).equals(currentAddress_)) {
				backList_.add(currentAddress_);
			}

		// clear next list
		nextList_.clear();

		// update current page
		currentAddress_ = new EquinoxAddress(address, location, header, EquinoxAddress.WEB_PAGE);

		// load page
		currentAddress_.loadPage();
	}

	/**
	 * Shows given output file.
	 *
	 * @param outputFile
	 *            Path to output file.
	 * @param header
	 *            Panel header.
	 */
	public void showOutputFile(Path outputFile, String header) {

		// set background color
		root_.setStyle("-fx-background-color:transparent;");

		// add current url to back list
		if (currentAddress_ != null)
			if (backList_.isEmpty()) {
				backList_.add(currentAddress_);
			}
			else if (!backList_.get(backList_.size() - 1).equals(currentAddress_)) {
				backList_.add(currentAddress_);
			}

		// clear next list
		nextList_.clear();

		// update current page
		currentAddress_ = new EquinoxAddress(outputFile, header);

		// load page
		currentAddress_.loadPage();
	}

	/**
	 * Called when a video is downloaded.
	 *
	 * @param file
	 *            Path to downloaded video.
	 */
	public void videoDownloaded(Path file) {

		// open file in default editor
		try {

			// desktop is not supported
			if (!java.awt.Desktop.isDesktopSupported()) {
				String message = "Playing help video has failed. Cannot start help video. Desktop class is not supported.";
				owner_.getOwner().getNotificationPane().showWarning(message, null);
				return;
			}

			// open action is not supported
			if (!java.awt.Desktop.getDesktop().isSupported(Action.OPEN)) {
				String message = "Playing help video has failed. Cannot start help video. Open action is not supported.";
				owner_.getOwner().getNotificationPane().showWarning(message, null);
				return;
			}

			// open video with default media player
			java.awt.Desktop.getDesktop().open(file.toFile());
		}

		// exception occurred
		catch (IOException e) {
			String msg = "Exception occurred during opening help video: ";
			Equinox.LOGGER.log(Level.WARNING, msg, e);
			msg += e.getLocalizedMessage();
			msg += " Click 'Details' for more information.";
			owner_.getOwner().getNotificationPane().showError("Problem encountered", msg, e);
		}
	}

	@FXML
	private void onBackClicked() {

		// back list is empty
		if (backList_.isEmpty())
			return;

		// add current url to next list
		if (currentAddress_ != null)
			if (nextList_.isEmpty()) {
				nextList_.add(currentAddress_);
			}
			else if (!nextList_.get(nextList_.size() - 1).equals(currentAddress_)) {
				nextList_.add(currentAddress_);
			}

		// get last element and remove it from the back list
		int size = backList_.size();
		currentAddress_ = backList_.get(size - 1);
		backList_.remove(size - 1);

		// load last back item
		currentAddress_.loadPage();
	}

	@FXML
	private void onNextClicked() {

		// next list is empty
		if (nextList_.isEmpty())
			return;

		// add current url to back list
		if (currentAddress_ != null)
			if (backList_.isEmpty()) {
				backList_.add(currentAddress_);
			}
			else if (!backList_.get(backList_.size() - 1).equals(currentAddress_)) {
				backList_.add(currentAddress_);
			}

		// get last element and remove it from the next list
		int size = nextList_.size();
		currentAddress_ = nextList_.get(size - 1);
		nextList_.remove(size - 1);

		// load last next item
		currentAddress_.loadPage();
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static WebViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("WebViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			WebViewPanel controller = (WebViewPanel) fxmlLoader.getController();

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

	/**
	 * Inner class for visited web pages.
	 *
	 * @author Murat Artim
	 * @date Dec 20, 2014
	 * @time 4:10:20 PM
	 */
	public class EquinoxAddress {

		/** Address type index. */
		private static final int HELP_PAGE = 0, OUTPUT_FILE = 1, WEB_PAGE = 2;

		/** URL and location within page. */
		private final String page_, location_, header_;

		/** Address type. */
		private final int adressType_;

		/** Path to output file. */
		private final Path outputFile_;

		/**
		 * Creates help page address.
		 *
		 * @param page
		 *            URL address.
		 * @param location
		 *            Location within the page.
		 * @param header
		 *            Panel header.
		 * @param addressType
		 *            Address type.
		 */
		public EquinoxAddress(String page, String location, String header, int addressType) {
			page_ = page;
			location_ = location;
			header_ = header;
			adressType_ = addressType;
			outputFile_ = null;
		}

		/**
		 * Creates web page address.
		 *
		 * @param outputFile
		 *            Analysis output file.
		 * @param header
		 *            Panel header.
		 */
		public EquinoxAddress(Path outputFile, String header) {
			outputFile_ = outputFile;
			header_ = outputFile == null ? "No Output File Found" : header;
			adressType_ = OUTPUT_FILE;
			page_ = null;
			location_ = null;
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder(13, 67).append(page_).toHashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof EquinoxAddress))
				return false;
			if (o == this)
				return true;
			EquinoxAddress ea = (EquinoxAddress) o;
			return new EqualsBuilder().append(page_, ea.page_).isEquals();
		}

		/**
		 * Returns URL address.
		 *
		 * @return URL address.
		 */
		public String getPage() {
			return page_;
		}

		/**
		 * Loads the page in the viewer.
		 *
		 */
		public void loadPage() {

			// help page
			if (adressType_ == HELP_PAGE) {

				// add banner
				if (!root_.getChildren().contains(banner_)) {
					root_.getChildren().add(0, banner_);
				}

				// load page
				try {
					String url = Equinox.HELP_DIR.resolve(page_ + ".html").toUri().toURL().toExternalForm();
					url = location_ == null ? url : url.concat("#" + location_);
					view_.getEngine().load(url);
					noOutputFilePanel_.setVisible(false);
					view_.setPrefWidth(Control.USE_COMPUTED_SIZE);
					scrollPane_.setFitToWidth(true);
					controls_.getSaveButton().setDisable(true);
				}

				// exception occurred
				catch (MalformedURLException e) {

					// log exception
					String message = "Exception occurred during showing help page: ";
					Equinox.LOGGER.log(Level.WARNING, message, e);

					// create and show notification
					message += e.getLocalizedMessage();
					message += " Click 'Details' for more information.";
					owner_.getOwner().getNotificationPane().showError("Problem encountered", message, e);
				}
			}

			// output file
			else if (adressType_ == OUTPUT_FILE) {

				// remove banner
				if (root_.getChildren().contains(banner_)) {
					root_.getChildren().remove(banner_);
				}

				// no output file
				if ((outputFile_ == null) || !Files.exists(outputFile_)) {
					noOutputFilePanel_.setVisible(true);
					controls_.getSaveButton().setDisable(true);
					return;
				}

				// load file
				try {
					String url = outputFile_.toUri().toURL().toExternalForm();
					view_.getEngine().load(url);
					noOutputFilePanel_.setVisible(false);
					view_.setPrefWidth(3000);
					scrollPane_.setFitToWidth(false);
					controls_.getSaveButton().setDisable(false);
				}

				// exception occurred
				catch (MalformedURLException e) {

					// log exception
					String message = "Exception occurred during showing analysis output file: ";
					Equinox.LOGGER.log(Level.WARNING, message, e);

					// create and show notification
					message += e.getLocalizedMessage();
					message += " Click 'Details' for more information.";
					owner_.getOwner().getNotificationPane().showError("Problem encountered", message, e);
				}
			}

			// web page
			else if (adressType_ == WEB_PAGE) {

				// remove banner
				if (root_.getChildren().contains(banner_)) {
					root_.getChildren().remove(banner_);
				}

				// load page
				try {
					String url = new URI(page_).toURL().toExternalForm();
					url = location_ == null ? url : url.concat("#" + location_);
					view_.getEngine().load(url);
					noOutputFilePanel_.setVisible(false);
					view_.setPrefWidth(Control.USE_COMPUTED_SIZE);
					scrollPane_.setFitToWidth(true);
					controls_.getSaveButton().setDisable(true);
				}

				// exception occurred
				catch (MalformedURLException | URISyntaxException e) {

					// log exception
					String message = "Exception occurred during showing web page: ";
					Equinox.LOGGER.log(Level.WARNING, message, e);

					// create and show notification
					message += e.getLocalizedMessage();
					message += " Click 'Details' for more information.";
					owner_.getOwner().getNotificationPane().showError("Problem encountered", message, e);
				}
			}
		}
	}

	/**
	 * Javascript interface object.
	 *
	 * @author Murat Artim
	 * @date Dec 21, 2014
	 * @time 12:47:45 PM
	 */
	public class JavaApp {

		/**
		 * Loads given page to help view.
		 *
		 * @param page
		 *            Page name. Not that, this doesn't include the file extension.
		 * @param location
		 *            Location within the page. This can be null for start of page.
		 */
		public void helpLink(String page, String location) {
			showHelp(page, location.isEmpty() ? null : location);
		}

		/**
		 * Shows given web address in the web view panel.
		 *
		 * @param address
		 *            Internet address.
		 * @param location
		 *            Location within the web page. This can be null for start of page.
		 * @param header
		 *            Panel header.
		 */
		public void addressLink(String address, String location, String header) {
			showAddress(address, location, header, "transparent");
		}

		/**
		 * Downloads and plays help video.
		 *
		 * @param videoName
		 *            Name of the video to play.
		 */
		public void helpVideoLink(String videoName) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new DownloadHelpVideo(videoName));
		}
	}
}
