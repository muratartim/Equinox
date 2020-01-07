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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import org.controlsfx.dialog.CommandLinksDialog;
import org.controlsfx.dialog.CommandLinksDialog.CommandLinksButtonType;

import equinox.Equinox;
import equinox.data.EquinoxTheme;
import equinox.data.Settings;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.ServerUtility;
import equinox.task.CheckForEquinoxUpdates;
import equinox.task.ConnectToServers;
import equinox.task.CreateWorkspace;
import equinox.task.InternalEquinoxTask;
import equinox.task.LoadAllFiles;
import equinox.task.LoadPlugins;
import equinox.task.LoadUserAuthentication;
import equinox.task.LoadWorkspacePaths;
import equinox.task.OpenWorkspace;
import equinox.task.ShowNewsFeed;
import equinox.task.UpdateWorkspace;
import equinox.utility.Animator;
import equinox.utility.Utility;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Class for intro panel controller.
 *
 * @author Murat Artim
 * @date Jan 8, 2014
 * @time 1:44:19 PM
 */
public class IntroPanel implements Initializable, ChangeListener<State> {

	/** Waiting duration in milliseconds. */
	private static final long WAIT_DURATION = 2000;

	/** The main screen. */
	private MainScreen owner_;

	/** Mode of panel. */
	private boolean isIntro_ = true;

	/** List names containing task names. */
	private List<String> taskNames_ = Collections.synchronizedList(new ArrayList<String>());

	/** Task count. */
	volatile private int done_ = 0;

	@FXML
	private VBox root_;

	@FXML
	private ImageView banner_;

	@FXML
	private Label progressInfo_;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		// set banner image
		banner_.setImage(Utility.getImage("aboutDataAnalyst.png"));
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
	 * Returns the main screen of the application.
	 *
	 * @return The main screen of the application.
	 */
	public MainScreen getOwner() {
		return owner_;
	}

	/**
	 * Shows this panel in introduction mode.
	 *
	 */
	public void showIntro() {

		// scale down root node
		root_.setScaleX(0.0);
		root_.setScaleY(0.0);
		root_.setScaleZ(0.0);

		// intro mode
		isIntro_ = true;
		banner_.setCursor(null);

		// clear back layer
		owner_.getBackLayer().getChildren().clear();

		// add this panel to back layer
		owner_.getBackLayer().getChildren().add(root_);

		// add intro animation to javafx thread queue
		Platform.runLater(() -> {

			// create parallel transition
			SequentialTransition transition = new SequentialTransition();

			// create banner animation
			transition.getChildren().add(Animator.bouncingScale(80.0, 500.0, 0.0, 1.0, 1.0, (EventHandler<ActionEvent>) arg0 -> {

				// show connecting label
				progressInfo_.setVisible(true);

				// hide front and menubar layers
				owner_.getFrontLayer().setDisable(true);
				owner_.getMenuBarLayer().setDisable(true);
				owner_.getFrontLayer().setOpacity(0.0);
				owner_.getMenuBarLayer().setOpacity(0.0);

				// load last database paths
				owner_.getActiveTasksPanel().runTasksSequentially(addTask(new LoadWorkspacePaths(IntroPanel.this)));
			}, root_));

			// play
			transition.play();
		});
	}

	/**
	 * Shows this panel in about mode.
	 *
	 */
	public void showAbout() {

		// about mode
		isIntro_ = false;
		Utility.setHandCursor(banner_);
		progressInfo_.setVisible(false);

		// hide 3D Viewer (if it is current view)
		if (owner_.getViewPanel().getCurrentSubPanelIndex() == ViewPanel.OBJECT_VIEW) {
			owner_.getViewPanel().getSubPanel(ViewPanel.OBJECT_VIEW).hiding();
		}

		// clear back layer
		owner_.getBackLayer().getChildren().clear();

		// add this panel to back layer
		owner_.getBackLayer().getChildren().add(root_);

		// add intro animation to javafx thread queue
		Platform.runLater(() -> {

			// create animation
			SequentialTransition transition = new SequentialTransition();
			transition.getChildren().add(Animator.bouncingScale(80.0, 500.0, 0.0, 1.0, 1.0, null, root_));

			// animate main screen
			VBox inputPanel = owner_.getInputPanel().getRoot();
			VBox viewPanel = owner_.getViewPanel().getRoot();
			StackPane menuBarPanel = owner_.getMenuBarPanel().getRoot();
			Animator.animateMainScreen(inputPanel, viewPanel, menuBarPanel, false, transition, arg0 -> {

				// hide front and menubar layers
				owner_.getFrontLayer().setDisable(true);
				owner_.getMenuBarLayer().setDisable(true);
				owner_.getFrontLayer().setOpacity(0.0);
				owner_.getMenuBarLayer().setOpacity(0.0);
			});
		});
	}

