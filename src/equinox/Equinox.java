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
package equinox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariDataSource;

import container.remote.EmbeddedApplication;
import equinox.controller.MainScreen;
import equinox.data.ProgramArguments;
import equinox.data.ProgramArguments.ArgumentType;
import equinox.data.User;
import equinox.data.WorkspacePaths;
import equinox.data.ui.NotificationPanel;
import equinox.serverUtilities.ServerUtility;
import equinox.utility.Utility;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * This is the main application class.
 *
 * @author Murat Artim
 * @date Dec 5, 2013
 * @time 4:37:57 PM
 */
public class Equinox extends EmbeddedApplication {

	/** Application version. */
	public static final Version VERSION = Version.VENUS;

	/** Current operating system type and architecture. */
	public static String OS_TYPE, OS_ARCH;

	/** Paths to resources. */
	// @formatter:off
	public static Path RESOURCES_DIR, EXT_RESOURCES_DIR, UPDATE_DIR, HELP_DIR, SCRIPTS_DIR, TEMP_DIR, PLUGINS_DIR, NEWSFEED_DIR,
	LOG_FILE, SETTINGS_FILE, WORKSPACE_PATHS_FILE, PERL_EXECUTABLE, WORKSPACE_SCHEMA_PATH, NEWSFEED_MAP_FILE,
	USER_AUTHENTICATION_FILE;
	// @formatter:on

	/** Logger. */
	public static Logger LOGGER;

	/** Database connection pool. */
	public static HikariDataSource DBC_POOL;

	/** Thread pools. */
	public static ExecutorService FIXED_THREADPOOL, SINGLE_THREADPOOL, SUBTASK_THREADPOOL, SCHEDULED_THREADPOOL, CACHED_THREADPOOL;

	/** Workspace paths. */
	public static WorkspacePaths WORKSPACE_PATHS;

	/** User. */
	public static User USER = new User(System.getProperty("user.name"));

	/** Program arguments. */
	public static ProgramArguments ARGUMENTS;

	/** The primary stage of the application. */
	private Stage stage_;

	/** Main screen of the application. */
	private MainScreen mainScreen_;

	/**
	 * No argument constructor. This constructor should be used when the application is not wrapped inside the <code>AppContainer</code> (i.e. no auto-update mechanism is employed).
	 */
	public Equinox() {
		super();
	}

	/**
	 * Creates embedded application.
	 *
	 * @param appName
	 *            Application name.
	 * @param appDir
	 *            Path to application directory.
	 * @param configFile
	 *            Path launch configuration file.
	 * @param parameters
	 *            Application parameters.
	 * @param classLoader
	 *            Class loader to set to FXML loaders.
	 */
	public Equinox(String appName, String appDir, String configFile, Parameters parameters, ClassLoader classLoader) {
		super(appName, appDir, configFile, parameters, classLoader);
	}

	@Override
	public void init() throws Exception {

		// set default locale
		Locale.setDefault(Locale.US);

		// set operating system
		OS_TYPE = ServerUtility.getOSType();
		OS_ARCH = ServerUtility.getOSArch();

		// set program arguments
		ARGUMENTS = new ProgramArguments(this);

		// set system properties
		Properties props = System.getProperties();
		// OFF embedded-database durability setting to test
		// props.setProperty("derby.system.durability", "test");
		props.setProperty("derby.storage.pageSize", ARGUMENTS.getArgument(ArgumentType.DATABASE_PAGE_SIZE));
		props.setProperty("derby.storage.pageCacheSize", ARGUMENTS.getArgument(ArgumentType.DATABASE_PAGE_CACHE_SIZE));
		System.setProperties(props);

		// create last paths
		WORKSPACE_PATHS = new WorkspacePaths();

		// set global paths
		Path extResourcesDir = Paths.get(System.getProperty("user.home")).resolve(".equinoxResources");
		EXT_RESOURCES_DIR = Files.exists(extResourcesDir) ? extResourcesDir : Files.createDirectory(extResourcesDir);
		Path updateDir = EXT_RESOURCES_DIR.resolve("update");
		UPDATE_DIR = Files.exists(updateDir) ? updateDir : Files.createDirectory(updateDir);
		RESOURCES_DIR = getCodeBase().resolve("resources");
		WORKSPACE_SCHEMA_PATH = RESOURCES_DIR.resolve("db.zip");
		HELP_DIR = RESOURCES_DIR.resolve("help");
		SCRIPTS_DIR = RESOURCES_DIR.resolve("scripts");
		NEWSFEED_DIR = RESOURCES_DIR.resolve("newsfeed");
		NEWSFEED_MAP_FILE = NEWSFEED_DIR.resolve("feedmap.txt");
		PERL_EXECUTABLE = SCRIPTS_DIR.resolve("perl").resolve("bin").resolve("perl.exe");
		LOG_FILE = EXT_RESOURCES_DIR.resolve("equinox.log");
		SETTINGS_FILE = EXT_RESOURCES_DIR.resolve("equinox.set");
		USER_AUTHENTICATION_FILE = EXT_RESOURCES_DIR.resolve("equinox.usr");
		WORKSPACE_PATHS_FILE = EXT_RESOURCES_DIR.resolve("equinox.dat");
		Path tempDir = RESOURCES_DIR.resolve("temp");
		TEMP_DIR = Files.exists(tempDir) ? tempDir : Files.createDirectory(tempDir);
		Path pluginsDir = RESOURCES_DIR.resolve("plugins");
		PLUGINS_DIR = Files.exists(pluginsDir) ? pluginsDir : Files.createDirectory(pluginsDir);

		// create logger
		LOGGER = Utility.createLogger(Level.INFO);
		LOGGER.info(this.getClass().getSimpleName() + " is initialized.");
	}

