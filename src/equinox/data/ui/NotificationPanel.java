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
package equinox.data.ui;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.Timer;

//DEPRECATION using org.apache.commons.text.WordUtils instead of org.apache.commons.lang3.text.WordUtils
import org.apache.commons.text.WordUtils;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.control.action.Action;

import equinox.Equinox;
import equinox.controller.ChatNotificationPanel;
import equinox.controller.ChatPopup;
import equinox.controller.DirectoryOutputtingOkNotificationPanel;
import equinox.controller.ErrorPanel;
import equinox.controller.IncomingFilePanel;
import equinox.controller.IncomingRunRequestPanel;
import equinox.controller.InfoNotificationPanel;
import equinox.controller.InternalEngineAnalysisFailedPanel;
import equinox.controller.MainScreen;
import equinox.controller.MaterialUpdateNotificationPanel;
import equinox.controller.NotificationPanel1;
import equinox.controller.OkNotificationPanel;
import equinox.controller.PluginUpdateNotificationPanel;
import equinox.controller.PrivilegeNotificationPanel;
import equinox.controller.QuestionNotificationPanel;
import equinox.controller.QueuedNotificationPanel;
import equinox.controller.SavedNotificationPanel;
import equinox.controller.ScheduledNotificationPanel;
import equinox.controller.ServerAnalysisFailedPanel;
import equinox.controller.ServerAnnouncementPanel;
import equinox.controller.SubmittedNotificationPanel;
import equinox.controller.UpdatePanel;
import equinox.controller.WarningNotificationPanel;
import equinox.controller.WarningPanel;
import equinox.data.ClientPluginInfo;
import equinox.data.Settings;
import equinox.dataServer.remote.data.EquinoxUpdate;
import equinox.dataServer.remote.data.EquinoxUpdate.EquinoxUpdateInfoType;
import equinox.exchangeServer.remote.message.Announcement;
import equinox.exchangeServer.remote.message.ChatMessage;
import equinox.exchangeServer.remote.message.InstructionSetRunRequest;
import equinox.exchangeServer.remote.message.ShareFile;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.ServerUtility;
import equinox.utility.exception.InternalEngineAnalysisFailedException;
import equinox.utility.exception.ServerAnalysisFailedException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 * Notification panel.
 *
 * @author Murat Artim
 * @date Nov 9, 2014
 * @time 3:03:30 PM
 */
public class NotificationPanel extends NotificationPane implements java.awt.event.ActionListener {

	/** True if notification panel is shown. */
	private volatile boolean isShown_ = false;

	/** Display timer. */
	private final Timer timer_;

	/** Notification queue. */
	private final ConcurrentLinkedQueue<Notification> notifications_;

	/** Tray icon. */
	private final TrayIcon trayIcon_;

	/** The owner main screen. */
	private final MainScreen mainScreen_;

	/**
	 * Creates notification panel.
	 *
	 * @param mainScreen
	 *            The owner main screen.
	 */
	public NotificationPanel(MainScreen mainScreen) {

		// construct pane
		super();

		// set main screen
		mainScreen_ = mainScreen;

		// create notifications list
		notifications_ = new ConcurrentLinkedQueue<>();

		// create timer
		timer_ = new Timer(6000, this);
		timer_.setRepeats(false);

		// setup
		setShowFromTop(false);

		// create tray icon
		trayIcon_ = createTrayIcon(mainScreen);
	}

	@Override
	public void actionPerformed(java.awt.event.ActionEvent e) {
		hide();
	}

	/**
	 * Shows question notification.
	 *
	 * @param title
	 *            Title of notification.
	 * @param text
	 *            Text of notification.
	 * @param yesButtonText
	 *            Yes button text.
	 * @param noButtonText
	 *            No button text.
	 * @param yesButtonAction
	 *            Yes button action.
	 * @param noButtonAction
	 *            No button action.
	 */
	public void showQuestion(String title, String text, String yesButtonText, String noButtonText, EventHandler<ActionEvent> yesButtonAction, EventHandler<ActionEvent> noButtonAction) {
		show(new Notification(MessageType.NONE, text, -1, QuestionNotificationPanel.load(title, text, yesButtonText, noButtonText, yesButtonAction, noButtonAction), true, false));
	}