	/**
	 * Called when last database paths are loaded.
	 *
	 * @param isSuccessful
	 *            True last paths were loaded successfully.
	 */
	public void databasePathsLoaded(boolean isSuccessful) {

		// successfully loaded
		if (isSuccessful) {

			// set database name as application title
			String title = Equinox.OS_ARCH.equals(ServerUtility.X86) ? "Equinox" : "Equinox 64bit";
			title += " - " + FileType.getNameWithoutExtension(Equinox.WORKSPACE_PATHS.getCurrentPath());
			owner_.getOwner().getStage().setTitle(title);

			// setup open recent menu
			Equinox.WORKSPACE_PATHS.setupOpenRecentMenu(owner_.getMenuBarPanel());

			// get server connection info, update database, load all files
			owner_.getActiveTasksPanel().runTasksSequentially(addTask(new LoadUserAuthentication()), addTask(new ConnectToServers()), addTask(new UpdateWorkspace()), addTask(new LoadAllFiles(IntroPanel.this)));
		}

		// unsuccessful
		else {

			// get server connection info
			owner_.getActiveTasksPanel().runTasksSequentially(addTask(new ConnectToServers()));

			// show setup workspace dialog
			PauseTransition pause = new PauseTransition(Duration.millis(WAIT_DURATION));
			pause.setOnFinished(event -> showSetupWorkspaceDialog("Welcome! Let's setup a local workspace to work with.\t\nHow would you like to proceed?"));
			pause.play();
		}
	}

	/**
	 * Adds given task to this panel.
	 *
	 * @param task
	 *            Task to add.
	 * @return The added task.
	 */
	public InternalEquinoxTask<?> addTask(InternalEquinoxTask<?> task) {
		task.stateProperty().addListener(this);
		taskNames_.add(task.getTaskTitle());
		return task;
	}

	/**
	 * Hides this panel in intro mode.
	 *
	 */
	public void hideIntro() {

		// wait and hide intro
		PauseTransition pause = new PauseTransition(Duration.millis(WAIT_DURATION));
		pause.setOnFinished(arg0 -> {

			// show front and menubar layers
			owner_.getFrontLayer().setDisable(false);
			owner_.getMenuBarLayer().setDisable(false);
			owner_.getFrontLayer().setOpacity(1.0);
			owner_.getMenuBarLayer().setOpacity(1.0);

			// create animation
			ParallelTransition transition = new ParallelTransition();
			transition.getChildren().add(Animator.bouncingScale(80.0, 500.0, 1.0, 0.0, 0.0, null, root_));

			// animate main screen
			VBox inputPanel = owner_.getInputPanel().getRoot();
			VBox viewPanel = owner_.getViewPanel().getRoot();
			StackPane menuBarPanel = owner_.getMenuBarPanel().getRoot();
			Animator.animateMainScreen(inputPanel, viewPanel, menuBarPanel, true, transition, arg01 -> {

				// remove this panel from back layer
				owner_.getBackLayer().getChildren().remove(root_);

				// load plugins
				owner_.getActiveTasksPanel().runTaskInParallel(new LoadPlugins());

				// check for Equinox updates
				if (Equinox.USER.hasPermission(Permission.CHECK_FOR_EQUINOX_UPDATES, false, owner_)) {
					if ((boolean) owner_.getSettings().getValue(Settings.NOTIFY_EQUINOX_UPDATES)) {
						owner_.getActiveTasksPanel().runTaskInParallel(new CheckForEquinoxUpdates(false));
					}
				}

				// show news feed
				if ((boolean) owner_.getSettings().getValue(Settings.SHOW_NEWSFEED)) {
					owner_.getActiveTasksPanel().runTaskInParallel(new ShowNewsFeed());
				}
			});
		});
		pause.play();
	}