	@Override
	public void start(Stage stage) throws Exception {

		// create thread pools
		FIXED_THREADPOOL = Executors.newFixedThreadPool(Integer.parseInt(ARGUMENTS.getArgument(ArgumentType.MAX_PARALLEL_TASKS)));
		SINGLE_THREADPOOL = Executors.newSingleThreadExecutor();
		SUBTASK_THREADPOOL = Executors.newFixedThreadPool(Integer.parseInt(ARGUMENTS.getArgument(ArgumentType.MAX_PARALLEL_SUBTASKS)));
		SCHEDULED_THREADPOOL = Executors.newSingleThreadScheduledExecutor();
		CACHED_THREADPOOL = Executors.newCachedThreadPool();
		LOGGER.info("Thread pools created.");

		// clean temporary directory
		Utility.deleteTemporaryFiles(Equinox.TEMP_DIR, Equinox.TEMP_DIR);
		LOGGER.info("Temporary directory cleaned.");

		// clean update directory
		Utility.deleteTemporaryFiles(Equinox.UPDATE_DIR, Equinox.UPDATE_DIR);
		LOGGER.info("Update directory cleaned.");

		// set stage
		stage_ = stage;

		// load main screen
		mainScreen_ = MainScreen.load(this);

		// setup stage
		stage_.setScene(new Scene(mainScreen_.getRoot()));
		stage_.setTitle(OS_ARCH.equals(ServerUtility.X86) ? "Equinox" : "Equinox 64bit");
		stage_.getIcons().add(Utility.getImage("equinoxIcon.png"));
		stage_.show();

		// start main screen
		mainScreen_.start();

		// listen for window close events
		stage_.setOnCloseRequest(event -> {

			// there are running tasks
			if (mainScreen_.getActiveTasksPanel().hasRunningTasks()) {

				// consume event
				event.consume();

				// ask user for closure
				askForClosure(false);
			}

			// there is no running task
			else {

				// consume event
				event.consume();

				// exit
				Platform.exit();
			}
		});

		// echo program arguments
		ARGUMENTS.echoArguments();

		// log
		LOGGER.info(this.getClass().getSimpleName() + " is started.");
	}

	@Override
	public void stop() throws Exception {

		// stop main screen
		mainScreen_.stop();

		// shutdown thread pools
		Utility.shutdownThreadExecutor(FIXED_THREADPOOL);
		Utility.shutdownThreadExecutor(SINGLE_THREADPOOL);
		Utility.shutdownThreadExecutor(SUBTASK_THREADPOOL);
		Utility.shutdownThreadExecutor(SCHEDULED_THREADPOOL);
		Utility.shutdownThreadExecutor(CACHED_THREADPOOL);
		LOGGER.info("Thread pools shutdown.");

		// shutdown database connection pool
		if (DBC_POOL != null) {
			DBC_POOL.close();
			LOGGER.info("Local database connection pool shutdown.");
		}

		// shutdown the local database
		Utility.shutdownWorkspace();

		// log
		LOGGER.info(this.getClass().getSimpleName() + " is stopped.");

		// close logger
		Arrays.stream(LOGGER.getHandlers()).forEach(h -> h.close());

		// exit
		System.exit(0);
	}

	/**
	 * Returns the main screen of the application.
	 *
	 * @return The main screen of the application.
	 */
	public MainScreen getMainScreen() {
		return mainScreen_;
	}

	/**
	 * Returns the primary stage of the application.
	 *
	 * @return The primary stage of the application.
	 */
	public Stage getStage() {
		return stage_;
	}

	/**
	 * Asks user for closure.
	 *
	 * @param restart
	 *            True to restart, otherwise quit.
	 */
	public void askForClosure(boolean restart) {

		// get notification pane
		NotificationPanel np = mainScreen_.getNotificationPane();

		// setup notification title and message
		String title = "Tasks running on background";
		String operation = restart ? "restart" : "close";
		String message = "There are running tasks on background. They will be canceled if you choose to " + operation + ". Are you sure you want to " + operation + " Equinox?";

		// show notification
		np.showQuestion(title, message, "Yes", "No", event -> {

			try {

				// cancel all tasks
				mainScreen_.getActiveTasksPanel().cancelAllTasks();

				// restart
				if (restart) {
					restartContainer();
				}

				// exit
				Platform.exit();
			}

			// exception occurred
			catch (Exception e) {
				Equinox.LOGGER.log(Level.WARNING, "Exception occured during restarting Equinox.", e);
				np.hide();
			}
		}, event -> np.hide());
	}

	/**
	 * The main() method is ignored in correctly deployed JavaFX application. main() serves only as fallback in case the application can not be launched through deployment artifacts, e.g., in IDEs with limited FX support. NetBeans ignores main().
	 *
	 * @param args
	 *            The command line arguments.
	 * @throws IOException
	 *             If exception occurs during launch.
	 */
	public static void main(String[] args) throws IOException {
		launch(args);
	}
}