	/**
	 * Creates and shows server announcement.
	 *
	 * @param announcement
	 *            Server announcement.
	 */
	public void showServerAnnouncement(Announcement announcement) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_MESSAGES))
			return;

		// setup notification text
		String text = announcement.getTitle() + ": " + announcement.getBody();

		// show
		show(new Notification(MessageType.INFO, text, -1, ServerAnnouncementPanel.load(announcement), false, true));
	}

	/**
	 * Creates and shows message notification.
	 *
	 * @param chatPanel
	 *            The chat panel.
	 * @param message
	 *            Chat message.
	 */
	public void showMessage(ChatPopup chatPanel, ChatMessage message) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_MESSAGES))
			return;

		// load and show notification panel
		ChatNotificationPanel.load(mainScreen_, chatPanel, message);
	}

	/**
	 * Creates and shows container update notification.
	 *
	 * @param update
	 *            Update message.
	 */
	public void showContainerUpdates(EquinoxUpdate update) {

		// set message
		String message = "A newer version of Equinox Container (v" + (double) update.getInfo(EquinoxUpdateInfoType.VERSION_NUMBER) + ") is available for download. ";
		message += "Click 'Install' to upgrade your software, or 'Details' to see the content of update.";

		// show notification
		show(new Notification(MessageType.INFO, message, -1, UpdatePanel.load(mainScreen_, message, update), false, true));
	}

	/**
	 * Creates and shows plugin update notification.
	 *
	 * @param toBeUpdated
	 *            To-be-updated plugins.
	 */
	public void showPluginUpdates(ArrayList<ClientPluginInfo> toBeUpdated) {

		// set message
		String message = "Plugin updates are available for download. ";
		message += "Click 'Details' for more information.";

		// show notification
		show(new Notification(MessageType.INFO, message, -1, PluginUpdateNotificationPanel.load(mainScreen_, message, toBeUpdated), false, false));
	}

	/**
	 * Creates and shows material update notification.
	 *
	 * @param toBeDownloaded
	 *            List containing the material ISAMI versions to be downloaded.
	 */
	public void showMaterialUpdates(ArrayList<String> toBeDownloaded) {

		// set message
		String message = "Material library update available. ";
		message += "Do you want to proceed with material library update?";

		// show notification
		show(new Notification(MessageType.INFO, message, -1, MaterialUpdateNotificationPanel.load(mainScreen_, toBeDownloaded), false, false));
	}

	/**
	 * Creates and shows incoming shared file notification.
	 *
	 * @param message
	 *            Message of notification.
	 */
	public void showIncomingSharedFile(ShareFile message) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_FILES))
			return;

		// load and show notification panel
		IncomingFilePanel.load(mainScreen_, message);
	}

	/**
	 * Creates and shows incoming run request notification.
	 *
	 * @param message
	 *            Message of notification.
	 */
	public void showIncomingRunRequest(InstructionSetRunRequest message) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_FILES))
			return;

		// load and show notification panel
		IncomingRunRequestPanel.load(mainScreen_, message);
	}

	/**
	 * Creates and shows info notification.
	 *
	 * @param title
	 *            Title of message.
	 * @param infoText
	 *            Information text.
	 */
	public void showInfo(String title, String infoText) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_INFO))
			return;

		// show notification
		show(new Notification(MessageType.INFO, infoText, -1, InfoNotificationPanel.load(title, infoText), true, false));
	}

	/**
	 * Creates and shows queued task notification.
	 *
	 * @param infoText
	 *            Information text.
	 */
	public void showQueued(String infoText) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_QUEUED))
			return;

		// show notification
		show(new Notification(MessageType.INFO, infoText, -1, QueuedNotificationPanel.load(mainScreen_, infoText), true, false));
	}

	/**
	 * Creates and shows info notification.
	 *
	 * @param infoText
	 *            Information text.
	 */
	public void showSumbitted(String infoText) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_SUBMITTED))
			return;

		// show
		show(new Notification(MessageType.INFO, infoText, -1, SubmittedNotificationPanel.load(mainScreen_, infoText), true, false));
	}

	/**
	 * Creates and shows saved task notification.
	 *
	 * @param infoText
	 *            Information text.
	 */
	public void showSaved(String infoText) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_SAVED))
			return;

		// show notification
		show(new Notification(MessageType.INFO, infoText, -1, SavedNotificationPanel.load(mainScreen_, infoText), true, false));
	}

	/**
	 * Creates and shows scheduled task notification.
	 *
	 * @param infoText
	 *            Information text.
	 */
	public void showScheduled(String infoText) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_SCHEDULED))
			return;

		// show notification
		show(new Notification(MessageType.INFO, infoText, -1, ScheduledNotificationPanel.load(mainScreen_, infoText), true, false));
	}

	/**
	 * Creates and shows warning notification.
	 *
	 * @param message
	 *            Warning message.
	 * @param title
	 *            Title. Can be null for default value (which is 'Warning').
	 */
	public void showWarning(String message, String title) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_WARNINGS))
			return;

		// set title
		if (title == null) {
			title = "Warning";
		}

		// show notification
		show(new Notification(MessageType.WARNING, message, -1, WarningNotificationPanel.load(message, title), true, false));
	}

	/**
	 * Creates and shows insufficient privilege notification.
	 *
	 * @param permission
	 *            The denied permission.
	 */
	public void showPermissionDenied(Permission permission) {

		// data service is not available
		if (!mainScreen_.getDataServerManager().isConnected()) {
			String msg = "You need to be connected to data service in order to perform this operation. Please connect to the service and try again.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.BOTTOM_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(msg, 50, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(mainScreen_.getInputPanel().getDataServiceButton());
			return;
		}

		// set message
		String message = "You don't have sufficient privileges to perform the operation ";
		message += "'" + permission.getDescription() + "'. ";
		message += "Please contact AF-Twin user administrator to request necessary privileges.";

		// show notification
		show(new Notification(MessageType.WARNING, message, -1, PrivilegeNotificationPanel.load(message, permission, mainScreen_), false, true));
	}

	/**
	 * Creates and shows ok notification.
	 *
	 * @param title
	 *            Title of notification.
	 * @param message
	 *            Message of notification.
	 */
	public void showOk(String title, String message) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_SUCCEEDED))
			return;

		// show notification
		show(new Notification(MessageType.INFO, message, -1, OkNotificationPanel.load(title, message), true, false));
	}

	/**
	 * Creates and shows directory outputting task ok notification.
	 *
	 * @param title
	 *            Title of notification.
	 * @param message
	 *            Message of notification.
	 * @param buttonText
	 *            Button text.
	 * @param outputDirectory
	 *            Output directory of the task.
	 */
	public void showDirectoryOutputtingOk(String title, String message, String buttonText, Path outputDirectory) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_SUCCEEDED))
			return;

		// show notification
		show(new Notification(MessageType.INFO, message, -1, DirectoryOutputtingOkNotificationPanel.load(mainScreen_, title, message, buttonText, outputDirectory), false, false));
	}

	/**
	 * Creates and shows error notification.
	 *
	 * @param title
	 *            Title of notification.
	 * @param message
	 *            Message of notification.
	 * @param e
	 *            Error.
	 */
	public void showError(String title, String message, Throwable e) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_ERRORS))
			return;

		// server analysis failed exception
		if (e instanceof ServerAnalysisFailedException) {
			show(new Notification(MessageType.ERROR, message, -1, ServerAnalysisFailedPanel.load(mainScreen_, title, WordUtils.wrap(message, 90), (ServerAnalysisFailedException) e), false, false));
		}

		// internal analysis failed exception
		else if (e instanceof InternalEngineAnalysisFailedException) {
			show(new Notification(MessageType.ERROR, message, -1, InternalEngineAnalysisFailedPanel.load(mainScreen_, title, WordUtils.wrap(message, 90), (InternalEngineAnalysisFailedException) e), false, false));
		}

		// other error message
		else {
			show(new Notification(MessageType.ERROR, message, -1, ErrorPanel.load(mainScreen_, title, WordUtils.wrap(message, 90), e), false, false));
		}
	}

	/**
	 * Creates and shows completed with warnings notification.
	 *
	 * @param title
	 *            Title of notification.
	 * @param message
	 *            Message of notification.
	 * @param warnings
	 *            Warning messages.
	 */
	public void showCompletedWithWarnings(String title, String message, String warnings) {

		// no notification
		if (!(boolean) mainScreen_.getSettings().getValue(Settings.NOTIFY_WARNINGS))
			return;

		// show
		show(new Notification(MessageType.WARNING, message, -1, WarningPanel.load(mainScreen_, title, WordUtils.wrap(message, 100), warnings), false, false));
	}

	/**
	 * Clears all notifications from the queue.
	 */
	public void clearAllNotifications() {
		notifications_.clear();
	}

	/**
	 * Shows notification panel for the given notification.
	 *
	 * @param notification
	 *            Notification to show.
	 */
	public void show(Notification notification) {

		// no notification left to show
		if (notification == null)
			return;

		// a notification is already on display
		if (isShown_) {
			notifications_.add(notification);
			return;
		}

		// set text and graphic
		setText(notification.getText());
		setGraphic(notification.getGraphic());

		// modal notification
		if (notification.isModal()) {

			// show from top
			setShowFromTop(true);

			// set on showing
			setOnShowing(event -> mainScreen_.addModalLayer("modalNotification"));

			// set on hiding
			setOnHiding(event -> mainScreen_.removeModalLayer("modalNotification"));
		}

		// non-modal
		else {
			setOnShowing(null);
			setOnHiding(null);
			boolean fromBottom = (boolean) mainScreen_.getSettings().getValue(Settings.SHOW_NOTIFY_FROM_BOTTOM);
			setShowFromTop(!fromBottom);
		}

		// set actions
		Action[] actions = notification.getActions();
		if (actions != null) {
			getActions().setAll(actions);
		}
		else {
			getActions().clear();
		}

		// set on shown
		setOnShown(event -> {
			isShown_ = true;
			if (notification.hasTimer()) {
				timer_.start();
			}
		});

		// set on hidden
		setOnHidden(event -> {
			isShown_ = false;
			timer_.stop();
			show(notifications_.poll());
		});

		// show
		super.show();

		// show on system tray
		if (mainScreen_.getOwner().getStage().isIconified() && (boolean) mainScreen_.getSettings().getValue(Settings.USE_SYSTEMTRAY)) {

			try {

				// windows or linux
				if (trayIcon_ != null) {
					trayIcon_.displayMessage("Equinox", notification.getSystemTrayText(), notification.getMessageType());
				}

				// mac
				else {
					String title = "\"Equinox\"";
					String message = "\"" + notification.getSystemTrayText() + "\"";
					String soundName = "\"Submarine\"";
					Runtime.getRuntime().exec(new String[] { "osascript", "-e", "display notification " + message + " with title " + title + " sound name " + soundName });
				}
			}

			// cannot show system tray message
			catch (Exception e) {
				Equinox.LOGGER.log(Level.WARNING, "Cannot show system tray message.", e);
			}
		}
	}

	/**
	 * Adds try icon to system tray for notifications.
	 *
	 * @param mainScreen
	 *            Main screen of the application.
	 *
	 * @return Newly created tray icon or null if it cannot be created.
	 */
	private static TrayIcon createTrayIcon(MainScreen mainScreen) {

		try {

			// no support for system tray in Mac OSX
			if (Equinox.OS_TYPE.equals(ServerUtility.MACOS))
				return null;

			// system tray is not supported
			if (!SystemTray.isSupported()) {
				Equinox.LOGGER.info("System tray is not supported.");
				return null;
			}

			// get system tray
			final SystemTray tray = SystemTray.getSystemTray();

			// create tray icon
			final TrayIcon trayIcon = new TrayIcon(new ImageIcon(Equinox.class.getResource("image/equinoxTrayIcon.png"), "tray icon").getImage());
			trayIcon.setImageAutoSize(true);

			// set action listener
			trayIcon.addActionListener(e -> Platform.runLater(() -> mainScreen.getOwner().getStage().setIconified(false)));

			// add to system tray
			tray.add(trayIcon);

			// return tray icon
			return trayIcon;
		}

		// system tray cannot be used
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during creating system tray icon.", e);
			return null;
		}
	}
}