	/**
	 * Shows setup workspace dialog.
	 *
	 * @param headerText
	 *            Header text of dialog.
	 */
	public void showSetupWorkspaceDialog(String headerText) {

		// create actions
		List<CommandLinksButtonType> links = Arrays.asList(new CommandLinksButtonType("Create a new workspace", "This creates an empty workspace at the selected location", new ImageView(Utility.getImage("newDatabase.png")), true),
				new CommandLinksButtonType("Open an existing workspace", "This connects to an existing workspace in the selected directory", new ImageView(Utility.getImage("browse.png")), false),
				new CommandLinksButtonType("Cancel and quit Equinox", "This cancels operation and quits Equinox", new ImageView(Utility.getImage("exit.png")), false));

		// create and setup dialog
		CommandLinksDialog dialog = new CommandLinksDialog(links);
		dialog.setTitle("Welcome");
		dialog.getDialogPane().setHeaderText(headerText);
		dialog.initStyle(StageStyle.UNDECORATED);
		dialog.initOwner(owner_.getOwner().getStage());

		// show dialog
		dialog.show();

		// listen for user choice
		dialog.resultProperty().addListener((InvalidationListener) observable -> {

			// get result
			String choice = dialog.getResult().getText();

			// create new workspace
			if (choice.equals("Create a new workspace")) {

				// get file chooser
				FileChooser fileChooser = owner_.getFileChooser(FileType.EQX.getExtensionFilter());

				// show save dialog
				fileChooser.setInitialFileName("newWorkspace" + FileType.EQX.getExtension());
				File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

				// no file selected
				if (selectedFile == null) {
					showSetupWorkspaceDialog(headerText);
					return;
				}

				// set initial directory
				owner_.setInitialDirectory(selectedFile);

				// append extension if necessary
				File file = FileType.appendExtension(selectedFile, FileType.EQX);

				// create new workspace
				owner_.getActiveTasksPanel().runTasksSequentially(addTask(new CreateWorkspace(file.toPath(), IntroPanel.this)));
			}

			// open an existing workspace
			else if (choice.equals("Open an existing workspace")) {

				// get directory chooser
				DirectoryChooser dirChooser = owner_.getDirectoryChooser();

				// show dialog
				File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

				// no directory selected
				if (selectedDir == null || !selectedDir.exists() || !selectedDir.getName().endsWith(FileType.EQX.getExtension())) {
					showSetupWorkspaceDialog(headerText);
					return;
				}

				// set initial directory
				owner_.setInitialDirectory(selectedDir);

				// open workspace
				owner_.getActiveTasksPanel().runTasksSequentially(addTask(new OpenWorkspace(selectedDir.toPath(), IntroPanel.this)));
			}

			// cancel and quit
			else if (choice.equals("Cancel and quit Equinox")) {
				Platform.exit();
			}
		});
	}

	@Override
	synchronized public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {

		// succeeded, failed or canceled
		if (newValue.equals(State.SUCCEEDED) || newValue.equals(State.CANCELLED) || newValue.equals(State.FAILED)) {

			// increment task count
			done_++;

			// add to javafx event queue
			Platform.runLater(() -> {
				String info = taskNames_.get(done_ - 1) + "\n(task " + done_ + " of " + taskNames_.size() + ")";
				progressInfo_.setText(info);
			});
		}
	}

	/**
	 * Hides this panel in about mode.
	 *
	 */
	private void hideAbout() {

		// show front and menubar layers
		owner_.getFrontLayer().setDisable(false);
		owner_.getMenuBarLayer().setDisable(false);
		owner_.getFrontLayer().setOpacity(1.0);
		owner_.getMenuBarLayer().setOpacity(1.0);

		// create animation
		ParallelTransition transition = new ParallelTransition();
		transition.getChildren().add(Animator.bouncingScale(80.0, 500.0, 1.0, 0.0, 0.0, null, root_));

		// animate main screen
		VBox inputPanel = owner_.getInputPanel().getRoot();
		VBox viewPanel = owner_.getViewPanel().getRoot();
		StackPane menuBarPanel = owner_.getMenuBarPanel().getRoot();
		Animator.animateMainScreen(inputPanel, viewPanel, menuBarPanel, true, transition, arg0 -> {

			// remove this panel from back layer
			owner_.getBackLayer().getChildren().remove(root_);

			// show 3D Viewer (if it is current view)
			if (owner_.getViewPanel().getCurrentSubPanelIndex() == ViewPanel.OBJECT_VIEW) {
				owner_.getViewPanel().getSubPanel(ViewPanel.OBJECT_VIEW).showing();
			}
		});
	}

	@FXML
	private void onBackClicked() {
		if (!isIntro_) {
			hideAbout();
		}
	}

	/**
	 * Loads and returns intro panel.
	 *
	 * @param owner
	 *            Main screen.
	 * @return The newly loaded intro panel.
	 */
	public static IntroPanel load(MainScreen owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("IntroPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			IntroPanel controller = (IntroPanel) fxmlLoader.getController();

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
